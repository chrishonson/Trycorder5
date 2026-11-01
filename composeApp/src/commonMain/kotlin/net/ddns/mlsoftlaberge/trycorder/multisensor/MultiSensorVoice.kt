package net.ddns.mlsoftlaberge.trycorder.multisensor

import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage

/** What to say back, and the new speak/listen language when the sentence switched it. */
data class VoiceReply(val speech: String, val language: TrekLanguage, val switchTo: TrekLanguage? = null)

/** matchvoice() of TrysensorFragment: the few sentences this screen answers. */
object MultiSensorVoice {
    fun reply(heard: String, speakLanguage: TrekLanguage): VoiceReply? {
        val text = heard.lowercase()
        val french = speakLanguage == TrekLanguage.French
        fun say(fr: String, en: String) = VoiceReply(if (french) fr else en, speakLanguage)
        return when {
            text.contains("french") || text.contains("français") ->
                VoiceReply("français", TrekLanguage.French, TrekLanguage.French)
            text.contains("english") || text.contains("anglais") ->
                VoiceReply("english", TrekLanguage.English, TrekLanguage.English)
            text.contains("martin") || text.contains("master") -> say("Martin est mon maître.", "Martin is my Master.")
            text.contains("computer") || text.contains("ordinateur") ->
                say("Faites votre requète", "State your question")
            text.contains("fuck") || text.contains("shit") -> say("Ce n'est pas très poli", "This is not very polite.")
            else -> null
        }
    }
}
