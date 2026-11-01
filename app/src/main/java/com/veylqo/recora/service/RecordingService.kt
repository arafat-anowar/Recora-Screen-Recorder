package com.veylqo.recora.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.veylqo.recora.R
import com.veylqo.recora.core.guard.EmergencyStorageGuard
import com.veylqo.recora.core.model.AudioSource
import com.veylqo.recora.core.model.FpsOption
import com.veylqo.recora.core.model.OrientationOption
import com.veylqo.recora.core.model.RecordingState
import com.veylqo.recora.core.model.ResolutionPreset
import com.veylqo.recora.core.negotiator.AdaptiveEncoderNegotiator
import com.veylqo.recora.core.negotiator.NegotiatedVideoConfig
import com.veylqo.recora.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {

    companion object {
        private const val TAG = "RecordingService"
        const val CHANNEL_ID = "recora_recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.veylqo.recora.action.START"
        const val ACTION_PAUSE = "com.veylqo.recora.action.PAUSE"
        const val ACTION_RESUME = "com.veylqo.recora.action.RESUME"
        const val ACTION_STOP = "com.veylqo.recora.action.STOP"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_AUDIO_SOURCE = "extra_audio_source"
        const val EXTRA_RESOLUTION_PRESET = "extra_resolution_preset"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_ORIENTATION = "extra_orientation"
        const val EXTRA_BITRATE = "extra_bitrate"

        private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
        val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

        fun updateState(state: RecordingState) {
            _recordingState.value = state
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private val storageGuard = EmergencyStorageGuard()

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null

    private var currentSessionId: String = ""
    private var stagingFile: File? = null
    private var startTimeMs: Long = 0L
    private var pausedDurationAccumulatedMs: Long = 0L
    private var pauseTimestampMs: Long = 0L
    private var isCurrentlyPaused: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            Log.d(TAG, "MediaProjection stopped by system")
            stopRecordingInternal()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                val audioSource = intent.getStringExtra(EXTRA_AUDIO_SOURCE)?.let { runCatching { AudioSource.valueOf(it) }.getOrNull() } ?: AudioSource.INTERNAL
                val preset = intent.getStringExtra(EXTRA_RESOLUTION_PRESET)?.let { runCatching { ResolutionPreset.valueOf(it) }.getOrNull() } ?: ResolutionPreset.RES_4K
                val fps = intent.getIntExtra(EXTRA_FPS, 60)
                val orientation = intent.getStringExtra(EXTRA_ORIENTATION)?.let { runCatching { OrientationOption.valueOf(it) }.getOrNull() } ?: OrientationOption.PORTRAIT
                val bitrate = intent.getIntExtra(EXTRA_BITRATE, 0)

                if (resultCode != 0 && resultData != null) {
                    startRecordingInternal(resultCode, resultData, audioSource, preset, FpsOption.entries.firstOrNull { it.value == fps } ?: FpsOption.FPS_60, orientation, bitrate)
                } else {
                    _recordingState.value = RecordingState.Failed("Missing MediaProjection permission data")
                    stopSelf()
                }
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecordingInternal()
        }
        return START_NOT_STICKY
    }

    private fun startRecordingInternal(
        resultCode: Int,
        resultData: Intent,
        audioSource: AudioSource,
        preset: ResolutionPreset,
        fpsOption: FpsOption,
        orientation: OrientationOption,
        bitrateBps: Int
    ) {
        try {
            // Verify storage first
            val stagingDir = File(cacheDir, "staging").apply { mkdirs() }
            if (!storageGuard.canStartRecording(stagingDir)) {
                _recordingState.value = RecordingState.Failed("Insufficient storage space (< 50MB).")
                stopSelf()
                return
            }

            currentSessionId = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            stagingFile = File(stagingDir, "Recora_${currentSessionId}_raw.mp4")

            // Android 14+ Foreground Service start requirement
            val initialNotification = buildNotification(0L, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                if (audioSource == AudioSource.MIC || audioSource == AudioSource.INTERNAL_AND_MIC) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    }
                }
                startForeground(NOTIFICATION_ID, initialNotification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }

            // Register projection callback with main looper handler
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, resultData)
            mediaProjection?.registerCallback(projectionCallback, mainHandler)

            // Negotiate video dimensions and bitrate
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)

            val negotiated = AdaptiveEncoderNegotiator.negotiateConfig(
                displayWidth = metrics.widthPixels,
                displayHeight = metrics.heightPixels,
                preset = preset,
                fpsOption = fpsOption,
                userBitrateBps = bitrateBps,
                orientation = orientation
            )

            setupMediaRecorder(negotiated, audioSource)

            // Setup Virtual Display
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "RecoraDisplay",
                negotiated.width,
                negotiated.height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                mainHandler
            )

            mediaRecorder?.start()
            startTimeMs = SystemClock.elapsedRealtime()
            pausedDurationAccumulatedMs = 0L
            isCurrentlyPaused = false

            // Update StateFlow to Recording
            _recordingState.value = RecordingState.Recording(
                sessionId = currentSessionId,
                durationMs = 0L,
                isPaused = false,
                filePath = stagingFile?.absolutePath.orEmpty()
            )

            // Start timer updates
            startTimerAndMonitoring(stagingDir)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            _recordingState.value = RecordingState.Failed("Recording failed to start: ${e.message}", e)
            cleanup()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun setupMediaRecorder(config: NegotiatedVideoConfig, audioSource: AudioSource) {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        // Configure Audio Source if needed
        if (audioSource == AudioSource.MIC || audioSource == AudioSource.INTERNAL_AND_MIC) {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        }

        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        if (audioSource == AudioSource.MIC || audioSource == AudioSource.INTERNAL_AND_MIC) {
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(44100)
            recorder.setAudioEncodingBitRate(128000)
        }

        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        recorder.setVideoSize(config.width, config.height)
        recorder.setVideoFrameRate(config.fps)
        recorder.setVideoEncodingBitRate(config.bitrateBps)

        recorder.setOutputFile(stagingFile?.absolutePath)
        recorder.prepare()
        mediaRecorder = recorder
    }

    private fun startTimerAndMonitoring(stagingDir: File) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                if (!isCurrentlyPaused) {
                    val currentDuration = SystemClock.elapsedRealtime() - startTimeMs - pausedDurationAccumulatedMs
                    _recordingState.value = RecordingState.Recording(
                        sessionId = currentSessionId,
                        durationMs = currentDuration,
                        isPaused = false,
                        filePath = stagingFile?.absolutePath.orEmpty()
                    )
                    updateNotification(currentDuration, false)
                }
                delay(1000)
            }
        }

        // Start Emergency Storage Guard
        storageGuard.startMonitoring(
            scope = serviceScope,
            targetDir = stagingDir,
            onWarning = { freeBytes ->
                Log.w(TAG, "Storage warning: $freeBytes bytes free")
            },
            onCriticalEmergencyStop = { freeBytes ->
                Log.e(TAG, "Storage depleted (< 50MB): $freeBytes bytes free. Cleanly stopping.")
                stopRecordingInternal()
            }
        )
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isCurrentlyPaused) {
            try {
                mediaRecorder?.pause()
                isCurrentlyPaused = true
                pauseTimestampMs = SystemClock.elapsedRealtime()

                val duration = pauseTimestampMs - startTimeMs - pausedDurationAccumulatedMs
                _recordingState.value = RecordingState.Recording(
                    sessionId = currentSessionId,
                    durationMs = duration,
                    isPaused = true,
                    filePath = stagingFile?.absolutePath.orEmpty()
                )
                updateNotification(duration, true)
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing recording", e)
            }
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isCurrentlyPaused) {
            try {
                mediaRecorder?.resume()
                isCurrentlyPaused = false
                pausedDurationAccumulatedMs += (SystemClock.elapsedRealtime() - pauseTimestampMs)

                val duration = SystemClock.elapsedRealtime() - startTimeMs - pausedDurationAccumulatedMs
                _recordingState.value = RecordingState.Recording(
                    sessionId = currentSessionId,
                    durationMs = duration,
                    isPaused = false,
                    filePath = stagingFile?.absolutePath.orEmpty()
                )
                updateNotification(duration, false)
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming recording", e)
            }
        }
    }

    private fun stopRecordingInternal() {
        _recordingState.value = RecordingState.Stopping(currentSessionId)
        timerJob?.cancel()
        storageGuard.stopMonitoring()

        val finalDuration = if (isCurrentlyPaused) {
            pauseTimestampMs - startTimeMs - pausedDurationAccumulatedMs
        } else {
            SystemClock.elapsedRealtime() - startTimeMs - pausedDurationAccumulatedMs
        }

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        }

        cleanup()

        val file = stagingFile
        if (file != null && file.exists() && file.length() > 0) {
            _recordingState.value = RecordingState.Completed(
                sessionId = currentSessionId,
                uri = Uri.fromFile(file),
                filePath = file.absolutePath,
                durationMs = finalDuration,
                sizeBytes = file.length()
            )
        } else {
            _recordingState.value = RecordingState.Failed("Recording file is missing or empty")
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanup() {
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {}
        virtualDisplay = null

        try {
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
        } catch (_: Exception) {}
        mediaProjection = null

        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.recording_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.recording_notification_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(durationMs: Long, isPaused: Boolean): Notification {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / 1000) / 60
        val timeStr = "%02d:%02d".format(minutes, seconds)

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, RecordingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = if (isPaused) {
            PendingIntent.getService(
                this,
                2,
                Intent(this, RecordingService::class.java).apply { action = ACTION_RESUME },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                this,
                2,
                Intent(this, RecordingService::class.java).apply { action = ACTION_PAUSE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recora • Screen Recording")
            .setContentText(if (isPaused) "Recording paused ($timeStr)" else "Recording: $timeStr")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                0,
                if (isPaused) getString(R.string.action_resume) else getString(R.string.action_pause),
                pauseResumeIntent
            )
            .addAction(0, getString(R.string.action_stop), stopIntent)

        return builder.build()
    }

    private fun updateNotification(durationMs: Long, isPaused: Boolean) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(durationMs, isPaused))
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        timerJob?.cancel()
        storageGuard.stopMonitoring()
    }
}
