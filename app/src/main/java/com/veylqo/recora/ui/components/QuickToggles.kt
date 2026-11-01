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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.ui.theme.CosmicBorder
import com.veylqo.recora.ui.theme.CosmicSurface
import com.veylqo.recora.ui.theme.TextPrimary
import com.veylqo.recora.ui.theme.TextSecondary
import com.veylqo.recora.ui.theme.VioletPrimary

@Composable
fun QuickTogglesGrid(
    showControls: Boolean,
    showPenTools: Boolean,
    showFacecam: Boolean,
    showTouches: Boolean,
    onControlsToggle: (Boolean) -> Unit,
    onPenToolsToggle: (Boolean) -> Unit,
    onFacecamToggle: (Boolean) -> Unit,
    onTouchesToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Controls & Pen & Tools
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TogglePill(
                icon = Icons.Rounded.PlayCircle,
                label = "Controls",
                isEnabled = showControls,
                onToggle = { onControlsToggle(!showControls) },
                modifier = Modifier.weight(1f)
            )
            TogglePill(
                icon = Icons.Rounded.Edit,
                label = "Pen & Tools",
                isEnabled = showPenTools,
                onToggle = { onPenToolsToggle(!showPenTools) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Facecam & Touches
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TogglePill(
                icon = Icons.Rounded.CameraAlt,
                label = "Facecam",
                isEnabled = showFacecam,
                onToggle = { onFacecamToggle(!showFacecam) },
                modifier = Modifier.weight(1f)
            )
            TogglePill(
                icon = Icons.Rounded.TouchApp,
                label = "Touches",
                isEnabled = showTouches,
                onToggle = { onTouchesToggle(!showTouches) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TogglePill(
    icon: ImageVector,
    label: String,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isEnabled) VioletPrimary else CosmicBorder
    val bgColor = if (isEnabled) Color(0xFF22174F) else CosmicSurface
    val textColor = if (isEnabled) TextPrimary else TextSecondary
    val iconTint = if (isEnabled) VioletPrimary else TextSecondary
    val statusText = if (isEnabled) "ON" else "OFF"

    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                color = bgColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$label: $statusText",
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}
