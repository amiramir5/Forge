package com.example.model

data class MotionTemplate(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val recommendedDurationSec: Float,
    val defaultPreset: AspectRatioPreset,
    val htmlCode: String
)

object MotionTemplatesCatalog {
    val templates: List<MotionTemplate> = listOf(
        MotionTemplate(
            id = "regression_test_10s",
            name = "🧪 Regression Test (10s Motion)",
            category = "Verification & Diagnostics",
            description = "Continuous 10s left-to-right moving shape with color morphing and numeric time indicator",
            recommendedDurationSec = 10.0f,
            defaultPreset = AspectRatioPreset.PORTRAIT_9_16,
            htmlCode = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: #000000;
    color: #ffffff;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, monospace;
    height: 100vh;
    width: 100vw;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    position: relative;
  }
  .test-title {
    position: absolute;
    top: 60px;
    font-size: 20px;
    letter-spacing: 2px;
    text-transform: uppercase;
    color: #888888;
    font-weight: 700;
  }
  .track {
    position: relative;
    width: 86vw;
    height: 220px;
    border: 2px dashed rgba(255, 255, 255, 0.2);
    border-radius: 24px;
    display: flex;
    align-items: center;
    overflow: hidden;
  }
  #moving-box {
    position: absolute;
    width: 100px;
    height: 100px;
    border-radius: 20px;
    box-shadow: 0 0 35px rgba(255,255,255,0.4);
    animation: moveAcross 10s linear infinite;
  }
  @keyframes moveAcross {
    0% {
      left: 2%;
      background-color: #ff3b30;
      transform: scale(1) rotate(0deg);
    }
    25% {
      left: 22%;
      background-color: #ff9500;
      transform: scale(1.15) rotate(45deg);
    }
    50% {
      left: 45%;
      background-color: #34c759;
      transform: scale(1.3) rotate(90deg);
    }
    75% {
      left: 65%;
      background-color: #007aff;
      transform: scale(1.15) rotate(135deg);
    }
    100% {
      left: 78%;
      background-color: #af52de;
      transform: scale(1) rotate(180deg);
    }
  }
  .time-badge {
    position: absolute;
    bottom: 80px;
    background: #111317;
    border: 1px solid rgba(255,255,255,0.25);
    padding: 12px 28px;
    border-radius: 30px;
    font-size: 26px;
    font-weight: 800;
    color: #ffffff;
    letter-spacing: 1px;
  }
  .diagnostic-note {
    position: absolute;
    bottom: 40px;
    font-size: 11px;
    color: #666666;
    letter-spacing: 1px;
  }
</style>
</head>
<body>
  <div class="test-title">Motion Timing Verification</div>
  <div class="track">
    <div id="moving-box"></div>
  </div>
  <div class="time-badge" id="time-display">00.0s / 10.0s</div>
  <div class="diagnostic-note">Continuous 10s Motion • 30/60 FPS • Non-Freezing</div>

