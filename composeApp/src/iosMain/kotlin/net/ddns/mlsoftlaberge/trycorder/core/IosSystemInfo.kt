package net.ddns.mlsoftlaberge.trycorder.core

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.CoreMotion.CMAltimeter
import platform.CoreMotion.CMMotionManager
import platform.CoreMotion.CMPedometer
import platform.CoreTelephony.CTTelephonyNetworkInfo
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSFileSystemSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSLocale
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.localTimeZone
import platform.Foundation.localeIdentifier
import platform.Foundation.lowPowerModeEnabled
import platform.Foundation.thermalState
import platform.UIKit.UIDevice
import platform.darwin.HOST_VM_INFO64
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.darwin.inet_ntop
import platform.darwin.KERN_SUCCESS
import platform.darwin.host_statistics64
import platform.darwin.integer_tVar
import platform.darwin.mach_host_self
import platform.darwin.vm_statistics64_data_t
import platform.posix.AF_INET
import platform.posix.INET_ADDRSTRLEN
import platform.posix.getpagesize
import platform.posix.sockaddr_in
import platform.posix.uname
import platform.posix.utsname

/** The iOS equivalent of utils/Fetcher. */
@OptIn(ExperimentalForeignApi::class)
class IosSystemInfo : SystemInfo {
    private val device = IosDeviceFacts()

    override fun deviceName(): String = UIDevice.currentDevice.name

    override fun deviceRelease(): String = UIDevice.currentDevice.systemVersion

    override fun appVersion(): String =
        NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""

    /** The Wi-Fi address (en0), else the cellular one (pdp_ip0). */
    override fun ipAddress(): String {
        val addresses = Interfaces.ipv4()
        return addresses["en0"] ?: addresses["pdp_ip0"] ?: addresses.values.firstOrNull().orEmpty()
    }

    override fun section(section: InfoSection): String = when (section) {
        InfoSection.Connectivity -> if (ipAddress().isNotEmpty()) "INTERNET is Active\n" else "INTERNET is Off\n"
        InfoSection.Telephony -> device.telephony()
        InfoSection.Network -> network()
        InfoSection.System -> device.system()
        InfoSection.OperSys -> device.operatingSystem()
        InfoSection.Package -> device.packageInfo(appVersion())
        InfoSection.WifiDhcp -> wifi()
        InfoSection.Sensors -> device.sensors()
        InfoSection.Cpu -> device.cpu()
        InfoSection.Memory -> device.memoryText(memory())
    }

    override fun memory(): MemoryStats = MemoryStats(
        total = NSProcessInfo.processInfo.physicalMemory.toLong(),
        free = Interfaces.freeMemory(),
    )

    private fun network(): String = buildString {
        val addresses = Interfaces.ipv4()
        val type = when {
            "en0" in addresses -> "wifi"
            "pdp_ip0" in addresses -> "mobile"
            else -> "none"
        }
        append("Network type : ").append(type).append('\n')
        append("Wifi IP Addr : ").append(addresses["en0"].orEmpty()).append('\n')
        append("Wifi MAC Addr : hidden by iOS\n")
        append("Wifi SSID : needs the Access WiFi Information entitlement\n")
        append("IP Address : ").append(ipAddress()).append('\n')
        addresses.forEach { (name, ip) -> append(name).append(" : ").append(ip).append('\n') }
    }

    private fun wifi(): String = buildString {
        append("Locale: ").append(NSLocale.currentLocale.localeIdentifier).append('\n')
        append("IP Address: ").append(Interfaces.ipv4()["en0"].orEmpty()).append('\n')
        append("DNS, Default Gateway, Lease Time, Server IP: not available on iOS\n")
    }
}

/** getifaddrs() and host_statistics64(). */
@OptIn(ExperimentalForeignApi::class)
internal object Interfaces {
    fun ipv4(): Map<String, String> = memScoped {
        val result = linkedMapOf<String, String>()
        val list = alloc<CPointerVar<ifaddrs>>()
        if (getifaddrs(list.ptr) != 0) return@memScoped result
        var cursor = list.value
        val buffer = allocArray<ByteVar>(INET_ADDRSTRLEN)
        while (cursor != null) {
            val entry = cursor.pointed
            val address = entry.ifa_addr
            if (address != null && address.pointed.sa_family.toInt() == AF_INET) {
                val ipv4 = address.reinterpret<sockaddr_in>().pointed
                inet_ntop(AF_INET, ipv4.sin_addr.ptr, buffer, INET_ADDRSTRLEN.convert())
                val name = entry.ifa_name?.toKString().orEmpty()
                val ip = buffer.toKString()
                if (name != "lo0" && name !in result) result[name] = ip
            }
            cursor = entry.ifa_next
        }
        freeifaddrs(list.value)
        result
    }

