package com.example.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.AnimationTimelineHarness
import com.example.model.AspectRatioPreset
import com.example.model.FpsOption
import com.example.ui.theme.BackgroundCanvas
import com.example.ui.theme.InkBlack
import com.example.ui.theme.ShadowElevated
import com.example.ui.theme.ShadowSoftAmbient
import com.example.ui.theme.SurfaceCardWhite
import com.example.ui.theme.SurfaceSubtlePill
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewTab(
    title: String,
    htmlCode: String,
    preset: AspectRatioPreset,
    fps: FpsOption,
    durationSec: Float,
    previewKey: Int,
    autoCleanControls: Boolean,
    onPresetChange: (AspectRatioPreset) -> Unit,
    onRefresh: () -> Unit,
    onOpenTemplates: () -> Unit,
    onStartRender: (WebView?) -> Unit
) {
    val scrollState = rememberScrollState()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPlaying by remember { mutableStateOf(true) }

    fun resumeWebViewPlay() {
        webViewRef?.evaluateJavascript(
            "if (window.__mf) window.__mf.play(); else if (window.__resumeLivePlay) window.__resumeLivePlay();",
            null
        )
    }

    fun pauseWebViewPlay() {
        webViewRef?.evaluateJavascript(
            "if (window.__mf) window.__mf.pause(); else if (window.__pauseLivePlay) window.__pauseLivePlay();",
            null
        )
    }

    // Unified HTML wrapper with deterministic AnimationTimelineHarness and exact artboard matching export
    val displayHtml = remember(htmlCode, autoCleanControls, previewKey, durationSec, preset) {
        if (htmlCode.isBlank()) ""
        else {
            AnimationTimelineHarness.buildHtml(
                rawCode = htmlCode,
                cleanControls = autoCleanControls,
                durationSec = durationSec,
                isExport = false,
                targetWidth = preset.width,
                targetHeight = preset.height
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Aspect Ratio Selector Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceSubtlePill,
                modifier = Modifier.padding(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AspectRatioPreset.values().forEach { item ->
                        val isSelected = item == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) SurfaceCardWhite else Color.Transparent)
                                .clickable { onPresetChange(item) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.shadow(2.dp, RoundedCornerShape(16.dp), spotColor = ShadowSoftAmbient)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.ratioTag,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) InkBlack else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // The Large Elevated Artwork / Video Preview Card
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val aspect = preset.width.toFloat() / preset.height.toFloat()
            val cardHeight = when (preset) {
                AspectRatioPreset.PORTRAIT_9_16 -> 380.dp
                AspectRatioPreset.PORTRAIT_4_5 -> 350.dp
                AspectRatioPreset.SQUARE_1_1 -> 310.dp
                AspectRatioPreset.LANDSCAPE_16_9 -> 200.dp
            }

            Surface(
                modifier = Modifier
                    .height(cardHeight)
                    .aspectRatio(aspect, matchHeightConstraintsFirst = true)
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = ShadowElevated,
                        ambientColor = ShadowSoftAmbient
                    ),
                shape = RoundedCornerShape(32.dp),
                color = SurfaceCardWhite
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp))
                        .background(if (htmlCode.isBlank()) SurfaceSubtlePill else Color(0xFF0F1218)),
                    contentAlignment = Alignment.Center
                ) {
                    if (htmlCode.isBlank()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Ready for Your Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Paste your HTML/CSS in the Code tab or choose a Preset.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        androidx.compose.runtime.key(previewKey, displayHtml) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        webViewRef = this
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                            textZoom = 100
                                            cacheMode = WebSettings.LOAD_NO_CACHE
                                            mediaPlaybackRequiresUserGesture = false
                                            minimumFontSize = 1
                                            minimumLogicalFontSize = 1
                                        }
                                        webChromeClient = WebChromeClient()
                                        webViewClient = WebViewClient()
                                        loadDataWithBaseURL("https://motionforge.studio/", displayHtml, "text/html", "UTF-8", null)
                                    }
                                },
                                update = { view ->
                                    webViewRef = view
                                },
                                onRelease = { view ->
                                    try {
                                        view.destroy()
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            )
                        }

                        // Watermark resolution badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(14.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${preset.width} × ${preset.height}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title and Subtitle Row with Direct Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onOpenTemplates,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Templates",
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title.ifBlank { "Motion Project" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        fontSize = 24.sp
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Motion Graphics • ${"%.1f".format(durationSec)}s • H.264 MP4",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            IconButton(
                onClick = {
                    onRefresh()
                    isPlaying = true
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clean Direct Playback Controls Bar (Waveform timeline scrubber removed per request)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceSubtlePill
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                isPlaying = false
                                pauseWebViewPlay()
                            } else {
                                isPlaying = true
                                resumeWebViewPlay()
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceCardWhite)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = InkBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = if (isPlaying) "Playing live preview" else "Paused",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = InkBlack
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCardWhite)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${fps.fps} FPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkBlack
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCardWhite)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${"%.1f".format(durationSec)}s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkBlack
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Pill Buttons: Export MP4 & Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Solid Black Pill: Export MP4
            Button(
                onClick = { onStartRender(webViewRef) },
                enabled = htmlCode.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("preview_export_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = InkBlack,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export MP4 (${"%.0f".format(durationSec)}s)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Soft Light Gray Pill: Presets / Templates
            Button(
                onClick = onOpenTemplates,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("preview_presets_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceSubtlePill,
                    contentColor = InkBlack
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = InkBlack
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Presets",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
