package com.veylqo.recora.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.veylqo.recora.R
import com.veylqo.recora.core.model.RecordingState
import com.veylqo.recora.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class RecoraTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var stateJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        stateJob?.cancel()
        stateJob = serviceScope.launch {
            RecordingService.recordingState.collect { state ->
                updateTileState(state)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        stateJob?.cancel()
        stateJob = null
    }

    override fun onClick() {
        super.onClick()
        val currentState = RecordingService.recordingState.value
        when (currentState) {
            is RecordingState.Recording -> {
                // Stop recording
                val intent = Intent(this, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP
                }
                startService(intent)
            }
            is RecordingState.Idle, is RecordingState.Completed, is RecordingState.Failed -> {
                // Launch MainActivity to initiate screen capture permission flow
                val intent = Intent(this, MainActivity::class.java).apply {
                    action = "com.veylqo.recora.ACTION_START_RECORDING"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    startActivityAndCollapse(pendingIntent)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(intent)
                }
            }
            else -> {}
        }
    }

    private fun updateTileState(state: RecordingState) {
        val tile = qsTile ?: return
        when (state) {
            is RecordingState.Recording -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Recording"
                val seconds = (state.durationMs / 1000) % 60
                val minutes = (state.durationMs / 1000) / 60
                val timeStr = "%02d:%02d".format(minutes, seconds)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = if (state.isPaused) "Paused • $timeStr" else timeStr
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_tile_active)
            }
            is RecordingState.Preparing, is RecordingState.Stopping -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.label = "Recora"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Busy"
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_tile)
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Recora"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Ready"
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_tile)
            }
        }
        tile.updateTile()
    }
}
