package net.ddns.mlsoftlaberge.trycorder.link

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.sockets.port
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.ddns.mlsoftlaberge.trycorder.core.InMemoryKeyValueStore
import net.ddns.mlsoftlaberge.trycorder.core.PrefKeys
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Exercises the engine over real loopback sockets. */
class LinkEngineTest {
    private val sound = RecordingSound()
    private val speaker = RecordingSpeaker()

    private fun settings(vararg values: Pair<String, Any>) = SettingsRepository(
        InMemoryKeyValueStore(
            mapOf(
                PrefKeys.DEBUG_MODE to true,
                PrefKeys.DEBUG_ADDR to "127.0.0.1",
                PrefKeys.DEVICE_NAME to "Enterprise",
            ) + values,
        ),
    )

    private fun engine(settings: SettingsRepository, serverPort: Int, peerPort: Int = LinkProtocol.PORT) = LinkEngine(
        settings = settings,
        systemInfo = FakeSystemInfo(),
        sound = sound,
        speaker = speaker,
        config = LinkConfig(
            io = Dispatchers.Default,
            effects = Dispatchers.Unconfined,
            serverPort = serverPort,
            localPort = 0,
            peerPort = peerPort,
            backoff = ReconnectBackoff(initialMillis = 50, maxMillis = 200),
        ),
    )

    private fun loopback(test: suspend CoroutineScope.(SelectorManager, ServerSocket) -> Unit) = runTest {
        withContext(Dispatchers.Default) {
            withTimeout(TIMEOUT) {
                val selector = SelectorManager(Dispatchers.Default)
                val server = aSocket(selector).tcp().bind("127.0.0.1", 0)
                try {
                    test(selector, server)
                } finally {
                    coroutineContext.cancelChildren()
                    server.close()
                    selector.close()
                }
            }
        }
    }

    private fun CoroutineScope.collect(engine: LinkEngine): Channel<LinkEvent> {
        val channel = Channel<LinkEvent>(Channel.UNLIMITED)
        launch { engine.events.collect { channel.send(it) } }
        return channel
    }

    private suspend fun Channel<LinkEvent>.await(predicate: (LinkEvent) -> Boolean): LinkEvent {
        while (true) {
            val event = receive()
            if (predicate(event)) return event
        }
    }

    @Test
    fun identifiesToTheServerAndRoutesItsLines() = loopback { _, server ->
        val link = engine(settings(), server.port)
        val events = collect(link)
        delay(SUBSCRIBE_MILLIS)
        link.start()
        val client = server.accept()
        val input = client.openReadChannel()
        val output = client.openWriteChannel(autoFlush = true)
        assertEquals("trycorder:Pixel/15/6.0.0", input.readUTF8Line())

        output.writeStringUtf8("statistics:3,2,1,4\ntrycorders:1.2.3.4,Kirk:\nred alert\n")
        assertEquals(ServerStats(3, 2, 1, 4), link.stats.first { it.trycorders == 3 })
        assertEquals(listOf(Peer("1.2.3.4", "Kirk")), link.peers.first { it.remote.isNotEmpty() }.remote)
        events.await { it is LinkEvent.StatsChanged }
        while (speaker.spoken.isEmpty()) delay(POLL_MILLIS)
        assertEquals(listOf(TrekSound.RedAlert), sound.played)
        assertEquals("red alert", speaker.spoken.single().first)

        link.send("hello starfleet")
        assertEquals("hello starfleet", input.readUTF8Line())
        link.stop()
        assertFalse(link.running.value)
        client.close()
    }

    @Test
    fun reconnectsAfterTheServerDropsTheLink() = loopback { _, server ->
        val link = engine(settings(), server.port)
        link.start()
        server.accept().close()
        val second = server.accept()
        assertEquals("trycorder:Pixel/15/6.0.0", second.openReadChannel().readUTF8Line())
        link.stop()
        second.close()
    }

    @Test
    fun localServerForwardsCommandsAndSpeaksChat() = loopback { selector, server ->
        val link = engine(settings(), server.port)
        val events = collect(link)
        delay(SUBSCRIBE_MILLIS)
        link.start()
        var port = link.localServerPort
        while (port == null) {
            delay(POLL_MILLIS)
            port = link.localServerPort
        }
        val peer = aSocket(selector).tcp().connect("127.0.0.1", port)
        peer.openWriteChannel(autoFlush = true).writeStringUtf8("computer phaser\nhello captain\n")
        assertEquals(LinkEvent.Text("computer phaser"), events.await { it is LinkEvent.Text })
        assertEquals(LinkEvent.Text("hello captain"), events.await { it is LinkEvent.Text })
        while (speaker.spoken.isEmpty()) delay(POLL_MILLIS)
        assertEquals(listOf("hello captain"), speaker.spoken.map { it.first })
        peer.close()
        link.stop()
    }

    @Test
    fun sendGoesToLocalPeersButNotToOurselves() = loopback { _, server ->
        val link = engine(settings(PrefKeys.SEND_REMOTE to false), serverPort = 1, peerPort = server.port)
        link.start()
        link.addLocalPeer(Peer("10.0.0.9", "Ourselves"))
        link.addLocalPeer(Peer("127.0.0.1", "Spock"))
        assertEquals(
            listOf(Peer("10.0.0.9", "Enterprise"), Peer("127.0.0.1", "Spock")),
            link.peers.value.local,
        )
        assertEquals(listOf(Peer("127.0.0.1", "Spock")), link.otherLocalPeers())
        link.send("fascinating")
        val client = server.accept()
        assertEquals("fascinating", client.openReadChannel().readUTF8Line())
        client.close()
        link.removeLocalPeer("Spock")
        assertTrue(link.otherLocalPeers().isEmpty())
        link.stop()
    }

    @Test
    fun sendWhileStoppedOnlyReportsIt() = runTest {
        val link = engine(settings(), serverPort = 1)
        val events = Channel<LinkEvent>(Channel.UNLIMITED)
        val job = launch { link.events.collect { events.send(it) } }
        testScheduler.runCurrent()
        link.send("anyone")
        testScheduler.runCurrent()
        assertTrue((events.receive() as LinkEvent.Status).text.contains("not sent"))
        job.cancel()
    }

    private companion object {
        const val TIMEOUT = 20_000L
        const val POLL_MILLIS = 20L
        const val SUBSCRIBE_MILLIS = 50L
    }
}
