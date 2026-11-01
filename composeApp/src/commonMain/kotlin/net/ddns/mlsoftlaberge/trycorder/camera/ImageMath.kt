package net.ddns.mlsoftlaberge.trycorder.camera

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Pure picture arithmetic shared by the platform camera and media code. */
object ImageMath {
    /** Most faces FaceDetector looks for, like processbitmap(). */
    const val MAX_FACES = 5

    /** Longest side used for face detection: big photos are scaled down first for speed. */
    const val DETECTION_SIZE = 1024

    /** Ratio between the face box width and the distance between the eyes. */
    private const val BOX_TO_EYES = 2.5f

    /** The eyes sit a bit above the middle of a face box (in top-left coordinates). */
    private const val EYES_HEIGHT = 0.4f

    /**
     * decodeFile(): the power of two sample size that keeps both sides at least [target]
     * pixels, so a 4000x3000 picture shown at 800 is decoded at 1000x750.
     */
    fun sampleSize(width: Int, height: Int, target: Int): Int {
        var w = width
        var h = height
        var scale = 1
        while (w / 2 >= target && h / 2 >= target) {
            w /= 2
            h /= 2
            scale *= 2
        }
        return scale
    }

    /** FaceDetector throws on odd widths, processbitmap() dropped the last column. */
    fun evenWidth(width: Int): Int = width - width % 2

    /** Factor to bring the longest side down to [limit] (1 when already small enough). */
    fun downscale(width: Int, height: Int, limit: Int): Float {
        val longest = max(width, height)
        return if (longest <= limit || longest == 0) 1f else limit.toFloat() / longest
    }

    /** Size of a picture scaled to fit in a [limit] square, keeping its ratio. */
    fun fit(width: Int, height: Int, limit: Int): Pair<Int, Int> {
        val factor = downscale(width, height, limit)
        return max(1, (width * factor).toInt()) to max(1, (height * factor).toInt())
    }

    /** Scales a face found on a downscaled picture back to the full picture. */
    fun scaleFace(face: Face, factor: Float): Face =
        if (factor == 1f || factor == 0f) {
            face
        } else {
            Face(face.midX / factor, face.midY / factor, face.eyesDistance / factor)
        }

    /**
     * Core Image (origin bottom-left) eye positions to a [Face] in top-left picture pixels.
     */
    fun faceFromEyes(leftX: Float, leftY: Float, rightX: Float, rightY: Float, imageHeight: Float): Face = Face(
        midX = (leftX + rightX) / 2,
        midY = imageHeight - (leftY + rightY) / 2,
        eyesDistance = hypot(rightX - leftX, rightY - leftY),
    )

    /** Core Image face bounds (origin bottom-left) when no eye was found. */
    fun faceFromBounds(x: Float, y: Float, width: Float, height: Float, imageHeight: Float): Face {
        val top = imageHeight - (y + height)
        return Face(
            midX = x + width / 2,
            midY = top + height * EYES_HEIGHT,
            eyesDistance = min(width, height) / BOX_TO_EYES,
        )
    }
}
