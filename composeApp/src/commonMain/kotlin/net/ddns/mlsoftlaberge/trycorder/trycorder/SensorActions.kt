package net.ddns.mlsoftlaberge.trycorder.trycorder

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound

/** Sensor pages, their hardware readings and the background scanning sounds. */
internal class SensorActions(private val ctx: TrycorderContext) {
    private var readingJob: Job? = null

    /** switchsensorlayout(): show a page without starting it. */
    fun showPage(page: SensorPage) = ctx.update {
        it.copy(sensorPage = page, scanPage = if (page.isScanPage) page else it.scanPage)
    }

    /** startsensors(): stop everything, then start the sensor or animation of [page]. */
    fun start(page: SensorPage) {
        stop()
        ctx.say(START_MESSAGES[page] ?: "Sensors OFF")
        val readings = readingsFor(page)
        if (readings != null) readingJob = readings.launchIn(ctx.scope)
        val started = page != SensorPage.EarthStill && page != SensorPage.StartrekLogo && page != SensorPage.Walkie
        ctx.update {
            it.copy(
                activeSensor = page.takeIf { started },
                sensorPage = page,
                scanPage = if (page.isScanPage) page else it.scanPage,
                fireShot = if (page == SensorPage.Fire) it.fireShot + 1 else it.fireShot,
            )
        }
        if (page in MUSIC_PAGES && ctx.state.soundEnabled) startMusic()
    }

    /** stopsensors(): stop every sensor, animation and the background sound. */
    fun stop() {
        readingJob?.cancel()
        readingJob = null
        locationJob = null
        stopMusic()
        ctx.update { it.copy(activeSensor = null) }
    }

    fun toggleSound() {
        if (ctx.state.soundEnabled) {
            ctx.update { it.copy(soundEnabled = false) }
            stopMusic()
        } else {
            ctx.update { it.copy(soundEnabled = true) }
            if (ctx.state.activeSensor?.isScanPage == true) startMusic()
        }
    }

    private fun startMusic() {
        val state = ctx.state
        if (!state.musicPlaying) {
            LOOPS[state.sensorPage]?.let { ctx.services.sound.startLoop(it) }
        }
        ctx.update { it.copy(musicPlaying = it.sensorPage != SensorPage.EarthStill) }
    }

    private fun stopMusic() {
        ctx.services.sound.stopLoop()
        ctx.update { it.copy(musicPlaying = false) }
    }

    private fun readingsFor(page: SensorPage): Flow<*>? {
        val sensors = ctx.services.sensors
        return when (page) {
            SensorPage.Magnetic -> sensors.magnetic().onEach { v -> ctx.update { it.copy(readings = it.readings.copy(magnetic = v)) } }
            SensorPage.Orientation -> orientationAndLocation()
            SensorPage.Gravity -> sensors.acceleration().onEach { v -> ctx.update { it.copy(readings = it.readings.copy(acceleration = v)) } }
            SensorPage.Temperature -> sensors.environment().onEach { v -> ctx.update { it.copy(readings = it.readings.copy(environment = v)) } }
            SensorPage.AudioWave -> sensors.audio().onEach { v -> ctx.update { it.copy(readings = it.readings.copy(audio = v)) } }
            else -> null
        }
    }

    private fun orientationAndLocation(): Flow<*> {
        val sensors = ctx.services.sensors
        ctx.scope.launch {
            sensors.location().collect { loc -> ctx.update { it.copy(readings = it.readings.copy(location = loc)) } }
        }.also { locationJob = it }
        return sensors.orientation().onEach { v -> ctx.update { it.copy(readings = it.readings.copy(orientation = v)) } }
    }

    private var locationJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    /** The Logs Stat page refreshes the memory figures every second while shown. */
    fun startMemoryStats(): Job = ctx.scope.launch {
        while (isActive) {
            val memory = ctx.services.systemInfo.memory()
            ctx.update { it.copy(readings = it.readings.copy(memory = memory)) }
            delay(MEMORY_REFRESH_MS)
        }
    }

    private companion object {
        const val MEMORY_REFRESH_MS = 1000L

        val START_MESSAGES = mapOf(
            SensorPage.Magnetic to "Sensors Magnetic",
            SensorPage.Orientation to "Sensors Orientation",
            SensorPage.Gravity to "Sensors Gravity",
            SensorPage.Temperature to "Sensors Temperature",
            SensorPage.AudioWave to "Sensors Audio Wave",
            SensorPage.Shields to "Sensors Shields",
            SensorPage.Fire to "Sensors Fire Animation",
            SensorPage.Transporter to "Sensors Transport Animation",
            SensorPage.Tractor to "Sensors Tractor Animation",
            SensorPage.Motor to "Sensors Motor Animation",
            SensorPage.StartrekLogo to "Sensors Startrek Logo",
            SensorPage.Walkie to "Sensors Walkie Panel",
        )

        /** Pages that start the background sound when sound is on. */
        val MUSIC_PAGES = setOf(
            SensorPage.Magnetic, SensorPage.Orientation, SensorPage.Gravity, SensorPage.Temperature,
            SensorPage.Fire, SensorPage.Tractor, SensorPage.Motor,
        )

        val LOOPS = mapOf(
            SensorPage.Magnetic to TrekSound.TricorderScan,
            SensorPage.Orientation to TrekSound.LongRangeScan,
            SensorPage.Gravity to TrekSound.ScanLow,
            SensorPage.Temperature to TrekSound.ScanHigh,
            SensorPage.Tractor to TrekSound.Tractor,
        )
    }
}
