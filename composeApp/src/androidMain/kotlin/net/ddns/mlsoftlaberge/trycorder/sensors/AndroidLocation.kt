package net.ddns.mlsoftlaberge.trycorder.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Platform LocationManager updates (no Google Play services, the app is on F-Droid), every
 * 400 ms or 1 m like OriSensorView. Emits nothing while the location permission is denied.
 */
internal class AndroidLocation(private val context: Context) {
    // Permissions are checked in usableProviders() right before the calls.
    @SuppressLint("MissingPermission")
    fun flow(): Flow<GeoLocation> = callbackFlow {
        val manager = context.getSystemService(LocationManager::class.java)
        val providers = usableProviders(manager)
        val listener = Listener { trySend(it.toGeo()) }
        providers.mapNotNull { manager?.getLastKnownLocation(it) }.maxByOrNull { it.time }?.let { trySend(it.toGeo()) }
        providers.forEach {
            manager?.requestLocationUpdates(it, MIN_TIME_MS, MIN_DISTANCE_M, listener, Looper.getMainLooper())
        }
        awaitClose { manager?.removeUpdates(listener) }
    }

    private fun usableProviders(manager: LocationManager?): List<String> {
        val fine = AndroidSensorHub.granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = AndroidSensorHub.granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (manager == null || !(fine || coarse)) return emptyList()
        return manager.getProviders(true).filter {
            it == LocationManager.NETWORK_PROVIDER || (fine && it == LocationManager.GPS_PROVIDER)
        }
    }

    private fun Location.toGeo() = GeoLocation(latitude, longitude, altitude)

    /**
     * All four callbacks are implemented: before API 30 they have no default implementation
     * and a lambda listener would crash when the framework calls them.
     */
    private class Listener(private val onLocation: (Location) -> Unit) : LocationListener {
        override fun onLocationChanged(location: Location) = onLocation(location)

        @Deprecated("Still abstract on the API 24-28 devices the app supports")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit
    }

    private companion object {
        const val MIN_TIME_MS = 400L
        const val MIN_DISTANCE_M = 1f
    }
}
