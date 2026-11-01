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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.core.model.AudioSource
import com.veylqo.recora.core.model.FpsOption
import com.veylqo.recora.core.model.OrientationOption
import com.veylqo.recora.core.model.ResolutionPreset
import com.veylqo.recora.core.model.TimerPreset
import com.veylqo.recora.ui.theme.CosmicBorder
import com.veylqo.recora.ui.theme.CosmicSurface
import com.veylqo.recora.ui.theme.TextPrimary
import com.veylqo.recora.ui.theme.TextSecondary
import com.veylqo.recora.ui.theme.VioletPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSelectionSheet(
    selectedSource: AudioSource,
    onSelect: (AudioSource) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF100C2A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            SheetHeader(title = "Audio Source", onDismiss = onDismiss)
            Spacer(modifier = Modifier.height(16.dp))

            AudioSource.entries.forEach { source ->
                SheetOptionItem(
                    title = source.displayName,
                    subtitle = source.description,
                    isSelected = source == selectedSource,
                    onClick = {
                        onSelect(source)
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelectionSheet(
    selectedPreset: ResolutionPreset,
    selectedFps: FpsOption,
    onSelectPreset: (ResolutionPreset) -> Unit,
    onSelectFps: (FpsOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF100C2A)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            item {
                SheetHeader(title = "Resolution & Quality", onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Resolution Presets",
                    color = VioletPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(ResolutionPreset.entries) { preset ->
                SheetOptionItem(
                    title = preset.displayName,
                    subtitle = if (preset == ResolutionPreset.NATIVE) "Device native display resolution" else "${preset.width} x ${preset.height}",
                    isSelected = preset == selectedPreset,
                    onClick = { onSelectPreset(preset) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Frame Rate (FPS)",
                    color = VioletPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FpsOption.entries.forEach { fps ->
                        val isSelected = fps == selectedFps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) VioletPrimary else CosmicBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .background(
                                    color = if (isSelected) Color(0xFF281E57) else CosmicSurface,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectFps(fps) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${fps.value} FPS",
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrientationSelectionSheet(
    selectedOrientation: OrientationOption,
    onSelect: (OrientationOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF100C2A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            SheetHeader(title = "Screen Orientation", onDismiss = onDismiss)
            Spacer(modifier = Modifier.height(16.dp))

            OrientationOption.entries.forEach { option ->
                SheetOptionItem(
                    title = option.displayName,
                    subtitle = when (option) {
                        OrientationOption.PORTRAIT -> "Locks recording video to portrait aspect ratio"
                        OrientationOption.LANDSCAPE -> "Locks recording video to landscape aspect ratio"
                        OrientationOption.AUTO -> "Matches current active device orientation"
                    },
                    isSelected = option == selectedOrientation,
                    onClick = {
                        onSelect(option)
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSelectionSheet(
    selectedTimer: TimerPreset,
    customSeconds: Int,
    onSelectTimer: (TimerPreset) -> Unit,
    onCustomSecondsChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderVal by remember { mutableFloatStateOf(customSeconds.toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF100C2A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            SheetHeader(title = "Countdown Timer", onDismiss = onDismiss)
            Spacer(modifier = Modifier.height(16.dp))

            TimerPreset.entries.forEach { preset ->
                if (preset != TimerPreset.CUSTOM) {
                    SheetOptionItem(
                        title = preset.displayName,
                        subtitle = if (preset.seconds == 0) "Starts recording immediately upon tap" else "Wait ${preset.seconds} seconds before capture begins",
                        isSelected = preset == selectedTimer,
                        onClick = {
                            onSelectTimer(preset)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Custom timer option
            val isCustom = selectedTimer == TimerPreset.CUSTOM
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isCustom) VioletPrimary else CosmicBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(
                        color = if (isCustom) Color(0xFF231A4D) else CosmicSurface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectTimer(TimerPreset.CUSTOM) }
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom (${sliderVal.toInt()}s)",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Choose custom delay between 0 and 15 seconds",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (isCustom) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = VioletPrimary
                            )
                        }
                    }

                    if (isCustom) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = sliderVal,
                            onValueChange = {
                                sliderVal = it
                                onCustomSecondsChange(it.toInt())
                            },
                            valueRange = 0f..15f,
                            steps = 14,
                            colors = SliderDefaults.colors(
                                thumbColor = VioletPrimary,
                                activeTrackColor = VioletPrimary,
                                inactiveTrackColor = CosmicBorder
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SheetHeader(title: String, onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SheetOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSelected) VioletPrimary else CosmicBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                color = if (isSelected) Color(0xFF231A4D) else CosmicSurface,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = VioletPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
