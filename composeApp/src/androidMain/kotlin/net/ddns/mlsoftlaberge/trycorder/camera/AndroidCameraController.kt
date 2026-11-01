package net.ddns.mlsoftlaberge.trycorder.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.media.FaceDetector
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.ddns.mlsoftlaberge.trycorder.media.ImageCodec
import net.ddns.mlsoftlaberge.trycorder.media.TrycorderStorage
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * The live camera of the viewer (CameraX). [CameraViewer] binds a preview and an [ImageCapture]
 * to the screen lifecycle and registers the capture here while it is shown.
 */
class AndroidCameraController(private val context: Context) : CameraController {
    @Volatile
    private var capture: ImageCapture? = null
    private val storage = TrycorderStorage(context)
    private val executor = Executors.newSingleThreadExecutor()

    override val isActive: Boolean get() = capture != null

    internal fun attach(imageCapture: ImageCapture) {
        capture = imageCapture
    }

    internal fun detach(imageCapture: ImageCapture) {
        if (capture === imageCapture) capture = null
    }

    /** snapphoto() + onPictureTaken() + processbitmap(). */
    override suspend fun snap(): SnapResult? {
        val imageCapture = capture ?: return null
        val bitmap = takePicture(imageCapture)
        return bitmap?.let {
            withContext(Dispatchers.Default) {
                val jpeg = ImageCodec.jpeg(it, JPEG_QUALITY)
                val uri = storage.saveImage(jpeg)
                val faces = FaceFinder.find(it)
                SnapResult(jpeg, it.width, it.height, faces, uri?.toString() ?: "")
            }
        }
    }

    private suspend fun takePicture(imageCapture: ImageCapture): Bitmap? = suspendCancellableCoroutine { cont ->
        imageCapture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.use { ImageCodec.rotate(it.toBitmap(), it.imageInfo.rotationDegrees.toFloat()) }
                    if (cont.isActive) cont.resume(bitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.w(TAG, "Picture failed: ${exception.message}")
                    if (cont.isActive) cont.resume(null)
                }
            },
        )
    }

    override fun toString() = "AndroidCameraController(${context.packageName})"

    private companion object {
        const val TAG = "Trycorder"
        const val JPEG_QUALITY = 95
    }
}

/** processbitmap(): android.media.FaceDetector on an RGB_565 copy with an even width. */
internal object FaceFinder {
    fun find(image: Bitmap): List<Face> {
        val factor = ImageMath.downscale(image.width, image.height, ImageMath.DETECTION_SIZE)
        val width = ImageMath.evenWidth((image.width * factor).toInt())
        val height = (image.height * factor).toInt()
        if (width < 2 || height < 2) return emptyList()
        val bitmap565 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap565)
        canvas.scale(factor, factor)
        canvas.drawBitmap(image, 0f, 0f, Paint().apply { isDither = true; isFilterBitmap = true })
        val found = arrayOfNulls<FaceDetector.Face>(ImageMath.MAX_FACES)
        val count = FaceDetector(width, height, ImageMath.MAX_FACES).findFaces(bitmap565, found)
        bitmap565.recycle()
        val mid = PointF()
        return found.take(count).filterNotNull().map { face ->
            face.getMidPoint(mid)
            ImageMath.scaleFace(Face(mid.x, mid.y, face.eyesDistance()), factor)
        }
    }
}
