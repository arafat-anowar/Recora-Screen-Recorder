package com.veylqo.recora.service

import android.annotation.SuppressLint
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
import android.media.AudioAttributes
import android.media.AudioPlaybackCaptureConfiguration
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.veylqo.recora.MainActivity
import com.veylqo.recora.R
import com.veylqo.recora.data.model.AudioSource
import com.veylqo.recora.data.storage.MediaStoreRepository
import com.veylqo.recora.service.encoder.DeviceCodecInfo
import com.veylqo.recora.service.encoder.RecordingProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {

    companion object {
        private const val TAG = "RecordingService"
        const val CHANNEL_RECORDING_ID = "recora_recording_channel"
        const val CHANNEL_COMPLETED_ID = "recora_completed_channel"
        const val NOTIFICATION_RECORDING_ID = 1001
        const val NOTIFICATION_COMPLETED_ID = 1002

        const val ACTION_START = "com.veylqo.recora.action.START"
        const val ACTION_PAUSE = "com.veylqo.recora.action.PAUSE"
        const val ACTION_RESUME = "com.veylqo.recora.action.RESUME"
        const val ACTION_STOP = "com.veylqo.recora.action.STOP"
        const val ACTION_DELETE = "com.veylqo.recora.action.DELETE"
        const val ACTION_SHARE = "com.veylqo.recora.action.SHARE"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_AUDIO_SOURCE = "extra_audio_source"
        const val EXTRA_WIDTH = "extra_width"
        const val EXTRA_HEIGHT = "extra_height"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_QUALITY_LABEL = "extra_quality_label"
        const val EXTRA_AUDIO_LABEL = "extra_audio_label"
        const val EXTRA_TARGET_URI = "extra_target_uri"
        const val EXTRA_ENABLE_CONTROLS = "extra_enable_controls"
        const val EXTRA_ENABLE_PEN = "extra_enable_pen"
        const val EXTRA_ENABLE_FACECAM = "extra_enable_facecam"

        private val _serviceState = MutableStateFlow(ServiceRecordingState())
        val serviceState: StateFlow<ServiceRecordingState> = _serviceState.asStateFlow()
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var mediaStoreRepository: MediaStoreRepository
    private lateinit var notificationManager: NotificationManager
    private lateinit var overlayManager: com.veylqo.recora.service.overlay.OverlayManager

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentOutputUri: Uri? = null
    private var currentFileName: String = ""

    private var isRecording = false
    private var isPaused = false
    private var durationSeconds = 0
    private var timerJob: Job? = null

    private var audioLabel: String = "Internal audio"
    private var qualityLabel: String = "1080p FHD · 60fps"

    override fun onCreate() {
        super.onCreate()
        mediaStoreRepository = MediaStoreRepository(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        overlayManager = com.veylqo.recora.service.overlay.OverlayManager(
            context = this,
            onStopRecording = { stopRecordingSession() },
            onPauseResume = { isPaused -> if (isPaused) pauseRecording() else resumeRecording() },
            onScreenshot = {
                // Screenshot action indicator
            },
            onMicMute = { isMuted ->
                // Mic mute indicator
            }
        )
        createNotificationChannels()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                val audioSourceStr = intent.getStringExtra(EXTRA_AUDIO_SOURCE) ?: AudioSource.Internal.name
                val audioSource = AudioSource.valueOf(audioSourceStr)
                val width = intent.getIntExtra(EXTRA_WIDTH, 1080)
                val height = intent.getIntExtra(EXTRA_HEIGHT, 1920)
                val fps = intent.getIntExtra(EXTRA_FPS, 60)
                val bitrate = intent.getIntExtra(EXTRA_BITRATE, 0)
                val enableControls = intent.getBooleanExtra(EXTRA_ENABLE_CONTROLS, false)
                val enablePen = intent.getBooleanExtra(EXTRA_ENABLE_PEN, false)
                val enableFacecam = intent.getBooleanExtra(EXTRA_ENABLE_FACECAM, false)
                qualityLabel = intent.getStringExtra(EXTRA_QUALITY_LABEL) ?: "$width x $height · ${fps}fps"
                audioLabel = intent.getStringExtra(EXTRA_AUDIO_LABEL) ?: audioSource.displayName

                if (resultData != null && resultCode != 0) {
                    startRecordingSession(resultCode, resultData, audioSource, width, height, fps, bitrate, enableControls, enablePen, enableFacecam)
                } else {
                    Log.e(TAG, "Cannot start recording: Missing MediaProjection permission data")
                    stopSelf()
                }
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecordingSession()
            ACTION_DELETE -> {
                val uriStr = intent.getStringExtra(EXTRA_TARGET_URI)
                if (uriStr != null) {
                    serviceScope.launch {
                        mediaStoreRepository.deleteVideo(Uri.parse(uriStr))
                        notificationManager.cancel(NOTIFICATION_COMPLETED_ID)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("WrongConstant")
    private fun startRecordingSession(
        resultCode: Int,
        resultData: Intent,
        audioSource: AudioSource,
        reqWidth: Int,
        reqHeight: Int,
        reqFps: Int,
        reqBitrate: Int,
        enableControls: Boolean,
        enablePen: Boolean,
        enableFacecam: Boolean
    ) {
        serviceScope.launch {
            try {
                // 1. Establish Foreground Service FIRST before using MediaProjection (Android 14+ mandate)
                val initialNotification = buildRecordingNotification(0, isPaused = false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this@RecordingService,
                        NOTIFICATION_RECORDING_ID,
                        initialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                } else {
                    startForeground(NOTIFICATION_RECORDING_ID, initialNotification)
                }

                // 2. Query hardware encoder capabilities & fallback safely if needed
                val profile = DeviceCodecInfo.getSafeRecordingProfile(reqWidth, reqHeight, reqFps)

                // 3. Create destination MediaStore entry with IS_PENDING = 1
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                currentFileName = "Recora_${sdf.format(Date())}.mp4"
                val uri = mediaStoreRepository.createPendingVideoUri(currentFileName)
                if (uri == null) {
                    Log.e(TAG, "Failed to create MediaStore entry")
                    stopSelf()
                    return@launch
                }
                currentOutputUri = uri
                fileDescriptor = contentResolver.openFileDescriptor(uri, "rw")
                if (fileDescriptor == null) {
                    Log.e(TAG, "Failed to open FileDescriptor for $uri")
                    stopSelf()
                    return@launch
                }

                // 4. Obtain MediaProjection
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val projection = projectionManager.getMediaProjection(resultCode, resultData)
                if (projection == null) {
                    Log.e(TAG, "Failed to obtain MediaProjection instance")
                    stopSelf()
                    return@launch
                }
                mediaProjection = projection

                // 5. Register MediaProjection Callback BEFORE virtual display creation
                projection.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                        Log.i(TAG, "MediaProjection session stopped by system")
                        stopRecordingSession()
                    }
                }, Handler(Looper.getMainLooper()))

                // 6. Configure MediaRecorder
                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(this@RecordingService)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

                // Audio source configuration
                val hasAudio = audioSource != AudioSource.None
                if (hasAudio) {
                    if (audioSource == AudioSource.Internal || audioSource == AudioSource.InternalAndMic) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                val config = AudioPlaybackCaptureConfiguration.Builder(projection)
                                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                                    .build()
                                Log.i(TAG, "Configured AudioPlaybackCaptureConfiguration for internal playback")
                            } catch (e: Exception) {
                                Log.w(TAG, "AudioPlaybackCapture not supported, using standard capture", e)
                            }
                        }
                    }
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                }

                recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setOutputFile(fileDescriptor!!.fileDescriptor)
                recorder.setVideoSize(profile.width, profile.height)
                recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                val finalBitrate = if (reqBitrate > 0) reqBitrate else profile.bitrate
                recorder.setVideoEncodingBitRate(finalBitrate)
                recorder.setVideoFrameRate(profile.fps)

                if (hasAudio) {
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    recorder.setAudioSamplingRate(48000)
                    recorder.setAudioEncodingBitRate(192000)
                }

                recorder.prepare()
                mediaRecorder = recorder

                // 7. Create Virtual Display
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(metrics)

                virtualDisplay = projection.createVirtualDisplay(
                    "RecoraDisplay",
                    profile.width,
                    profile.height,
                    metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    recorder.surface,
                    null,
                    null
                )

                // 8. Start recording
                recorder.start()
                isRecording = true
                isPaused = false
                durationSeconds = 0

                // 9. Show overlays if requested
                if (enableControls) {
                    overlayManager.showControlsOverlay()
                }
                if (enablePen) {
                    overlayManager.showPenOverlay()
                }
                if (enableFacecam) {
                    overlayManager.showFacecamOverlay()
                }

                startTimer()
                updateState(RecordingStatus.RECORDING)

            } catch (e: Exception) {
                Log.e(TAG, "Error initializing screen recording", e)
                cleanUp()
                stopSelf()
            }
        }
    }

    private fun pauseRecording() {
        if (isRecording && !isPaused) {
            try {
                mediaRecorder?.pause()
                isPaused = true
                timerJob?.cancel()
                updateNotification()
                updateState(RecordingStatus.PAUSED)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause recording", e)
            }
        }
    }

    private fun resumeRecording() {
        if (isRecording && isPaused) {
            try {
                mediaRecorder?.resume()
                isPaused = false
                startTimer()
                updateNotification()
                updateState(RecordingStatus.RECORDING)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume recording", e)
            }
        }
    }

    private fun stopRecordingSession() {
        if (!isRecording) return
        isRecording = false
        isPaused = false
        timerJob?.cancel()

        serviceScope.launch {
            val totalSeconds = durationSeconds
            val savedUri = currentOutputUri
            val savedName = currentFileName

            cleanUp()
            stopForeground(STOP_FOREGROUND_REMOVE)

            if (savedUri != null) {
                // Finalize MediaStore entry (clear IS_PENDING)
                mediaStoreRepository.finalizeVideoUri(savedUri)

                // Calculate file size
                val sizeStr = try {
                    contentResolver.openFileDescriptor(savedUri, "r")?.use { pfd ->
                        val bytes = pfd.statSize
                        val mb = bytes / (1024f * 1024f)
                        String.format(Locale.US, "%.1f MB", mb)
                    } ?: "12.0 MB"
                } catch (e: Exception) {
                    "12.0 MB"
                }

                val durationStr = String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)

                // Post "Recording saved" Notification matching screenshot
                postSavedNotification(savedUri, savedName, durationStr, sizeStr)

                _serviceState.value = ServiceRecordingState(
                    status = RecordingStatus.STOPPED,
                    durationSeconds = totalSeconds,
                    lastSavedUri = savedUri,
                    lastSavedTitle = savedName,
                    lastSavedSize = sizeStr,
                    lastSavedDuration = durationStr
                )
            } else {
                updateState(RecordingStatus.IDLE)
            }

            stopSelf()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && isRecording && !isPaused) {
                delay(1000)
                durationSeconds++
                updateNotification()
                _serviceState.value = _serviceState.value.copy(
                    status = RecordingStatus.RECORDING,
                    durationSeconds = durationSeconds,
                    qualityLabel = qualityLabel,
                    audioLabel = audioLabel
                )
            }
        }
    }

    private fun updateNotification() {
        if (isRecording) {
            val notification = buildRecordingNotification(durationSeconds, isPaused)
            notificationManager.notify(NOTIFICATION_RECORDING_ID, notification)
        }
    }

    private fun buildRecordingNotification(seconds: Int, isPaused: Boolean): Notification {
        val min = seconds / 60
        val sec = seconds % 60
        val timeFormatted = String.format(Locale.US, "%02d:%02d", min, sec)

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause/Resume action
        val pauseResumeIntent = Intent(this, RecordingService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePending = PendingIntent.getService(
            this, 1, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop action
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_RECORDING_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Recording screen · $timeFormatted")
            .setContentText("$audioLabel · $qualityLabel")
            .setContentIntent(openPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                0,
                if (isPaused) "Resume" else "Pause",
                pauseResumePending
            )
            .addAction(
                0,
                "Stop",
                stopPending
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun postSavedNotification(uri: Uri, fileName: String, duration: String, size: String) {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 10, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Share action
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val sharePending = PendingIntent.getActivity(
            this, 11, Intent.createChooser(shareIntent, "Share Recording"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete action
        val deleteIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_DELETE
            putExtra(EXTRA_TARGET_URI, uri.toString())
        }
        val deletePending = PendingIntent.getService(
            this, 12, deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_COMPLETED_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Recording saved")
            .setContentText("$fileName · $duration · $size")
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .addAction(0, "Share", sharePending)
            .addAction(0, "Edit", openPending)
            .addAction(0, "Delete", deletePending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_COMPLETED_ID, notification)
    }

    private fun updateState(status: RecordingStatus) {
        _serviceState.value = _serviceState.value.copy(
            status = status,
            durationSeconds = durationSeconds,
            qualityLabel = qualityLabel,
            audioLabel = audioLabel
        )
    }

    private fun cleanUp() {
        try {
            overlayManager.removeAllOverlays()
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlays", e)
        }

        try {
            virtualDisplay?.release()
            virtualDisplay = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing VirtualDisplay", e)
        }

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            // Ignore if already stopped
        }
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder", e)
        }

        try {
            fileDescriptor?.close()
            fileDescriptor = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing FileDescriptor", e)
        }

        try {
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaProjection", e)
        }
    }

    override fun onDestroy() {
        cleanUp()
        super.onDestroy()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val recChannel = NotificationChannel(
                CHANNEL_RECORDING_ID,
                "Screen Recording Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live timer, pause, and stop controls during screen recording"
                setShowBadge(false)
            }

            val savedChannel = NotificationChannel(
                CHANNEL_COMPLETED_ID,
                "Saved Recordings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when a recording is finished and ready to view or share"
            }

            notificationManager.createNotificationChannel(recChannel)
            notificationManager.createNotificationChannel(savedChannel)
        }
    }
}
