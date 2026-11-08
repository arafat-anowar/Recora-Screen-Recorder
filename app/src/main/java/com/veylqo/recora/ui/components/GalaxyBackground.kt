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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.veylqo.recora.ui.theme.BgDarkNavyBottom
import com.veylqo.recora.ui.theme.BgDarkNavyCenter
import com.veylqo.recora.ui.theme.BgDarkNavyTop
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-precision celestial star with astronomical brightness, realistic color temperature,
 * scintillation twinkling, and optional 4-point James Webb diamond diffraction spikes.
 */
private data class CosmicStar(
    val radiusFraction: Float,
    val angleRad: Float,
    val size: Float,
    val baseAlpha: Float,
    val twinkleSpeed: Float,
    val phase: Float,
    val color: Color,
    val hasSpike: Boolean = false,
    val spikeLength: Float = 0f
)

/**
 * Luminous wisp node along galactic spiral arms
 */
private data class SpiralNebulaNode(
    val radiusFraction: Float,
    val angleRad: Float,
    val sizeFraction: Float,
    val color: Color
)

/**
 * Celestial orb traveling along luminous galaxy orbital circles
 */
private data class OrbitingGem(
    val orbitRadiusFraction: Float,
    val speedMultiplier: Float,
    val initialAngleRad: Float,
    val coreColor: Color,
    val glowColor: Color,
    val coreRadius: Float,
    val glowRadius: Float
)

