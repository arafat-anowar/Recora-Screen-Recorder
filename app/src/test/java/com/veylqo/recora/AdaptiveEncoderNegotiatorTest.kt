package com.veylqo.recora

import com.veylqo.recora.core.model.FpsOption
import com.veylqo.recora.core.model.OrientationOption
import com.veylqo.recora.core.model.ResolutionPreset
import com.veylqo.recora.core.negotiator.AdaptiveEncoderNegotiator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveEncoderNegotiatorTest {

    @Test
    fun testClampingToMultipleOf16() {
        assertEquals(16, AdaptiveEncoderNegotiator.clampToMultipleOf16(16))
        assertEquals(1072, AdaptiveEncoderNegotiator.clampToMultipleOf16(1080))
        assertEquals(1920, AdaptiveEncoderNegotiator.clampToMultipleOf16(1920))
        assertEquals(1280, AdaptiveEncoderNegotiator.clampToMultipleOf16(1280))
        assertEquals(720, AdaptiveEncoderNegotiator.clampToMultipleOf16(720))
        assertEquals(848, AdaptiveEncoderNegotiator.clampToMultipleOf16(854))
        assertEquals(16, AdaptiveEncoderNegotiator.clampToMultipleOf16(5))
    }

    @Test
    fun testPortraitOrientationEnforcement() {
        val config = AdaptiveEncoderNegotiator.negotiateConfig(
            displayWidth = 1920,
            displayHeight = 1080,
            preset = ResolutionPreset.RES_1080P,
            fpsOption = FpsOption.FPS_60,
            userBitrateBps = 0,
            orientation = OrientationOption.PORTRAIT
        )
        // In portrait, width must be <= height
        assertTrue("Width (${config.width}) must be <= height (${config.height}) for Portrait", config.width <= config.height)
        assertEquals(0, config.width % 16)
        assertEquals(0, config.height % 16)
        assertEquals(60, config.fps)
    }

    @Test
    fun testLandscapeOrientationEnforcement() {
        val config = AdaptiveEncoderNegotiator.negotiateConfig(
            displayWidth = 1080,
            displayHeight = 1920,
            preset = ResolutionPreset.RES_1080P,
            fpsOption = FpsOption.FPS_30,
            userBitrateBps = 12_000_000,
            orientation = OrientationOption.LANDSCAPE
        )
        // In landscape, width must be >= height
        assertTrue("Width (${config.width}) must be >= height (${config.height}) for Landscape", config.width >= config.height)
        assertEquals(0, config.width % 16)
        assertEquals(0, config.height % 16)
        assertEquals(30, config.fps)
        assertEquals(12_000_000, config.bitrateBps)
    }
}
