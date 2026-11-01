package net.ddns.mlsoftlaberge.trycorder.sensors

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SensorMathTest {
    private fun assertNear(expected: Float, actual: Float) =
        assertTrue(kotlin.math.abs(expected - actual) < 0.01f, "expected $expected but was $actual")

    @Test
    fun azimuthIsWrappedIntoAFullTurn() {
        assertNear(270f, SensorMath.normalizeAzimuth(-90f))
        assertNear(10f, SensorMath.normalizeAzimuth(370f))
        assertNear(0f, SensorMath.normalizeAzimuth(0f))
    }

    @Test
    fun androidOrientationIsInDegreesWithAPositiveAzimuth() {
        val v = SensorMath.fromAndroidOrientation((-PI / 2).toFloat(), (PI / 4).toFloat(), (PI / 6).toFloat())
        assertNear(270f, v.x)
        assertNear(45f, v.y)
        assertNear(30f, v.z)
    }

    @Test
    fun iosYawMinus90IsNorthAndPitchIsInverted() {
        val north = SensorMath.fromIosAttitude(-PI / 2, PI / 4, 0.0)
        assertNear(0f, north.x)
        assertNear(-45f, north.y)
        val east = SensorMath.fromIosAttitude(PI, 0.0, PI / 6)
        assertNear(90f, east.x)
        assertNear(30f, east.z)
    }

    @Test
    fun iosAccelerationIsConvertedToAndroidMetersPerSecond() {
        val upright = SensorMath.fromIosAcceleration(0.0, -1.0, 0.0)
        assertNear(SensorMath.STANDARD_GRAVITY, upright.y)
        assertNear(0f, upright.x)
    }

    @Test
    fun pressureKiloPascalBecomesHectoPascal() {
        assertNear(1013.25f, SensorMath.kiloPascalToHectoPascal(101.325))
    }

    @Test
    fun decimatorAveragesBlocksAndEmitsFullChunks() {
        val chunks = mutableListOf<ShortArray>()
        val decimator = PcmDecimator(sourceRate = 32.0, targetRate = 8.0, chunkSize = 2) { chunks.add(it) }
        decimator.accept(floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f, -1f, -1f, -1f))
        assertEquals(0, chunks.size)
        decimator.accept(floatArrayOf(-1f, 2f, 2f, 2f, 2f))
        assertEquals(1, chunks.size)
        assertContentEquals(shortArrayOf(16383, -32767), chunks[0])
    }

    @Test
    fun pcmConversionClampsToSixteenBits() {
        assertEquals(32767.toShort(), PcmDecimator.toPcm16(3f))
        assertEquals((-32768).toShort(), PcmDecimator.toPcm16(-3f))
        assertEquals(0.toShort(), PcmDecimator.toPcm16(0f))
    }
}
