package com.veylqo.recora.ui.screens.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.data.model.CountdownTimer
import com.veylqo.recora.data.model.RecordUiState
import com.veylqo.recora.ui.components.SettingCard
import com.veylqo.recora.ui.components.SettingRow
import com.veylqo.recora.ui.components.ToggleButton
import com.veylqo.recora.ui.theme.CardBg
import com.veylqo.recora.ui.theme.CardBorder
import com.veylqo.recora.ui.theme.CardCornerRadius
import com.veylqo.recora.ui.theme.PillCornerRadius
import com.veylqo.recora.ui.theme.PrimaryPurple
import com.veylqo.recora.ui.theme.ProGold
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recora",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Clean, private screen recorder",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }

                // Pro Badge / Action
                if (state.isProUser) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ProGold.copy(alpha = 0.15f))
                            .border(1.dp, ProGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "PRO ACTIVE",
                            color = ProGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryPurple.copy(alpha = 0.15f))
                            .border(1.dp, PrimaryPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { onUpgradeClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "UPGRADE",
                            color = PrimaryPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Recording in progress banner (if active)
        item {
            AnimatedVisibility(visible = isRecording) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CardCornerRadius))
                        .background(CardBg)
                        .border(1.5.dp, RecordPink, RoundedCornerShape(CardCornerRadius))
                        .padding(18.dp)
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
                                    text = "Recording in progress",
                                    fontSize = 14.sp,
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

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Pause
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(PrimaryPurple.copy(alpha = 0.2f))
                                    .clickable { onPauseResumeClick() }
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Pause,
                                    contentDescription = "Pause",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Stop
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(RecordPink)
                                    .clickable { onStopRecordingClick() }
                                    .padding(10.dp)
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
        }

        // Section Title: Capture Settings
        item {
            Text(
                text = "Capture Settings",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Audio Setting Card
        item {
            SettingCard(
                title = "Audio Source",
                subtitle = state.audioSource.displayName,
                icon = Icons.Rounded.GraphicEq,
                valueBadge = state.audioSource.displayName,
                onClick = onAudioClick
            )
        }

        // Quality Setting Card
        item {
            val qualityText = "${state.resolution.label} · ${state.fps.label}"
            SettingCard(
                title = "Quality",
                subtitle = qualityText,
                icon = Icons.Rounded.HighQuality,
                valueBadge = state.resolution.label,
                onClick = onQualityClick
            )
        }

        // Orientation Setting Card
        item {
            SettingCard(
                title = "Orientation",
                subtitle = state.orientation.displayName,
                icon = Icons.Rounded.ScreenRotation,
                valueBadge = state.orientation.displayName,
                onClick = onOrientationClick
            )
        }

        // Timer Setting Card
        item {
            SettingCard(
                title = "Countdown Timer",
                subtitle = if (state.timer == CountdownTimer.Off) "Off" else "${state.timer.seconds} seconds delay",
                icon = Icons.Rounded.Timer,
                valueBadge = state.timer.displayName,
                onClick = onTimerClick
            )
        }

        // Section Title: Overlays & Tools
        item {
            Text(
                text = "Overlays & Tools",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // Overlays Container Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CardCornerRadius))
                    .background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(CardCornerRadius))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(
                        title = "Floating Controls",
                        subtitle = "Show floating overlay controls while recording",
                        bulletDot = true,
                        trailingContent = {
                            ToggleButton(
                                checked = state.floatingControls,
                                onCheckedChange = onToggleControls
                            )
                        }
                    )
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    SettingRow(
                        title = "Pen & Tools",
                        subtitle = "Draw, highlight and annotate on screen",
                        bulletDot = true,
                        trailingContent = {
                            ToggleButton(
                                checked = state.penAndTools,
                                onCheckedChange = onTogglePen
                            )
                        }
                    )
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    SettingRow(
                        title = "Facecam",
                        subtitle = "Draggable circular camera bubble",
                        bulletDot = true,
                        trailingContent = {
                            ToggleButton(
                                checked = state.facecam,
                                onCheckedChange = onToggleFacecam
                            )
                        }
                    )
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    SettingRow(
                        title = "Show Touches",
                        subtitle = "Visual touch ripples when tapping screen",
                        bulletDot = true,
                        trailingContent = {
                            ToggleButton(
                                checked = state.showTouches,
                                onCheckedChange = onToggleTouches
                            )
                        }
                    )
                }
            }
        }

        // Big Record CTA
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStartRecordingClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(PillCornerRadius),
                        spotColor = RecordPinkGlow,
                        ambientColor = RecordPinkGlow
                    ),
                shape = RoundedCornerShape(PillCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RecordPink,
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FiberManualRecord,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isRecording) "Recording Active" else "Start Recording",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
