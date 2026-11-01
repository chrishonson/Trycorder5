package net.ddns.mlsoftlaberge.trycorder.link

import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.core.TrycorderSettings
import net.ddns.mlsoftlaberge.trycorder.voice.VoiceCommands

/** Where a received line came from: another Trycorder on the local server, or the tryserver link. */
enum class LineOrigin { Local, Remote }

/**
 * What to do with one received line. [remotePeers] and [stats] replace the current values when set;
 * [sound] and [speech] are the side effects the service played; [event] goes to the screens.
 */
data class LinkRoute(
    val event: LinkEvent? = null,
    val remotePeers: List<Peer>? = null,
    val stats: ServerStats? = null,
    val sound: TrekSound? = null,
    val speech: String? = null,
)

/** Pure port of TrycorderService.displaytext(): decides what a received line means. */
object LinkRouter {
    fun route(line: String, origin: LineOrigin, settings: TrycorderSettings): LinkRoute = when {
        LinkProtocol.TRYCORDERS_TAG in line -> LinkRoute(remotePeers = LinkProtocol.parseTrycorders(line))
        LinkProtocol.STATISTICS_TAG in line -> LinkProtocol.parseStatistics(line).let {
            LinkRoute(event = LinkEvent.StatsChanged(it), stats = it)
        }
        LinkProtocol.LOGS_TAG in line -> LinkRoute(event = LinkEvent.Logs(line.substringAfter(LinkProtocol.LOGS_TAG)))
        "server ok" in line -> LinkRoute(sound = TrekSound.ComputerBeep39)
        "red alert" in line -> LinkRoute(sound = TrekSound.RedAlert, speech = line)
        "yellow alert" in line -> LinkRoute(sound = TrekSound.Alert, speech = line)
        else -> message(line, origin, settings)
    }

    /** A chat line or a command: spoken unless it is a command, executed if the exec setting allows it. */
    private fun message(line: String, origin: LineOrigin, settings: TrycorderSettings): LinkRoute {
        val speech = if (VoiceCommands.looksLikeCommand(line)) null else line
        val allowed = when (origin) {
            LineOrigin.Local -> settings.execLocal
            LineOrigin.Remote -> settings.execRemote
        }
        return LinkRoute(event = if (allowed) LinkEvent.Text(line) else null, speech = speech)
    }
}
