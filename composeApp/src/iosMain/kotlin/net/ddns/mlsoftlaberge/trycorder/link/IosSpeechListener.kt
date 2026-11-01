package net.ddns.mlsoftlaberge.trycorder.link

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import net.ddns.mlsoftlaberge.trycorder.core.SpeechListener
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage

/** Placeholder: replaced by the SFSpeechRecognizer implementation. */
class IosSpeechListener : SpeechListener {
    override val results: Flow<List<String>> = emptyFlow()
    override fun listen(language: TrekLanguage) = Unit
}
