package com.veylqo.recora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = Color.White,
    primaryContainer = VioletSecondary,
    onPrimaryContainer = Color.White,
    secondary = VioletGlow,
    onSecondary = Color.White,
    background = CosmosBase,
    onBackground = TextPrimary,
    surface = CosmosSurface,
    onSurface = TextPrimary,
    surfaceVariant = CosmosSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CosmosBorder,
    error = RecordingRed,
    onError = Color.White
)

@Composable
fun RecoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
