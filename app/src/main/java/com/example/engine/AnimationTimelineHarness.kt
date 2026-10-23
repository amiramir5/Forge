package com.example.engine

import java.util.regex.Matcher

/**
 * Professional-grade, deterministic Animation Timeline Harness.
 *
 * Provides Remotion/Puppeteer-level frame accuracy, timing determinism,
 * and layout consistency between preview and video export.
 *
 * Key Capabilities:
 * 1. Fixed Logical Artboard (CSS width = targetWidth / 3) and exact pixel ratio alignment.
 * 2. Absolute Web Animations API (WAAPI) currentTime seeking (NO modulo; preserves delay & alternate direction).
 * 3. Media elements (<video>, <audio>) synchronization and pause/seek control.
 * 4. SVG SMIL animation synchronization (setCurrentTime).
 * 5. Virtualized Date.now(), performance.now(), requestAnimationFrame, and virtual timer queue.
 * 6. Deterministic PRNG (mulberry32) for reproducible procedural / particle effects in export.
 * 7. Unified window.__mf API with backward-compatible aliases for window.__seekAnimation.
 */
object AnimationTimelineHarness {

    const val JS_HARNESS = """
    <script id="motionforge-harness">
    (function() {
        if (window.__mfInitialized) return;
        window.__mfInitialized = true;

        var nativePerfNow = (window.performance && window.performance.now) ? window.performance.now.bind(window.performance) : null;
        var nativeDateNow = Date.now ? Date.now.bind(Date) : function() { return new Date().getTime(); };
        var nativeRaf = window.requestAnimationFrame ? window.requestAnimationFrame.bind(window) : null;
        var nativeCaf = window.cancelAnimationFrame ? window.cancelAnimationFrame.bind(window) : null;
        var nativeSetTimeout = window.setTimeout ? window.setTimeout.bind(window) : null;
        var nativeClearTimeout = window.clearTimeout ? window.clearTimeout.bind(window) : null;
        var nativeSetInterval = window.setInterval ? window.setInterval.bind(window) : null;
        var nativeClearInterval = window.clearInterval ? window.clearInterval.bind(window) : null;
        var nativeRandom = Math.random.bind(Math);

        // Mulberry32 deterministic PRNG
        var prngSeed = 0x12345678;
        function mulberry32() {
            var t = (prngSeed += 0x6D2B79F5);
            t = Math.imul(t ^ (t >>> 15), t | 1);
            t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
            return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
        }

        var mf = {
            time: 0,
            duration: 10000,
            isExport: false,
            isVirtual: true,
            rafQueue: [],
            timerQueue: [],
            nextId: 1,

            resetPrng: function(seed) {
                prngSeed = (typeof seed === 'number') ? (seed >>> 0) : 0x12345678;
            },

            seek: function(timeMs, durationMs) {
                mf.isVirtual = true;
                mf.time = (typeof timeMs === 'number' && !isNaN(timeMs)) ? timeMs : 0;
                if (typeof durationMs === 'number' && durationMs > 0) {
                    mf.duration = durationMs;
                }

                // 1. Process virtual timers scheduled up to this timestamp
                if (mf.isExport) {
                    var remainingTimers = [];
                    for (var t = 0; t < mf.timerQueue.length; t++) {
                        var timer = mf.timerQueue[t];
                        if (timer.triggerTime <= mf.time) {
                            try { timer.cb(); } catch (e) {}
                            if (timer.isInterval && timer.interval > 0) {
                                timer.triggerTime += timer.interval;
                                remainingTimers.push(timer);
                            }
                        } else {
                            remainingTimers.push(timer);
                        }
                    }
                    mf.timerQueue = remainingTimers;
                }

                // 2. Force CSS style resolution before querying animations
                if (document.documentElement) {
                    try { void window.getComputedStyle(document.documentElement).opacity; } catch (e) {}
                }

                // 3. Web Animations API (CSS @keyframes and Element.animate)
                // Note: We ALWAYS set currentTime directly without modulo.
                // Chromium's WAAPI effect engine natively handles iterations, delays, and alternate directions!
                if (document.getAnimations) {
                    try {
                        var anims = document.getAnimations({ subtree: true });
                        for (var i = 0; i < anims.length; i++) {
                            try {
                                var a = anims[i];
                                if (a.playState !== 'paused') {
                                    a.pause();
                                }
                                a.currentTime = mf.time;
                            } catch (e) {}
                        }
                    } catch (e) {}
                }

                // 4. SVG SMIL Animations
                try {
                    var svgs = document.querySelectorAll('svg');
                    for (var s = 0; s < svgs.length; s++) {
                        if (typeof svgs[s].setCurrentTime === 'function') {
                            svgs[s].setCurrentTime(mf.time / 1000);
                        }
                    }
                } catch (e) {}

                // 5. Media elements (<video>, <audio>)
                try {
                    var mediaElements = document.querySelectorAll('video, audio');
                    for (var m = 0; m < mediaElements.length; m++) {
                        try {
                            var el = mediaElements[m];
                            if (!el.paused) el.pause();
                            el.currentTime = mf.time / 1000;
                        } catch (e) {}
                    }
                } catch (e) {}

                // 6. CSS Custom Properties (--motion-time, --motion-progress)
                try {
                    var progress = mf.duration > 0 ? (mf.time / mf.duration) : 0;
                    document.documentElement.style.setProperty('--motion-time', mf.time + 'ms');
                    document.documentElement.style.setProperty('--motion-progress', progress.toString());
                } catch (e) {}

                // 7. Custom frame render hook
                if (typeof window.onRenderFrame === 'function') {
                    try {
                        window.onRenderFrame(mf.time, mf.duration > 0 ? (mf.time / mf.duration) : 0);
                    } catch (e) {}
                }

                // 8. Drain and execute queued requestAnimationFrame callbacks with exact timestamp
                var q = mf.rafQueue.slice();
                mf.rafQueue = [];
                for (var j = 0; j < q.length; j++) {
                    try {
                        q[j].cb(mf.time);
                    } catch (e) {}
                }

                // 9. Synchronous layout flush
                if (document.body) {
                    void document.body.offsetHeight;
                }

                return true;
            },

            play: function() {
                mf.isVirtual = false;
                if (document.getAnimations) {
                    try {
                        var anims = document.getAnimations({ subtree: true });
                        for (var i = 0; i < anims.length; i++) {
                            try { anims[i].play(); } catch (e) {}
                        }
                    } catch (e) {}
                }
                try {
                    var mediaElements = document.querySelectorAll('video, audio');
                    for (var m = 0; m < mediaElements.length; m++) {
                        try { mediaElements[m].play(); } catch (e) {}
                    }
                } catch (e) {}
                if (document.body) {
                    void document.body.offsetHeight;
                }
            },

            pause: function() {
                mf.isVirtual = true;
                if (document.getAnimations) {
                    try {
                        var anims = document.getAnimations({ subtree: true });
                        for (var i = 0; i < anims.length; i++) {
                            try { anims[i].pause(); } catch (e) {}
                        }
                    } catch (e) {}
                }
                try {
                    var mediaElements = document.querySelectorAll('video, audio');
                    for (var m = 0; m < mediaElements.length; m++) {
                        try { mediaElements[m].pause(); } catch (e) {}
                    }
                } catch (e) {}
                if (document.body) {
                    void document.body.offsetHeight;
                }
            }
        };

        window.__mf = mf;

        // Virtualized requestAnimationFrame
        window.requestAnimationFrame = function(cb) {
            if (!mf.isVirtual && nativeRaf) {
                return nativeRaf(cb);
            }
            var id = mf.nextId++;
            mf.rafQueue.push({ id: id, cb: cb });
            return id;
        };

        window.cancelAnimationFrame = function(id) {
            if (!mf.isVirtual && nativeCaf) {
                return nativeCaf(id);
            }
            mf.rafQueue = mf.rafQueue.filter(function(item) { return item.id !== id; });
        };

        // Virtualized performance.now and Date.now
        if (window.performance) {
            window.performance.now = function() {
                if (mf.isVirtual || mf.isExport) {
                    return mf.time;
                }
                return nativePerfNow ? nativePerfNow() : Date.now();
            };
        }

        Date.now = function() {
            if (mf.isExport) {
                return mf.time;
            }
            return nativeDateNow();
        };

        // Virtualized setTimeout / setInterval for deterministic export
        window.setTimeout = function(cb, delay) {
            if (!mf.isExport && nativeSetTimeout) {
                return nativeSetTimeout(cb, delay);
            }
            var id = mf.nextId++;
            var delayMs = typeof delay === 'number' ? delay : 0;
            mf.timerQueue.push({
                id: id,
                cb: cb,
                triggerTime: mf.time + delayMs,
                isInterval: false,
                interval: 0
            });
            return id;
        };

        window.clearTimeout = function(id) {
            if (!mf.isExport && nativeClearTimeout) {
                return nativeClearTimeout(id);
            }
            mf.timerQueue = mf.timerQueue.filter(function(t) { return t.id !== id; });
        };

        window.setInterval = function(cb, interval) {
            if (!mf.isExport && nativeSetInterval) {
                return nativeSetInterval(cb, interval);
            }
            var id = mf.nextId++;
            var intervalMs = typeof interval === 'number' && interval > 0 ? interval : 16;
            mf.timerQueue.push({
                id: id,
                cb: cb,
                triggerTime: mf.time + intervalMs,
                isInterval: true,
                interval: intervalMs
            });
            return id;
        };

        window.clearInterval = function(id) {
            if (!mf.isExport && nativeClearInterval) {
                return nativeClearInterval(id);
            }
            mf.timerQueue = mf.timerQueue.filter(function(t) { return t.id !== id; });
        };

        // Backward compatibility bridge
        window.__motionForgeTime = 0;
        window.__motionForgeDuration = 10000;
        window.__seekAnimation = function(timeMs, durationMs) {
            window.__motionForgeTime = timeMs;
            if (durationMs) window.__motionForgeDuration = durationMs;
            return mf.seek(timeMs, durationMs);
        };
        window.__resumeLivePlay = function() { mf.play(); };
        window.__pauseLivePlay = function() { mf.pause(); };
    })();
    </script>
    """

