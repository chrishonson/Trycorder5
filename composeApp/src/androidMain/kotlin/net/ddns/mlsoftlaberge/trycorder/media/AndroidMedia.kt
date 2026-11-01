package net.ddns.mlsoftlaberge.trycorder.media

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ddns.mlsoftlaberge.trycorder.AndroidActivityHolder
import java.io.IOException

/**
 * The Trycorder pictures and videos in the MediaStore (Pictures/Trycorder, Movies/Trycorder and the
 * DCIM/Camera folder of the original app when the media permission is granted).
 */
class AndroidMediaLibrary(private val context: Context) : MediaLibrary {
    private val storage = TrycorderStorage(context)
    private val query = MediaQuery(context, storage)
    private val askedPermission = mutableSetOf<String>()

    override suspend fun list(type: MediaType): List<MediaItem> {
        askReadPermission(type)
        return withContext(Dispatchers.IO) {
            MediaNames.newestFirst(query.items(type, canReadOthers(type)) + storage.legacyItems(type))
        }
    }

    override suspend fun delete(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(Uri.parse(item.uri), null, null) > 0
        } catch (e: SecurityException) {
            // Pictures of other apps need a user confirmation (RecoverableSecurityException).
            Log.w(TAG, "Delete needs confirmation: ${e.message}")
            false
        }
    }

    override suspend fun read(uri: String): ByteArray? = withContext(Dispatchers.IO) {
        val parsed = Uri.parse(uri)
        val mime = context.contentResolver.getType(parsed).orEmpty()
        if (mime.startsWith("video/")) return@withContext rawBytes(parsed)
        ImageCodec.decode(context.contentResolver, parsed, READ_SIZE)?.let { ImageCodec.jpeg(it) } ?: rawBytes(parsed)
    }

    override suspend fun thumbnail(uri: String, size: Int): ByteArray? = withContext(Dispatchers.IO) {
        ImageCodec.decode(context.contentResolver, Uri.parse(uri), size)?.let { ImageCodec.jpeg(it) }
    }

    override fun openGallery(type: MediaType): Boolean {
        val gallery = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)
        val view = Intent(Intent.ACTION_VIEW).setType(if (type == MediaType.Image) "image/*" else "video/*")
        return start(gallery) || start(view)
    }

    override fun share(item: MediaItem): Boolean {
        val mime = if (item.type == MediaType.Image) "image/*" else "video/*"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_SUBJECT, if (item.type == MediaType.Image) "Image" else "Video")
            putExtra(Intent.EXTRA_TEXT, "From my Trycorder\n${item.uri}")
            putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val label = if (item.type == MediaType.Image) "Send image to ..." else "Send video to ..."
        return start(Intent.createChooser(send, label))
    }

    private fun start(intent: Intent): Boolean {
        val activity = AndroidActivityHolder.activity
        return try {
            if (activity != null) {
                activity.startActivity(intent)
            } else {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No application for ${intent.action}: ${e.message}")
            false
        }
    }

    private fun rawBytes(uri: Uri): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: IOException) {
        Log.w(TAG, "Cannot read $uri: ${e.message}")
        null
    } catch (e: SecurityException) {
        Log.w(TAG, "Not allowed to read $uri: ${e.message}")
        null
    }

    /** Asks once per run for the permission to see the pictures of the camera app. */
    private suspend fun askReadPermission(type: MediaType) {
        val permission = readPermission(type)
        if (canReadOthers(type) || !askedPermission.add(permission)) return
        ActivityResults.launch(ActivityResultContracts.RequestPermission(), permission)
    }

    private fun canReadOthers(type: MediaType): Boolean =
        ContextCompat.checkSelfPermission(context, readPermission(type)) == PackageManager.PERMISSION_GRANTED

    private fun readPermission(type: MediaType): String = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_EXTERNAL_STORAGE
        type == MediaType.Image -> Manifest.permission.READ_MEDIA_IMAGES
        else -> Manifest.permission.READ_MEDIA_VIDEO
    }

    override fun toString() = "AndroidMediaLibrary(${context.packageName})"

    private companion object {
        const val TAG = "Trycorder"

        /** Pictures read for the viewer are kept around screen size. */
        const val READ_SIZE = 1600
    }
}

/** MediaStore queries of the Trycorder folders. */
internal class MediaQuery(private val context: Context, private val storage: TrycorderStorage) {
    fun items(type: MediaType, includeLegacy: Boolean): List<MediaItem> {
        val scoped = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        if (!scoped && !includeLegacy) return emptyList()
        val (selection, args) = if (scoped) {
            val legacy = if (includeLegacy) listOf("${MediaNames.LEGACY_FOLDER}/") else emptyList()
            val paths = listOf(TrycorderStorage.relativePath(type)) + legacy
            paths.joinToString(" OR ") { "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?" } to paths.map { "$it%" }
        } else {
            @Suppress("DEPRECATION") // DATA is the only folder column before Android 10
            "${MediaStore.MediaColumns.DATA} LIKE ?" to listOf("%/${MediaNames.LEGACY_FOLDER}/%")
        }
        return try {
            run(type, selection, args.toTypedArray())
        } catch (e: SecurityException) {
            Log.w("Trycorder", "Media query refused: ${e.message}")
            emptyList()
        }
    }

    private fun run(type: MediaType, selection: String, args: Array<String>): List<MediaItem> {
        val collection = storage.collection(type)
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
        )
        val items = mutableListOf<MediaItem>()
        context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val name = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val added = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            while (c.moveToNext()) {
                val displayName = c.getString(name).orEmpty()
                if (!MediaNames.matches(displayName, type)) continue
                val uri = ContentUris.withAppendedId(collection, c.getLong(id))
                items += MediaItem(uri.toString(), displayName, type, c.getLong(added) * MILLIS)
            }
        }
        return items
    }

    private companion object {
        const val MILLIS = 1000L
    }
}

/** takephoto() / recordvideo(): the system camera application writing to a Trycorder uri. */
class AndroidMediaCapture(private val context: Context) : MediaCapture {
    private val storage = TrycorderStorage(context)

    override suspend fun takePhoto(): CaptureResult = capture(MediaType.Image)

    override suspend fun recordVideo(): CaptureResult = capture(MediaType.Video)

    private suspend fun capture(type: MediaType): CaptureResult {
        val uri = withContext(Dispatchers.IO) { storage.newEntry(type) } ?: return CaptureResult.Failed
        val saved = when (type) {
            MediaType.Image -> ActivityResults.launch(ActivityResultContracts.TakePicture(), uri)
            MediaType.Video -> ActivityResults.launch(ActivityResultContracts.CaptureVideo(), uri)
        }
        if (saved != true) withContext(Dispatchers.IO) { storage.discard(uri) }
        return when (saved) {
            true -> CaptureResult.Saved(uri.toString())
            false -> CaptureResult.Cancelled
            null -> CaptureResult.Failed
        }
    }

    override fun toString() = "AndroidMediaCapture(${context.packageName})"
}
