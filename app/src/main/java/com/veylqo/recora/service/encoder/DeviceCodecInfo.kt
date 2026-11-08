package com.veylqo.recora.service.encoder

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Log

data class RecordingProfile(
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrate: Int,
    val mimeType: String,
    val isFallback: Boolean = false,
    val fallbackReason: String? = null
)

object DeviceCodecInfo {
    private const val TAG = "DeviceCodecInfo"

    fun getSafeRecordingProfile(
        requestedWidth: Int,
        requestedHeight: Int,
        requestedFps: Int,
        preferHevc: Boolean = false
    ): RecordingProfile {
        val preferredMime = if (preferHevc) MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC
        val codec = findEncoderForMime(preferredMime) ?: findEncoderForMime(MediaFormat.MIMETYPE_VIDEO_AVC)

        if (codec == null) {
            Log.w(TAG, "No hardware video encoder found for H.264/HEVC, using safe 1080p fallback")
            return RecordingProfile(
                width = 1080,
                height = 1920,
                fps = 30,
                bitrate = 8_000_000,
                mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
                isFallback = true,
                fallbackReason = "No compatible hardware encoder found"
            )
        }

        val capabilities = codec.getCapabilitiesForType(codec.supportedTypes.firstOrNull { it.contains("video", ignoreCase = true) } ?: MediaFormat.MIMETYPE_VIDEO_AVC)
        val videoCaps = capabilities.videoCapabilities

        // Check if requested resolution is supported
        val isSizeSupported = videoCaps?.isSizeSupported(requestedWidth, requestedHeight) == true
        val isRateSupported = if (isSizeSupported && videoCaps != null) {
            val range = videoCaps.getSupportedFrameRatesFor(requestedWidth, requestedHeight)
            range != null && range.contains(requestedFps.toDouble())
        } else false

        return if (isSizeSupported && isRateSupported) {
            val bitrate = calculateTargetBitrate(requestedWidth, requestedHeight, requestedFps)
            RecordingProfile(
                width = requestedWidth,
                height = requestedHeight,
                fps = requestedFps,
                bitrate = bitrate,
                mimeType = capabilities.mimeType ?: MediaFormat.MIMETYPE_VIDEO_AVC
            )
        } else {
            // Automatic fallback to 1080p @ 60 or 30
            val fallbackW = 1080
            val fallbackH = 1920
            val fallbackFps = if (videoCaps?.getSupportedFrameRatesFor(fallbackW, fallbackH)?.contains(60.0) == true) 60 else 30
            val bitrate = calculateTargetBitrate(fallbackW, fallbackH, fallbackFps)

            Log.i(TAG, "Device does not support ${requestedWidth}x$requestedHeight @ $requestedFps fps. Falling back to ${fallbackW}x$fallbackH @ $fallbackFps fps")
            RecordingProfile(
                width = fallbackW,
                height = fallbackH,
                fps = fallbackFps,
                bitrate = bitrate,
                mimeType = capabilities.mimeType ?: MediaFormat.MIMETYPE_VIDEO_AVC,
                isFallback = true,
                fallbackReason = "Hardware encoder limit: max ${videoCaps?.supportedWidths?.upper}x${videoCaps?.supportedHeights?.upper}"
            )
        }
    }

    fun getDeviceCodecSummary(): String {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val sb = StringBuilder()
        sb.appendLine("Supported Hardware Encoders:")

        var count = 0
        for (info in codecList.codecInfos) {
            if (info.isEncoder) {
                for (type in info.supportedTypes) {
                    if (type == MediaFormat.MIMETYPE_VIDEO_AVC || type == MediaFormat.MIMETYPE_VIDEO_HEVC) {
                        count++
                        val caps = info.getCapabilitiesForType(type)
                        val vCaps = caps.videoCapabilities
                        val maxWidth = vCaps?.supportedWidths?.upper ?: 0
                        val maxHeight = vCaps?.supportedHeights?.upper ?: 0
                        val maxBitrate = vCaps?.bitrateRange?.upper ?: 0
                        sb.appendLine("• ${info.name}")
                        sb.appendLine("  Format: $type")
                        sb.appendLine("  Max Bounds: ${maxWidth}×${maxHeight}")
                        sb.appendLine("  Max Bitrate: ${maxBitrate / 1_000_000} Mbps")
                    }
                }
            }
        }

        if (count == 0) {
            sb.appendLine("• Default Standard Android AVC/H.264 Encoder")
        }

        sb.appendLine()
        sb.appendLine("Audio Capabilities:")
        sb.appendLine("• Internal Audio (Android 10+ AudioPlaybackCapture)")
        sb.appendLine("• AAC Stereo @ 48kHz, 192kbps")
        return sb.toString().trimEnd()
    }

    private fun findEncoderForMime(mime: String): MediaCodecInfo? {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in codecList.codecInfos) {
            if (info.isEncoder) {
                for (type in info.supportedTypes) {
                    if (type.equals(mime, ignoreCase = true)) {
                        return info
                    }
                }
            }
        }
        return null
    }

    private fun calculateTargetBitrate(w: Int, h: Int, fps: Int): Int {
        val pixels = w.toLong() * h.toLong()
        return when {
            pixels >= 3840 * 2160 -> if (fps >= 60) 40_000_000 else 25_000_000
            pixels >= 2560 * 1440 -> if (fps >= 60) 24_000_000 else 16_000_000
            pixels >= 1920 * 1080 -> if (fps >= 60) 14_000_000 else 10_000_000
            else -> 6_000_000
        }
    }
}
