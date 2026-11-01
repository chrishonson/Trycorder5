package net.ddns.mlsoftlaberge.trycorder.trycorder

import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.link.LinkEvent
import net.ddns.mlsoftlaberge.trycorder.voice.VoiceCommands
import net.ddns.mlsoftlaberge.trycorder.voice.VoiceMatch

/** Sentences heard by the microphone or received from the network (understood/matchvoice/displaytext). */
internal class VoiceActions(
    private val ctx: TrycorderContext,
    private val sensors: SensorActions,
    private val viewer: ViewerActions,
    private val commands: CommandActions,
) {
    fun listen() {
        ctx.update { it.copy(topStatus = "") }
        ctx.services.listener.listen(ctx.state.listenLanguage)
    }

    /** understood(): the best sentence of an utterance. */
    fun understood(candidates: List<String>) {
        val text = VoiceCommands.pickCandidate(candidates) ?: return
        ctx.update { it.copy(topStatus = text) }
        if (match(text)) {
            ctx.say("Said: $text")
            ctx.sendText(text)
            if (ctx.settings.autoListen) listen()
        } else {
            ctx.say("Understood: $text")
            ctx.sendText(text)
            ctx.speak(text)
        }
    }

    fun onLinkEvent(event: LinkEvent) {
        when (event) {
            is LinkEvent.PeersChanged -> ctx.update { it.copy(peersText = PeerFormat.format(event.peers)) }
            is LinkEvent.StatsChanged -> with(event.stats) {
                ctx.sayChat("Trycorders=$trycorders")
                ctx.sayChat("Countrys=$countries")
                ctx.sayChat("States=$states")
                ctx.sayChat("Citys=$cities")
            }
            is LinkEvent.Logs -> {
                ctx.update { it.copy(topStatus = event.text) }
                ctx.say("Logs: ${event.text}")
                ctx.sayChat(event.text)
            }
            is LinkEvent.Text -> {
                ctx.update { it.copy(topStatus = event.text) }
                ctx.say("Received: ${event.text}")
                if (!match(event.text)) ctx.sayChat(event.text)
            }
            is LinkEvent.Status -> Unit
        }
    }

    /** matchvoice(): act on a sentence, true when it was a Trycorder command. */
    private fun match(text: String): Boolean {
        val match = VoiceCommands.parse(text) ?: return false
        when (match) {
            is VoiceMatch.SwitchLanguage -> {
                ctx.update { it.copy(speakLanguage = match.language, listenLanguage = match.language) }
                ctx.speak(if (match.language == TrekLanguage.French) "français" else "english", match.language)
            }
            VoiceMatch.Rude -> {
                if (ctx.state.speakLanguage == TrekLanguage.French) {
                    ctx.speak("Ne m'adressez pas la parole de cette façon")
                } else {
                    ctx.play(TrekSound.DoNotAddress)
                }
                viewer.show(ViewerPage.FederationLogo)
                sensors.showPage(SensorPage.EarthStill)
                viewer.showGroup(ButtonGroup.None)
            }
            is VoiceMatch.Command -> commands.execute(match.command, fromVoice = true)
        }
        return true
    }
}
