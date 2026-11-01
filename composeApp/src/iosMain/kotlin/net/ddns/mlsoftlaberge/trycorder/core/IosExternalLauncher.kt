package net.ddns.mlsoftlaberge.trycorder.core

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosExternalLauncher : ExternalLauncher {
    override fun openMap(latitude: Double, longitude: Double): Boolean =
        openUrl("https://maps.apple.com/?ll=$latitude,$longitude")

    override fun openUrl(url: String): Boolean {
        val nsUrl = NSURL.URLWithString(url) ?: return false
        UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any>(), null)
        return true
    }
}
