package net.ddns.mlsoftlaberge.trycorder.multisensor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.voice.VoiceCommands

/**
 * Store of the MultiSensor screen (the old TrysensorFragment): magnetic, orientation, gravity,
 * temperature and audio sensors all running while the screen is resumed.
 * [navigate] switches mode like onTrysensorModeChange or opens the settings.
 */
class MultiSensorStore(
    private val deps: MultiSensorDeps,
    private val navigate: (Screen) -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<MultiSensorState> = _state.asStateFlow()

    private var sensorJobs: List<Job> = emptyList()

    /** Only answer the utterances this screen asked for: other screens share the recognizer. */
    private var awaitingSpeech = false

    init {
        viewModelScope.launch {
            deps.listener.results.collect { candidates ->
                if (awaitingSpeech) {
                    awaitingSpeech = false
                    VoiceCommands.pickCandidate(candidates)?.let { understood(it) }
                }
            }
        }
    }

    fun dispatch(intent: MultiSensorIntent) {
        when (intent) {
            MultiSensorIntent.Resume -> startSensors()
            MultiSensorIntent.Pause -> stopSensors()
            MultiSensorIntent.Back -> {
                buttonSound()
                navigate(Screen.Trycorder)
            }
            MultiSensorIntent.Settings -> {
                buttonSound()
                say("Settings")
                navigate(Screen.Settings)
            }
            MultiSensorIntent.Speak -> listen()
            MultiSensorIntent.Send -> buttonSound()
            MultiSensorIntent.OpenMap -> openMap()
        }
    }

    private fun initialState(): MultiSensorState {
        val settings = deps.settings.current
        return MultiSensorState(speakLanguage = settings.speakLanguage, listenLanguage = settings.listenLanguage)
    }

    private fun startSensors() {
        if (sensorJobs.isNotEmpty()) return
        val sensors = deps.sensors
        sensorJobs = listOf(
            sensors.magnetic().into { s, v -> s.copy(magnetic = v) },
            sensors.orientation().into { s, v -> s.copy(orientation = v) },
            sensors.acceleration().into { s, v -> s.copy(acceleration = v) },
            sensors.environment().into { s, v -> s.copy(environment = v) },
            sensors.location().into { s, v -> s.copy(location = v) },
            sensors.audio().into { s, v -> s.copy(audio = AudioChunk(v)) },
        )
        _state.update { it.copy(running = true) }
    }

    private fun stopSensors() {
        sensorJobs.forEach { it.cancel() }
        sensorJobs = emptyList()
        _state.update { it.copy(running = false) }
    }

    private fun <T> Flow<T>.into(reduce: (MultiSensorState, T) -> MultiSensorState): Job =
        onEach { value -> _state.update { reduce(it, value) } }.launchIn(viewModelScope)

    private fun listen() {
        _state.update { it.copy(topStatus = "") }
        awaitingSpeech = true
        deps.listener.listen(_state.value.listenLanguage)
    }

    /** understood(): show the sentence and answer it when matchvoice knows it. */
    private fun understood(text: String) {
        _state.update { it.copy(topStatus = text) }
        val reply = MultiSensorVoice.reply(text, _state.value.speakLanguage)
        if (reply == null) {
            say("Understood: $text")
            return
        }
        reply.switchTo?.let { lang -> _state.update { it.copy(speakLanguage = lang, listenLanguage = lang) } }
        deps.speaker.speak(reply.speech, reply.language)
        say("Said: $text")
    }

    /** googlemapactivity(): open the map at the last known position. */
    private fun openMap() {
        val location = _state.value.location
        val latitude = location?.latitude?.toFloat() ?: 0f
        val longitude = location?.longitude?.toFloat() ?: 0f
        say("Open planetary mapping")
        say("geo:$latitude,$longitude")
        if (!deps.launcher.openMap(latitude.toDouble(), longitude.toDouble())) say("No application for mapping.")
        buttonSound()
    }

    private fun say(text: String) = _state.update { it.copy(bottomStatus = text) }

    private fun buttonSound() = deps.sound.play(TrekSound.KeyOk)

    override fun onCleared() {
        stopSensors()
    }
}
