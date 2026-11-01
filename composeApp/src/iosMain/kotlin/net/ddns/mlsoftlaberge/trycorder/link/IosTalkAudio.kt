package net.ddns.mlsoftlaberge.trycorder.link

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.setActive

/**
 * Push-to-talk on iOS, compatible with the Android app: the microphone (AVAudioEngine input tap) is
 * resampled to 44100 Hz mono 16 bit and sent over UDP 1701 to the other local Trycorders; datagrams
 * received on UDP 1701 are played through an AVAudioPlayerNode.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosTalkAudio(private val engine: LinkEngine) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val selector = SelectorManager(Dispatchers.IO)
    private var recorder: AVAudioEngine? = null
    private var player: AVAudioEngine? = null
    private var playerNode: AVAudioPlayerNode? = null
    private var serverJob: Job? = null
    private var sender: BoundDatagramSocket? = null
    private val playFormat = AVAudioFormat(standardFormatWithSampleRate = Pcm.SAMPLE_RATE.toDouble(), channels = 1u)

    fun startStreaming(targets: List<Peer>) {
        if (recorder != null) return
        activateSession()
        val audio = AVAudioEngine()
        val input = audio.inputNode
        val format = input.outputFormatForBus(0u)
        val rate = format.sampleRate
        input.installTapOnBus(0u, bufferSize = TAP_FRAMES, format = format) { buffer, _ ->
            val samples = buffer?.let { monoSamples(it) }
            if (samples != null) sendPcm(Pcm.floatsToPcm16(samples, rate), targets)
        }
        audio.prepare()
        if (audio.startAndReturnError(null)) {
            recorder = audio
        } else {
            input.removeTapOnBus(0u)
            engine.status("Microphone unavailable")
        }
    }

    fun stopStreaming() {
        recorder?.let {
            it.inputNode.removeTapOnBus(0u)
            it.stop()
        }
        recorder = null
    }

    fun startServer() {
        if (serverJob != null) return
        engine.status("Start talk server thread")
        activateSession()
        val audio = AVAudioEngine()
        val node = AVAudioPlayerNode()
        audio.attachNode(node)
        audio.connect(node, to = audio.mainMixerNode, format = playFormat)
        audio.prepare()
        if (!audio.startAndReturnError(null)) return engine.status("Cant start the talk server audio")
        node.play()
        player = audio
        playerNode = node
        serverJob = scope.launch {
            networkCall { receive(node) }.onFailure { engine.status("Talk server stopped: ${it.message}") }
        }
    }

    fun stopServer() {
        engine.status("Stop the talk server")
        serverJob?.cancel()
        serverJob = null
        playerNode?.stop()
        player?.stop()
        playerNode = null
        player = null
    }

    private suspend fun receive(node: AVAudioPlayerNode) {
        val address = InetSocketAddress(ANY_ADDRESS, LinkProtocol.PORT)
        val socket = aSocket(selector).udp().bind(address) { reuseAddress = true }
        try {
            while (true) {
                val datagram = socket.receive()
                play(node, Pcm.pcm16ToFloats(datagram.packet.readByteArray()))
            }
        } finally {
            socket.close()
        }
    }

    private fun play(node: AVAudioPlayerNode, samples: FloatArray) {
        if (samples.isEmpty()) return
        val buffer = AVAudioPCMBuffer(pCMFormat = playFormat, frameCapacity = samples.size.toUInt())
        val channel = buffer.floatChannelData?.get(0) ?: return
        samples.forEachIndexed { i, value -> channel[i] = value }
        buffer.frameLength = samples.size.toUInt()
        node.scheduleBuffer(buffer, completionHandler = null)
    }

    private fun sendPcm(pcm: ByteArray, targets: List<Peer>) {
        if (pcm.isEmpty() || targets.isEmpty()) return
        scope.launch {
            networkCall {
                val socket = sender ?: aSocket(selector).udp().bind().also { sender = it }
                targets.forEach { peer ->
                    val packet = Buffer().apply { write(pcm) }
                    socket.send(Datagram(packet, InetSocketAddress(peer.ip, LinkProtocol.PORT)))
                }
            }
        }
    }

    private fun monoSamples(buffer: AVAudioPCMBuffer): FloatArray? {
        val channel = buffer.floatChannelData?.get(0) ?: return null
        return FloatArray(buffer.frameLength.toInt()) { channel[it] }
    }

    private fun activateSession() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker,
            error = null,
        )
        session.setActive(true, error = null)
    }

    private companion object {
        const val TAP_FRAMES = 4096u
        const val ANY_ADDRESS = "0.0.0.0"
    }
}
