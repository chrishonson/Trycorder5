package net.ddns.mlsoftlaberge.trycorder.multisensor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.core.ExternalLauncher
import net.ddns.mlsoftlaberge.trycorder.core.FakeSpeechListener
import net.ddns.mlsoftlaberge.trycorder.core.InMemoryKeyValueStore
import net.ddns.mlsoftlaberge.trycorder.core.PrefKeys
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.Speaker
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.sensors.Environment
import net.ddns.mlsoftlaberge.trycorder.sensors.GeoLocation
import net.ddns.mlsoftlaberge.trycorder.sensors.SensorHub
import net.ddns.mlsoftlaberge.trycorder.sensors.Vector3
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class FakeSensorHub : SensorHub {
    val magneticFlow = MutableSharedFlow<Vector3>()
    val orientationFlow = MutableSharedFlow<Vector3>()
    val accelerationFlow = MutableSharedFlow<Vector3>()
    val environmentFlow = MutableSharedFlow<Environment>()
    val locationFlow = MutableSharedFlow<GeoLocation>()
    val audioFlow = MutableSharedFlow<ShortArray>()

    override fun magnetic(): Flow<Vector3> = magneticFlow
    override fun orientation(): Flow<Vector3> = orientationFlow
    override fun acceleration(): Flow<Vector3> = accelerationFlow
    override fun environment(): Flow<Environment> = environmentFlow
    override fun location(): Flow<GeoLocation> = locationFlow
    override fun audio(): Flow<ShortArray> = audioFlow
}

private class RecordingSound : SoundPlayer {
    val played = mutableListOf<TrekSound>()
    override fun play(sound: TrekSound) {
        played.add(sound)
    }

    override fun startLoop(sound: TrekSound) = Unit
    override fun stopLoop() = Unit
    override fun release() = Unit
}

private class RecordingSpeaker : Speaker {
    val spoken = mutableListOf<Pair<String, TrekLanguage?>>()
    override fun speak(text: String, language: TrekLanguage?) {
        spoken.add(text to language)
    }

    override fun setLanguage(language: TrekLanguage) = Unit
    override fun shutdown() = Unit
}

private class FakeLauncher(private val canOpen: Boolean) : ExternalLauncher {
    val maps = mutableListOf<Pair<Double, Double>>()
    override fun openMap(latitude: Double, longitude: Double): Boolean {
        maps.add(latitude to longitude)
        return canOpen
    }

    override fun openUrl(url: String): Boolean = false
}

@OptIn(ExperimentalCoroutinesApi::class)
class MultiSensorStoreTest {
    private lateinit var dispatcher: TestDispatcher
    private val sensors = FakeSensorHub()
    private val sound = RecordingSound()
    private val speaker = RecordingSpeaker()
    private val listener = FakeSpeechListener()
    private val navigated = mutableListOf<Screen>()

    @BeforeTest
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun store(
        canOpenMap: Boolean = true,
        launcher: FakeLauncher = FakeLauncher(canOpenMap),
    ): MultiSensorStore {
        val prefs = mapOf(PrefKeys.SPEAK_LANGUAGE to "FR", PrefKeys.LISTEN_LANGUAGE to "FR")
        val settings = SettingsRepository(InMemoryKeyValueStore(prefs))
        val deps = MultiSensorDeps(sensors, sound, speaker, listener, settings, launcher)
        return MultiSensorStore(deps) { navigated.add(it) }
    }

    @Test
    fun initialStateMatchesTheLayoutTexts() = runTest(dispatcher) {
        val state = store().state.value
        assertEquals("Sensors", state.topStatus)
        assertEquals("Ready", state.bottomStatus)
        assertFalse(state.running)
        assertEquals(TrekLanguage.French, state.listenLanguage)
    }

    @Test
    fun resumeCollectsEverySensor() = runTest(dispatcher) {
        val store = store()
        store.dispatch(MultiSensorIntent.Resume)
        advanceUntilIdle()
        assertTrue(store.state.value.running)
        val samples = shortArrayOf(1, 2, 3)
        sensors.magneticFlow.emit(Vector3(1f, 2f, 3f))
        sensors.orientationFlow.emit(Vector3(90f))
        sensors.accelerationFlow.emit(Vector3(z = 9.8f))
        sensors.environmentFlow.emit(Environment(pressure = 1013f))
        sensors.locationFlow.emit(GeoLocation(45.5, -73.6))
        sensors.audioFlow.emit(samples)
        advanceUntilIdle()
        val state = store.state.value
        assertEquals(Vector3(1f, 2f, 3f), state.magnetic)
        assertEquals(90f, state.orientation.x)
        assertEquals(9.8f, state.acceleration.z)
        assertEquals(1013f, state.environment.pressure)
        assertNull(state.environment.temperature)
        assertEquals(GeoLocation(45.5, -73.6), state.location)
        assertSame(samples, state.audio.samples)
    }

