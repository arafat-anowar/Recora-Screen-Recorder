package com.veylqo.recora.ui.sheets

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.data.model.VideoFps
import com.veylqo.recora.data.model.VideoResolution
import com.veylqo.recora.ui.theme.CardBg
import com.veylqo.recora.ui.theme.CardBorder
import com.veylqo.recora.ui.theme.PrimaryPurple
import com.veylqo.recora.ui.theme.ProGold
import com.veylqo.recora.ui.theme.TextMuted
import com.veylqo.recora.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityPickerSheet(
    selectedResolution: VideoResolution,
    selectedFps: VideoFps,
    isProUser: Boolean,
    onSelectResolution: (VideoResolution) -> Unit,
    onSelectFps: (VideoFps) -> Unit,
    onShowPlan: () -> Unit,
    onDismiss: () -> Unit
) {
    RecoraBottomSheet(
        onDismissRequest = onDismiss,
        title = "Recording Quality",
        subtitle = "Choose capture resolution and framerate"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Resolution Section
            Text(
                text = "RESOLUTION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VideoResolution.entries.forEach { res ->
                    val isLocked = res.isPro && !isProUser
                    SheetOptionItem(
                        title = res.label,
                        subtitle = res.pixels,
                        selected = selectedResolution == res,
                        badge = if (res.isPro) "PRO" else null,
                        badgeColor = ProGold,
                        onClick = {
                            if (isLocked) {
                                onDismiss()
                                onShowPlan()
                            } else {
                                onSelectResolution(res)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Framerate Section
            Text(
                text = "FRAME RATE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VideoFps.entries.forEach { fps ->
                    val isSelected = selectedFps == fps
                    val bgColor = if (isSelected) PrimaryPurple.copy(alpha = 0.15f) else CardBg
                    val borderColor = if (isSelected) PrimaryPurple else CardBorder

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable { onSelectFps(fps) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fps.label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextPrimary
                        )
                    }
                }
            }
        }
    }
}
