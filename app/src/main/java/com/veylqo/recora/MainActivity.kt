package com.veylqo.recora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.data.model.ClipItem
import com.veylqo.recora.data.model.CountdownTimer
import com.veylqo.recora.data.model.RecordUiState
import com.veylqo.recora.data.model.sampleClips
import com.veylqo.recora.ui.components.GalaxyBackground
import com.veylqo.recora.ui.components.GlassNavBar
import com.veylqo.recora.ui.dialogs.DeleteConfirmDialog
import com.veylqo.recora.ui.dialogs.InfoDialog
import com.veylqo.recora.ui.dialogs.RedeemCodeDialog
import com.veylqo.recora.ui.navigation.NavTab
import com.veylqo.recora.ui.screens.more.MoreScreen
import com.veylqo.recora.ui.screens.record.CountdownScreen
import com.veylqo.recora.ui.screens.record.RecordScreen
import com.veylqo.recora.ui.screens.recordings.RecordingsScreen
import com.veylqo.recora.ui.sheets.AudioPickerSheet
import com.veylqo.recora.ui.sheets.ClipActionSheet
import com.veylqo.recora.ui.sheets.OrientationPickerSheet
import com.veylqo.recora.ui.sheets.PlanSheet
import com.veylqo.recora.ui.sheets.QualityPickerSheet
import com.veylqo.recora.ui.sheets.TimerPickerSheet
import com.veylqo.recora.ui.theme.CardBorder
import com.veylqo.recora.ui.theme.GlassNavBg
import com.veylqo.recora.ui.theme.PillCornerRadius
import com.veylqo.recora.ui.theme.RecoraTheme
import com.veylqo.recora.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecoraTheme {
                RecoraApp()
            }
        }
    }
}

