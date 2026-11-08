package com.veylqo.recora.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.veylqo.recora.MainActivity
import com.veylqo.recora.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RecoraTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val currentState = RecordingService.serviceState.value

        if (currentState.status == RecordingStatus.RECORDING) {
            // If already recording, stop it
            val stopIntent = Intent(this, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
            startService(stopIntent)
        } else {
            // Open main activity and trigger countdown / projection request
            val appIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("action_from_tile", true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this, 100, appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(appIntent)
            }
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRecording = RecordingService.serviceState.value.status == RecordingStatus.RECORDING

        tile.state = if (isRecording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Recora"
        tile.subtitle = if (isRecording) "Recording..." else "Tap to record"
        try {
            val bmp = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.ic_qs_recora_art)
            if (bmp != null) {
                tile.icon = Icon.createWithBitmap(bmp)
            } else {
                tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_recora)
            }
        } catch (e: Exception) {
            try {
                tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_recora)
            } catch (ignored: Exception) {}
        }
        tile.updateTile()
    }
}
