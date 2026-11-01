package net.ddns.mlsoftlaberge.trycorder.link

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.Speaker
import net.ddns.mlsoftlaberge.trycorder.core.SystemInfo

/**
 * iOS starship link: the shared [LinkEngine] (ktor sockets), Bonjour discovery and push-to-talk.
 * iOS has no long running background service, so the link runs while the app is alive in the foreground.
 */
class IosStarshipLink(
    private val settings: SettingsRepository,
    systemInfo: SystemInfo,
    sound: SoundPlayer,
    speaker: Speaker,
) : StarshipLink {
    private val engine = LinkEngine(
        settings = settings,
        systemInfo = systemInfo,
        sound = sound,
        speaker = speaker,
        config = LinkConfig(io = Dispatchers.IO, effects = Dispatchers.Main),
    )
    private val discovery = BonjourDiscovery(engine)
    private val talk = IosTalkAudio(engine)

    override val events: Flow<LinkEvent> = engine.events
    override val peers: StateFlow<PeerList> = engine.peers
    override val stats: StateFlow<ServerStats> = engine.stats
    override val running: StateFlow<Boolean> = engine.running

    override fun start() {
        if (engine.running.value) return
        engine.start()
        talk.startServer()
        discovery.start(settings.current.deviceName)
    }

    override fun stop() {
        if (!engine.running.value) return
        discovery.stop()
        talk.stopStreaming()
        talk.stopServer()
        engine.stop()
    }

    override fun send(text: String) = engine.send(text)

    override fun startAudioStream() = talk.startStreaming(engine.otherLocalPeers())

    override fun stopAudioStream() = talk.stopStreaming()
}
