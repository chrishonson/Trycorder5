package net.ddns.mlsoftlaberge.trycorder.link

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import net.ddns.mlsoftlaberge.trycorder.core.SpeechListener
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage

/** Placeholder: replaced by the SpeechRecognizer implementation. */
class AndroidSpeechListener(private val context: Context) : SpeechListener {
    override val results: Flow<List<String>> = emptyFlow()
    override fun listen(language: TrekLanguage) = Unit
    override fun toString() = "AndroidSpeechListener(${context.packageName})"
}
