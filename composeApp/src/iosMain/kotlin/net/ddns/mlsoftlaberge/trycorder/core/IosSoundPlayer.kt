package net.ddns.mlsoftlaberge.trycorder.core

import kotlinx.cinterop.ExperimentalForeignApi
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSURL

/** AVAudioPlayer on the sound files bundled by compose resources. */
@OptIn(ExperimentalForeignApi::class, ExperimentalResourceApi::class)
class IosSoundPlayer : SoundPlayer {
    private var effect: AVAudioPlayer? = null
    private var loop: AVAudioPlayer? = null

    init {
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)
        AVAudioSession.sharedInstance().setActive(true, null)
    }

    override fun play(sound: TrekSound) {
        effect?.stop()
        effect = create(sound)?.apply { play() }
    }

    override fun startLoop(sound: TrekSound) {
        loop?.stop()
        loop = create(sound)?.apply {
            numberOfLoops = -1
            play()
        }
    }

    override fun stopLoop() {
        loop?.stop()
        loop = null
    }

    override fun release() {
        stopLoop()
        effect?.stop()
        effect = null
    }

    private fun create(sound: TrekSound): AVAudioPlayer? {
        val url = NSURL.URLWithString(Res.getUri(sound.resourcePath)) ?: return null
        return AVAudioPlayer(contentsOfURL = url, error = null)
    }
}
