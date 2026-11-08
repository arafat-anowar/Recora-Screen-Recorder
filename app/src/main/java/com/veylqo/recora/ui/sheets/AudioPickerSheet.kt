package com.veylqo.recora.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.veylqo.recora.data.model.AudioSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPickerSheet(
    selectedSource: AudioSource,
    onSelectSource: (AudioSource) -> Unit,
    onDismiss: () -> Unit
) {
    RecoraBottomSheet(
        onDismissRequest = onDismiss,
        title = "Audio Source",
        subtitle = "Select which audio track to capture during recording"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AudioSource.entries.forEach { source ->
                SheetOptionItem(
                    title = source.displayName,
                    subtitle = source.subtitle,
                    selected = selectedSource == source,
                    onClick = {
                        onSelectSource(source)
                        onDismiss()
                    }
                )
            }
        }
    }
}
