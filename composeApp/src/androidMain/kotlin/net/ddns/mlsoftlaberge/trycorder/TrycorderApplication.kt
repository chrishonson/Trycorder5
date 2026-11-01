package net.ddns.mlsoftlaberge.trycorder

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager
import net.ddns.mlsoftlaberge.trycorder.camera.AndroidCameraController
import net.ddns.mlsoftlaberge.trycorder.core.AndroidExternalLauncher
import net.ddns.mlsoftlaberge.trycorder.core.AndroidKeyValueStore
import net.ddns.mlsoftlaberge.trycorder.core.AndroidSoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.AndroidSpeaker
import net.ddns.mlsoftlaberge.trycorder.core.AndroidSystemInfo
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository
import net.ddns.mlsoftlaberge.trycorder.crew.AndroidCrewDirectory
import net.ddns.mlsoftlaberge.trycorder.inventory.AndroidInventory
import net.ddns.mlsoftlaberge.trycorder.link.AndroidSpeechListener
import net.ddns.mlsoftlaberge.trycorder.link.AndroidStarshipLink
import net.ddns.mlsoftlaberge.trycorder.media.AndroidMediaCapture
import net.ddns.mlsoftlaberge.trycorder.media.AndroidMediaLibrary
import net.ddns.mlsoftlaberge.trycorder.sensors.AndroidSensorHub

/** Owns the process-wide [PlatformServices], shared by the activity and the network service. */
class TrycorderApplication : Application() {
    val services: PlatformServices by lazy { createServices(this) }

    private fun createServices(context: Context): PlatformServices {
        @Suppress("DEPRECATION") // same file as the original app, so existing settings survive the update
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val keyValueStore = AndroidKeyValueStore(prefs)
        val systemInfo = AndroidSystemInfo(context)
        val settings = SettingsRepository(keyValueStore) { systemInfo.deviceName() }
        val sound = AndroidSoundPlayer(context)
        val speaker = AndroidSpeaker(context, settings)
        return PlatformServices(
            keyValueStore = keyValueStore,
            settings = settings,
            sound = sound,
            speaker = speaker,
            listener = AndroidSpeechListener(context),
            sensors = AndroidSensorHub(context),
            link = AndroidStarshipLink(context, settings, systemInfo, sound, speaker),
            systemInfo = systemInfo,
            launcher = AndroidExternalLauncher(context),
            camera = AndroidCameraController(context),
            mediaCapture = AndroidMediaCapture(context),
            mediaLibrary = AndroidMediaLibrary(context),
            crew = AndroidCrewDirectory(context),
            inventory = AndroidInventory(context),
        )
    }
}
