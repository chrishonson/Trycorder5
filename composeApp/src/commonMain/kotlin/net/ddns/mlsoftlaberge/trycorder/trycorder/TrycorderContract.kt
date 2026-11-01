package net.ddns.mlsoftlaberge.trycorder.trycorder

import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.camera.Face
import net.ddns.mlsoftlaberge.trycorder.core.MemoryStats
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.sensors.Environment
import net.ddns.mlsoftlaberge.trycorder.sensors.GeoLocation
import net.ddns.mlsoftlaberge.trycorder.sensors.Vector3
import net.ddns.mlsoftlaberge.trycorder.voice.TrekCommand

/** Content of the sensor area (switchsensorlayout numbers of the original fragment). */
enum class SensorPage(val number: Int) {
    EarthStill(0), Magnetic(1), Orientation(2), Gravity(3), Temperature(4),
    AudioWave(5), Shields(6), Fire(7), Transporter(8), Tractor(9), Motor(10),
    StartrekLogo(11), Walkie(12), Chat(13);

    /** Pages 0..4 are the scanning pages remembered by the SENSORS button. */
    val isScanPage: Boolean get() = number <= Temperature.number

    companion object {
        fun from(number: Int) = entries.firstOrNull { it.number == number } ?: EarthStill
    }
}

/** Button group shown under the sensor area (switchbuttonlayout numbers). */
enum class ButtonGroup(val number: Int) {
    None(0), Sensor(1), Comm(2), Shield(3), Fire(4), Transporter(5),
    Tractor(6), Motor(7), Viewer(8), Logs(9), Mode(10);

    companion object {
        fun from(number: Int) = entries.firstOrNull { it.number == number } ?: None
    }
}

/** Content of the bottom viewer area (switchviewer numbers). */
enum class ViewerPage(val number: Int) {
    FederationLogo(0), Camera(1), LogsConsole(2), LogsInfo(3), Plans(4), Photo(5),
    SystemInfo(6), Animate(7), SpeakList(8), LogsStat(9), ModeWindow(10),
    TransporterSeek(11), FireControl(12), TractorBeam(13), CommandControl(14);

    companion object {
        fun from(number: Int) = entries.firstOrNull { it.number == number } ?: FederationLogo
    }
}

enum class CommStatus(val number: Int) {
    Closed(0), Hailing(1), Intercom(2), Chat(3);

    companion object {
        fun from(number: Int) = entries.firstOrNull { it.number == number } ?: Closed
    }
}

/** Animation of the motor viewer: still earth, rotating earth (impulse), warp tunnel. */
enum class MotorAnimation { Still, Impulse, Warp }

/** Frequency/strength/rotation of the shield and tractor beam animations. */
data class BeamParams(val rotate: Boolean = false, val freq: Int = 0, val force: Int = 0)

/** One of the three vertical transporter sliders. */
data class TransporterSlider(val progress: Int = 0, val mode: Int = 0)

/** The picture shown on the Photo viewer page. Null bytes means the default picture. */
data class ViewerPicture(val bytes: ByteArray? = null, val faces: List<Face> = emptyList(), val id: Int = 0) {
    override fun equals(other: Any?) = other is ViewerPicture && other.id == id && other.faces == faces
    override fun hashCode() = id
}

/** Live sensor values for the sensor pages. */
data class SensorReadings(
    val magnetic: Vector3 = Vector3(),
    val orientation: Vector3 = Vector3(),
    val acceleration: Vector3 = Vector3(),
    val environment: Environment = Environment(),
    val location: GeoLocation? = null,
    val audio: ShortArray = ShortArray(0),
    val memory: MemoryStats = MemoryStats(0, 0),
) {
    override fun equals(other: Any?): Boolean = other is SensorReadings &&
        other.magnetic == magnetic && other.orientation == orientation &&
        other.acceleration == acceleration && other.environment == environment &&
        other.location == location && other.audio === audio && other.memory == memory

    override fun hashCode(): Int = magnetic.hashCode() * HASH + orientation.hashCode()

    private companion object {
        const val HASH = 31
    }
}

