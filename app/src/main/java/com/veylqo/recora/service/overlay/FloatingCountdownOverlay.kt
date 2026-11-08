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
    private var rootLayout: FrameLayout? = null
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

        // Semi-transparent cosmic scrim so the mobile home screen is visible underneath!
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#990A0818"))
        }

        // Top Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((28 * density).toInt(), (52 * density).toInt(), 0, 0)
        }
        val appTitle = TextView(context).apply {
            text = "Recora"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val subTitle = TextView(context).apply {
            text = "Starting soon"
            textSize = 14f
            setTextColor(Color.parseColor("#B6B0D4"))
        }
        header.addView(appTitle)
        header.addView(subTitle)
        root.addView(header)

        // Center Column: Ring + Text + Cancel Button
        val centerCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        // 240dp Circular Ring
        val ringSize = (240 * density).toInt()
        val ring = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(ringSize, ringSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke((5 * density).toInt(), Color.parseColor("#7F5AF0"))
            }
        }

        // Center Number
        val numberText = TextView(context).apply {
            text = "$remaining"
            textSize = 86f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        ring.addView(numberText)
        centerCol.addView(ring)

        // Status Text: "Recording starts in 3..."
        val statusText = TextView(context).apply {
            text = "Recording starts in $remaining..."
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, (32 * density).toInt(), 0, (24 * density).toInt())
        }
        centerCol.addView(statusText)

        // Cancel Button Centered Underneath
        val cancelBtn = TextView(context).apply {
            text = "Cancel"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding((44 * density).toInt(), (12 * density).toInt(), (44 * density).toInt(), (12 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 32 * density
                setColor(Color.parseColor("#CC141029"))
                setStroke((1.2f * density).toInt(), Color.parseColor("#2A2352"))
            }
            isClickable = true
            isFocusable = false
            setOnClickListener {
                remove()
                onCancel()
            }
        }
        centerCol.addView(cancelBtn)

        root.addView(centerCol)
        rootLayout = root

        try {
            windowManager.addView(root, params)
        } catch (e: Exception) {
            remove()
            onComplete()
            return
        }

        // Countdown Ticker Loop with Scale Pulse Animation on Ring
        val ticker = object : Runnable {
            override fun run() {
                if (remaining > 1) {
                    remaining--
                    numberText.text = "$remaining"
                    statusText.text = "Recording starts in $remaining..."

                    // Scale pulse animation
                    ring.scaleX = 1.12f
                    ring.scaleY = 1.12f
                    ring.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(350)
                        .setInterpolator(OvershootInterpolator())
                        .start()

                    countdownRunnable = this
                    mainHandler.postDelayed(this, 1000)
                } else {
                    // Countdown complete!
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
