package net.ddns.mlsoftlaberge.trycorder.link

import net.ddns.mlsoftlaberge.trycorder.core.InfoSection
import net.ddns.mlsoftlaberge.trycorder.core.MemoryStats
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.Speaker
import net.ddns.mlsoftlaberge.trycorder.core.SystemInfo
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound

class FakeSystemInfo(private val ip: String = "10.0.0.9") : SystemInfo {
    override fun deviceName() = "Pixel"
    override fun deviceRelease() = "15"
    override fun appVersion() = "6.0.0"
    override fun ipAddress() = ip
    override fun section(section: InfoSection) = ""
    override fun memory() = MemoryStats(0, 0)
}

class RecordingSound : SoundPlayer {
    val played = mutableListOf<TrekSound>()
    override fun play(sound: TrekSound) {
        played += sound
    }
    override fun startLoop(sound: TrekSound) = Unit
    override fun stopLoop() = Unit
    override fun release() = Unit
}

class RecordingSpeaker : Speaker {
    val spoken = mutableListOf<Pair<String, TrekLanguage?>>()
    override fun speak(text: String, language: TrekLanguage?) {
        spoken += text to language
    }
    override fun setLanguage(language: TrekLanguage) = Unit
    override fun shutdown() = Unit
}
