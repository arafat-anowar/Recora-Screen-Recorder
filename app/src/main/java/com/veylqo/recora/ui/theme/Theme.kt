package com.veylqo.recora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = CardBg,
    onPrimaryContainer = AccentPurpleLight,
    secondary = AccentPurpleLight,
    onSecondary = Color.White,
    background = BgDarkNavyTop,
    onBackground = TextPrimary,
    surface = CardBg,
    onSurface = TextPrimary,
    surfaceVariant = IconTileBg,
    onSurfaceVariant = TextMuted,
    outline = CardBorder
)

@Composable
fun RecoraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
