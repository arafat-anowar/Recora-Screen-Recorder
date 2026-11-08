package com.veylqo.recora.service.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlin.math.abs

class FacecamOverlay(
    private val context: Context,
    private val onCloseClick: () -> Unit
) : LifecycleOwner {

    companion object {
        private const val TAG = "FacecamOverlay"
        private val SIZES_DP = listOf(115, 155, 195)
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootLayout: FrameLayout? = null
    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var currentSizeIndex = 0

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (rootLayout != null) return

        val density = context.resources.displayMetrics.density
        val sizePx = (SIZES_DP[currentSizeIndex] * density).toInt()

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
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
            x = (24 * density).toInt()
            y = (240 * density).toInt()
        }

        val frame = FrameLayout(context).apply {
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.BLACK)
                setStroke((3 * density).toInt(), Color.parseColor("#7F5AF0"))
            }
            elevation = 28f
        }

        // Camera Preview
        val preview = PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        previewView = preview
        frame.addView(preview, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Dedicated Touch Drag Overlay that leaves the close button clickable
        val dragOverlay = View(context)
        frame.addView(dragOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Close Button
        val closeBtnSize = (28 * density).toInt()
        val closeBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            val p = (5 * density).toInt()
            setPadding(p, p, p, p)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC141029"))
                setStroke((1 * density).toInt(), Color.parseColor("#7F5AF0"))
            }
            elevation = 30f
            isClickable = true
            isFocusable = false
            setOnClickListener {
                remove()
                onCloseClick()
            }
        }
        val closeParams = FrameLayout.LayoutParams(closeBtnSize, closeBtnSize).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, (8 * density).toInt(), (8 * density).toInt(), 0)
        }
        frame.addView(closeBtn, closeParams)

        // Dragging & Double-tap resizing strictly on dragOverlay
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var lastClickTime = 0L

        dragOverlay.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    // Double-tap resize
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime < 320) {
                        currentSizeIndex = (currentSizeIndex + 1) % SIZES_DP.size
                        val newSizePx = (SIZES_DP[currentSizeIndex] * density).toInt()
                        params.width = newSizePx
                        params.height = newSizePx
                        try {
                            windowManager.updateViewLayout(frame, params)
                        } catch (e: Exception) {}
                    }
                    lastClickTime = now
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 5 || abs(dy) > 5) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(frame, params)
                        } catch (e: Exception) {}
                    }
                    true
                }
                else -> false
            }
        }

        rootLayout = frame
        try {
            windowManager.addView(frame, params)
            startCamera(preview)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add Facecam window", e)
            rootLayout = null
        }
    }

    private fun startCamera(previewView: PreviewView) {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val hasFront = cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true
                val selector = if (hasFront) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, selector, preview)
            } catch (e: Exception) {
                Log.e(TAG, "Error binding CameraX to Facecam lifecycle", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun remove() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        try {
            cameraProvider?.unbindAll()
            cameraProvider = null
        } catch (e: Exception) {}

        rootLayout?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {}
            rootLayout = null
            previewView = null
        }
    }
}
