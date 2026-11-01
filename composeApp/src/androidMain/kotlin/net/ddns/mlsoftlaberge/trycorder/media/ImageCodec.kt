package net.ddns.mlsoftlaberge.trycorder.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import net.ddns.mlsoftlaberge.trycorder.camera.ImageMath
import java.io.ByteArrayOutputStream
import java.io.IOException

/** decodeFile() of the gallery: decodes a picture upright, scaled down by a power of two. */
internal object ImageCodec {
    private const val TAG = "Trycorder"
    private const val JPEG_QUALITY = 90
    private const val ROTATE_90 = 90f
    private const val ROTATE_180 = 180f
    private const val ROTATE_270 = 270f

    /** The picture at [uri] with both sides at least [size] pixels (when it is that big). */
    fun decode(resolver: ContentResolver, uri: Uri, size: Int): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeModern(resolver, uri, size)
        } else {
            decodeLegacy(resolver, uri, size)
        }
    } catch (e: IOException) {
        Log.w(TAG, "Cannot decode $uri: ${e.message}")
        null
    } catch (e: SecurityException) {
        Log.w(TAG, "Cannot read $uri: ${e.message}")
        null
    }

    fun jpeg(bitmap: Bitmap, quality: Int = JPEG_QUALITY): ByteArray =
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }

    /** ImageDecoder applies the EXIF rotation itself. */
    private fun decodeModern(resolver: ContentResolver, uri: Uri, size: Int): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            decoder.setTargetSampleSize(ImageMath.sampleSize(info.size.width, info.size.height, size))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

    private fun decodeLegacy(resolver: ContentResolver, uri: Uri, size: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageMath.sampleSize(bounds.outWidth, bounds.outHeight, size)
        }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val degrees = resolver.openInputStream(uri)?.use { rotation(ExifInterface(it)) } ?: 0f
        return rotate(bitmap, degrees)
    }

    private fun rotation(exif: ExifInterface): Float =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> ROTATE_90
            ExifInterface.ORIENTATION_ROTATE_180 -> ROTATE_180
            ExifInterface.ORIENTATION_ROTATE_270 -> ROTATE_270
            else -> 0f
        }

    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
