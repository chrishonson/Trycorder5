package net.ddns.mlsoftlaberge.trycorder.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import net.ddns.mlsoftlaberge.trycorder.camera.CameraController
import net.ddns.mlsoftlaberge.trycorder.camera.SnapResult
import net.ddns.mlsoftlaberge.trycorder.core.ExternalLauncher
import net.ddns.mlsoftlaberge.trycorder.core.FakeSpeechListener
import net.ddns.mlsoftlaberge.trycorder.core.InMemoryKeyValueStore
import net.ddns.mlsoftlaberge.trycorder.core.InfoSection
import net.ddns.mlsoftlaberge.trycorder.core.MemoryStats
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.Speaker
import net.ddns.mlsoftlaberge.trycorder.core.SystemInfo
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.crew.CrewDirectory
import net.ddns.mlsoftlaberge.trycorder.inventory.Inventory
import net.ddns.mlsoftlaberge.trycorder.link.LinkEvent
import net.ddns.mlsoftlaberge.trycorder.link.PeerList
import net.ddns.mlsoftlaberge.trycorder.link.ServerStats
import net.ddns.mlsoftlaberge.trycorder.link.StarshipLink
import net.ddns.mlsoftlaberge.trycorder.media.CaptureResult
import net.ddns.mlsoftlaberge.trycorder.media.MediaCapture
import net.ddns.mlsoftlaberge.trycorder.media.MediaItem
import net.ddns.mlsoftlaberge.trycorder.media.MediaLibrary
import net.ddns.mlsoftlaberge.trycorder.media.MediaType
import net.ddns.mlsoftlaberge.trycorder.sensors.Environment
import net.ddns.mlsoftlaberge.trycorder.sensors.GeoLocation
import net.ddns.mlsoftlaberge.trycorder.sensors.SensorHub
import net.ddns.mlsoftlaberge.trycorder.sensors.Vector3

class RecordingSoundPlayer : SoundPlayer {
    val played = mutableListOf<TrekSound>()
    var loop: TrekSound? = null
    override fun play(sound: TrekSound) {
        played += sound
    }
    override fun startLoop(sound: TrekSound) {
        loop = sound
    }
    override fun stopLoop() {
        loop = null
    }
    override fun release() = Unit
}

class RecordingSpeaker : Speaker {
    val spoken = mutableListOf<Pair<String, TrekLanguage?>>()
    override fun speak(text: String, language: TrekLanguage?) {
        spoken += text to language
    }
    override fun setLanguage(language: TrekLanguage) = Unit
    override fun shutdown() = Unit
}

class FakeSensorHub : SensorHub {
    val magnetic = MutableSharedFlow<Vector3>(extraBufferCapacity = 8)
    var magneticCollectors = 0
        private set
    override fun magnetic(): Flow<Vector3> =
        magnetic.onStart { magneticCollectors++ }.onCompletion { magneticCollectors-- }
    override fun orientation(): Flow<Vector3> = MutableSharedFlow()
    override fun acceleration(): Flow<Vector3> = MutableSharedFlow()
    override fun environment(): Flow<Environment> = MutableSharedFlow()
    override fun location(): Flow<GeoLocation> = MutableSharedFlow()
    override fun audio(): Flow<ShortArray> = MutableSharedFlow()
}

class FakeLink : StarshipLink {
    val sent = mutableListOf<String>()
    val eventFlow = MutableSharedFlow<LinkEvent>(extraBufferCapacity = 8)
    var starts = 0
    var stops = 0
    override val events: Flow<LinkEvent> = eventFlow
    override val peers: StateFlow<PeerList> = MutableStateFlow(PeerList())
    override val stats: StateFlow<ServerStats> = MutableStateFlow(ServerStats())
    override val running: StateFlow<Boolean> = MutableStateFlow(false)
    override fun start() {
        starts++
    }
    override fun stop() {
        stops++
    }
    override fun send(text: String) {
        sent += text
    }
    override fun startAudioStream() = Unit
    override fun stopAudioStream() = Unit
}

class FakeSystemInfo : SystemInfo {
    override fun deviceName() = "Pixel"
    override fun deviceRelease() = "16"
    override fun appVersion() = "6.0.0"
    override fun ipAddress() = "192.168.0.10"
    override fun section(section: InfoSection) = "${section.title} data"
    override fun memory() = MemoryStats(total = TOTAL, free = FREE)

    private companion object {
        const val TOTAL = 8L
        const val FREE = 4L
    }
}

class FakeLauncher : ExternalLauncher {
    val maps = mutableListOf<Pair<Double, Double>>()
    override fun openMap(latitude: Double, longitude: Double): Boolean {
        maps += latitude to longitude
        return true
    }
    override fun openUrl(url: String) = true
}

class FakeCamera(override var isActive: Boolean = false, private val result: SnapResult? = null) : CameraController {
    override suspend fun snap(): SnapResult? = result
}

class FakeMediaCapture(var photo: CaptureResult = CaptureResult.Cancelled) : MediaCapture {
    override suspend fun takePhoto() = photo
    override suspend fun recordVideo(): CaptureResult = CaptureResult.Cancelled
}

class FakeMediaLibrary : MediaLibrary {
    override suspend fun list(type: MediaType): List<MediaItem> = emptyList()
    override suspend fun delete(item: MediaItem) = false
    override suspend fun read(uri: String): ByteArray? = PICTURE

    private companion object {
        val PICTURE = byteArrayOf(1, 2, 3)
    }
}

/** All fakes wired together, exposed for assertions. */
class TestServices(
    initialPrefs: Map<String, Any> = emptyMap(),
    camera: CameraController = FakeCamera(),
) {
    val store = InMemoryKeyValueStore(initialPrefs)
    val settings = SettingsRepository(store)
    val sound = RecordingSoundPlayer()
    val speaker = RecordingSpeaker()
    val listener = FakeSpeechListener()
    val sensors = FakeSensorHub()
    val link = FakeLink()
    val launcher = FakeLauncher()
    val mediaCapture = FakeMediaCapture()

    val services = PlatformServices(
        keyValueStore = store,
        settings = settings,
        sound = sound,
        speaker = speaker,
        listener = listener,
        sensors = sensors,
        link = link,
        systemInfo = FakeSystemInfo(),
        launcher = launcher,
        camera = camera,
        mediaCapture = mediaCapture,
        mediaLibrary = FakeMediaLibrary(),
        crew = object : CrewDirectory {},
        inventory = object : Inventory {},
    )
}
