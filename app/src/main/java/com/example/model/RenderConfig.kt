package com.example.model

import android.graphics.Bitmap
import java.io.File

/**
 * Aspect ratio and resolution presets for video export.
 * Fixed to ensure NO duplicate aspect ratios exist.
 */
enum class AspectRatioPreset(
    val label: String,
    val subtitle: String,
    val width: Int,
    val height: Int,
    val ratioTag: String
) {
    PORTRAIT_9_16(
        label = "9:16 Reels & Story",
        subtitle = "1080 × 1920 (TikTok, Reels, Shorts)",
        width = 1080,
        height = 1920,
        ratioTag = "9:16"
    ),
    LANDSCAPE_16_9(
        label = "16:9 Landscape",
        subtitle = "1920 × 1080 (YouTube, Web)",
        width = 1920,
        height = 1080,
        ratioTag = "16:9"
    ),
    SQUARE_1_1(
        label = "1:1 Square",
        subtitle = "1080 × 1080 (Instagram Square Post)",
        width = 1080,
        height = 1080,
        ratioTag = "1:1"
    ),
    PORTRAIT_4_5(
        label = "4:5 Portrait Feed",
        subtitle = "1080 × 1350 (Instagram Vertical Feed)",
        width = 1080,
        height = 1350,
        ratioTag = "4:5"
    );

    val artboardCssWidth: Int get() = (width / 3).coerceAtLeast(1)
    val artboardCssHeight: Int get() = (height / 3).coerceAtLeast(1)
}

/**
 * Target export frame rates.
 */
enum class FpsOption(val fps: Int, val label: String) {
    FPS_30(30, "30 FPS (Standard Smooth)"),
    FPS_60(60, "60 FPS (Ultra Fluid Motion)")
}

/**
 * Encoding bitrate presets.
 */
enum class BitratePreset(val bps: Int, val label: String) {
    BALANCED(8_000_000, "8 Mbps (Standard HD)"),
    HIGH(14_000_000, "14 Mbps (High Fidelity)"),
    ULTRA(22_000_000, "22 Mbps (Ultra Crystal)")
}

/**
 * Capture method:
 * - REALTIME_RECORDING: Captures live animation at exact 1.0x native playback speed.
 *   Guarantees 100% fluid motion for any CSS keyframes, Canvas, WebGL, and JS loops.
 * - DETERMINISTIC_SCRUB: Stepping through Web Animations API for frame-precise rendering.
 */
enum class CaptureMode(val label: String, val description: String) {
    DETERMINISTIC_SCRUB("Frame-Accurate Clock Step (Recommended)", "Steps frame-by-frame via Animation Timeline API (Exact timing, no freeze)"),
    REALTIME_RECORDING("Real-Time Record", "Live playback recording (for un-steppable WebGL or streaming media)")
}

/**
 * Rendering state machine for progress tracking.
 */
sealed class RenderPhase {
    object Idle : RenderPhase()
    object Initializing : RenderPhase()
    data class Rendering(
        val currentFrame: Int,
        val totalFrames: Int,
        val percent: Float,
        val speedFps: Float,
        val currentBitmap: Bitmap?
    ) : RenderPhase()
    object Finalizing : RenderPhase()
    data class Success(
        val videoFile: File,
        val durationSec: Float,
        val resolution: String,
        val fileSizeBytes: Long
    ) : RenderPhase()
    data class Error(val message: String) : RenderPhase()
}

/**
 * Exported video item saved in app history.
 */
data class ExportedVideoItem(
    val id: String,
    val title: String,
    val file: File,
    val durationSec: Float,
    val resolution: String,
    val timestamp: Long,
    val thumbnail: Bitmap? = null
)
