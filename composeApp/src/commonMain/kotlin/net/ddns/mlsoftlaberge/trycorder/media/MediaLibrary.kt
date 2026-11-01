package net.ddns.mlsoftlaberge.trycorder.media

enum class MediaType { Image, Video }

data class MediaItem(val uri: String, val name: String, val type: MediaType, val dateMillis: Long)

/** Pictures and videos taken by the Trycorder (the gallery and viewer pages). */
interface MediaLibrary {
    suspend fun list(type: MediaType): List<MediaItem>
    suspend fun delete(item: MediaItem): Boolean

    /** Bytes of a picture, for showing it in the viewer. Null when it cannot be read. */
    suspend fun read(uri: String): ByteArray?

    /**
     * An upright JPEG of the picture scaled down so its sides are about [size] pixels
     * (decodeFile(f, size) of the gallery). Null when it cannot be read.
     */
    suspend fun thumbnail(uri: String, size: Int): ByteArray? = read(uri)

    /** Opens the system gallery application (GAL button). False when there is none. */
    fun openGallery(type: MediaType): Boolean = false

    /** Sends a picture or video to another application (SEND button). False when it cannot. */
    fun share(item: MediaItem): Boolean = false
}

sealed interface CaptureResult {
    data class Saved(val uri: String) : CaptureResult
    data object Cancelled : CaptureResult
    data object Failed : CaptureResult
}

/** Photo and video capture through the system camera app (Photo and Record buttons). */
interface MediaCapture {
    suspend fun takePhoto(): CaptureResult
    suspend fun recordVideo(): CaptureResult
}
