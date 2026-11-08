package com.veylqo.recora.service

import android.net.Uri

enum class RecordingStatus {
    IDLE,
    COUNTDOWN,
    RECORDING,
    PAUSED,
    STOPPED
}

data class ServiceRecordingState(
    val status: RecordingStatus = RecordingStatus.IDLE,
    val durationSeconds: Int = 0,
    val audioLabel: String = "Internal audio",
    val qualityLabel: String = "1080p FHD · 60fps",
    val lastSavedUri: Uri? = null,
    val lastSavedTitle: String? = null,
    val lastSavedSize: String? = null,
    val lastSavedDuration: String? = null
)
