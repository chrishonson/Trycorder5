package net.ddns.mlsoftlaberge.trycorder.link

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.Speaker
import net.ddns.mlsoftlaberge.trycorder.core.SystemInfo

/** Placeholder: replaced by the shared ktor-network link. */
@Suppress("UnusedPrivateProperty")
class IosStarshipLink(
    private val settings: SettingsRepository,
    private val systemInfo: SystemInfo,
    private val sound: SoundPlayer,
    private val speaker: Speaker,
) : StarshipLink {
    override val events: Flow<LinkEvent> = emptyFlow()
    override val peers: StateFlow<PeerList> = MutableStateFlow(PeerList())
    override val stats: StateFlow<ServerStats> = MutableStateFlow(ServerStats())
    override val running: StateFlow<Boolean> = MutableStateFlow(false)
    override fun start() = Unit
    override fun stop() = Unit
    override fun send(text: String) = Unit
    override fun startAudioStream() = Unit
    override fun stopAudioStream() = Unit
}
