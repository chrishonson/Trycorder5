package net.ddns.mlsoftlaberge.trycorder.trycorder

import net.ddns.mlsoftlaberge.trycorder.core.KeyValueStore
import net.ddns.mlsoftlaberge.trycorder.link.PeerList

/** Pages and toggles saved on pause and restored on resume (same keys as the original). */
internal class DynamicStatus(private val store: KeyValueStore) {
    data class Saved(
        val sensorPage: SensorPage,
        val scanPage: SensorPage,
        val buttonGroup: ButtonGroup,
        val viewerPage: ViewerPage,
        val soundEnabled: Boolean,
        val commStatus: CommStatus,
        val viewerFront: Boolean,
    )

    fun load() = Saved(
        sensorPage = SensorPage.from(store.getInt(SENSOR_MODE, 0)),
        scanPage = SensorPage.from(store.getInt(SENSOR_PAGE, 0)),
        buttonGroup = ButtonGroup.from(store.getInt(BUTTONS_MODE, 0)),
        viewerPage = ViewerPage.from(store.getInt(VIEWER_MODE, 0)),
        soundEnabled = store.getBoolean(AUDIO_MODE, true),
        commStatus = CommStatus.from(store.getInt(COMM_STATUS, 0)),
        viewerFront = store.getBoolean(VIEWER_FRONT, false),
    )

    fun save(state: TrycorderState) {
        store.putInt(SENSOR_MODE, state.sensorPage.number)
        store.putInt(SENSOR_PAGE, state.scanPage.number)
        store.putInt(BUTTONS_MODE, state.buttonGroup.number)
        store.putInt(VIEWER_MODE, state.viewerPage.number)
        store.putBoolean(AUDIO_MODE, state.soundEnabled)
        store.putInt(COMM_STATUS, state.commStatus.number)
        store.putBoolean(VIEWER_FRONT, state.viewerFront)
    }

    private companion object {
        const val SENSOR_MODE = "pref_key_sensor_mode"
        const val SENSOR_PAGE = "pref_key_sensor_page"
        const val BUTTONS_MODE = "pref_key_buttons_mode"
        const val VIEWER_MODE = "pref_key_viewer_mode"
        const val AUDIO_MODE = "pref_key_audio_mode"
        const val COMM_STATUS = "pref_key_comm_status"
        const val VIEWER_FRONT = "pref_key_viewer_front"
    }
}

/** The walkie-talkie peer list: ":ip - name" for local peers, "*ip - name" for server peers. */
object PeerFormat {
    fun format(peers: PeerList, ownIp: String = ""): String {
        val lines = peers.local.map { ":${it.ip} - ${it.name}" } + peers.remote.map { "*${it.ip} - ${it.name}" }
        return if (lines.isEmpty()) ownIp else lines.joinToString("\n", postfix = "\n")
    }
}
