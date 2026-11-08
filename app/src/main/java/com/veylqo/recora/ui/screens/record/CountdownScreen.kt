package com.veylqo.recora.ui.screens.record

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.ui.components.GalaxyBackground
import com.veylqo.recora.ui.theme.CardBg
import com.veylqo.recora.ui.theme.CardBorder
import com.veylqo.recora.ui.theme.PrimaryPurple
import com.veylqo.recora.ui.theme.TextMuted
import com.veylqo.recora.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CountdownScreen(
    initialSeconds: Int = 3,
    onCountdownComplete: () -> Unit,
    onCancel: () -> Unit
) {
    var remainingSeconds by remember { mutableIntStateOf(if (initialSeconds <= 0) 3 else initialSeconds) }

    // Number punch animation (Scale & Alpha)
    val numberScale = remember { Animatable(1f) }
    val numberAlpha = remember { Animatable(1f) }

    // Ring glow wave animation (Zero layout / zero box artifact)
    val ringPulse = remember { Animatable(1f) }

    LaunchedEffect(remainingSeconds) {
        launch {
            numberScale.snapTo(1.35f)
            numberScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(360, easing = FastOutSlowInEasing)
            )
        }
        launch {
            numberAlpha.snapTo(0.25f)
            numberAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            )
        }
        launch {
            ringPulse.snapTo(1.12f)
            ringPulse.animateTo(
                targetValue = 1f,
                animationSpec = tween(420, easing = FastOutSlowInEasing)
            )
        }
    }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 1) {
            delay(1000)
            remainingSeconds--
        }
        delay(1000)
        onCountdownComplete()
    }

    // Align galaxy background celestial center directly with the countdown circle (0.44f)
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

            // Center Content: Glowing Ring + "Recording starts in X..." + Cancel Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Countdown Circle: Pure Canvas GPU Rendering (Zero square shadow box, zero clipping artifacts)
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .drawBehind {
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val baseR = size.minDimension / 2f - 10.dp.toPx()

                            // 1. Soft glowing outer pulse halo (100% circular, no elevation box shadow)
                            val pulseR = baseR + 8.dp.toPx() * ringPulse.value
                            drawCircle(
                                color = PrimaryPurple.copy(alpha = 0.18f * ringPulse.value),
                                radius = pulseR,
                                center = c,
                                style = Stroke(width = 10.dp.toPx())
                            )

                            // 2. Translucent cosmic core fill
                            drawCircle(
                                color = PrimaryPurple.copy(alpha = 0.08f),
                                radius = baseR,
                                center = c
                            )

                            // 3. Crisp Primary Purple ring
                            drawCircle(
                                color = PrimaryPurple,
                                radius = baseR,
                                center = c,
                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // 4. Subtle inner celestial highlight ring
                            drawCircle(
                                color = Color.White.copy(alpha = 0.16f),
                                radius = baseR - 6.dp.toPx(),
                                center = c,
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$remainingSeconds",
                        fontSize = 88.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.graphicsLayer {
                            scaleX = numberScale.value
                            scaleY = numberScale.value
                            alpha = numberAlpha.value
                        }
                    )
                }

                Spacer(modifier = Modifier.height(38.dp))

                // Text: "Recording starts in 3..."
                Text(
                    text = "Recording starts in $remainingSeconds...",
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
