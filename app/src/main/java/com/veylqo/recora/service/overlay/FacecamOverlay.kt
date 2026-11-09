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
import android.widget.LinearLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.veylqo.recora.R
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
    private var rootLayout: LinearLayout? = null
    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var currentSizeIndex = 0
    private var currentRotationDegrees = 0f
    private var isFrontCamera = true

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (rootLayout != null) return

        val density = context.resources.displayMetrics.density
        val sizePx = (SIZES_DP[currentSizeIndex] * density).toInt()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (24 * density).toInt()
            y = (180 * density).toInt()
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // =====================================================================
        // 1. Sleek Action Bar OUTSIDE the Circle (Rotate, Switch Camera, Kata/Close)
        // =====================================================================
        val toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24 * density
                setColor(Color.parseColor("#F4141029"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#7F5AF0"))
            }
            elevation = 32f
            gravity = Gravity.CENTER_VERTICAL
        }

        // Action 1: Rotate Button (🔄)
        val rotateBtn = createExternalToolButton(
            iconRes = R.drawable.ic_control_rotate,
            density = density
        ) {
            currentRotationDegrees = (currentRotationDegrees + 90f) % 360f
            previewView?.animate()?.rotation(currentRotationDegrees)?.setDuration(220)?.start()
        }
        toolbar.addView(rotateBtn)

        // Action 2: Camera Switch Button (📷⇄)
        val switchBtn = createExternalToolButton(
            iconRes = R.drawable.ic_control_switch_cam,
            density = density
        ) {
            isFrontCamera = !isFrontCamera
            previewView?.let { bindCameraPreview(it) }
        }
        toolbar.addView(switchBtn)

        // Action 3: Kata / Close Button (✕)
        val closeBtn = createExternalToolButton(
            iconRes = R.drawable.ic_control_close,
            density = density
        ) {
            remove()
            onCloseClick()
        }
        toolbar.addView(closeBtn)

        container.addView(toolbar)

        // =====================================================================
        // 2. Pure Clean Circular Camera Bubble (Zero buttons inside covering face!)
        // =====================================================================
        val circleFrame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                topMargin = (8 * density).toInt()
            }
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val w = if (view.width > 0) view.width else sizePx
                    val h = if (view.height > 0) view.height else sizePx
                    outline.setOval(0, 0, w, h)
                }
            }
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                invalidateOutline()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.BLACK)
                setStroke((3 * density).toInt(), Color.parseColor("#7F5AF0"))
            }
            elevation = 0f
        }

        // Camera PreviewView inside the circle
        val preview = PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            rotation = currentRotationDegrees
        }
        previewView = preview
        circleFrame.addView(preview, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        container.addView(circleFrame)

        // =====================================================================
        // 3. Smooth Dragging & Double-Tap Resizing across circle and toolbar
        // =====================================================================
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var lastClickTime = 0L

        val dragListener = View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    val now = System.currentTimeMillis()
                    if (now - lastClickTime < 320) {
                        currentSizeIndex = (currentSizeIndex + 1) % SIZES_DP.size
                        val newSizePx = (SIZES_DP[currentSizeIndex] * density).toInt()
                        circleFrame.layoutParams = (circleFrame.layoutParams as LinearLayout.LayoutParams).apply {
                            width = newSizePx
                            height = newSizePx
                        }
                        circleFrame.requestLayout()
                    }
                    lastClickTime = now
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 4 || abs(dy) > 4) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(container, params)
                        } catch (e: Exception) {}
                    }
                    true
                }
                else -> false
            }
        }

        circleFrame.setOnTouchListener(dragListener)

        rootLayout = container
        try {
            windowManager.addView(container, params)
            startCamera(preview)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add Facecam window", e)
            rootLayout = null
        }
    }

    private fun createExternalToolButton(iconRes: Int, density: Float, onClick: () -> Unit): ImageView {
        val btnSize = (32 * density).toInt()
        val pad = (6 * density).toInt()

        return ImageView(context).apply {
            setImageResource(iconRes)
            setPadding(pad, pad, pad, pad)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#221A47"))
            }
            elevation = 34f
            isClickable = true
            isFocusable = false
            setOnClickListener { onClick() }
        }
    }

    private fun startCamera(previewView: PreviewView) {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        bindCameraPreview(previewView)
    }

    private fun bindCameraPreview(previewView: PreviewView) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot bind camera: CAMERA permission not granted")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val hasFront = cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true
                val hasBack = cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) == true

                val selector = if (isFrontCamera && hasFront) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else if (!isFrontCamera && hasBack) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else if (hasFront) {
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
