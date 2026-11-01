package net.ddns.mlsoftlaberge.trycorder.desktop

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import net.ddns.mlsoftlaberge.trycorder.core.FakeSpeechListener
import net.ddns.mlsoftlaberge.trycorder.core.InMemoryKeyValueStore
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository
import net.ddns.mlsoftlaberge.trycorder.link.FakeSystemInfo
import net.ddns.mlsoftlaberge.trycorder.link.LinkEvent
import net.ddns.mlsoftlaberge.trycorder.link.PeerList
import net.ddns.mlsoftlaberge.trycorder.link.RecordingSound
import net.ddns.mlsoftlaberge.trycorder.link.RecordingSpeaker
import net.ddns.mlsoftlaberge.trycorder.link.ServerStats
import net.ddns.mlsoftlaberge.trycorder.link.StarshipLink

class FakeStarshipLink : StarshipLink {
    override val events = MutableSharedFlow<LinkEvent>(extraBufferCapacity = 16)
    override val peers = MutableStateFlow(PeerList())
    override val stats = MutableStateFlow(ServerStats())
    override val running = MutableStateFlow(false)
    val sent = mutableListOf<String>()
    val audio = mutableListOf<Boolean>()

    override fun start() {
        running.value = true
    }

    override fun stop() {
        running.value = false
    }

    override fun send(text: String) {
        sent += text
    }

    override fun startAudioStream() {
        audio += true
    }

    override fun stopAudioStream() {
        audio += false
    }
}

/** Everything a walkie-talkie store talks to, recorded. */
class WalkieWorld(values: Map<String, Any> = emptyMap()) {
    val settings = SettingsRepository(InMemoryKeyValueStore(values))
    val sound = RecordingSound()
    val speaker = RecordingSpeaker()
    val listener = FakeSpeechListener()
    val link = FakeStarshipLink()
    val deps = WalkieDeps(settings, sound, speaker, listener, link, FakeSystemInfo())
}
