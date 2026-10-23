package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BorderLightSubtle
import com.example.ui.theme.InkBlack
import com.example.ui.theme.ShadowElevated
import com.example.ui.theme.SurfaceCardWhite
import com.example.ui.theme.SurfaceSubtlePill
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class AiTaskMode(val id: String, val label: String, val subtitle: String) {
    EDIT("EDIT", "Edit & Reposition", "Move objects, adjust layout, colors or timing"),
    GENERATE("GENERATE", "Generate Motion", "Create a fresh animation from text prompt"),
    FIX("FIX", "Fix & Smooth Animate", "Add fluid keyframes, remove buttons, fix errors")
}

@Composable
fun AiMotionEditorDialog(
    modelName: String,
    isThinking: Boolean,
    errorMessage: String?,
    onExecuteTask: (taskType: String, prompt: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(AiTaskMode.EDIT) }
    var instruction by remember { mutableStateOf("") }

    val quickPrompts = when (selectedMode) {
        AiTaskMode.EDIT -> listOf(
            "Move box to center",
            "Make animation 10 seconds",
            "Remove bottom buttons & fix layout",
            "Change colors to luxury black & gold",
            "Add smooth pulse glow effect"
        )
        AiTaskMode.GENERATE -> listOf(
            "Glowing neon cyber pulse ring for 10s story",
            "Clean Apple-style typographic kinetic title",
            "Fluid 3D floating glass card with smooth drift",
            "Dynamic audio frequency visualizer wave"
        )
        AiTaskMode.FIX -> listOf(
            "Make all elements move smoothly with 10s loop",
            "Remove extraneous web buttons and center artboard",
            "Fix static positioning and add cubic-bezier motion"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .shadow(28.dp, RoundedCornerShape(28.dp), spotColor = ShadowElevated)
                .testTag("ai_motion_editor_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = SurfaceCardWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = InkBlack
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gemini Motion Studio",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "Model: $modelName • Auto Fallback Enabled",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                        }
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

                // Mode Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AiTaskMode.values().forEach { mode ->
                        val isSelected = selectedMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) InkBlack else SurfaceSubtlePill)
                                .clickable {
                                    selectedMode = mode
                                    if (mode == AiTaskMode.FIX && instruction.isBlank()) {
                                        instruction = "Make animation fluid and remove extraneous buttons"
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }

                Text(
                    text = selectedMode.subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // Quick Prompt Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPrompts.forEach { prompt ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceSubtlePill)
                                .clickable { instruction = prompt }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(prompt, fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }

                // Instruction Input
                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                        .testTag("ai_instruction_input"),
                    placeholder = {
                        Text(
                            text = when (selectedMode) {
                                AiTaskMode.EDIT -> "e.g., Move the square to center, make duration 10s, remove buttons..."
                                AiTaskMode.GENERATE -> "e.g., Vertical story 10s with neon particles and spinning geometric portal..."
                                AiTaskMode.FIX -> "e.g., Analyze and fix animation so elements move smoothly at 60 FPS..."
                            },
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InkBlack,
                        unfocusedBorderColor = BorderLightSubtle,
                        cursorColor = InkBlack
                    )
                )

                // Error message if any
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceSubtlePill,
                            contentColor = InkBlack
                        )
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { onExecuteTask(selectedMode.id, instruction) },
                        enabled = (instruction.isNotBlank() || selectedMode == AiTaskMode.FIX) && !isThinking,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InkBlack,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("ai_apply_button")
                    ) {
                        if (isThinking) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing Motion...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (selectedMode) {
                                    AiTaskMode.EDIT -> "Apply Changes"
                                    AiTaskMode.GENERATE -> "Generate Motion"
                                    AiTaskMode.FIX -> "Fix & Animate"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
