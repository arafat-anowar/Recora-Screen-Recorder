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

        // Center floating card window: DOES NOT cover the home screen with dark scrim!
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

        // Floating Dark Glass Card Container
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val padHoriz = (24 * density).toInt()
            val padVert = (20 * density).toInt()
            setPadding(padHoriz, padVert, padHoriz, padVert)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 32 * density
                setColor(Color.parseColor("#EE141029"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#7F5AF0"))
            }
            elevation = 36f
        }

        // Top Tag: "Recora · Starting"
        val topTag = TextView(context).apply {
            text = "Recora · Starting in"
            textSize = 13f
            setTextColor(Color.parseColor("#B8A7FF"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (14 * density).toInt())
        }
        card.addView(topTag)

        // Glowing Purple Outer Ring
        val ringSize = (110 * density).toInt()
        val ring = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(ringSize, ringSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#261D4D"))
                setStroke((4 * density).toInt(), Color.parseColor("#7F5AF0"))
            }
        }

        // Center Countdown Number
        val numberText = TextView(context).apply {
            text = "$remaining"
            textSize = 52f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        ring.addView(numberText)
        card.addView(ring)

        // Cancel Button Centered Underneath
        val cancelBtn = TextView(context).apply {
            text = "Cancel"
            textSize = 13f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val btnPadH = (28 * density).toInt()
            val btnPadV = (8 * density).toInt()
            setPadding(btnPadH, btnPadV, btnPadH, btnPadV)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (16 * density).toInt()
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20 * density
                setColor(Color.parseColor("#33FFFFFF"))
                setStroke((1f * density).toInt(), Color.parseColor("#55FFFFFF"))
            }
            isClickable = true
            isFocusable = false
            setOnClickListener {
                remove()
                onCancel()
            }
        }
        card.addView(cancelBtn)

        rootLayout = card

        try {
            windowManager.addView(card, params)
        } catch (e: Exception) {
            remove()
            onComplete()
            return
        }

        // Ticker loop
        val ticker = object : Runnable {
            override fun run() {
                if (remaining > 1) {
                    remaining--
                    numberText.text = "$remaining"

                    // Pulse ring on each tick
                    ring.scaleX = 1.15f
                    ring.scaleY = 1.15f
                    ring.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(320)
                        .setInterpolator(OvershootInterpolator())
                        .start()

                    countdownRunnable = this
                    mainHandler.postDelayed(this, 1000)
                } else {
                    remove()
                    onComplete()
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