@Composable
fun GalaxyBackground(
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    // 1. GENERATE DEEP MULTI-LAYERED CELESTIAL STARFIELD (Zero runtime garbage)
    val cosmicStars = remember {
        val list = mutableListOf<CosmicStar>()

        val starColors = listOf(
            Color(0xFFFFFFFF), // Pure Diamond White
            Color(0xFFBAE6FD), // Icy Blue
            Color(0xFFDDD6FE), // Soft Lavender
            Color(0xFFFDE68A), // Solar Gold
            Color(0xFFFBCFE8)  // Rose Nebula
        )

        // Tier A: Micro Deep-Space Dust Stars (~45 stars)
        for (i in 0 until 45) {
            val dist = 0.08f + (i * 0.026f)
            val ang = ((i * 137.508) * (PI / 180.0)).toFloat() // Golden ratio angle distribution
            list.add(
                CosmicStar(
                    radiusFraction = dist,
                    angleRad = ang,
                    size = 1.0f + (i % 3) * 0.35f,
                    baseAlpha = 0.28f + (i % 5) * 0.10f,
                    twinkleSpeed = 0.8f + (i % 4) * 0.35f,
                    phase = (i * 0.82f) % (2f * PI.toFloat()),
                    color = starColors[i % starColors.size]
                )
            )
        }

        // Tier B: Prominent Galactic Body Stars (~35 stars)
        for (i in 0 until 35) {
            val dist = 0.15f + (i * 0.031f)
            val ang = ((i * 99.7) * (PI / 180.0)).toFloat()
            val hasSpike = (i % 9 == 0) // Hero stars with diffraction spikes
            list.add(
                CosmicStar(
                    radiusFraction = dist,
                    angleRad = ang,
                    size = if (hasSpike) 2.6f else 1.8f + (i % 3) * 0.4f,
                    baseAlpha = 0.45f + (i % 4) * 0.13f,
                    twinkleSpeed = 1.1f + (i % 3) * 0.45f,
                    phase = (i * 1.15f) % (2f * PI.toFloat()),
                    color = if (hasSpike) Color.White else starColors[(i + 1) % starColors.size],
                    hasSpike = hasSpike,
                    spikeLength = if (hasSpike) 22f + (i % 3) * 6f else 0f
                )
            )
        }

        list
    }

    // 2. GALACTIC SPIRAL ARMS NODES (Milky Way / Andromeda spiral vortex)
    val spiralNebulaNodes = remember {
        val list = mutableListOf<SpiralNebulaNode>()
        val armColors = listOf(
            Color(0xFF8B5CF6), // Royal Purple Arm
            Color(0xFFEC4899), // Cosmic Pink Aurora Arm
            Color(0xFF06B6D4)  // Neon Cyan Celestial Arm
        )

        // 3 Spiral arms curling outwards
        for (arm in 0..2) {
            val armBaseAngle = arm * (2f * PI.toFloat() / 3f)
            val armColor = armColors[arm]

            for (step in 1..7) {
                val t = step / 7f
                val dist = 0.22f + t * 0.78f
                val spiralAngle = armBaseAngle + t * 1.85f // Logarithmic curl
                val nodeSize = 0.20f + t * 0.14f

                list.add(
                    SpiralNebulaNode(
                        radiusFraction = dist,
                        angleRad = spiralAngle,
                        sizeFraction = nodeSize,
                        color = armColor
                    )
                )
            }
        }
        list
    }

    // 3. CELESTIAL ORBITING GEMS (Luminous gems gliding on the galaxy circles)
    val orbitingGems = remember {
        listOf(
            // Gem 1: Inner Orbit (Aquamarine Cyan)
            OrbitingGem(
                orbitRadiusFraction = 0.38f,
                speedMultiplier = 1.45f,
                initialAngleRad = 0.4f,
                coreColor = Color(0xFFE0F2FE),
                glowColor = Color(0xFF00E5FF),
                coreRadius = 3.6f,
                glowRadius = 11.0f
            ),
            // Gem 2: Mid Orbit (Hot Rose / Magenta Sun)
            OrbitingGem(
                orbitRadiusFraction = 0.60f,
                speedMultiplier = 1.05f,
                initialAngleRad = 2.4f,
                coreColor = Color(0xFFFFF1F2),
                glowColor = Color(0xFFF0446A),
                coreRadius = 4.2f,
                glowRadius = 14.0f
            ),
            // Gem 3: Outer Belt (Solar Gold / Amber Gem)
            OrbitingGem(
                orbitRadiusFraction = 0.84f,
                speedMultiplier = 0.75f,
                initialAngleRad = 4.5f,
                coreColor = Color(0xFFFEF9C3),
                glowColor = Color(0xFFFFD21F),
                coreRadius = 3.8f,
                glowRadius = 12.0f
            ),
            // Gem 4: Equatorial Disc Pearl (Lilac Glow)
            OrbitingGem(
                orbitRadiusFraction = 1.10f,
                speedMultiplier = 0.55f,
                initialAngleRad = 1.2f,
                coreColor = Color(0xFFFAF5FF),
                glowColor = Color(0xFFA78BFA),
                coreRadius = 3.2f,
                glowRadius = 10.0f
            )
        )
    }

    // Precomputed dashed path effects for astronomical orbit tracks
    val dashedOrbitEffect1 = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 16f), 0f) }
    val dashedOrbitEffect2 = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 22f), 0f) }

    val bgGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF060412), // Deep Cosmic Void
                BgDarkNavyTop,
                BgDarkNavyCenter,
                BgDarkNavyBottom,
                Color(0xFF04030B)  // Midnight Abyss
            )
        )
    }

    // High performance infinite transitions (Read only inside drawBehind = 0 recomposition)
    val infiniteTransition = rememberInfiniteTransition(label = "GalaxyMasterTransition")

    // Slow, regal clockwise rotation of main galaxy disk & celestial rings (Complete 360° rotation in 26s)
    val galaxyRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GalaxyRotation"
    )

    // Counter-clockwise rotation for inner parallax nebula & secondary celestial sphere (38s)
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = if (reduceMotion) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(38000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CounterRotation"
    )

    // Gentle breathing pulse of the galactic nucleus (7s cycle)
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = if (reduceMotion) 0.90f else 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CorePulse"
    )

    // Shimmer wave for James Webb diamond star flares
    val flareShimmer by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = if (reduceMotion) 0.75f else 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlareShimmer"
    )

    // Periodic shooting star zooming across the cosmos (Every 8 seconds)
    val shootingStarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
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
                // Galactic Nucleus crowned directly behind the Hero Record Circle
                val core = Offset(width * 0.5f, height * 0.30f)
                val baseRadius = width * 0.88f

                // ==============================================================
                // 1. DEEP SPACE NEBULA CLOUDS (PARALLAX COUNTER-ROTATING)
                // ==============================================================
                rotate(degrees = counterRotation, pivot = core) {
                    // Magenta Aurora Wisp
                    val wisp1R = baseRadius * 0.75f * corePulse
                    val wisp1Center = Offset(core.x - width * 0.14f, core.y + height * 0.05f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFF0446A).copy(alpha = 0.15f),
                                Color(0xFF6B1745).copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = wisp1Center,
                            radius = wisp1R
                        ),
                        radius = wisp1R,
                        center = wisp1Center
                    )

                    // Teal Celestial Gas Cloud
                    val wisp2R = baseRadius * 0.65f * corePulse
                    val wisp2Center = Offset(core.x + width * 0.18f, core.y - height * 0.04f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.10f),
                                Color(0xFF0E3D5C).copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = wisp2Center,
                            radius = wisp2R
                        ),
                        radius = wisp2R,
                        center = wisp2Center
                    )
                }

                // ==============================================================
                // 2. MAIN ROTATING GALAXY CORE & SPIRAL ARMS
                // ==============================================================
                rotate(degrees = galaxyRotation, pivot = core) {
                    // (A) Intense Glowing Galactic Nucleus (Multi-stage radial diffusion)
                    val coreR = baseRadius * 0.72f * corePulse
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFA78BFA).copy(alpha = 0.28f),
                                Color(0xFF7F5AF0).copy(alpha = 0.16f),
                                Color(0xFF3B1779).copy(alpha = 0.07f),
                                Color(0xFF140B30).copy(alpha = 0.02f),
                                Color.Transparent
                            ),
                            center = core,
                            radius = coreR
                        ),
                        radius = coreR,
                        center = core
                    )

                    // (B) 3 Organic Galactic Spiral Arms (Nebula Dust Wisps)
                    for (node in spiralNebulaNodes) {
                        val dist = node.radiusFraction * baseRadius
                        val nodePos = Offset(
                            core.x + dist * cos(node.angleRad),
                            core.y + dist * sin(node.angleRad)
                        )
                        val nodeR = baseRadius * node.sizeFraction

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    node.color.copy(alpha = 0.13f),
                                    node.color.copy(alpha = 0.04f),
                                    Color.Transparent
                                ),
                                center = nodePos,
                                radius = nodeR
                            ),
                            radius = nodeR,
                            center = nodePos
                        )
                    }

                    // ==============================================================
                    // 3. MAJESTIC GALAXY CIRCLES & CELESTIAL ORBITAL RINGS
                    // ==============================================================
                    val ring1R = baseRadius * 0.38f
                    val ring2R = baseRadius * 0.60f
                    val ring3R = baseRadius * 0.84f
                    val ring4R = baseRadius * 1.10f

                    // Ring 1: Inner Glowing Celestial Orbit Circle (Fine Dashed Track)
                    // Feathered soft aura pass
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.08f),
                        radius = ring1R,
                        center = core,
                        style = Stroke(width = 4.5f)
                    )
                    // Luminous dashed track
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.32f),
                        radius = ring1R,
                        center = core,
                        style = Stroke(
                            width = 1.6f,
                            pathEffect = dashedOrbitEffect1,
                            cap = StrokeCap.Round
                        )
                    )

                    // Ring 2: Mid Galaxy Orbit Circle (Glowing Double-Line Corona)
                    // Outer diffuse glow
                    drawCircle(
                        color = Color(0xFFF0446A).copy(alpha = 0.10f),
                        radius = ring2R,
                        center = core,
                        style = Stroke(width = 6.0f)
                    )
                    // Mid vibrant wire
                    drawCircle(
                        color = Color(0xFFEC4899).copy(alpha = 0.28f),
                        radius = ring2R,
                        center = core,
                        style = Stroke(width = 1.8f)
                    )
                    // Inner starlight core
                    drawCircle(
                        color = Color(0xFFFDE047).copy(alpha = 0.14f),
                        radius = ring2R * 0.985f,
                        center = core,
                        style = Stroke(width = 0.9f)
                    )

                    // Ring 3: Outer Galaxy Belt Circle (Astronomical Stippled Chart Track)
                    drawCircle(
                        color = Color(0xFFA78BFA).copy(alpha = 0.26f),
                        radius = ring3R,
                        center = core,
                        style = Stroke(
                            width = 1.4f,
                            pathEffect = dashedOrbitEffect2,
                            cap = StrokeCap.Round
                        )
                    )

                    // Ring 4: Far Cosmic Halo Circle (Subtle Boundary Halo)
                    drawCircle(
                        color = Color(0xFF818CF8).copy(alpha = 0.14f),
                        radius = ring4R,
                        center = core,
                        style = Stroke(width = 1.0f)
                    )

                    // ==============================================================
                    // 4. 3D TILTED ACCRETION RINGS (SATURN / MILKY WAY GALACTIC DISC)
                    // ==============================================================
                    // Primary 3D Tilted Galactic Disc (-26° tilt angle)
                    rotate(degrees = -26f, pivot = core) {
                        val discWidth = baseRadius * 1.85f
                        val discHeight = baseRadius * 0.62f
                        val discTopLeft = Offset(core.x - discWidth * 0.5f, core.y - discHeight * 0.5f)
                        val discSize = Size(discWidth, discHeight)

                        // Outer soft atmospheric glow
                        drawOval(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6).copy(alpha = 0.12f),
                                    Color(0xFFEC4899).copy(alpha = 0.14f),
                                    Color(0xFF00E5FF).copy(alpha = 0.10f),
                                    Color(0xFFFFD21F).copy(alpha = 0.08f),
                                    Color(0xFF8B5CF6).copy(alpha = 0.12f)
                                ),
                                center = core
                            ),
                            topLeft = discTopLeft,
                            size = discSize,
                            style = Stroke(width = 6.5f)
                        )

                        // Luminous core razor line
                        drawOval(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFA78BFA).copy(alpha = 0.42f),
                                    Color(0xFFF472B6).copy(alpha = 0.38f),
                                    Color(0xFF38BDF8).copy(alpha = 0.35f),
                                    Color(0xFFFDE68A).copy(alpha = 0.25f),
                                    Color(0xFFA78BFA).copy(alpha = 0.42f)
                                ),
                                center = core
                            ),
                            topLeft = discTopLeft,
                            size = discSize,
                            style = Stroke(width = 1.8f)
                        )
                    }

                    // Secondary Equatorial Orbit Ring (+22° counter-tilt angle)
                    rotate(degrees = 22f, pivot = core) {
                        val disc2Width = baseRadius * 1.45f
                        val disc2Height = baseRadius * 0.48f
                        val disc2TopLeft = Offset(core.x - disc2Width * 0.5f, core.y - disc2Height * 0.5f)

                        drawOval(
                            color = Color(0xFFC084FC).copy(alpha = 0.22f),
                            topLeft = disc2TopLeft,
                            size = Size(disc2Width, disc2Height),
                            style = Stroke(
                                width = 1.2f,
                                pathEffect = dashedOrbitEffect1,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // ==============================================================
                    // 5. ORBITING LUMINOUS CELESTIAL GEMS (PLANETS & CORONAS)
                    // ==============================================================
                    if (!reduceMotion) {
                        for (gem in orbitingGems) {
                            val orbitR = gem.orbitRadiusFraction * baseRadius
                            val curAng = gem.initialAngleRad + (galaxyRotation * (PI / 180.0) * gem.speedMultiplier).toFloat()
                            val gemPos = Offset(
                                core.x + orbitR * cos(curAng),
                                core.y + orbitR * sin(curAng)
                            )

                            // Outer diffuse corona
                            drawCircle(
                                color = gem.glowColor.copy(alpha = 0.22f),
                                radius = gem.glowRadius * 1.8f,
                                center = gemPos
                            )

                            // Vibrant neon aura
                            drawCircle(
                                color = gem.glowColor.copy(alpha = 0.55f),
                                radius = gem.glowRadius,
                                center = gemPos
                            )

                            // Diamond white core
                            drawCircle(
                                color = gem.coreColor,
                                radius = gem.coreRadius,
                                center = gemPos
                            )

                            // Faint trailing stardust node
                            val trailAng = curAng - (0.09f * gem.speedMultiplier)
                            val trailPos = Offset(
                                core.x + orbitR * cos(trailAng),
                                core.y + orbitR * sin(trailAng)
                            )
                            drawCircle(
                                color = gem.glowColor.copy(alpha = 0.35f),
                                radius = gem.coreRadius * 0.6f,
                                center = trailPos
                            )
                        }
                    }

                    // ==============================================================
                    // 6. SCINTILLATING STARFIELD & JAMES WEBB DIAMOND FLARES
                    // ==============================================================
                    val timeSec = galaxyRotation * 0.12f
                    for (star in cosmicStars) {
                        val dist = star.radiusFraction * baseRadius
                        val starPos = Offset(
                            core.x + dist * cos(star.angleRad),
                            core.y + dist * sin(star.angleRad)
                        )

                        // Astronomical twinkle scintillation
                        val wave = sin(timeSec * star.twinkleSpeed + star.phase)
                        val alpha = if (reduceMotion) star.baseAlpha else (star.baseAlpha + wave * 0.35f).coerceIn(0.12f, 1.0f)
                        val sz = star.size * if (!reduceMotion && alpha > 0.72f) 1.25f else 1.0f

                        // Main Star Orb
                        drawCircle(
                            color = star.color.copy(alpha = alpha),
                            radius = sz,
                            center = starPos
                        )

                        // 4-Point James Webb Diamond Cross-Spikes on Hero Stars
                        if (star.hasSpike && !reduceMotion) {
                            val spLen = star.spikeLength * flareShimmer
                            val spikeAlpha = (alpha * 0.65f).coerceIn(0f, 0.85f)
                            val spikeColor = Color.White.copy(alpha = spikeAlpha)

                            // Horizontal Ray
                            drawLine(
                                color = spikeColor,
                                start = Offset(starPos.x - spLen, starPos.y),
                                end = Offset(starPos.x + spLen, starPos.y),
                                strokeWidth = 1.0f,
                                cap = StrokeCap.Round
                            )

                            // Vertical Ray
                            drawLine(
                                color = spikeColor,
                                start = Offset(starPos.x, starPos.y - spLen),
                                end = Offset(starPos.x, starPos.y + spLen),
                                strokeWidth = 1.0f,
                                cap = StrokeCap.Round
                            )

                            // Soft ambient halo around hero star
                            drawCircle(
                                color = Color(0xFFA78BFA).copy(alpha = spikeAlpha * 0.40f),
                                radius = sz * 3.5f,
                                center = starPos
                            )
                        }
                    }
                }

                // ==============================================================
                // 7. REALISTIC PERIODIC SHOOTING STAR (METEOR)
                // ==============================================================
                if (!reduceMotion && shootingStarProgress in 0.08f..0.28f) {
                    val meteorT = (shootingStarProgress - 0.08f) / 0.20f // Normalized 0f -> 1f
                    val startX = width * (0.88f - meteorT * 0.68f)
                    val startY = height * (0.06f + meteorT * 0.36f)
                    val tailLen = 80f * (1f - (meteorT - 0.5f) * (meteorT - 0.5f) * 4f).coerceAtLeast(0.2f)

                    val endX = startX + tailLen * 0.85f
                    val endY = startY - tailLen * 0.52f
                    val meteorAlpha = (sin(meteorT * PI.toFloat())).coerceIn(0f, 1f) * 0.90f

                    // Meteor tail
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = meteorAlpha),
                                Color(0xFF67E8F9).copy(alpha = meteorAlpha * 0.70f),
                                Color(0xFFA78BFA).copy(alpha = meteorAlpha * 0.35f),
                                Color.Transparent
                            ),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round
                    )

                    // Meteor Head Spark
                    drawCircle(
                        color = Color.White.copy(alpha = meteorAlpha),
                        radius = 2.4f,
                        center = Offset(startX, startY)
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = meteorAlpha * 0.5f),
                        radius = 5.0f,
                        center = Offset(startX, startY)
                    )
                }
            }
    ) {
        content()
    }
}
