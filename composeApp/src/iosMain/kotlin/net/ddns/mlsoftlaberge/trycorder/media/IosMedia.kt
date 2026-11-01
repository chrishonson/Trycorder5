package net.ddns.mlsoftlaberge.trycorder.media

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModeVideo
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerMediaURL
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerQualityTypeHigh
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import kotlin.coroutines.resume

/** The pictures and videos of the app's Documents/Trycorder folder. */
class IosMediaLibrary : MediaLibrary {
    override suspend fun list(type: MediaType): List<MediaItem> = withContext(Dispatchers.Default) {
        MediaNames.newestFirst(TrycorderFolder.items(type))
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun delete(item: MediaItem): Boolean {
        val url = NSURL.URLWithString(item.uri) ?: return false
        return NSFileManager.defaultManager.removeItemAtURL(url, null)
    }

    override suspend fun read(uri: String): ByteArray? = withContext(Dispatchers.Default) {
        val data = NSURL.URLWithString(uri)?.let { NSData.dataWithContentsOfURL(it) }
        val image = data?.let { UIImage(data = it) }
        if (image == null) data?.toByteArray() else TrycorderFolder.jpeg(TrycorderFolder.upright(image, READ_SIZE))
    }

    override suspend fun thumbnail(uri: String, size: Int): ByteArray? = withContext(Dispatchers.Default) {
        val data = NSURL.URLWithString(uri)?.let { NSData.dataWithContentsOfURL(it) }
        val image = data?.let { UIImage(data = it) } ?: return@withContext null
        // The gallery asks for the short side; fit() limits the long side.
        TrycorderFolder.jpeg(TrycorderFolder.upright(image, size * THUMB_RATIO))
    }

    /** The Photos application, through its undocumented but long-standing url scheme. */
    override fun openGallery(type: MediaType): Boolean {
        val url = NSURL.URLWithString("photos-redirect://") ?: return false
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        return true
    }

    override fun share(item: MediaItem): Boolean {
        val url = NSURL.URLWithString(item.uri)
        val presenter = Presenter.top()
        if (url == null || presenter == null) return false
        val items = listOf("From my Trycorder", url)
        val sheet = UIActivityViewController(activityItems = items, applicationActivities = null)
        presenter.presentViewController(sheet, animated = true, completion = null)
        return true
    }

    private companion object {
        const val READ_SIZE = 1600
        const val THUMB_RATIO = 2
    }
}

/** The view controller on top, to present the camera picker and share sheet from. */
internal object Presenter {
    fun top(): UIViewController? {
        val scenes = UIApplication.sharedApplication.connectedScenes
        val scene = scenes.firstOrNull { it is UIWindowScene } as? UIWindowScene
        val windows = scene?.windows.orEmpty().filterIsInstance<UIWindow>()
        var controller = (windows.firstOrNull { it.isKeyWindow() } ?: windows.firstOrNull())?.rootViewController
        while (controller?.presentedViewController != null) controller = controller.presentedViewController
        return controller
    }
}

/** takephoto() / recordvideo() with the system camera (UIImagePickerController). */
class IosMediaCapture : MediaCapture {
    /** The picker only keeps a weak reference to its delegate. */
    private var delegate: PickerDelegate? = null

    override suspend fun takePhoto(): CaptureResult = capture(MediaType.Image)

    override suspend fun recordVideo(): CaptureResult = capture(MediaType.Video)

    private suspend fun capture(type: MediaType): CaptureResult = withContext<CaptureResult>(Dispatchers.Main) {
        val source = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        val presenter = Presenter.top()
        if (!UIImagePickerController.isSourceTypeAvailable(source) || presenter == null) {
            return@withContext CaptureResult.Failed
        }
        suspendCancellableCoroutine { cont ->
            val picker = UIImagePickerController()
            picker.sourceType = source
            if (type == MediaType.Video) {
                picker.mediaTypes = listOf(MOVIE)
                picker.cameraCaptureMode = UIImagePickerControllerCameraCaptureModeVideo
                picker.videoQuality = UIImagePickerControllerQualityTypeHigh
            } else {
                picker.mediaTypes = listOf(IMAGE)
            }
            val callback = PickerDelegate(type) { result ->
                picker.dismissViewControllerAnimated(true, completion = null)
                delegate = null
                if (cont.isActive) cont.resume(result)
            }
            delegate = callback
            picker.delegate = callback
            presenter.presentViewController(picker, animated = true, completion = null)
        }
    }

    private companion object {
        const val IMAGE = "public.image"
        const val MOVIE = "public.movie"
    }
}

/** onActivityResult(): saves what the picker returns into Documents/Trycorder. */
private class PickerDelegate(
    private val type: MediaType,
    private val done: (CaptureResult) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val info = didFinishPickingMediaWithInfo
        val saved = when (type) {
            MediaType.Image -> (info[UIImagePickerControllerOriginalImage] as? UIImage)?.let { saveImage(it) }
            MediaType.Video -> (info[UIImagePickerControllerMediaURL] as? NSURL)?.let { saveVideo(it) }
        }
        done(saved?.let { CaptureResult.Saved(it) } ?: CaptureResult.Failed)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        done(CaptureResult.Cancelled)
    }

    private fun saveImage(image: UIImage): String? {
        val file = TrycorderFolder.newFile(MediaType.Image)
        val bytes = TrycorderFolder.jpeg(TrycorderFolder.upright(image))
        PhotosAlbum.save(image)
        val written = file != null && bytes != null && bytes.toNSData().writeToURL(file, atomically = true)
        return if (written) file?.absoluteString else null
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun saveVideo(source: NSURL): String? {
        val extension = source.pathExtension?.lowercase().takeUnless { it.isNullOrEmpty() } ?: "mov"
        val file = TrycorderFolder.newFile(MediaType.Video, extension) ?: return null
        val copied = NSFileManager.defaultManager.copyItemAtURL(source, file, null)
        if (copied) PhotosAlbum.saveVideo(file)
        return if (copied) file.absoluteString else null
    }
}
