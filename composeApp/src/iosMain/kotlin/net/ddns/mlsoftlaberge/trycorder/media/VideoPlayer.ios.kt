package net.ddns.mlsoftlaberge.trycorder.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.CoreGraphics.CGRectZero
import platform.CoreMedia.kCMTimeZero
import platform.Foundation.NSURL
import platform.QuartzCore.CATransaction
import platform.UIKit.UIColor
import platform.UIKit.UIView

/** An AVPlayerLayer that follows the size of its view. */
@OptIn(ExperimentalForeignApi::class)
private class PlayerView(val player: AVPlayer) : UIView(frame = CGRectZero.readValue()) {
    private val playerLayer = AVPlayerLayer.playerLayerWithPlayer(player).apply {
        videoGravity = AVLayerVideoGravityResizeAspect
    }

    init {
        backgroundColor = UIColor.blackColor
        layer.addSublayer(playerLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        playerLayer.frame = bounds
        CATransaction.commit()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    uri: String,
    playing: Boolean,
    startId: Int,
    onStarted: () -> Unit,
    modifier: Modifier,
) {
    val started by rememberUpdatedState(onStarted)
    val player = remember { AVPlayer() }
    LaunchedEffect(startId) {
        val url = NSURL.URLWithString(uri) ?: return@LaunchedEffect
        player.replaceCurrentItemWithPlayerItem(AVPlayerItem(uRL = url))
        player.seekToTime(kCMTimeZero.readValue())
        player.play()
        started()
    }
    LaunchedEffect(playing) {
        if (playing) player.play() else player.pause()
    }
    DisposableEffect(player) {
        onDispose {
            player.pause()
            player.replaceCurrentItemWithPlayerItem(null)
        }
    }
    UIKitView(factory = { PlayerView(player) }, modifier = modifier)
}
