package com.veylqo.recora.data.model

enum class AudioSource(val displayName: String, val subtitle: String) {
    Internal("Internal Audio", "Game & app sounds only"),
    Mic("Microphone", "Voice & ambient room audio"),
    InternalAndMic("Internal + Mic", "Mix device sounds and voice commentary"),
    None("Mute (No Audio)", "Silent screen recording")
}

enum class VideoResolution(val label: String, val pixels: String, val isPro: Boolean) {
    Native("Native Display", "Screen 1:1 match", false),
    FHD("1080p FHD", "1920 × 1080", false),
    QHD("2K QHD", "2560 × 1440", true),
    UHD("4K UHD", "3840 × 2160", true)
}

enum class VideoFps(val label: String) {
    FPS_30("30 fps"),
    FPS_60("60 fps")
}

enum class VideoOrientation(val displayName: String, val subtitle: String) {
    Auto("Auto", "Follows device screen orientation"),
    Portrait("Portrait", "Lock recording to vertical (9:16)"),
    Landscape("Landscape", "Lock recording to horizontal (16:9)")
}

enum class CountdownTimer(val displayName: String, val seconds: Int) {
    Off("Off", 0),
    Seconds3("3s", 3),
    Seconds5("5s", 5),
    Seconds10("10s", 10)
}

data class RecordUiState(
    val audioSource: AudioSource = AudioSource.Internal,
    val resolution: VideoResolution = VideoResolution.Native,
    val fps: VideoFps = VideoFps.FPS_60,
    val orientation: VideoOrientation = VideoOrientation.Auto,
    val timer: CountdownTimer = CountdownTimer.Seconds3,
    val floatingControls: Boolean = true,
    val penAndTools: Boolean = false,
    val facecam: Boolean = false,
    val showTouches: Boolean = false,
    val isProUser: Boolean = false,
    val hapticFeedback: Boolean = true,
    val smoothAnimations: Boolean = true
)
