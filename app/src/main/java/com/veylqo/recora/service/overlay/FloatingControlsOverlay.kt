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
import androidx.core.content.ContextCompat
import com.veylqo.recora.R
import kotlin.math.abs

class FloatingControlsOverlay(
    private val context: Context,
    private val onStopClick: () -> Unit,
    private val onPauseResumeClick: (isPaused: Boolean) -> Unit,
    private val onPenToggleClick: () -> Unit,
    private val onScreenshotClick: () -> Unit,
    private val onMicMuteClick: (isMuted: Boolean) -> Unit,
    private val onFacecamToggleClick: (() -> Unit)? = null
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootLayout: View? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var isExpanded = true
    private var isPaused = false
    private var isMuted = false

    private var expandedBar: LinearLayout? = null
    private var collapsedBubble: View? = null

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
            y = (140 * density).toInt()
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

    fun remove() {
        rootLayout?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
            rootLayout = null
        }
    }

    private fun toggleExpandCollapse() {
        isExpanded = !isExpanded
        val density = context.resources.displayMetrics.density
        val screenWidth = context.resources.displayMetrics.widthPixels

        if (isExpanded) {
            collapsedBubble?.visibility = View.GONE
            expandedBar?.visibility = View.VISIBLE

            // Prevent expanded bar from going off screen to the right
            windowParams?.let { p ->
                val approxBarWidth = (320 * density).toInt()
                if (p.x + approxBarWidth > screenWidth) {
                    p.x = maxOf((12 * density).toInt(), screenWidth - approxBarWidth - (12 * density).toInt())
                }
            }
        } else {
            expandedBar?.visibility = View.GONE
            collapsedBubble?.visibility = View.VISIBLE
        }

        rootLayout?.let { root ->
            windowParams?.let { params ->
                try {
                    windowManager.updateViewLayout(root, params)
                } catch (e: Exception) {}
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildExpandedBar(density: Float, params: WindowManager.LayoutParams, root: View): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 60 * density
                setColor(Color.parseColor("#EE120E29"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#7F5AF0"))
            }
            elevation = 32f
            gravity = Gravity.CENTER_VERTICAL
        }

        // Drag Handle
        val dragHandle = TextView(context).apply {
            text = "⠿"
            setTextColor(Color.parseColor("#B8A7FF"))
            textSize = 18f
            setPadding((6 * density).toInt(), 0, (8 * density).toInt(), 0)
            gravity = Gravity.CENTER
        }
        bar.addView(dragHandle)
        attachDragListener(dragHandle, params, root)

        // Pause / Resume Button
        val pauseBtn = createActionButton(R.drawable.ic_control_pause, bgColor = "#251D4D", density) { view ->
            isPaused = !isPaused
            onPauseResumeClick(isPaused)
            (view as ImageView).setImageResource(
                if (isPaused) R.drawable.ic_control_play else R.drawable.ic_control_pause
            )
        }
        bar.addView(pauseBtn)

        // Stop Button (Pink Disc with clean solid stop square)
        val stopBtn = createActionButton(R.drawable.ic_control_stop, bgColor = "#F0446A", density) {
            onStopClick()
        }
        bar.addView(stopBtn)

        // Pen & Tools Toggle Button
        val penBtn = createActionButton(R.drawable.ic_control_pen, bgColor = "#251D4D", density) {
            onPenToggleClick()
        }
        bar.addView(penBtn)

        // Screenshot Button
        val snapBtn = createActionButton(R.drawable.ic_control_screenshot, bgColor = "#251D4D", density) {
            onScreenshotClick()
        }
        bar.addView(snapBtn)

        // Facecam Toggle Button (if supported)
        onFacecamToggleClick?.let { onFacecam ->
            val facecamBtn = createActionButton(R.drawable.ic_control_camera, bgColor = "#251D4D", density) {
                onFacecam()
            }
            bar.addView(facecamBtn)
        }

        // Mic Mute Button
        val micBtn = createActionButton(R.drawable.ic_control_mic, bgColor = "#251D4D", density) { view ->
            isMuted = !isMuted
            onMicMuteClick(isMuted)
            (view as ImageView).setImageResource(
                if (isMuted) R.drawable.ic_control_mic_off else R.drawable.ic_control_mic
            )
            view.alpha = if (isMuted) 0.45f else 1.0f
        }
        bar.addView(micBtn)

        // Collapse Button (›)
        val collapseBtn = TextView(context).apply {
            text = "›"
            textSize = 22f
            setTextColor(Color.parseColor("#B8A7FF"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding((8 * density).toInt(), 0, (6 * density).toInt(), 0)
            isClickable = true
            setOnClickListener { toggleExpandCollapse() }
        }
        bar.addView(collapseBtn)

        return bar
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildCollapsedBubble(density: Float, params: WindowManager.LayoutParams, root: View): View {
        val bubbleSize = (48 * density).toInt()

        val bubble = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#F2120E29"))
                setStroke((2 * density).toInt(), Color.parseColor("#7F5AF0"))
            }
            elevation = 32f
        }

        val cameraIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_control_camera)
            setColorFilter(Color.parseColor("#B8A7FF"))
            val iconPad = (13 * density).toInt()
            setPadding(iconPad, iconPad, iconPad, iconPad)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        bubble.addView(cameraIcon)

        // Tiny blinking recording indicator dot
        val dot = View(context).apply {
            val dotSize = (8 * density).toInt()
            layoutParams = FrameLayout.LayoutParams(dotSize, dotSize).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = (6 * density).toInt()
                rightMargin = (6 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#F0446A"))
            }
        }
        bubble.addView(dot)

        // Dragging & Click to expand on collapsed bubble
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var hasMoved = false

        bubble.setOnTouchListener { _, event ->
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
                            windowManager.updateViewLayout(root, params)
                        } catch (e: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved) {
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
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                setMargins((3 * density).toInt(), 0, (3 * density).toInt(), 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(bgColor))
            }
            isClickable = true
            setOnClickListener { onClick(it) }
        }
    }
}
