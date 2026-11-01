package net.ddns.mlsoftlaberge.trycorder.link

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Another Trycorder: found on the local network (NSD) or announced by the tryserver. */
data class Peer(val ip: String, val name: String)

data class PeerList(val local: List<Peer> = emptyList(), val remote: List<Peer> = emptyList())

/** Statistics line sent by the tryserver ("statistics:trycorders,countrys,citys,states"). */
data class ServerStats(val trycorders: Int = 0, val countries: Int = 0, val cities: Int = 0, val states: Int = 0)

/** Events the old TrycorderService broadcast to the activity (TRYSERVERCMD extras). */
sealed interface LinkEvent {
    /** "iplist": the peer list changed. */
    data class PeersChanged(val peers: PeerList) : LinkEvent

    /** "statlist": new server statistics. */
    data class StatsChanged(val stats: ServerStats) : LinkEvent

    /** "logslist": a logs line from the server. */
    data class Logs(val text: String) : LinkEvent

    /** "text": a message or command received from another Trycorder or the server, allowed to execute. */
    data class Text(val text: String) : LinkEvent

    /** Status/debug line from the link itself. */
    data class Status(val text: String) : LinkEvent
}

/**
 * The network side of the old TrycorderService: TCP link to the tryserver on port 1701,
 * a local TCP server for other Trycorders, NSD discovery, and push-to-talk UDP audio.
 */
interface StarshipLink {
    val events: Flow<LinkEvent>
    val peers: StateFlow<PeerList>
    val stats: StateFlow<ServerStats>
    val running: StateFlow<Boolean>

    fun start()
    fun stop()

    /** Send a line to the other Trycorders and to the server, following the send settings. */
    fun send(text: String)

    fun startAudioStream()
    fun stopAudioStream()
}
