package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MinimalistStudioColorScheme = lightColorScheme(
    primary = InkBlack,
    onPrimary = Color.White,
    primaryContainer = InkCharcoal,
    onPrimaryContainer = Color.White,
    secondary = SurfaceSubtlePill,
    onSecondary = InkBlack,
    secondaryContainer = SurfaceSubtlePill,
    onSecondaryContainer = InkBlack,
    tertiary = InkCharcoal,
    onTertiary = Color.White,
    background = BackgroundCanvas,
    onBackground = TextPrimary,
    surface = SurfaceCardWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSubtlePill,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceCardWhite,
    outline = BorderLightSubtle,
    outlineVariant = BorderDivider,
    error = AccentRed,
    onError = Color.White
)

@Composable
fun MotionForgeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MinimalistStudioColorScheme,
        typography = Typography,
        content = content
    )
}
