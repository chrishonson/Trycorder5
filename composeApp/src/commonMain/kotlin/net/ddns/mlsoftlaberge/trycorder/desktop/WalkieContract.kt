package net.ddns.mlsoftlaberge.trycorder.desktop

import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.Speaker
import net.ddns.mlsoftlaberge.trycorder.core.SpeechListener
import net.ddns.mlsoftlaberge.trycorder.core.SystemInfo
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.link.StarshipLink

/**
 * State shared by the Desktop screen (TrydesktopFragment) and the Client screen (TryclientFragment):
 * both are walkie-talkie consoles on top of the starship link.
 */
data class WalkieState(
    /** textstatus_top: the screen title, then the last sentence heard or received. */
    val topStatus: String,
    /** textstatus_bottom: the last say() line. */
    val bottomStatus: String = "Ready",
    /** logs_console: every say() line, newest first. */
    val consoleLog: String = "Console log",
    /** walkie_iplist: "ip - name" of the local Trycorders. */
    val peersText: String = "",
    val listenLanguage: TrekLanguage = TrekLanguage.English,
    val speakLanguage: TrekLanguage = TrekLanguage.English,
)

/** Button and lifecycle actions of the walkie-talkie screens. */
sealed interface WalkieIntent {
    sealed interface Lifecycle : WalkieIntent
    sealed interface Bar : WalkieIntent
    sealed interface Comm : WalkieIntent

    /** The screen is shown (onStart): listen to the link and the recognizer, refresh the peer list. */
    data object Start : Lifecycle

    /** The screen is hidden (onStop). */
    data object Stop : Lifecycle

    /** The LCARS corners and the Back button. */
    data object Back : Bar
    data object Settings : Bar

    /** View, Send and COMM only beep in the original. */
    data object View : Bar
    data object Send : Bar
    data object CommMode : Bar

    data object Speak : Comm
    data object TalkDown : Comm
    data object TalkUp : Comm
    data object Scan : Comm
    data object ServerOn : Comm
    data object ServerOff : Comm
}

/** The platform services the walkie-talkie stores use (a subset of [PlatformServices], easy to fake). */
class WalkieDeps(
    val settings: SettingsRepository,
    val sound: SoundPlayer,
    val speaker: Speaker,
    val listener: SpeechListener,
    val link: StarshipLink,
    val systemInfo: SystemInfo,
) {
    companion object {
        fun from(services: PlatformServices) = WalkieDeps(
            settings = services.settings,
            sound = services.sound,
            speaker = services.speaker,
            listener = services.listener,
            link = services.link,
            systemInfo = services.systemInfo,
        )
    }
}
