package net.ddns.mlsoftlaberge.trycorder.camera

/**
 * Color effects of the vision screens (Camera.Parameters.EFFECT_* of TryvisionFragment and
 * CaptureRequest.CONTROL_EFFECT_MODE_* of TryviscamFragment).
 */
enum class CameraEffect { None, Mono, Negative, Sepia, Aqua, Blackboard, Whiteboard, Posterize, Solarize }

/** Scene modes of the Camera2 vision screen (CONTROL_SCENE_MODE_*). */
enum class CameraScene { Portrait, NightPortrait }

/**
 * 4x5 row-major color matrices (same layout and 0..255 offsets as android.graphics.ColorMatrix
 * and Compose ColorMatrix) that imitate an effect when the camera hardware has no effect mode.
 * Posterize and solarize are not linear, so their matrices are approximations.
 */
object EffectMatrices {
    private const val LR = 0.299f
    private const val LG = 0.587f
    private const val LB = 0.114f
    private const val FULL = 255f
    private const val CONTRAST = 1.6f
    private const val CONTRAST_OFFSET = -48f
    private const val POSTER_GAIN = 2.2f
    private const val POSTER_OFFSET = -150f

    private val MONO = luma(1f, 1f, 1f)
    private val NEGATIVE = floatArrayOf(
        -1f, 0f, 0f, 0f, FULL,
        0f, -1f, 0f, 0f, FULL,
        0f, 0f, -1f, 0f, FULL,
        0f, 0f, 0f, 1f, 0f,
    )
    private val SEPIA = floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
    private val AQUA = luma(0.55f, 1.0f, 1.1f)
    private val BLACKBOARD = scaledLuma(-CONTRAST, FULL - CONTRAST_OFFSET)
    private val WHITEBOARD = scaledLuma(CONTRAST, CONTRAST_OFFSET)
    private val POSTERIZE = floatArrayOf(
        POSTER_GAIN, 0f, 0f, 0f, POSTER_OFFSET,
        0f, POSTER_GAIN, 0f, 0f, POSTER_OFFSET,
        0f, 0f, POSTER_GAIN, 0f, POSTER_OFFSET,
        0f, 0f, 0f, 1f, 0f,
    )
    private const val SOLAR_OFFSET = 64f
    private val SOLARIZE = floatArrayOf(
        -0.5f, 0.5f, 0.5f, 0f, SOLAR_OFFSET,
        0.5f, -0.5f, 0.5f, 0f, SOLAR_OFFSET,
        0.5f, 0.5f, -0.5f, 0f, SOLAR_OFFSET,
        0f, 0f, 0f, 1f, 0f,
    )

    /** The matrix for [effect], null when nothing has to be applied. Returns a copy. */
    fun of(effect: CameraEffect): FloatArray? = when (effect) {
        CameraEffect.None -> null
        CameraEffect.Mono -> MONO
        CameraEffect.Negative -> NEGATIVE
        CameraEffect.Sepia -> SEPIA
        CameraEffect.Aqua -> AQUA
        CameraEffect.Blackboard -> BLACKBOARD
        CameraEffect.Whiteboard -> WHITEBOARD
        CameraEffect.Posterize -> POSTERIZE
        CameraEffect.Solarize -> SOLARIZE
    }?.copyOf()

    /** Applies a matrix to one 0..255 RGB color, clamped like the GPU does. */
    fun transform(matrix: FloatArray, r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        fun row(i: Int) =
            (matrix[i] * r + matrix[i + 1] * g + matrix[i + 2] * b + matrix[i + OFFSET]).coerceIn(0f, FULL)
        return Triple(row(0), row(ROW), row(ROW * 2))
    }

    private const val ROW = 5
    private const val OFFSET = 4

    private fun luma(r: Float, g: Float, b: Float) = floatArrayOf(
        LR * r, LG * r, LB * r, 0f, 0f,
        LR * g, LG * g, LB * g, 0f, 0f,
        LR * b, LG * b, LB * b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    private fun scaledLuma(gain: Float, offset: Float) = floatArrayOf(
        LR * gain, LG * gain, LB * gain, 0f, offset,
        LR * gain, LG * gain, LB * gain, 0f, offset,
        LR * gain, LG * gain, LB * gain, 0f, offset,
        0f, 0f, 0f, 1f, 0f,
    )
}
