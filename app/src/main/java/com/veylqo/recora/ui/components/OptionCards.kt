package com.veylqo.recora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.core.model.AudioSource
import com.veylqo.recora.core.model.FpsOption
import com.veylqo.recora.core.model.OrientationOption
import com.veylqo.recora.core.model.ResolutionPreset
import com.veylqo.recora.core.model.TimerPreset
import com.veylqo.recora.ui.theme.CosmicCardBorder
import com.veylqo.recora.ui.theme.CosmicSurface
import com.veylqo.recora.ui.theme.TextPrimary
import com.veylqo.recora.ui.theme.TextSecondary

@Composable
fun QuickOptionCardsGrid(
    audioSource: AudioSource,
    resolutionPreset: ResolutionPreset,
    fpsOption: FpsOption,
    orientationOption: OrientationOption,
    timerPreset: TimerPreset,
    onAudioClick: () -> Unit,
    onQualityClick: () -> Unit,
    onOrientationClick: () -> Unit,
    onTimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Audio & Quality
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OptionCard(
                icon = Icons.Rounded.GraphicEq,
                title = "Audio",
                subtitle = audioSource.displayName,
                onClick = onAudioClick,
                modifier = Modifier.weight(1f)
            )
            OptionCard(
                icon = Icons.Rounded.Tv,
                title = "Quality",
                subtitle = "${resolutionPreset.displayName} • ${fpsOption.value}fps",
                onClick = onQualityClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Orientation & Timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OptionCard(
                icon = Icons.Rounded.ScreenLockRotation,
                title = "Orientation",
                subtitle = orientationOption.displayName,
                onClick = onOrientationClick,
                modifier = Modifier.weight(1f)
            )
            OptionCard(
                icon = Icons.Rounded.Timer,
                title = "Timer",
                subtitle = timerPreset.displayName,
                onClick = onTimerClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun OptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = CosmicCardBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .background(
                color = CosmicSurface,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon capsule
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = Color(0xFF1E1848),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
