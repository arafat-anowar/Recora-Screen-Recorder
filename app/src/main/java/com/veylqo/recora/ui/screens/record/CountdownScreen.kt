package com.veylqo.recora.ui.screens.record

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.ui.components.GalaxyBackground
import com.veylqo.recora.ui.theme.AccentPurpleLight
import com.veylqo.recora.ui.theme.CardBg
import com.veylqo.recora.ui.theme.CardBorder
import com.veylqo.recora.ui.theme.PrimaryPurple
import com.veylqo.recora.ui.theme.RecordPink
import com.veylqo.recora.ui.theme.TextMuted
import com.veylqo.recora.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CountdownScreen(
    initialSeconds: Int = 3,
    onCountdownComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val totalSeconds = if (initialSeconds <= 0) 3 else initialSeconds
    val totalMillis = totalSeconds * 1000

    // Continuous linear chronograph progress: 1.0f -> 0.0f (Smooth, continuous 120 FPS sweep)
    val progressAnim = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = totalMillis,
                easing = LinearEasing
            )
        )
        onCountdownComplete()
    }

    // Mathematical second calculation: exactly 1.0s per digit, zero blinking
    val displaySeconds = ceil(progressAnim.value * totalSeconds).toInt().coerceIn(1, totalSeconds)

    GalaxyBackground(coreYFraction = 0.44f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 52.dp, bottom = 40.dp)
        ) {
            // Top Header: "Recora", "Starting soon"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "Recora",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Starting soon",
                    fontSize = 15.sp,
                    color = TextMuted
                )
            }

            // Center Content: Precision Chronometer Dial + Rolling Digits + Cancel Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Precision Chronograph Dial (Continuous sweep, zero blinking, zero box artifacts)
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .drawBehind {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val strokeWidth = 7.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2f - 4.dp.toPx()

                            // 1. Subtle translucent cosmic core fill
                            drawCircle(
                                color = PrimaryPurple.copy(alpha = 0.07f),
                                radius = radius,
                                center = center
                            )

                            // 2. Background Track Ring (Faint elegant circular track)
                            drawCircle(
                                color = PrimaryPurple.copy(alpha = 0.16f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )

                            // 3. Continuous Smooth Progress Sweep Arc
                            val sweepAngle = 360f * progressAnim.value
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        PrimaryPurple,
                                        AccentPurpleLight,
                                        RecordPink,
                                        PrimaryPurple
                                    ),
                                    center = center
                                ),
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // 4. Glowing Leading-Edge Pearl (Smoothly gliding along the orbit)
                            if (progressAnim.value > 0.008f) {
                                val tipAngleRad = ((-90f + sweepAngle) * (PI / 180.0)).toFloat()
                                val tipPos = Offset(
                                    center.x + radius * cos(tipAngleRad),
                                    center.y + radius * sin(tipAngleRad)
                                )

                                // Soft bead glow
                                drawCircle(
                                    color = AccentPurpleLight.copy(alpha = 0.45f),
                                    radius = 8.dp.toPx(),
                                    center = tipPos
                                )
                                // Pure white bead nucleus
                                drawCircle(
                                    color = Color.White,
                                    radius = 3.5.dp.toPx(),
                                    center = tipPos
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Smooth Vertical Number Roll (Odometer Transition - No Blinking, No Flashing)
                    AnimatedContent(
                        targetState = displaySeconds,
                        transitionSpec = {
                            (slideInVertically(animationSpec = tween(260, easing = FastOutSlowInEasing)) { height -> -height / 2 } + fadeIn(tween(180)))
                                .togetherWith(
                                    slideOutVertically(animationSpec = tween(260, easing = FastOutSlowInEasing)) { height -> height / 2 } + fadeOut(tween(180))
                                )
                        },
                        label = "CountdownNumberRoll"
                    ) { sec ->
                        Text(
                            text = "$sec",
                            fontSize = 92.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(38.dp))

                // Text: "Recording starts in X..."
                Text(
                    text = "Recording starts in $displaySeconds...",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Cancel Button centered directly underneath
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(CardBg.copy(alpha = 0.8f))
                        .border(1.dp, CardBorder, RoundedCornerShape(32.dp))
                        .clickable { onCancel() }
                        .padding(horizontal = 44.dp, vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
