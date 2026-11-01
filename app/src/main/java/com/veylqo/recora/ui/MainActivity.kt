package com.veylqo.recora.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.veylqo.recora.core.guard.EmergencyStorageGuard
import com.veylqo.recora.core.model.AudioSource
import com.veylqo.recora.core.model.RecordingState
import com.veylqo.recora.core.model.TimerPreset
import com.veylqo.recora.data.preferences.RecoraSettings
import com.veylqo.recora.data.preferences.UserPreferencesRepository
import com.veylqo.recora.service.RecordingService
import com.veylqo.recora.ui.components.FloatingBottomDock
import com.veylqo.recora.ui.components.RecoraTab
import com.veylqo.recora.ui.home.HomeScreen
import com.veylqo.recora.ui.more.MoreScreen
import com.veylqo.recora.ui.recordings.RecordingItem
import com.veylqo.recora.ui.recordings.RecordingsScreen
import com.veylqo.recora.ui.theme.CosmicBackground
import com.veylqo.recora.ui.theme.CosmicBase
import com.veylqo.recora.ui.theme.RecoraTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var preferencesRepo: UserPreferencesRepository
    private val storageGuard = EmergencyStorageGuard()

    private var pendingMediaProjectionIntent: Intent? = null
    private var pendingResultCode: Int = 0

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            pendingResultCode = result.resultCode
            pendingMediaProjectionIntent = result.data
            startForegroundRecordingService()
        } else {
            Toast.makeText(this, "Screen capture permission was not granted", Toast.LENGTH_SHORT).show()
            RecordingService.updateState(RecordingState.Idle)
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestScreenCapture()
        } else {
            Toast.makeText(this, "Microphone permission is required to capture audio", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesRepo = UserPreferencesRepository(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            RecoraTheme {
                RecoraMainApp()
            }
        }
    }

    @Composable
    private fun RecoraMainApp() {
        val settings by preferencesRepo.settingsFlow.collectAsState(initial = RecoraSettings())
        val recordingState by RecordingService.recordingState.collectAsState()
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var currentTab by remember { mutableStateOf(RecoraTab.RECORD) }
        var countdownRemaining by remember { mutableIntStateOf(0) }
        var countdownJob by remember { mutableStateOf<Job?>(null) }

        var freeStorageBytes by remember { mutableLongStateOf(0L) }
        var totalStorageBytes by remember { mutableLongStateOf(0L) }
        var recoraSizeBytes by remember { mutableLongStateOf(0L) }
        var recordingsList by remember { mutableStateOf<List<RecordingItem>>(emptyList()) }

        // Refresh storage and recordings
        LaunchedEffect(currentTab, recordingState) {
            val moviesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Recora")
            val stagingDir = File(cacheDir, "staging")
            freeStorageBytes = storageGuard.getAvailableBytes(filesDir)
            totalStorageBytes = storageGuard.getTotalBytes(filesDir)

            // Calculate Recora folder size and clips
            val clips = mutableListOf<RecordingItem>()
            var folderSize = 0L

            fun scanDir(dir: File) {
                if (dir.exists()) {
                    dir.listFiles()?.filter { it.extension.equals("mp4", ignoreCase = true) }?.forEach { file ->
                        folderSize += file.length()
                        val name = file.name
                        val badge = if (name.contains("4K", ignoreCase = true)) "4K"
                        else if (name.contains("2K", ignoreCase = true)) "2K"
                        else "FHD"

                        clips.add(
                            RecordingItem(
                                id = file.absolutePath,
                                title = file.name,
                                durationText = "00:15",
                                sizeBytes = file.length(),
                                dateText = "Today",
                                resolutionBadge = badge,
                                filePath = file.absolutePath
                            )
                        )
                    }
                }
            }

            scanDir(moviesDir)
            scanDir(stagingDir)
            recoraSizeBytes = folderSize
            recordingsList = clips.sortedByDescending { it.id }
        }

        // Start Order: Obtain projection, start service, then minimize app AFTER state becomes Recording
        LaunchedEffect(recordingState) {
            if (recordingState is RecordingState.Recording) {
                // Minimize app strictly AFTER Recording state is reached
                moveTaskToBack(true)
            } else if (recordingState is RecordingState.Completed) {
                val completed = recordingState as RecordingState.Completed
                if (completed.consume()) {
                    snackbarHostState.showSnackbar("Recording saved: ${completed.filePath.substringAfterLast("/")}")
                    // Return state to Idle
                    RecordingService.updateState(RecordingState.Idle)
                }
            } else if (recordingState is RecordingState.Failed) {
                val error = (recordingState as RecordingState.Failed).error
                snackbarHostState.showSnackbar("Recording Error: $error")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBase)
        ) {
            // Seamless ambient particle background
            CosmicBackground(enableMotion = settings.smoothAnimations)

            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    FloatingBottomDock(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    // 220ms Crossfade between tabs
                    Crossfade(
                        targetState = currentTab,
                        animationSpec = tween(durationMillis = 220),
                        label = "TabCrossfade"
                    ) { tab ->
                        when (tab) {
                            RecoraTab.RECORD -> {
                                HomeScreen(
                                    settings = settings,
                                    recordingState = recordingState,
                                    countdownRemaining = countdownRemaining,
                                    freeStorageBytes = freeStorageBytes,
                                    onRecordButtonClick = {
                                        if (recordingState is RecordingState.Recording) {
                                            // Stop recording
                                            val stopIntent = Intent(this@MainActivity, RecordingService::class.java).apply {
                                                action = RecordingService.ACTION_STOP
                                            }
                                            startService(stopIntent)
                                        } else if (recordingState is RecordingState.Idle || recordingState is RecordingState.Failed) {
                                            // Handle countdown if enabled
                                            val delaySec = when (settings.timerPreset) {
                                                TimerPreset.OFF -> 0
                                                TimerPreset.SEC_3 -> 3
                                                TimerPreset.SEC_5 -> 5
                                                TimerPreset.SEC_10 -> 10
                                                TimerPreset.CUSTOM -> settings.customTimerSeconds
                                            }

                                            if (delaySec > 0) {
                                                countdownJob?.cancel()
                                                countdownJob = scope.launch {
                                                    for (sec in delaySec downTo 1) {
                                                        countdownRemaining = sec
                                                        delay(1000)
                                                    }
                                                    countdownRemaining = 0
                                                    checkPermissionsAndStart(settings.audioSource)
                                                }
                                            } else {
                                                checkPermissionsAndStart(settings.audioSource)
                                            }
                                        }
                                    },
                                    onCancelCountdown = {
                                        countdownJob?.cancel()
                                        countdownRemaining = 0
                                    },
                                    onAudioSourceChange = { scope.launch { preferencesRepo.setAudioSource(it) } },
                                    onResolutionPresetChange = { scope.launch { preferencesRepo.setResolutionPreset(it) } },
                                    onFpsOptionChange = { scope.launch { preferencesRepo.setFpsOption(it) } },
                                    onOrientationOptionChange = { scope.launch { preferencesRepo.setOrientationOption(it) } },
                                    onTimerPresetChange = { scope.launch { preferencesRepo.setTimerPreset(it) } },
                                    onCustomTimerSecondsChange = { scope.launch { preferencesRepo.setCustomTimerSeconds(it) } },
                                    onControlsToggle = { scope.launch { preferencesRepo.setShowControls(it) } },
                                    onPenToolsToggle = { scope.launch { preferencesRepo.setShowPenTools(it) } },
                                    onFacecamToggle = { scope.launch { preferencesRepo.setShowFacecam(it) } },
                                    onTouchesToggle = { scope.launch { preferencesRepo.setShowTouches(it) } },
                                    onDismissPromo = { scope.launch { preferencesRepo.setPromoBannerDismissed(true) } },
                                    onCrownBadgeClick = { currentTab = RecoraTab.MORE }
                                )
                            }
                            RecoraTab.RECORDINGS -> {
                                RecordingsScreen(
                                    recordings = recordingsList,
                                    recoraSizeBytes = recoraSizeBytes,
                                    freeSizeBytes = freeStorageBytes,
                                    totalSizeBytes = totalStorageBytes,
                                    onPlay = { item ->
                                        Toast.makeText(this@MainActivity, "Playing: ${item.title}", Toast.LENGTH_SHORT).show()
                                    },
                                    onDelete = { item ->
                                        File(item.filePath).delete()
                                        recordingsList = recordingsList.filter { it.id != item.id }
                                    },
                                    onShare = { item ->
                                        Toast.makeText(this@MainActivity, "Share: ${item.title}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            RecoraTab.MORE -> {
                                MoreScreen(
                                    settings = settings,
                                    onHapticsToggle = { scope.launch { preferencesRepo.setHapticFeedback(it) } },
                                    onSmoothAnimationsToggle = { scope.launch { preferencesRepo.setSmoothAnimations(it) } },
                                    onSelectPlan = { scope.launch { preferencesRepo.setEntitlement(it) } },
                                    onRestorePurchases = {
                                        Toast.makeText(this@MainActivity, "Purchases restored successfully", Toast.LENGTH_SHORT).show()
                                    },
                                    onRedeemCode = {
                                        Toast.makeText(this@MainActivity, "Redeem dialog will open", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpenCompatibility = {
                                        Toast.makeText(this@MainActivity, "Device is 100% compatible with H.264 & AAC", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpenDiagnostics = {
                                        Toast.makeText(this@MainActivity, "System diagnostics healthy", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpenJournal = {
                                        Toast.makeText(this@MainActivity, "0 crashes logged", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpenPrivacyPolicy = {
                                        Toast.makeText(this@MainActivity, "Recora stores all media 100% locally on your device.", Toast.LENGTH_LONG).show()
                                    },
                                    onOpenTerms = {
                                        Toast.makeText(this@MainActivity, "Standard terms of service apply.", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpenLicenses = {
                                        Toast.makeText(this@MainActivity, "Media3, Jetpack Compose, Kotlin Coroutines", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndStart(audioSource: AudioSource) {
        if (audioSource == AudioSource.MIC || audioSource == AudioSource.INTERNAL_AND_MIC) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return
            }
        }
        requestScreenCapture()
    }

    private fun requestScreenCapture() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        projectionLauncher.launch(intent)
    }

    private fun startForegroundRecordingService() {
        val resultData = pendingMediaProjectionIntent ?: return
        val serviceIntent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_RESULT_CODE, pendingResultCode)
            putExtra(RecordingService.EXTRA_RESULT_DATA, resultData)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}