data class TrycorderState(
    val topStatus: String = "",
    val bottomStatus: String = "Ready",
    val sensorPage: SensorPage = SensorPage.EarthStill,
    /** Last scanning page, restored by the SENSORS button (mSensorpage). */
    val scanPage: SensorPage = SensorPage.EarthStill,
    val buttonGroup: ButtonGroup = ButtonGroup.None,
    val viewerPage: ViewerPage = ViewerPage.FederationLogo,
    /** Page whose sensor or animation is started, null when stopped. */
    val activeSensor: SensorPage? = null,
    val soundEnabled: Boolean = true,
    val musicPlaying: Boolean = false,
    val commStatus: CommStatus = CommStatus.Closed,
    val viewerFront: Boolean = false,
    val readings: SensorReadings = SensorReadings(),
    val shieldMode: Int = 0,
    val shield: BeamParams = BeamParams(),
    val tractorMode: Int = 0,
    val tractor: BeamParams = BeamParams(),
    /** Values of the shared rotate/freq/force controls of the tractor beam window. */
    val beamControls: BeamParams = BeamParams(),
    val fireMode: Int = 0,
    val fireDirection: Int = 0,
    val fireForce: Int = DEFAULT_FIRE_FORCE,
    /** Incremented each time a shot is fired, so the fire animation restarts. */
    val fireShot: Int = 0,
    val transporterMode: Int = 0,
    val transporterPosition: Int = 0,
    val transporterSliders: List<TransporterSlider> = List(TRANSPORTER_SLIDERS) { TransporterSlider() },
    val transporterTextUp: String = "Beam",
    val transporterTextDown: String = "Ready",
    val motorMode: Int = 0,
    val motorAnimation: MotorAnimation = MotorAnimation.Still,
    val planIndex: Int = 0,
    val consoleLog: String = "",
    val commandLog: String = "",
    val chatLog: String = "",
    val infoText: String = "",
    val peersText: String = "",
    val chatInput: String = "",
    val commandInput: String = "",
    val picture: ViewerPicture = ViewerPicture(),
    val speakLanguage: TrekLanguage = TrekLanguage.English,
    val listenLanguage: TrekLanguage = TrekLanguage.English,
) {
    companion object {
        const val DEFAULT_FIRE_FORCE = 10
        const val TRANSPORTER_SLIDERS = 3
        const val TRANSPORTER_MAX = 255
        const val FIRE_DIRECTION_MAX = 100
        const val FIRE_FORCE_MAX = 100
        const val BEAM_FREQ_MAX = 10
        const val BEAM_FORCE_MAX = 5
        const val PLAN_COUNT = 7
        const val LOG_LIMIT = 1000
    }
}

/**
 * Every button, slider and system event of the main Trycorder screen.
 * Intents are grouped by the sub-interface that handles them.
 */
sealed interface TrycorderIntent {
    sealed interface Lifecycle : TrycorderIntent
    sealed interface Bar : TrycorderIntent
    sealed interface Control : TrycorderIntent
    sealed interface Slider : TrycorderIntent
    sealed interface Comm : TrycorderIntent

    // lifecycle of the screen (fragment onStart/onStop/onResume/onPause)
    data object Start : Lifecycle
    data object Stop : Lifecycle
    data object Resume : Lifecycle
    data object Pause : Lifecycle

    // top and bottom bars
    data object Talk : Bar
    data object StartSensors : Bar
    data object StopSensors : Bar
    data object ToggleSound : Bar
    data object OpenSettings : Bar
    data object Snap : Bar
    data object TakePhoto : Bar
    data object RecordVideo : Bar
    data object OpenGallery : Bar

    // left column, group buttons, mode window
    data class SelectGroup(val group: ButtonGroup) : Control

    /** Group buttons and voice commands share the same command set. */
    data class Command(val command: TrekCommand) : Control
    data object YellowAlert : Control
    data object RedAlert : Control
    data object NextPlan : Control
    data object FederationLogo : Control
    data object OpenMap : Control
    data class OpenScreen(val screen: Screen) : Control

    // sliders of the fire, transporter and tractor beam windows
    data class FireDirection(val progress: Int) : Slider
    data class FireForce(val progress: Int) : Slider
    data class TransporterSlideStart(val index: Int) : Slider
    data class TransporterSlide(val index: Int, val progress: Int) : Slider
    data class TransporterSlideStop(val index: Int) : Slider
    data class BeamRotate(val rotate: Boolean) : Slider
    data class BeamFreq(val freq: Int) : Slider
    data class BeamForce(val force: Int) : Slider

    // walkie-talkie panel, chat panel and command window
    data object PushToTalkDown : Comm
    data object PushToTalkUp : Comm
    data object CommandWindow : Comm
    data object ServerOn : Comm
    data object ServerOff : Comm
    data class ChatInput(val text: String) : Comm
    data object ChatSend : Comm

    /** Demo, Logs, City, Ver., List: canned requests to the tryserver. */
    data class ChatRequest(val request: String) : Comm
    data class CommandInput(val text: String) : Comm
    data object CommandSend : Comm
}
