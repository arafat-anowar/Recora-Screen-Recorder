package com.veylqo.recora.editor

enum class EditorAspectRatio(val displayName: String, val ratioLabel: String) {
    Original("Original", "Full"),
    Portrait9_16("Reels / TikTok", "9:16"),
    Landscape16_9("YouTube", "16:9"),
    Square1_1("Square", "1:1")
}

enum class PlaybackSpeed(val speed: Float, val label: String) {
    Slow0_5(0.5f, "0.5x"),
    Slow0_75(0.75f, "0.75x"),
    Normal(1.0f, "1.0x"),
    Fast1_25(1.25f, "1.25x"),
    Fast1_5(1.5f, "1.5x"),
    Fast2_0(2.0f, "2.0x")
}

data class EditorState(
    val durationMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val speed: PlaybackSpeed = PlaybackSpeed.Normal,
    val isAudioEnabled: Boolean = true,
    val aspectRatio: EditorAspectRatio = EditorAspectRatio.Original,
    val isExporting: Boolean = false,
    val exportProgressPercent: Int = 0,
    val exportError: String? = null
)
