package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ExportedVideoItem
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.InkBlack
import com.example.ui.theme.ShadowElevated
import com.example.ui.theme.SurfaceCardWhite
import com.example.ui.theme.SurfaceSubtlePill
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.VideoExportManager

@Composable
fun VideoPlayerDialog(
    item: ExportedVideoItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isSavedToGallery by remember { mutableStateOf(false) }
    val sizeMb = item.file.length() / (1024f * 1024f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .shadow(28.dp, RoundedCornerShape(28.dp), spotColor = ShadowElevated)
                .testTag("video_player_modal"),
            shape = RoundedCornerShape(28.dp),
            color = SurfaceCardWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top header with title and close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "${item.resolution} • ${"%.1f".format(sizeMb)} MB • MP4",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceSubtlePill)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = InkBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                val (vidWidth, vidHeight) = remember(item.resolution) {
                    val parts = item.resolution.split("×", "x", "*")
                    if (parts.size == 2) {
                        val w = parts[0].trim().toFloatOrNull() ?: 1080f
                        val h = parts[1].trim().toFloatOrNull() ?: 1920f
                        Pair(w, h)
                    } else {
                        Pair(9f, 16f)
                    }
                }
                val videoAspectRatio = (vidWidth / vidHeight).coerceIn(0.4f, 2.5f)

                // Video Surface with accurate aspect ratio (prevents squashing or stretching)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 420.dp)
                        .aspectRatio(videoAspectRatio, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(20.dp))
                        .shadow(12.dp, RoundedCornerShape(20.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(videoAspectRatio, matchHeightConstraintsFirst = true),
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val uri = Uri.fromFile(item.file)
                                setVideoURI(uri)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        }
                    )
                }

                // Primary Black Pill Button: Save to Gallery
                Button(
                    onClick = {
                        val uri = VideoExportManager.saveVideoToGallery(context, item.file)
                        if (uri != null) {
                            isSavedToGallery = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("modal_download_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSavedToGallery) AccentSuccess else InkBlack,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isSavedToGallery) Icons.Default.Check else Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSavedToGallery) "Saved to Device Photos!" else "Save to Gallery",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Secondary Row: Share & Open External
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { VideoExportManager.shareVideo(context, item.file) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("modal_share_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceSubtlePill,
                            contentColor = InkBlack
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = InkBlack
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { VideoExportManager.openVideoInSystemPlayer(context, item.file) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("modal_open_external_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceSubtlePill,
                            contentColor = InkBlack
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = InkBlack
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Player", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
