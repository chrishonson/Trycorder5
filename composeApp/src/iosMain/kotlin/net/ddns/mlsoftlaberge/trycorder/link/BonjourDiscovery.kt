package net.ddns.mlsoftlaberge.trycorder.link

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSNetService
import platform.Foundation.NSNetServiceBrowser
import platform.Foundation.NSNetServiceBrowserDelegateProtocol
import platform.Foundation.NSNetServiceDelegateProtocol
import platform.darwin.NSObject
import platform.posix.memcpy

/**
 * Bonjour registration of this Trycorder as `_trycorder._tcp` on port 1701 and discovery of the others
 * with NSNetService / NSNetServiceBrowser, feeding the local peer list of the [LinkEngine].
 * Runs on the main run loop (start/stop are called from the main thread).
 */
internal class BonjourDiscovery(private val engine: LinkEngine) {
    private var published: NSNetService? = null
    private var browser: NSNetServiceBrowser? = null
    private var ownName: String = ""
    private val resolving = mutableListOf<NSNetService>()
    private val resolver = ResolveDelegate { service -> onResolved(service) }
    private val browserDelegate = BrowserDelegate { service -> onFound(service) }

    fun start(deviceName: String) {
        ownName = deviceName.ifBlank { DEFAULT_NAME }
        engine.status("Register service")
        published = NSNetService(
            domain = DOMAIN,
            type = LinkProtocol.SERVICE_TYPE,
            name = ownName,
            port = LinkProtocol.PORT,
        ).also { it.publish() }
        engine.status("Discover services")
        browser = NSNetServiceBrowser().also {
            it.delegate = browserDelegate
            it.searchForServicesOfType(LinkProtocol.SERVICE_TYPE, inDomain = DOMAIN)
        }
    }

    fun stop() {
        browser?.stop()
        published?.stop()
        resolving.forEach { it.stop() }
        resolving.clear()
        browser = null
        published = null
    }

    private fun onFound(service: NSNetService) {
        if (service.name == ownName) return
        resolving += service
        service.delegate = resolver
        service.resolveWithTimeout(RESOLVE_TIMEOUT_SECONDS)
    }

    private fun onResolved(service: NSNetService) {
        resolving -= service
        val ip = service.addresses.orEmpty()
            .filterIsInstance<NSData>()
            .firstNotNullOfOrNull { SockAddr.ipv4(it.toByteArray()) }
            ?: service.hostName
            ?: return
        engine.addLocalPeer(Peer(ip, LinkProtocol.cleanServiceName(service.name)))
    }

    private class BrowserDelegate(private val onFound: (NSNetService) -> Unit) :
        NSObject(), NSNetServiceBrowserDelegateProtocol {
        override fun netServiceBrowser(
            browser: NSNetServiceBrowser,
            didFindService: NSNetService,
            moreComing: Boolean,
        ) {
            onFound(didFindService)
        }
    }

    private class ResolveDelegate(private val onResolved: (NSNetService) -> Unit) :
        NSObject(), NSNetServiceDelegateProtocol {
        override fun netServiceDidResolveAddress(sender: NSNetService) {
            onResolved(sender)
        }
    }

    private companion object {
        const val DOMAIN = "local."
        const val DEFAULT_NAME = "Trycorder"
        const val RESOLVE_TIMEOUT_SECONDS = 5.0
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val out = ByteArray(size)
    if (size > 0) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}
