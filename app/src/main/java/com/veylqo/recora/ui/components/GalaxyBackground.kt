package com.veylqo.recora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.veylqo.recora.ui.theme.BgDarkNavyBottom
import com.veylqo.recora.ui.theme.BgDarkNavyCenter
import com.veylqo.recora.ui.theme.BgDarkNavyTop

private data class Star(
    val xFraction: Float,
    val yFraction: Float,
    val radius: Float,
    val alpha: Float
)

@Composable
fun GalaxyBackground(
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    // Generate deterministic star positions once
    val stars = remember {
        val list = mutableListOf<Star>()
        val starCoords = listOf(
            Triple(0.12f, 0.08f, 1.2f), Triple(0.28f, 0.15f, 1.5f), Triple(0.85f, 0.12f, 1.0f),
            Triple(0.65f, 0.05f, 1.8f), Triple(0.42f, 0.22f, 0.9f), Triple(0.92f, 0.25f, 1.4f),
            Triple(0.08f, 0.32f, 1.1f), Triple(0.78f, 0.38f, 1.6f), Triple(0.22f, 0.45f, 1.3f),
            Triple(0.58f, 0.48f, 1.0f), Triple(0.90f, 0.52f, 1.2f), Triple(0.15f, 0.62f, 1.5f),
            Triple(0.38f, 0.68f, 1.1f), Triple(0.72f, 0.72f, 1.4f), Triple(0.88f, 0.80f, 1.7f),
            Triple(0.05f, 0.85f, 0.9f), Triple(0.30f, 0.88f, 1.3f), Triple(0.60f, 0.92f, 1.1f),
            Triple(0.82f, 0.95f, 1.5f), Triple(0.48f, 0.12f, 1.2f), Triple(0.18f, 0.24f, 1.4f),
            Triple(0.55f, 0.35f, 1.0f), Triple(0.32f, 0.55f, 1.6f), Triple(0.80f, 0.60f, 1.1f),
            Triple(0.45f, 0.78f, 1.3f), Triple(0.68f, 0.84f, 1.2f), Triple(0.25f, 0.96f, 1.5f),
            Triple(0.95f, 0.08f, 0.8f), Triple(0.10f, 0.50f, 1.2f), Triple(0.87f, 0.42f, 1.3f)
        )
        starCoords.forEachIndexed { index, (x, y, r) ->
            list.add(
                Star(
                    xFraction = x,
                    yFraction = y,
                    radius = r,
                    alpha = 0.35f + ((index % 4) * 0.12f)
                )
            )
        }
        list
    }

    val bgGradient = remember {
        Brush.verticalGradient(
            colors = listOf(BgDarkNavyTop, BgDarkNavyCenter, BgDarkNavyBottom)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .drawBehind {
                val width = size.width
                val height = size.height

                // 1. Subtle static ambient nebula glow behind hero area (Zero per-frame shader rebuilds)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7F5AF0).copy(alpha = 0.09f),
                            Color(0xFF382372).copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.30f),
                        radius = width * 0.85f
                    ),
                    radius = width * 0.85f,
                    center = Offset(width * 0.5f, height * 0.30f)
                )

                // 2. Crisp subtle stars (drawn cleanly without frame loops)
                for (star in stars) {
                    drawCircle(
                        color = Color.White.copy(alpha = star.alpha),
                        radius = star.radius,
                        center = Offset(star.xFraction * width, star.yFraction * height)
                    )
                }
            }
    ) {
        content()
    }
}
