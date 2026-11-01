package net.ddns.mlsoftlaberge.trycorder.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.core.PrefKeys
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.link.LinkEvent
import net.ddns.mlsoftlaberge.trycorder.link.Peer
import net.ddns.mlsoftlaberge.trycorder.link.PeerList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopStoreTest {
    private val navigated = mutableListOf<Screen>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun store(world: WalkieWorld) = DesktopStore(world.deps) { navigated += it }

    @Test
    fun startsLikeTheLayout() {
        val state = store(WalkieWorld()).state.value
        assertEquals("Desktop", state.topStatus)
        assertEquals("Ready", state.bottomStatus)
        assertEquals("Console log", state.consoleLog)
        assertEquals("10.0.0.9", state.peersText)
    }

    @Test
    fun cornersAndBackReturnToTheTrycorderMode() {
        val world = WalkieWorld()
        val store = store(world)
        store.dispatch(WalkieIntent.Back)
        assertEquals(listOf(Screen.Trycorder), navigated)
        assertEquals(listOf(TrekSound.KeyOk), world.sound.played)
    }

    @Test
    fun settingsOpensTheSettingsScreen() {
        val world = WalkieWorld(mapOf(PrefKeys.IS_CHATTY to true))
        val store = store(world)
        store.dispatch(WalkieIntent.Settings)
        assertEquals(listOf(Screen.Settings), navigated)
        assertEquals("Settings", store.state.value.bottomStatus)
        assertEquals("Settings", world.speaker.spoken.single().first)
    }

    @Test
    fun beepOnlyButtons() {
        val world = WalkieWorld()
        val store = store(world)
        listOf(WalkieIntent.View, WalkieIntent.Send, WalkieIntent.CommMode).forEach(store::dispatch)
        assertEquals(List(3) { TrekSound.KeyOk }, world.sound.played)
        assertTrue(navigated.isEmpty())
    }

    @Test
    fun speakListensInTheListenLanguage() {
        val world = WalkieWorld(mapOf(PrefKeys.LISTEN_LANGUAGE to "FR"))
        val store = store(world)
        store.dispatch(WalkieIntent.Speak)
        assertEquals(1, world.listener.listenCount)
        assertEquals("", store.state.value.topStatus)
        assertEquals(TrekLanguage.French, store.state.value.listenLanguage)
    }

    @Test
    fun understoodSentencesAreSent() = runTest {
        val world = WalkieWorld()
        val store = store(world)
        store.dispatch(WalkieIntent.Start)
        advanceUntilIdle()
        world.listener.emit(listOf("hello there", "computer phaser"))
        advanceUntilIdle()
        assertEquals(listOf("computer phaser"), world.link.sent)
        assertEquals("computer phaser", store.state.value.topStatus)
        assertEquals("Send: computer phaser", store.state.value.bottomStatus)
        assertTrue(store.state.value.consoleLog.startsWith("Send: computer phaser\nUnderstood: computer phaser\n"))
    }

    @Test
    fun languageSwitchIsSaidAndSpoken() = runTest {
        val world = WalkieWorld(mapOf(PrefKeys.AUTO_LISTEN to true))
        val store = store(world)
        store.dispatch(WalkieIntent.Start)
        advanceUntilIdle()
        world.listener.emit(listOf("français"))
        advanceUntilIdle()
        val state = store.state.value
        assertEquals(TrekLanguage.French, state.speakLanguage)
        assertEquals(TrekLanguage.French, state.listenLanguage)
        assertEquals("français" to TrekLanguage.French, world.speaker.spoken.single())
        assertTrue(state.consoleLog.contains("Said: français"))
        assertEquals(1, world.listener.listenCount)
    }

    @Test
    fun linkEventsAreShownWhileVisible() = runTest {
        val world = WalkieWorld()
        val store = store(world)
        store.dispatch(WalkieIntent.Start)
        advanceUntilIdle()
        world.link.events.emit(LinkEvent.Text("hello captain"))
        world.link.events.emit(LinkEvent.Status("Accepted socket"))
        world.link.peers.value = PeerList(local = listOf(Peer("10.0.0.9", "Enterprise"), Peer("10.0.0.7", "Spock")))
        world.link.events.emit(LinkEvent.PeersChanged(world.link.peers.value))
        advanceUntilIdle()
        val state = store.state.value
        assertEquals("hello captain", state.topStatus)
        assertEquals("Accepted socket", state.bottomStatus)
        assertTrue(state.consoleLog.contains("Received: hello captain"))
        assertEquals("10.0.0.9 - Enterprise\n10.0.0.7 - Spock\n", state.peersText)
        assertTrue(world.speaker.spoken.isEmpty())

        store.dispatch(WalkieIntent.Stop)
        advanceUntilIdle()
        world.link.events.emit(LinkEvent.Status("ignored"))
        advanceUntilIdle()
        assertEquals("Accepted socket", store.state.value.bottomStatus)
    }

    @Test
    fun serverButtonsStartAndStopTheLink() {
        val world = WalkieWorld()
        val store = store(world)
        store.dispatch(WalkieIntent.ServerOn)
        assertTrue(world.link.running.value)
        assertEquals("Start Trycorder Service", store.state.value.bottomStatus)
        store.dispatch(WalkieIntent.ServerOff)
        assertEquals(false, world.link.running.value)
        assertEquals("Stop Trycorder Service", store.state.value.bottomStatus)
    }

    @Test
    fun pushToTalkStreamsWhileHeld() {
        val world = WalkieWorld()
        val store = store(world)
        store.dispatch(WalkieIntent.TalkDown)
        assertEquals("start streaming audio", store.state.value.bottomStatus)
        store.dispatch(WalkieIntent.TalkUp)
        assertEquals("stop streaming audio", store.state.value.bottomStatus)
        assertEquals(listOf(true, false), world.link.audio)
    }

    @Test
    fun scanShowsTheLocalPeers() {
        val world = WalkieWorld()
        val store = store(world)
        world.link.peers.value = PeerList(local = listOf(Peer("10.0.0.9", "Enterprise")))
        store.dispatch(WalkieIntent.Scan)
        assertEquals("10.0.0.9 - Enterprise\n", store.state.value.peersText)
    }

    @Test
    fun consoleKeepsTheNewestLinesFirst() {
        assertEquals("b\na\n", WalkieFormat.prepend(WalkieFormat.prepend(WalkieFormat.CONSOLE_PLACEHOLDER, "a"), "b"))
        val full = "x".repeat(WalkieFormat.MAX_LOG_CHARS)
        assertEquals(WalkieFormat.MAX_LOG_CHARS, WalkieFormat.prepend(full, "y").length)
        assertEquals("1.2.3.4", WalkieFormat.peers(PeerList(), "1.2.3.4"))
    }
}
