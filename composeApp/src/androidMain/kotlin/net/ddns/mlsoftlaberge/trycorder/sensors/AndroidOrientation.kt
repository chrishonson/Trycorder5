package net.ddns.mlsoftlaberge.trycorder.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Replacement of the deprecated TYPE_ORIENTATION sensor OriSensorView used: azimuth, pitch and
 * roll in degrees computed from the rotation vector (or the geomagnetic rotation vector, or the
 * accelerometer and magnetometer on devices without either).
 */
internal class AndroidOrientation(private val manager: SensorManager?) {
    private val rotation = FloatArray(MATRIX_SIZE)
    private val angles = FloatArray(AXES)
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    fun flow(): Flow<Vector3> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                orientation(event)?.let { trySend(it) }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sources().forEach { manager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        awaitClose { manager?.unregisterListener(listener) }
    }

    private fun sources(): List<Sensor> {
        val vector = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: manager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        if (vector != null) return listOf(vector)
        return listOfNotNull(
            manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
        )
    }

    private fun orientation(event: SensorEvent): Vector3? {
        val ready = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                gravity = event.values.copyOf()
                fromAccelerometer()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                geomagnetic = event.values.copyOf()
                fromAccelerometer()
            }
            else -> {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                true
            }
        }
        if (!ready) return null
        SensorManager.getOrientation(rotation, angles)
        return SensorMath.fromAndroidOrientation(angles[0], angles[1], angles[2])
    }

    private fun fromAccelerometer(): Boolean {
        val g = gravity ?: return false
        val m = geomagnetic ?: return false
        return SensorManager.getRotationMatrix(rotation, null, g, m)
    }

    private companion object {
        const val MATRIX_SIZE = 9
        const val AXES = 3
    }
}
