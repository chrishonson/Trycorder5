package net.ddns.mlsoftlaberge.trycorder.trycorder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.core.TrycorderSettings

/**
 * Shared state and side effect helpers of the Trycorder store, used by the action classes
 * (the old fragment's say/speak/playsound/sendtext helpers).
 */
internal class TrycorderContext(
    val services: PlatformServices,
    val scope: CoroutineScope,
    private val mutableState: MutableStateFlow<TrycorderState>,
) {
    val state: TrycorderState get() = mutableState.value
    val settings: TrycorderSettings get() = services.settings.current

    fun update(transform: (TrycorderState) -> TrycorderState) = mutableState.update(transform)

    /** Bottom status line and console log. */
    fun say(text: String) = update {
        it.copy(bottomStatus = text, consoleLog = prepend(it.consoleLog, text))
    }

    fun sayChat(text: String) = update { it.copy(chatLog = prepend(it.chatLog, text)) }

    fun sayCommand(text: String) = update { it.copy(commandLog = prepend(it.commandLog, text)) }

    fun speak(text: String, language: TrekLanguage? = null) = services.speaker.speak(text, language)

    fun speakIfChatty(text: String) {
        if (settings.isChatty) speak(text)
    }

    fun play(sound: TrekSound) = services.sound.play(sound)

    fun buttonSound() = play(TrekSound.KeyOk)

    fun buttonBad() = play(TrekSound.Deny)

    /** Send a line to the other Trycorders and the server. */
    fun sendText(text: String) {
        say("Send: $text")
        services.link.send(text)
    }

    /** Echo a button action to the other Trycorders when we are master. */
    fun sendCommand(text: String) {
        if (settings.isMaster) {
            sayCommand(text)
            services.link.send("computer $text")
        }
    }

    companion object {
        /** Newest line first, capped like the original 1000 char StringBuffers. */
        fun prepend(log: String, line: String): String =
            "$line\n$log".take(TrycorderState.LOG_LIMIT)
    }
}
