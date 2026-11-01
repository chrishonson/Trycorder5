package net.ddns.mlsoftlaberge.trycorder.vision

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.camera.CameraController
import net.ddns.mlsoftlaberge.trycorder.camera.CameraEffect
import net.ddns.mlsoftlaberge.trycorder.camera.CameraScene
import net.ddns.mlsoftlaberge.trycorder.camera.SnapResult
import net.ddns.mlsoftlaberge.trycorder.core.FakeSpeechListener
import net.ddns.mlsoftlaberge.trycorder.core.Speaker
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.gallery.RecordingSound
import net.ddns.mlsoftlaberge.trycorder.link.LinkEvent
import net.ddns.mlsoftlaberge.trycorder.link.PeerList
import net.ddns.mlsoftlaberge.trycorder.link.ServerStats
import net.ddns.mlsoftlaberge.trycorder.link.StarshipLink
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FakeCamera(var active: Boolean = true, var result: SnapResult? = null) : CameraController {
    var snaps = 0
    override val isActive: Boolean get() = active
    override suspend fun snap(): SnapResult? {
        snaps++
        return result
    }
}

class FakeLink : StarshipLink {
    var streaming = false
    val calls = mutableListOf<String>()
    override val events = MutableSharedFlow<LinkEvent>()
    override val peers = MutableStateFlow(PeerList())
    override val stats = MutableStateFlow(ServerStats())
    override val running = MutableStateFlow(false)
    override fun start() = Unit
    override fun stop() = Unit
    override fun send(text: String) = Unit
    override fun startAudioStream() {
        streaming = true
        calls += "start"
    }

    override fun stopAudioStream() {
        streaming = false
        calls += "stop"
    }
}

class RecordingSpeaker : Speaker {
    val spoken = mutableListOf<Pair<String, TrekLanguage?>>()
    override fun speak(text: String, language: TrekLanguage?) {
        spoken += text to language
    }

    override fun setLanguage(language: TrekLanguage) = Unit
    override fun shutdown() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class VisionStoreTest {
    private val camera = FakeCamera()
    private val sound = RecordingSound()
    private val speaker = RecordingSpeaker()
    private val listener = FakeSpeechListener()
    private val link = FakeLink()
    private val navigated = mutableListOf<Screen>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.store(day: Boolean): VisionStore {
        val store = VisionStore(day, VisionServices(camera, sound, speaker, listener, link)) { navigated += it }
        store.dispatch(VisionIntent.Load)
        advanceUntilIdle()
        return store
    }

    @Test
    fun nightVisionCyclesTheNineEffectsOnTap() = runTest {
        val store = store(day = false)
        assertEquals("Vision", store.state.value.topStatus)
        assertEquals(CameraEffect.Mono, store.state.value.effect)
        assertNull(store.state.value.scene)
        val seen = (1..9).map {
            store.dispatch(VisionIntent.ViewerTap)
            store.state.value.effect
        }
        assertEquals(VisionModes.NIGHT_EFFECTS.drop(1) + CameraEffect.Mono, seen)
        assertEquals(9, sound.played.count { it == TrekSound.KeyOk })
        assertEquals("Ready", store.state.value.bottomStatus)
        assertEquals(0, camera.snaps)
    }

    @Test
    fun dayVisionSaysEffectAndScene() = runTest {
        val store = store(day = true)
        assertEquals("Vision Camera2", store.state.value.topStatus)
        assertEquals("View Normal, Portrait", store.state.value.bottomStatus)
        store.dispatch(VisionIntent.NextEffect)
        assertEquals("View Mono, Portrait", store.state.value.bottomStatus)
        store.dispatch(VisionIntent.NextScene)
        assertEquals(CameraScene.NightPortrait, store.state.value.scene)
        assertEquals("View Mono, Night Portrait", store.state.value.bottomStatus)
        store.dispatch(VisionIntent.NextScene)
        assertEquals(CameraScene.Portrait, store.state.value.scene)
    }

    @Test
    fun loadResetsTheModes() = runTest {
        val store = store(day = true)
        store.dispatch(VisionIntent.NextEffect)
        store.dispatch(VisionIntent.NextScene)
        store.dispatch(VisionIntent.Load)
        assertEquals(CameraEffect.None, store.state.value.effect)
        assertEquals(CameraScene.Portrait, store.state.value.scene)
    }

    @Test
    fun dayTapTakesAPicture() = runTest {
        camera.result = SnapResult(ByteArray(0), 1, 1, emptyList(), "x")
        val store = store(day = true)
        store.dispatch(VisionIntent.ViewerTap)
        advanceUntilIdle()
        assertEquals(1, camera.snaps)
        assertEquals("Picture saved", store.state.value.bottomStatus)
        camera.result = null
        store.dispatch(VisionIntent.ViewerTap)
        advanceUntilIdle()
        assertEquals("Something bad happened with vision", store.state.value.bottomStatus)
        assertEquals(TrekSound.Deny, sound.played.last())
    }

    @Test
    fun dayTapWithoutCameraIsDenied() = runTest {
        camera.active = false
        val store = store(day = true)
        store.dispatch(VisionIntent.ViewerTap)
        advanceUntilIdle()
        assertEquals(0, camera.snaps)
        assertEquals(TrekSound.Deny, sound.played.last())
    }

    @Test
    fun backAndSettingsNavigate() = runTest {
        val store = store(day = false)
        store.dispatch(VisionIntent.Settings)
        assertEquals("Settings", store.state.value.bottomStatus)
        store.dispatch(VisionIntent.Back)
        assertEquals(listOf(Screen.Settings, Screen.Trycorder), navigated)
    }

    @Test
    fun speakListensInFrenchAndAnswers() = runTest {
        val store = store(day = false)
        store.dispatch(VisionIntent.Speak)
        assertEquals(1, listener.listenCount)
        assertEquals("", store.state.value.topStatus)
        listener.emit(listOf("Martin"))
        advanceUntilIdle()
        assertEquals("Martin", store.state.value.topStatus)
        assertEquals("Said: Martin", store.state.value.bottomStatus)
        assertEquals("Martin est mon maître." to null, speaker.spoken.last())
        store.dispatch(VisionIntent.Speak)
        listener.emit(listOf("english please"))
        advanceUntilIdle()
        assertEquals(TrekLanguage.English, store.state.value.language)
        assertEquals("english" to TrekLanguage.English, speaker.spoken.last())
        store.dispatch(VisionIntent.Speak)
        listener.emit(listOf("hello there"))
        advanceUntilIdle()
        assertEquals("Understood: hello there", store.state.value.bottomStatus)
    }

    @Test
    fun speechNotAskedForIsIgnored() = runTest {
        val store = store(day = false)
        listener.emit(listOf("computer"))
        advanceUntilIdle()
        assertEquals("Vision", store.state.value.topStatus)
        assertEquals(0, speaker.spoken.size)
    }

    @Test
    fun talkStreamsWhilePressed() = runTest {
        val store = store(day = false)
        store.dispatch(VisionIntent.TalkDown)
        assertEquals("start streaming audio", store.state.value.bottomStatus)
        store.dispatch(VisionIntent.TalkDown)
        store.dispatch(VisionIntent.TalkUp)
        assertEquals("stop streaming audio", store.state.value.bottomStatus)
        assertEquals(listOf("start", "stop"), link.calls)
    }
}
