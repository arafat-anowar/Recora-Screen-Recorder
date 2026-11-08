package com.veylqo.recora.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.veylqo.recora.ui.theme.BgDarkNavyBottom
import com.veylqo.recora.ui.theme.BgDarkNavyCenter
import com.veylqo.recora.ui.theme.BgDarkNavyTop
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class Star(
    val xFraction: Float,
    val yFraction: Float,
    val radius: Float,
    val baseAlpha: Float,
    val twinkleSpeed: Float,
    val phase: Float
)

private data class CosmicDust(
    val initialX: Float,
    val initialY: Float,
    val speedX: Float,
    val speedY: Float,
    val radius: Float,
    val color: Color
)

@Composable
fun GalaxyBackground(
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    // Generate deterministic stars with independent twinkle phases
    val stars = remember {
        val list = mutableListOf<Star>()
        val starCoords = listOf(
            Triple(0.12f, 0.08f, 1.4f), Triple(0.28f, 0.15f, 1.8f), Triple(0.85f, 0.12f, 1.2f),
            Triple(0.65f, 0.05f, 2.0f), Triple(0.42f, 0.22f, 1.0f), Triple(0.92f, 0.25f, 1.6f),
            Triple(0.08f, 0.32f, 1.3f), Triple(0.78f, 0.38f, 1.9f), Triple(0.22f, 0.45f, 1.5f),
            Triple(0.58f, 0.48f, 1.1f), Triple(0.90f, 0.52f, 1.4f), Triple(0.15f, 0.62f, 1.7f),
            Triple(0.38f, 0.68f, 1.2f), Triple(0.72f, 0.72f, 1.6f), Triple(0.88f, 0.80f, 2.1f),
            Triple(0.05f, 0.85f, 1.0f), Triple(0.30f, 0.88f, 1.5f), Triple(0.60f, 0.92f, 1.3f),
            Triple(0.82f, 0.95f, 1.7f), Triple(0.48f, 0.12f, 1.3f), Triple(0.18f, 0.24f, 1.6f),
            Triple(0.55f, 0.35f, 1.1f), Triple(0.32f, 0.55f, 1.8f), Triple(0.80f, 0.60f, 1.2f),
            Triple(0.45f, 0.78f, 1.5f), Triple(0.68f, 0.84f, 1.3f), Triple(0.25f, 0.96f, 1.7f),
            Triple(0.95f, 0.08f, 0.9f), Triple(0.10f, 0.50f, 1.4f), Triple(0.87f, 0.42f, 1.5f),
            Triple(0.04f, 0.18f, 1.1f), Triple(0.50f, 0.62f, 1.6f), Triple(0.75f, 0.18f, 1.3f),
            Triple(0.35f, 0.38f, 1.4f), Triple(0.62f, 0.82f, 1.7f), Triple(0.96f, 0.70f, 1.2f)
        )
        starCoords.forEachIndexed { index, (x, y, r) ->
            list.add(
                Star(
                    xFraction = x,
                    yFraction = y,
                    radius = r,
                    baseAlpha = 0.35f + ((index % 3) * 0.15f),
                    twinkleSpeed = 0.8f + ((index % 5) * 0.3f),
                    phase = (index * 0.73f) % (2f * PI.toFloat())
                )
            )
        }
        list
    }

    // Floating cosmic particles
    val cosmicParticles = remember {
        listOf(
            CosmicDust(0.15f, 0.75f, 0.015f, -0.025f, 2.5f, Color(0xFF9D72FF).copy(alpha = 0.45f)),
            CosmicDust(0.82f, 0.65f, -0.012f, -0.020f, 3.2f, Color(0xFFF0446A).copy(alpha = 0.35f)),
            CosmicDust(0.35f, 0.20f, 0.018f, -0.015f, 2.0f, Color(0xFF7F5AF0).copy(alpha = 0.50f)),
            CosmicDust(0.68f, 0.40f, -0.016f, -0.030f, 2.8f, Color(0xFF00E5FF).copy(alpha = 0.30f)),
            CosmicDust(0.52f, 0.88f, 0.010f, -0.022f, 2.2f, Color(0xFFFFD21F).copy(alpha = 0.35f))
        )
    }

    val bgGradient = remember {
        Brush.verticalGradient(
            colors = listOf(BgDarkNavyTop, BgDarkNavyCenter, BgDarkNavyBottom)
        )
    }

    // Infinite transition for slow, cinematic cosmic rotation & twinkle (Zero content recomposition)
    val infiniteTransition = rememberInfiniteTransition(label = "GalaxyAnimation")
    val cosmicProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CosmicCycle"
    )

    val nebulaPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = if (reduceMotion) 0.88f else 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NebulaPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .drawBehind {
                val width = size.width
                val height = size.height
                val time = cosmicProgress * 2f * PI.toFloat()

                // 1. Slow, organic drifting Purple Nebula Cloud
                val nebula1CenterX = width * (0.50f + cos(time) * 0.06f)
                val nebula1CenterY = height * (0.32f + sin(time) * 0.04f)
                val nebula1Radius = width * 0.82f * nebulaPulse

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7F5AF0).copy(alpha = 0.16f),
                            Color(0xFF4C2899).copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(nebula1CenterX, nebula1CenterY),
                        radius = nebula1Radius
                    ),
                    radius = nebula1Radius,
                    center = Offset(nebula1CenterX, nebula1CenterY)
                )

                // 2. Secondary Magenta / Pink Cosmic Aurora Cloud (Drifting gently in reverse)
                val nebula2CenterX = width * (0.42f - sin(time * 0.8f) * 0.08f)
                val nebula2CenterY = height * (0.48f + cos(time * 0.8f) * 0.05f)
                val nebula2Radius = width * 0.65f * (2f - nebulaPulse)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF0446A).copy(alpha = 0.09f),
                            Color(0xFF5B1745).copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(nebula2CenterX, nebula2CenterY),
                        radius = nebula2Radius
                    ),
                    radius = nebula2Radius,
                    center = Offset(nebula2CenterX, nebula2CenterY)
                )

                // 3. Twinkling Living Starfield
                for (star in stars) {
                    val twinkleAlpha = if (reduceMotion) {
                        star.baseAlpha
                    } else {
                        val wave = sin((time * star.twinkleSpeed) + star.phase)
                        (star.baseAlpha + wave * 0.30f).coerceIn(0.12f, 0.95f)
                    }

                    drawCircle(
                        color = Color.White.copy(alpha = twinkleAlpha),
                        radius = star.radius * if (!reduceMotion && twinkleAlpha > 0.65f) 1.25f else 1.0f,
                        center = Offset(star.xFraction * width, star.yFraction * height)
                    )
                }

                // 4. Floating Cosmic Dust Particles drifting across the screen
                if (!reduceMotion) {
                    for (particle in cosmicParticles) {
                        val currentX = ((particle.initialX + particle.speedX * cosmicProgress) % 1.0f + 1.0f) % 1.0f
                        val currentY = ((particle.initialY + particle.speedY * cosmicProgress) % 1.0f + 1.0f) % 1.0f

                        drawCircle(
                            color = particle.color,
                            radius = particle.radius,
                            center = Offset(currentX * width, currentY * height)
                        )
                    }
                }
            }
    ) {
        content()
    }
}
