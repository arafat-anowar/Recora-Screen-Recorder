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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Stack

class PenToolsOverlay(
    private val context: Context,
    private val onCloseClick: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootLayout: FrameLayout? = null
    private var drawingCanvasView: DrawingCanvasView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var currentColor = Color.parseColor("#7F5AF0")
    private var currentStrokeWidth = 12f
    private var isEraser = false
    private var isTouchThrough = false

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (rootLayout != null) return

        val params = WindowManager.LayoutParams(
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
        windowParams = params

        val frame = FrameLayout(context)

        // 1. Drawing Canvas
        val canvasView = DrawingCanvasView(context)
        drawingCanvasView = canvasView
        frame.addView(canvasView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // 2. Draggable Toolbar at Top Center
        val toolbar = createToolbar(frame, params)
        val density = context.resources.displayMetrics.density
        val toolbarParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = (50 * density).toInt()
        }
        frame.addView(toolbar, toolbarParams)

        rootLayout = frame
        try {
            windowManager.addView(frame, params)
        } catch (e: Exception) {
            rootLayout = null
        }
    }

    fun remove() {
        rootLayout?.let { frame ->
            try {
                windowManager.removeView(frame)
            } catch (e: Exception) {}
            rootLayout = null
            drawingCanvasView = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createToolbar(frame: FrameLayout, params: WindowManager.LayoutParams): View {
        val density = context.resources.displayMetrics.density

        val toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((12 * density).toInt(), (8 * density).toInt(), (14 * density).toInt(), (8 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 60 * density
                setColor(Color.parseColor("#F0120E29"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#7F5AF0"))
            }
            elevation = 26f
            gravity = Gravity.CENTER_VERTICAL
        }

        // Drag handle for moving toolbar
        val dragHandle = TextView(context).apply {
            text = "⋮⋮"
            setTextColor(Color.parseColor("#A58BFF"))
            textSize = 15f
            setPadding((4 * density).toInt(), 0, (6 * density).toInt(), 0)
        }
        toolbar.addView(dragHandle)

        var tInitialX = 0f
        var tInitialY = 0f
        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    tInitialX = event.rawX - toolbar.x
                    tInitialY = event.rawY - toolbar.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    toolbar.x = event.rawX - tInitialX
                    toolbar.y = event.rawY - tInitialY
                    true
                }
                else -> false
            }
        }

        // Color dots
        val purpleDot = createColorDot(Color.parseColor("#7F5AF0"))
        val pinkDot = createColorDot(Color.parseColor("#F0446A"))
        val goldDot = createColorDot(Color.parseColor("#FFD21F"))
        val whiteDot = createColorDot(Color.WHITE)

        val colorDots = listOf(purpleDot, pinkDot, goldDot, whiteDot)

        fun selectColor(selectedDot: View, color: Int) {
            currentColor = color
            isEraser = false
            colorDots.forEach { dot ->
                (dot.background as GradientDrawable).setStroke(
                    if (dot == selectedDot) (3 * density).toInt() else 0,
                    Color.WHITE
                )
            }
        }

        purpleDot.setOnClickListener { selectColor(it, Color.parseColor("#7F5AF0")) }
        pinkDot.setOnClickListener { selectColor(it, Color.parseColor("#F0446A")) }
        goldDot.setOnClickListener { selectColor(it, Color.parseColor("#FFD21F")) }
        whiteDot.setOnClickListener { selectColor(it, Color.WHITE) }

        // Initial selection
        selectColor(purpleDot, Color.parseColor("#7F5AF0"))

        toolbar.addView(purpleDot)
        toolbar.addView(pinkDot)
        toolbar.addView(goldDot)
        toolbar.addView(whiteDot)

        // Eraser button
        val eraserBtn = createToolButton(android.R.drawable.ic_menu_delete) { view ->
            isEraser = !isEraser
            view.alpha = if (isEraser) 1.0f else 0.45f
            if (isEraser) {
                colorDots.forEach { dot ->
                    (dot.background as GradientDrawable).setStroke(0, Color.TRANSPARENT)
                }
            }
        }
        eraserBtn.alpha = 0.45f
        toolbar.addView(eraserBtn)

        // Undo button
        toolbar.addView(createToolButton(android.R.drawable.ic_menu_revert) {
            drawingCanvasView?.undo()
        })

        // Clear all button
        toolbar.addView(createToolButton(android.R.drawable.ic_menu_close_clear_cancel) {
            drawingCanvasView?.clear()
        })

        // Close button
        toolbar.addView(createToolButton(android.R.drawable.ic_lock_power_off) {
            remove()
            onCloseClick()
        })

        return toolbar
    }

    private fun createColorDot(color: Int): View {
        val density = context.resources.displayMetrics.density
        val size = (24 * density).toInt()

        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            isClickable = true
        }
    }

    private fun createToolButton(iconRes: Int, onClick: (View) -> Unit): ImageView {
        val density = context.resources.displayMetrics.density
        val size = (34 * density).toInt()
        val pad = (7 * density).toInt()

        return ImageView(context).apply {
            setImageResource(iconRes)
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
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
            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    previousX = x
                    previousY = y
                    val path = Path().apply {
                        moveTo(x, y)
                    }
                    currentPath = path

                    val paint = Paint().apply {
                        isAntiAlias = true
                        isDither = true
                        style = Paint.Style.STROKE
                        strokeJoin = Paint.Join.ROUND
                        strokeCap = Paint.Cap.ROUND
                        strokeWidth = if (isEraser) 48f else currentStrokeWidth
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
