package net.ddns.mlsoftlaberge.trycorder.core

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.IOException

/** MediaPlayer on the compose resources packaged in the APK assets. */
class AndroidSoundPlayer(private val context: Context) : SoundPlayer {
    private var effect: MediaPlayer? = null
    private var loop: MediaPlayer? = null

    override fun play(sound: TrekSound) {
        effect?.release()
        effect = create(sound)?.apply { start() }
    }

    override fun startLoop(sound: TrekSound) {
        loop?.release()
        loop = create(sound)?.apply {
            isLooping = true
            start()
        }
    }

    override fun stopLoop() {
        loop?.release()
        loop = null
    }

    override fun release() {
        stopLoop()
        effect?.release()
        effect = null
    }

    private fun create(sound: TrekSound): MediaPlayer? = try {
        context.assets.openFd(assetPath(sound)).use { fd ->
            MediaPlayer().apply {
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                prepare()
            }
        }
    } catch (e: IOException) {
        Log.w(TAG, "cannot play ${sound.fileName}", e)
        null
    }

    private companion object {
        const val TAG = "TrycorderSound"

        /** Where the compose resources plugin puts files/ inside the APK assets. */
        fun assetPath(sound: TrekSound) =
            "composeResources/net.ddns.mlsoftlaberge.trycorder.resources/${sound.resourcePath}"
    }
}
