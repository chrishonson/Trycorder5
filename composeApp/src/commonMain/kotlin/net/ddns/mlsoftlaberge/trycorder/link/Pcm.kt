package net.ddns.mlsoftlaberge.trycorder.link

import kotlin.math.roundToInt

/**
 * Push-to-talk audio format of the original app: 44100 Hz, mono, signed 16 bit little endian PCM,
 * sent raw in UDP datagrams on port 1701. These helpers convert to and from the float samples
 * of platforms whose audio engine works in float (iOS AVAudioEngine).
 */
object Pcm {
    const val SAMPLE_RATE = 44_100
    private const val FULL_SCALE = 32_767f
    private const val BYTE_MASK = 0xFF
    private const val BITS_PER_BYTE = 8

    /** Resample [samples] from [inputRate] to 44100 Hz by linear interpolation and encode as 16 bit PCM. */
    fun floatsToPcm16(samples: FloatArray, inputRate: Double): ByteArray {
        if (samples.isEmpty() || inputRate <= 0.0) return ByteArray(0)
        val step = inputRate / SAMPLE_RATE
        val count = (samples.size.toDouble() * SAMPLE_RATE / inputRate).toInt().coerceAtLeast(1)
        val out = ByteArray(count * 2)
        for (i in 0 until count) {
            val position = i * step
            val index = position.toInt().coerceAtMost(samples.lastIndex)
            val next = (index + 1).coerceAtMost(samples.lastIndex)
            val fraction = (position - index).toFloat()
            val value = samples[index] + (samples[next] - samples[index]) * fraction
            val pcm = (value.coerceIn(-1f, 1f) * FULL_SCALE).roundToInt()
            out[i * 2] = (pcm and BYTE_MASK).toByte()
            out[i * 2 + 1] = (pcm shr BITS_PER_BYTE).toByte()
        }
        return out
    }

    /** Decode 16 bit little endian PCM into float samples in [-1, 1]. */
    fun pcm16ToFloats(bytes: ByteArray, length: Int = bytes.size): FloatArray {
        val count = length.coerceAtMost(bytes.size) / 2
        return FloatArray(count) { i ->
            val low = bytes[i * 2].toInt() and BYTE_MASK
            val high = bytes[i * 2 + 1].toInt()
            ((high shl BITS_PER_BYTE) or low) / FULL_SCALE
        }
    }
}
