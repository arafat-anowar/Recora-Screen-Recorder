package com.veylqo.recora.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.veylqo.recora.data.model.ClipItem
import com.veylqo.recora.ui.theme.ErrorRed
import com.veylqo.recora.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipActionSheet(
    clip: ClipItem,
    onPlay: () -> Unit,
    onEditAndTrim: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    RecoraBottomSheet(
        onDismissRequest = onDismiss,
        title = clip.title
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ClipActionItem(
                icon = Icons.Rounded.PlayArrow,
                title = "Play",
                onClick = {
                    onDismiss()
                    onPlay()
                }
            )
            ClipActionItem(
                icon = Icons.Rounded.ContentCut,
                title = "Edit & Trim",
                onClick = {
                    onDismiss()
                    onEditAndTrim()
                }
            )
            ClipActionItem(
                icon = Icons.Rounded.Share,
                title = "Share",
                onClick = {
                    onDismiss()
                    onShare()
                }
            )
            ClipActionItem(
                icon = Icons.Rounded.DriveFileRenameOutline,
                title = "Rename",
                onClick = {
                    onDismiss()
                    onRename()
                }
            )
            ClipActionItem(
                icon = Icons.Rounded.DeleteOutline,
                title = "Delete",
                titleColor = ErrorRed,
                iconTint = ErrorRed,
                onClick = {
                    onDismiss()
                    onDelete()
                }
            )
        }
    }
}

@Composable
private fun ClipActionItem(
    icon: ImageVector,
    title: String,
    titleColor: Color = TextPrimary,
    iconTint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = titleColor
        )
    }
}
