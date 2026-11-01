package net.ddns.mlsoftlaberge.trycorder.sensors

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject

/** CLLocationManager with when-in-use authorization; emits nothing while it is denied. */
internal class IosLocation {
    fun flow(): Flow<GeoLocation> = callbackFlow {
        val manager = CLLocationManager()
        val delegate = Delegate { trySend(it) }
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = MIN_DISTANCE_M
        delegate.locationManagerDidChangeAuthorization(manager)
        awaitClose {
            manager.stopUpdatingLocation()
            // The delegate property is weak: referencing it here keeps it alive until now.
            if (manager.delegate === delegate) manager.delegate = null
        }
    }.flowOn(Dispatchers.Main)

    @OptIn(ExperimentalForeignApi::class)
    private class Delegate(
        private val onLocation: (GeoLocation) -> Unit,
    ) : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
            location.coordinate.useContents { onLocation(GeoLocation(latitude, longitude, location.altitude)) }
        }

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            when (manager.authorizationStatus) {
                kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
                kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways ->
                    manager.startUpdatingLocation()
                else -> manager.stopUpdatingLocation()
            }
        }
    }

    private companion object {
        /** OriSensorView asked for updates every metre. */
        const val MIN_DISTANCE_M = 1.0
    }
}
