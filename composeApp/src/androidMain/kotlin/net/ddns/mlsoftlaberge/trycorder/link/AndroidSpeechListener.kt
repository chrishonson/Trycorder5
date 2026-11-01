package net.ddns.mlsoftlaberge.trycorder.link

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.ddns.mlsoftlaberge.trycorder.core.SpeechListener
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage

/**
 * SpeechRecognizer port of the service's listen(): free form model, up to 5 candidates,
 * fr-FR / en-US or the device default for Auto. The recognizer lives on the main thread.
 * Errors emit nothing, like the original empty onError().
 */
class AndroidSpeechListener(context: Context) : SpeechListener {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val flow = MutableSharedFlow<List<String>>(extraBufferCapacity = RESULT_BUFFER)
    private var recognizer: SpeechRecognizer? = null

    override val results: SharedFlow<List<String>> = flow.asSharedFlow()

    override fun listen(language: TrekLanguage) {
        main.post { startListening(language) }
    }

    private fun startListening(language: TrekLanguage) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) return
        val current = recognizer ?: SpeechRecognizer.createSpeechRecognizer(appContext).also {
            it.setRecognitionListener(Listener())
            recognizer = it
        }
        // A new request replaces a pending one instead of failing with ERROR_RECOGNIZER_BUSY.
        current.cancel()
        current.startListening(recognizerIntent(language))
    }

    private fun recognizerIntent(language: TrekLanguage) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, SpeechLocales.MAX_RESULTS)
        SpeechLocales.tag(language)?.let { putExtra(RecognizerIntent.EXTRA_LANGUAGE, it) }
    }

    private inner class Listener : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (candidates.isNotEmpty()) flow.tryEmit(candidates.toList())
        }

        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onError(error: Int) = Unit
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private companion object {
        const val RESULT_BUFFER = 8
    }
}
