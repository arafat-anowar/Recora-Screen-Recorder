package com.veylqo.recora.service.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Stack

class PenToolsOverlay(
    private val context: Context,
    private val onCloseClick: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Window 1: Fullscreen Transparent Canvas
    private var canvasView: DrawingCanvasView? = null
    private var canvasParams: WindowManager.LayoutParams? = null

    // Window 2: Draggable Floating Toolbar Pill
    private var toolbarView: View? = null
    private var toolbarParams: WindowManager.LayoutParams? = null

    // Drawing state
    private var currentColor = Color.parseColor("#7F5AF0")
    private var currentStrokeWidth = 12f
    private var isEraser = false
    private var isTouchThrough = false

    private val STROKE_WIDTHS = listOf(6f, 14f, 26f)
    private var strokeIndex = 1

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (canvasView != null || toolbarView != null) return

        val density = context.resources.displayMetrics.density

        // 1. Setup Fullscreen Drawing Canvas Window
        val cParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        canvasParams = cParams

        val canvas = DrawingCanvasView(context)
        canvasView = canvas

        // 2. Setup Floating Toolbar Window
        val tParams = WindowManager.LayoutParams(
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
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (45 * density).toInt()
        }
        toolbarParams = tParams

        val toolbar = createToolbarView(density, tParams)
        toolbarView = toolbar

        try {
            windowManager.addView(canvas, cParams)
            windowManager.addView(toolbar, tParams)
        } catch (e: Exception) {
            remove()
        }
    }

    private fun setTouchThroughMode(enabled: Boolean) {
        isTouchThrough = enabled
        canvasParams?.let { params ->
            if (enabled) {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            } else {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            }
            try {
                canvasView?.let { windowManager.updateViewLayout(it, params) }
            } catch (e: Exception) {}
        }
    }

    fun remove() {
        canvasView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            canvasView = null
        }
        toolbarView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            toolbarView = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createToolbarView(density: Float, tParams: WindowManager.LayoutParams): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((10 * density).toInt(), (7 * density).toInt(), (10 * density).toInt(), (7 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 32 * density
                setColor(Color.parseColor("#F2141029"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#7F5AF0"))
            }
            elevation = 30f
            gravity = Gravity.CENTER_VERTICAL
        }

        // 1. Drag Handle
        val dragHandle = TextView(context).apply {
            text = "⋮⋮"
            setTextColor(Color.parseColor("#A58BFF"))
            textSize = 15f
            setPadding((2 * density).toInt(), 0, (6 * density).toInt(), 0)
        }
        container.addView(dragHandle)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = tParams.x
                    initialY = tParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    tParams.x = initialX + dx
                    tParams.y = initialY + dy
                    try { windowManager.updateViewLayout(container, tParams) } catch (e: Exception) {}
                    true
                }
                else -> false
            }
        }

        // 2. Mode Switch: Draw Mode (✏️) vs Touch Screen Mode (👆)
        val modeBtn = createIconButton(android.R.drawable.ic_menu_edit, active = true, density) { view ->
            isTouchThrough = !isTouchThrough
            setTouchThroughMode(isTouchThrough)
            (view as ImageView).setImageResource(
                if (isTouchThrough) android.R.drawable.ic_input_get else android.R.drawable.ic_menu_edit
            )
            view.alpha = if (isTouchThrough) 0.5f else 1.0f
        }
        container.addView(modeBtn)

        // 3. Color Dots
        val purpleDot = createColorDot(Color.parseColor("#7F5AF0"), density)
        val pinkDot = createColorDot(Color.parseColor("#F0446A"), density)
        val goldDot = createColorDot(Color.parseColor("#FFD21F"), density)
        val cyanDot = createColorDot(Color.parseColor("#00E5FF"), density)
        val whiteDot = createColorDot(Color.WHITE, density)

        val dots = listOf(purpleDot, pinkDot, goldDot, cyanDot, whiteDot)

        fun selectColor(targetDot: View, color: Int) {
            currentColor = color
            isEraser = false
            setTouchThroughMode(false)
            modeBtn.setImageResource(android.R.drawable.ic_menu_edit)
            modeBtn.alpha = 1.0f
            dots.forEach { d ->
                (d.background as GradientDrawable).setStroke(
                    if (d == targetDot) (3 * density).toInt() else 0,
                    Color.WHITE
                )
            }
        }

        purpleDot.setOnClickListener { selectColor(it, Color.parseColor("#7F5AF0")) }
        pinkDot.setOnClickListener { selectColor(it, Color.parseColor("#F0446A")) }
        goldDot.setOnClickListener { selectColor(it, Color.parseColor("#FFD21F")) }
        cyanDot.setOnClickListener { selectColor(it, Color.parseColor("#00E5FF")) }
        whiteDot.setOnClickListener { selectColor(it, Color.WHITE) }

        selectColor(purpleDot, Color.parseColor("#7F5AF0"))

        dots.forEach { container.addView(it) }

        // 4. Stroke Thickness Toggle Button
        val strokeBtn = createIconButton(android.R.drawable.presence_online, active = false, density) { view ->
            strokeIndex = (strokeIndex + 1) % STROKE_WIDTHS.size
            currentStrokeWidth = STROKE_WIDTHS[strokeIndex]
            val scale = when (strokeIndex) {
                0 -> 0.7f
                1 -> 1.0f
                else -> 1.4f
            }
            view.scaleX = scale
            view.scaleY = scale
        }
        container.addView(strokeBtn)

        // 5. Eraser Button
        val eraserBtn = createIconButton(android.R.drawable.ic_menu_delete, active = false, density) { view ->
            isEraser = !isEraser
            setTouchThroughMode(false)
            modeBtn.setImageResource(android.R.drawable.ic_menu_edit)
            modeBtn.alpha = 1.0f
            view.alpha = if (isEraser) 1.0f else 0.45f
            if (isEraser) {
                dots.forEach { (it.background as GradientDrawable).setStroke(0, Color.TRANSPARENT) }
            }
        }
        eraserBtn.alpha = 0.45f
        container.addView(eraserBtn)

        // 6. Undo Button
        container.addView(createIconButton(android.R.drawable.ic_menu_revert, active = false, density) {
            canvasView?.undo()
        })

        // 7. Clear All Button
        container.addView(createIconButton(android.R.drawable.ic_menu_close_clear_cancel, active = false, density) {
            canvasView?.clear()
        })

        // 8. Close Button
        container.addView(createIconButton(android.R.drawable.ic_lock_power_off, active = false, density) {
            remove()
            onCloseClick()
        })

        return container
    }

    private fun createColorDot(color: Int, density: Float): View {
        val size = (22 * density).toInt()
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins((3 * density).toInt(), 0, (3 * density).toInt(), 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            isClickable = true
        }
    }

    private fun createIconButton(iconRes: Int, active: Boolean, density: Float, onClick: (View) -> Unit): ImageView {
        val size = (32 * density).toInt()
        val pad = (6 * density).toInt()
        return ImageView(context).apply {
            setImageResource(iconRes)
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins((2 * density).toInt(), 0, (2 * density).toInt(), 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#251D4D"))
            }
            setColorFilter(Color.WHITE)
            isClickable = true
            setOnClickListener { onClick(it) }
        }
    }

    private inner class DrawingCanvasView(ctx: Context) : View(ctx) {
        private val paths = Stack<DrawPath>()
        private var currentPath: Path? = null
        private var currentPaint: Paint? = null
        private var previousX = 0f
        private var previousY = 0f

        init {
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }

        fun undo() {
            if (paths.isNotEmpty()) {
                paths.pop()
                invalidate()
            }
        }

        fun clear() {
            paths.clear()
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            for (p in paths) {
                canvas.drawPath(p.path, p.paint)
            }
            if (currentPath != null && currentPaint != null) {
                canvas.drawPath(currentPath!!, currentPaint!!)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (isTouchThrough) return false

            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    previousX = x
                    previousY = y
                    val path = Path().apply { moveTo(x, y) }
                    currentPath = path

                    val paint = Paint().apply {
                        isAntiAlias = true
                        isDither = true
                        style = Paint.Style.STROKE
                        strokeJoin = Paint.Join.ROUND
                        strokeCap = Paint.Cap.ROUND
                        strokeWidth = if (isEraser) 52f else currentStrokeWidth
                        color = if (isEraser) Color.TRANSPARENT else currentColor
                        if (isEraser) {
                            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                        }
                    }
                    currentPaint = paint
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val midX = (previousX + x) / 2
                    val midY = (previousY + y) / 2
                    currentPath?.quadTo(previousX, previousY, midX, midY)
                    previousX = x
                    previousY = y
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    currentPath?.lineTo(x, y)
                    if (currentPath != null && currentPaint != null) {
                        paths.push(DrawPath(currentPath!!, currentPaint!!))
                        currentPath = null
                        currentPaint = null
                    }
                    invalidate()
                    return true
                }
            }
            return false
        }
    }

    private data class DrawPath(
        val path: Path,
        val paint: Paint
    )
}
