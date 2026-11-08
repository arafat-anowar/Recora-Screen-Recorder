package com.veylqo.recora.service.overlay

import android.annotation.SuppressLint
import android.content.Context
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
    private var rootLayout: FrameLayout? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var isExpanded = true
    private var isPaused = false
    private var isMuted = false

    private var expandedBar: LinearLayout? = null
    private var collapsedBubble: FrameLayout? = null

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (rootLayout != null) return

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
            x = (16 * density).toInt()
            y = (180 * density).toInt()
        }
        windowParams = params

        val root = FrameLayout(context)

        // 1. Build Expanded Bar
        val bar = buildExpandedBar(density, params, root)
        expandedBar = bar
        root.addView(bar)

        // 2. Build Collapsed Bubble
        val bubble = buildCollapsedBubble(density, params, root)
        collapsedBubble = bubble
        bubble.visibility = View.GONE
        root.addView(bubble)

        rootLayout = root
        try {
            windowManager.addView(root, params)
        } catch (e: Exception) {
            rootLayout = null
        }
    }

    private fun toggleExpandCollapse() {
        isExpanded = !isExpanded
        if (isExpanded) {
            collapsedBubble?.visibility = View.GONE
            expandedBar?.visibility = View.VISIBLE
        } else {
            expandedBar?.visibility = View.GONE
            collapsedBubble?.visibility = View.VISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildExpandedBar(density: Float, params: WindowManager.LayoutParams, root: View): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((10 * density).toInt(), (7 * density).toInt(), (12 * density).toInt(), (7 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 60 * density
                setColor(Color.parseColor("#F4120E29"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#7F5AF0"))
            }
            elevation = 28f
            gravity = Gravity.CENTER_VERTICAL
        }

        // Drag Handle
        val dragHandle = TextView(context).apply {
            text = "⋮⋮"
            setTextColor(Color.parseColor("#A58BFF"))
            textSize = 16f
            setPadding((4 * density).toInt(), 0, (8 * density).toInt(), 0)
            gravity = Gravity.CENTER
        }
        bar.addView(dragHandle)

        attachDragListener(dragHandle, params, root)

        // Pause / Resume Button
        val pauseBtn = createActionButton(android.R.drawable.ic_media_pause, bgColor = "#251D4D", density) { view ->
            isPaused = !isPaused
            onPauseResumeClick(isPaused)
            (view as ImageView).setImageResource(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
            )
        }
        bar.addView(pauseBtn)

        // Stop Button (Vibrant Pink Disc)
        val stopBtn = createActionButton(android.R.drawable.ic_delete, bgColor = "#F0446A", density) {
            onStopClick()
        }
        bar.addView(stopBtn)

        // Pen & Tools Toggle Button
        val penBtn = createActionButton(android.R.drawable.ic_menu_edit, bgColor = "#251D4D", density) {
            onPenToggleClick()
        }
        bar.addView(penBtn)

        // Screenshot Button
        val cameraBtn = createActionButton(android.R.drawable.ic_menu_camera, bgColor = "#251D4D", density) {
            onScreenshotClick()
        }
        bar.addView(cameraBtn)

        // Mic Mute Button
        val micBtn = createActionButton(android.R.drawable.stat_notify_chat, bgColor = "#251D4D", density) { view ->
            isMuted = !isMuted
            onMicMuteClick(isMuted)
            view.alpha = if (isMuted) 0.35f else 1.0f
        }
        bar.addView(micBtn)

        // Collapse Button (<)
        val collapseBtn = createActionButton(android.R.drawable.ic_media_previous, bgColor = "#1E1642", density) {
            toggleExpandCollapse()
        }
        bar.addView(collapseBtn)

        return bar
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildCollapsedBubble(density: Float, params: WindowManager.LayoutParams, root: View): FrameLayout {
        val bubbleSize = (44 * density).toInt()

        val bubble = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#F4141029"))
                setStroke((2f * density).toInt(), Color.parseColor("#7F5AF0"))
            }
            elevation = 28f
        }

        // Inner Record Dot
        val dot = View(context).apply {
            val dotSize = (16 * density).toInt()
            layoutParams = FrameLayout.LayoutParams(dotSize, dotSize).apply {
                gravity = Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#F0446A"))
            }
        }
        bubble.addView(dot)

        // Dragging & Click to expand on bubble
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 6 || abs(dy) > 6) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(root, params)
                        } catch (e: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        toggleExpandCollapse()
                    }
                    true
                }
                else -> false
            }
        }

        return bubble
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragListener(view: View, params: WindowManager.LayoutParams, root: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
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
                        windowManager.updateViewLayout(root, params)
                    } catch (e: Exception) {}
                    true
                }
                else -> false
            }
        }
    }

    private fun createActionButton(iconRes: Int, bgColor: String, density: Float, onClick: (View) -> Unit): ImageView {
        val btnSize = (36 * density).toInt()
        val pad = (8 * density).toInt()

        return ImageView(context).apply {
            setImageResource(iconRes)
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                setMargins((3 * density).toInt(), 0, (3 * density).toInt(), 0)
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

    fun remove() {
        rootLayout?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {}
            rootLayout = null
            expandedBar = null
            collapsedBubble = null
        }
    }
}
