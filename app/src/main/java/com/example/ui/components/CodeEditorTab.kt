package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundCanvas
import com.example.ui.theme.BorderLightSubtle
import com.example.ui.theme.InkBlack
import com.example.ui.theme.ShadowSoftAmbient
import com.example.ui.theme.SurfaceCardWhite
import com.example.ui.theme.SurfaceSubtlePill
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CodeEditorTab(
    htmlCode: String,
    hasChangesToReset: Boolean,
    onCodeChange: (String) -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenAiEditor: () -> Unit,
    onResetAllChanges: () -> Unit,
    onApplyAndPreview: () -> Unit
) {
    val context = LocalContext.current
    var localCode by remember(htmlCode) { mutableStateOf(htmlCode) }

    val snippets = listOf(
        "@keyframes" to """
@keyframes pulse {
  0% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.15); opacity: 1; }
  100% { transform: scale(1); opacity: 0.8; }
}
""".trimIndent(),
        "3D Transform" to """
transform: perspective(1000px) rotateY(20deg) rotateX(10deg);
transform-style: preserve-3d;
""".trimIndent(),
        "Waveform" to """
animation: soundWave 1.2s ease-in-out infinite alternate;
""".trimIndent(),
        "Frame Hook" to """
<script>
window.onRenderFrame = function(timeMs) {
  // Deterministic frame update
};
</script>
""".trimIndent()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Toolbar: Paste, Presets, AI Edit, Reset, Run
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Paste Pill Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                if (pasted.isNotEmpty()) {
                                    localCode = pasted
                                    onCodeChange(pasted)
                                }
                            }
                        }
                        .testTag("paste_code_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCardWhite,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = null,
                            tint = InkBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = InkBlack)
                    }
                }

                // AI Motion Editor Pill Button (Gemini)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenAiEditor() }
                        .testTag("ai_editor_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCardWhite,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = InkBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Edit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = InkBlack)
                    }
                }

                // Presets Pill Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenTemplates() },
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCardWhite,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shuffle,
                            contentDescription = null,
                            tint = InkBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Presets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = InkBlack)
                    }
                }

                // Reset All Changes Button (User requested one-tap reset)
                if (hasChangesToReset) {
                    IconButton(
                        onClick = onResetAllChanges,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("reset_changes_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Changes",
                            tint = InkBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Clear
                IconButton(
                    onClick = {
                        localCode = ""
                        onCodeChange("")
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("clear_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Clear",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Run Preview Black Pill Button
            Button(
                onClick = {
                    onCodeChange(localCode)
                    onApplyAndPreview()
                },
                modifier = Modifier
                    .height(38.dp)
                    .testTag("apply_code_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = InkBlack,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Quick Snippets Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            snippets.forEach { (label, snippet) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceSubtlePill)
                        .clickable {
                            localCode += "\n" + snippet
                            onCodeChange(localCode)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("+ $label", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                }
            }
        }

        // Clean White Monospace Code Editor Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = ShadowSoftAmbient),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCardWhite
        ) {
            OutlinedTextField(
                value = localCode,
                onValueChange = {
                    localCode = it
                    onCodeChange(it)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("html_code_input")
                    .padding(8.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = InkBlack
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = InkBlack
                ),
                placeholder = {
                    Text(
                        text = "Paste or type your HTML, CSS & JavaScript animation code here...",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            )
        }

        // Bottom status line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${localCode.lines().size} lines • ${localCode.length} characters",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Text(
                text = "HTML • CSS • JS",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkBlack
            )
        }
    }
}
