package net.ddns.mlsoftlaberge.trycorder.vision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.camera.CameraController
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.Speaker
import net.ddns.mlsoftlaberge.trycorder.core.SpeechListener
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.link.StarshipLink
import net.ddns.mlsoftlaberge.trycorder.voice.VoiceCommands

/** The services the vision screens use. */
class VisionServices(
    val camera: CameraController,
    val sound: SoundPlayer,
    val speaker: Speaker,
    val listener: SpeechListener,
    val link: StarshipLink,
)

/**
 * Store of the vision screens (TryvisionFragment and TryviscamFragment).
 * [navigate] is onTryvisionModeChange / onTryviscamModeChange, or opens the settings.
 */
class VisionStore(
    day: Boolean,
    private val services: VisionServices,
    private val navigate: (Screen) -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(VisionState(day = day))
    val state: StateFlow<VisionState> = _state.asStateFlow()

    /** Only the sentences asked for with the Speak button are for this screen. */
    private var listening = false

    init {
        viewModelScope.launch {
            services.listener.results.collect { candidates ->
                if (listening) {
                    listening = false
                    VoiceCommands.pickCandidate(candidates)?.let { understood(it) }
                }
            }
        }
    }

    fun dispatch(intent: VisionIntent) {
        when (intent) {
            VisionIntent.Load -> load()
            VisionIntent.Back -> buttonSound().also { navigate(Screen.Trycorder) }
            VisionIntent.Settings -> {
                buttonSound()
                say("Settings")
                navigate(Screen.Settings)
            }
            VisionIntent.ViewerTap -> buttonSound().also { if (state.value.day) takePicture() else nextEffect() }
            else -> onButton(intent)
        }
    }

    private fun onButton(intent: VisionIntent) {
        when (intent) {
            VisionIntent.Speak -> listen()
            VisionIntent.TalkDown -> talk(true)
            VisionIntent.TalkUp -> talk(false)
            VisionIntent.NextEffect -> buttonSound().also { nextEffect() }
            VisionIntent.NextScene -> buttonSound().also { nextScene() }
            else -> Unit
        }
    }

    private fun load() {
        if (state.value.talking) services.link.stopAudioStream()
        listening = false
        _state.value = VisionState(day = state.value.day)
        if (state.value.day) sayMode()
    }

    /** rotatecam() / rotateeffect(): modes 1..9 then back to 1. */
    private fun nextEffect() {
        _state.update { it.copy(effectIndex = (it.effectIndex + 1) % VisionModes.effects(it.day).size) }
        if (state.value.day) sayMode()
    }

    /** rotatescene(): Portrait, Night Portrait. */
    private fun nextScene() {
        _state.update { it.copy(sceneIndex = (it.sceneIndex + 1) % VisionModes.SCENES.size) }
        sayMode()
    }

    private fun sayMode() {
        val s = state.value
        say(VisionModes.describe(s.effect, s.scene ?: VisionModes.SCENES.first()))
    }

    /** takePicture() of the Camera2 fragment. */
    private fun takePicture() {
        val camera = services.camera
        if (!camera.isActive) {
            services.sound.play(TrekSound.Deny)
            say("Something bad happened with vision")
            return
        }
        viewModelScope.launch {
            val result = camera.snap()
            if (result == null) {
                services.sound.play(TrekSound.Deny)
                say("Something bad happened with vision")
            } else {
                say("Picture saved")
            }
        }
    }

    /** listen(): the Speak button. */
    private fun listen() {
        _state.update { it.copy(topStatus = "") }
        listening = true
        services.listener.listen(state.value.language)
    }

    /** understood(): the recognized sentence goes in the top status line. */
    private fun understood(text: String) {
        _state.update { it.copy(topStatus = text) }
        val reply = VisionVoice.reply(text, state.value.language)
        if (reply == null) {
            say("Understood: $text")
            return
        }
        _state.update { it.copy(language = reply.language) }
        services.speaker.speak(reply.speech, reply.speechLanguage)
        say("Said: $text")
    }

    /** The push-to-talk Talk button: stream the microphone to the other Trycorders. */
    private fun talk(down: Boolean) {
        if (down == state.value.talking) return
        _state.update { it.copy(talking = down) }
        if (down) {
            say("start streaming audio")
            services.link.startAudioStream()
        } else {
            say("stop streaming audio")
            services.link.stopAudioStream()
        }
    }

    private fun buttonSound() = services.sound.play(TrekSound.KeyOk)

    private fun say(text: String) = _state.update { it.copy(bottomStatus = text) }

    override fun onCleared() {
        if (state.value.talking) services.link.stopAudioStream()
    }
}
