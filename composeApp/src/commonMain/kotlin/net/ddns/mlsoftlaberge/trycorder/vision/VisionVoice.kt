package net.ddns.mlsoftlaberge.trycorder.vision

import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage

/** What the vision screen answers to a sentence: the new language and what to say in which language. */
data class VoiceReply(val language: TrekLanguage, val speech: String, val speechLanguage: TrekLanguage?)

/** matchvoice() of TryvisionFragment. */
object VisionVoice {
    fun reply(sentence: String, language: TrekLanguage): VoiceReply? {
        val text = sentence.lowercase()
        val french = language == TrekLanguage.French
        fun answer(fr: String, en: String) = VoiceReply(language, if (french) fr else en, null)
        return when {
            "french" in text || "français" in text ->
                VoiceReply(TrekLanguage.French, "français", TrekLanguage.French)
            "english" in text || "anglais" in text ->
                VoiceReply(TrekLanguage.English, "english", TrekLanguage.English)
            "martin" in text || "master" in text -> answer("Martin est mon maître.", "Martin is my Master.")
            "computer" in text || "ordinateur" in text -> answer("Faites votre requète", "State your question")
            "fuck" in text || "shit" in text -> answer("Ce n'est pas très poli", "This is not very polite.")
            else -> null
        }
    }
}
