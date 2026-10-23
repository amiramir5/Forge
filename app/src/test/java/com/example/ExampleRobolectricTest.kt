package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.AnimationTimelineHarness
import com.example.engine.VideoEncoder
import com.example.model.AspectRatioPreset
import com.example.model.CaptureMode
import com.example.model.FpsOption
import com.example.model.MotionTemplatesCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MotionForge", appName)
    }

    @Test
    fun `test all aspect ratio presets have even dimensions required by h264 codecs`() {
        AspectRatioPreset.values().forEach { preset ->
            assertEquals("${preset.name} width must be even", 0, preset.width % 2)
            assertEquals("${preset.name} height must be even", 0, preset.height % 2)
            assertTrue("${preset.name} width must be >= 720", preset.width >= 720)
            assertTrue("${preset.name} height must be >= 720", preset.height >= 720)
        }
    }

    @Test
    fun `test aspect ratio presets have distinct ratios and labels`() {
        val ratios = AspectRatioPreset.values().map { it.ratioTag }
        assertEquals("All aspect ratio tags must be unique", ratios.toSet().size, ratios.size)

        val labels = AspectRatioPreset.values().map { it.label }
        assertEquals("All preset labels must be unique", labels.toSet().size, labels.size)
    }

    @Test
    fun `test animation timeline harness injects deterministic seek methods`() {
        val rawHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>Test</title></head>
            <body><div class="box"></div></body>
            </html>
        """.trimIndent()

        val renderedHtml = AnimationTimelineHarness.buildHtml(
            rawCode = rawHtml,
            cleanControls = true,
            durationSec = 10.0f,
            isExport = true
        )

        assertTrue(renderedHtml.contains("window.__seekAnimation"))
        assertTrue(renderedHtml.contains("window.__motionForgeDuration = 10000"))
        assertTrue(renderedHtml.contains("document.getAnimations"))
        assertTrue(renderedHtml.contains("motionforge-harness"))
        assertTrue(renderedHtml.contains("button, input[type=\"button\"]"))
        assertTrue(renderedHtml.contains("--motion-time"))
    }

    @Test
    fun `test animation timeline harness injects into head with attributes`() {
        val rawHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head lang="en" class="dark-theme">
                <meta charset="utf-8">
                <title>Motion</title>
            </head>
            <body><div class="box"></div></body>
            </html>
        """.trimIndent()

        val renderedHtml = AnimationTimelineHarness.buildHtml(
            rawCode = rawHtml,
            cleanControls = true,
            durationSec = 10.0f,
            isExport = true
        )

        assertTrue(renderedHtml.contains("window.__seekAnimation"))
        assertTrue(renderedHtml.contains("window.__motionForgeDuration = 10000"))
        assertTrue(renderedHtml.contains("document.getAnimations"))
        assertTrue(renderedHtml.contains("<head lang=\"en\" class=\"dark-theme\">"))
    }

    @Test
    fun `test animation timeline harness injects into html without head`() {
        val rawHtml = """
            <html lang="en">
            <body><div class="box"></div></body>
            </html>
        """.trimIndent()

        val renderedHtml = AnimationTimelineHarness.buildHtml(
            rawCode = rawHtml,
            cleanControls = true,
            durationSec = 5.0f,
            isExport = true
        )

        assertTrue(renderedHtml.contains("window.__seekAnimation"))
        assertTrue(renderedHtml.contains("<head>"))
    }

    @Test
    fun `test animation timeline harness wraps raw snippets without structure tags`() {
        val rawSnippet = """
            <style>.circle { width: 50px; height: 50px; border-radius: 50%; }</style>
            <div class="circle"></div>
        """.trimIndent()

        val renderedHtml = AnimationTimelineHarness.buildHtml(
            rawCode = rawSnippet,
            cleanControls = true,
            durationSec = 8.0f,
            isExport = true
        )

        assertTrue(renderedHtml.contains("window.__seekAnimation"))
        assertTrue(renderedHtml.contains("<!DOCTYPE html>"))
        assertTrue(renderedHtml.contains("<div class=\"circle\"></div>"))
    }

    @Test
    fun `test animation timeline calculation for 10 second animation at 60fps`() {
        val durationSec = 10.0f
        val fps = 60
        val totalFrames = (durationSec * fps).toInt()
        assertEquals(600, totalFrames)

        var previousPts = -1L
        for (frame in 0 until totalFrames) {
            val ptsUs = frame * 1_000_000L / fps
            val targetTimeMs = (frame * 1000.0) / fps

            assertTrue("PTS must be strictly monotonic: $ptsUs > $previousPts", ptsUs > previousPts)
            previousPts = ptsUs

            assertEquals(ptsUs / 1000.0, targetTimeMs, 1.0)
        }

        val lastPtsUs = (totalFrames - 1) * 1_000_000L / fps
        assertEquals(9_983_333L, lastPtsUs)
    }

    @Test
    fun `test animation timeline calculation for 10 second animation at 30fps`() {
        val durationSec = 10.0f
        val fps = 30
        val totalFrames = (durationSec * fps).toInt()
        assertEquals(300, totalFrames)

        var previousPts = -1L
        for (frame in 0 until totalFrames) {
            val ptsUs = frame * 1_000_000L / fps
            val targetTimeMs = (frame * 1000.0) / fps

            assertTrue("PTS must be strictly monotonic: $ptsUs > $previousPts", ptsUs > previousPts)
            previousPts = ptsUs

            // Verify timestamp math
            assertEquals(ptsUs / 1000.0, targetTimeMs, 1.0)
        }

        // Final frame at frame 299 or 300 should reach close to 10 seconds
        val lastPtsUs = (totalFrames - 1) * 1_000_000L / fps
        assertEquals(9_966_666L, lastPtsUs)
    }

    @Test
    fun `test video encoder rejects invalid odd dimensions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dummyFile = File(context.cacheDir, "test.mp4")

        try {
            VideoEncoder(dummyFile, 1081, 1920, 30, 8000000)
            fail("Expected IllegalArgumentException for odd width")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("even numbers") == true)
        }

        try {
            VideoEncoder(dummyFile, 1080, 1921, 30, 8000000)
            fail("Expected IllegalArgumentException for odd height")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("even numbers") == true)
        }
    }

    @Test
    fun `test regression test template exists and is properly configured`() {
        val regressionTemplate = MotionTemplatesCatalog.templates.find { it.id == "regression_test_10s" }
        assertNotNull("Regression test template must exist in catalog", regressionTemplate)

        assertEquals(10.0f, regressionTemplate!!.recommendedDurationSec, 0.01f)
        assertEquals(AspectRatioPreset.PORTRAIT_9_16, regressionTemplate.defaultPreset)
        assertTrue(regressionTemplate.htmlCode.contains("moveAcross 10s linear infinite"))
        assertTrue(regressionTemplate.htmlCode.contains("onRenderFrame"))
        assertTrue(regressionTemplate.htmlCode.contains("time-display"))
    }

    @Test
    fun `test capture modes include deterministic clock step as default`() {
        val defaultMode = CaptureMode.DETERMINISTIC_SCRUB
        assertEquals("Frame-Accurate Clock Step (Recommended)", defaultMode.label)
        assertTrue(defaultMode.description.contains("Animation Timeline API"))
    }

    @Test
    fun `test artboard css dimensions match target resolution divided by three`() {
        assertEquals(360, AspectRatioPreset.PORTRAIT_9_16.artboardCssWidth)
        assertEquals(640, AspectRatioPreset.PORTRAIT_9_16.artboardCssHeight)

        assertEquals(640, AspectRatioPreset.LANDSCAPE_16_9.artboardCssWidth)
        assertEquals(360, AspectRatioPreset.LANDSCAPE_16_9.artboardCssHeight)

        assertEquals(360, AspectRatioPreset.SQUARE_1_1.artboardCssWidth)
        assertEquals(360, AspectRatioPreset.SQUARE_1_1.artboardCssHeight)

        assertEquals(360, AspectRatioPreset.PORTRAIT_4_5.artboardCssWidth)
        assertEquals(450, AspectRatioPreset.PORTRAIT_4_5.artboardCssHeight)
    }

    @Test
    fun `test harness injects artboard viewport meta without modulo in waapi seeking`() {
        val rawHtml = "<html><head><title>Artboard</title></head><body>Motion</body></html>"
        val html916 = AnimationTimelineHarness.buildHtml(rawHtml, false, 5f, true, 1080, 1920)
        assertTrue(html916.contains("width=360"))
        assertTrue(html916.contains("window.__mf"))
        assertFalse("WAAPI seeking must never use modulo", html916.contains("currentTime = timeMs %"))

        val html169 = AnimationTimelineHarness.buildHtml(rawHtml, false, 5f, true, 1920, 1080)
        assertTrue(html169.contains("width=640"))
    }
}
