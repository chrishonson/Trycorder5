package net.ddns.mlsoftlaberge.trycorder.desktop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.link.LinkEvent
import net.ddns.mlsoftlaberge.trycorder.voice.VoiceCommands

/**
 * The walkie-talkie console logic shared by [DesktopStore] and the client screen store:
 * speak to send a sentence, push to talk, scan the local Trycorders, start and stop the link,
 * and show what the link receives. [onBack] is what the corners and the Back button do.
 */
open class WalkieStore(
    private val deps: WalkieDeps,
    title: String,
    private val onBack: () -> Unit,
    private val onSettings: () -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(
        WalkieState(
            topStatus = title,
            peersText = deps.systemInfo.ipAddress(),
            listenLanguage = deps.settings.current.listenLanguage,
            speakLanguage = deps.settings.current.speakLanguage,
        ),
    )
    val state: StateFlow<WalkieState> = _state.asStateFlow()

    private var visible: Job? = null

    fun dispatch(intent: WalkieIntent) {
        when (intent) {
            is WalkieIntent.Lifecycle -> onLifecycle(intent)
            is WalkieIntent.Bar -> onBar(intent)
            is WalkieIntent.Comm -> onComm(intent)
        }
    }

    private fun onLifecycle(intent: WalkieIntent.Lifecycle) {
        when (intent) {
            WalkieIntent.Start -> start()
            WalkieIntent.Stop -> visible?.cancel().also { visible = null }
        }
    }

    private fun onBar(intent: WalkieIntent.Bar) {
        buttonSound()
        when (intent) {
            WalkieIntent.Back -> onBack()
            WalkieIntent.Settings -> {
                say("Settings")
                if (deps.settings.current.isChatty) deps.speaker.speak("Settings")
                onSettings()
            }
            WalkieIntent.View, WalkieIntent.Send, WalkieIntent.CommMode -> Unit
        }
    }

    private fun onComm(intent: WalkieIntent.Comm) {
        when (intent) {
            WalkieIntent.Speak -> listen()
            WalkieIntent.TalkDown -> say("start streaming audio").also { deps.link.startAudioStream() }
            WalkieIntent.TalkUp -> say("stop streaming audio").also { deps.link.stopAudioStream() }
            WalkieIntent.Scan -> buttonSound().also { askScanList() }
            WalkieIntent.ServerOn -> {
                buttonSound()
                say("Start Trycorder Service")
                deps.link.start()
                askScanList()
            }
            WalkieIntent.ServerOff -> {
                buttonSound()
                say("Stop Trycorder Service")
                deps.link.stop()
            }
        }
    }

    /** onStart()/onResume(): reload the languages, follow the link and the recognizer while shown. */
    private fun start() {
        if (visible != null) return
        val settings = deps.settings.current
        _state.update { it.copy(listenLanguage = settings.listenLanguage, speakLanguage = settings.speakLanguage) }
        askScanList()
        visible = viewModelScope.launch {
            launch { deps.listener.results.collect { VoiceCommands.pickCandidate(it)?.let(::understood) } }
            launch { deps.link.events.collect(::onLinkEvent) }
        }
    }

    private fun onLinkEvent(event: LinkEvent) {
        when (event) {
            is LinkEvent.PeersChanged -> askScanList()
            is LinkEvent.Text -> displayText(event.text)
            is LinkEvent.Status -> say(event.text)
            is LinkEvent.StatsChanged, is LinkEvent.Logs -> Unit
        }
    }

    private fun listen() {
        _state.update { it.copy(topStatus = "") }
        deps.listener.listen(_state.value.listenLanguage)
    }

    /** understood(): a sentence from the microphone is shown and sent to the other Trycorders. */
    private fun understood(text: String) {
        _state.update { it.copy(topStatus = text) }
        say(if (matchVoice(text)) "Said: $text" else "Understood: $text")
        say("Send: $text")
        deps.link.send(text)
        if (deps.settings.current.autoListen) listen()
    }

    /** displaytext(): a line received from the link. The link already spoke it. */
    private fun displayText(text: String) {
        _state.update { it.copy(topStatus = text) }
        say("Received: $text")
        matchVoice(text)
    }

    /** matchvoice() of these screens: only the language switch. */
    private fun matchVoice(text: String): Boolean {
        val lower = text.lowercase()
        val language = when {
            "french" in lower || "français" in lower -> TrekLanguage.French
            "english" in lower || "anglais" in lower -> TrekLanguage.English
            else -> return false
        }
        _state.update { it.copy(listenLanguage = language, speakLanguage = language) }
        deps.speaker.speak(if (language == TrekLanguage.French) "français" else "english", language)
        return true
    }

    private fun askScanList() {
        _state.update { it.copy(peersText = WalkieFormat.peers(deps.link.peers.value, deps.systemInfo.ipAddress())) }
    }

    private fun say(text: String) {
        _state.update { it.copy(bottomStatus = text, consoleLog = WalkieFormat.prepend(it.consoleLog, text)) }
    }

    private fun buttonSound() = deps.sound.play(TrekSound.KeyOk)
}
