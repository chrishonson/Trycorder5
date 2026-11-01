package net.ddns.mlsoftlaberge.trycorder.sensors

import kotlin.math.PI

/**
 * Conversions from the platform sensor units to the ones the original sensor views used
 * (the deprecated Android TYPE_ORIENTATION degrees, accelerometer in m/s2, pressure in hPa).
 */
object SensorMath {
    /** Standard gravity, used to turn CoreMotion g into Android m/s2. */
    const val STANDARD_GRAVITY = 9.80665f

    private const val FULL_TURN = 360f
    private const val HALF_TURN = 180f
    private const val QUARTER_TURN = 90f
    private const val KPA_TO_HPA = 10f

    fun degrees(radians: Double): Float = (radians * HALF_TURN / PI).toFloat()

    /** Wraps an angle into [0, 360). */
    fun normalizeAzimuth(degrees: Float): Float {
        val wrapped = degrees % FULL_TURN
        return if (wrapped < 0f) wrapped + FULL_TURN else wrapped
    }

    /**
     * SensorManager.getOrientation() radians to the TYPE_ORIENTATION values the original
     * OriSensorView read: azimuth 0..360 (0 = north, 90 = east), pitch and roll in degrees.
     * This is what Android's own fused orientation sensor computed from the rotation vector.
     */
    fun fromAndroidOrientation(azimuth: Float, pitch: Float, roll: Float): Vector3 = Vector3(
        x = normalizeAzimuth(degrees(azimuth.toDouble())),
        y = degrees(pitch.toDouble()),
        z = degrees(roll.toDouble()),
    )

    /**
     * CoreMotion attitude (reference frame X = magnetic north, Z vertical) to the same values.
     * Yaw is counter-clockwise from north for the device x axis, so the top edge of a portrait
     * device points north when yaw is -90 degrees. iOS pitch is positive when the top edge rises,
     * which Android reports as a negative pitch.
     */
    fun fromIosAttitude(yaw: Double, pitch: Double, roll: Double): Vector3 = Vector3(
        x = normalizeAzimuth(-degrees(yaw) - QUARTER_TURN),
        y = -degrees(pitch),
        z = degrees(roll),
    )

    /**
     * CoreMotion acceleration (in g, -1 on y for an upright device) to the Android accelerometer
     * convention (m/s2, +9.81 on y for an upright device).
     */
    fun fromIosAcceleration(x: Double, y: Double, z: Double): Vector3 = Vector3(
        x = (-x * STANDARD_GRAVITY).toFloat(),
        y = (-y * STANDARD_GRAVITY).toFloat(),
        z = (-z * STANDARD_GRAVITY).toFloat(),
    )

    /** CMAltimeter reports kPa, Android TYPE_PRESSURE reports hPa. */
    fun kiloPascalToHectoPascal(kpa: Double): Float = (kpa * KPA_TO_HPA).toFloat()
}
