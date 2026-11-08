package com.veylqo.recora.service.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.veylqo.recora.R
import com.veylqo.recora.service.RecordingService
import kotlin.math.abs

class FloatingControlsOverlay(
    private val context: Context,
    private val onStopClick: () -> Unit,
    private val onPauseResumeClick: (isPaused: Boolean) -> Unit,
    private val onPenToggleClick: () -> Unit,
    private val onScreenshotClick: () -> Unit,
    private val onMicMuteClick: (isMuted: Boolean) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isPaused = false
    private var isMuted = false

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 16, 24, 16)
            background = createPillBackground()
            elevation = 20f
        }

        // 1. Pause / Resume Button
        val pauseBtn = createActionButton(android.R.drawable.ic_media_pause) {
            isPaused = !isPaused
            onPauseResumeClick(isPaused)
            (it as ImageView).setImageResource(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
            )
        }
        container.addView(pauseBtn)

        // 2. Stop Button
        val stopBtn = createActionButton(android.R.drawable.ic_delete, isStop = true) {
            onStopClick()
        }
        container.addView(stopBtn)

        // 3. Pen & Tools Toggle Button
        val penBtn = createActionButton(android.R.drawable.ic_menu_edit) {
            onPenToggleClick()
        }
        container.addView(penBtn)

        // 4. Screenshot Button
        val cameraBtn = createActionButton(android.R.drawable.ic_menu_camera) {
            onScreenshotClick()
        }
        container.addView(cameraBtn)

        // 5. Mic Mute Toggle Button
        val micBtn = createActionButton(android.R.drawable.stat_notify_chat) {
            isMuted = !isMuted
            onMicMuteClick(isMuted)
            it.alpha = if (isMuted) 0.4f else 1.0f
        }
        container.addView(micBtn)

        // Touch listener for smooth dragging
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var hasMoved = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    hasMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        hasMoved = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(container, params)
                        } catch (e: Exception) {
                            // Ignored if window detached
                        }
                    }
                    true
                }
                else -> false
            }
        }

        overlayView = container
        try {
            windowManager.addView(container, params)
        } catch (e: Exception) {
            overlayView = null
        }
    }

    fun remove() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // Already removed
            }
            overlayView = null
        }
    }

    private fun createActionButton(iconRes: Int, isStop: Boolean = false, onClick: (View) -> Unit): View {
        return ImageView(context).apply {
            setImageResource(iconRes)
            val pad = 18
            setPadding(pad, pad, pad, pad)
            val btnSize = 100
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                setMargins(8, 0, 8, 0)
            }
            background = if (isStop) createStopCircleBackground() else createCircleButtonBackground()
            setColorFilter(android.graphics.Color.WHITE)
            setOnClickListener { onClick(it) }
        }
    }

    private fun createPillBackground(): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 90f
            setColor(android.graphics.Color.parseColor("#E6141029"))
            setStroke(3, android.graphics.Color.parseColor("#7F5AF0"))
        }
    }

    private fun createCircleButtonBackground(): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(android.graphics.Color.parseColor("#251D4D"))
        }
    }

    private fun createStopCircleBackground(): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(android.graphics.Color.parseColor("#F0446A"))
        }
    }
}
