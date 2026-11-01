package net.ddns.mlsoftlaberge.trycorder.link

import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkProtocolTest {
    @Test
    fun identificationLine() {
        assertEquals("trycorder:Pixel 9/15/6.0.0", LinkProtocol.identification("Pixel 9", "15", "6.0.0"))
    }

    @Test
    fun parsesTheTrycorderList() {
        val peers = LinkProtocol.parseTrycorders("trycorders:1.2.3.4,Kirk:5.6.7.8,Spock Phone:")
        assertEquals(listOf(Peer("1.2.3.4", "Kirk"), Peer("5.6.7.8", "Spock Phone")), peers)
    }

    @Test
    fun skipsMalformedTrycorderEntries() {
        assertEquals(listOf(Peer("1.2.3.4", "Kirk")), LinkProtocol.parseTrycorders("trycorders:garbage:1.2.3.4,Kirk:"))
        assertEquals(emptyList(), LinkProtocol.parseTrycorders("trycorders:"))
    }

    @Test
    fun parsesStatistics() {
        assertEquals(ServerStats(12, 3, 7, 5), LinkProtocol.parseStatistics("statistics:12,3,7,5"))
    }

    @Test
    fun unreadableStatisticsCountAsZero() {
        assertEquals(ServerStats(4, 0, 0, 0), LinkProtocol.parseStatistics("statistics:4,x"))
    }

    @Test
    fun cleansEscapedServiceNames() {
        assertEquals("Galaxy S24", LinkProtocol.cleanServiceName("Galaxy\\032S24"))
    }

    @Test
    fun readsIpv4SockAddr() {
        val bytes = byteArrayOf(16, 2, 6, -91, -64, -88, 1, 42, 0, 0, 0, 0, 0, 0, 0, 0)
        assertEquals("192.168.1.42", SockAddr.ipv4(bytes))
        assertNull(SockAddr.ipv4(byteArrayOf(28, 30, 6, -91, 0, 0, 0, 0)))
        assertNull(SockAddr.ipv4(byteArrayOf(16, 2)))
    }

    @Test
    fun speechLocales() {
        assertEquals("fr-FR", SpeechLocales.tag(TrekLanguage.French))
        assertEquals("en-US", SpeechLocales.tag(TrekLanguage.English))
        assertNull(SpeechLocales.tag(TrekLanguage.Auto))
    }

    @Test
    fun backoffDoublesUpToTheMaximumAndResets() {
        val backoff = ReconnectBackoff(initialMillis = 1_000, maxMillis = 5_000)
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 5_000L, 5_000L), List(5) { backoff.nextDelay() })
        backoff.reset()
        assertEquals(1_000L, backoff.nextDelay())
    }
}
