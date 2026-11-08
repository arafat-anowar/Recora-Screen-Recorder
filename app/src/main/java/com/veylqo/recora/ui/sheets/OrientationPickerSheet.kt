package com.veylqo.recora.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.veylqo.recora.data.model.VideoOrientation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrientationPickerSheet(
    selectedOrientation: VideoOrientation,
    onSelectOrientation: (VideoOrientation) -> Unit,
    onDismiss: () -> Unit
) {
    RecoraBottomSheet(
        onDismissRequest = onDismiss,
        title = "Video Orientation",
        subtitle = "Choose screen locking direction"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            VideoOrientation.entries.forEach { orientation ->
                SheetOptionItem(
                    title = orientation.displayName,
                    subtitle = orientation.subtitle,
                    selected = selectedOrientation == orientation,
                    onClick = {
                        onSelectOrientation(orientation)
                        onDismiss()
                    }
                )
            }
        }
    }
}
