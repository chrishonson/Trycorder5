package net.ddns.mlsoftlaberge.trycorder.desktop

import net.ddns.mlsoftlaberge.trycorder.link.PeerList

/** Text formatting of the walkie-talkie screens. */
object WalkieFormat {
    const val CONSOLE_PLACEHOLDER = "Console log"

    /** The console keeps the newest lines; the original StringBuffer grew forever. */
    const val MAX_LOG_CHARS = 5_000

    /** saylist(): one "ip - name" line per local Trycorder, or our own IP before the link knows any. */
    fun peers(peers: PeerList, ownIp: String): String =
        if (peers.local.isEmpty()) ownIp else peers.local.joinToString("") { "${it.ip} - ${it.name}\n" }

    /** say(): the new line goes on top of the console log. */
    fun prepend(log: String, line: String): String {
        val previous = if (log == CONSOLE_PLACEHOLDER) "" else log
        return "$line\n$previous".take(MAX_LOG_CHARS)
    }
}
