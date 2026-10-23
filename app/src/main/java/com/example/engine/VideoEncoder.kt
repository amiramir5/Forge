package com.example.engine

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * High-performance, hardware-accelerated H.264/AVC Video Encoder.
 *
 * Implements:
 * 1. Safe encoder selection and capabilities verification.
 * 2. Box-filtered 2x2 chroma subsampling (ITU-R BT.601) to eliminate color bleeding.
 * 3. Strict backpressure handling (zero dropped frames during offline rendering).
 * 4. Reliable End-Of-Stream (EOS) propagation and MP4 muxer finalization.
 * 5. Comprehensive diagnostic logging and metrics.
 */
class VideoEncoder(
    private val outputFile: File,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrate: Int
) {
    companion object {
        private const val TAG = "VideoEncoder"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val TIMEOUT_USEC = 10000L // 10ms timeout per dequeue
    }

    private var codec: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var trackIndex = -1
    private var muxerStarted = false
    private val bufferInfo = MediaCodec.BufferInfo()

    var chosenCodecName: String = "unknown"
        private set
    var chosenColorFormat: Int = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        private set

    // Diagnostic metrics
    var framesSubmitted: Int = 0
        private set
    var framesWrittenToMuxer: Int = 0
        private set
    var droppedFrames: Int = 0
        private set
    var totalBytesWritten: Long = 0L
        private set

    private var argbBuffer = IntArray(width * height)
    private var yuvBuffer = ByteArray(width * height * 3 / 2)

    init {
        require(width > 0 && height > 0) { "Dimensions must be positive: ${width}x$height" }
        require(width % 2 == 0 && height % 2 == 0) { "Width and height must be even numbers: ${width}x$height" }
        require(fps > 0) { "FPS must be positive: $fps" }
        require(bitrate > 0) { "Bitrate must be positive: $bitrate" }
    }

    fun start() {
        val codecInfo = selectCodecInfo(MIME_TYPE)
            ?: throw IllegalStateException("No H.264 (AVC) encoder available on this device")
        chosenCodecName = codecInfo.name

        val caps = codecInfo.getCapabilitiesForType(MIME_TYPE)
        chosenColorFormat = selectSupportedColorFormat(caps)

        val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, chosenColorFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second keyframe interval

            // Explicit BT.709 Limited Range SDR video metadata for pristine color fidelity
            try {
                setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709)
                setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED)
                setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_SDR_VIDEO)
            } catch (e: Exception) {
                Log.w(TAG, "Color metadata not supported on this device: ${e.message}")
            }

            // High profile if supported, otherwise baseline
            try {
                if (caps.profileLevels.any { it.profile == MediaCodecInfo.CodecProfileLevel.AVCProfileHigh }) {
                    setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
                }
            } catch (e: Exception) {
                // Ignore profile level query failure
            }
        }

        Log.i(TAG, "Configuring encoder $chosenCodecName: ${width}x${height}@${fps}fps, bitrate=${bitrate}bps, colorFormat=$chosenColorFormat")

        val mediaCodec = MediaCodec.createByCodecName(chosenCodecName)
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec.start()
        this.codec = mediaCodec

        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxerStarted = false
        trackIndex = -1
        framesSubmitted = 0
        framesWrittenToMuxer = 0
        droppedFrames = 0
        totalBytesWritten = 0L
    }

    private fun selectCodecInfo(mimeType: String): MediaCodecInfo? {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        var softwareFallback: MediaCodecInfo? = null

        for (info in codecList.codecInfos) {
            if (!info.isEncoder) continue
            val types = info.supportedTypes
            if (types.any { it.equals(mimeType, ignoreCase = true) }) {
                // Check if size is supported
                try {
                    val caps = info.getCapabilitiesForType(mimeType)
                    val vCaps = caps.videoCapabilities
                    if (vCaps != null && vCaps.isSizeSupported(width, height)) {
                        // Prefer hardware encoder (doesn't start with OMX.google or c2.android)
                        val isSoftware = info.name.startsWith("OMX.google.", ignoreCase = true) ||
                                info.name.startsWith("c2.android.", ignoreCase = true)
                        if (!isSoftware) {
                            return info
                        } else if (softwareFallback == null) {
                            softwareFallback = info
                        }
                    }
                } catch (e: Exception) {
                    // Try next
                }
            }
        }

        return softwareFallback ?: codecList.codecInfos.firstOrNull { it.isEncoder && it.supportedTypes.any { t -> t.equals(mimeType, ignoreCase = true) } }
    }

    private fun selectSupportedColorFormat(caps: MediaCodecInfo.CodecCapabilities): Int {
        for (fmt in caps.colorFormats) {
            if (fmt == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                return fmt // Preferred NV12
            }
            if (fmt == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
                return fmt // Fallback I420
            }
        }
        return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
    }

    fun encodeFrame(bitmap: Bitmap, presentationTimeUs: Long) {
        val mediaCodec = codec ?: throw IllegalStateException("Encoder has not been started")

        // 1. Drain available output buffers first to avoid pipeline stalls
        drainEncoder(false)

        // 2. Extract ARGB pixels and convert to YUV with 2x2 box filtering
        bitmap.getPixels(argbBuffer, 0, width, 0, 0, width, height)
        if (chosenColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
            convertArgbToI420BoxFiltered(argbBuffer, yuvBuffer, width, height)
        } else {
            convertArgbToNV12BoxFiltered(argbBuffer, yuvBuffer, width, height)
        }

        // 3. Dequeue input buffer with retry loop (never drop frames during offline rendering)
        var inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC)
        var tries = 0
        val maxTries = 50 // 50 * 10ms = 500ms max backpressure wait
        while (inputBufferIndex < 0 && tries < maxTries) {
            drainEncoder(false)
            inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC)
            tries++
        }

        if (inputBufferIndex >= 0) {
            val inputBuffer: ByteBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                ?: throw IllegalStateException("MediaCodec input buffer was null at index $inputBufferIndex")
            inputBuffer.clear()
            inputBuffer.put(yuvBuffer)
            mediaCodec.queueInputBuffer(
                inputBufferIndex,
                0,
                yuvBuffer.size,
                presentationTimeUs,
                0
            )
            framesSubmitted++
        } else {
            // Buffer starvation occurred
            droppedFrames++
            Log.e(TAG, "Input buffer starvation at ptsUs=$presentationTimeUs after $tries retries")
            throw IllegalStateException("Encoder input buffer starved at ptsUs=$presentationTimeUs")
        }

        // 4. Drain again
        drainEncoder(false)
    }

    fun finish() {
        val mediaCodec = codec ?: return
        Log.i(TAG, "Finalizing encoder. Total submitted frames: $framesSubmitted")

        // Submit End-Of-Stream buffer with retry
        var eosQueued = false
        var tries = 0
        while (!eosQueued && tries < 50) {
            val inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC)
            if (inputBufferIndex >= 0) {
                mediaCodec.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    0,
                    0,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
                eosQueued = true
            } else {
                drainEncoder(false)
                tries++
            }
        }

        if (!eosQueued) {
            Log.w(TAG, "Could not dequeue input buffer to queue EOS; proceeding to drain")
        }

        // Drain until end-of-stream reached
        drainEncoder(true)

        Log.i(TAG, "Encoder finalized: $framesWrittenToMuxer frames written to muxer, totalBytes=$totalBytesWritten")
        release()
    }

    private fun drainEncoder(endOfStream: Boolean) {
        val mediaCodec = codec ?: return
        val mediaMuxer = muxer ?: return
        var tryAgainCount = 0

        while (true) {
            val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
                tryAgainCount++
                if (tryAgainCount > 50) { // 50 * 10ms = 500ms timeout on EOS drain
                    Log.w(TAG, "Timed out waiting for EOS buffer from encoder")
                    break
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (muxerStarted) {
                    Log.w(TAG, "Format changed after muxer started; ignoring")
                } else {
                    val newFormat = mediaCodec.outputFormat
                    Log.i(TAG, "Encoder output format configured: $newFormat")
                    trackIndex = mediaMuxer.addTrack(newFormat)
                    mediaMuxer.start()
                    muxerStarted = true
                }
            } else if (outputBufferIndex >= 0) {
                tryAgainCount = 0
                val encodedData: ByteBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)
                    ?: throw RuntimeException("OutputBuffer was null at index $outputBufferIndex")

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // Codec config data (SPS/PPS), handled by format
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0) {
                    if (!muxerStarted) {
                        throw RuntimeException("Muxer hasn't started yet when writing sample data")
                    }
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    mediaMuxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    framesWrittenToMuxer++
                    totalBytesWritten += bufferInfo.size
                }

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    Log.i(TAG, "Reached end-of-stream buffer flag")
                    break
                }
            }
        }
    }

    fun release() {
        try {
            codec?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping codec", e)
        }
        try {
            codec?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing codec", e)
        }
        codec = null

        try {
            if (muxerStarted) {
                muxer?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping muxer", e)
        }
        try {
            muxer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing muxer", e)
        }
        muxer = null
        muxerStarted = false
    }

    /**
     * High-quality ARGB to NV12 converter with 2x2 box filtering for chroma subsampling.
     * Conforms to ITU-R BT.709 limited-range standard (Y: 16-235, UV: 16-240).
     */
    private fun convertArgbToNV12BoxFiltered(argb: IntArray, nv12: ByteArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize

        for (j in 0 until height step 2) {
            val row0Offset = j * width
            val row1Offset = (j + 1) * width

            for (i in 0 until width step 2) {
                // Top-left pixel (row0, col0)
                val c00 = argb[row0Offset + i]
                val r00 = (c00 shr 16) and 0xff
                val g00 = (c00 shr 8) and 0xff
                val b00 = c00 and 0xff
                val y00 = ((47 * r00 + 157 * g00 + 16 * b00 + 128) shr 8) + 16
                nv12[yIndex] = (if (y00 > 235) 235 else if (y00 < 16) 16 else y00).toByte()

                // Top-right pixel (row0, col1)
                val c01 = argb[row0Offset + i + 1]
                val r01 = (c01 shr 16) and 0xff
                val g01 = (c01 shr 8) and 0xff
                val b01 = c01 and 0xff
                val y01 = ((47 * r01 + 157 * g01 + 16 * b01 + 128) shr 8) + 16
                nv12[yIndex + 1] = (if (y01 > 235) 235 else if (y01 < 16) 16 else y01).toByte()

                // Bottom-left pixel (row1, col0)
                val c10 = argb[row1Offset + i]
                val r10 = (c10 shr 16) and 0xff
                val g10 = (c10 shr 8) and 0xff
                val b10 = c10 and 0xff
                val y10 = ((47 * r10 + 157 * g10 + 16 * b10 + 128) shr 8) + 16
                nv12[yIndex + width] = (if (y10 > 235) 235 else if (y10 < 16) 16 else y10).toByte()

                // Bottom-right pixel (row1, col1)
                val c11 = argb[row1Offset + i + 1]
                val r11 = (c11 shr 16) and 0xff
                val g11 = (c11 shr 8) and 0xff
                val b11 = c11 and 0xff
                val y11 = ((47 * r11 + 157 * g11 + 16 * b11 + 128) shr 8) + 16
                nv12[yIndex + width + 1] = (if (y11 > 235) 235 else if (y11 < 16) 16 else y11).toByte()

                // Box-filtered chroma: 2x2 average of RGB channels (ITU-R BT.709)
                val rAvg = (r00 + r01 + r10 + r11) shr 2
                val gAvg = (g00 + g01 + g10 + g11) shr 2
                val bAvg = (b00 + b01 + b10 + b11) shr 2

                val u = ((-26 * rAvg - 87 * gAvg + 112 * bAvg + 128) shr 8) + 128
                val v = ((112 * rAvg - 102 * gAvg - 10 * bAvg + 128) shr 8) + 128

                nv12[uvIndex] = (if (u > 240) 240 else if (u < 16) 16 else u).toByte()
                nv12[uvIndex + 1] = (if (v > 240) 240 else if (v < 16) 16 else v).toByte()

                yIndex += 2
                uvIndex += 2
            }
            yIndex += width
        }
    }

    /**
     * High-quality ARGB to I420 converter with 2x2 box filtering (ITU-R BT.709).
     */
    private fun convertArgbToI420BoxFiltered(argb: IntArray, i420: ByteArray, width: Int, height: Int) {
        val frameSize = width * height
        val qFrameSize = frameSize / 4
        var yIndex = 0
        var uIndex = frameSize
        var vIndex = frameSize + qFrameSize

        for (j in 0 until height step 2) {
            val row0Offset = j * width
            val row1Offset = (j + 1) * width

            for (i in 0 until width step 2) {
                val c00 = argb[row0Offset + i]
                val r00 = (c00 shr 16) and 0xff
                val g00 = (c00 shr 8) and 0xff
                val b00 = c00 and 0xff
                val y00 = ((47 * r00 + 157 * g00 + 16 * b00 + 128) shr 8) + 16
                i420[yIndex] = (if (y00 > 235) 235 else if (y00 < 16) 16 else y00).toByte()

                val c01 = argb[row0Offset + i + 1]
                val r01 = (c01 shr 16) and 0xff
                val g01 = (c01 shr 8) and 0xff
                val b01 = c01 and 0xff
                val y01 = ((47 * r01 + 157 * g01 + 16 * b01 + 128) shr 8) + 16
                i420[yIndex + 1] = (if (y01 > 235) 235 else if (y01 < 16) 16 else y01).toByte()

                val c10 = argb[row1Offset + i]
                val r10 = (c10 shr 16) and 0xff
                val g10 = (c10 shr 8) and 0xff
                val b10 = c10 and 0xff
                val y10 = ((47 * r10 + 157 * g10 + 16 * b10 + 128) shr 8) + 16
                i420[yIndex + width] = (if (y10 > 235) 235 else if (y10 < 16) 16 else y10).toByte()

                val c11 = argb[row1Offset + i + 1]
                val r11 = (c11 shr 16) and 0xff
                val g11 = (c11 shr 8) and 0xff
                val b11 = c11 and 0xff
                val y11 = ((47 * r11 + 157 * g11 + 16 * b11 + 128) shr 8) + 16
                i420[yIndex + width + 1] = (if (y11 > 235) 235 else if (y11 < 16) 16 else y11).toByte()

                val rAvg = (r00 + r01 + r10 + r11) shr 2
                val gAvg = (g00 + g01 + g10 + g11) shr 2
                val bAvg = (b00 + b01 + b10 + b11) shr 2

                val u = ((-26 * rAvg - 87 * gAvg + 112 * bAvg + 128) shr 8) + 128
                val v = ((112 * rAvg - 102 * gAvg - 10 * bAvg + 128) shr 8) + 128

                i420[uIndex++] = (if (u > 240) 240 else if (u < 16) 16 else u).toByte()
                i420[vIndex++] = (if (v > 240) 240 else if (v < 16) 16 else v).toByte()

                yIndex += 2
            }
            yIndex += width
        }
    }
}
