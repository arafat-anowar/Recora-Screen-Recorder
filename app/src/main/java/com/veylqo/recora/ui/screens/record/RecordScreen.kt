package com.veylqo.recora.ui.screens.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.data.model.CountdownTimer
import com.veylqo.recora.data.model.RecordUiState
import com.veylqo.recora.data.model.VideoOrientation
import com.veylqo.recora.ui.theme.CardBg
import com.veylqo.recora.ui.theme.CardBorder
import com.veylqo.recora.ui.theme.IconTileBg
import com.veylqo.recora.ui.theme.PillCornerRadius
import com.veylqo.recora.ui.theme.PrimaryPurple
import com.veylqo.recora.ui.theme.ProGold
import com.veylqo.recora.ui.theme.ProGoldBorder
import com.veylqo.recora.ui.theme.RecordPink
import com.veylqo.recora.ui.theme.RecordPinkGlow
import com.veylqo.recora.ui.theme.TextDim
import com.veylqo.recora.ui.theme.TextMuted
import com.veylqo.recora.ui.theme.TextPrimary

@Composable
fun RecordScreen(
    state: RecordUiState,
    isRecording: Boolean,
    recordingDurationText: String,
    onAudioClick: () -> Unit,
    onQualityClick: () -> Unit,
    onOrientationClick: () -> Unit,
    onTimerClick: () -> Unit,
    onToggleControls: (Boolean) -> Unit,
    onTogglePen: (Boolean) -> Unit,
    onToggleFacecam: (Boolean) -> Unit,
    onToggleTouches: (Boolean) -> Unit,
    onStartRecordingClick: () -> Unit,
    onPauseResumeClick: () -> Unit,
    onStopRecordingClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Breathing pulse for record button
    val infiniteTransition = rememberInfiniteTransition(label = "RecordPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.05f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ButtonPulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 36.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Recora",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ready when you are",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }

            // PRO Pill Button (matches screenshot: 👑 PRO)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(PillCornerRadius))
                    .background(Color(0xFF141029))
                    .border(1.5.dp, ProGoldBorder, RoundedCornerShape(PillCornerRadius))
                    .clickable { onUpgradeClick() }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "👑",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "PRO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProGold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Active recording status banner (if recording)
        AnimatedVisibility(visible = isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .border(1.5.dp, RecordPink, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(RecordPink)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Recording Screen",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = recordingDurationText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = RecordPink
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple.copy(alpha = 0.2f))
                            .clickable { onPauseResumeClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = "Pause",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(RecordPink)
                                .clickable { onStopRecordingClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = "Stop",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // CENTER RECORD HERO AREA
        Box(
            modifier = Modifier
                .size(230.dp),
            contentAlignment = Alignment.Center
        ) {
            // Faint outer ambient orbital ring
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .border(1.dp, PrimaryPurple.copy(alpha = 0.15f), CircleShape)
            )

            // Even fainter larger orbital ring
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .border(1.dp, PrimaryPurple.copy(alpha = 0.08f), CircleShape)
            )

            // Large Purple Outer Circle Button
            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .size(140.dp)
                    .shadow(
                        elevation = 30.dp,
                        shape = CircleShape,
                        spotColor = PrimaryPurple.copy(alpha = 0.5f),
                        ambientColor = PrimaryPurple.copy(alpha = 0.3f)
                    )
                    .clip(CircleShape)
                    .background(PrimaryPurple)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onStartRecordingClick() },
                contentAlignment = Alignment.Center
            ) {
                // White concentric ring
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner pink record dot
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(RecordPink),
                        contentAlignment = Alignment.Center
                    ) {
                        // Tiny center white/pink pip
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // "Tap to record" label
        Text(
            text = if (isRecording) "Recording in progress..." else "Tap to record",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 2x2 GRID OF SETTING CARDS (Audio, Quality, Orientation, Timer)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Audio
            SettingGridCard(
                title = "Audio",
                value = when (state.audioSource) {
                    com.veylqo.recora.data.model.AudioSource.Internal -> "Internal"
                    com.veylqo.recora.data.model.AudioSource.Mic -> "Mic"
                    com.veylqo.recora.data.model.AudioSource.InternalAndMic -> "Int + Mic"
                    com.veylqo.recora.data.model.AudioSource.None -> "Mute"
                },
                icon = Icons.Rounded.VolumeUp,
                onClick = onAudioClick,
                modifier = Modifier.weight(1f)
            )

            // Card 2: Quality
            val qualityText = "${state.resolution.label.split(" ").firstOrNull() ?: "FHD"} · ${state.fps.label}"
            SettingGridCard(
                title = "Quality",
                value = "${state.resolution.label.replace("1080p ", "").replace("2K ", "").replace("4K ", "")} · ${state.fps.label}".let {
                    if (state.resolution == com.veylqo.recora.data.model.VideoResolution.UHD) "4K UHD · ${state.fps.label}"
                    else if (state.resolution == com.veylqo.recora.data.model.VideoResolution.QHD) "2K QHD · ${state.fps.label}"
                    else "1080p · ${state.fps.label}"
                },
                icon = Icons.Rounded.Tv,
                onClick = onQualityClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 3: Orientation
            SettingGridCard(
                title = "Orientation",
                value = state.orientation.displayName,
                icon = Icons.Rounded.Smartphone,
                onClick = onOrientationClick,
                modifier = Modifier.weight(1f)
            )

            // Card 4: Timer
            SettingGridCard(
                title = "Timer",
                value = if (state.timer == CountdownTimer.Off) "Immediate\n(Off)" else "${state.timer.seconds}s Delay",
                icon = Icons.Rounded.Timer,
                onClick = onTimerClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2x2 GRID OF QUICK TOGGLE PILLS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickTogglePill(
                icon = Icons.Rounded.PlayCircleOutline,
                label = "Controls",
                checked = state.floatingControls,
                onToggle = onToggleControls,
                modifier = Modifier.weight(1f)
            )

            QuickTogglePill(
                icon = Icons.Rounded.Edit,
                label = "Pen & Tools",
                checked = state.penAndTools,
                onToggle = onTogglePen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickTogglePill(
                icon = Icons.Rounded.CameraAlt,
                label = "Facecam",
                checked = state.facecam,
                onToggle = onToggleFacecam,
                modifier = Modifier.weight(1f)
            )

            QuickTogglePill(
                icon = Icons.Rounded.TouchApp,
                label = "Touches",
                checked = state.showTouches,
                onToggle = onToggleTouches,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SettingGridCard(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon tile
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IconTileBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = TextPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun QuickTogglePill(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (checked) PrimaryPurple else CardBorder,
        label = "PillBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (checked) PrimaryPurple.copy(alpha = 0.15f) else CardBg,
        label = "PillBg"
    )

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(PillCornerRadius))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(PillCornerRadius))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (checked) PrimaryPurple else TextMuted,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "$label: ${if (checked) "ON" else "OFF"}",
                fontSize = 12.sp,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                color = if (checked) Color.White else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
