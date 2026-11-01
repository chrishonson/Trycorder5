package net.ddns.mlsoftlaberge.trycorder.media

import android.content.Context

/** Placeholder: replaced by the MediaStore implementation. */
class AndroidMediaLibrary(private val context: Context) : MediaLibrary {
    override suspend fun list(type: MediaType): List<MediaItem> = emptyList()
    override suspend fun delete(item: MediaItem): Boolean = false
    override suspend fun read(uri: String): ByteArray? = null
    override fun toString() = "AndroidMediaLibrary(${context.packageName})"
}

/** Placeholder: replaced by the activity result implementation. */
class AndroidMediaCapture(private val context: Context) : MediaCapture {
    override suspend fun takePhoto(): CaptureResult = CaptureResult.Failed
    override suspend fun recordVideo(): CaptureResult = CaptureResult.Failed
    override fun toString() = "AndroidMediaCapture(${context.packageName})"
}
