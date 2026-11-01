package net.ddns.mlsoftlaberge.trycorder.media

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import net.ddns.mlsoftlaberge.trycorder.camera.ImageMath
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSLocale
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

/** NSData <-> ByteArray. */
@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData = usePinned {
    NSData.create(bytes = if (isEmpty()) null else it.addressOf(0), length = size.toULong())
}

/** The app's Documents/Trycorder folder: the iOS place of the Trycorder pictures and videos. */
@OptIn(ExperimentalForeignApi::class)
internal object TrycorderFolder {
    private const val JPEG_QUALITY = 0.9

    val url: NSURL?
        get() {
            val manager = NSFileManager.defaultManager
            val documents = manager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, true, null)
                ?: return null
            val folder = documents.URLByAppendingPathComponent(MediaNames.FOLDER, isDirectory = true) ?: return null
            manager.createDirectoryAtURL(folder, withIntermediateDirectories = true, attributes = null, error = null)
            return folder
        }

    /** A new file url named like the original IMG_/VID_ files. */
    fun newFile(type: MediaType, extension: String = "mp4"): NSURL? =
        url?.URLByAppendingPathComponent(MediaNames.fileName(type, stamp(), extension))

    fun items(type: MediaType): List<MediaItem> {
        val folder = url ?: return emptyList()
        val manager = NSFileManager.defaultManager
        val names = manager.contentsOfDirectoryAtPath(folder.path.orEmpty(), null).orEmpty().filterIsInstance<String>()
        return names.filter { MediaNames.matches(it, type) }.mapNotNull { name ->
            val file = folder.URLByAppendingPathComponent(name) ?: return@mapNotNull null
            val attributes = manager.attributesOfItemAtPath(file.path.orEmpty(), null)
            val date = attributes?.get(NSFileModificationDate) as? NSDate
            val millis = ((date?.timeIntervalSince1970 ?: 0.0) * MILLIS).toLong()
            MediaItem(file.absoluteString.orEmpty(), name, type, millis)
        }
    }

    /** UIImage redrawn upright (EXIF orientation applied), at most [limit] pixels on its longest side. */
    fun upright(image: UIImage, limit: Int = Int.MAX_VALUE): UIImage {
        val (w, h) = image.size.useContents { width to height }
        val (fw, fh) = ImageMath.fit(w.toInt(), h.toInt(), limit)
        val format = UIGraphicsImageRendererFormat.defaultFormat().apply { scale = 1.0 }
        val renderer = UIGraphicsImageRenderer(size = CGSizeMake(fw.toDouble(), fh.toDouble()), format = format)
        return renderer.imageWithActions { image.drawInRect(CGRectMake(0.0, 0.0, fw.toDouble(), fh.toDouble())) }
    }

    fun jpeg(image: UIImage): ByteArray? = UIImageJPEGRepresentation(image, JPEG_QUALITY)?.toByteArray()

    fun stamp(): String = NSDateFormatter().apply {
        dateFormat = "yyyyMMdd_HHmmss"
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
    }.stringFromDate(NSDate())

    private const val MILLIS = 1000.0
}
