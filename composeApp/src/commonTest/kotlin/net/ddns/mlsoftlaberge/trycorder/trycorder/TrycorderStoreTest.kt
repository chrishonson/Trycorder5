package net.ddns.mlsoftlaberge.trycorder.trycorder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.camera.Face
import net.ddns.mlsoftlaberge.trycorder.camera.SnapResult
import net.ddns.mlsoftlaberge.trycorder.core.PrefKeys
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.link.LinkEvent
import net.ddns.mlsoftlaberge.trycorder.link.Peer
import net.ddns.mlsoftlaberge.trycorder.link.PeerList
import net.ddns.mlsoftlaberge.trycorder.media.CaptureResult
import net.ddns.mlsoftlaberge.trycorder.sensors.Vector3
import net.ddns.mlsoftlaberge.trycorder.testing.FakeCamera
import net.ddns.mlsoftlaberge.trycorder.testing.TestServices
import net.ddns.mlsoftlaberge.trycorder.voice.TrekCommand
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TrycorderStoreTest {
    private val dispatcher = StandardTestDispatcher()
    private val navigated = mutableListOf<Screen>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.store(t: TestServices = TestServices()): TrycorderStore =
        TrycorderStore(t.services) { navigated += it }.also {
            it.dispatch(TrycorderIntent.Resume)
            advanceUntilIdle()
        }

    @Test
    fun initialStateIsReadyWithOwnIp() = runTest {
        val s = TrycorderStore(TestServices().services) {}.state.value
        assertEquals("Ready", s.bottomStatus)
        assertEquals("192.168.0.10", s.peersText)
        assertEquals(SensorPage.EarthStill, s.sensorPage)
    }

    @Test
    fun magneticButtonStartsTheSensorTheScanSoundAndEchoes() = runTest {
        val t = TestServices()
        val store = store(t)
        store.dispatch(TrycorderIntent.Command(TrekCommand.Magnetic))
        advanceUntilIdle()
        t.sensors.magnetic.emit(Vector3(1f, 2f, 3f))
        advanceUntilIdle()
        val s = store.state.value
        assertEquals(SensorPage.Magnetic, s.sensorPage)
        assertEquals(SensorPage.Magnetic, s.activeSensor)
        assertEquals(Vector3(1f, 2f, 3f), s.readings.magnetic)
        assertEquals(TrekSound.TricorderScan, t.sound.loop)
        assertTrue(s.musicPlaying)
        assertEquals("Sensors Magnetic", s.bottomStatus)
        assertEquals(listOf("computer magnetic"), t.link.sent)
        assertEquals(TrekSound.KeyOk, t.sound.played.first())
    }

    @Test
    fun stopButtonStopsTheSensorAndTheSound() = runTest {
        val t = TestServices()
        val store = store(t)
        store.dispatch(TrycorderIntent.Command(TrekCommand.Magnetic))
        advanceUntilIdle()
        store.dispatch(TrycorderIntent.StopSensors)
        advanceUntilIdle()
        assertNull(store.state.value.activeSensor)
        assertNull(t.sound.loop)
        assertEquals(0, t.sensors.magneticCollectors)
    }

    @Test
    fun echoIsSkippedWhenNotMaster() = runTest {
        val t = TestServices(mapOf(PrefKeys.IS_MASTER to false))
        val store = store(t)
        store.dispatch(TrycorderIntent.Command(TrekCommand.Phaser))
        assertTrue(t.link.sent.isEmpty())
    }

    @Test
    fun voiceCommandSwitchesGroupAndDoesNotEcho() = runTest {
        val t = TestServices()
        val store = store(t)
        store.dispatch(TrycorderIntent.Talk)
        t.listener.emit(listOf("fazer", "computer phaser"))
        advanceUntilIdle()
        val s = store.state.value
        assertEquals("computer phaser", s.topStatus)
        assertEquals(ButtonGroup.Fire, s.buttonGroup)
        assertEquals(SensorPage.Fire, s.sensorPage)
        assertEquals(2, s.fireMode)
        assertTrue(TrekSound.Phaser in t.sound.played)
        // the recognized sentence itself is sent, the command is not echoed again
        assertEquals(listOf("computer phaser"), t.link.sent)
    }

    @Test
    fun plainSpeechIsSpokenBackAndSent() = runTest {
        val t = TestServices()
        val store = store(t)
        store.dispatch(TrycorderIntent.Talk)
        t.listener.emit(listOf("hello bridge"))
        advanceUntilIdle()
        // like the original, sendtext() overwrites the status line right after "Understood"
        assertEquals("Send: hello bridge", store.state.value.bottomStatus)
        assertTrue("Understood: hello bridge" in store.state.value.consoleLog)
        assertEquals("hello bridge", t.speaker.spoken.last().first)
        assertEquals(listOf("hello bridge"), t.link.sent)
    }

    @Test
    fun frenchSwitchesListenAndSpeakLanguage() = runTest {
        val t = TestServices()
        val store = store(t)
        store.dispatch(TrycorderIntent.Talk)
        t.listener.emit(listOf("français"))
        advanceUntilIdle()
        assertEquals(TrekLanguage.French, store.state.value.listenLanguage)
        assertEquals("français" to TrekLanguage.French, t.speaker.spoken.last())
        store.dispatch(TrycorderIntent.Talk)
        assertEquals(2, t.listener.listenCount)
    }

    @Test
    fun linkEventsAreIgnoredWhilePaused() = runTest {
        val t = TestServices()
        val store = store(t)
        store.dispatch(TrycorderIntent.Pause)
        t.link.eventFlow.emit(LinkEvent.Text("computer phaser"))
        advanceUntilIdle()
        assertEquals(ButtonGroup.None, store.state.value.buttonGroup)
    }

    @Test
    fun speechRequestedByAnotherScreenIsIgnored() = runTest {
        val t = TestServices()
        val store = store(t)
        t.listener.emit(listOf("computer phaser"))
        advanceUntilIdle()
        assertEquals(ButtonGroup.None, store.state.value.buttonGroup)
        assertTrue(t.link.sent.isEmpty())
    }

    @Test
    fun rudenessResetsTheScreen() = runTest {
        val t = TestServices()
        val store = store(t)
        store.dispatch(TrycorderIntent.SelectGroup(ButtonGroup.Fire))
        t.link.eventFlow.emit(LinkEvent.Text("shit"))
        advanceUntilIdle()
        val s = store.state.value
        assertEquals(ButtonGroup.None, s.buttonGroup)
        assertEquals(SensorPage.EarthStill, s.sensorPage)
        assertEquals(ViewerPage.FederationLogo, s.viewerPage)
        assertEquals(TrekSound.DoNotAddress, t.sound.played.last())
    }

    @Test
    fun receivedChatTextGoesToTheChatLog() = runTest {
        val t = TestServices()
        val store = store(t)
        t.link.eventFlow.emit(LinkEvent.Text("hi from the bridge"))
        val peers = PeerList(listOf(Peer("1.2.3.4", "Nexus")), listOf(Peer("5.6.7.8", "Moto")))
        t.link.eventFlow.emit(LinkEvent.PeersChanged(peers))
        advanceUntilIdle()
        val s = store.state.value
        assertEquals("Received: hi from the bridge", s.bottomStatus)
        assertTrue(s.chatLog.startsWith("hi from the bridge"))
        assertEquals(":1.2.3.4 - Nexus\n*5.6.7.8 - Moto\n", s.peersText)
    }

    @Test
    fun commGroupShowsThePageOfTheCommStatus() = runTest {
        val store = store()
        store.dispatch(TrycorderIntent.Command(TrekCommand.ChatComm))
        store.dispatch(TrycorderIntent.SelectGroup(ButtonGroup.Shield))
        store.dispatch(TrycorderIntent.SelectGroup(ButtonGroup.Comm))
        val s = store.state.value
        assertEquals(SensorPage.Chat, s.sensorPage)
        assertEquals(ViewerPage.LogsConsole, s.viewerPage)
    }

    @Test
    fun chatSendSendsAndClears() = runTest {
        val t = TestServices()
        val store = store(t)
        store.dispatch(TrycorderIntent.ChatInput("demo please"))
        store.dispatch(TrycorderIntent.ChatSend)
        assertEquals("", store.state.value.chatInput)
        assertEquals(listOf("demo please"), t.link.sent)
        assertTrue(store.state.value.chatLog.startsWith("demo please"))
    }

    @Test
    fun snapWithoutViewerIsDenied() = runTest {
        val t = TestServices()
        val store = store(t)
        store.dispatch(TrycorderIntent.Snap)
        assertEquals(TrekSound.Deny, t.sound.played.last())
    }

    @Test
    fun snapShowsThePictureWithFaces() = runTest {
        val face = Face(10f, 20f, 5f)
        val snap = SnapResult(byteArrayOf(1), 1, 1, listOf(face), "x")
        val t = TestServices(camera = FakeCamera(isActive = true, result = snap))
        val store = store(t)
        store.dispatch(TrycorderIntent.Command(TrekCommand.ViewerOn))
        store.dispatch(TrycorderIntent.Snap)
        advanceUntilIdle()
        val s = store.state.value
        assertEquals(ViewerPage.Photo, s.viewerPage)
        assertEquals(listOf(face), s.picture.faces)
        assertEquals("Faces Found 1", s.bottomStatus)
    }

    @Test
    fun photoAppResultIsShown() = runTest {
        val t = TestServices()
        t.mediaCapture.photo = CaptureResult.Saved("content://photo/1")
        val store = store(t)
        store.dispatch(TrycorderIntent.TakePhoto)
        advanceUntilIdle()
        val s = store.state.value
        assertEquals(ViewerPage.Photo, s.viewerPage)
        assertEquals(3, s.picture.bytes?.size)
    }

    @Test
    fun transporterSliderBeamsDownFromZero() = runTest {
        val t = TestServices()
        val store = store(t)
        store.dispatch(TrycorderIntent.TransporterSlideStart(1))
        store.dispatch(TrycorderIntent.TransporterSlide(1, 100))
        store.dispatch(TrycorderIntent.TransporterSlideStop(1))
        val s = store.state.value
        assertEquals(TrekSound.BeamOut, t.sound.played.last())
        assertEquals(2, s.transporterMode)
        assertEquals(100, s.transporterPosition)
        assertEquals("Transport 2: 100/255", s.transporterTextUp)
        assertEquals("Transport 2 Failed: 100/255", s.transporterTextDown)
    }

    @Test
    fun beamControlsDriveTheVisibleGroup() = runTest {
        val store = store()
        store.dispatch(TrycorderIntent.SelectGroup(ButtonGroup.Shield))
        store.dispatch(TrycorderIntent.BeamFreq(7))
        store.dispatch(TrycorderIntent.SelectGroup(ButtonGroup.Tractor))
        store.dispatch(TrycorderIntent.BeamForce(3))
        val s = store.state.value
        assertEquals(7, s.shield.freq)
        assertEquals(0, s.shield.force)
        assertEquals(3, s.tractor.force)
    }

    @Test
    fun pauseSavesAndResumeRestores() = runTest {
        val t = TestServices()
        val first = store(t)
        first.dispatch(TrycorderIntent.SelectGroup(ButtonGroup.Logs))
        first.dispatch(TrycorderIntent.Command(TrekCommand.SystemPlans))
        first.dispatch(TrycorderIntent.Command(TrekCommand.Gravity))
        first.dispatch(TrycorderIntent.Pause)
        advanceUntilIdle()
        val second = store(t)
        second.dispatch(TrycorderIntent.Resume)
        advanceUntilIdle()
        val s = second.state.value
        assertEquals(ButtonGroup.Logs, s.buttonGroup)
        assertEquals(ViewerPage.Plans, s.viewerPage)
        assertEquals(SensorPage.Gravity, s.sensorPage)
        assertEquals(SensorPage.Gravity, s.activeSensor)
    }

    @Test
    fun stopKeepsTheServerWhenAutoStopIsOff() = runTest {
        val t = TestServices(mapOf(PrefKeys.AUTO_STOP to false))
        val store = store(t)
        store.dispatch(TrycorderIntent.Start)
        store.dispatch(TrycorderIntent.Stop)
        assertEquals(1, t.link.starts)
        assertEquals(0, t.link.stops)
    }

    @Test
    fun modeButtonsNavigate() = runTest {
        val store = store()
        store.dispatch(TrycorderIntent.OpenScreen(Screen.Crew))
        store.dispatch(TrycorderIntent.OpenGallery)
        store.dispatch(TrycorderIntent.OpenSettings)
        assertEquals(listOf(Screen.Crew, Screen.PhotoGallery, Screen.Settings), navigated)
    }

    @Test
    fun orientationPageOpensTheMap() = runTest {
        val t = TestServices()
        val store = store(t)
        store.dispatch(TrycorderIntent.OpenMap)
        assertEquals(listOf(0.0 to 0.0), t.launcher.maps)
        assertEquals("geo:0.0,0.0", store.state.value.bottomStatus)
    }

    @Test
    fun logsAreNewestFirstAndCapped() {
        var log = ""
        repeat(500) { log = TrycorderContext.prepend(log, "line $it") }
        assertTrue(log.startsWith("line 499\n"))
        assertEquals(TrycorderState.LOG_LIMIT, log.length)
    }
}