  <script>
    window.onRenderFrame = function(timeMs, progress) {
      var sec = (timeMs / 1000).toFixed(1);
      var el = document.getElementById('time-display');
      if (el) {
        el.innerText = (sec < 10 ? '0' : '') + sec + 's / 10.0s';
      }
    };
  </script>
</body>
</html>
            """.trimIndent()
        ),
        MotionTemplate(
            id = "neon_cyber_pulse",
            name = "Neon Cyber Pulse",
            category = "Motion Graphics",
            description = "Glowing pulsating rings with neon kinetic text and holographic chromatic sweep",
            recommendedDurationSec = 4.0f,
            defaultPreset = AspectRatioPreset.PORTRAIT_9_16,
            htmlCode = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: #050811;
    color: #ffffff;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
    height: 100vh;
    width: 100vw;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    position: relative;
  }

  /* Radiant background mesh */
  .glow-mesh {
    position: absolute;
    width: 500px;
    height: 500px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(0,229,255,0.25) 0%, rgba(168,85,247,0.15) 50%, transparent 70%);
    filter: blur(40px);
    animation: pulseMesh 3s ease-in-out infinite alternate;
  }

  @keyframes pulseMesh {
    0% { transform: scale(0.85); opacity: 0.6; }
    100% { transform: scale(1.25); opacity: 1; }
  }

  /* Rotating Kinetic Rings */
  .ring-container {
    position: relative;
    width: 280px;
    height: 280px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .ring {
    position: absolute;
    border-radius: 50%;
    border: 3px solid transparent;
  }

  .ring-1 {
    width: 260px;
    height: 260px;
    border-top-color: #00e5ff;
    border-right-color: #a855f7;
    animation: spinClockwise 4s linear infinite;
    filter: drop-shadow(0 0 16px rgba(0,229,255,0.7));
  }

  .ring-2 {
    width: 210px;
    height: 210px;
    border-bottom-color: #ec4899;
    border-left-color: #00e5ff;
    animation: spinCounter 3s cubic-bezier(0.4, 0, 0.2, 1) infinite;
    filter: drop-shadow(0 0 14px rgba(236,72,153,0.7));
  }

  .ring-3 {
    width: 160px;
    height: 160px;
    border-top-color: #f59e0b;
    border-bottom-color: #a855f7;
    animation: spinClockwise 2.2s ease-in-out infinite alternate;
    filter: drop-shadow(0 0 10px rgba(168,85,247,0.8));
  }

  @keyframes spinClockwise {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }

  @keyframes spinCounter {
    0% { transform: rotate(360deg); }
    100% { transform: rotate(0deg); }
  }

  .core-badge {
    position: relative;
    z-index: 2;
    background: rgba(15, 23, 42, 0.85);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 20px;
    padding: 14px 28px;
    text-align: center;
    box-shadow: 0 10px 30px rgba(0,0,0,0.5);
    animation: floatBadge 3s ease-in-out infinite alternate;
  }

  @keyframes floatBadge {
    0% { transform: translateY(6px); }
    100% { transform: translateY(-6px); }
  }

  .badge-tag {
    font-size: 11px;
    letter-spacing: 3px;
    text-transform: uppercase;
    color: #38bdf8;
    font-weight: 700;
    margin-bottom: 4px;
  }

  .badge-title {
    font-size: 26px;
    font-weight: 900;
    letter-spacing: -0.5px;
    background: linear-gradient(135deg, #00e5ff, #ffffff, #a855f7);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  /* Bottom audio-wave bars */
  .wave-bars {
    display: flex;
    gap: 6px;
    margin-top: 50px;
    align-items: center;
    height: 40px;
  }

  .bar {
    width: 6px;
    background: linear-gradient(to top, #a855f7, #00e5ff);
    border-radius: 999px;
    animation: waveBounce 1.2s ease-in-out infinite alternate;
    filter: drop-shadow(0 0 6px rgba(0,229,255,0.6));
  }

  .bar:nth-child(1) { height: 16px; animation-delay: 0.1s; }
  .bar:nth-child(2) { height: 32px; animation-delay: 0.25s; }
  .bar:nth-child(3) { height: 48px; animation-delay: 0.4s; }
  .bar:nth-child(4) { height: 28px; animation-delay: 0.15s; }
  .bar:nth-child(5) { height: 42px; animation-delay: 0.35s; }
  .bar:nth-child(6) { height: 20px; animation-delay: 0.5s; }
  .bar:nth-child(7) { height: 36px; animation-delay: 0.2s; }

  @keyframes waveBounce {
    0% { transform: scaleY(0.4); }
    100% { transform: scaleY(1.3); }
  }

  .caption {
    margin-top: 20px;
    font-size: 13px;
    letter-spacing: 2px;
    color: #64748b;
    text-transform: uppercase;
  }
</style>
</head>
<body>
  <div class="glow-mesh"></div>

  <div class="ring-container">
    <div class="ring ring-1"></div>
    <div class="ring ring-2"></div>
    <div class="ring ring-3"></div>
    <div class="core-badge">
      <div class="badge-tag">STUDIO MOTION</div>
      <div class="badge-title">FUTURE</div>
    </div>
  </div>

  <div class="wave-bars">
    <div class="bar"></div>
    <div class="bar"></div>
    <div class="bar"></div>
    <div class="bar"></div>
    <div class="bar"></div>
    <div class="bar"></div>
    <div class="bar"></div>
  </div>

  <div class="caption">High Fidelity 60FPS Render</div>
</body>
</html>
""".trimIndent()
        ),
        MotionTemplate(
            id = "kinetic_typography",
            name = "Kinetic Typography",
            category = "Typography",
            description = "Bold, modern staggered kinetic text with split reveal and graphic accent lines",
            recommendedDurationSec = 3.5f,
            defaultPreset = AspectRatioPreset.LANDSCAPE_16_9,
            htmlCode = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: #09090b;
    color: #ffffff;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    height: 100vh;
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 0 10vw;
    overflow: hidden;
    position: relative;
  }

  .accent-line {
    position: absolute;
    left: 0;
    top: 50%;
    height: 4px;
    background: linear-gradient(90deg, #00e5ff, #ec4899);
    animation: expandLine 3.5s cubic-bezier(0.16, 1, 0.3, 1) infinite;
  }

  @keyframes expandLine {
    0% { width: 0%; opacity: 0; }
    30% { width: 100%; opacity: 1; }
    80% { width: 100%; opacity: 0.8; }
    100% { width: 0%; opacity: 0; }
  }

  .word-row {
    overflow: hidden;
    margin: 8px 0;
  }

  .word {
    font-size: 9vw;
    font-weight: 900;
    line-height: 0.95;
    text-transform: uppercase;
    letter-spacing: -2px;
    display: inline-block;
    animation: slideUp 3.5s cubic-bezier(0.16, 1, 0.3, 1) infinite;
  }

  .w1 { color: #f43f5e; animation-delay: 0.1s; }
  .w2 { color: #ffffff; animation-delay: 0.25s; }
  .w3 {
    background: linear-gradient(90deg, #00e5ff, #a855f7);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    animation-delay: 0.4s;
  }

  @keyframes slideUp {
    0% { transform: translateY(120%) rotate(4deg); opacity: 0; }
    25% { transform: translateY(0) rotate(0deg); opacity: 1; }
    75% { transform: translateY(0) rotate(0deg); opacity: 1; }
    100% { transform: translateY(-120%) rotate(-4deg); opacity: 0; }
  }

  .sub-tag {
    margin-top: 24px;
    font-size: 16px;
    letter-spacing: 6px;
    color: #71717a;
    font-weight: 700;
    text-transform: uppercase;
    animation: fadeIn 3.5s ease-in-out infinite;
  }

  @keyframes fadeIn {
    0% { opacity: 0; }
    30% { opacity: 1; }
    80% { opacity: 1; }
    100% { opacity: 0; }
  }
</style>
</head>
<body>
  <div class="accent-line"></div>
  <div class="word-row"><span class="word w1">DESIGN</span></div>
  <div class="word-row"><span class="word w2">WITHOUT</span></div>
  <div class="word-row"><span class="word w3">LIMITS.</span></div>
  <div class="sub-tag">MotionForge Studio — 4K Ready</div>
</body>
</html>
""".trimIndent()
        ),
        MotionTemplate(
            id = "glass_card_3d",
            name = "3D Glassmorphic Card",
            category = "3D & UI",
            description = "Floating frosted glass card with 3D tilt, shimmering borders, and live metrics",
            recommendedDurationSec = 4.0f,
            defaultPreset = AspectRatioPreset.SQUARE_1_1,
            htmlCode = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: radial-gradient(circle at 50% 20%, #1e1b4b, #09090b);
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    perspective: 1000px;
    overflow: hidden;
  }

  .card-3d {
    width: 320px;
    padding: 30px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 28px;
    backdrop-filter: blur(20px);
    border: 1px solid rgba(255, 255, 255, 0.15);
    box-shadow: 0 30px 60px rgba(0, 0, 0, 0.6), inset 0 1px 0 rgba(255, 255, 255, 0.3);
    animation: tilt3D 4s ease-in-out infinite alternate;
    position: relative;
    transform-style: preserve-3d;
  }

  @keyframes tilt3D {
    0% { transform: rotateY(-18deg) rotateX(12deg) translateY(10px); }
    100% { transform: rotateY(18deg) rotateX(-12deg) translateY(-10px); }
  }

  .card-chip {
    width: 44px;
    height: 32px;
    background: linear-gradient(135deg, #f59e0b, #d97706);
    border-radius: 8px;
    margin-bottom: 24px;
    box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4);
  }

  .card-number {
    color: #e2e8f0;
    font-size: 20px;
    font-family: monospace;
    letter-spacing: 3px;
    margin-bottom: 20px;
  }

  .card-details {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
  }

  .label {
    font-size: 9px;
    letter-spacing: 2px;
    color: #94a3b8;
    text-transform: uppercase;
  }

  .val {
    font-size: 15px;
    font-weight: 700;
    color: #ffffff;
    margin-top: 4px;
  }

  .glow-orb {
    position: absolute;
    width: 140px;
    height: 140px;
    border-radius: 50%;
    background: #00e5ff;
    filter: blur(50px);
    opacity: 0.35;
    top: -30px;
    right: -30px;
    animation: orbMove 4s ease-in-out infinite alternate;
  }

  @keyframes orbMove {
    0% { transform: translate(0, 0); }
    100% { transform: translate(-40px, 40px); }
  }
</style>
</head>
<body>
  <div class="glow-orb"></div>
  <div class="card-3d">
    <div class="card-chip"></div>
    <div class="card-number">4029 •••• •••• 8820</div>
    <div class="card-details">
      <div>
        <div class="label">Card Holder</div>
        <div class="val">ALEX MORGAN</div>
      </div>
      <div>
        <div class="label">Expires</div>
        <div class="val">12/29</div>
      </div>
    </div>
  </div>
</body>
</html>
""".trimIndent()
        ),
        MotionTemplate(
            id = "canvas_starfield",
            name = "Canvas Particle Warp",
            category = "Canvas & WebGL",
            description = "Interactive HTML5 canvas particle acceleration with virtual frame timing hook",
            recommendedDurationSec = 4.0f,
            defaultPreset = AspectRatioPreset.PORTRAIT_9_16,
            htmlCode = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: #030712;
    overflow: hidden;
    height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: sans-serif;
  }
  canvas {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
  }
  .title-overlay {
    position: relative;
    z-index: 10;
    text-align: center;
    color: white;
    text-shadow: 0 0 20px rgba(0,229,255,0.8);
  }
  .title-overlay h1 {
    font-size: 38px;
    font-weight: 800;
    letter-spacing: 4px;
  }
  .title-overlay p {
    font-size: 14px;
    letter-spacing: 2px;
    color: #38bdf8;
    margin-top: 8px;
  }
</style>
</head>
<body>
  <canvas id="warpCanvas"></canvas>
  <div class="title-overlay">
    <h1>COSMIC VOYAGE</h1>
    <p>HTML5 CANVAS ENGINE</p>
  </div>

  <script>
    const canvas = document.getElementById('warpCanvas');
    const ctx = canvas.getContext('2d');
    let width, height;

    function resize() {
      width = canvas.width = window.innerWidth;
      height = canvas.height = window.innerHeight;
    }
    window.addEventListener('resize', resize);
    resize();

    const STARS_COUNT = 150;
    const stars = [];
    for (let i = 0; i < STARS_COUNT; i++) {
      stars.push({
        x: (Math.random() - 0.5) * 2000,
        y: (Math.random() - 0.5) * 2000,
        z: Math.random() * 1000,
        color: ['#00e5ff', '#a855f7', '#ffffff', '#38bdf8'][Math.floor(Math.random()*4)]
      });
    }

    function renderAtTime(timeMs) {
      ctx.fillStyle = '#030712';
      ctx.fillRect(0, 0, width, height);

      const cx = width / 2;
      const cy = height / 2;
      const speed = 1.2;

      for (let s of stars) {
        // Deterministic position based on timeMs
        let currentZ = (s.z - (timeMs * speed) % 1000 + 1000) % 1000;
        if (currentZ <= 0) currentZ = 999;

        const k = 400 / currentZ;
        const px = s.x * k + cx;
        const py = s.y * k + cy;

        if (px >= 0 && px <= width && py >= 0 && py <= height) {
          const size = Math.max(1, (1 - currentZ / 1000) * 4);
          ctx.beginPath();
          ctx.arc(px, py, size, 0, Math.PI * 2);
          ctx.fillStyle = s.color;
          ctx.shadowBlur = size * 3;
          ctx.shadowColor = s.color;
          ctx.fill();
        }
      }
    }

    // Hook for MotionForge deterministic frame exporter
    window.onRenderFrame = function(timeMs) {
      renderAtTime(timeMs);
    };

    // Live preview fallback loop
    function loop(t) {
      renderAtTime(t);
      requestAnimationFrame(loop);
    }
    requestAnimationFrame(loop);
  </script>
</body>
</html>
""".trimIndent()
        ),
        MotionTemplate(
            id = "custom_starter",
            name = "Custom Starter Template",
            category = "Boilerplate",
            description = "Clean modern starter template ready for your custom HTML & CSS animations",
            recommendedDurationSec = 3.0f,
            defaultPreset = AspectRatioPreset.PORTRAIT_9_16,
            htmlCode = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>My Motion</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: #0b0f19;
    color: #f8fafc;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }

  .box {
    width: 140px;
    height: 140px;
    background: linear-gradient(135deg, #00e5ff, #a855f7);
    border-radius: 30px;
    box-shadow: 0 20px 40px rgba(0, 229, 255, 0.35);
    animation: pulseSpin 3s ease-in-out infinite;
  }

  @keyframes pulseSpin {
    0% { transform: scale(0.9) rotate(0deg); }
    50% { transform: scale(1.15) rotate(180deg); border-radius: 50%; }
    100% { transform: scale(0.9) rotate(360deg); }
  }

  h1 {
    margin-top: 40px;
    font-size: 28px;
    font-weight: 800;
    letter-spacing: -0.5px;
  }

  p {
    margin-top: 8px;
    color: #94a3b8;
    font-size: 14px;
  }
</style>
</head>
<body>
  <div class="box"></div>
  <h1>Paste Your Code Here</h1>
  <p>MotionForge converts any HTML/CSS to MP4</p>
</body>
</html>
""".trimIndent()
        )
    )
}
