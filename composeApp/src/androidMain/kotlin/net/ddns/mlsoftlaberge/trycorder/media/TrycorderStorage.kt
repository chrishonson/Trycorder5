package net.ddns.mlsoftlaberge.trycorder.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Where the Trycorder pictures and videos live (the modern FileUtils).
 * Android 10+: MediaStore Pictures/Trycorder and Movies/Trycorder (scoped storage, no permission).
 * Android 7-9: the app's own external Pictures/Trycorder and Movies/Trycorder folders, shared
 * through a FileProvider (no WRITE_EXTERNAL_STORAGE).
 */
internal class TrycorderStorage(private val context: Context) {
    private val resolver get() = context.contentResolver

    private val scoped: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** getOutputMediaFileUri(): a new empty entry the camera application writes to. */
    fun newEntry(type: MediaType): Uri? {
        val name = MediaNames.fileName(type, stamp())
        return if (scoped) {
            resolver.insert(collection(type), values(type, name, pending = false))
        } else {
            val file = File(legacyDir(type) ?: return null, name)
            FileProvider.getUriForFile(context, authority(context), file)
        }
    }

    /** A capture that was cancelled leaves no empty file behind. */
    fun discard(uri: Uri) {
        try {
            resolver.delete(uri, null, null)
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot discard $uri: ${e.message}")
        }
    }

    /** onPictureTaken(): writes a JPEG and returns its uri. */
    fun saveImage(jpeg: ByteArray): Uri? {
        val name = MediaNames.fileName(MediaType.Image, stamp())
        return try {
            if (scoped) saveScoped(name, jpeg) else saveLegacy(name, jpeg)
        } catch (e: IOException) {
            Log.w(TAG, "Cannot save $name: ${e.message}")
            null
        }
    }

    private fun saveScoped(name: String, jpeg: ByteArray): Uri? {
        val pending = values(MediaType.Image, name, pending = true)
        val uri = resolver.insert(collection(MediaType.Image), pending) ?: return null
        resolver.openOutputStream(uri)?.use { it.write(jpeg) } ?: throw IOException("No output stream for $uri")
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        return uri
    }

    private fun saveLegacy(name: String, jpeg: ByteArray): Uri? {
        val file = File(legacyDir(MediaType.Image) ?: return null, name)
        file.writeBytes(jpeg)
        return FileProvider.getUriForFile(context, authority(context), file)
    }

    /** The app folder of Android 7-9, null when the external storage is not mounted. */
    fun legacyDir(type: MediaType): File? {
        val folder = if (type == MediaType.Image) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES
        val dir = File(context.getExternalFilesDir(folder) ?: return null, MediaNames.FOLDER)
        return dir.takeIf { it.isDirectory || it.mkdirs() }
    }

    /** The files of the Android 7-9 app folder. */
    fun legacyItems(type: MediaType): List<MediaItem> {
        val files = legacyDir(type)?.listFiles().orEmpty().filter { it.isFile && MediaNames.matches(it.name, type) }
        return files.map { file ->
            val uri = FileProvider.getUriForFile(context, authority(context), file)
            MediaItem(uri.toString(), file.name, type, file.lastModified())
        }
    }

    fun collection(type: MediaType): Uri = if (scoped) {
        when (type) {
            MediaType.Image -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            MediaType.Video -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
    } else {
        when (type) {
            MediaType.Image -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaType.Video -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
    }

    private fun values(type: MediaType, name: String, pending: Boolean) = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, MediaNames.mimeType(type))
        if (scoped) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath(type))
            put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis())
            if (pending) put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    companion object {
        private const val TAG = "Trycorder"

        fun authority(context: Context) = "${context.packageName}.fileprovider"

        /** Pictures/Trycorder/ or Movies/Trycorder/ (MediaStore RELATIVE_PATH). */
        fun relativePath(type: MediaType): String {
            val root = if (type == MediaType.Image) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES
            return "$root/${MediaNames.FOLDER}/"
        }

        /** The yyyyMMdd_HHmmss time stamp of the original file names. */
        fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }
}
