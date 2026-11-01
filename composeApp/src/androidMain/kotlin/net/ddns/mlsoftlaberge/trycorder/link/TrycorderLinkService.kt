package net.ddns.mlsoftlaberge.trycorder.link

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.MainActivity
import net.ddns.mlsoftlaberge.trycorder.TrycorderApplication

/**
 * The foreground service that keeps the starship link alive (the old TrycorderService), with its
 * notification "Talk Server running on <ip>:1701" listing the local Trycorders.
 *
 * Foreground service type: connectedDevice. The service keeps TCP/UDP links to other Trycorders and
 * to the tryserver over Wi-Fi and announces itself with multicast DNS-SD, which is what this type
 * covers (its prerequisite permission here is CHANGE_WIFI_MULTICAST_STATE, used for the NSD multicast
 * lock). It is also allowed when started from BOOT_COMPLETED on Android 15, unlike dataSync,
 * mediaPlayback, microphone, camera, phoneCall and mediaProjection, so the autoBoot setting keeps
 * working; and it has no run time limit, unlike dataSync on Android 15. Push-to-talk records only
 * while the user holds the button with the app on screen, so no microphone type is needed.
 */
class TrycorderLinkService : Service() {
    private var link: AndroidStarshipLink? = null
    private var scope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        goForeground(notification(STARTING))
        val services = (application as TrycorderApplication).services
        link = (services.link as? AndroidStarshipLink)?.also { it.onServiceCreated() }
        scope = MainScope().apply {
            launch {
                services.link.peers.collect { peers ->
                    goForeground(notification(peers.local.joinToString("\n") { "${it.ip} - ${it.name}" }))
                }
            }
        }
        Toast.makeText(this, "Trycorder Service Started.", Toast.LENGTH_LONG).show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope?.cancel()
        link?.onServiceDestroyed()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        Toast.makeText(this, "Trycorder Service Destroyed", Toast.LENGTH_LONG).show()
        super.onDestroy()
    }

    private fun goForeground(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )
    }

    private fun createChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName("Trycorder link")
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun notification(text: String): Notification {
        val services = (application as TrycorderApplication).services
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Trycorder ${services.settings.current.deviceName}")
            .setContentText("Talk Server running on ${services.systemInfo.ipAddress()}:${LinkProtocol.PORT}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(applicationInfo.icon)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private companion object {
        /** NCC-1701, like the original notification id. */
        const val NOTIFICATION_ID = 1701
        const val CHANNEL_ID = "trycorder_link"
        const val STARTING = "Starting..."
    }
}
