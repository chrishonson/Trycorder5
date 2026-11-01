package net.ddns.mlsoftlaberge.trycorder.link

import io.ktor.network.selector.SelectorManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.Speaker
import net.ddns.mlsoftlaberge.trycorder.core.SystemInfo
import kotlin.coroutines.CoroutineContext

/** Platform knobs of the [LinkEngine]. Ports are only changed by tests. */
data class LinkConfig(
    /** Where the sockets run. */
    val io: CoroutineDispatcher,
    /** Where sounds and speech are played (the main thread on the devices, like the old Handler posts). */
    val effects: CoroutineContext,
    val serverPort: Int = LinkProtocol.PORT,
    val localPort: Int = LinkProtocol.PORT,
    val peerPort: Int = LinkProtocol.PORT,
    val peerTimeoutMillis: Long = PEER_TIMEOUT_MILLIS,
    val backoff: ReconnectBackoff = ReconnectBackoff(),
) {
    companion object {
        const val PEER_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * The shared network engine of the old TrycorderService: the tryserver link, the local server,
 * sending lines, and the routing of received lines. One [CoroutineScope] lives from [start] to [stop].
 * Platform links add discovery ([addLocalPeer]) and push-to-talk audio around it.
 */
class LinkEngine(
    private val settings: SettingsRepository,
    private val systemInfo: SystemInfo,
    private val sound: SoundPlayer,
    private val speaker: Speaker,
    private val config: LinkConfig,
) {
    private val _events = MutableSharedFlow<LinkEvent>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<LinkEvent> = _events.asSharedFlow()

    private val _peers = MutableStateFlow(PeerList())
    val peers: StateFlow<PeerList> = _peers.asStateFlow()

    private val _stats = MutableStateFlow(ServerStats())
    val stats: StateFlow<ServerStats> = _stats.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private var scope: CoroutineScope? = null
    private var selector: SelectorManager? = null
    private val discovered = MutableStateFlow<List<Peer>>(emptyList())

    private val server = ServerConnection(
        host = { settings.current.serverHost },
        port = config.serverPort,
        identification = {
            LinkProtocol.identification(systemInfo.deviceName(), systemInfo.deviceRelease(), systemInfo.appVersion())
        },
        backoff = config.backoff,
        status = ::status,
    )
    private val localServer = LocalServer(config.localPort, ::status)

    /** Port of the local server once it listens (tests bind port 0). */
    val localServerPort: Int? get() = localServer.boundPort

    fun start() {
        if (scope != null) return
        val newSelector = SelectorManager(config.io)
        val newScope = CoroutineScope(SupervisorJob() + config.io)
        selector = newSelector
        scope = newScope
        discovered.value = emptyList()
        publishLocalPeers()
        _running.value = true
        status("Initialize the network server")
        newScope.launch { localServer.run(newSelector) { onLine(it, LineOrigin.Local) } }
        status("Initialize the starship network")
        newScope.launch { server.run(newSelector) { onLine(it, LineOrigin.Remote) } }
    }

    fun stop() {
        val current = scope ?: return
        status("Stop the starship network")
        status("Stop the network")
        current.cancel()
        selector?.close()
        scope = null
        selector = null
        _running.value = false
    }

    /** sendtext(): to every other local Trycorder when sendLocal, to the tryserver when sendRemote. */
    fun send(text: String) {
        val current = scope
        val currentSelector = selector
        if (current == null || currentSelector == null) {
            status("Starship network is off, not sent: $text")
            return
        }
        val s = settings.current
        if (s.sendLocal) {
            otherLocalPeers().forEach { peer ->
                current.launch {
                    networkCall {
                        sendToPeer(currentSelector, peer.ip, config.peerPort, text, config.peerTimeoutMillis)
                    }.onFailure { status("Cant send to ${peer.ip}: ${it.message}") }
                }
            }
        }
        if (s.sendRemote) {
            current.launch { if (!server.write(text)) status("Starship network not connected") }
        }
    }

    /** addiplist(): a Trycorder found on the local network. */
    fun addLocalPeer(peer: Peer) {
        if (peer.ip.isBlank() || peer.ip == systemInfo.ipAddress()) return
        var added = false
        discovered.update { list ->
            added = list.none { it.ip == peer.ip }
            if (added) list + peer else list
        }
        publishLocalPeers()
        if (!added) return
        status("Added ${peer.ip} - ${peer.name}")
    }

    /** A Trycorder that left the local network. */
    fun removeLocalPeer(name: String) {
        discovered.update { list -> list.filterNot { it.name == name } }
        publishLocalPeers()
    }

    /** The local Trycorders but ourselves: where lines and push-to-talk audio go. */
    fun otherLocalPeers(): List<Peer> {
        val ownIp = systemInfo.ipAddress()
        return discovered.value.filter { it.ip != ownIp }
    }

    /** say(): a status line for the screens. */
    fun status(text: String) {
        _events.tryEmit(LinkEvent.Status(text))
    }

    private fun publishLocalPeers() {
        val self = Peer(systemInfo.ipAddress(), settings.current.deviceName)
        _peers.update { it.copy(local = listOf(self) + discovered.value) }
        _events.tryEmit(LinkEvent.PeersChanged(_peers.value))
    }

    private fun onLine(line: String, origin: LineOrigin) {
        val route = LinkRouter.route(line, origin, settings.current)
        route.remotePeers?.let { remote ->
            _peers.update { it.copy(remote = remote) }
            _events.tryEmit(LinkEvent.PeersChanged(_peers.value))
        }
        route.stats?.let { _stats.value = it }
        route.event?.let { _events.tryEmit(it) }
        if (route.sound != null || route.speech != null) {
            scope?.launch(config.effects) {
                route.sound?.let { sound.play(it) }
                route.speech?.let { speaker.speak(it) }
            }
        }
    }

    private companion object {
        const val EVENT_BUFFER = 64
    }
}
