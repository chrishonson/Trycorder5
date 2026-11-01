package net.ddns.mlsoftlaberge.trycorder.core

import platform.UIKit.UIDevice

/** Placeholder: the full iOS equivalent of utils/Fetcher replaces this. */
class IosSystemInfo : SystemInfo {
    override fun deviceName(): String = UIDevice.currentDevice.name
    override fun deviceRelease(): String = UIDevice.currentDevice.systemVersion
    override fun appVersion(): String = ""
    override fun ipAddress(): String = ""
    override fun section(section: InfoSection): String = ""
    override fun memory(): MemoryStats = MemoryStats(0, 0)
}
