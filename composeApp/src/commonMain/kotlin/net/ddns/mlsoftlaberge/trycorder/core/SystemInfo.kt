package net.ddns.mlsoftlaberge.trycorder.core

/** Sections of the original Fetcher utility shown in the Logs Info / Sys viewer pages. */
enum class InfoSection(val title: String) {
    Connectivity("Connectivity"),
    Telephony("Telephony"),
    Network("Network"),
    System("System"),
    OperSys("OperSys"),
    Package("Package"),
    WifiDhcp("Wifi-Dhcp"),
    Sensors("Sensors List"),
    Cpu("CPU Info"),
    Memory("Memory Info"),
}

/** Device and system facts (the original utils/Fetcher). */
interface SystemInfo {
    fun deviceName(): String
    fun deviceRelease(): String
    fun appVersion(): String
    fun ipAddress(): String
    fun section(section: InfoSection): String

    /** Total and available memory in bytes (the Logs Stat page). */
    fun memory(): MemoryStats
}

data class MemoryStats(val total: Long, val free: Long)

private const val RULE = "--------------------"

/** Formats several sections the way the original viewer did. */
fun SystemInfo.report(vararg sections: InfoSection): String = buildString {
    sections.forEach { s ->
        append(RULE).append('\n').append(s.title).append('\n').append(RULE).append('\n')
        append(section(s))
        if (!endsWith("\n")) append('\n')
    }
}

/** Opens things outside the app: maps at a position, the system settings, a web page. */
interface ExternalLauncher {
    /** Returns false when no app can show the location. */
    fun openMap(latitude: Double, longitude: Double): Boolean
    fun openUrl(url: String): Boolean
}
