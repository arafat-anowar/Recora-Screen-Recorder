package com.veylqo.recora.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.core.model.AudioSource
import com.veylqo.recora.core.model.FpsOption
import com.veylqo.recora.core.model.OrientationOption
import com.veylqo.recora.core.model.RecordingState
import com.veylqo.recora.core.model.ResolutionPreset
import com.veylqo.recora.core.model.TimerPreset
import com.veylqo.recora.data.preferences.RecoraSettings
import com.veylqo.recora.ui.components.AudioSelectionSheet
import com.veylqo.recora.ui.components.CountdownOverlay
import com.veylqo.recora.ui.components.CrownBadge
import com.veylqo.recora.ui.components.LowStorageWarningBanner
import com.veylqo.recora.ui.components.OptionSelectionSheets
import com.veylqo.recora.ui.components.OrientationSelectionSheet
import com.veylqo.recora.ui.components.PromoBanner
import com.veylqo.recora.ui.components.QualitySelectionSheet
import com.veylqo.recora.ui.components.QuickOptionCardsGrid
import com.veylqo.recora.ui.components.QuickTogglesGrid
import com.veylqo.recora.ui.components.RecordButton
import com.veylqo.recora.ui.components.TimerSelectionSheet
import com.veylqo.recora.ui.theme.TextMuted
import com.veylqo.recora.ui.theme.TextPrimary
import com.veylqo.recora.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    settings: RecoraSettings,
    recordingState: RecordingState,
    countdownRemaining: Int,
    freeStorageBytes: Long,
    onRecordButtonClick: () -> Unit,
    onCancelCountdown: () -> Unit,
    onAudioSourceChange: (AudioSource) -> Unit,
    onResolutionPresetChange: (ResolutionPreset) -> Unit,
    onFpsOptionChange: (FpsOption) -> Unit,
    onOrientationOptionChange: (OrientationOption) -> Unit,
    onTimerPresetChange: (TimerPreset) -> Unit,
    onCustomTimerSecondsChange: (Int) -> Unit,
    onControlsToggle: (Boolean) -> Unit,
    onPenToolsToggle: (Boolean) -> Unit,
    onFacecamToggle: (Boolean) -> Unit,
    onTouchesToggle: (Boolean) -> Unit,
    onDismissPromo: () -> Unit,
    onCrownBadgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAudioSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showOrientationSheet by remember { mutableStateOf(false) }
    var showTimerSheet by remember { mutableStateOf(false) }

    val freeMb = freeStorageBytes / (1024 * 1024)
    val showStorageWarning = freeMb in 1..200

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Recora, Ready when you are, CrownBadge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recora",
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ready when you are",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                CrownBadge(
                    entitlement = settings.entitlement,
                    onClick = onCrownBadgeClick
                )
            }

            // Low storage warning if below 200MB
            if (showStorageWarning) {
                Spacer(modifier = Modifier.height(14.dp))
                LowStorageWarningBanner(freeMb = freeMb)
            }

            // Remote Promo Banner if not dismissed
            if (!settings.promoBannerDismissed && !settings.entitlement.isPremium) {
                Spacer(modifier = Modifier.height(14.dp))
                PromoBanner(
                    onDismiss = onDismissPromo,
                    onClick = onCrownBadgeClick
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Center Big Circular Record Button
            RecordButton(
                recordingState = recordingState,
                onClick = onRecordButtonClick
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 2x2 Quick Option Cards (Audio, Quality, Orientation, Timer)
            QuickOptionCardsGrid(
                audioSource = settings.audioSource,
                resolutionPreset = settings.resolutionPreset,
                fpsOption = settings.fpsOption,
                orientationOption = settings.orientationOption,
                timerPreset = settings.timerPreset,
                onAudioClick = { showAudioSheet = true },
                onQualityClick = { showQualitySheet = true },
                onOrientationClick = { showOrientationSheet = true },
                onTimerClick = { showTimerSheet = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2x2 Quick Toggles (Controls, Pen & Tools, Facecam, Touches)
            QuickTogglesGrid(
                showControls = settings.showControls,
                showPenTools = settings.showPenTools,
                showFacecam = settings.showFacecam,
                showTouches = settings.showTouches,
                onControlsToggle = onControlsToggle,
                onPenToolsToggle = onPenToolsToggle,
                onFacecamToggle = onFacecamToggle,
                onTouchesToggle = onTouchesToggle
            )
        }

        // Active Countdown Overlay
        if (countdownRemaining > 0) {
            CountdownOverlay(
                secondsRemaining = countdownRemaining,
                onCancel = onCancelCountdown
            )
        }

        // Option Bottom Sheets
        if (showAudioSheet) {
            AudioSelectionSheet(
                selectedSource = settings.audioSource,
                onSelect = onAudioSourceChange,
                onDismiss = { showAudioSheet = false }
            )
        }

        if (showQualitySheet) {
            QualitySelectionSheet(
                selectedPreset = settings.resolutionPreset,
                selectedFps = settings.fpsOption,
                onSelectPreset = onResolutionPresetChange,
                onSelectFps = onFpsOptionChange,
                onDismiss = { showQualitySheet = false }
            )
        }

        if (showOrientationSheet) {
            OrientationSelectionSheet(
                selectedOrientation = settings.orientationOption,
                onSelect = onOrientationOptionChange,
                onDismiss = { showOrientationSheet = false }
            )
        }

        if (showTimerSheet) {
            TimerSelectionSheet(
                selectedTimer = settings.timerPreset,
                customSeconds = settings.customTimerSeconds,
                onSelectTimer = onTimerPresetChange,
                onCustomSecondsChange = onCustomTimerSecondsChange,
                onDismiss = { showTimerSheet = false }
            )
        }
    }
}
