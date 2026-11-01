package net.ddns.mlsoftlaberge.trycorder.core

import android.content.Context
import android.os.Build

/** Placeholder: the full port of utils/Fetcher replaces this. */
class AndroidSystemInfo(private val context: Context) : SystemInfo {
    override fun deviceName(): String = Build.MODEL
    override fun deviceRelease(): String = Build.VERSION.RELEASE
    override fun appVersion(): String = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    override fun ipAddress(): String = ""
    override fun section(section: InfoSection): String = ""
    override fun memory(): MemoryStats = MemoryStats(0, 0)
}
