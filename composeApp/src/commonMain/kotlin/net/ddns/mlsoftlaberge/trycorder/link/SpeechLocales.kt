package net.ddns.mlsoftlaberge.trycorder.link

import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage

/** Recognizer settings shared by the Android and iOS speech listeners (the old listen()). */
object SpeechLocales {
    /** Number of candidate sentences asked from the recognizer (EXTRA_MAX_RESULTS). */
    const val MAX_RESULTS = 5

    /** BCP 47 tag of the listen language; null for Auto, which keeps the device default. */
    fun tag(language: TrekLanguage): String? = when (language) {
        TrekLanguage.French -> "fr-FR"
        TrekLanguage.English -> "en-US"
        TrekLanguage.Auto -> null
    }
}
