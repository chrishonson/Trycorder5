package net.ddns.mlsoftlaberge.trycorder.core

/** Sound effects from the original res/raw, now in composeResources/files/sounds. */
enum class TrekSound(val fileName: String) {
    Alert("alert15.mp3"),
    BeamOut("beam1a.wav"),
    BeamIn("beam1b.wav"),
    CommClose("commclose.mp3"),
    CommOpen("commopen.wav"),
    ComputerBeep39("computerbeep_39.mp3"),
    ComputerBeep55("computerbeep_55.mp3"),
    Deny("denybeep1.mp3"),
    DestructSequence("destructsequence.mp3"),
    DoNotAddress("donotaddressthisunitinthatmanner_clean.mp3"),
    KeyOk("keyok2.mp3"),
    LongRangeScan("long_range_scan.mp3"),
    Phaser("phasertype2.wav"),
    Torpedo("photorp1.wav"),
    ScanHigh("scan_high.wav"),
    ScanLow("scan_low.wav"),
    ShieldDown("shielddown.wav"),
    ShieldUp("shieldup.mp3"),
    RedAlert("tng_red_alert1.mp3"),
    SlowWarp("tng_slowwarp_clean2.mp3"),
    Tractor("tng_tractor_clean.mp3"),
    Warp("tng_warp5_clean.mp3"),
    TricorderScan("tricscan2.wav"),
    Core("voy_core_2.mp3");

    /** Path for Res.getUri / Res.readBytes. */
    val resourcePath: String get() = "files/sounds/$fileName"
}

/**
 * Plays the Trycorder sound effects.
 * [play] replaces the previous one-shot effect like the original playsound(); [startLoop] runs
 * one looping background sound (the sensor scanning sounds).
 */
interface SoundPlayer {
    fun play(sound: TrekSound)
    fun startLoop(sound: TrekSound)
    fun stopLoop()
    fun release()
}

object SilentSoundPlayer : SoundPlayer {
    override fun play(sound: TrekSound) = Unit
    override fun startLoop(sound: TrekSound) = Unit
    override fun stopLoop() = Unit
    override fun release() = Unit
}
