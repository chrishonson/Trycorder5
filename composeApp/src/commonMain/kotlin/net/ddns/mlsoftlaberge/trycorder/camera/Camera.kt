package net.ddns.mlsoftlaberge.trycorder.camera

/** A photo taken from the viewer, with faces found by the platform face detector. */
class SnapResult(
    /** JPEG bytes of the picture. */
    val jpeg: ByteArray,
    val width: Int,
    val height: Int,
    val faces: List<Face>,
    /** Where the picture was saved, shown in the status line. */
    val savedTo: String,
)

/** A detected face: mid point between the eyes and eye distance, in picture pixels. */
data class Face(val midX: Float, val midY: Float, val eyesDistance: Float)

/**
 * Handle to the live camera shown by [CameraViewer]. The viewer binds itself to the
 * controller while it is on screen.
 */
interface CameraController {
    val isActive: Boolean

    /** Take a picture, save it to the Trycorder pictures folder and detect faces. Null on failure. */
    suspend fun snap(): SnapResult?
}
