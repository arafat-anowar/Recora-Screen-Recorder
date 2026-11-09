package com.veylqo.recora.service.overlay

import android.content.Context
import android.provider.Settings
import android.util.Log

class OverlayManager(
    private val context: Context,
    private val onStartRecording: (() -> Unit)? = null,
    private val onStopRecording: () -> Unit,
    private val onPauseResume: (isPaused: Boolean) -> Unit,
    private val onScreenshot: () -> Unit,
    private val onMicMute: (isMuted: Boolean) -> Unit,
    private val onControlsClosed: (() -> Unit)? = null,
    private val onPenClosed: (() -> Unit)? = null,
    private val onFacecamClosed: (() -> Unit)? = null,
    private var initialIsRecording: Boolean = false,
    private var initialIsPaused: Boolean = false
) {
    private var controlsOverlay: FloatingControlsOverlay? = null
    private var penOverlay: PenToolsOverlay? = null
    private var facecamOverlay: FacecamOverlay? = null

    private var currentIsRecording = initialIsRecording
    private var currentIsPaused = initialIsPaused

    fun updateRecordingState(isRecording: Boolean, isPaused: Boolean) {
        currentIsRecording = isRecording
        currentIsPaused = isPaused
        controlsOverlay?.updateState(isRecording, isPaused)
    }

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun showControlsOverlay() {
        if (!canDrawOverlays()) return
        if (controlsOverlay == null) {
            controlsOverlay = FloatingControlsOverlay(
                context = context,
                onRecordClick = { onStartRecording?.invoke() },
                onStopClick = onStopRecording,
                onPauseResumeClick = onPauseResume,
                onPenToggleClick = { togglePenOverlay() },
                onScreenshotClick = onScreenshot,
                onMicMuteClick = onMicMute,
                onFacecamToggleClick = { toggleFacecamOverlay() },
                onCloseClick = {
                    controlsOverlay = null
                    onControlsClosed?.invoke()
                },
                isRecording = currentIsRecording,
                isPaused = currentIsPaused
            )
        }
        controlsOverlay?.show()
    }

    fun hideControlsOverlay() {
        controlsOverlay?.remove()
        controlsOverlay = null
    }

    fun togglePenOverlay() {
        if (penOverlay == null) {
            showPenOverlay()
        } else {
            hidePenOverlay()
        }
    }

    fun showPenOverlay() {
        if (!canDrawOverlays()) return
        if (penOverlay == null) {
            penOverlay = PenToolsOverlay(context) {
                penOverlay = null
                onPenClosed?.invoke()
            }
        }
        penOverlay?.show()
    }

    fun hidePenOverlay() {
        penOverlay?.remove()
        penOverlay = null
    }

    fun toggleFacecamOverlay() {
        if (facecamOverlay == null) {
            showFacecamOverlay()
        } else {
            hideFacecamOverlay()
        }
    }

    fun showFacecamOverlay() {
        if (!canDrawOverlays()) return
        if (facecamOverlay == null) {
            facecamOverlay = FacecamOverlay(context) {
                facecamOverlay = null
                onFacecamClosed?.invoke()
            }
        }
        facecamOverlay?.show()
    }

    fun hideFacecamOverlay() {
        facecamOverlay?.remove()
        facecamOverlay = null
    }

    fun removeAllOverlays() {
        hideControlsOverlay()
        hidePenOverlay()
        hideFacecamOverlay()
    }
}
