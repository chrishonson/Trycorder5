package net.ddns.mlsoftlaberge.trycorder.link

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.Executors

/**
 * NSD (DNS-SD) registration of this Trycorder as `_trycorder._tcp` on port 1701 and discovery of the
 * others, feeding the local peer list (registerService / startdiscoverService of the old service).
 * A Wi-Fi multicast lock is held while discovering so mDNS answers are not filtered out.
 */
internal class NsdPeerDiscovery(context: Context, private val engine: LinkEngine) {
    private val nsd: NsdManager? = context.getSystemService(NsdManager::class.java)
    private val wifi: WifiManager? = context.getSystemService(WifiManager::class.java)
    private val executor = Executors.newSingleThreadExecutor()
    private var multicastLock: WifiManager.MulticastLock? = null
    private var registration: NsdManager.RegistrationListener? = null
    private var discovery: NsdManager.DiscoveryListener? = null

    @Volatile
    private var ownName: String = ""

    fun start(deviceName: String) {
        val manager = nsd ?: return engine.status("Network service discovery unavailable")
        ownName = deviceName.ifBlank { DEFAULT_NAME }
        multicastLock = wifi?.createMulticastLock(LOCK_TAG)?.apply {
            setReferenceCounted(false)
            acquire()
        }
        val info = NsdServiceInfo().apply {
            serviceName = ownName
            serviceType = LinkProtocol.SERVICE_TYPE
            port = LinkProtocol.PORT
        }
        engine.status("Register service")
        registration = Registration().also { manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, it) }
        engine.status("Discover services")
        discovery = Discovery(manager).also {
            manager.discoverServices(LinkProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, it)
        }
    }

    fun stop() {
        val manager = nsd ?: return
        registration?.let { listener -> runCatching { manager.unregisterService(listener) } }
        discovery?.let { listener -> runCatching { manager.stopServiceDiscovery(listener) } }
        registration = null
        discovery = null
        multicastLock?.release()
        multicastLock = null
    }

    private fun resolve(manager: NsdManager, service: NsdServiceInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            manager.registerServiceInfoCallback(service, executor, InfoCallback(manager))
        } else {
            @Suppress("DEPRECATION") // replaced by registerServiceInfoCallback from API 34, used above
            manager.resolveService(service, Resolver())
        }
    }

    private fun found(name: String, addresses: List<InetAddress>) {
        val address = addresses.firstOrNull { it is Inet4Address } ?: addresses.firstOrNull() ?: return
        address.hostAddress?.let { engine.addLocalPeer(Peer(it, LinkProtocol.cleanServiceName(name))) }
    }

    private inner class Registration : NsdManager.RegistrationListener {
        override fun onServiceRegistered(info: NsdServiceInfo) {
            ownName = info.serviceName
            engine.status("Registration done: ${info.serviceName}")
        }

        override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
            engine.status("Registration failed: ${info.serviceName}")
        }

        override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
        override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
    }

    private inner class Discovery(private val manager: NsdManager) : NsdManager.DiscoveryListener {
        override fun onServiceFound(service: NsdServiceInfo) {
            if (service.serviceName == ownName) return
            resolve(manager, service)
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            engine.removeLocalPeer(LinkProtocol.cleanServiceName(service.serviceName))
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            engine.status("Discovery failed: $errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String) = Unit
        override fun onDiscoveryStopped(serviceType: String) = Unit
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
    }

    @Suppress("DEPRECATION") // the pre-API 34 resolver and NsdServiceInfo.host
    private inner class Resolver : NsdManager.ResolveListener {
        override fun onServiceResolved(info: NsdServiceInfo) {
            info.host?.let { found(info.serviceName, listOf(it)) }
        }

        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private inner class InfoCallback(private val manager: NsdManager) : NsdManager.ServiceInfoCallback {
        override fun onServiceUpdated(info: NsdServiceInfo) {
            found(info.serviceName, info.hostAddresses)
            runCatching { manager.unregisterServiceInfoCallback(this) }
        }

        override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) = Unit
        override fun onServiceLost() = Unit
        override fun onServiceInfoCallbackUnregistered() = Unit
    }

    private companion object {
        const val DEFAULT_NAME = "Trycorder"
        const val LOCK_TAG = "trycorder-nsd"
    }
}
