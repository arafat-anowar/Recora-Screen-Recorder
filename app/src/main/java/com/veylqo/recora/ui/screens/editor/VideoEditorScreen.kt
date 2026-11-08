package com.veylqo.recora.ui.screens.editor

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.veylqo.recora.data.model.ClipItem
import com.veylqo.recora.editor.EditorAspectRatio
import com.veylqo.recora.editor.PlaybackSpeed
import com.veylqo.recora.editor.VideoEditorManager
import com.veylqo.recora.ui.components.GalaxyBackground
import com.veylqo.recora.ui.dialogs.ExportProgressDialog
import com.veylqo.recora.ui.theme.CardBg
import com.veylqo.recora.ui.theme.CardBorder
import com.veylqo.recora.ui.theme.IconTileBg
import com.veylqo.recora.ui.theme.PillCornerRadius
import com.veylqo.recora.ui.theme.PrimaryPurple
import com.veylqo.recora.ui.theme.RecordPink
import com.veylqo.recora.ui.theme.TextMuted
import com.veylqo.recora.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import java.util.Locale

private enum class EditorTab(val title: String, val icon: ImageVector) {
    Trim("Trim", Icons.Rounded.ContentCut),
    Speed("Speed", Icons.Rounded.FastForward),
    Audio("Audio", Icons.Rounded.VolumeUp),
    Crop("Crop", Icons.Rounded.AspectRatio)
}

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun VideoEditorScreen(
    clip: ClipItem,
    editorManager: VideoEditorManager,
    onBackClick: () -> Unit,
    onExportSuccess: (outputUri: Uri, newTitle: String) -> Unit,
    onError: (message: String) -> Unit
) {
    val context = LocalContext.current
    val editorState by editorManager.editorState.collectAsState()
    var selectedTab by remember { mutableStateOf(EditorTab.Trim) }

    // ExoPlayer for Live Video Preview
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            clip.uriString?.let { uriStr ->
                setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)))
                prepare()
            }
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var videoDurationMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val duration = exoPlayer.duration
                    if (duration > 0) {
                        videoDurationMs = duration
                        editorManager.updateDuration(duration)
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Playback loop confined to trim bounds
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val current = exoPlayer.currentPosition
            currentPositionMs = current

            val trimEnd = if (editorState.trimEndMs > 0) editorState.trimEndMs else videoDurationMs
            if (trimEnd > 0 && current >= trimEnd) {
                exoPlayer.seekTo(editorState.trimStartMs)
            }
            delay(100)
        }
    }

    // Sync playback speed and volume
    LaunchedEffect(editorState.speed) {
        exoPlayer.setPlaybackSpeed(editorState.speed.speed)
    }
    LaunchedEffect(editorState.isAudioEnabled) {
        exoPlayer.volume = if (editorState.isAudioEnabled) 1f else 0f
    }

    // Export Progress Dialog
    if (editorState.isExporting) {
        ExportProgressDialog(
            progressPercent = editorState.exportProgressPercent,
            onCancel = { editorManager.cancelExport() }
        )
    }

    GalaxyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 40.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = clip.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )

                // Export Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(PillCornerRadius))
                        .background(PrimaryPurple)
                        .clickable {
                            val uriStr = clip.uriString
                            if (uriStr != null) {
                                exoPlayer.pause()
                                editorManager.exportVideo(
                                    sourceUri = Uri.parse(uriStr),
                                    onSuccess = onExportSuccess,
                                    onError = onError
                                )
                            } else {
                                onError("Cannot find source video file")
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Export",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // VIDEO PREVIEW PLAYER
            val previewRatio = when (editorState.aspectRatio) {
                EditorAspectRatio.Square1_1 -> 1.0f
                EditorAspectRatio.Portrait9_16 -> 9f / 16f
                EditorAspectRatio.Landscape16_9 -> 16f / 9f
                EditorAspectRatio.Original -> 16f / 9f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(previewRatio.coerceIn(0.6f, 1.8f))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Center Play/Pause button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.85f))
                        .clickable {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                if (exoPlayer.currentPosition >= (if (editorState.trimEndMs > 0) editorState.trimEndMs else videoDurationMs)) {
                                    exoPlayer.seekTo(editorState.trimStartMs)
                                }
                                exoPlayer.play()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PLAYHEAD SCRUBBER SLIDER
            if (videoDurationMs > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMs(currentPositionMs),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = formatMs(videoDurationMs),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Slider(
                    value = currentPositionMs.toFloat().coerceIn(0f, videoDurationMs.toFloat()),
                    onValueChange = { newPos ->
                        currentPositionMs = newPos.toLong()
                        exoPlayer.seekTo(newPos.toLong())
                    },
                    valueRange = 0f..videoDurationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryPurple,
                        activeTrackColor = PrimaryPurple,
                        inactiveTrackColor = PrimaryPurple.copy(alpha = 0.2f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // EDIT TOOL SELECTOR TABS (Trim, Speed, Audio, Crop)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EditorTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PrimaryPurple else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) Color.White else TextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = tab.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TAB CONTENT PANELS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                when (selectedTab) {
                    EditorTab.Trim -> {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Trim Video",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                val trimmedDuration = ((if (editorState.trimEndMs > 0) editorState.trimEndMs else videoDurationMs) - editorState.trimStartMs).coerceAtLeast(0L)
                                Text(
                                    text = "Duration: ${formatMs(trimmedDuration)}",
                                    fontSize = 12.sp,
                                    color = RecordPink,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (videoDurationMs > 0) {
                                val currentEnd = if (editorState.trimEndMs > 0) editorState.trimEndMs.toFloat() else videoDurationMs.toFloat()
                                var rangeValues by remember(editorState.trimStartMs, editorState.trimEndMs, videoDurationMs) {
                                    mutableStateOf(editorState.trimStartMs.toFloat()..currentEnd)
                                }

                                RangeSlider(
                                    value = rangeValues,
                                    onValueChange = { range ->
                                        rangeValues = range
                                        editorManager.setTrimRange(range.start.toLong(), range.endInclusive.toLong())
                                        exoPlayer.seekTo(range.start.toLong())
                                    },
                                    valueRange = 0f..videoDurationMs.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = RecordPink,
                                        activeTrackColor = RecordPink,
                                        inactiveTrackColor = RecordPink.copy(alpha = 0.25f)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Start: ${formatMs(editorState.trimStartMs)}",
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "End: ${formatMs(if (editorState.trimEndMs > 0) editorState.trimEndMs else videoDurationMs)}",
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(
                                    text = "Loading video timeline...",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    EditorTab.Speed -> {
                        Column {
                            Text(
                                text = "Playback Speed",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlaybackSpeed.entries.forEach { speed ->
                                    val isSelected = editorState.speed == speed
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) PrimaryPurple else IconTileBg)
                                            .border(1.dp, if (isSelected) PrimaryPurple else CardBorder, RoundedCornerShape(12.dp))
                                            .clickable { editorManager.setSpeed(speed) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = speed.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    EditorTab.Audio -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Video Audio Track",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (editorState.isAudioEnabled) "Audio included in export" else "Audio muted in exported video",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (editorState.isAudioEnabled) PrimaryPurple else IconTileBg)
                                    .clickable { editorManager.toggleAudio() }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (editorState.isAudioEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                                        contentDescription = "Audio toggle",
                                        tint = if (editorState.isAudioEnabled) Color.White else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (editorState.isAudioEnabled) "Sound ON" else "Muted",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (editorState.isAudioEnabled) Color.White else TextMuted
                                    )
                                }
                            }
                        }
                    }

                    EditorTab.Crop -> {
                        Column {
                            Text(
                                text = "Aspect Ratio Crop",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EditorAspectRatio.entries.forEach { ratio ->
                                    val isSelected = editorState.aspectRatio == ratio
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) PrimaryPurple else IconTileBg)
                                            .border(1.dp, if (isSelected) PrimaryPurple else CardBorder, RoundedCornerShape(12.dp))
                                            .clickable { editorManager.setAspectRatio(ratio) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = ratio.ratioLabel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else TextPrimary
                                            )
                                            Text(
                                                text = ratio.displayName,
                                                fontSize = 10.sp,
                                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
