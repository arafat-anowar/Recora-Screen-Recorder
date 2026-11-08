package com.veylqo.recora.service.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class FloatingCountdownOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootLayout: View? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null

    @SuppressLint("SetTextI18n")
    fun startCountdown(
        initialSeconds: Int,
        onComplete: () -> Unit,
        onCancel: () -> Unit
    ) {
        remove()

        val density = context.resources.displayMetrics.density
        var remaining = if (initialSeconds <= 0) 3 else initialSeconds

        // Center floating minimalist countdown (Zero dark scrim, completely transparent home screen)
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

        // Glowing Neon Red Circular Countdown Badge matching app theme (96dp)
        val badgeSize = (96 * density).toInt()
        val ring = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(badgeSize, badgeSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E60A0710"))
                setStroke((4 * density).toInt(), Color.parseColor("#FF2B54"))
            }
            elevation = 0f
        }

        // Giant Center Number (3, 2, 1)
        val numberText = TextView(context).apply {
            text = "$remaining"
            textSize = 48f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        ring.addView(numberText)
        container.addView(ring)

        // Sleek Minimalist "✕ Cancel" Pill directly underneath
        val cancelPill = TextView(context).apply {
            text = "✕ Cancel"
            textSize = 12f
            setTextColor(Color.parseColor("#E0D6FF"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val padH = (20 * density).toInt()
            val padV = (7 * density).toInt()
            setPadding(padH, padV, padH, padV)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (12 * density).toInt()
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24 * density
                setColor(Color.parseColor("#CC171233"))
                setStroke((1.2f * density).toInt(), Color.parseColor("#5A458D"))
            }
            elevation = 30f
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

        // Countdown Ticker Loop with punchy scale-in animation
        val ticker = object : Runnable {
            override fun run() {
                if (remaining > 1) {
                    remaining--
                    numberText.text = "$remaining"

                    countdownRunnable = this
                    mainHandler.postDelayed(this, 1000)
                } else {
                    // Smooth exit animation
                    ring.animate()
                        .scaleX(0.4f)
                        .scaleY(0.4f)
                        .alpha(0f)
                        .setDuration(180)
                        .withEndAction {
                            remove()
                            onComplete()
                        }
                        .start()
                }
            }
        }

        countdownRunnable = ticker
        mainHandler.postDelayed(ticker, 1000)
    }

    fun remove() {
        countdownRunnable?.let { mainHandler.removeCallbacks(it) }
        countdownRunnable = null

        rootLayout?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
            rootLayout = null
        }
    }
}
