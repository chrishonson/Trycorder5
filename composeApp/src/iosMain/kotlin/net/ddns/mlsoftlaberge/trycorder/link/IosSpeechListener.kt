package net.ddns.mlsoftlaberge.trycorder.link

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.core.SpeechListener
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognitionResult
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import platform.Speech.SFTranscription

/**
 * SFSpeechRecognizer + AVAudioEngine listener. After authorization it records one utterance; the
 * utterance ends after a short silence (no new partial result), then the best transcriptions are emitted.
 * Errors and refused authorization emit nothing, like the Android listener.
 */
class IosSpeechListener : SpeechListener {
    private val flow = MutableSharedFlow<List<String>>(extraBufferCapacity = RESULT_BUFFER)
    private val scope = MainScope()
    private var session: RecognitionSession? = null

    override val results: SharedFlow<List<String>> = flow.asSharedFlow()

    override fun listen(language: TrekLanguage) {
        SFSpeechRecognizer.requestAuthorization { status ->
            if (status == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
                scope.launch { start(language) }
            }
        }
    }

    private fun start(language: TrekLanguage) {
        session?.cancel()
        val recognizer = SpeechLocales.tag(language)
            ?.let { SFSpeechRecognizer(locale = NSLocale(localeIdentifier = it)) }
            ?: SFSpeechRecognizer()
        if (!recognizer.available) return
        session = RecognitionSession(recognizer, scope) { candidates -> flow.tryEmit(candidates) }
            .takeIf { it.begin() }
    }

    private companion object {
        const val RESULT_BUFFER = 8
    }
}

/** One utterance: microphone tap feeding a buffer request, ended by silence or a maximum duration. */
@OptIn(ExperimentalForeignApi::class)
private class RecognitionSession(
    private val recognizer: SFSpeechRecognizer,
    private val scope: CoroutineScope,
    private val onCandidates: (List<String>) -> Unit,
) {
    private val audio = AVAudioEngine()
    private val request = SFSpeechAudioBufferRecognitionRequest().apply { shouldReportPartialResults = true }
    private var task: SFSpeechRecognitionTask? = null
    private var endTimer: Job? = null
    private var finished = false

    fun begin(): Boolean {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker,
            error = null,
        )
        session.setActive(true, error = null)
        val input = audio.inputNode
        input.installTapOnBus(0u, bufferSize = TAP_FRAMES, format = input.outputFormatForBus(0u)) { buffer, _ ->
            buffer?.let { request.appendAudioPCMBuffer(it) }
        }
        audio.prepare()
        if (!audio.startAndReturnError(null)) {
            input.removeTapOnBus(0u)
            return false
        }
        task = recognizer.recognitionTaskWithRequest(request) { result, error ->
            scope.launch { onUpdate(result, error) }
        }
        endAfter(MAX_UTTERANCE_MILLIS)
        return true
    }

    fun cancel() {
        finished = true
        endTimer?.cancel()
        task?.cancel()
        stopAudio()
    }

    private fun onUpdate(result: SFSpeechRecognitionResult?, error: NSError?) {
        if (finished) return
        when {
            result?.final == true -> finish(candidates(result))
            error != null -> finish(emptyList())
            result != null -> endAfter(SILENCE_MILLIS)
        }
    }

    /** The utterance ends when no new words arrive for [millis]: stop the audio and wait for the final result. */
    private fun endAfter(millis: Long) {
        endTimer?.cancel()
        endTimer = scope.launch {
            delay(millis)
            stopAudio()
            request.endAudio()
        }
    }

    private fun finish(candidates: List<String>) {
        finished = true
        endTimer?.cancel()
        stopAudio()
        if (candidates.isNotEmpty()) onCandidates(candidates)
    }

    private fun stopAudio() {
        if (audio.running) audio.stop()
        audio.inputNode.removeTapOnBus(0u)
    }

    private fun candidates(result: SFSpeechRecognitionResult): List<String> {
        val all = listOf(result.bestTranscription) + result.transcriptions.filterIsInstance<SFTranscription>()
        return all.map { it.formattedString }.filter { it.isNotBlank() }.distinct().take(SpeechLocales.MAX_RESULTS)
    }

    private companion object {
        const val TAP_FRAMES = 1024u
        const val SILENCE_MILLIS = 1_500L
        const val MAX_UTTERANCE_MILLIS = 10_000L
    }
}
