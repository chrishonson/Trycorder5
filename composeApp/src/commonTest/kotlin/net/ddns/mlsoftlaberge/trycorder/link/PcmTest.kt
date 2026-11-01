package net.ddns.mlsoftlaberge.trycorder.link

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PcmTest {
    @Test
    fun encodesLittleEndianSixteenBit() {
        val bytes = Pcm.floatsToPcm16(floatArrayOf(0f, 1f, -1f), Pcm.SAMPLE_RATE.toDouble())
        assertContentEquals(byteArrayOf(0, 0, -1, 127, 1, -128), bytes)
    }

    @Test
    fun clipsOutOfRangeSamples() {
        val bytes = Pcm.floatsToPcm16(floatArrayOf(2f), Pcm.SAMPLE_RATE.toDouble())
        assertContentEquals(byteArrayOf(-1, 127), bytes)
    }

    @Test
    fun resamplesFortyEightKiloHertz() {
        val input = FloatArray(4_800) { 0.5f }
        val bytes = Pcm.floatsToPcm16(input, 48_000.0)
        assertEquals(4_410 * 2, bytes.size)
    }

    @Test
    fun roundTrips() {
        val samples = floatArrayOf(0f, 0.25f, -0.5f, 0.99f)
        val decoded = Pcm.pcm16ToFloats(Pcm.floatsToPcm16(samples, Pcm.SAMPLE_RATE.toDouble()))
        assertEquals(samples.size, decoded.size)
        samples.indices.forEach { assertTrue(abs(samples[it] - decoded[it]) < 0.001f) }
    }

    @Test
    fun emptyInputGivesNothing() {
        assertEquals(0, Pcm.floatsToPcm16(FloatArray(0), 44_100.0).size)
        assertEquals(0, Pcm.pcm16ToFloats(byteArrayOf(1)).size)
    }
}
