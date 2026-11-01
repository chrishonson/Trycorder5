package net.ddns.mlsoftlaberge.trycorder

import androidx.compose.ui.window.ComposeUIViewController
import net.ddns.mlsoftlaberge.trycorder.camera.IosCameraController
import net.ddns.mlsoftlaberge.trycorder.core.IosExternalLauncher
import net.ddns.mlsoftlaberge.trycorder.core.IosKeyValueStore
import net.ddns.mlsoftlaberge.trycorder.core.IosSoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.IosSpeaker
import net.ddns.mlsoftlaberge.trycorder.core.IosSystemInfo
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository
import net.ddns.mlsoftlaberge.trycorder.crew.IosCrewDirectory
import net.ddns.mlsoftlaberge.trycorder.inventory.IosInventory
import net.ddns.mlsoftlaberge.trycorder.link.IosSpeechListener
import net.ddns.mlsoftlaberge.trycorder.link.IosStarshipLink
import net.ddns.mlsoftlaberge.trycorder.media.IosMediaCapture
import net.ddns.mlsoftlaberge.trycorder.media.IosMediaLibrary
import net.ddns.mlsoftlaberge.trycorder.sensors.IosSensorHub

private val services: PlatformServices by lazy {
    val keyValueStore = IosKeyValueStore()
    val systemInfo = IosSystemInfo()
    val settings = SettingsRepository(keyValueStore) { systemInfo.deviceName() }
    val sound = IosSoundPlayer()
    val speaker = IosSpeaker(settings)
    PlatformServices(
        keyValueStore = keyValueStore,
        settings = settings,
        sound = sound,
        speaker = speaker,
        listener = IosSpeechListener(),
        sensors = IosSensorHub(),
        link = IosStarshipLink(settings, systemInfo, sound, speaker),
        systemInfo = systemInfo,
        launcher = IosExternalLauncher(),
        camera = IosCameraController(),
        mediaCapture = IosMediaCapture(),
        mediaLibrary = IosMediaLibrary(),
        crew = IosCrewDirectory(),
        inventory = IosInventory(),
    )
}

@Suppress("FunctionName", "ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController { App(services) }
