package net.ddns.mlsoftlaberge.trycorder.sensors

import kotlinx.coroutines.flow.Flow

/** A 3-axis reading (magnetic field in uT, acceleration in m/s2, or orientation in degrees). */
data class Vector3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f)

/** Ambient readings shown by the temperature page. Null means the device has no such sensor. */
data class Environment(
    val temperature: Float? = null,
    val pressure: Float? = null,
    val light: Float? = null,
)

data class GeoLocation(val latitude: Double, val longitude: Double, val altitude: Double = 0.0)

/**
 * Hardware sensors. Every flow starts the sensor on collection and stops it on cancellation,
 * which replaces the start()/stop() pairs of the original sensor views.
 */
interface SensorHub {
    fun magnetic(): Flow<Vector3>

    /** azimuth (x), pitch (y), roll (z) in degrees. */
    fun orientation(): Flow<Vector3>
    fun acceleration(): Flow<Vector3>
    fun environment(): Flow<Environment>
    fun location(): Flow<GeoLocation>

    /** Microphone PCM 16 bit mono chunks for the audio waveform page. */
    fun audio(): Flow<ShortArray>
}
