package net.ddns.mlsoftlaberge.trycorder.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

class AndroidExternalLauncher(private val context: Context) : ExternalLauncher {
    override fun openMap(latitude: Double, longitude: Double): Boolean = open(Uri.parse("geo:$latitude,$longitude"))

    override fun openUrl(url: String): Boolean = open(Uri.parse(url))

    private fun open(uri: Uri): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (expected: ActivityNotFoundException) {
        false
    }
}
