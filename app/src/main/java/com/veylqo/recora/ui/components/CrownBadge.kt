package com.veylqo.recora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.core.model.UserEntitlement

@Composable
fun CrownBadge(
    entitlement: UserEntitlement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGold = entitlement.isPremium
    val borderColor = if (isGold) Color(0xFFF59E0B) else Color(0xFF4B5563)
    val bgColor = if (isGold) Color(0x33F59E0B) else Color(0x22374151)
    val textColor = if (isGold) Color(0xFFFBBF24) else Color(0xFF9CA3AF)

    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                color = bgColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "👑",
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = entitlement.badgeText,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }
    }
}
