package net.ddns.mlsoftlaberge.trycorder.media

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/** The VideoView with its MediaController, like startvideo() set it up. */
@Composable
actual fun VideoPlayer(
    uri: String,
    playing: Boolean,
    startId: Int,
    onStarted: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val started by rememberUpdatedState(onStarted)
    val videoView = remember(context) {
        VideoView(context).also { view ->
            val controller = MediaController(context)
            controller.setAnchorView(view)
            view.setMediaController(controller)
        }
    }
    LaunchedEffect(startId) {
        videoView.setOnPreparedListener { player ->
            player.isLooping = false
            videoView.start()
            started()
        }
        videoView.setVideoURI(Uri.parse(uri))
        videoView.requestFocus()
    }
    LaunchedEffect(playing) {
        if (!playing && videoView.isPlaying) videoView.pause()
        if (playing && !videoView.isPlaying) videoView.start()
    }
    DisposableEffect(videoView) {
        onDispose { videoView.stopPlayback() }
    }
    AndroidView(factory = { videoView }, modifier = modifier)
}
