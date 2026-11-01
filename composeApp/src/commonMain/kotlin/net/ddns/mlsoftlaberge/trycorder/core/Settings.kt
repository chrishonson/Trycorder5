package net.ddns.mlsoftlaberge.trycorder.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Language codes used by the original preferences ("FR", "EN", "Auto"). */
enum class TrekLanguage(val code: String, val label: String) {
    French("FR", "French"),
    English("EN", "English"),
    Auto("Auto", "Auto");

    companion object {
        fun fromCode(code: String): TrekLanguage = entries.firstOrNull { it.code == code } ?: English
    }
}

/** The user settings of res/xml/preferences.xml. Keys are kept so existing installs keep their values. */
data class TrycorderSettings(
    val autoListen: Boolean = false,
    val isChatty: Boolean = false,
    val displayLanguage: TrekLanguage = TrekLanguage.English,
    val listenLanguage: TrekLanguage = TrekLanguage.English,
    val speakLanguage: TrekLanguage = TrekLanguage.English,
    val deviceName: String = DEFAULT_DEVICE_NAME,
    val isMaster: Boolean = true,
    val sendLocal: Boolean = true,
    val sendRemote: Boolean = true,
    val execLocal: Boolean = true,
    val execRemote: Boolean = true,
    val replaySent: Boolean = false,
    val autoBoot: Boolean = false,
    val autoStop: Boolean = true,
    val debugMode: Boolean = false,
    val debugAddr: String = DEFAULT_DEBUG_ADDR,
) {
    /** Host of the tryserver the starship link talks to. */
    val serverHost: String get() = if (debugMode) debugAddr else PUBLIC_SERVER

    companion object {
        const val DEFAULT_DEVICE_NAME = "Trycorder"
        const val DEFAULT_DEBUG_ADDR = "192.168.0.184"
        const val PUBLIC_SERVER = "mlsoftlaberge.ddns.net"

        /** Network Common Channel - NCC-1701. */
        const val SERVER_PORT = 1701
    }
}

object PrefKeys {
    const val AUTO_LISTEN = "pref_key_auto_listen"
    const val IS_CHATTY = "pref_key_ischatty"
    const val DISPLAY_LANGUAGE = "pref_key_display_language"
    const val LISTEN_LANGUAGE = "pref_key_listen_language"
    const val SPEAK_LANGUAGE = "pref_key_speak_language"
    const val DEVICE_NAME = "pref_key_device_name"
    const val IS_MASTER = "pref_key_ismaster"
    const val SEND_LOCAL = "pref_key_send_local"
    const val SEND_REMOTE = "pref_key_send_remote"
    const val EXEC_LOCAL = "pref_key_exec_local"
    const val EXEC_REMOTE = "pref_key_exec_remote"
    const val REPLAY_SENT = "pref_key_replay_sent"
    const val AUTO_BOOT = "pref_key_auto_boot"
    const val AUTO_STOP = "pref_key_auto_stop"
    const val DEBUG_MODE = "pref_key_debug_mode"
    const val DEBUG_ADDR = "pref_key_debug_addr"
    const val LAST_MODE = "pref_key_last_mode"
}

/** Reads and writes [TrycorderSettings], and publishes changes to every screen. */
class SettingsRepository(private val store: KeyValueStore, systemDeviceName: () -> String = { "" }) {
    private val _settings = MutableStateFlow(load(systemDeviceName))
    val settings: StateFlow<TrycorderSettings> = _settings.asStateFlow()

    val current: TrycorderSettings get() = _settings.value

    fun update(transform: (TrycorderSettings) -> TrycorderSettings) {
        _settings.update(transform)
        save(_settings.value)
    }

    private fun load(systemDeviceName: () -> String): TrycorderSettings {
        val d = TrycorderSettings()
        val loaded = TrycorderSettings(
            autoListen = store.getBoolean(PrefKeys.AUTO_LISTEN, d.autoListen),
            isChatty = store.getBoolean(PrefKeys.IS_CHATTY, d.isChatty),
            displayLanguage = TrekLanguage.fromCode(store.getString(PrefKeys.DISPLAY_LANGUAGE, d.displayLanguage.code)),
            listenLanguage = TrekLanguage.fromCode(store.getString(PrefKeys.LISTEN_LANGUAGE, d.listenLanguage.code)),
            speakLanguage = TrekLanguage.fromCode(store.getString(PrefKeys.SPEAK_LANGUAGE, d.speakLanguage.code)),
            deviceName = store.getString(PrefKeys.DEVICE_NAME, d.deviceName),
            isMaster = store.getBoolean(PrefKeys.IS_MASTER, d.isMaster),
            sendLocal = store.getBoolean(PrefKeys.SEND_LOCAL, d.sendLocal),
            sendRemote = store.getBoolean(PrefKeys.SEND_REMOTE, d.sendRemote),
            execLocal = store.getBoolean(PrefKeys.EXEC_LOCAL, d.execLocal),
            execRemote = store.getBoolean(PrefKeys.EXEC_REMOTE, d.execRemote),
            replaySent = store.getBoolean(PrefKeys.REPLAY_SENT, d.replaySent),
            autoBoot = store.getBoolean(PrefKeys.AUTO_BOOT, d.autoBoot),
            autoStop = store.getBoolean(PrefKeys.AUTO_STOP, d.autoStop),
            debugMode = store.getBoolean(PrefKeys.DEBUG_MODE, d.debugMode),
            debugAddr = store.getString(PrefKeys.DEBUG_ADDR, d.debugAddr),
        )
        // The original app replaced the placeholder name with the system device name on first run.
        val systemName = systemDeviceName()
        return if (loaded.deviceName == TrycorderSettings.DEFAULT_DEVICE_NAME && systemName.isNotBlank()) {
            loaded.copy(deviceName = systemName).also { save(it) }
        } else {
            loaded
        }
    }

    private fun save(s: TrycorderSettings) {
        store.putBoolean(PrefKeys.AUTO_LISTEN, s.autoListen)
        store.putBoolean(PrefKeys.IS_CHATTY, s.isChatty)
        store.putString(PrefKeys.DISPLAY_LANGUAGE, s.displayLanguage.code)
        store.putString(PrefKeys.LISTEN_LANGUAGE, s.listenLanguage.code)
        store.putString(PrefKeys.SPEAK_LANGUAGE, s.speakLanguage.code)
        store.putString(PrefKeys.DEVICE_NAME, s.deviceName)
        store.putBoolean(PrefKeys.IS_MASTER, s.isMaster)
        store.putBoolean(PrefKeys.SEND_LOCAL, s.sendLocal)
        store.putBoolean(PrefKeys.SEND_REMOTE, s.sendRemote)
        store.putBoolean(PrefKeys.EXEC_LOCAL, s.execLocal)
        store.putBoolean(PrefKeys.EXEC_REMOTE, s.execRemote)
        store.putBoolean(PrefKeys.REPLAY_SENT, s.replaySent)
        store.putBoolean(PrefKeys.AUTO_BOOT, s.autoBoot)
        store.putBoolean(PrefKeys.AUTO_STOP, s.autoStop)
        store.putBoolean(PrefKeys.DEBUG_MODE, s.debugMode)
        store.putString(PrefKeys.DEBUG_ADDR, s.debugAddr)
    }
}
