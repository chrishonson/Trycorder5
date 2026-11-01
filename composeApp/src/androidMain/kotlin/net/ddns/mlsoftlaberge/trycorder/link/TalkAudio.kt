package net.ddns.mlsoftlaberge.trycorder.link

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Push-to-talk of the old service: while the button is held, AudioRecord 44100 Hz mono 16 bit is sent
 * raw over UDP 1701 to the other local Trycorders; the talk server plays what it receives with AudioTrack.
 */
internal class TalkAudio(private val context: Context, private val engine: LinkEngine) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var sending = false

    @Volatile
    private var serverSocket: DatagramSocket? = null
    private var serverJob: Job? = null

    fun startStreaming(targets: List<Peer>) {
        if (sending) return
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return engine.status("Microphone permission refused")
        sending = true
        scope.launch {
            networkCall { stream(targets) }.onFailure { engine.status("Streaming error: ${it.message}") }
            sending = false
        }
    }

    fun stopStreaming() {
        sending = false
    }

    fun startServer() {
        if (serverJob != null) return
        engine.status("Start talk server thread")
        serverJob = scope.launch {
            networkCall { serve() }.onFailure {
                if (serverSocket != null) engine.status("Talk server error: ${it.message}")
            }
        }
    }

    fun stopServer() {
        engine.status("Stop the talk server")
        val socket = serverSocket
        serverSocket = null
        socket?.close()
        serverJob?.cancel()
        serverJob = null
    }

    @SuppressLint("MissingPermission") // checked in startStreaming
    private fun stream(targets: List<Peer>) {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING)
        val addresses = targets.map { InetAddress.getByName(it.ip) }
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            ENCODING,
            minBuffer * RECORD_BUFFER_FACTOR,
        )
        try {
            DatagramSocket().use { socket -> pump(recorder, socket, addresses, ByteArray(minBuffer)) }
        } finally {
            recorder.release()
        }
    }

    private fun pump(recorder: AudioRecord, socket: DatagramSocket, addresses: List<InetAddress>, buffer: ByteArray) {
        recorder.startRecording()
        while (sending) {
            val read = recorder.read(buffer, 0, buffer.size)
            if (read > 0) addresses.forEach { socket.send(DatagramPacket(buffer, read, it, LinkProtocol.PORT)) }
        }
        recorder.stop()
    }

    private fun serve() {
        val socket = DatagramSocket(null).apply {
            reuseAddress = true
            bind(InetSocketAddress(LinkProtocol.PORT))
        }
        serverSocket = socket
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(ENCODING)
                    .build(),
            )
            .setBufferSizeInBytes(PLAY_BUFFER)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        try {
            track.play()
            receive(socket, track)
        } finally {
            track.flush()
            track.release()
            socket.close()
        }
    }

    private fun receive(socket: DatagramSocket, track: AudioTrack) {
        val data = ByteArray(PLAY_BUFFER)
        var last = 0L
        while (!socket.isClosed) {
            val packet = DatagramPacket(data, data.size)
            socket.receive(packet)
            val now = SystemClock.elapsedRealtime()
            if (now - last > SPEAKING_GAP_MILLIS) engine.status("Speaking ...")
            last = now
            track.write(packet.data, packet.offset, packet.length)
        }
    }

    private companion object {
        const val SAMPLE_RATE = Pcm.SAMPLE_RATE
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val RECORD_BUFFER_FACTOR = 10
        const val PLAY_BUFFER = 10_000
        const val SPEAKING_GAP_MILLIS = 1_000L
    }
}
