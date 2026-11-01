package net.ddns.mlsoftlaberge.trycorder.media

enum class MediaType { Image, Video }

data class MediaItem(val uri: String, val name: String, val type: MediaType, val dateMillis: Long)

/** Pictures and videos taken by the Trycorder (the gallery and viewer pages). */
interface MediaLibrary {
    suspend fun list(type: MediaType): List<MediaItem>
    suspend fun delete(item: MediaItem): Boolean

    /** Bytes of a picture, for showing it in the viewer. Null when it cannot be read. */
    suspend fun read(uri: String): ByteArray?
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
