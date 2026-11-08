package com.veylqo.recora.service.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.veylqo.recora.R
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

        val density = context.resources.displayMetrics.density
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
            x = (20 * density).toInt()
            y = (180 * density).toInt()
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((12 * density).toInt(), (8 * density).toInt(), (14 * density).toInt(), (8 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 60 * density
                setColor(Color.parseColor("#EE120E29"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#7F5AF0"))
            }
            elevation = 24f
            gravity = Gravity.CENTER_VERTICAL
        }

        // 1. Dedicated Drag Handle Grip on the left
        val dragHandle = TextView(context).apply {
            text = "⋮⋮"
            setTextColor(Color.parseColor("#A58BFF"))
            textSize = 16f
            setPadding((4 * density).toInt(), 0, (8 * density).toInt(), 0)
            gravity = Gravity.CENTER
        }
        container.addView(dragHandle)

        // Drag listener strictly on the dragHandle so buttons receive pure clicks!
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    params.x = initialX + dx
                    params.y = initialY + dy
                    try {
                        windowManager.updateViewLayout(container, params)
                    } catch (e: Exception) {}
                    true
                }
                else -> false
            }
        }

        // 2. Pause / Resume Button
        val pauseBtn = createActionButton(android.R.drawable.ic_media_pause, bgColor = "#251D4D") { view ->
            isPaused = !isPaused
            onPauseResumeClick(isPaused)
            (view as ImageView).setImageResource(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
            )
        }
        container.addView(pauseBtn)

        // 3. Stop Button (Vibrant Pink Disc)
        val stopBtn = createActionButton(android.R.drawable.ic_delete, bgColor = "#F0446A") {
            onStopClick()
        }
        container.addView(stopBtn)

        // 4. Pen & Tools Toggle Button
        val penBtn = createActionButton(android.R.drawable.ic_menu_edit, bgColor = "#251D4D") {
            onPenToggleClick()
        }
        container.addView(penBtn)

        // 5. Screenshot Button
        val cameraBtn = createActionButton(android.R.drawable.ic_menu_camera, bgColor = "#251D4D") {
            onScreenshotClick()
        }
        container.addView(cameraBtn)

        // 6. Mic Mute Button
        val micBtn = createActionButton(android.R.drawable.stat_notify_chat, bgColor = "#251D4D") { view ->
            isMuted = !isMuted
            onMicMuteClick(isMuted)
            view.alpha = if (isMuted) 0.35f else 1.0f
        }
        container.addView(micBtn)

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
            } catch (e: Exception) {}
            overlayView = null
        }
    }

    private fun createActionButton(iconRes: Int, bgColor: String, onClick: (View) -> Unit): ImageView {
        val density = context.resources.displayMetrics.density
        val btnSize = (38 * density).toInt()
        val pad = (8 * density).toInt()

        return ImageView(context).apply {
            setImageResource(iconRes)
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(bgColor))
            }
            setColorFilter(Color.WHITE)
            isClickable = true
            isFocusable = false
            setOnClickListener { onClick(it) }
        }
    }
}
