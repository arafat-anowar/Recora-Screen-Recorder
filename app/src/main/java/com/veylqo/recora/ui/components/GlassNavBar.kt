package com.veylqo.recora.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.ui.navigation.NavTab
import com.veylqo.recora.ui.theme.GlassNavBg
import com.veylqo.recora.ui.theme.GlassNavBorder
import com.veylqo.recora.ui.theme.PrimaryPurple
import com.veylqo.recora.ui.theme.TextDim
import com.veylqo.recora.ui.theme.TextMuted
import com.veylqo.recora.ui.theme.TextPrimary

@Composable
fun GlassNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(36.dp),
                    spotColor = Color.Black.copy(alpha = 0.5f),
                    ambientColor = PrimaryPurple.copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(36.dp))
                .background(GlassNavBg)
                .border(1.dp, GlassNavBorder, RoundedCornerShape(36.dp))
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val icon: ImageVector = when (tab) {
                    NavTab.Record -> Icons.Rounded.Videocam
                    NavTab.Recordings -> Icons.Rounded.SmartDisplay
                    NavTab.More -> Icons.Rounded.MoreHoriz
                }

                val interactionSource = remember { MutableInteractionSource() }
                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) PrimaryPurple else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "NavPillBg"
                )
                val pillScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.90f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "NavPillScale"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onTabSelected(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Pill around icon when active
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .width(58.dp)
                            .clip(CircleShape)
                            .background(pillBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) Color.White else TextMuted,
                            modifier = Modifier.size((22 * pillScale).dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextDim,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}
