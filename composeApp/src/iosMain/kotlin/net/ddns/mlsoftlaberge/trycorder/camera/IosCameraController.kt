package net.ddns.mlsoftlaberge.trycorder.camera

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.ddns.mlsoftlaberge.trycorder.media.MediaType
import net.ddns.mlsoftlaberge.trycorder.media.PhotosAlbum
import net.ddns.mlsoftlaberge.trycorder.media.TrycorderFolder
import net.ddns.mlsoftlaberge.trycorder.media.toNSData
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreImage.CIDetector
import platform.CoreImage.CIDetectorAccuracy
import platform.CoreImage.CIDetectorAccuracyHigh
import platform.CoreImage.CIDetectorTypeFace
import platform.CoreImage.CIFaceFeature
import platform.CoreImage.CIImage
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.writeToURL
import platform.UIKit.UIImage
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create
import kotlin.coroutines.resume

/**
 * The live camera of the viewer (AVFoundation). [CameraViewer] starts the session with the
 * lens it wants and stops it when it leaves the screen.
 */
@OptIn(ExperimentalForeignApi::class)
class IosCameraController : CameraController {
    private val session = AVCaptureSession()
    private val photoOutput = AVCapturePhotoOutput()
    private val queue = dispatch_queue_create("trycorder.camera", null)
    private var input: AVCaptureDeviceInput? = null
    private var shown = false

    /** The picture delegate is only weakly held by AVFoundation. */
    private var pending: PhotoDelegate? = null

    internal val previewLayer: AVCaptureVideoPreviewLayer by lazy {
        AVCaptureVideoPreviewLayer(session = session).apply { videoGravity = AVLayerVideoGravityResizeAspectFill }
    }

    override val isActive: Boolean get() = shown && session.running

    /** switchcam(): back or front lens, after asking for the camera permission. */
    internal fun start(front: Boolean) {
        shown = true
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> configure(front)
            AVAuthorizationStatusNotDetermined ->
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted -> if (granted) configure(front) }
            else -> Unit
        }
    }

    internal fun stop() {
        shown = false
        dispatch_async(queue) { if (!shown) session.stopRunning() }
    }

    private fun configure(front: Boolean) = dispatch_async(queue) {
        session.beginConfiguration()
        input?.let { session.removeInput(it) }
        input = null
        val position = if (front) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack
        val device = AVCaptureDevice.defaultDeviceWithDeviceType(
            AVCaptureDeviceTypeBuiltInWideAngleCamera,
            AVMediaTypeVideo,
            position,
        )
        val newInput = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) as? AVCaptureDeviceInput }
        if (newInput != null && session.canAddInput(newInput)) {
            session.addInput(newInput)
            input = newInput
        }
        if (session.outputs.isEmpty() && session.canAddOutput(photoOutput)) session.addOutput(photoOutput)
        if (session.canSetSessionPreset(AVCaptureSessionPresetPhoto)) {
            session.sessionPreset = AVCaptureSessionPresetPhoto
        }
        session.commitConfiguration()
        if (shown && !session.running) session.startRunning()
    }

    /** snapphoto() + onPictureTaken() + processbitmap(). */
    override suspend fun snap(): SnapResult? {
        val data = if (isActive) capturePhoto() else null
        return data?.let { withContext(Dispatchers.Default) { process(it) } }
    }

    private suspend fun capturePhoto(): NSData? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val delegate = PhotoDelegate { data ->
                pending = null
                if (cont.isActive) cont.resume(data)
            }
            pending = delegate
            photoOutput.capturePhotoWithSettings(AVCapturePhotoSettings.photoSettings(), delegate)
        }
    }

    private fun process(data: NSData): SnapResult? {
        val image = UIImage(data = data)
        val upright = TrycorderFolder.upright(image)
        val jpeg = TrycorderFolder.jpeg(upright) ?: return null
        val file = TrycorderFolder.newFile(MediaType.Image)
        val saved = file != null && jpeg.toNSData().writeToURL(file, atomically = true)
        PhotosAlbum.save(upright)
        val (width, height) = upright.size.useContents { width.toInt() to height.toInt() }
        val savedTo = if (saved) file?.path.orEmpty() else ""
        return SnapResult(jpeg, width, height, FaceScanner.find(upright, height), savedTo)
    }
}

/** processbitmap() with Core Image: CIDetector faces converted to the Android FaceDetector form. */
@OptIn(ExperimentalForeignApi::class)
internal object FaceScanner {
    fun find(image: UIImage, height: Int): List<Face> {
        val cgImage = image.CGImage ?: return emptyList()
        val detector = CIDetector.detectorOfType(
            CIDetectorTypeFace,
            context = null,
            options = mapOf<Any?, Any?>(CIDetectorAccuracy to CIDetectorAccuracyHigh),
        )
        val features = detector?.featuresInImage(CIImage.imageWithCGImage(cgImage)).orEmpty()
        return features.filterIsInstance<CIFaceFeature>().take(ImageMath.MAX_FACES).map { toFace(it, height.toFloat()) }
    }

    private fun toFace(feature: CIFaceFeature, height: Float): Face {
        if (feature.hasLeftEyePosition && feature.hasRightEyePosition) {
            val (lx, ly) = feature.leftEyePosition.useContents { x.toFloat() to y.toFloat() }
            val (rx, ry) = feature.rightEyePosition.useContents { x.toFloat() to y.toFloat() }
            return ImageMath.faceFromEyes(lx, ly, rx, ry, height)
        }
        return feature.bounds.useContents {
            ImageMath.faceFromBounds(
                origin.x.toFloat(), origin.y.toFloat(), size.width.toFloat(), size.height.toFloat(), height,
            )
        }
    }
}

/** onPictureTaken(): receives the photo data. */
private class PhotoDelegate(
    private val done: (NSData?) -> Unit,
) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?,
    ) {
        done(if (error == null) didFinishProcessingPhoto.fileDataRepresentation() else null)
    }
}
