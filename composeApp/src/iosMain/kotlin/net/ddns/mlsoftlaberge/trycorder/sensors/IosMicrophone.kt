package net.ddns.mlsoftlaberge.trycorder.sensors

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionUndetermined
import platform.AVFAudio.setActive
import kotlin.coroutines.resume

/**
 * AVAudioEngine input tap, decimated to 8 kHz 16 bit PCM chunks of 1024 samples like the
 * AudioRecord of AudSensorView. Emits nothing while the microphone permission is denied.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosMicrophone {
    fun flow(): Flow<ShortArray> = callbackFlow {
        val session = AVAudioSession.sharedInstance()
        val engine = AVAudioEngine()
        val started = askPermission(session) && start(session, engine) { trySend(it) }
        awaitClose {
            if (started) {
                engine.inputNode.removeTapOnBus(BUS)
                engine.stop()
            }
        }
    }

    private fun start(session: AVAudioSession, engine: AVAudioEngine, onChunk: (ShortArray) -> Unit): Boolean {
        session.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            AVAudioSessionCategoryOptionDefaultToSpeaker or AVAudioSessionCategoryOptionMixWithOthers,
            null,
        )
        session.setActive(true, null)
        val input = engine.inputNode
        val format = input.outputFormatForBus(BUS)
        val decimator = PcmDecimator(format.sampleRate, onChunk = onChunk)
        input.installTapOnBus(BUS, TAP_FRAMES, format) { buffer, _ ->
            val channel = buffer?.floatChannelData?.get(0)
            if (channel != null) {
                val count = buffer.frameLength.toInt()
                decimator.accept(FloatArray(count) { channel[it] })
            }
        }
        engine.prepare()
        val running = engine.startAndReturnError(null)
        if (!running) input.removeTapOnBus(BUS)
        return running
    }

    private suspend fun askPermission(session: AVAudioSession): Boolean = when (session.recordPermission) {
        AVAudioSessionRecordPermissionGranted -> true
        AVAudioSessionRecordPermissionUndetermined -> suspendCancellableCoroutine { cont ->
            session.requestRecordPermission { granted -> if (cont.isActive) cont.resume(granted) }
        }
        else -> false
    }

    private companion object {
        const val BUS = 0uL
        const val TAP_FRAMES = 4096u
    }
}