@Composable
fun RecoraApp() {
    var currentTab by remember { mutableStateOf(NavTab.Record) }
    var uiState by remember { mutableStateOf(RecordUiState()) }

    // Recording and Countdown State
    var isCountingDown by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    // Recorded Clips State
    val clipList = remember { mutableStateListOf<ClipItem>().apply { addAll(sampleClips) } }
    var selectedClipForMenu by remember { mutableStateOf<ClipItem?>(null) }
    var clipToDelete by remember { mutableStateOf<ClipItem?>(null) }

    // Sheet visibility states
    var showAudioSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showOrientationSheet by remember { mutableStateOf(false) }
    var showTimerSheet by remember { mutableStateOf(false) }
    var showPlanSheet by remember { mutableStateOf(false) }
    var showRedeemDialog by remember { mutableStateOf(false) }

    // Info dialog state
    var infoDialogData by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Floating toast pill
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun showToast(message: String) {
        toastMessage = message
        coroutineScope.launch {
            delay(2500)
            if (toastMessage == message) {
                toastMessage = null
            }
        }
    }

    // Recording duration timer
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording && !isPaused) {
            while (true) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    val durationFormatted = remember(recordingSeconds) {
        val minutes = recordingSeconds / 60
        val seconds = recordingSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    GalaxyBackground(reduceMotion = !uiState.smoothAnimations) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isCountingDown) {
                CountdownScreen(
                    initialSeconds = uiState.timer.seconds,
                    onCountdownComplete = {
                        isCountingDown = false
                        isRecording = true
                        recordingSeconds = 0
                        isPaused = false
                        showToast("Recording started")
                    },
                    onCancel = {
                        isCountingDown = false
                    }
                )
            } else {
                // Main Content Screen
                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentTab) {
                        NavTab.Record -> {
                            RecordScreen(
                                state = uiState,
                                isRecording = isRecording,
                                recordingDurationText = durationFormatted,
                                onAudioClick = { showAudioSheet = true },
                                onQualityClick = { showQualitySheet = true },
                                onOrientationClick = { showOrientationSheet = true },
                                onTimerClick = { showTimerSheet = true },
                                onToggleControls = { uiState = uiState.copy(floatingControls = it) },
                                onTogglePen = { uiState = uiState.copy(penAndTools = it) },
                                onToggleFacecam = { uiState = uiState.copy(facecam = it) },
                                onToggleTouches = { uiState = uiState.copy(showTouches = it) },
                                onStartRecordingClick = {
                                    if (isRecording) {
                                        showToast("Recording is already in progress")
                                    } else {
                                        if (uiState.timer == CountdownTimer.Off) {
                                            isRecording = true
                                            recordingSeconds = 0
                                            isPaused = false
                                            showToast("Recording started")
                                        } else {
                                            isCountingDown = true
                                        }
                                    }
                                },
                                onPauseResumeClick = {
                                    isPaused = !isPaused
                                    showToast(if (isPaused) "Recording paused" else "Recording resumed")
                                },
                                onStopRecordingClick = {
                                    isRecording = false
                                    val newTitle = "Recora_${System.currentTimeMillis() / 1000}.mp4"
                                    clipList.add(
                                        0,
                                        ClipItem(
                                            id = System.currentTimeMillis().toString(),
                                            title = newTitle,
                                            durationText = durationFormatted,
                                            resolutionBadge = uiState.resolution.label,
                                            fpsBadge = uiState.fps.label,
                                            sizeText = "${((recordingSeconds * 0.8f) + 1.2f).toInt()} MB",
                                            dateText = "Just now"
                                        )
                                    )
                                    recordingSeconds = 0
                                    showToast("Recording saved: $newTitle")
                                },
                                onUpgradeClick = { showPlanSheet = true }
                            )
                        }

                        NavTab.Recordings -> {
                            RecordingsScreen(
                                clips = clipList,
                                onClipMenuClick = { clip -> selectedClipForMenu = clip },
                                onClipPlayClick = { clip -> showToast("Playing: ${clip.title}") }
                            )
                        }

                        NavTab.More -> {
                            MoreScreen(
                                state = uiState,
                                onToggleHaptic = { uiState = uiState.copy(hapticFeedback = it) },
                                onToggleSmoothAnimations = { uiState = uiState.copy(smoothAnimations = it) },
                                onRestorePurchases = {
                                    showToast("Purchases restored")
                                    uiState = uiState.copy(isProUser = true)
                                },
                                onRedeemCode = { showRedeemDialog = true },
                                onSelectMonthly = {
                                    uiState = uiState.copy(isProUser = true)
                                    showToast("Subscribed to Monthly Pro Plan")
                                },
                                onSelectLifetime = { showPlanSheet = true },
                                onOpenInfoDialog = { title, message ->
                                    infoDialogData = title to message
                                }
                            )
                        }
                    }

                    // Floating Glass Bottom Navigation Bar
                    GlassNavBar(
                        selectedTab = currentTab,
                        onTabSelected = { currentTab = it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            // Floating Toast Banner (like "Purchases restored" pill in design)
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
            ) {
                toastMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(PillCornerRadius))
                            .background(GlassNavBg)
                            .border(1.dp, CardBorder, RoundedCornerShape(PillCornerRadius))
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = msg,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Bottom Sheets
            if (showAudioSheet) {
                AudioPickerSheet(
                    selectedSource = uiState.audioSource,
                    onSelectSource = { uiState = uiState.copy(audioSource = it) },
                    onDismiss = { showAudioSheet = false }
                )
            }

            if (showQualitySheet) {
                QualityPickerSheet(
                    selectedResolution = uiState.resolution,
                    selectedFps = uiState.fps,
                    isProUser = uiState.isProUser,
                    onSelectResolution = { uiState = uiState.copy(resolution = it) },
                    onSelectFps = { uiState = uiState.copy(fps = it) },
                    onShowPlan = { showPlanSheet = true },
                    onDismiss = { showQualitySheet = false }
                )
            }

            if (showOrientationSheet) {
                OrientationPickerSheet(
                    selectedOrientation = uiState.orientation,
                    onSelectOrientation = { uiState = uiState.copy(orientation = it) },
                    onDismiss = { showOrientationSheet = false }
                )
            }

            if (showTimerSheet) {
                TimerPickerSheet(
                    selectedTimer = uiState.timer,
                    onSelectTimer = { uiState = uiState.copy(timer = it) },
                    onDismiss = { showTimerSheet = false }
                )
            }

            selectedClipForMenu?.let { clip ->
                ClipActionSheet(
                    clip = clip,
                    onPlay = { showToast("Playing: ${clip.title}") },
                    onEditAndTrim = { showToast("Opening editor for: ${clip.title}") },
                    onShare = { showToast("Sharing: ${clip.title}") },
                    onRename = { showToast("Rename ${clip.title}") },
                    onDelete = { clipToDelete = clip },
                    onDismiss = { selectedClipForMenu = null }
                )
            }

            if (showPlanSheet) {
                PlanSheet(
                    onContinue = {
                        uiState = uiState.copy(isProUser = true)
                        showToast("Unlocked Lifetime Pro!")
                    },
                    onRestore = {
                        uiState = uiState.copy(isProUser = true)
                        showToast("Purchases restored")
                    },
                    onDismiss = { showPlanSheet = false }
                )
            }

            // Dialogs
            if (showRedeemDialog) {
                RedeemCodeDialog(
                    onDismiss = { showRedeemDialog = false },
                    onRedeem = { code ->
                        uiState = uiState.copy(isProUser = true)
                        showToast("Promo Code applied: $code")
                    }
                )
            }

            clipToDelete?.let { clip ->
                DeleteConfirmDialog(
                    clipTitle = clip.title,
                    onConfirm = {
                        clipList.remove(clip)
                        clipToDelete = null
                        showToast("Deleted ${clip.title}")
                    },
                    onDismiss = { clipToDelete = null }
                )
            }

            infoDialogData?.let { (title, msg) ->
                InfoDialog(
                    title = title,
                    message = msg,
                    onDismiss = { infoDialogData = null }
                )
            }
        }
    }
}
