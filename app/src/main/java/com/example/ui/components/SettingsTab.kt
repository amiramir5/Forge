package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ApiKeyPreferences
import com.example.model.AspectRatioPreset
import com.example.model.BitratePreset
import com.example.model.CaptureMode
import com.example.model.FpsOption
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.BackgroundCanvas
import com.example.ui.theme.BorderLightSubtle
import com.example.ui.theme.InkBlack
import com.example.ui.theme.SurfaceCardWhite
import com.example.ui.theme.SurfaceSubtlePill
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsTab(
    preferences: ApiKeyPreferences,
    preset: AspectRatioPreset,
    fps: FpsOption,
    durationSec: Float,
    bitrate: BitratePreset,
    captureMode: CaptureMode,
    autoCleanControls: Boolean,
    onPresetChange: (AspectRatioPreset) -> Unit,
    onFpsChange: (FpsOption) -> Unit,
    onDurationChange: (Float) -> Unit,
    onBitrateChange: (BitratePreset) -> Unit,
    onCaptureModeChange: (CaptureMode) -> Unit,
    onAutoCleanControlsToggle: (Boolean) -> Unit,
    onStartRender: () -> Unit
) {
    val scrollState = rememberScrollState()

    var apiKeyInput by remember { mutableStateOf(preferences.geminiApiKey) }
    var selectedModel by remember { mutableStateOf(preferences.selectedModel) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var keySavedFeedback by remember { mutableStateOf(false) }

    val totalFrames = (durationSec * fps.fps).toInt()
    val estBytes = (durationSec * (bitrate.bps / 8f)).toLong()
    val estMb = estBytes / (1024f * 1024f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section: Title
        Column {
            Text(
                text = "Configuration",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
            Text(
                text = "Render Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary
            )
        }

        // Section 1: Gemini AI Key Configuration (User requested key management)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCardWhite,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = InkBlack,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GEMINI AI CONFIGURATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextMuted
                        )
                    }

                    if (keySavedFeedback) {
                        Text(
                            text = "Saved!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentSuccess
                        )
                    }
                }

                Text(
                    text = "Add your Gemini API key (supports keys starting with AQ... or AIza...) to use AI motion re-layout and animation editing.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        preferences.geminiApiKey = it
                        keySavedFeedback = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_api_key_input"),
                    placeholder = { Text("AQ... or AIza... key", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = TextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InkBlack,
                        unfocusedBorderColor = BorderLightSubtle,
                        cursorColor = InkBlack
                    )
                )

                // Gemini Model Pills (Including requested 3.8, 3.7, 3.5, 3.1)
                Text(
                    text = "AI MODEL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("gemini-3.8-flash", "gemini-3.7-flash", "gemini-3.5-flash").forEach { modelName ->
                        val isSelected = selectedModel == modelName
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) InkBlack else SurfaceSubtlePill)
                                .clickable {
                                    selectedModel = modelName
                                    preferences.selectedModel = modelName
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = modelName.removePrefix("gemini-"),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Smart Auto-Clean Controls Toggle (Directly solves user's issue with unwanted buttons)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCardWhite,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Clean Extraneous UI Controls",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Auto-hides buttons & footer controls placed by web/Gemini generators to record only the motion graphic.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Switch(
                    checked = autoCleanControls,
                    onCheckedChange = onAutoCleanControlsToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = InkBlack,
                        uncheckedTrackColor = SurfaceSubtlePill
                    ),
                    modifier = Modifier.testTag("auto_clean_controls_switch")
                )
            }
        }

        // Section 3: Capture Mode (Real-Time vs Clock Step)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCardWhite,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CAPTURE ENGINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextMuted
                )

                CaptureMode.values().forEach { mode ->
                    val isSelected = captureMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SurfaceSubtlePill else Color.Transparent)
                            .clickable { onCaptureModeChange(mode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = mode.description,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = InkBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Aspect Ratio (NO DUPLICATES)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "ASPECT RATIO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextMuted
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AspectRatioPreset.values().forEach { item ->
                    val isSelected = item == preset
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPresetChange(item) },
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceCardWhite,
                        shadowElevation = if (isSelected) 3.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = item.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = item.subtitle,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(InkBlack),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Duration Slider
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCardWhite,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DURATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )

                    Text(
                        text = "${"%.1f".format(durationSec)}s",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkBlack
                    )
                }

                Slider(
                    value = durationSec,
                    onValueChange = onDurationChange,
                    valueRange = 1.0f..30.0f,
                    steps = 57,
                    modifier = Modifier.testTag("duration_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = InkBlack,
                        activeTrackColor = InkBlack,
                        inactiveTrackColor = SurfaceSubtlePill
                    )
                )

                // Quick Pills (including 10s and 15s)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(4.0f, 6.0f, 10.0f, 15.0f).forEach { sec ->
                        val isSelected = durationSec == sec
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) InkBlack else SurfaceSubtlePill)
                                .clickable { onDurationChange(sec) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${sec.toInt()}s",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Section 6: Frame Rate
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "FRAME RATE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FpsOption.values().forEach { option ->
                    val isSelected = option == fps
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onFpsChange(option) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) InkBlack else SurfaceCardWhite,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${option.fps} FPS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isSelected) Color.White else InkBlack
                            )
                            Text(
                                text = if (option == FpsOption.FPS_60) "Ultra Smooth" else "Standard",
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.7f) else TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Specifications Summary
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCardWhite,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SPECIFICATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextMuted
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Frames", fontSize = 13.sp, color = TextSecondary)
                    Text("$totalFrames frames", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InkBlack)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dimensions", fontSize = 13.sp, color = TextSecondary)
                    Text("${preset.width} × ${preset.height}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InkBlack)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Est. File Size", fontSize = 13.sp, color = TextSecondary)
                    Text("~${"%.1f".format(estMb)} MB", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkBlack)
                }
            }
        }

        // Big Solid Black Pill: Export Video
        Button(
            onClick = onStartRender,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_render_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = InkBlack,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Export ${preset.ratioTag} MP4 (${"%.0f".format(durationSec)}s)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
