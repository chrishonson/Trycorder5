package net.ddns.mlsoftlaberge.trycorder.link

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import net.ddns.mlsoftlaberge.trycorder.TrycorderApplication

/**
 * Port of TrycorderReceiver: on boot, start the starship link service when the autoBoot setting is on.
 * BOOT_COMPLETED is one of the exemptions that allow starting a foreground service from the background.
 */
class TrycorderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Toast.makeText(context, "Trycorder Boot Detected.", Toast.LENGTH_LONG).show()
        val services = (context.applicationContext as TrycorderApplication).services
        if (services.settings.current.autoBoot) services.link.start()
    }
}
