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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.ui.theme.CardBg
import com.veylqo.recora.ui.theme.CardBorder
import com.veylqo.recora.ui.theme.TextPrimary
import java.util.Locale
import kotlin.math.ceil

@Composable
fun CountdownScreen(
    initialSeconds: Int = 3,
    onCountdownComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val totalSeconds = if (initialSeconds <= 0) 3 else initialSeconds
    val totalMillis = totalSeconds * 1000

    // Continuous linear chronograph progress: 1.0f -> 0.0f (Smooth 120 FPS sweep)
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

    // Exact integer seconds remaining: 3, 2, 1
    val displaySeconds = ceil(progressAnim.value * totalSeconds).toInt().coerceIn(1, totalSeconds)
    val clockFormat = String.format(Locale.US, "00:00:%02d", displaySeconds)

    // Reference Image Palette: Vibrant glowing neon red & dimmed track
    val neonRed = Color(0xFFFF2B54)
    val dimTrackRed = Color(0x38FF2B54)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06040A))
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 48.dp)
    ) {
        // Center: Circular Countdown Chronometer matching reference image
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
                .drawBehind {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val strokeWidth = 11.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2f - 10.dp.toPx()

                    // 1. Outer Soft Neon Glow (Multi-layer diffuse aura)
                    drawCircle(
                        color = neonRed.copy(alpha = 0.12f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth + 24.dp.toPx())
                    )
                    drawCircle(
                        color = neonRed.copy(alpha = 0.28f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth + 10.dp.toPx())
                    )

                    // 2. Full Background Dimmed Track
                    drawCircle(
                        color = dimTrackRed,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )

                    // 3. Active Glowing Red Arc (Depletes clockwise from 12 o'clock)
                    val sweepAngle = 360f * progressAnim.value
                    val startAngle = -90f + (360f * (1f - progressAnim.value))

                    drawArc(
                        color = neonRed,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                // Big Countdown Number with smooth rolling transition
                AnimatedContent(
                    targetState = displaySeconds,
                    transitionSpec = {
                        (slideInVertically(animationSpec = tween(260, easing = FastOutSlowInEasing)) { height -> -height / 2 } + fadeIn(tween(180)))
                            .togetherWith(
                                slideOutVertically(animationSpec = tween(260, easing = FastOutSlowInEasing)) { height -> height / 2 } + fadeOut(tween(180))
                            )
                    },
                    label = "CountdownDigitRoll"
                ) { sec ->
                    Text(
                        text = "$sec",
                        fontSize = 98.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // "Recording starts in" subtitle
                Text(
                    text = "Recording starts in",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.90f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // "00:00:03" Digital Clock Format
                Text(
                    text = clockFormat,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            }
        }

        // Bottom: Cancel Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(32.dp))
                .background(CardBg.copy(alpha = 0.85f))
                .border(1.dp, CardBorder, RoundedCornerShape(32.dp))
                .clickable { onCancel() }
                .padding(horizontal = 46.dp, vertical = 13.dp),
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
