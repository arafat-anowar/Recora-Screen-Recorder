package com.veylqo.recora.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class CosmicParticle(
    val relX: Float,
    val relY: Float,
    val radiusPx: Float,
    val baseAlpha: Float,
    val ampAlpha: Float,
    val ampXPx: Float,
    val ampYPx: Float,
    val freqX: Int,
    val freqY: Int,
    val phaseX: Float,
    val phaseY: Float,
    val color: Color
)

@Composable
fun CosmicBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 42,
    enableMotion: Boolean = true
) {
    val particles = remember(particleCount) {
        val random = Random(42) // Stable seed for deterministic cosmic map
        val palette = listOf(
            Color(0xFF8B63F6), // Violet accent
            Color(0xFFA78BFA), // Lavender
            Color(0xFF6C44D6), // Deep violet
            Color(0xFFEF4444), // Tiny ruby cosmic speck
            Color(0xFFF59E0B), // Tiny starlight gold
            Color(0xFFFFFFFF)  // Pure starlight
        )

        List(particleCount) {
            val freqX = random.nextInt(1, 3)
            val freqY = random.nextInt(1, 3)
            CosmicParticle(
                relX = random.nextFloat(),
                relY = random.nextFloat(),
                radiusPx = random.nextFloat() * 2.8f + 1.2f,
                baseAlpha = random.nextFloat() * 0.35f + 0.15f,
                ampAlpha = random.nextFloat() * 0.25f + 0.1f,
                ampXPx = random.nextFloat() * 32f + 10f,
                ampYPx = random.nextFloat() * 40f + 12f,
                freqX = freqX,
                freqY = freqY,
                phaseX = (random.nextFloat() * Math.PI * 2).toFloat(),
                phaseY = (random.nextFloat() * Math.PI * 2).toFloat(),
                color = palette[random.nextInt(palette.size)]
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "CosmicTransition")
    val phase = if (enableMotion) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 14000, easing = LinearEasing)
            ),
            label = "CosmicPhase"
        ).value
    } else {
        0f
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Deep cosmos background with subtle radial nebulae
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF140D30),
                    Color(0xFF0C081F),
                    Color(0xFF080615)
                ),
                center = Offset(w * 0.5f, h * 0.35f),
                radius = w * 0.95f
            )
        )

        // Lower soft nebula glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x1F8B63F6),
                    Color(0x00080615)
                ),
                center = Offset(w * 0.8f, h * 0.85f),
                radius = w * 0.7f
            )
        )

        // Draw periodic trig particles
        for (p in particles) {
            val px = w * p.relX + p.ampXPx * sin(phase * p.freqX + p.phaseX)
            val py = h * p.relY + p.ampYPx * cos(phase * p.freqY + p.phaseY)
            val alpha = (p.baseAlpha + p.ampAlpha * sin(phase + p.phaseX)).coerceIn(0.08f, 0.92f)

            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.radiusPx,
                center = Offset(px, py)
            )
        }
    }
}
