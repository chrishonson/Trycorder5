package net.ddns.mlsoftlaberge.trycorder.sensors

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Placeholder: replaced by the SensorManager / LocationManager / AudioRecord implementation. */
class AndroidSensorHub(private val context: Context) : SensorHub {
    override fun magnetic(): Flow<Vector3> = emptyFlow()
    override fun orientation(): Flow<Vector3> = emptyFlow()
    override fun acceleration(): Flow<Vector3> = emptyFlow()
    override fun environment(): Flow<Environment> = emptyFlow()
    override fun location(): Flow<GeoLocation> = emptyFlow()
    override fun audio(): Flow<ShortArray> = emptyFlow()
    override fun toString() = "AndroidSensorHub(${context.packageName})"
}
