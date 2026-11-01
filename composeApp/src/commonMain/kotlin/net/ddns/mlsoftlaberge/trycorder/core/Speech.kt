package net.ddns.mlsoftlaberge.trycorder.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.emptyFlow

/** Text to speech. Android: TextToSpeech. iOS: AVSpeechSynthesizer. */
interface Speaker {
    fun speak(text: String, language: TrekLanguage? = null)
    fun setLanguage(language: TrekLanguage)
    fun shutdown()
}

/** Speech to text. Android: SpeechRecognizer. iOS: SFSpeechRecognizer. */
interface SpeechListener {
    /** Each emission is the list of candidate sentences for one utterance, best first. */
    val results: Flow<List<String>>

    fun listen(language: TrekLanguage)
}

object SilentSpeaker : Speaker {
    override fun speak(text: String, language: TrekLanguage?) = Unit
    override fun setLanguage(language: TrekLanguage) = Unit
    override fun shutdown() = Unit
}

/** Listener for tests: [emit] simulates a recognized utterance. */
class FakeSpeechListener : SpeechListener {
    private val flow = MutableSharedFlow<List<String>>(extraBufferCapacity = 8)
    var listenCount = 0
        private set
    override val results: SharedFlow<List<String>> = flow

    override fun listen(language: TrekLanguage) {
        listenCount++
    }

    fun emit(candidates: List<String>) {
        flow.tryEmit(candidates)
    }
}

object DeafSpeechListener : SpeechListener {
    override val results: Flow<List<String>> = emptyFlow()
    override fun listen(language: TrekLanguage) = Unit
}
