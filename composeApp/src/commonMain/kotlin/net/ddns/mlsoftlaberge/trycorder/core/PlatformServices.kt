package net.ddns.mlsoftlaberge.trycorder.core

import net.ddns.mlsoftlaberge.trycorder.camera.CameraController
import net.ddns.mlsoftlaberge.trycorder.crew.CrewDirectory
import net.ddns.mlsoftlaberge.trycorder.inventory.Inventory
import net.ddns.mlsoftlaberge.trycorder.link.StarshipLink
import net.ddns.mlsoftlaberge.trycorder.media.MediaCapture
import net.ddns.mlsoftlaberge.trycorder.media.MediaLibrary
import net.ddns.mlsoftlaberge.trycorder.sensors.SensorHub

/**
 * Everything platform specific the shared stores need. Built once per process by
 * the Android application / the iOS view controller, and passed down to the stores.
 */
@Suppress("LongParameterList") // service container: one constructor argument per platform service
class PlatformServices(
    val keyValueStore: KeyValueStore,
    val settings: SettingsRepository,
    val sound: SoundPlayer,
    val speaker: Speaker,
    val listener: SpeechListener,
    val sensors: SensorHub,
    val link: StarshipLink,
    val systemInfo: SystemInfo,
    val launcher: ExternalLauncher,
    val camera: CameraController,
    val mediaCapture: MediaCapture,
    val mediaLibrary: MediaLibrary,
    val crew: CrewDirectory,
    val inventory: Inventory,
)