    /** Free plus inactive pages, like the "available" memory of Android. */
    fun freeMemory(): Long = memScoped {
        val stats = alloc<vm_statistics64_data_t>()
        val count = alloc<UIntVar>()
        count.value = (sizeOf<vm_statistics64_data_t>() / sizeOf<integer_tVar>()).convert()
        val status = host_statistics64(mach_host_self(), HOST_VM_INFO64, stats.ptr.reinterpret(), count.ptr)
        if (status != KERN_SUCCESS) return@memScoped 0L
        (stats.free_count.toLong() + stats.inactive_count.toLong()) * getpagesize()
    }

    fun machine(): String = memScoped {
        val name = alloc<utsname>()
        uname(name.ptr)
        name.machine.toKString()
    }
}

/** The device sections: telephony, process, system, package, sensors, CPU and memory. */
@OptIn(ExperimentalForeignApi::class)
internal class IosDeviceFacts {
    private val process get() = NSProcessInfo.processInfo

    fun telephony(): String = buildString {
        val radios = CTTelephonyNetworkInfo().serviceCurrentRadioAccessTechnology.orEmpty()
        if (radios.isEmpty()) append("No cellular service\n")
        radios.forEach { (service, radio) -> append("Radio ").append(service).append(" = ").append(radio).append('\n') }
        append("Carrier, IMEI, IMSI, phone number: not available on iOS\n")
    }

    fun system(): String = buildString {
        append("process.name:").append(process.processName).append('\n')
        append("host.name:").append(process.hostName).append('\n')
        append("os.version:").append(process.operatingSystemVersionString).append('\n')
        append("os.arch:").append(Interfaces.machine()).append('\n')
        append("user.home:").append(NSHomeDirectory()).append('\n')
        append("user.timezone:").append(NSTimeZone.localTimeZone.name).append('\n')
        append("system.uptime:").append(process.systemUptime.toLong()).append(" s\n")
        append("low.power.mode:").append(process.lowPowerModeEnabled).append('\n')
    }

    fun operatingSystem(): String = buildString {
        val device = UIDevice.currentDevice
        append("System: ").append(device.systemName).append(' ').append(device.systemVersion).append('\n')
        append("Model: ").append(device.model).append('\n')
        append("Localized Model: ").append(device.localizedModel).append('\n')
        append("Vendor Id: ").append(device.identifierForVendor?.UUIDString.orEmpty()).append('\n')
        append("Process Id: ").append(process.processIdentifier).append('\n')
    }

    fun packageInfo(version: String): String = buildString {
        val device = UIDevice.currentDevice
        append("Locale: ").append(NSLocale.currentLocale.localeIdentifier).append('\n')
        append("Version: ").append(version).append('\n')
        append("Package: ").append(NSBundle.mainBundle.bundleIdentifier.orEmpty()).append('\n')
        append("Phone Model: ").append(device.model).append('\n')
        append("iOS Version: ").append(device.systemVersion).append('\n')
        append("Device: ").append(Interfaces.machine()).append('\n')
        val attributes = NSFileManager.defaultManager.attributesOfFileSystemForPath(NSHomeDirectory(), null)
        append("Total Internal memory: ").append(attributes?.get(NSFileSystemSize)).append('\n')
        append("Available Internal memory: ").append(attributes?.get(NSFileSystemFreeSize)).append('\n')
    }

    fun sensors(): String = buildString {
        val motion = CMMotionManager()
        append("Accelerometer - Apple - ").append(motion.accelerometerAvailable).append('\n')
        append("Gyroscope - Apple - ").append(motion.gyroAvailable).append('\n')
        append("Magnetometer - Apple - ").append(motion.magnetometerAvailable).append('\n')
        append("Device Motion - Apple - ").append(motion.deviceMotionAvailable).append('\n')
        append("Barometer - Apple - ").append(CMAltimeter.isRelativeAltitudeAvailable()).append('\n')
        append("Pedometer - Apple - ").append(CMPedometer.isStepCountingAvailable()).append('\n')
    }

    fun cpu(): String = buildString {
        append("machine: ").append(Interfaces.machine()).append('\n')
        append("processors: ").append(process.processorCount).append('\n')
        append("active processors: ").append(process.activeProcessorCount).append('\n')
        append("thermal state: ").append(process.thermalState).append('\n')
    }

    fun memoryText(stats: MemoryStats): String =
        "\nTotal Available Memory :${stats.free shr KILO}k" +
            "\nTotal Available Memory :${stats.free shr MEGA}M" +
            "\nTotal Memory :${stats.total shr MEGA}M\n"

    private companion object {
        const val KILO = 10
        const val MEGA = 20
    }
}
