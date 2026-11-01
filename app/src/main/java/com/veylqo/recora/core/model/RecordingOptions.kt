package com.veylqo.recora.core.model

enum class AudioSource(val displayName: String, val description: String) {
    INTERNAL("Internal", "Record system internal audio"),
    MIC("Microphone", "Record external voice via microphone"),
    INTERNAL_AND_MIC("Internal + Mic", "Simultaneously record system and mic"),
    MUTE("Mute", "Record video only without audio")
}

enum class OrientationOption(val displayName: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    AUTO("Auto")
}

enum class FpsOption(val value: Int, val displayName: String) {
    FPS_24(24, "24 fps"),
    FPS_30(30, "30 fps"),
    FPS_60(60, "60 fps")
}

enum class BitrateOption(val bps: Int, val displayName: String) {
    MBPS_4(4_000_000, "4 Mbps"),
    MBPS_8(8_000_000, "8 Mbps"),
    MBPS_12(12_000_000, "12 Mbps"),
    MBPS_16(16_000_000, "16 Mbps"),
    MBPS_24(24_000_000, "24 Mbps"),
    MBPS_40(40_000_000, "40 Mbps")
}

enum class ResolutionPreset(
    val displayName: String,
    val width: Int,
    val height: Int,
    val defaultBitrateBps: Int
) {
    NATIVE("Native", 0, 0, 16_000_000),
    RES_4K("4K UHD", 3840, 2160, 24_000_000),
    RES_2K("2K QHD", 2560, 1440, 16_000_000),
    RES_1080P("1080p FHD", 1920, 1080, 8_000_000),
    RES_720P("720p HD", 1280, 720, 4_000_000),
    RES_480P("480p SD", 854, 480, 2_000_000)
}

enum class TimerPreset(val seconds: Int, val displayName: String) {
    OFF(0, "Immediate (Off)"),
    SEC_3(3, "3s"),
    SEC_5(5, "5s"),
    SEC_10(10, "10s"),
    CUSTOM(-1, "Custom")
}
