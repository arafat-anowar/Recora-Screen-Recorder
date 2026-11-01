package com.veylqo.recora

import com.veylqo.recora.core.guard.EmergencyStorageGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EmergencyStorageGuardTest {

    private class MockStorageGuard(private val mockFreeBytes: Long) : EmergencyStorageGuard() {
        override fun getAvailableBytes(path: File): Long = mockFreeBytes
    }

    @Test
    fun testDepletedCriticalThreshold() {
        // Less than or equal to 50MB
        val criticalBytes = 40L * 1024L * 1024L
        val guard = MockStorageGuard(criticalBytes)
        val dummyFile = File(".")

        assertEquals(EmergencyStorageGuard.StorageStatus.DEPLETED_CRITICAL, guard.evaluateStorage(dummyFile))
        assertFalse(guard.canStartRecording(dummyFile))
    }

    @Test
    fun testLowWarningThreshold() {
        // Between 50MB and 200MB
        val warningBytes = 150L * 1024L * 1024L
        val guard = MockStorageGuard(warningBytes)
        val dummyFile = File(".")

        assertEquals(EmergencyStorageGuard.StorageStatus.LOW_WARNING, guard.evaluateStorage(dummyFile))
        assertTrue(guard.canStartRecording(dummyFile))
    }

    @Test
    fun testNormalStorageThreshold() {
        // Above 200MB
        val normalBytes = 500L * 1024L * 1024L
        val guard = MockStorageGuard(normalBytes)
        val dummyFile = File(".")

        assertEquals(EmergencyStorageGuard.StorageStatus.OK, guard.evaluateStorage(dummyFile))
        assertTrue(guard.canStartRecording(dummyFile))
    }
}