    @Test
    fun pauseStopsEverySensor() = runTest(dispatcher) {
        val store = store()
        store.dispatch(MultiSensorIntent.Resume)
        advanceUntilIdle()
        assertEquals(1, sensors.magneticFlow.subscriptionCount.value)
        store.dispatch(MultiSensorIntent.Pause)
        advanceUntilIdle()
        assertFalse(store.state.value.running)
        assertEquals(0, sensors.magneticFlow.subscriptionCount.value)
        assertEquals(0, sensors.audioFlow.subscriptionCount.value)
        assertEquals(0, sensors.locationFlow.subscriptionCount.value)
    }

    @Test
    fun resumeTwiceDoesNotCollectTwice() = runTest(dispatcher) {
        val store = store()
        store.dispatch(MultiSensorIntent.Resume)
        store.dispatch(MultiSensorIntent.Resume)
        advanceUntilIdle()
        assertEquals(1, sensors.orientationFlow.subscriptionCount.value)
    }

    @Test
    fun backSwitchesToTheTrycorderMode() = runTest(dispatcher) {
        store().dispatch(MultiSensorIntent.Back)
        assertEquals(listOf(Screen.Trycorder), navigated)
        assertEquals(listOf(TrekSound.KeyOk), sound.played)
    }

    @Test
    fun settingsOpensTheSettingsScreen() = runTest(dispatcher) {
        val store = store()
        store.dispatch(MultiSensorIntent.Settings)
        assertEquals(listOf(Screen.Settings), navigated)
        assertEquals("Settings", store.state.value.bottomStatus)
    }

    @Test
    fun sendOnlyBeeps() = runTest(dispatcher) {
        val store = store()
        store.dispatch(MultiSensorIntent.Send)
        assertEquals(listOf(TrekSound.KeyOk), sound.played)
        assertEquals("Ready", store.state.value.bottomStatus)
    }

    @Test
    fun speakListensAndAnswersKnownSentences() = runTest(dispatcher) {
        val store = store()
        advanceUntilIdle()
        store.dispatch(MultiSensorIntent.Speak)
        assertEquals(1, listener.listenCount)
        assertEquals("", store.state.value.topStatus)
        listener.emit(listOf("computer"))
        advanceUntilIdle()
        assertEquals("computer", store.state.value.topStatus)
        assertEquals("Said: computer", store.state.value.bottomStatus)
        val expected: List<Pair<String, TrekLanguage?>> = listOf("Faites votre requète" to TrekLanguage.French)
        assertEquals(expected, speaker.spoken)
    }

    @Test
    fun unknownSentencesAreOnlyShown() = runTest(dispatcher) {
        val store = store()
        advanceUntilIdle()
        store.dispatch(MultiSensorIntent.Speak)
        listener.emit(listOf("hello there"))
        advanceUntilIdle()
        assertEquals("Understood: hello there", store.state.value.bottomStatus)
        assertTrue(speaker.spoken.isEmpty())
    }

    @Test
    fun languageSentenceSwitchesLanguages() = runTest(dispatcher) {
        val store = store()
        advanceUntilIdle()
        store.dispatch(MultiSensorIntent.Speak)
        listener.emit(listOf("english please"))
        advanceUntilIdle()
        assertEquals(TrekLanguage.English, store.state.value.speakLanguage)
        assertEquals(TrekLanguage.English, store.state.value.listenLanguage)
        assertEquals(listOf<Pair<String, TrekLanguage?>>("english" to TrekLanguage.English), speaker.spoken)
    }

    @Test
    fun resultsForOtherScreensAreIgnored() = runTest(dispatcher) {
        val store = store()
        advanceUntilIdle()
        listener.emit(listOf("computer"))
        advanceUntilIdle()
        assertEquals("Sensors", store.state.value.topStatus)
        assertTrue(speaker.spoken.isEmpty())
    }

    @Test
    fun openMapUsesTheLastLocation() = runTest(dispatcher) {
        val launcher = FakeLauncher(canOpen = true)
        val store = store(launcher = launcher)
        store.dispatch(MultiSensorIntent.Resume)
        advanceUntilIdle()
        sensors.locationFlow.emit(GeoLocation(45.5, -73.5))
        advanceUntilIdle()
        store.dispatch(MultiSensorIntent.OpenMap)
        assertEquals(listOf(45.5 to -73.5), launcher.maps)
        assertEquals("geo:45.5,-73.5", store.state.value.bottomStatus)
        assertEquals(listOf(TrekSound.KeyOk), sound.played)
    }

    @Test
    fun openMapWithoutMappingAppSaysSo() = runTest(dispatcher) {
        val store = store(canOpenMap = false)
        store.dispatch(MultiSensorIntent.OpenMap)
        assertEquals("No application for mapping.", store.state.value.bottomStatus)
    }
}
