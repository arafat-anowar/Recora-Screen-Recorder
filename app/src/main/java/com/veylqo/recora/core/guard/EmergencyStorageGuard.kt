package com.veylqo.recora.core.guard

import android.os.StatFs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class EmergencyStorageGuard(
    private val checkIntervalMs: Long = 3000L
) {
    companion object {
        const val CRITICAL_THRESHOLD_BYTES = 50L * 1024L * 1024L // 50 MB
        const val WARNING_THRESHOLD_BYTES = 200L * 1024L * 1024L // 200 MB
    }

    enum class StorageStatus {
        OK,
        LOW_WARNING,
        DEPLETED_CRITICAL
    }

    private var monitoringJob: Job? = null

    fun getAvailableBytes(path: File): Long {
        return try {
            val stat = StatFs(path.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            path.freeSpace
        }
    }

    fun getTotalBytes(path: File): Long {
        return try {
            val stat = StatFs(path.absolutePath)
            stat.blockCountLong * stat.blockSizeLong
        } catch (_: Exception) {
            path.totalSpace
        }
    }

    fun evaluateStorage(path: File): StorageStatus {
        val free = getAvailableBytes(path)
        return when {
            free <= CRITICAL_THRESHOLD_BYTES -> StorageStatus.DEPLETED_CRITICAL
            free <= WARNING_THRESHOLD_BYTES -> StorageStatus.LOW_WARNING
            else -> StorageStatus.OK
        }
    }

    fun canStartRecording(path: File): Boolean {
        return getAvailableBytes(path) > CRITICAL_THRESHOLD_BYTES
    }

    fun startMonitoring(
        scope: CoroutineScope,
        targetDir: File,
        onWarning: (freeBytes: Long) -> Unit,
        onCriticalEmergencyStop: (freeBytes: Long) -> Unit
    ) {
        stopMonitoring()
        monitoringJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val free = getAvailableBytes(targetDir)
                if (free <= CRITICAL_THRESHOLD_BYTES) {
                    onCriticalEmergencyStop(free)
                    break
                } else if (free <= WARNING_THRESHOLD_BYTES) {
                    onWarning(free)
                }
                delay(checkIntervalMs)
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
}
