package net.ddns.mlsoftlaberge.trycorder.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Locale

/** The original utils/Fetcher, on current Android APIs and without READ_PHONE_STATE / GET_TASKS. */
class AndroidSystemInfo(private val context: Context) : SystemInfo {
    private val network = NetworkFacts(context)
    private val device = DeviceFacts(context)

    override fun deviceName(): String = Build.MODEL

    override fun deviceRelease(): String = Build.VERSION.RELEASE

    override fun appVersion(): String = device.packageVersion()

    /** fetch_ip_address(): the IPv4 address of the active network. */
    override fun ipAddress(): String = network.ipAddress()

    override fun section(section: InfoSection): String = when (section) {
        InfoSection.Connectivity -> network.connectivity()
        InfoSection.Telephony -> device.telephony()
        InfoSection.Network -> network.networkInfo()
        InfoSection.System -> device.systemProperties()
        InfoSection.OperSys -> device.tasks()
        InfoSection.Package -> device.packageInfo()
        InfoSection.WifiDhcp -> network.dhcpInfo()
        InfoSection.Sensors -> device.sensors()
        InfoSection.Cpu -> readProc("/proc/cpuinfo")
        InfoSection.Memory -> device.memoryInfo() + "\n\n" + readProc("/proc/meminfo")
    }

    override fun memory(): MemoryStats {
        val info = device.memoryState()
        return MemoryStats(total = info.totalMem, free = info.availMem)
    }

    private fun readProc(path: String): String = try {
        File(path).readText()
    } catch (e: IOException) {
        "$path: ${e.message}\n"
    }
}

/** fetch_connectivity, fetch_network_info, fetch_dhcpinfo and the IP address. */
internal class NetworkFacts(private val context: Context) {
    private val connectivity get() = context.getSystemService(ConnectivityManager::class.java)

    private val linkProperties: LinkProperties?
        get() = connectivity?.let { cm -> cm.activeNetwork?.let { cm.getLinkProperties(it) } }

    private val capabilities: NetworkCapabilities?
        get() = connectivity?.let { cm -> cm.activeNetwork?.let { cm.getNetworkCapabilities(it) } }

    fun connectivity(): String =
        if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
            "INTERNET is Active\n"
        } else {
            "INTERNET is Off\n"
        }

    fun ipAddress(): String =
        linkProperties?.linkAddresses?.map { it.address }?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress ?: interfaceAddress()

    fun networkInfo(): String = buildString {
        append("Network type : ").append(networkType()).append('\n')
        append("Wifi IP Addr : ").append(ipAddress()).append('\n')
        append("Wifi MAC Addr : ").append("hidden by Android").append('\n')
        append("Wifi SSID : ").append(wifiInfo()?.ssid ?: "").append('\n')
        append("IP Address : ").append(interfaceAddress()).append('\n')
    }

    /** fetch_dhcpinfo(): the same lines, from LinkProperties instead of the deprecated DhcpInfo. */
    fun dhcpInfo(): String = buildString {
        val link = linkProperties
        val dns = link?.dnsServers.orEmpty().map { it.hostAddress.orEmpty() }
        val ipv4 = link?.linkAddresses.orEmpty().firstOrNull { it.address is Inet4Address }
        val gateway = link?.routes.orEmpty().firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
        append("Locale: ").append(Locale.getDefault()).append('\n')
        append("DNS 1: ").append(dns.getOrElse(0) { "" }).append('\n')
        append("DNS 2: ").append(dns.getOrElse(1) { "" }).append('\n')
        append("Default Gateway: ").append(gateway?.gateway?.hostAddress.orEmpty()).append('\n')
        append("IP Address: ").append(ipv4?.address?.hostAddress.orEmpty()).append('\n')
        append("Lease Time: ").append("not available").append('\n')
        append("Subnet Mask: ").append(ipv4?.prefixLength?.let { prefixToMask(it) }.orEmpty()).append('\n')
        val server = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) link?.dhcpServerAddress?.hostAddress else null
        append("Server IP: ").append(server.orEmpty()).append('\n')
    }

    private fun networkType(): String {
        val caps = capabilities ?: return "none"
        return TRANSPORTS.entries.firstOrNull { caps.hasTransport(it.key) }?.value ?: "other"
    }

    private fun wifiInfo(): WifiInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            capabilities?.transportInfo as? WifiInfo
        } else {
            @Suppress("DEPRECATION") // getConnectionInfo is the only source before Android 10
            context.applicationContext.getSystemService(WifiManager::class.java)?.connectionInfo
        }

    /** NetInfo.getIPAddress(): the first non loopback address of the interfaces. */
    private fun interfaceAddress(): String = try {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }?.hostAddress.orEmpty()
    } catch (e: SocketException) {
        Log.w("Trycorder", "No interfaces: ${e.message}")
        ""
    }

    private companion object {
        const val BITS = 32
        const val BYTE = 0xff
        const val BYTE_BITS = 8
        const val BYTES = 4
        const val IPV4_MASK = 0xffffffffL

        val TRANSPORTS = linkedMapOf(
            NetworkCapabilities.TRANSPORT_WIFI to "wifi",
            NetworkCapabilities.TRANSPORT_CELLULAR to "mobile",
            NetworkCapabilities.TRANSPORT_ETHERNET to "ethernet",
            NetworkCapabilities.TRANSPORT_VPN to "vpn",
            NetworkCapabilities.TRANSPORT_BLUETOOTH to "bluetooth",
        )

        fun prefixToMask(prefix: Int): String {
            val mask = if (prefix == 0) 0L else (-1L shl (BITS - prefix)) and IPV4_MASK
            return (BYTES - 1 downTo 0).joinToString(".") { ((mask shr (it * BYTE_BITS)) and BYTE.toLong()).toString() }
        }
    }
}

