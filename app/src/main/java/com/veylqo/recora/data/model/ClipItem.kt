package com.veylqo.recora.data.model

data class ClipItem(
    val id: String,
    val title: String,
    val durationText: String,
    val resolutionBadge: String,
    val fpsBadge: String,
    val sizeText: String,
    val dateText: String,
    val uriString: String? = null
)

val sampleClips = listOf(
    ClipItem(
        id = "1",
        title = "Recora_20261101_015914.mp4",
        durationText = "00:24",
        resolutionBadge = "4K UHD",
        fpsBadge = "60fps",
        sizeText = "13.2 MB",
        dateText = "Nov 1, 2026"
    ),
    ClipItem(
        id = "2",
        title = "Recora_20261101_021530.mp4",
        durationText = "01:45",
        resolutionBadge = "1080p FHD",
        fpsBadge = "60fps",
        sizeText = "48.6 MB",
        dateText = "Nov 1, 2026"
    ),
    ClipItem(
        id = "3",
        title = "Recora_20261031_184210.mp4",
        durationText = "05:12",
        resolutionBadge = "1080p FHD",
        fpsBadge = "30fps",
        sizeText = "112.0 MB",
        dateText = "Oct 31, 2026"
    )
)
