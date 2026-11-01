package net.ddns.mlsoftlaberge.trycorder.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageMathTest {
    @Test
    fun sampleSizeIsThePowerOfTwoOfDecodeFile() {
        assertEquals(4, ImageMath.sampleSize(4000, 3000, 640))
        assertEquals(2, ImageMath.sampleSize(4000, 3000, 800))
        assertEquals(1, ImageMath.sampleSize(800, 600, 800))
        assertEquals(16, ImageMath.sampleSize(4000, 3000, 100))
        assertEquals(1, ImageMath.sampleSize(0, 0, 100))
    }

    @Test
    fun evenWidthDropsTheLastColumn() {
        assertEquals(640, ImageMath.evenWidth(641))
        assertEquals(640, ImageMath.evenWidth(640))
    }

    @Test
    fun downscaleAndFit() {
        assertEquals(1f, ImageMath.downscale(800, 600, 1024))
        assertEquals(0.25f, ImageMath.downscale(4096, 3072, 1024))
        assertEquals(1024 to 768, ImageMath.fit(4096, 3072, 1024))
        assertEquals(30 to 40, ImageMath.fit(30, 40, Int.MAX_VALUE))
    }

    @Test
    fun facesFoundOnASmallerPictureAreScaledBack() {
        assertEquals(Face(400f, 200f, 80f), ImageMath.scaleFace(Face(100f, 50f, 20f), 0.25f))
        assertEquals(Face(1f, 2f, 3f), ImageMath.scaleFace(Face(1f, 2f, 3f), 1f))
    }

    @Test
    fun coreImageEyesAreFlipped() {
        val face = ImageMath.faceFromEyes(100f, 700f, 160f, 700f, imageHeight = 1000f)
        assertEquals(Face(130f, 300f, 60f), face)
    }

    @Test
    fun coreImageBoundsGiveAnApproximateFace() {
        val face = ImageMath.faceFromBounds(100f, 600f, 200f, 200f, imageHeight = 1000f)
        assertEquals(200f, face.midX)
        assertEquals(280f, face.midY)
        assertEquals(80f, face.eyesDistance)
    }
}

class EffectMatricesTest {
    @Test
    fun noneHasNoMatrix() {
        assertNull(EffectMatrices.of(CameraEffect.None))
        CameraEffect.entries.filter { it != CameraEffect.None }.forEach {
            assertEquals(20, assertNotNull(EffectMatrices.of(it)).size)
        }
    }

    @Test
    fun negativeInvertsAndMonoIsGray() {
        val negative = assertNotNull(EffectMatrices.of(CameraEffect.Negative))
        assertEquals(Triple(0f, 255f, 155f), EffectMatrices.transform(negative, 255f, 0f, 100f))
        val (r, g, b) = EffectMatrices.transform(assertNotNull(EffectMatrices.of(CameraEffect.Mono)), 255f, 0f, 0f)
        assertEquals(r, g)
        assertEquals(g, b)
    }

    @Test
    fun blackboardIsDarkOnWhiteAndWhiteboardBright() {
        val blackboard = assertNotNull(EffectMatrices.of(CameraEffect.Blackboard))
        val white = EffectMatrices.transform(blackboard, 255f, 255f, 255f)
        assertTrue(white.first < 50f)
        val whiteboard = assertNotNull(EffectMatrices.of(CameraEffect.Whiteboard))
        val bright = EffectMatrices.transform(whiteboard, 200f, 200f, 200f)
        assertEquals(255f, bright.first)
    }

    @Test
    fun matricesAreCopies() {
        val first = assertNotNull(EffectMatrices.of(CameraEffect.Sepia))
        first[0] = 99f
        assertEquals(0.393f, assertNotNull(EffectMatrices.of(CameraEffect.Sepia))[0])
    }
}
