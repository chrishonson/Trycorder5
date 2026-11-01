package net.ddns.mlsoftlaberge.trycorder.core

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** TextToSpeech, created on first use like the original initspeak(). */
class AndroidSpeaker(private val context: Context, private val settings: SettingsRepository) : Speaker {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = mutableListOf<Pair<String, TrekLanguage?>>()

    override fun speak(text: String, language: TrekLanguage?) {
        val engine = tts ?: init()
        if (!ready) {
            pending += text to language
            return
        }
        language?.let { setLanguage(it) }
        engine.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    override fun setLanguage(language: TrekLanguage) {
        when (language) {
            TrekLanguage.French -> tts?.language = Locale.FRENCH
            TrekLanguage.English -> tts?.language = Locale.US
            TrekLanguage.Auto -> Unit
        }
    }

    override fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun init(): TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status != TextToSpeech.ERROR) {
            ready = true
            setLanguage(settings.current.speakLanguage)
            pending.toList().forEach { (text, lang) -> speak(text, lang) }
            pending.clear()
        }
    }.also { tts = it }
}
