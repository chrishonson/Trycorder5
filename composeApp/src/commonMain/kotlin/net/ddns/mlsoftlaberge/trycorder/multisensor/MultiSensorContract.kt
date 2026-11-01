package net.ddns.mlsoftlaberge.trycorder.multisensor

import net.ddns.mlsoftlaberge.trycorder.core.ExternalLauncher
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.Speaker
import net.ddns.mlsoftlaberge.trycorder.core.SpeechListener
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.sensors.Environment
import net.ddns.mlsoftlaberge.trycorder.sensors.GeoLocation
import net.ddns.mlsoftlaberge.trycorder.sensors.SensorHub
import net.ddns.mlsoftlaberge.trycorder.sensors.Vector3

/** One microphone buffer. Compared by identity: each new chunk is a new waveform. */
class AudioChunk(val samples: ShortArray) {
    companion object {
        val Empty = AudioChunk(ShortArray(0))
    }
}

/** State of the MultiSensor screen (the old TrysensorFragment): five sensors at once. */
data class MultiSensorState(
    val topStatus: String = "Sensors",
    val bottomStatus: String = "Ready",
    val running: Boolean = false,
    val magnetic: Vector3 = Vector3(),
    val orientation: Vector3 = Vector3(),
    val acceleration: Vector3 = Vector3(),
    val environment: Environment = Environment(),
    val location: GeoLocation? = null,
    val audio: AudioChunk = AudioChunk.Empty,
    val speakLanguage: TrekLanguage = TrekLanguage.English,
    val listenLanguage: TrekLanguage = TrekLanguage.English,
)

sealed interface MultiSensorIntent {
    /** onResume: start every sensor. */
    data object Resume : MultiSensorIntent

    /** onPause: stop every sensor. */
    data object Pause : MultiSensorIntent

    /** The LCARS corners and the Back button: switchtryviewermode(1). */
    data object Back : MultiSensorIntent
    data object Settings : MultiSensorIntent

    /** Speak button: listen to a voice command. */
    data object Speak : MultiSensorIntent
    data object Send : MultiSensorIntent

    /** Tap on the orientation panel: show where we are on the map. */
    data object OpenMap : MultiSensorIntent
}

/** The platform services the MultiSensor store uses. */
class MultiSensorDeps(
    val sensors: SensorHub,
    val sound: SoundPlayer,
    val speaker: Speaker,
    val listener: SpeechListener,
    val settings: SettingsRepository,
    val launcher: ExternalLauncher,
) {
    companion object {
        fun from(services: PlatformServices) = MultiSensorDeps(
            sensors = services.sensors,
            sound = services.sound,
            speaker = services.speaker,
            listener = services.listener,
            settings = services.settings,
            launcher = services.launcher,
        )
    }
}
