package com.veylqo.recora.service.overlay

import android.content.Context
import android.provider.Settings
import android.util.Log

class OverlayManager(
    private val context: Context,
    private val onStopRecording: () -> Unit,
    private val onPauseResume: (isPaused: Boolean) -> Unit,
    private val onScreenshot: () -> Unit,
    private val onMicMute: (isMuted: Boolean) -> Unit
) {
    private var controlsOverlay: FloatingControlsOverlay? = null
    private var penOverlay: PenToolsOverlay? = null
    private var facecamOverlay: FacecamOverlay? = null

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun showControlsOverlay() {
        if (!canDrawOverlays()) return
        if (controlsOverlay == null) {
            controlsOverlay = FloatingControlsOverlay(
                context = context,
                onStopClick = onStopRecording,
                onPauseResumeClick = onPauseResume,
                onPenToggleClick = { togglePenOverlay() },
                onScreenshotClick = onScreenshot,
                onMicMuteClick = onMicMute
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
            }
        }
        penOverlay?.show()
    }

    fun hidePenOverlay() {
        penOverlay?.remove()
        penOverlay = null
    }

    fun showFacecamOverlay() {
        if (!canDrawOverlays()) return
        if (facecamOverlay == null) {
            facecamOverlay = FacecamOverlay(context) {
                facecamOverlay = null
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
