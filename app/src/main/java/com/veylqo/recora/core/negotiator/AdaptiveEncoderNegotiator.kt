package com.veylqo.recora.core.negotiator

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import com.veylqo.recora.core.model.FpsOption
import com.veylqo.recora.core.model.OrientationOption
import com.veylqo.recora.core.model.ResolutionPreset

data class NegotiatedVideoConfig(
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrateBps: Int,
    val mimeType: String = "video/avc"
)

object AdaptiveEncoderNegotiator {
    private const val AVC_MIME = "video/avc"
    private const val HEVC_MIME = "video/hevc"

    fun clampToMultipleOf16(value: Int): Int {
        val clamped = (value / 16) * 16
        return if (clamped < 16) 16 else clamped
    }

    fun negotiateConfig(
        displayWidth: Int,
        displayHeight: Int,
        preset: ResolutionPreset,
        fpsOption: FpsOption,
        userBitrateBps: Int,
        orientation: OrientationOption
    ): NegotiatedVideoConfig {
        var targetW: Int
        var targetH: Int

        if (preset == ResolutionPreset.NATIVE || preset.width <= 0 || preset.height <= 0) {
            targetW = displayWidth
            targetH = displayHeight
        } else {
            targetW = preset.width
            targetH = preset.height
        }

        // Apply orientation adjustment
        when (orientation) {
            OrientationOption.PORTRAIT -> {
                if (targetW > targetH) {
                    val tmp = targetW
                    targetW = targetH
                    targetH = tmp
                }
            }
            OrientationOption.LANDSCAPE -> {
                if (targetH > targetW) {
                    val tmp = targetW
                    targetW = targetH
                    targetH = tmp
                }
            }
            OrientationOption.AUTO -> {
                // Follow display aspect ratio
                val isDisplayLandscape = displayWidth > displayHeight
                val isTargetLandscape = targetW > targetH
                if (isDisplayLandscape != isTargetLandscape) {
                    val tmp = targetW
                    targetW = targetH
                    targetH = tmp
                }
            }
        }

        // Clamp to multiple of 16
        val clampedW = clampToMultipleOf16(targetW)
        val clampedH = clampToMultipleOf16(targetH)

        val fps = fpsOption.value
        val bitrate = if (userBitrateBps > 0) userBitrateBps else preset.defaultBitrateBps

        // Query MediaCodecList capabilities to ensure hardware encoder limits aren't exceeded
        val codecCapabilities = findEncoderCapabilities(AVC_MIME)
        if (codecCapabilities != null) {
            val videoCaps = codecCapabilities.videoCapabilities
            if (videoCaps != null) {
                val supportedWidths = videoCaps.supportedWidths
                val supportedHeights = videoCaps.supportedHeights

                var adjustedW = clampedW
                var adjustedH = clampedH

                if (!supportedWidths.contains(adjustedW)) {
                    adjustedW = clampToMultipleOf16(clampedW.coerceIn(supportedWidths.lower, supportedWidths.upper))
                }
                if (!supportedHeights.contains(adjustedH)) {
                    adjustedH = clampToMultipleOf16(clampedH.coerceIn(supportedHeights.lower, supportedHeights.upper))
                }

                val maxBitrate = videoCaps.bitrateRange.upper
                val safeBitrate = bitrate.coerceAtMost(maxBitrate)

                return NegotiatedVideoConfig(
                    width = adjustedW,
                    height = adjustedH,
                    fps = fps,
                    bitrateBps = safeBitrate,
                    mimeType = AVC_MIME
                )
            }
        }

        return NegotiatedVideoConfig(
            width = clampedW,
            height = clampedH,
            fps = fps,
            bitrateBps = bitrate,
            mimeType = AVC_MIME
        )
    }

    private fun findEncoderCapabilities(mime: String): MediaCodecInfo.CodecCapabilities? {
        return try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val info = codecList.codecInfos.firstOrNull { it.isEncoder && it.supportedTypes.any { type -> type.equals(mime, ignoreCase = true) } }
            info?.getCapabilitiesForType(mime)
        } catch (_: Exception) {
            null
        }
    }
}
