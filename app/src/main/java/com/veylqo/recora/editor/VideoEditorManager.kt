package com.veylqo.recora.editor

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.veylqo.recora.data.storage.MediaStoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(UnstableApi::class)
class VideoEditorManager(
    private val context: Context,
    private val mediaStoreRepository: MediaStoreRepository
) {
    companion object {
        private const val TAG = "VideoEditorManager"
    }

    private var currentTransformer: Transformer? = null
    private var progressJob: Job? = null
    private val editorScope = CoroutineScope(Dispatchers.Main + Job())

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    fun updateDuration(durationMs: Long) {
        _editorState.value = _editorState.value.copy(
            durationMs = durationMs,
            trimEndMs = if (_editorState.value.trimEndMs == 0L) durationMs else _editorState.value.trimEndMs
        )
    }

    fun setTrimRange(startMs: Long, endMs: Long) {
        val safeStart = maxOf(0L, startMs)
        val safeEnd = minOf(_editorState.value.durationMs, endMs)
        if (safeEnd > safeStart) {
            _editorState.value = _editorState.value.copy(
                trimStartMs = safeStart,
                trimEndMs = safeEnd
            )
        }
    }

    fun setSpeed(speed: PlaybackSpeed) {
        _editorState.value = _editorState.value.copy(speed = speed)
    }

    fun toggleAudio() {
        _editorState.value = _editorState.value.copy(isAudioEnabled = !_editorState.value.isAudioEnabled)
    }

    fun setAspectRatio(ratio: EditorAspectRatio) {
        _editorState.value = _editorState.value.copy(aspectRatio = ratio)
    }

    fun cancelExport() {
        try {
            currentTransformer?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling transformer", e)
        }
        progressJob?.cancel()
        currentTransformer = null
        _editorState.value = _editorState.value.copy(
            isExporting = false,
            exportProgressPercent = 0
        )
    }

    fun exportVideo(
        sourceUri: Uri,
        onSuccess: (outputUri: Uri, newTitle: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val state = _editorState.value
        _editorState.value = state.copy(isExporting = true, exportProgressPercent = 0, exportError = null)

        val tempOutputFile = File(context.cacheDir, "recora_export_${System.currentTimeMillis()}.mp4")

        try {
            // 1. Build Clipping Configuration for Trim
            val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(state.trimStartMs)
                .setEndPositionMs(if (state.trimEndMs > 0) state.trimEndMs else state.durationMs)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(sourceUri)
                .setClippingConfiguration(clippingConfig)
                .build()

            // 2. Build Video Effects (Speed & Aspect Ratio Crop)
            val effectsList = mutableListOf<Effect>()

            // Speed effect
            if (state.speed != PlaybackSpeed.Normal) {
                effectsList.add(SpeedChangeEffect(state.speed.speed))
            }

            // Aspect Ratio Crop effect (Normalized crop bounds [-1f, 1f])
            when (state.aspectRatio) {
                EditorAspectRatio.Square1_1 -> {
                    // Crop left & right to 1:1 square
                    effectsList.add(Crop(-0.56f, 0.56f, -1.0f, 1.0f))
                }
                EditorAspectRatio.Portrait9_16 -> {
                    // 9:16 vertical crop
                    effectsList.add(Crop(-0.5625f, 0.5625f, -1.0f, 1.0f))
                }
                EditorAspectRatio.Landscape16_9 -> {
                    // 16:9 widescreen crop
                    effectsList.add(Crop(-1.0f, 1.0f, -0.5625f, 0.5625f))
                }
                EditorAspectRatio.Original -> {
                    // No crop applied
                }
            }

            // 3. Build EditedMediaItem with optional audio removal
            val editedItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(!state.isAudioEnabled)
                .setEffects(Effects(emptyList(), effectsList))
                .build()

            // 4. Build Transformer with Progress Listener
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        Log.i(TAG, "Transformer export completed successfully")
                        progressJob?.cancel()
                        currentTransformer = null

                        // 5. Save to MediaStore (Non-destructive new file)
                        editorScope.launch(Dispatchers.IO) {
                            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                            val newTitle = "Recora_Edited_${sdf.format(Date())}.mp4"
                            val destinationUri = mediaStoreRepository.createPendingVideoUri(newTitle)

                            if (destinationUri == null) {
                                withContext(Dispatchers.Main) {
                                    _editorState.value = _editorState.value.copy(
                                        isExporting = false,
                                        exportError = "Could not create MediaStore entry"
                                    )
                                    onError("Failed to create storage entry")
                                }
                                tempOutputFile.delete()
                                return@launch
                            }

                            val pfd = context.contentResolver.openFileDescriptor(destinationUri, "w")
                            if (pfd != null) {
                                try {
                                    FileInputStream(tempOutputFile).use { input ->
                                        FileOutputStream(pfd.fileDescriptor).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    pfd.close()
                                    mediaStoreRepository.finalizeVideoUri(destinationUri)

                                    withContext(Dispatchers.Main) {
                                        _editorState.value = _editorState.value.copy(
                                            isExporting = false,
                                            exportProgressPercent = 100
                                        )
                                        onSuccess(destinationUri, newTitle)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error copying exported video to MediaStore", e)
                                    withContext(Dispatchers.Main) {
                                        _editorState.value = _editorState.value.copy(
                                            isExporting = false,
                                            exportError = e.message ?: "Failed writing video"
                                        )
                                        onError(e.message ?: "Failed writing video")
                                    }
                                } finally {
                                    tempOutputFile.delete()
                                }
                            } else {
                                tempOutputFile.delete()
                                withContext(Dispatchers.Main) {
                                    _editorState.value = _editorState.value.copy(
                                        isExporting = false,
                                        exportError = "Cannot open destination FileDescriptor"
                                    )
                                    onError("Cannot open destination storage")
                                }
                            }
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        Log.e(TAG, "Transformer export failed", exportException)
                        progressJob?.cancel()
                        currentTransformer = null
                        tempOutputFile.delete()
                        _editorState.value = _editorState.value.copy(
                            isExporting = false,
                            exportError = exportException.message ?: "Export failed"
                        )
                        onError(exportException.message ?: "Export failed")
                    }
                })
                .build()

            currentTransformer = transformer
            transformer.start(editedItem, tempOutputFile.absolutePath)

            // 6. Polling Progress Loop
            progressJob = editorScope.launch {
                val progressHolder = ProgressHolder()
                while (isActive) {
                    delay(200)
                    val progressState = transformer.getProgress(progressHolder)
                    if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                        _editorState.value = _editorState.value.copy(
                            exportProgressPercent = progressHolder.progress
                        )
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start export", e)
            tempOutputFile.delete()
            _editorState.value = _editorState.value.copy(
                isExporting = false,
                exportError = e.message ?: "Could not start export"
            )
            onError(e.message ?: "Could not start export")
        }
    }
}
