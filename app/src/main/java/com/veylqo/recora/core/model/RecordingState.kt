package com.veylqo.recora.core.model

import android.net.Uri

sealed interface RecordingState {
    data object Idle : RecordingState

    data class Preparing(
        val countdownRemainingSeconds: Int = 0
    ) : RecordingState

    data class Recording(
        val sessionId: String,
        val durationMs: Long = 0L,
        val isPaused: Boolean = false,
        val filePath: String = ""
    ) : RecordingState

    data class Stopping(
        val sessionId: String
    ) : RecordingState

    data class Completed(
        val sessionId: String,
        val uri: Uri?,
        val filePath: String,
        val durationMs: Long = 0L,
        val sizeBytes: Long = 0L,
        private var isConsumed: Boolean = false
    ) : RecordingState {
        fun consume(): Boolean {
            return if (!isConsumed) {
                isConsumed = true
                true
            } else {
                false
            }
        }
        val consumed: Boolean get() = isConsumed
    }

    data class Failed(
        val error: String,
        val throwable: Throwable? = null
    ) : RecordingState
}