/** The device sections of Fetcher: telephony, system properties, tasks, package, sensors, memory. */
internal class DeviceFacts(private val context: Context) {
    private val activityManager get() = context.getSystemService(ActivityManager::class.java)

    /** fetch_tel_status() without READ_PHONE_STATE: identifiers and numbers are not readable. */
    fun telephony(): String = buildString {
        val tm = context.getSystemService(TelephonyManager::class.java)
        if (tm == null) {
            append("No telephony\n")
            return@buildString
        }
        append("NetworkCountryIso = ").append(tm.networkCountryIso).append('\n')
        append("NetworkOperator = ").append(tm.networkOperator).append('\n')
        append("NetworkOperatorName = ").append(tm.networkOperatorName).append('\n')
        append("PhoneType = ").append(tm.phoneType).append('\n')
        append("SimCountryIso = ").append(tm.simCountryIso).append('\n')
        append("SimOperator = ").append(tm.simOperator).append('\n')
        append("SimOperatorName = ").append(tm.simOperatorName).append('\n')
        append("SimState = ").append(tm.simState).append('\n')
        val config = context.resources.configuration
        append("IMSI MCC (Mobile Country Code):").append(config.mcc).append('\n')
        append("IMSI MNC (Mobile Network Code):").append(config.mnc).append('\n')
        append("DeviceId, Line1Number, SimSerialNumber, SubscriberId, VoiceMailNumber: ")
        append("need READ_PHONE_STATE, not requested\n")
    }

    /** fetch_system_info(). */
    fun systemProperties(): String = buildString {
        SYSTEM_PROPERTIES.forEach { append(it).append(':').append(System.getProperty(it.trim())).append('\n') }
    }

    /** fetch_os_info(): getRunningTasks needs GET_TASKS, the app's own tasks are listed instead. */
    fun tasks(): String = buildString {
        activityManager?.appTasks.orEmpty().forEach { task ->
            val info = task.taskInfo
            @Suppress("DEPRECATION") // persistentId is the task id before Android 10
            val id = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.taskId else info.persistentId
            append("id: ").append(id)
            append("\nbaseActivity: ").append(info.baseActivity?.flattenToString())
            append("\nnumActivities: ").append(info.numActivities)
            append("\ndescription: ").append(info.description)
            append("\n\n")
        }
    }

    /** fetch_packinfo(). */
    fun packageInfo(): String = buildString {
        append("Locale: ").append(Locale.getDefault()).append('\n')
        append("Version: ").append(packageVersion()).append('\n')
        append("Package: ").append(context.packageName).append('\n')
        append("Phone Model: ").append(Build.MODEL).append('\n')
        append("Android Version: ").append(Build.VERSION.RELEASE).append('\n')
        append("Board: ").append(Build.BOARD).append('\n')
        append("Brand: ").append(Build.BRAND).append('\n')
        append("Device: ").append(Build.DEVICE).append('\n')
        append("Host: ").append(Build.HOST).append('\n')
        append("ID: ").append(Build.ID).append('\n')
        append("Model: ").append(Build.MODEL).append('\n')
        append("Product: ").append(Build.PRODUCT).append('\n')
        append("Type: ").append(Build.TYPE).append('\n')
        val stat = StatFs(Environment.getDataDirectory().path)
        append("Total Internal memory: ").append(stat.totalBytes).append('\n')
        append("Available Internal memory: ").append(stat.availableBytes).append('\n')
    }

    fun packageVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    } catch (e: PackageManager.NameNotFoundException) {
        Log.w("Trycorder", "No package info: ${e.message}")
        ""
    }

    /** fetch_sensors_list(). */
    fun sensors(): String = buildString {
        val manager = context.getSystemService(SensorManager::class.java)
        manager?.getSensorList(Sensor.TYPE_ALL).orEmpty().forEach {
            append(it.name).append(" - ").append(it.vendor).append(" - ").append(it.type).append('\n')
        }
    }

    fun memoryState(): ActivityManager.MemoryInfo =
        ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) }

    /** First half of fetch_memory_info(). */
    fun memoryInfo(): String {
        val info = memoryState()
        return "\nTotal Available Memory :${info.availMem shr KILO}k" +
            "\nTotal Available Memory :${info.availMem shr MEGA}M" +
            "\nIn low memory situation:${info.lowMemory}"
    }

    private companion object {
        const val KILO = 10
        const val MEGA = 20

        val SYSTEM_PROPERTIES = listOf(
            "java.vendor.url", "java.class.path", "user.home", "java.class.version", "os.version", "java.vendor",
            "user.dir", "user.timezone", "path.separator", " os.name", "os.arch", "line.separator", "file.separator",
            "user.name", "java.version", "java.home",
        )
    }
}
