package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.RenderPhase
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.InkBlack
import com.example.ui.theme.ShadowElevated
import com.example.ui.theme.SurfaceCardWhite
import com.example.ui.theme.SurfaceSubtlePill
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RenderProgressDialog(
    renderPhase: RenderPhase,
    onCancel: () -> Unit
) {
    if (renderPhase is RenderPhase.Idle || renderPhase is RenderPhase.Success) return

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .shadow(28.dp, RoundedCornerShape(28.dp), spotColor = ShadowElevated)
                .testTag("render_progress_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = SurfaceCardWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon indicator
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = SurfaceSubtlePill
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = InkBlack,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Text(
                    text = when (renderPhase) {
                        is RenderPhase.Initializing -> "Preparing Encoder"
                        is RenderPhase.Rendering -> "Rendering Video"
                        is RenderPhase.Finalizing -> "Saving MP4 Stream"
                        is RenderPhase.Error -> "Encoding Error"
                        else -> "Processing"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = TextPrimary
                )

                when (renderPhase) {
                    is RenderPhase.Initializing -> {
                        CircularProgressIndicator(
                            color = InkBlack,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Configuring hardware MediaCodec...",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    is RenderPhase.Rendering -> {
                        // Live Thumbnail Preview Frame
                        if (renderPhase.currentBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .size(width = 160.dp, height = 210.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .shadow(8.dp, RoundedCornerShape(16.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = renderPhase.currentBitmap.asImageBitmap(),
                                    contentDescription = "Live Render Preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Progress Bar & Percentage
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { renderPhase.percent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = InkBlack,
                                trackColor = SurfaceSubtlePill
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Frame ${renderPhase.currentFrame} of ${renderPhase.totalFrames}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${(renderPhase.percent * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InkBlack
                                )
                            }
                        }

                        // Speed and remaining time pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceSubtlePill)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = InkBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${"%.1f".format(renderPhase.speedFps)} FPS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = InkBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val remainingFrames = renderPhase.totalFrames - renderPhase.currentFrame
                                val remainingSec = if (renderPhase.speedFps > 0) remainingFrames / renderPhase.speedFps else 0f
                                Text(
                                    text = "${"%.1f".format(remainingSec)}s left",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    is RenderPhase.Finalizing -> {
                        CircularProgressIndicator(
                            color = InkBlack,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Writing MP4 container...",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    is RenderPhase.Error -> {
                        Text(
                            text = renderPhase.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    else -> {}
                }

                // Cancel Button
                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("cancel_render_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceSubtlePill,
                        contentColor = InkBlack
                    )
                ) {
                    Text(
                        text = if (renderPhase is RenderPhase.Error) "Dismiss" else "Cancel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
