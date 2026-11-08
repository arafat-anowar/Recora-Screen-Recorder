package com.veylqo.recora.service.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
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
        private val SIZES_DP = listOf(110, 150, 190)
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
            x = 60
            y = 350
        }

        val frame = FrameLayout(context).apply {
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.BLACK)
                setStroke((3 * density).toInt(), android.graphics.Color.parseColor("#7F5AF0"))
            }
            elevation = 25f
        }

        // Camera Preview
        val preview = PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        previewView = preview
        frame.addView(preview, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Small Close Button at top right
        val closeBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setPadding(10, 10, 10, 10)
            setColorFilter(android.graphics.Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.parseColor("#99000000"))
            }
            val btnSize = (28 * density).toInt()
            layoutParams = FrameLayout.LayoutParams(btnSize, btnSize).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(0, 10, 10, 0)
            }
            setOnClickListener {
                remove()
                onCloseClick()
            }
        }
        frame.addView(closeBtn)

        // Dragging and Resizing Touch Handler
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var lastClickTime = 0L

        frame.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    // Double tap to resize
                    val clickTime = System.currentTimeMillis()
                    if (clickTime - lastClickTime < 300) {
                        currentSizeIndex = (currentSizeIndex + 1) % SIZES_DP.size
                        val newSizePx = (SIZES_DP[currentSizeIndex] * density).toInt()
                        params.width = newSizePx
                        params.height = newSizePx
                        try {
                            windowManager.updateViewLayout(frame, params)
                        } catch (e: Exception) {}
                    }
                    lastClickTime = clickTime
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 6 || abs(dy) > 6) {
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

                val cameraSelector = if (cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding CameraX", e)
        }

        rootLayout?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // Already removed
            }
            rootLayout = null
            previewView = null
        }
    }
}
