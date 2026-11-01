package net.ddns.mlsoftlaberge.trycorder.sensors

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreMotion.CMAltimeter
import platform.CoreMotion.CMAttitudeReferenceFrameXArbitraryCorrectedZVertical
import platform.CoreMotion.CMAttitudeReferenceFrameXMagneticNorthZVertical
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

/**
 * CoreMotion, CoreLocation and AVAudioEngine behind [SensorHub]. Values are converted to the
 * Android units the panels expect (see [SensorMath]). Every flow stops its source on cancellation.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSensorHub : SensorHub {
    override fun magnetic(): Flow<Vector3> = callbackFlow {
        val manager = CMMotionManager()
        manager.magnetometerUpdateInterval = UPDATE_INTERVAL_S
        if (manager.magnetometerAvailable) {
            manager.startMagnetometerUpdatesToQueue(NSOperationQueue.mainQueue) { data, _ ->
                data?.magneticField?.useContents { trySend(Vector3(x.toFloat(), y.toFloat(), z.toFloat())) }
            }
        }
        awaitClose { manager.stopMagnetometerUpdates() }
    }.flowOn(Dispatchers.Main)

    override fun orientation(): Flow<Vector3> = callbackFlow {
        val manager = CMMotionManager()
        manager.deviceMotionUpdateInterval = ORIENTATION_INTERVAL_S
        val frames = CMMotionManager.availableAttitudeReferenceFrames()
        val frame = if (frames and CMAttitudeReferenceFrameXMagneticNorthZVertical != 0uL) {
            CMAttitudeReferenceFrameXMagneticNorthZVertical
        } else {
            CMAttitudeReferenceFrameXArbitraryCorrectedZVertical
        }
        if (manager.deviceMotionAvailable) {
            manager.startDeviceMotionUpdatesUsingReferenceFrame(frame, NSOperationQueue.mainQueue) { motion, _ ->
                motion?.attitude?.let { trySend(SensorMath.fromIosAttitude(it.yaw, it.pitch, it.roll)) }
            }
        }
        awaitClose { manager.stopDeviceMotionUpdates() }
    }.flowOn(Dispatchers.Main)

    override fun acceleration(): Flow<Vector3> = callbackFlow {
        val manager = CMMotionManager()
        manager.accelerometerUpdateInterval = UPDATE_INTERVAL_S
        if (manager.accelerometerAvailable) {
            manager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) { data, _ ->
                data?.acceleration?.useContents { trySend(SensorMath.fromIosAcceleration(x, y, z)) }
            }
        }
        awaitClose { manager.stopAccelerometerUpdates() }
    }.flowOn(Dispatchers.Main)

    /** Only the barometer exists on iOS: temperature and light stay null. */
    override fun environment(): Flow<Environment> = callbackFlow {
        var current = Environment()
        trySend(current)
        val altimeter = CMAltimeter()
        if (CMAltimeter.isRelativeAltitudeAvailable()) {
            altimeter.startRelativeAltitudeUpdatesToQueue(NSOperationQueue.mainQueue) { data, _ ->
                data?.pressure?.doubleValue?.let {
                    current = current.copy(pressure = SensorMath.kiloPascalToHectoPascal(it))
                    trySend(current)
                }
            }
        }
        awaitClose { altimeter.stopRelativeAltitudeUpdates() }
    }.flowOn(Dispatchers.Main)

    override fun location(): Flow<GeoLocation> = IosLocation().flow()

    override fun audio(): Flow<ShortArray> = IosMicrophone().flow()

    private companion object {
        /** About the 50 Hz the Android hub uses. */
        const val UPDATE_INTERVAL_S = 0.02

        /** SENSOR_DELAY_NORMAL of the original orientation sensor. */
        const val ORIENTATION_INTERVAL_S = 0.2
    }
}
