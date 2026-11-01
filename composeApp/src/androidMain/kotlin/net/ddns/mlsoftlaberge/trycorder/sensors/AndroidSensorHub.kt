package net.ddns.mlsoftlaberge.trycorder.sensors

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * SensorManager, LocationManager and AudioRecord behind [SensorHub]. Each flow registers its
 * listener on collection and unregisters it on cancellation (the start()/stop() of the views).
 */
class AndroidSensorHub(private val context: Context) : SensorHub {
    private val sensorManager: SensorManager? get() = context.getSystemService(SensorManager::class.java)

    override fun magnetic(): Flow<Vector3> =
        sensorFlow(Sensor.TYPE_MAGNETIC_FIELD) { Vector3(it.values[0], it.values[1], it.values[2]) }

    override fun orientation(): Flow<Vector3> = AndroidOrientation(sensorManager).flow()

    override fun acceleration(): Flow<Vector3> =
        sensorFlow(Sensor.TYPE_ACCELEROMETER) { Vector3(it.values[0], it.values[1], it.values[2]) }

    /** Temperature, pressure and light; a value stays null when the device lacks that sensor. */
    override fun environment(): Flow<Environment> = callbackFlow {
        val manager = sensorManager
        var current = Environment()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val value = event.values[0]
                current = when (event.sensor.type) {
                    Sensor.TYPE_AMBIENT_TEMPERATURE -> current.copy(temperature = value)
                    Sensor.TYPE_PRESSURE -> current.copy(pressure = value)
                    else -> current.copy(light = value)
                }
                trySend(current)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        trySend(current)
        ENVIRONMENT_SENSORS.mapNotNull { manager?.getDefaultSensor(it) }.forEach {
            manager?.registerListener(listener, it, SAMPLING)
        }
        awaitClose { manager?.unregisterListener(listener) }
    }

    override fun location(): Flow<GeoLocation> = AndroidLocation(context).flow()

    override fun audio(): Flow<ShortArray> = AndroidMicrophone(context).flow()

    override fun toString() = "AndroidSensorHub(${context.packageName})"

    /** One sensor mapped with [map]; emits nothing when the device lacks it. */
    private fun sensorFlow(type: Int, map: (SensorEvent) -> Vector3): Flow<Vector3> = callbackFlow {
        val manager = sensorManager
        val sensor = manager?.getDefaultSensor(type)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(map(event))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        if (sensor != null) manager.registerListener(listener, sensor, SAMPLING)
        awaitClose { manager?.unregisterListener(listener) }
    }

    internal companion object {
        /**
         * The views asked for SENSOR_DELAY_FASTEST. Since Android 12 that needs the
         * HIGH_SAMPLING_RATE_SENSORS permission, and the screen cannot show more than one value
         * per frame anyway, so the game rate (about 50 Hz) is used.
         */
        const val SAMPLING = SensorManager.SENSOR_DELAY_GAME

        val ENVIRONMENT_SENSORS = listOf(Sensor.TYPE_AMBIENT_TEMPERATURE, Sensor.TYPE_PRESSURE, Sensor.TYPE_LIGHT)

        fun granted(context: Context, permission: String): Boolean =
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}
