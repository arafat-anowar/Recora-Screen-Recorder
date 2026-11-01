package com.veylqo.recora.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class FloatingControlsService : Service() {
    companion object {
        const val ACTION_SHOW = "com.veylqo.recora.action.SHOW_CONTROLS"
        const val ACTION_HIDE = "com.veylqo.recora.action.HIDE_CONTROLS"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> stopSelf()
        }
        return START_NOT_STICKY
    }
}
