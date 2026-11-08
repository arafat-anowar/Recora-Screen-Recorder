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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.veylqo.recora.ui.theme.BgDarkNavyBottom
import com.veylqo.recora.ui.theme.BgDarkNavyCenter
import com.veylqo.recora.ui.theme.BgDarkNavyTop
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class PolarStar(
    val radiusFraction: Float,
    val angleRad: Float,
    val size: Float,
    val baseAlpha: Float,
    val twinkleSpeed: Float,
    val phase: Float
)

private data class OrbitingDust(
    val orbitRadiusFraction: Float,
    val baseAngleRad: Float,
    val angularSpeedMultiplier: Float,
    val size: Float,
    val color: Color
)

@Composable
fun GalaxyBackground(
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    // Generate rotating spiral starfield clustered around the galactic core
    val spiralStars = remember {
        val list = mutableListOf<PolarStar>()
        val starData = listOf(
            // Inner Core Stars
            Triple(0.08f, 0.2f, 1.6f), Triple(0.12f, 1.4f, 2.0f), Triple(0.15f, 2.8f, 1.8f),
            Triple(0.18f, 4.2f, 1.5f), Triple(0.20f, 5.6f, 2.2f), Triple(0.22f, 0.9f, 1.9f),
            // Spiral Arm 1 (Purple Arm)
            Triple(0.25f, 0.4f, 1.7f), Triple(0.32f, 0.9f, 2.2f), Triple(0.40f, 1.5f, 1.6f),
            Triple(0.48f, 2.1f, 2.4f), Triple(0.56f, 2.7f, 1.8f), Triple(0.65f, 3.2f, 2.0f),
            Triple(0.74f, 3.8f, 1.5f), Triple(0.85f, 4.3f, 1.9f), Triple(0.95f, 4.9f, 1.6f),
            // Spiral Arm 2 (Pink Aurora Arm)
            Triple(0.26f, 2.5f, 1.8f), Triple(0.34f, 3.0f, 2.1f), Triple(0.42f, 3.6f, 1.5f),
            Triple(0.50f, 4.2f, 2.3f), Triple(0.58f, 4.8f, 1.7f), Triple(0.68f, 5.3f, 2.1f),
            Triple(0.76f, 5.9f, 1.6f), Triple(0.88f, 0.3f, 1.8f), Triple(0.98f, 0.8f, 1.5f),
            // Spiral Arm 3 (Cyan/Cosmic Blue Arm)
            Triple(0.27f, 4.6f, 1.9f), Triple(0.35f, 5.1f, 2.2f), Triple(0.44f, 5.7f, 1.6f),
            Triple(0.52f, 6.3f, 2.4f), Triple(0.60f, 0.6f, 1.8f), Triple(0.70f, 1.2f, 2.0f),
            Triple(0.78f, 1.7f, 1.5f), Triple(0.90f, 2.2f, 1.9f), Triple(1.02f, 2.8f, 1.7f),
            // Outer Halo Stars
            Triple(0.82f, 1.1f, 1.4f), Triple(0.92f, 3.5f, 1.6f), Triple(1.05f, 5.2f, 1.3f),
            Triple(1.10f, 2.0f, 1.5f), Triple(1.15f, 4.0f, 1.8f), Triple(1.20f, 0.5f, 1.4f)
        )

        starData.forEachIndexed { index, (dist, angle, sz) ->
            list.add(
                PolarStar(
                    radiusFraction = dist,
                    angleRad = angle,
                    size = sz,
                    baseAlpha = 0.40f + ((index % 3) * 0.15f),
                    twinkleSpeed = 0.9f + ((index % 4) * 0.35f),
                    phase = (index * 0.68f) % (2f * PI.toFloat())
                )
            )
        }
        list
    }

    // Orbiting luminous cosmic dust
    val orbitingDust = remember {
        listOf(
            OrbitingDust(0.32f, 0.5f, 1.2f, 3.0f, Color(0xFF9D72FF).copy(alpha = 0.55f)),
            OrbitingDust(0.48f, 2.2f, 0.9f, 3.8f, Color(0xFFF0446A).copy(alpha = 0.45f)),
            OrbitingDust(0.62f, 3.9f, 1.1f, 2.6f, Color(0xFF00E5FF).copy(alpha = 0.45f)),
            OrbitingDust(0.75f, 5.3f, 0.8f, 3.4f, Color(0xFFFFD21F).copy(alpha = 0.40f)),
            OrbitingDust(0.22f, 1.8f, 1.4f, 2.8f, Color(0xFFC084FC).copy(alpha = 0.60f))
        )
    }

    val bgGradient = remember {
        Brush.verticalGradient(
            colors = listOf(BgDarkNavyTop, BgDarkNavyCenter, BgDarkNavyBottom)
        )
    }

    // Infinite transitions for visible, active rotation (0 content recomposition)
    val infiniteTransition = rememberInfiniteTransition(label = "GalaxyMovingAnimation")

    // Visible clockwise rotation of main galaxy disk & stars (Complete 360° rotation in 20 seconds)
    val galaxyRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GalaxyRotation"
    )

    // Counter-clockwise rotation for inner parallax nebula depth (32 seconds)
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = if (reduceMotion) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CounterRotation"
    )

    // Breathing pulse of the galactic core
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = if (reduceMotion) 0.88f else 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CorePulse"
    )

    // Periodic shooting star zooming across the cosmos (Every 7 seconds)
    val shootingStarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShootingStar"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .drawBehind {
                val width = size.width
                val height = size.height
                // Galactic Core centered directly behind the Hero Record Area
                val core = Offset(width * 0.5f, height * 0.34f)
                val baseRadius = width * 0.85f

                // ==========================================
                // 1. ROTATING INNER NEBULA (COUNTER-ROTATING)
                // ==========================================
                rotate(degrees = counterRotation, pivot = core) {
                    val r = baseRadius * 0.70f * corePulse
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFF0446A).copy(alpha = 0.14f),
                                Color(0xFF6B1745).copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(core.x - width * 0.10f, core.y + height * 0.04f),
                            radius = r
                        ),
                        radius = r,
                        center = Offset(core.x - width * 0.10f, core.y + height * 0.04f)
                    )
                }

                // ==========================================
                // 2. MAIN ROTATING GALAXY (CLOCKWISE)
                // ==========================================
                rotate(degrees = galaxyRotation, pivot = core) {
                    // (A) Intense Glowing Galactic Core
                    val coreR = baseRadius * 0.65f * corePulse
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.22f),
                                Color(0xFF4C2899).copy(alpha = 0.10f),
                                Color(0xFF1E103F).copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = core,
                            radius = coreR
                        ),
                        radius = coreR,
                        center = core
                    )

                    // (B) 3 Visible Spiral Arms of the Galaxy
                    val armAngles = listOf(0.0, 2.0944, 4.1888) // 0°, 120°, 240°
                    val armColors = listOf(
                        Color(0xFF8B5CF6).copy(alpha = 0.15f), // Purple
                        Color(0xFFEC4899).copy(alpha = 0.12f), // Pink Aurora
                        Color(0xFF38BDF8).copy(alpha = 0.11f)  // Cyan Cosmic
                    )

                    for (i in armAngles.indices) {
                        val baseAng = armAngles[i]
                        val armCol = armColors[i]

                        // Draw 3 luminous nodes along each spiral arm
                        for (step in 1..3) {
                            val dist = baseRadius * (0.28f + step * 0.18f)
                            val ang = (baseAng + step * 0.45f).toFloat()
                            val nodePos = Offset(
                                core.x + dist * cos(ang),
                                core.y + dist * sin(ang)
                            )
                            val nodeR = baseRadius * (0.22f + step * 0.06f)

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(armCol, Color.Transparent),
                                    center = nodePos,
                                    radius = nodeR
                                ),
                                radius = nodeR,
                                center = nodePos
                            )
                        }
                    }

                    // (C) LUMINOUS CONCENTRIC GALAXY CIRCLES & ORBITAL RINGS
                    val ring1R = baseRadius * 0.38f
                    val ring2R = baseRadius * 0.58f
                    val ring3R = baseRadius * 0.82f
                    val ring4R = baseRadius * 1.10f

                    // Ring 1: Inner Glowing Celestial Orbit Circle
                    drawCircle(
                        color = Color(0xFF8B5CF6).copy(alpha = 0.22f),
                        radius = ring1R,
                        center = core,
                        style = Stroke(width = 1.8f)
                    )

                    // Ring 2: Mid Galaxy Orbit Circle
                    drawCircle(
                        color = Color(0xFFEC4899).copy(alpha = 0.18f),
                        radius = ring2R,
                        center = core,
                        style = Stroke(width = 1.4f)
                    )

                    // Ring 3: Outer Galaxy Belt Circle
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.16f),
                        radius = ring3R,
                        center = core,
                        style = Stroke(width = 1.6f)
                    )

                    // Ring 4: Far Cosmic Halo Circle
                    drawCircle(
                        color = Color(0xFFA78BFA).copy(alpha = 0.11f),
                        radius = ring4R,
                        center = core,
                        style = Stroke(width = 1.2f)
                    )

                    // Tilted 3D Elliptical Galaxy Ring (Saturn / Milky Way Disc Circle)
                    val ovalWidth = baseRadius * 1.70f
                    val ovalHeight = baseRadius * 0.65f
                    val ovalTopLeft = Offset(core.x - ovalWidth * 0.5f, core.y - ovalHeight * 0.5f)

                    drawOval(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.30f),
                                Color(0xFFEC4899).copy(alpha = 0.24f),
                                Color(0xFF38BDF8).copy(alpha = 0.20f),
                                Color(0xFF8B5CF6).copy(alpha = 0.08f),
                                Color(0xFF8B5CF6).copy(alpha = 0.30f)
                            ),
                            center = core
                        ),
                        topLeft = ovalTopLeft,
                        size = Size(ovalWidth, ovalHeight),
                        style = Stroke(width = 2.2f)
                    )

                    // Secondary Tilted Orbit Circle
                    val oval2Width = baseRadius * 1.30f
                    val oval2Height = baseRadius * 0.50f
                    val oval2TopLeft = Offset(core.x - oval2Width * 0.5f, core.y - oval2Height * 0.5f)

                    drawOval(
                        color = Color(0xFFC084FC).copy(alpha = 0.18f),
                        topLeft = oval2TopLeft,
                        size = Size(oval2Width, oval2Height),
                        style = Stroke(width = 1.4f)
                    )

                    // (D) Glowing Orbital Marker Beads orbiting on the circles
                    if (!reduceMotion) {
                        // Pearl on Ring 1
                        val bead1Ang = (galaxyRotation * 1.3f * (PI / 180.0)).toFloat()
                        drawCircle(
                            color = Color(0xFFEC4899),
                            radius = 4.0f,
                            center = Offset(core.x + ring1R * cos(bead1Ang), core.y + ring1R * sin(bead1Ang))
                        )
                        drawCircle(
                            color = Color(0xFFEC4899).copy(alpha = 0.35f),
                            radius = 9.0f,
                            center = Offset(core.x + ring1R * cos(bead1Ang), core.y + ring1R * sin(bead1Ang))
                        )

                        // Pearl on Ring 2
                        val bead2Ang = (galaxyRotation * 0.9f * (PI / 180.0) + 2.1).toFloat()
                        drawCircle(
                            color = Color(0xFF38BDF8),
                            radius = 3.5f,
                            center = Offset(core.x + ring2R * cos(bead2Ang), core.y + ring2R * sin(bead2Ang))
                        )
                        drawCircle(
                            color = Color(0xFF38BDF8).copy(alpha = 0.30f),
                            radius = 8.0f,
                            center = Offset(core.x + ring2R * cos(bead2Ang), core.y + ring2R * sin(bead2Ang))
                        )

                        // Pearl on Ring 3
                        val bead3Ang = (galaxyRotation * 0.7f * (PI / 180.0) + 4.2).toFloat()
                        drawCircle(
                            color = Color(0xFFFFD21F),
                            radius = 3.8f,
                            center = Offset(core.x + ring3R * cos(bead3Ang), core.y + ring3R * sin(bead3Ang))
                        )
                        drawCircle(
                            color = Color(0xFFFFD21F).copy(alpha = 0.35f),
                            radius = 8.5f,
                            center = Offset(core.x + ring3R * cos(bead3Ang), core.y + ring3R * sin(bead3Ang))
                        )
                    }

                    // (E) Rotating Starfield (Stars revolve around the core in real-time)
                    val timeSec = galaxyRotation * 0.1f
                    for (star in spiralStars) {
                        val dist = star.radiusFraction * baseRadius
                        val starPos = Offset(
                            core.x + dist * cos(star.angleRad),
                            core.y + dist * sin(star.angleRad)
                        )

                        // Twinkle calculation
                        val wave = sin(timeSec * star.twinkleSpeed + star.phase)
                        val alpha = if (reduceMotion) star.baseAlpha else (star.baseAlpha + wave * 0.35f).coerceIn(0.15f, 1.0f)
                        val sz = star.size * if (!reduceMotion && alpha > 0.75f) 1.35f else 1.0f

                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = sz,
                            center = starPos
                        )

                        // Star flare on prominent stars
                        if (sz > 2.0f && alpha > 0.80f && !reduceMotion) {
                            drawCircle(
                                color = Color(0xFFA78BFA).copy(alpha = 0.30f),
                                radius = sz * 2.8f,
                                center = starPos
                            )
                        }
                    }

                    // (F) Orbiting Luminous Cosmic Dust Particles
                    if (!reduceMotion) {
                        for (dust in orbitingDust) {
                            val dustDist = dust.orbitRadiusFraction * baseRadius
                            val curAng = dust.baseAngleRad + (galaxyRotation * (PI / 180.0) * dust.angularSpeedMultiplier).toFloat()
                            val dustPos = Offset(
                                core.x + dustDist * cos(curAng),
                                core.y + dustDist * sin(curAng)
                            )

                            drawCircle(
                                color = dust.color,
                                radius = dust.size,
                                center = dustPos
                            )
                        }
                    }
                }

                // ==========================================
                // 3. PERIODIC SHOOTING STAR (METEOR)
                // ==========================================
                if (!reduceMotion && shootingStarProgress in 0.10f..0.32f) {
                    val meteorT = (shootingStarProgress - 0.10f) / 0.22f // 0f -> 1f
                    val startX = width * (0.85f - meteorT * 0.65f)
                    val startY = height * (0.08f + meteorT * 0.38f)
                    val tailLen = 65f * (1f - (meteorT - 0.5f) * (meteorT - 0.5f) * 4f).coerceAtLeast(0.3f)

                    val endX = startX + tailLen * 0.85f
                    val endY = startY - tailLen * 0.52f
                    val meteorAlpha = (sin(meteorT * PI.toFloat())).coerceIn(0f, 1f) * 0.85f

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = meteorAlpha),
                                Color(0xFFA78BFA).copy(alpha = meteorAlpha * 0.6f),
                                Color.Transparent
                            ),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.4f,
                        cap = StrokeCap.Round
                    )
                }
            }
    ) {
        content()
    }
}
