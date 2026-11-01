package net.ddns.mlsoftlaberge.trycorder.settings

import net.ddns.mlsoftlaberge.trycorder.core.PrefKeys
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.core.TrycorderSettings

/** One entry of res/xml/preferences.xml. */
sealed interface Preference {
    val key: String
    val title: String
    val summary: String

    /** CheckBoxPreference. */
    data class Check(override val key: String, override val title: String, override val summary: String) : Preference

    /** ListPreference over language_array_entries / language_array_entryvalues. */
    data class LanguageChoice(
        override val key: String,
        override val title: String,
        override val summary: String,
    ) : Preference

    /** EditTextPreference. */
    data class Text(override val key: String, override val title: String, override val summary: String) : Preference
}

/** The preferences of the original screen, in the original order, and how they map to [TrycorderSettings]. */
object SettingsCatalog {
    /** The PreferenceCategory title. */
    const val CATEGORY = "Trycorder"

    val all: List<Preference> = listOf(
        Preference.Check(PrefKeys.AUTO_LISTEN, "Automatic loop", "Determine if the listen function is looping"),
        Preference.Check(PrefKeys.IS_CHATTY, "Speak more often", "Determine if the speak function is chatty"),
        Preference.LanguageChoice(
            PrefKeys.DISPLAY_LANGUAGE,
            "Display Language",
            "Language used to display to the user",
        ),
        Preference.LanguageChoice(PrefKeys.LISTEN_LANGUAGE, "Listen Language", "Language used to listen to the user"),
        Preference.LanguageChoice(
            PrefKeys.SPEAK_LANGUAGE,
            "Speak Language",
            "Language used to pronounce text to speak",
        ),
        Preference.Text(PrefKeys.DEVICE_NAME, "Device Name", "Device name used in walkie-talkie"),
        Preference.Check(
            PrefKeys.IS_MASTER,
            "Is Master",
            "Determine if we are master of commands with the others trycorders",
        ),
        Preference.Check(PrefKeys.SEND_LOCAL, "Send Local", "Determine if we send to Local Net"),
        Preference.Check(PrefKeys.SEND_REMOTE, "Send Remote", "Determine if we send to Remote Net"),
        Preference.Check(PrefKeys.EXEC_LOCAL, "Exec Local", "Determine if we exec from Local Net"),
        Preference.Check(PrefKeys.EXEC_REMOTE, "Exec Remote", "Determine if we exec from Remote Net"),
        Preference.Check(PrefKeys.REPLAY_SENT, "Replay Sent", "Determine if we replay the sent text locally"),
        Preference.Check(PrefKeys.AUTO_BOOT, "Auto Boot", "Determine if we start talk server at boot"),
        Preference.Check(PrefKeys.AUTO_STOP, "Auto Stop", "Determine if we stop the server at exit"),
        Preference.Check(PrefKeys.DEBUG_MODE, "Debug Mode", "Enable to debug locally"),
        Preference.Text(PrefKeys.DEBUG_ADDR, "Debug Server Address", "Debug server address"),
    )

    /** language_array_entries, in the original order. */
    val languages: List<TrekLanguage> = listOf(TrekLanguage.French, TrekLanguage.English, TrekLanguage.Auto)

    fun find(key: String): Preference? = all.firstOrNull { it.key == key }
}

/** Reads and writes one checkbox of [TrycorderSettings] by preference key. */
object CheckFields {
    fun get(s: TrycorderSettings, key: String): Boolean = when (key) {
        PrefKeys.AUTO_LISTEN -> s.autoListen
        PrefKeys.IS_CHATTY -> s.isChatty
        PrefKeys.IS_MASTER -> s.isMaster
        PrefKeys.SEND_LOCAL -> s.sendLocal
        PrefKeys.SEND_REMOTE -> s.sendRemote
        PrefKeys.EXEC_LOCAL -> s.execLocal
        PrefKeys.EXEC_REMOTE -> s.execRemote
        PrefKeys.REPLAY_SENT -> s.replaySent
        PrefKeys.AUTO_BOOT -> s.autoBoot
        PrefKeys.AUTO_STOP -> s.autoStop
        PrefKeys.DEBUG_MODE -> s.debugMode
        else -> false
    }

    fun set(s: TrycorderSettings, key: String, v: Boolean): TrycorderSettings = when (key) {
        PrefKeys.AUTO_LISTEN -> s.copy(autoListen = v)
        PrefKeys.IS_CHATTY -> s.copy(isChatty = v)
        PrefKeys.IS_MASTER -> s.copy(isMaster = v)
        PrefKeys.SEND_LOCAL -> s.copy(sendLocal = v)
        PrefKeys.SEND_REMOTE -> s.copy(sendRemote = v)
        PrefKeys.EXEC_LOCAL -> s.copy(execLocal = v)
        PrefKeys.EXEC_REMOTE -> s.copy(execRemote = v)
        PrefKeys.REPLAY_SENT -> s.copy(replaySent = v)
        PrefKeys.AUTO_BOOT -> s.copy(autoBoot = v)
        PrefKeys.AUTO_STOP -> s.copy(autoStop = v)
        PrefKeys.DEBUG_MODE -> s.copy(debugMode = v)
        else -> s
    }
}

/** Reads and writes the list and text preferences of [TrycorderSettings] by preference key. */
object ValueFields {
    fun language(s: TrycorderSettings, key: String): TrekLanguage = when (key) {
        PrefKeys.DISPLAY_LANGUAGE -> s.displayLanguage
        PrefKeys.LISTEN_LANGUAGE -> s.listenLanguage
        else -> s.speakLanguage
    }

    fun withLanguage(s: TrycorderSettings, key: String, v: TrekLanguage): TrycorderSettings = when (key) {
        PrefKeys.DISPLAY_LANGUAGE -> s.copy(displayLanguage = v)
        PrefKeys.LISTEN_LANGUAGE -> s.copy(listenLanguage = v)
        PrefKeys.SPEAK_LANGUAGE -> s.copy(speakLanguage = v)
        else -> s
    }

    fun text(s: TrycorderSettings, key: String): String = if (key == PrefKeys.DEBUG_ADDR) s.debugAddr else s.deviceName

    fun withText(s: TrycorderSettings, key: String, v: String): TrycorderSettings = when (key) {
        PrefKeys.DEVICE_NAME -> s.copy(deviceName = v)
        PrefKeys.DEBUG_ADDR -> s.copy(debugAddr = v)
        else -> s
    }
}
