package net.ddns.mlsoftlaberge.trycorder.link

import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.core.TrycorderSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkRouterTest {
    private val settings = TrycorderSettings()

    @Test
    fun trycorderListReplacesRemotePeers() {
        val route = LinkRouter.route("trycorders:1.2.3.4,Kirk:", LineOrigin.Remote, settings)
        assertEquals(listOf(Peer("1.2.3.4", "Kirk")), route.remotePeers)
        assertNull(route.event)
        assertNull(route.speech)
    }

    @Test
    fun statisticsAreStoredAndAnnounced() {
        val route = LinkRouter.route("statistics:3,2,1,4", LineOrigin.Remote, settings)
        assertEquals(ServerStats(3, 2, 1, 4), route.stats)
        assertEquals(LinkEvent.StatsChanged(ServerStats(3, 2, 1, 4)), route.event)
    }

    @Test
    fun logsLinesLoseTheirTag() {
        val route = LinkRouter.route("logs:nbtrycorders=12", LineOrigin.Remote, settings)
        assertEquals(LinkEvent.Logs("nbtrycorders=12"), route.event)
    }

    @Test
    fun serverOkBeeps() {
        val route = LinkRouter.route("server ok", LineOrigin.Remote, settings)
        assertEquals(LinkRoute(sound = TrekSound.ComputerBeep39), route)
    }

    @Test
    fun alertsPlayTheirSoundAndAreSpoken() {
        assertEquals(
            LinkRoute(sound = TrekSound.RedAlert, speech = "red alert"),
            LinkRouter.route("red alert", LineOrigin.Local, settings),
        )
        assertEquals(
            LinkRoute(sound = TrekSound.Alert, speech = "computer yellow alert"),
            LinkRouter.route("computer yellow alert", LineOrigin.Local, settings),
        )
    }

    @Test
    fun chatIsSpokenAndForwarded() {
        val route = LinkRouter.route("hello captain", LineOrigin.Local, settings)
        assertEquals("hello captain", route.speech)
        assertEquals(LinkEvent.Text("hello captain"), route.event)
    }

    @Test
    fun commandsAreForwardedButNotSpoken() {
        val route = LinkRouter.route("computer phaser", LineOrigin.Remote, settings)
        assertNull(route.speech)
        assertEquals(LinkEvent.Text("computer phaser"), route.event)
    }

    @Test
    fun execSettingsFilterByOrigin() {
        val noLocal = settings.copy(execLocal = false)
        assertNull(LinkRouter.route("computer phaser", LineOrigin.Local, noLocal).event)
        val remote = LinkRouter.route("computer phaser", LineOrigin.Remote, noLocal)
        assertEquals(LinkEvent.Text("computer phaser"), remote.event)
        val noRemote = settings.copy(execRemote = false)
        val route = LinkRouter.route("hello", LineOrigin.Remote, noRemote)
        assertNull(route.event)
        assertEquals("hello", route.speech)
    }
}
