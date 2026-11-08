package com.veylqo.recora

import android.Manifest
import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.veylqo.recora.data.model.AudioSource
import com.veylqo.recora.data.model.ClipItem
import com.veylqo.recora.data.model.CountdownTimer
import com.veylqo.recora.data.model.RecordUiState
import com.veylqo.recora.data.model.VideoFps
import com.veylqo.recora.data.model.VideoOrientation
import com.veylqo.recora.data.model.VideoResolution
import com.veylqo.recora.data.storage.MediaStoreRepository
import com.veylqo.recora.data.storage.StorageInfo
import com.veylqo.recora.service.RecoraTileService
import com.veylqo.recora.service.RecordingService
import com.veylqo.recora.service.RecordingStatus
import com.veylqo.recora.service.encoder.DeviceCodecInfo
import com.veylqo.recora.ui.components.GalaxyBackground
import com.veylqo.recora.ui.components.GlassNavBar
import com.veylqo.recora.ui.dialogs.DeleteConfirmDialog
import com.veylqo.recora.ui.dialogs.InfoDialog
import com.veylqo.recora.ui.dialogs.OverlayPermissionDialog
import com.veylqo.recora.ui.dialogs.RedeemCodeDialog
import com.veylqo.recora.ui.dialogs.RenameDialog
import com.veylqo.recora.ui.dialogs.TouchSetupDialog
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
import java.util.Locale

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaStoreRepo = remember { MediaStoreRepository(context) }

    var currentTab by remember { mutableStateOf(NavTab.Record) }
    var uiState by remember { mutableStateOf(RecordUiState()) }

    // Service Recording State
    val serviceState by RecordingService.serviceState.collectAsState()
    val isRecording = serviceState.status == RecordingStatus.RECORDING || serviceState.status == RecordingStatus.PAUSED
    val isPaused = serviceState.status == RecordingStatus.PAUSED

    // Countdown state
    var isCountingDown by remember { mutableStateOf(false) }
    var pendingProjectionData by remember { mutableStateOf<Pair<Int, Intent>?>(null) }

    // MediaStore Clips List & Storage info
    val clipList = remember { mutableStateListOf<ClipItem>() }
    var storageInfo by remember { mutableStateOf(StorageInfo("Scanning storage...", 0.35f)) }

    fun refreshRecordings() {
        coroutineScope.launch {
            val items = mediaStoreRepo.queryRecordedVideos()
            clipList.clear()
            clipList.addAll(items)
            storageInfo = mediaStoreRepo.getStorageHeadroom()
        }
    }

    LaunchedEffect(Unit) {
        refreshRecordings()
    }

    // Refresh when recording finishes
    LaunchedEffect(serviceState.status) {
        if (serviceState.status == RecordingStatus.STOPPED) {
            refreshRecordings()
        }
    }

    // Active bottom sheets & dialogs
    var selectedClipForMenu by remember { mutableStateOf<ClipItem?>(null) }
    var clipToDelete by remember { mutableStateOf<ClipItem?>(null) }
    var clipToRename by remember { mutableStateOf<ClipItem?>(null) }

    var showAudioSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showOrientationSheet by remember { mutableStateOf(false) }
    var showTimerSheet by remember { mutableStateOf(false) }
    var showPlanSheet by remember { mutableStateOf(false) }
    var showRedeemDialog by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var showTouchSetupDialog by remember { mutableStateOf(false) }
    var infoDialogData by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Floating toast pill
    var toastMessage by remember { mutableStateOf<String?>(null) }
    fun showToast(message: String) {
        toastMessage = message
        coroutineScope.launch {
            delay(2500)
            if (toastMessage == message) {
                toastMessage = null
            }
        }
    }

    // In-app Overlay Manager for interactive preview & live controls
    val previewOverlayManager = remember {
        com.veylqo.recora.service.overlay.OverlayManager(
            context = context.applicationContext,
            onStopRecording = {
                if (isRecording) {
                    val intent = Intent(context, RecordingService::class.java).apply {
                        action = RecordingService.ACTION_STOP
                    }
                    context.startService(intent)
                } else {
                    uiState = uiState.copy(floatingControls = false)
                }
            },
            onPauseResume = { isPaused ->
                if (isRecording) {
                    val action = if (isPaused) RecordingService.ACTION_PAUSE else RecordingService.ACTION_RESUME
                    val intent = Intent(context, RecordingService::class.java).apply { this.action = action }
                    context.startService(intent)
                }
            },
            onScreenshot = {
                showToast("Screenshot captured")
            },
            onMicMute = { isMuted ->
                showToast(if (isMuted) "Microphone muted" else "Microphone unmuted")
            },
            onPenClosed = {
                uiState = uiState.copy(penAndTools = false)
            },
            onFacecamClosed = {
                uiState = uiState.copy(facecam = false)
            }
        )
    }

    // Floating Countdown Overlay that displays over the mobile home screen!
    val countdownOverlay = remember {
        com.veylqo.recora.service.overlay.FloatingCountdownOverlay(context.applicationContext)
    }

    fun goToHomeScreen() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
        } catch (e: Exception) {}
    }

    // Observe activity lifecycle to sync touches setting and prevent window leaks
    androidx.compose.runtime.DisposableEffect(context) {
        val activity = context as? ComponentActivity
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val touchesOn = try {
                    Settings.System.getInt(context.contentResolver, "show_touches", 0) == 1
                } catch (e: Exception) {
                    false
                }
                uiState = uiState.copy(showTouches = touchesOn)

                // Force refresh Quick Settings tile icon on Android 7.0+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        android.service.quicksettings.TileService.requestListeningState(
                            context,
                            ComponentName(context, RecoraTileService::class.java)
                        )
                    } catch (e: Exception) {}
                }
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
            previewOverlayManager.removeAllOverlays()
            countdownOverlay.remove()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            uiState = uiState.copy(facecam = true)
            showToast("Facecam: ON")
        } else {
            showToast("Camera permission required for Facecam")
        }
    }

    // Launch Service with projection intent
    fun executeStartService(resultCode: Int, resultData: Intent) {
        previewOverlayManager.removeAllOverlays()

        val (w, h) = when (uiState.resolution) {
            VideoResolution.Native -> {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val bounds = wm.currentWindowMetrics.bounds
                    bounds.width() to bounds.height()
                } else {
                    val dm = android.util.DisplayMetrics()
                    @Suppress("DEPRECATION")
                    wm.defaultDisplay.getRealMetrics(dm)
                    dm.widthPixels to dm.heightPixels
                }
            }
            VideoResolution.UHD -> 3840 to 2160
            VideoResolution.QHD -> 2560 to 1440
            VideoResolution.FHD -> 1920 to 1080
        }
        val (finalW, finalH) = when (uiState.orientation) {
            VideoOrientation.Landscape -> maxOf(w, h) to minOf(w, h)
            VideoOrientation.Portrait -> minOf(w, h) to maxOf(w, h)
            VideoOrientation.Auto -> {
                val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                if (isLandscape) maxOf(w, h) to minOf(w, h) else minOf(w, h) to maxOf(w, h)
            }
        }

        val fpsVal = if (uiState.fps == VideoFps.FPS_60) 60 else 30

        val serviceIntent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_RESULT_CODE, resultCode)
            putExtra(RecordingService.EXTRA_RESULT_DATA, resultData)
            putExtra(RecordingService.EXTRA_AUDIO_SOURCE, uiState.audioSource.name)
            putExtra(RecordingService.EXTRA_WIDTH, finalW)
            putExtra(RecordingService.EXTRA_HEIGHT, finalH)
            putExtra(RecordingService.EXTRA_FPS, fpsVal)
            putExtra(RecordingService.EXTRA_BITRATE, uiState.bitrate.bps)
            putExtra(RecordingService.EXTRA_QUALITY_LABEL, "${uiState.resolution.label} · ${uiState.fps.label}")
            putExtra(RecordingService.EXTRA_AUDIO_LABEL, uiState.audioSource.displayName)
            putExtra(RecordingService.EXTRA_ENABLE_CONTROLS, uiState.floatingControls)
            putExtra(RecordingService.EXTRA_ENABLE_PEN, uiState.penAndTools)
            putExtra(RecordingService.EXTRA_ENABLE_FACECAM, uiState.facecam)
        }

        ContextCompat.startForegroundService(context, serviceIntent)
        showToast("Screen recording started")
    }

    // Projection consent launcher
    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resCode = result.resultCode
            val resData = result.data!!

            if (uiState.timer == CountdownTimer.Off) {
                goToHomeScreen()
                executeStartService(resCode, resData)
            } else {
                if (Settings.canDrawOverlays(context)) {
                    // Minimize Recora to the mobile home screen and show floating countdown on top!
                    goToHomeScreen()
                    countdownOverlay.startCountdown(
                        initialSeconds = uiState.timer.seconds,
                        onComplete = {
                            executeStartService(resCode, resData)
                        },
                        onCancel = {
                            showToast("Recording cancelled")
                        }
                    )
                } else {
                    pendingProjectionData = resCode to resData
                    isCountingDown = true
                }
            }
        } else {
            showToast("Screen recording permission was cancelled")
        }
    }

    // Permission launcher for Audio and Notifications
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val audioGranted = grants[Manifest.permission.RECORD_AUDIO] ?: true
        val projManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(projManager.createScreenCaptureIntent())
    }

    fun requestRecordingStart() {
        if (!Settings.canDrawOverlays(context)) {
            showOverlayPermissionDialog = true
            return
        }
        val neededPermissions = mutableListOf<String>()
        if (uiState.audioSource != AudioSource.None) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.RECORD_AUDIO)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (neededPermissions.isNotEmpty()) {
            permissionsLauncher.launch(neededPermissions.toTypedArray())
        } else {
            val projManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(projManager.createScreenCaptureIntent())
        }
    }

    val durationFormatted = remember(serviceState.durationSeconds) {
        val min = serviceState.durationSeconds / 60
        val sec = serviceState.durationSeconds % 60
        String.format(Locale.US, "%02d:%02d", min, sec)
    }

    GalaxyBackground(reduceMotion = isRecording || !uiState.smoothAnimations) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isCountingDown) {
                CountdownScreen(
                    initialSeconds = uiState.timer.seconds,
                    onCountdownComplete = {
                        isCountingDown = false
                        pendingProjectionData?.let { (code, data) ->
                            goToHomeScreen()
                            executeStartService(code, data)
                        }
                        pendingProjectionData = null
                    },
                    onCancel = {
                        isCountingDown = false
                        pendingProjectionData = null
                        showToast("Recording cancelled")
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            if (targetState.ordinal > initialState.ordinal) {
                                (slideInHorizontally { width -> (width * 0.12f).toInt() } + fadeIn(tween(220)))
                                    .togetherWith(slideOutHorizontally { width -> (-width * 0.12f).toInt() } + fadeOut(tween(180)))
                            } else {
                                (slideInHorizontally { width -> (-width * 0.12f).toInt() } + fadeIn(tween(220)))
                                    .togetherWith(slideOutHorizontally { width -> (width * 0.12f).toInt() } + fadeOut(tween(180)))
                            }
                        },
                        label = "TabContentAnimation"
                    ) { tab ->
                        when (tab) {
                        NavTab.Record -> {
                            RecordScreen(
                                state = uiState,
                                isRecording = isRecording,
                                recordingDurationText = durationFormatted,
                                onAudioClick = { showAudioSheet = true },
                                onQualityClick = { showQualitySheet = true },
                                onOrientationClick = { showOrientationSheet = true },
                                onTimerClick = { showTimerSheet = true },
                                onToggleControls = { enabled ->
                                    if (enabled) {
                                        if (!Settings.canDrawOverlays(context)) {
                                            showOverlayPermissionDialog = true
                                        } else {
                                            uiState = uiState.copy(floatingControls = true)
                                            showToast("Floating Controls: ON")
                                        }
                                    } else {
                                        uiState = uiState.copy(floatingControls = false)
                                        showToast("Floating Controls: OFF")
                                    }
                                },
                                onTogglePen = { enabled ->
                                    if (enabled) {
                                        if (!Settings.canDrawOverlays(context)) {
                                            showOverlayPermissionDialog = true
                                        } else {
                                            uiState = uiState.copy(penAndTools = true)
                                            showToast("Pen & Tools: ON")
                                        }
                                    } else {
                                        uiState = uiState.copy(penAndTools = false)
                                        showToast("Pen & Tools: OFF")
                                    }
                                },
                                onToggleFacecam = { enabled ->
                                    if (enabled) {
                                        if (!Settings.canDrawOverlays(context)) {
                                            showOverlayPermissionDialog = true
                                        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        } else {
                                            uiState = uiState.copy(facecam = true)
                                            showToast("Facecam: ON")
                                        }
                                    } else {
                                        uiState = uiState.copy(facecam = false)
                                        showToast("Facecam: OFF")
                                    }
                                },
                                onToggleTouches = {
                                    val currentlyOn = try {
                                        Settings.System.getInt(context.contentResolver, "show_touches", 0) == 1
                                    } catch (e: Exception) {
                                        false
                                    }
                                    if (Settings.System.canWrite(context)) {
                                        try {
                                            Settings.System.putInt(context.contentResolver, "show_touches", if (currentlyOn) 0 else 1)
                                            val newState = !currentlyOn
                                            uiState = uiState.copy(showTouches = newState)
                                            showToast(if (newState) "Touches enabled" else "Touches disabled")
                                        } catch (e: Exception) {
                                            showTouchSetupDialog = true
                                        }
                                    } else {
                                        showTouchSetupDialog = true
                                    }
                                },
                                onStartRecordingClick = {
                                    if (isRecording) {
                                        showToast("Recording already in progress")
                                    } else {
                                        requestRecordingStart()
                                    }
                                },
                                onPauseResumeClick = {
                                    val action = if (isPaused) RecordingService.ACTION_RESUME else RecordingService.ACTION_PAUSE
                                    val intent = Intent(context, RecordingService::class.java).apply { this.action = action }
                                    context.startService(intent)
                                    showToast(if (isPaused) "Recording resumed" else "Recording paused")
                                },
                                onStopRecordingClick = {
                                    val intent = Intent(context, RecordingService::class.java).apply {
                                        action = RecordingService.ACTION_STOP
                                    }
                                    context.startService(intent)
                                    showToast("Stopping recording...")
                                },
                                onUpgradeClick = { showPlanSheet = true }
                            )
                        }

                        NavTab.Recordings -> {
                            RecordingsScreen(
                                clips = clipList,
                                storageInfo = storageInfo,
                                onClipMenuClick = { clip -> selectedClipForMenu = clip },
                                onClipPlayClick = { clip ->
                                    val uriStr = clip.uriString
                                    if (uriStr != null) {
                                        try {
                                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(Uri.parse(uriStr), "video/mp4")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(viewIntent)
                                        } catch (e: Exception) {
                                            showToast("No compatible video player found")
                                        }
                                    } else {
                                        showToast("Video file not available")
                                    }
                                }
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
                                    if (title == "Device Compatibility") {
                                        val realCodecInfo = DeviceCodecInfo.getDeviceCodecSummary()
                                        infoDialogData = title to realCodecInfo
                                    } else {
                                        infoDialogData = title to message
                                    }
                                }
                            )
                        }
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

            // Floating Toast Pill Banner
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
                val (nativeW, nativeH) = remember {
                    val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val bounds = wm.currentWindowMetrics.bounds
                        bounds.width() to bounds.height()
                    } else {
                        val dm = android.util.DisplayMetrics()
                        @Suppress("DEPRECATION")
                        wm.defaultDisplay.getRealMetrics(dm)
                        dm.widthPixels to dm.heightPixels
                    }
                }
                QualityPickerSheet(
                    selectedResolution = uiState.resolution,
                    selectedFps = uiState.fps,
                    selectedBitrate = uiState.bitrate,
                    isProUser = uiState.isProUser,
                    nativeResolutionText = "${minOf(nativeW, nativeH)} × ${maxOf(nativeW, nativeH)} (Native 1:1)",
                    onSelectResolution = { uiState = uiState.copy(resolution = it) },
                    onSelectFps = { uiState = uiState.copy(fps = it) },
                    onSelectBitrate = { uiState = uiState.copy(bitrate = it) },
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
                    onPlay = {
                        val uriStr = clip.uriString
                        if (uriStr != null) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(uriStr), "video/mp4")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                showToast("No compatible video player found")
                            }
                        }
                    },
                    onEditAndTrim = { showToast("Opening editor for: ${clip.title}") },
                    onShare = {
                        val uriStr = clip.uriString
                        if (uriStr != null) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "video/mp4"
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(uriStr))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share recording"))
                        }
                    },
                    onRename = { clipToRename = clip },
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
                        val uriStr = clip.uriString
                        if (uriStr != null) {
                            coroutineScope.launch {
                                val success = mediaStoreRepo.deleteVideo(Uri.parse(uriStr))
                                if (success) {
                                    showToast("Deleted ${clip.title}")
                                    refreshRecordings()
                                } else {
                                    showToast("Could not delete file")
                                }
                            }
                        }
                        clipToDelete = null
                    },
                    onDismiss = { clipToDelete = null }
                )
            }

            clipToRename?.let { clip ->
                RenameDialog(
                    initialName = clip.title,
                    onConfirm = { newName ->
                        val uriStr = clip.uriString
                        if (uriStr != null) {
                            coroutineScope.launch {
                                val success = mediaStoreRepo.renameVideo(Uri.parse(uriStr), newName)
                                if (success) {
                                    showToast("Renamed to $newName")
                                    refreshRecordings()
                                } else {
                                    showToast("Could not rename file")
                                }
                            }
                        }
                        clipToRename = null
                    },
                    onDismiss = { clipToRename = null }
                )
            }

            infoDialogData?.let { (title, msg) ->
                InfoDialog(
                    title = title,
                    message = msg,
                    onDismiss = { infoDialogData = null }
                )
            }

            if (showOverlayPermissionDialog) {
                OverlayPermissionDialog(
                    context = context,
                    onDismiss = { showOverlayPermissionDialog = false }
                )
            }

            if (showTouchSetupDialog) {
                TouchSetupDialog(
                    context = context,
                    onDismiss = { showTouchSetupDialog = false }
                )
            }
        }
    }
}
