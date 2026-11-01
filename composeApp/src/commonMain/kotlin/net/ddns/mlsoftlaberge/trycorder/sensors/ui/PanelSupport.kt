package net.ddns.mlsoftlaberge.trycorder.sensors.ui

import net.ddns.mlsoftlaberge.trycorder.sensors.Vector3
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/**
 * The scrolling value table of MagSensorView/GraSensorView: keeps the last [capacity] readings,
 * dropping the oldest one when full.
 */
class TraceHistory(private val capacity: Int) {
    private val buffer = ArrayDeque<Vector3>(capacity)

    val size: Int get() = buffer.size

    fun add(value: Vector3) {
        if (buffer.size >= capacity) buffer.removeFirst()
        buffer.addLast(value)
    }

    operator fun get(index: Int): Vector3 = buffer[index]

    fun toList(): List<Vector3> = buffer.toList()
}

/** String.format("%.Nf") without a platform formatter: rounds half up, always uses a dot. */
object FixedFormat {
    private const val TEN = 10.0
    private const val HALF = 0.5

    fun format(value: Float, decimals: Int): String {
        val factor = TEN.pow(decimals).toLong()
        val scaled = floor(abs(value.toDouble()) * factor + HALF).toLong()
        val sign = if (value < 0f && scaled != 0L) "-" else ""
        val whole = scaled / factor
        return if (decimals == 0) {
            "$sign$whole"
        } else {
            "$sign$whole." + (scaled % factor).toString().padStart(decimals, '0')
        }
    }
}

/** Transporter, shields and tractor animations: what the original timers did on each tick. */
object PanelAnimation {
    /** Shield and transporter fade: grid/person alpha for [position] 0..255. */
    fun fadeAlpha(position: Int, raising: Boolean): Int {
        val alpha = when {
            position == 0 -> if (raising) MAX_ALPHA else 0
            raising -> position
            else -> MAX_ALPHA - position
        }
        return alpha.coerceIn(0, MAX_ALPHA)
    }

    /** Next frequency of a rotating beam: 0..10 then back to 0. */
    fun nextFreq(freq: Int): Int = if (freq + 1 > MAX_FREQ) 0 else freq + 1

    const val MAX_ALPHA = 255
    const val MAX_FREQ = 10
}
