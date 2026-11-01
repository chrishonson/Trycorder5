package net.ddns.mlsoftlaberge.trycorder.sensors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Placeholder: replaced by the CoreMotion / CoreLocation / AVAudioEngine implementation. */
class IosSensorHub : SensorHub {
    override fun magnetic(): Flow<Vector3> = emptyFlow()
    override fun orientation(): Flow<Vector3> = emptyFlow()
    override fun acceleration(): Flow<Vector3> = emptyFlow()
    override fun environment(): Flow<Environment> = emptyFlow()
    override fun location(): Flow<GeoLocation> = emptyFlow()
    override fun audio(): Flow<ShortArray> = emptyFlow()
}
