package com.veylqo.recora.service.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import java.util.Stack

class PenToolsOverlay(
    private val context: Context,
    private val onCloseClick: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootLayout: FrameLayout? = null
    private var drawingCanvasView: DrawingCanvasView? = null

    private var currentColor = android.graphics.Color.parseColor("#7F5AF0")
    private var currentStrokeWidth = 10f
    private var isEraser = false

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

        val frame = FrameLayout(context)

        // 1. Fullscreen transparent drawing canvas view
        val canvasView = DrawingCanvasView(context)
        drawingCanvasView = canvasView
        frame.addView(canvasView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // 2. Floating mini toolbar at top center
        val toolbar = createToolbar()
        val toolbarParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = 80
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
            } catch (e: Exception) {
                // Already removed
            }
            rootLayout = null
            drawingCanvasView = null
        }
    }

    private fun createToolbar(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 14, 20, 14)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 60f
                setColor(android.graphics.Color.parseColor("#E6141029"))
                setStroke(2, android.graphics.Color.parseColor("#7F5AF0"))
            }
            elevation = 20f

            // Color: Purple (#7F5AF0)
            addView(createColorDot(android.graphics.Color.parseColor("#7F5AF0")))
            // Color: Pink (#F0446A)
            addView(createColorDot(android.graphics.Color.parseColor("#F0446A")))
            // Color: Gold (#FFD21F)
            addView(createColorDot(android.graphics.Color.parseColor("#FFD21F")))
            // Color: White (#FFFFFF)
            addView(createColorDot(android.graphics.Color.WHITE))

            // Eraser
            addView(createToolButton(android.R.drawable.ic_menu_delete) {
                isEraser = !isEraser
                it.alpha = if (isEraser) 1.0f else 0.5f
            })

            // Undo
            addView(createToolButton(android.R.drawable.ic_menu_revert) {
                drawingCanvasView?.undo()
            })

            // Clear
            addView(createToolButton(android.R.drawable.ic_menu_close_clear_cancel) {
                drawingCanvasView?.clear()
            })

            // Close
            addView(createToolButton(android.R.drawable.btn_dialog) {
                remove()
                onCloseClick()
            })
        }
    }

    private fun createColorDot(color: Int): View {
        return View(context).apply {
            val size = 52
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(10, 0, 10, 0)
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(color)
                setStroke(2, android.graphics.Color.WHITE)
            }
            setOnClickListener {
                currentColor = color
                isEraser = false
            }
        }
    }

    private fun createToolButton(iconRes: Int, onClick: (View) -> Unit): View {
        return ImageView(context).apply {
            setImageResource(iconRes)
            val size = 70
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(8, 0, 8, 0)
            }
            setPadding(12, 12, 12, 12)
            setColorFilter(android.graphics.Color.WHITE)
            setOnClickListener { onClick(it) }
        }
    }

    private inner class DrawingCanvasView(ctx: Context) : View(ctx) {
        private val paths = Stack<DrawPath>()

        private var currentPath: Path? = null
        private var currentPaint: Paint? = null

        init {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
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
                    val path = Path()
                    path.moveTo(x, y)
                    currentPath = path

                    val paint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeJoin = Paint.Join.ROUND
                        strokeCap = Paint.Cap.ROUND
                        strokeWidth = if (isEraser) 40f else currentStrokeWidth
                        color = if (isEraser) android.graphics.Color.TRANSPARENT else currentColor
                        if (isEraser) {
                            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                        }
                    }
                    currentPaint = paint
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    currentPath?.lineTo(x, y)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
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
