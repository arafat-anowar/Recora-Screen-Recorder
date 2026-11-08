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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.veylqo.recora.ui.theme.BgDarkNavyBottom
import com.veylqo.recora.ui.theme.BgDarkNavyCenter
import com.veylqo.recora.ui.theme.BgDarkNavyTop
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimalist, subtle celestial star with gentle scintillation.
 */
private data class MinimalStar(
    val radiusFraction: Float,
    val angleRad: Float,
    val size: Float,
    val baseAlpha: Float,
    val twinkleSpeed: Float,
    val phase: Float,
    val color: Color
)

@Composable
fun GalaxyBackground(
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    // 1. SUBTLE, CALM CELESTIAL STARFIELD (Clean, non-distracting pinprick stars)
    val stars = remember {
        val list = mutableListOf<MinimalStar>()
        val starColors = listOf(
            Color.White,
            Color(0xFFE2E8F0),
            Color(0xFFDDD6FE),
            Color(0xFFBAE6FD)
        )

        // 36 tiny, elegant stars scattered across the celestial dome
        for (i in 0 until 36) {
            val dist = 0.12f + (i * 0.028f)
            val ang = ((i * 137.5) * (PI / 180.0)).toFloat() // Golden ratio distribution
            list.add(
                MinimalStar(
                    radiusFraction = dist,
                    angleRad = ang,
                    size = if (i % 5 == 0) 1.6f else 1.1f,
                    baseAlpha = 0.20f + (i % 4) * 0.10f,
                    twinkleSpeed = 0.6f + (i % 3) * 0.3f,
                    phase = (i * 0.9f) % (2f * PI.toFloat()),
                    color = starColors[i % starColors.size]
                )
            )
        }
        list
    }

    val bgGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF070514),
                BgDarkNavyTop,
                BgDarkNavyCenter,
                BgDarkNavyBottom,
                Color(0xFF05030E)
            )
        )
    }

    // 2. SLOW, SERENE ANIMATIONS (Calm and premium, zero recomposition)
    val infiniteTransition = rememberInfiniteTransition(label = "MinimalGalaxyTransition")

    // Slow, serene rotation (1 full turn every 48 seconds)
    val galaxyRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(48000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GalaxySlowRotation"
    )

    // Gentle breathing of the soft galactic core (8s cycle)
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = if (reduceMotion) 0.92f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CoreGentlePulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .drawBehind {
                val width = size.width
                val height = size.height
                // Centered right behind the Record Hero button
                val core = Offset(width * 0.5f, height * 0.30f)
                val baseRadius = width * 0.85f

                // ==============================================================
                // 1. SOFT, ATMOSPHERIC GALAXY GLOW (Gentle & Deep, not loud)
                // ==============================================================
                val glowR = baseRadius * 0.70f * corePulse
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7F5AF0).copy(alpha = 0.14f),
                            Color(0xFF4C2899).copy(alpha = 0.06f),
                            Color(0xFF1E103F).copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        center = core,
                        radius = glowR
                    ),
                    radius = glowR,
                    center = core
                )

                // ==============================================================
                // 2. CLEAN, MINIMAL GALAXY CIRCLES & SERENE STARFIELD
                // ==============================================================
                rotate(degrees = galaxyRotation, pivot = core) {
                    val ring1R = baseRadius * 0.40f
                    val ring2R = baseRadius * 0.65f

                    // Ring 1: Clean, delicate inner orbital circle
                    drawCircle(
                        color = Color(0xFFA78BFA).copy(alpha = 0.13f),
                        radius = ring1R,
                        center = core,
                        style = Stroke(width = 1.2f)
                    )

                    // Ring 2: Subtle outer celestial circle
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.08f),
                        radius = ring2R,
                        center = core,
                        style = Stroke(width = 1.0f)
                    )

                    // Single tiny luminous starlight pearl orbiting smoothly on Ring 1
                    if (!reduceMotion) {
                        val pearlAng = (galaxyRotation * 1.5f * (PI / 180.0)).toFloat()
                        val pearlPos = Offset(
                            core.x + ring1R * cos(pearlAng),
                            core.y + ring1R * sin(pearlAng)
                        )

                        // Faint pearl glow
                        drawCircle(
                            color = Color(0xFFC084FC).copy(alpha = 0.28f),
                            radius = 6.0f,
                            center = pearlPos
                        )
                        // Pearl core
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = 2.4f,
                            center = pearlPos
                        )
                    }

                    // Delicate, calm pinprick starfield
                    val timeSec = galaxyRotation * 0.08f
                    for (star in stars) {
                        val dist = star.radiusFraction * baseRadius
                        val starPos = Offset(
                            core.x + dist * cos(star.angleRad),
                            core.y + dist * sin(star.angleRad)
                        )

                        // Gentle twinkle
                        val wave = sin(timeSec * star.twinkleSpeed + star.phase)
                        val alpha = if (reduceMotion) star.baseAlpha else (star.baseAlpha + wave * 0.18f).coerceIn(0.08f, 0.70f)

                        drawCircle(
                            color = star.color.copy(alpha = alpha),
                            radius = star.size,
                            center = starPos
                        )
                    }
                }
            }
    ) {
        content()
    }
}
