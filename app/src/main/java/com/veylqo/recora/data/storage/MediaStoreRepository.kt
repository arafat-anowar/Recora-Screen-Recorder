package com.veylqo.recora.data.storage

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import com.veylqo.recora.data.model.ClipItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaStoreRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    companion object {
        private const val TAG = "MediaStoreRepository"
        const val RELATIVE_SUBDIRECTORY = "Movies/Recora"
    }

    suspend fun createPendingVideoUri(fileName: String): Uri? = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, RELATIVE_SUBDIRECTORY)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        try {
            contentResolver.insert(collection, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating pending video uri", e)
            null
        }
    }

    suspend fun finalizeVideoUri(uri: Uri) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            try {
                contentResolver.update(uri, values, null, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear IS_PENDING on $uri", e)
            }
        }
    }

    suspend fun queryRecordedVideos(): List<ClipItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ClipItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.RELATIVE_PATH
        )

        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$RELATIVE_SUBDIRECTORY%")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Untitled.mp4"
                    val durationMs = cursor.getLong(durCol)
                    val sizeBytes = cursor.getLong(sizeCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val dateAddedSec = cursor.getLong(dateCol)

                    val uri = ContentUris.withAppendedId(collection, id)

                    val resolutionBadge = when {
                        width >= 3840 || height >= 3840 -> "4K UHD"
                        width >= 2560 || height >= 2560 -> "2K QHD"
                        width >= 1920 || height >= 1920 -> "1080p FHD"
                        width > 0 -> "${width}p"
                        else -> "1080p FHD"
                    }

                    val durationFormatted = formatDuration(durationMs)
                    val sizeFormatted = formatFileSize(sizeBytes)
                    val dateFormatted = formatDate(dateAddedSec * 1000)

                    list.add(
                        ClipItem(
                            id = id.toString(),
                            title = name,
                            durationText = durationFormatted,
                            resolutionBadge = resolutionBadge,
                            fpsBadge = "60fps",
                            sizeText = sizeFormatted,
                            dateText = dateFormatted,
                            uriString = uri.toString()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying recorded videos", e)
        }

        list
    }

    suspend fun deleteVideo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val count = contentResolver.delete(uri, null, null)
            count > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting video $uri", e)
            false
        }
    }

    suspend fun renameVideo(uri: Uri, newDisplayName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, newDisplayName)
            }
            val count = contentResolver.update(uri, values, null, null)
            count > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error renaming video $uri", e)
            false
        }
    }

    fun getStorageHeadroom(): StorageInfo {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val usedBytes = totalBytes - availableBytes
            val freeGb = availableBytes / (1024f * 1024f * 1024f)
            val progress = (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)

            StorageInfo(
                freeText = String.format(Locale.US, "%.1f GB free", freeGb),
                usedProgress = progress
            )
        } catch (e: Exception) {
            StorageInfo("Storage ready", 0.35f)
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }

    private fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 MB"
        val mb = sizeBytes / (1024f * 1024f)
        return if (mb >= 1024) {
            String.format(Locale.US, "%.2f GB", mb / 1024f)
        } else {
            String.format(Locale.US, "%.1f MB", mb)
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

data class StorageInfo(
    val freeText: String,
    val usedProgress: Float
)
