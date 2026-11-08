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

        // Align dimensions to 8 for hardware encoder compatibility
        val alignedW = (requestedWidth / 8) * 8
        val alignedH = (requestedHeight / 8) * 8

        if (codec == null) {
            Log.w(TAG, "No hardware video encoder found for H.264/HEVC, using aligned bounds")
            return RecordingProfile(
                width = alignedW,
                height = alignedH,
                fps = requestedFps,
                bitrate = calculateTargetBitrate(alignedW, alignedH, requestedFps),
                mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
            )
        }

        val capabilities = codec.getCapabilitiesForType(
            codec.supportedTypes.firstOrNull { it.contains("video", ignoreCase = true) } ?: MediaFormat.MIMETYPE_VIDEO_AVC
        )
        val videoCaps = capabilities.videoCapabilities

        // Check hardware size capabilities: handles portrait & landscape aspect ratios gracefully
        val minDim = minOf(alignedW, alignedH)
        val maxDim = maxOf(alignedW, alignedH)

        val maxWidth = videoCaps?.supportedWidths?.upper ?: 3840
        val maxHeight = videoCaps?.supportedHeights?.upper ?: 2560

        // In Android, hardware encoders support (minDim x maxDim) or (maxDim x minDim) as macroblocks
        val isHardwareSizeOk = videoCaps != null && (
            videoCaps.isSizeSupported(alignedW, alignedH) ||
            videoCaps.isSizeSupported(alignedH, alignedW) ||
            (minDim <= maxWidth && maxDim <= maxHeight)
        )

        val targetFps = if (videoCaps != null && videoCaps.supportedFrameRates.contains(requestedFps)) {
            requestedFps
        } else {
            30
        }

        return if (isHardwareSizeOk) {
            val bitrate = calculateTargetBitrate(alignedW, alignedH, targetFps)
            Log.i(TAG, "Hardware encoder validated for Native $alignedW x $alignedH @ ${targetFps}fps ($bitrate bps)")
            RecordingProfile(
                width = alignedW,
                height = alignedH,
                fps = targetFps,
                bitrate = bitrate,
                mimeType = capabilities.mimeType ?: MediaFormat.MIMETYPE_VIDEO_AVC
            )
        } else {
            // Safe fallback preserving aspect ratio rather than forcing 16:9
            val fallbackW = 1080
            val aspectRatio = requestedHeight.toFloat() / requestedWidth.toFloat()
            val fallbackH = ((fallbackW * aspectRatio).toInt() / 8) * 8
            val bitrate = calculateTargetBitrate(fallbackW, fallbackH, 30)

            Log.i(TAG, "Falling back to safe bounds $fallbackW x $fallbackH @ 30fps")
            RecordingProfile(
                width = fallbackW,
                height = fallbackH,
                fps = 30,
                bitrate = bitrate,
                mimeType = capabilities.mimeType ?: MediaFormat.MIMETYPE_VIDEO_AVC,
                isFallback = true,
                fallbackReason = "Hardware encoder limit"
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
        sb.appendLine("• Pure Internal Audio (Android 10+ AudioPlaybackCapture)")
        sb.appendLine("• AAC Stereo @ 44.1kHz, 192kbps")
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
            pixels >= 3840 * 2160 -> if (fps >= 60) 28_000_000 else 20_000_000
            pixels >= 2560 * 1440 -> if (fps >= 60) 20_000_000 else 14_000_000
            pixels >= 1920 * 1080 -> if (fps >= 60) 14_000_000 else 10_000_000
            else -> 6_000_000
        }
    }
}
