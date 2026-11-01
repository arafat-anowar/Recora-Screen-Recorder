package com.veylqo.recora.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.core.model.RecordingState
import com.veylqo.recora.ui.theme.RecordingRed
import com.veylqo.recora.ui.theme.TextPrimary
import com.veylqo.recora.ui.theme.TextSecondary
import com.veylqo.recora.ui.theme.VioletPrimary
import com.veylqo.recora.ui.theme.VioletSecondary

@Composable
fun RecordButton(
    recordingState: RecordingState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = recordingState is RecordingState.Recording
    val isPaused = (recordingState as? RecordingState.Recording)?.isPaused == true
    val durationMs = (recordingState as? RecordingState.Recording)?.durationMs ?: 0L

    val infiniteTransition = rememberInfiniteTransition(label = "RecordPulse")
    val pulseScale = if (isRecording && !isPaused) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseScale"
        ).value
    } else {
        1f
    }

    val pulseAlpha = if (isRecording && !isPaused) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseAlpha"
        ).value
    } else {
        0.3f
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Concentric Canvas Rings
            Canvas(modifier = Modifier.size(240.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Faint outermost orbital ring
                drawCircle(
                    color = Color(0xFF322868).copy(alpha = 0.45f),
                    radius = (size.width / 2f - 4.dp.toPx()),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Mid orbital ring with pulsing glow
                drawCircle(
                    color = if (isRecording) RecordingRed.copy(alpha = pulseAlpha * 0.5f) else Color(0xFF5341A8).copy(alpha = 0.5f),
                    radius = (size.width / 2f - 24.dp.toPx()) * pulseScale,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Violet Orb Container (or Red glow when recording)
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isRecording) {
                                listOf(Color(0xFF7F1D1D), Color(0xFF450A0A))
                            } else {
                                listOf(VioletPrimary, VioletSecondary)
                            }
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = true, color = Color.White),
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                // White Aperture Ring
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        // Stop square (Material rounded square)
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RecordingRed)
                        )
                    } else {
                        // Red circular core with center white highlight
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFF87171), RecordingRed)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Center white reflection dot
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.95f))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Label / Timer under Record button
        when (recordingState) {
            is RecordingState.Idle -> {
                Text(
                    text = "Tap to record",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
            }
            is RecordingState.Preparing -> {
                Text(
                    text = if (recordingState.countdownRemainingSeconds > 0) {
                        "Starting in ${recordingState.countdownRemainingSeconds}s..."
                    } else {
                        "Preparing recorder..."
                    },
                    color = VioletPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            is RecordingState.Recording -> {
                val seconds = (durationMs / 1000) % 60
                val minutes = (durationMs / 1000) / 60
                val timeStr = "%02d:%02d".format(minutes, seconds)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeStr,
                        color = if (isPaused) Color(0xFFFBBF24) else RecordingRed,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isPaused) "Paused • Tap to resume" else "Tap to stop recording",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            is RecordingState.Stopping -> {
                Text(
                    text = "Finalizing video...",
                    color = VioletPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            else -> {
                Text(
                    text = "Tap to record",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
