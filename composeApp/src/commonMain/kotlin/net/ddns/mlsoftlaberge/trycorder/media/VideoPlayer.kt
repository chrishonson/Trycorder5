package net.ddns.mlsoftlaberge.trycorder.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Plays the video at [uri] (the VideoView of the videos viewer).
 * Each new [startId] loads the uri and plays it from the start, calling [onStarted] once playback
 * begins (OnPreparedListener). [playing] false pauses, true resumes.
 */
@Composable
expect fun VideoPlayer(
    uri: String,
    playing: Boolean,
    startId: Int,
    onStarted: () -> Unit,
    modifier: Modifier = Modifier,
)
