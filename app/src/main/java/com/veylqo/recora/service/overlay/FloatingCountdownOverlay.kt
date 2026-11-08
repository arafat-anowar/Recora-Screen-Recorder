package com.veylqo.recora.service.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.ceil

class FloatingCountdownOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootLayout: View? = null
    private var progressAnimator: ValueAnimator? = null

    /**
     * Compact, custom canvas-drawn countdown dial (100% zero-blinking, zero square box artifacts)
     */
    private class CountdownDialView(context: Context) : View(context) {
        val density = resources.displayMetrics.density

        var progress: Float = 1.0f
            set(value) {
                field = value
                invalidate()
            }

        var currentSeconds: Int = 3
            set(value) {
                if (field != value) {
                    field = value
                    invalidate()
                }
            }

        // Paints for pure hardware-accelerated canvas rendering
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#E60D0A1E") // Translucent obsidian glass
        }

        private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 14 * density
            color = Color.parseColor("#388B5CF6") // Soft purple neon glow
        }

        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5.5f * density
            color = Color.parseColor("#308B5CF6") // Dimmed purple track
        }

        private val activeArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5.5f * density
            strokeCap = Paint.Cap.BUTT
            color = Color.parseColor("#8B5CF6") // Recora signature primary purple
        }

        private val numberTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 38 * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        private val subtitleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C4B5FD") // Soft lavender
            textSize = 10 * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        private val clockTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13 * density
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        private val arcRect = RectF()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val strokeWidth = 5.5f * density
            val radius = cx - (strokeWidth / 2f) - (6 * density)

            // 1. Translucent Dark Glass Circle
            canvas.drawCircle(cx, cy, radius, bgPaint)

            // 2. Soft Outer Neon Purple Glow
            canvas.drawCircle(cx, cy, radius, outerGlowPaint)

            // 3. Full Background Dimmed Purple Track
            canvas.drawCircle(cx, cy, radius, trackPaint)

            // 4. Active Chronograph Purple Progress Arc (Depletes smoothly clockwise from 12 o'clock)
            val sweepAngle = 360f * progress
            val startAngle = -90f + (360f * (1f - progress))
            arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawArc(arcRect, startAngle, sweepAngle, false, activeArcPaint)

            // 5. Giant Center Number (3, 2, 1)
            canvas.drawText("$currentSeconds", cx, cy - (6 * density), numberTextPaint)

            // 6. Subtitle "Starts in"
            canvas.drawText("Starts in", cx, cy + (10 * density), subtitleTextPaint)

            // 7. Monospace Digital Clock ("00:03", "00:02", "00:01")
            val clockStr = String.format(Locale.US, "00:%02d", currentSeconds)
            canvas.drawText(clockStr, cx, cy + (26 * density), clockTextPaint)
        }
    }

    @SuppressLint("SetTextI18n")
    fun startCountdown(
        initialSeconds: Int,
        onComplete: () -> Unit,
        onCancel: () -> Unit
    ) {
        remove()

        val density = context.resources.displayMetrics.density
        val totalSec = if (initialSeconds <= 0) 3 else initialSeconds

        // Center floating minimalist countdown on user's mobile home screen (Transparent overlay)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Compact 116dp Dial View
        val dialSize = (116 * density).toInt()
        val dialView = CountdownDialView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dialSize, dialSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        container.addView(dialView)

        // Sleek Minimalist "✕ Cancel" Pill underneath (0 elevation to avoid any square box artifacts)
        val cancelPill = TextView(context).apply {
            text = "✕ Cancel"
            textSize = 12f
            setTextColor(Color.parseColor("#E0D6FF"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val padH = (18 * density).toInt()
            val padV = (6 * density).toInt()
            setPadding(padH, padV, padH, padV)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (10 * density).toInt()
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20 * density
                setColor(Color.parseColor("#E6141029"))
                setStroke((1.2f * density).toInt(), Color.parseColor("#4C3877"))
            }
            elevation = 0f
            isClickable = true
            isFocusable = false
            setOnClickListener {
                remove()
                onCancel()
            }
        }
        container.addView(cancelPill)

        rootLayout = container

        try {
            windowManager.addView(container, params)
        } catch (e: Exception) {
            remove()
            onComplete()
            return
        }

        // Continuous smooth linear chronograph animation (120 FPS, zero blinking)
        progressAnimator = ValueAnimator.ofFloat(1.0f, 0.0f).apply {
            duration = totalSec * 1000L
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                dialView.progress = fraction
                val sec = ceil(fraction * totalSec).toInt().coerceIn(1, totalSec)
                dialView.currentSeconds = sec
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Smooth, elegant exit
                    container.animate()
                        .alpha(0f)
                        .scaleX(0.7f)
                        .scaleY(0.7f)
                        .setDuration(160)
                        .withEndAction {
                            remove()
                            onComplete()
                        }
                        .start()
                }
            })
            start()
        }
    }

    fun remove() {
        progressAnimator?.cancel()
        progressAnimator = null

        rootLayout?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
            rootLayout = null
        }
    }
}
