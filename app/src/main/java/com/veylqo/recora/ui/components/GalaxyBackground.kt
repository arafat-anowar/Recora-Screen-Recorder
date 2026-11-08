package com.veylqo.recora.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    val baseAlpha: Float,
    val twinklingGroup: Int
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
                    baseAlpha = 0.25f + ((index % 5) * 0.12f),
                    twinklingGroup = index % 3
                )
            )
        }
        list
    }

    val nebulaPulse1: Float
    val nebulaPulse2: Float
    val twinkleAlpha1: Float
    val twinkleAlpha2: Float

    if (reduceMotion) {
        // High performance mode: zero GPU animation redraw overhead!
        nebulaPulse1 = 0.08f
        nebulaPulse2 = 0.06f
        twinkleAlpha1 = 0.4f
        twinkleAlpha2 = 0.6f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "GalaxyMotion")
        val p1 by infiniteTransition.animateFloat(
            initialValue = 0.06f,
            targetValue = 0.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Nebula1"
        )
        val p2 by infiniteTransition.animateFloat(
            initialValue = 0.05f,
            targetValue = 0.10f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 11000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Nebula2"
        )
        val t1 by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Twinkle1"
        )
        val t2 by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Twinkle2"
        )
        nebulaPulse1 = p1
        nebulaPulse2 = p2
        twinkleAlpha1 = t1
        twinkleAlpha2 = t2
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Base Dark Navy Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(BgDarkNavyTop, BgDarkNavyCenter, BgDarkNavyBottom),
                    startY = 0f,
                    endY = height
                )
            )

            // 2. Subtle Nebula Glow 1 (Upper Left Purple)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7F5AF0).copy(alpha = nebulaPulse1),
                        Color(0xFF7F5AF0).copy(alpha = nebulaPulse1 * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.25f, height * 0.22f),
                    radius = width * 0.75f
                ),
                radius = width * 0.75f,
                center = Offset(width * 0.25f, height * 0.22f)
            )

            // 3. Subtle Nebula Glow 2 (Lower Right Indigo)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF382372).copy(alpha = nebulaPulse2),
                        Color(0xFF201344).copy(alpha = nebulaPulse2 * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.85f, height * 0.70f),
                    radius = width * 0.8f
                ),
                radius = width * 0.8f,
                center = Offset(width * 0.85f, height * 0.70f)
            )

            // 4. Very faint Pink warmth near bottom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF0446A).copy(alpha = 0.035f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.5f, height * 0.95f),
                    radius = width * 0.5f
                ),
                radius = width * 0.5f,
                center = Offset(width * 0.5f, height * 0.95f)
            )

            // 5. Dim subtle stars
            stars.forEach { star ->
                val alpha = when (star.twinklingGroup) {
                    0 -> star.baseAlpha
                    1 -> if (reduceMotion) star.baseAlpha else twinkleAlpha1 * star.baseAlpha
                    else -> if (reduceMotion) star.baseAlpha else twinkleAlpha2 * star.baseAlpha
                }.coerceIn(0.1f, 0.9f)

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = star.radius,
                    center = Offset(star.xFraction * width, star.yFraction * height)
                )
            }
        }

        // Screen content on top of background
        content()
    }
}