    fun buildHtml(
        rawCode: String,
        cleanControls: Boolean,
        durationSec: Float,
        isExport: Boolean = false,
        targetWidth: Int = 1080,
        targetHeight: Int = 1920
    ): String {
        if (rawCode.isBlank()) return ""

        val durationMs = (durationSec * 1000).toLong()

        // Exact logical artboard: target_px / 3 CSS px
        // 1080x1920 -> 360x640, 1920x1080 -> 640x360, 1080x1080 -> 360x360, 1080x1350 -> 360x450
        val artboardCssWidth = (targetWidth / 3).coerceAtLeast(1)
        val artboardCssHeight = (targetHeight / 3).coerceAtLeast(1)

        val cleanControlsCss = if (cleanControls) {
            """
            [data-mf-hide], .mf-hide, button, input[type="button"], input[type="submit"],
            .controls, .control-bar, .buttons, .bottom-bar, footer.controls {
                display: none !important;
                visibility: hidden !important;
            }
            """.trimIndent()
        } else ""

        val layoutCss = """
            <style id="motionforge-layout-css">
              html, body {
                  margin: 0 !important;
                  padding: 0 !important;
                  overflow: hidden !important;
                  width: ${artboardCssWidth}px !important;
                  height: ${artboardCssHeight}px !important;
                  max-width: ${artboardCssWidth}px !important;
                  max-height: ${artboardCssHeight}px !important;
                  box-sizing: border-box !important;
              }
              * {
                  box-sizing: border-box;
              }
              $cleanControlsCss
            </style>
        """.trimIndent()

        val modeConfigScript = if (isExport) {
            """
            <script id="motionforge-mode-config">
              if (window.__mf) {
                  window.__mf.isExport = true;
                  window.__mf.duration = $durationMs;
                  window.__mf.resetPrng(42);
              }
              window.__motionForgeDuration = $durationMs;
              function __initMotionPause() {
                  if (window.__mf) {
                      window.__mf.seek(0, $durationMs);
                  } else if (window.__seekAnimation) {
                      window.__seekAnimation(0, $durationMs);
                  }
              }
              if (document.readyState === 'loading') {
                  document.addEventListener('DOMContentLoaded', __initMotionPause);
              } else {
                  __initMotionPause();
              }
            </script>
            """.trimIndent()
        } else {
            """
            <script id="motionforge-mode-config">
              if (window.__mf) {
                  window.__mf.isExport = false;
                  window.__mf.duration = $durationMs;
              }
              window.__motionForgeDuration = $durationMs;
            </script>
            """.trimIndent()
        }

        // Viewport meta enforces exact artboard CSS pixels
        val viewportMeta = "<meta name=\"viewport\" content=\"width=$artboardCssWidth, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">"

        val injection = """
            $viewportMeta
            $layoutCss
            $JS_HARNESS
            $modeConfigScript
        """.trimIndent()

        // Strip any existing conflicting viewport meta
        val strippedHtml = rawCode.replace("(?i)<meta\\s+name=[\"']viewport[\"'][^>]*>".toRegex(), "")

        val headRegex = "(?i)<head(\\s[^>]*)?>".toRegex()
        val htmlRegex = "(?i)<html(\\s[^>]*)?>".toRegex()
        val bodyRegex = "(?i)<body(\\s[^>]*)?>".toRegex()

        val quotedInjection = Matcher.quoteReplacement(injection)

        return when {
            headRegex.containsMatchIn(strippedHtml) -> {
                strippedHtml.replaceFirst(headRegex, "$0\n$quotedInjection")
            }
            htmlRegex.containsMatchIn(strippedHtml) -> {
                strippedHtml.replaceFirst(htmlRegex, "$0\n<head>\n$quotedInjection\n</head>")
            }
            bodyRegex.containsMatchIn(strippedHtml) -> {
                strippedHtml.replaceFirst(bodyRegex, "<head>\n$quotedInjection\n</head>\n$0")
            }
            else -> {
                "<!DOCTYPE html>\n<html>\n<head>\n$injection\n</head>\n<body>\n$strippedHtml\n</body>\n</html>"
            }
        }
    }
}
