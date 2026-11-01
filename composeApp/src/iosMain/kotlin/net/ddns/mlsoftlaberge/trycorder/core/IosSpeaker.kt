package net.ddns.mlsoftlaberge.trycorder.core

import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

class IosSpeaker(private val settings: SettingsRepository) : Speaker {
    private val synthesizer = AVSpeechSynthesizer()
    private var language: TrekLanguage = settings.current.speakLanguage

    override fun speak(text: String, language: TrekLanguage?) {
        language?.let { setLanguage(it) }
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        voiceCode(this.language)?.let { utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(it) }
        synthesizer.speakUtterance(utterance)
    }

    override fun setLanguage(language: TrekLanguage) {
        this.language = language
    }

    override fun shutdown() {
        synthesizer.stopSpeakingAtBoundary(platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    private fun voiceCode(language: TrekLanguage): String? = when (language) {
        TrekLanguage.French -> "fr-FR"
        TrekLanguage.English -> "en-US"
        TrekLanguage.Auto -> null
    }
}
