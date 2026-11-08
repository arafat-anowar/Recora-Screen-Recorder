package com.veylqo.recora.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.veylqo.recora.data.model.CountdownTimer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerPickerSheet(
    selectedTimer: CountdownTimer,
    onSelectTimer: (CountdownTimer) -> Unit,
    onDismiss: () -> Unit
) {
    RecoraBottomSheet(
        onDismissRequest = onDismiss,
        title = "Countdown Timer",
        subtitle = "Delay before recording begins"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CountdownTimer.entries.forEach { timer ->
                val subtitle = if (timer.seconds == 0) "Immediate start" else "${timer.seconds} seconds delay"
                SheetOptionItem(
                    title = timer.displayName,
                    subtitle = subtitle,
                    selected = selectedTimer == timer,
                    onClick = {
                        onSelectTimer(timer)
                        onDismiss()
                    }
                )
            }
        }
    }
}
