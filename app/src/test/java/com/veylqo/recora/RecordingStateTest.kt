package com.veylqo.recora

import android.net.Uri
import com.veylqo.recora.core.model.RecordingState
import com.veylqo.recora.core.model.UserEntitlement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingStateTest {

    @Test
    fun testCompletedOneShotEventConsumption() {
        val completed = RecordingState.Completed(
            sessionId = "20261101_120000",
            uri = null,
            filePath = "/sdcard/test.mp4",
            durationMs = 5000L,
            sizeBytes = 1024L
        )

        assertFalse(completed.consumed)
        // First consume should return true
        assertTrue("First call to consume() must return true", completed.consume())
        assertTrue(completed.consumed)
        // Subsequent calls must return false
        assertFalse("Second call to consume() must return false", completed.consume())
        assertFalse("Third call to consume() must return false", completed.consume())
    }

    @Test
    fun testEntitlementReconciliationPriority() {
        val reconciled = UserEntitlement.reconcile(
            UserEntitlement.FREE,
            UserEntitlement.CODE_PASS,
            UserEntitlement.MONTHLY,
            UserEntitlement.LIFETIME
        )
        assertEquals(UserEntitlement.LIFETIME, reconciled)

        val passVsMonthly = UserEntitlement.reconcile(
            UserEntitlement.CODE_PASS,
            UserEntitlement.MONTHLY
        )
        assertEquals(UserEntitlement.MONTHLY, passVsMonthly)

        val freeVsPass = UserEntitlement.reconcile(
            UserEntitlement.FREE,
            UserEntitlement.CODE_PASS
        )
        assertEquals(UserEntitlement.CODE_PASS, freeVsPass)
    }

    @Test
    fun testPremiumEntitlements() {
        assertFalse(UserEntitlement.FREE.isPremium)
        assertTrue(UserEntitlement.CODE_PASS.isPremium)
        assertTrue(UserEntitlement.MONTHLY.isPremium)
        assertTrue(UserEntitlement.LIFETIME.isPremium)
    }
}
