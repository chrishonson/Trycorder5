package net.ddns.mlsoftlaberge.trycorder.link

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.sockets.port
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * Runs [block] and turns network failures into a failed [Result]. Cancellation is rethrown so
 * structured concurrency keeps working; any other error ends only the current connection.
 */
internal inline fun <T> networkCall(block: () -> T): Result<T> =
    runCatching(block).onFailure { if (it is CancellationException) throw it }

/** Reads lines from [socket] until the other side closes it. */
internal suspend fun readLines(socket: Socket, onLine: (String) -> Unit) {
    val input = socket.openReadChannel()
    while (true) {
        val line = input.readUTF8Line() ?: break
        onLine(line)
    }
}

/**
 * The persistent connection to the tryserver (the old StarshipThread): connect, identify,
 * read lines until the link drops, wait with a backoff and connect again.
 */
internal class ServerConnection(
    private val host: () -> String,
    private val port: Int,
    private val identification: () -> String,
    private val backoff: ReconnectBackoff,
    private val status: (String) -> Unit,
) {
    private val lock = Mutex()
    private var writer: ByteWriteChannel? = null

    suspend fun run(selector: SelectorManager, onLine: (String) -> Unit) {
        while (currentCoroutineContext().isActive) {
            val destination = host()
            val result = networkCall { session(selector, destination, onLine) }
            lock.withLock { writer = null }
            val wait = backoff.nextDelay()
            val reason = result.exceptionOrNull()?.message ?: "closed"
            status("Starship network $destination lost: $reason, retry in ${wait / MILLIS_PER_SECOND}s")
            delay(wait)
        }
    }

    /** serversend(): write a line on the open tryserver connection. False when it is not connected. */
    suspend fun write(text: String): Boolean = lock.withLock {
        val out = writer ?: return@withLock false
        networkCall { out.writeStringUtf8("$text\n") }.isSuccess
    }

    private suspend fun session(selector: SelectorManager, destination: String, onLine: (String) -> Unit) {
        val socket = aSocket(selector).tcp().connect(destination, port)
        try {
            val out = socket.openWriteChannel(autoFlush = true)
            out.writeStringUtf8(identification() + "\n")
            lock.withLock { writer = out }
            backoff.reset()
            status("Starship network connected to $destination")
            readLines(socket, onLine)
        } finally {
            socket.close()
        }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000
    }
}

/** The local TCP server other Trycorders send their lines to (the old ServerThread/CommunicationThread). */
internal class LocalServer(private val port: Int, private val status: (String) -> Unit) {
    /** The port actually bound, once listening (differs from [port] when it is 0). */
    var boundPort: Int? = null
        private set

    suspend fun run(selector: SelectorManager, onLine: (String) -> Unit) {
        val server = networkCall { aSocket(selector).tcp().bind(ANY_ADDRESS, port) { reuseAddress = true } }
            .getOrElse {
                status("Cant start network server: ${it.message}")
                return
            }
        boundPort = server.port
        try {
            coroutineScope {
                status("Accepting server socket")
                while (isActive) {
                    val client = server.accept()
                    status("Accepted socket")
                    launch {
                        status("Receiving text")
                        networkCall { readLines(client, onLine) }
                        client.close()
                    }
                }
            }
        } finally {
            server.close()
        }
    }

    private companion object {
        const val ANY_ADDRESS = "0.0.0.0"
    }
}

/** clientsend(): a short connection to another Trycorder to deliver one line. */
internal suspend fun sendToPeer(selector: SelectorManager, ip: String, port: Int, text: String, timeoutMillis: Long) {
    withTimeout(timeoutMillis) {
        val socket = aSocket(selector).tcp().connect(ip, port)
        try {
            val out = socket.openWriteChannel(autoFlush = true)
            out.writeStringUtf8("$text\n")
            out.flushAndClose()
        } finally {
            socket.close()
        }
    }
}
