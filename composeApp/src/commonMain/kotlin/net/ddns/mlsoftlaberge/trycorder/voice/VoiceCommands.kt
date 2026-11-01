package net.ddns.mlsoftlaberge.trycorder.voice

import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage

/** Commands understood by the Trycorder ("computer phaser", "ordinateur ..."), from matchvoice(). */
enum class TrekCommand {
    SensorOff, Magnetic, Orientation, Gravity, Temperature,
    HailingClose, Hailing, Intercom, ChatComm,
    LowerShield, RaiseShield,
    Phaser, Torpedo,
    BeamMeUp, BeamMeDown,
    TractorPush, TractorOff, TractorPull,
    ImpulsePower, StayHere, WarpDrive,
    ViewerOn, LocalViewer, ViewerOff, ViewerPhoto, SnapPhoto,
    LogsConsole, LogsInfo, SystemPlans, SystemInfo, SystemStat,
    AutoDestruct, SpeakList,
}

/** Result of parsing a sentence. */
sealed interface VoiceMatch {
    data class SwitchLanguage(val language: TrekLanguage) : VoiceMatch
    data object Rude : VoiceMatch
    data class Command(val command: TrekCommand) : VoiceMatch
}

object VoiceCommands {
    private val prefixes = listOf("computer ", "ordinateur ")

    /** Keyword table in the order the original matchvoice() tested them. First match wins. */
    private val table: List<Pair<(String) -> Boolean, TrekCommand>> = listOf(
        has("sensor off") to TrekCommand.SensorOff,
        has("sensor", "magnetic") to TrekCommand.Magnetic,
        has("orientation", "direction") to TrekCommand.Orientation,
        has("gravity", "vibration") to TrekCommand.Gravity,
        has("temperature", "pressure", "light") to TrekCommand.Temperature,
        { t: String -> "hailing" in t && "close" in t } to TrekCommand.HailingClose,
        has("hailing") to TrekCommand.Hailing,
        has("intercom") to TrekCommand.Intercom,
        has("chatcomm") to TrekCommand.ChatComm,
        has("lower shield") to TrekCommand.LowerShield,
        has("shield", "raise") to TrekCommand.RaiseShield,
        has("phaser") to TrekCommand.Phaser,
        has("fire", "torpedo") to TrekCommand.Torpedo,
        has("beam me up", "scotty", "transporteur") to TrekCommand.BeamMeUp,
        has("beam me down") to TrekCommand.BeamMeDown,
        has("tractor push") to TrekCommand.TractorPush,
        has("tractor off") to TrekCommand.TractorOff,
        has("tractor pull") to TrekCommand.TractorPull,
        has("impulse power") to TrekCommand.ImpulsePower,
        has("stay here") to TrekCommand.StayHere,
        has("warp drive") to TrekCommand.WarpDrive,
        has("viewer on") to TrekCommand.ViewerOn,
        has("local viewer") to TrekCommand.LocalViewer,
        has("viewer off") to TrekCommand.ViewerOff,
        has("viewer photo") to TrekCommand.ViewerPhoto,
        has("snap photo") to TrekCommand.SnapPhoto,
        has("logs console") to TrekCommand.LogsConsole,
        has("logs info") to TrekCommand.LogsInfo,
        has("system plans") to TrekCommand.SystemPlans,
        has("system info") to TrekCommand.SystemInfo,
        has("system stat") to TrekCommand.SystemStat,
        has("auto destruct") to TrekCommand.AutoDestruct,
        has("speak list") to TrekCommand.SpeakList,
    )

    private fun has(vararg words: String): (String) -> Boolean = { text -> words.any { it in text } }

    /** Parse a sentence heard by the microphone or received from the network. */
    fun parse(sentence: String): VoiceMatch? {
        val text = sentence.lowercase()
        return when {
            text.startsWith("french") || text.startsWith("français") -> VoiceMatch.SwitchLanguage(TrekLanguage.French)
            text.startsWith("english") || text.startsWith("anglais") -> VoiceMatch.SwitchLanguage(TrekLanguage.English)
            "fuck" in text || "shit" in text -> VoiceMatch.Rude
            prefixes.none { text.startsWith(it) } -> null
            else -> table.firstOrNull { (matches, _) -> matches(text) }?.let { VoiceMatch.Command(it.second) }
        }
    }

    /**
     * Pick the sentence to act on among the recognizer candidates: the first one that looks like
     * a Trycorder command, otherwise the best candidate (the service's onResults()).
     */
    fun pickCandidate(candidates: List<String>): String? =
        candidates.firstOrNull { looksLikeCommand(it) } ?: candidates.firstOrNull()

    /** The service-side filter: sentences that are commands and must not be spoken back. */
    fun looksLikeCommand(sentence: String): Boolean {
        val text = sentence.lowercase()
        return text.startsWith("server ok") ||
            text.startsWith("french") || text.startsWith("français") ||
            text.startsWith("english") || text.startsWith("anglais") ||
            "fuck" in text || "shit" in text ||
            prefixes.any { text.startsWith(it) }
    }

    /** Help text of the Speak List viewer page. */
    const val SPEAK_LIST = "The SPEAK will respond to:\n" +
        "francais | french\n" +
        "anglais | english\n" +
        "computer | ordinateur\n" +
        "intercom\n" +
        "phaser\n" +
        "fire | torpedo\n" +
        "shield down\n" +
        "raise shield\n" +
        "sensor off\n" +
        "sensor\n" +
        "magnetic\n" +
        "orientation | direction\n" +
        "gravity | vibration\n" +
        "temperature | pressure | light\n" +
        "hailing close\n" +
        "hailing\n" +
        "beam me up | scotty | transporteur\n" +
        "beam me down\n" +
        "viewer\n" +
        "logs\n" +
        "fuck | shit\n"
}
