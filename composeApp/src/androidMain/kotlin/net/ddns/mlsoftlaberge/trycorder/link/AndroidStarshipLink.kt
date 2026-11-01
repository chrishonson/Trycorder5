package net.ddns.mlsoftlaberge.trycorder.link

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.Speaker
import net.ddns.mlsoftlaberge.trycorder.core.SystemInfo

/**
 * Android starship link. [start] starts [TrycorderLinkService], a foreground service that runs the
 * shared [LinkEngine], NSD discovery and the push-to-talk server while it is alive; [stop] stops it.
 */
class AndroidStarshipLink(
    context: Context,
    private val settings: SettingsRepository,
    systemInfo: SystemInfo,
    sound: SoundPlayer,
    speaker: Speaker,
) : StarshipLink {
    private val appContext = context.applicationContext
    private val engine = LinkEngine(
        settings = settings,
        systemInfo = systemInfo,
        sound = sound,
        speaker = speaker,
        config = LinkConfig(io = Dispatchers.IO, effects = Dispatchers.Main),
    )
    private val discovery = NsdPeerDiscovery(appContext, engine)
    private val talk = TalkAudio(appContext, engine)

    override val events: Flow<LinkEvent> = engine.events
    override val peers: StateFlow<PeerList> = engine.peers
    override val stats: StateFlow<ServerStats> = engine.stats
    override val running: StateFlow<Boolean> = engine.running

    override fun start() {
        try {
            ContextCompat.startForegroundService(appContext, serviceIntent())
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException: started from the background without an exemption.
            Log.w(TAG, "cannot start the link service", e)
            engine.status("Cant start trycorder service")
        }
    }

    override fun stop() {
        appContext.stopService(serviceIntent())
    }

    override fun send(text: String) = engine.send(text)

    override fun startAudioStream() = talk.startStreaming(engine.otherLocalPeers())

    override fun stopAudioStream() = talk.stopStreaming()

    /** Called by the service once it is in the foreground: onCreate() of the old service. */
    internal fun onServiceCreated() {
        engine.start()
        talk.startServer()
        discovery.start(settings.current.deviceName)
    }

    /** Called by the service when it is destroyed: onDestroy() of the old service. */
    internal fun onServiceDestroyed() {
        discovery.stop()
        talk.stopStreaming()
        talk.stopServer()
        engine.stop()
    }

    private fun serviceIntent() = Intent(appContext, TrycorderLinkService::class.java)

    private companion object {
        const val TAG = "StarshipLink"
    }
}
