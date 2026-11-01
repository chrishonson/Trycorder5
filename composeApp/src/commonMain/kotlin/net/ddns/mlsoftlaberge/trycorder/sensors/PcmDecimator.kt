package net.ddns.mlsoftlaberge.trycorder.sensors

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Turns float microphone samples at [sourceRate] into 16 bit PCM chunks of [chunkSize]
 * samples at about [targetRate] (the 8 kHz the original AudSensorView recorded at).
 * Each output sample is the average of one block of input samples.
 */
class PcmDecimator(
    sourceRate: Double,
    targetRate: Double = AUDIO_SAMPLE_RATE.toDouble(),
    private val chunkSize: Int = AUDIO_CHUNK,
    private val onChunk: (ShortArray) -> Unit,
) {
    private val step: Int = max(1, (sourceRate / targetRate).roundToInt())
    private var blockSum = 0f
    private var blockCount = 0
    private val chunk = ShortArray(chunkSize)
    private var filled = 0

    fun accept(samples: FloatArray, count: Int = samples.size) {
        for (i in 0 until count) {
            blockSum += samples[i]
            blockCount++
            if (blockCount == step) {
                push(blockSum / step)
                blockSum = 0f
                blockCount = 0
            }
        }
    }

    private fun push(sample: Float) {
        chunk[filled++] = toPcm16(sample)
        if (filled == chunkSize) {
            onChunk(chunk.copyOf())
            filled = 0
        }
    }

    companion object {
        /** RECORDER_SAMPLERATE of AudSensorView. */
        const val AUDIO_SAMPLE_RATE = 8000

        /** BufferElements2Rec of AudSensorView: samples read per refresh. */
        const val AUDIO_CHUNK = 1024

        private const val PCM_MAX = 32767f
        private const val PCM_MIN = -32768f

        fun toPcm16(sample: Float): Short = (sample * PCM_MAX).coerceIn(PCM_MIN, PCM_MAX).toInt().toShort()
    }
}
