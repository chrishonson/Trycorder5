package net.ddns.mlsoftlaberge.trycorder.media

/** Placeholder: replaced by the Photos / app documents implementation. */
class IosMediaLibrary : MediaLibrary {
    override suspend fun list(type: MediaType): List<MediaItem> = emptyList()
    override suspend fun delete(item: MediaItem): Boolean = false
    override suspend fun read(uri: String): ByteArray? = null
}

/** Placeholder: replaced by the UIImagePickerController implementation. */
class IosMediaCapture : MediaCapture {
    override suspend fun takePhoto(): CaptureResult = CaptureResult.Failed
    override suspend fun recordVideo(): CaptureResult = CaptureResult.Failed
}
