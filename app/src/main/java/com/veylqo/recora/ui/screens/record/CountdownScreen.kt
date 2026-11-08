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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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

@Composable
fun CountdownScreen(
    initialSeconds: Int = 3,
    onCountdownComplete: () -> Unit,
    onCancel: () -> Unit
) {
    var remainingSeconds by remember { mutableIntStateOf(if (initialSeconds <= 0) 3 else initialSeconds) }
    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(remainingSeconds) {
        scaleAnim.snapTo(1.15f)
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 1) {
            delay(1000)
            remainingSeconds--
        }
        delay(1000)
        onCountdownComplete()
    }

    GalaxyBackground {
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
                // Countdown Circle with Purple Glow (Optimized graphicsLayer + subtle shadow)
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .graphicsLayer {
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                        }
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            spotColor = PrimaryPurple.copy(alpha = 0.5f),
                            ambientColor = PrimaryPurple.copy(alpha = 0.3f)
                        )
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(5.dp, PrimaryPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$remainingSeconds",
                        fontSize = 86.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
