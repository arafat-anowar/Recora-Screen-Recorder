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

    // App Signature Purple Theme Palette
    val themePurple = Color(0xFF8B5CF6)
    val dimTrackPurple = Color(0x388B5CF6)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070514))
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 48.dp)
    ) {
        // Center: Circular Countdown Chronometer with ONLY the countdown number inside
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.Center)
                .drawBehind {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val strokeWidth = 5.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2f - 4.dp.toPx()

                    // 1. Outer Soft Neon Glow (Multi-layer diffuse aura)
                    drawCircle(
                        color = themePurple.copy(alpha = 0.12f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth + 12.dp.toPx())
                    )
                    drawCircle(
                        color = themePurple.copy(alpha = 0.28f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth + 4.dp.toPx())
                    )

                    // 2. Full Background Dimmed Track
                    drawCircle(
                        color = dimTrackPurple,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )

                    // 3. Active Glowing Purple Arc (Depletes clockwise from 12 o'clock)
                    val sweepAngle = 360f * progressAnim.value
                    val startAngle = -90f + (360f * (1f - progressAnim.value))

                    drawArc(
                        color = themePurple,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Big Countdown Number with smooth rolling transition (ONLY the countdown number)
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
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
