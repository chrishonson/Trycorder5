package net.ddns.mlsoftlaberge.trycorder.media

/** File naming and sorting rules of the original FileUtils, shared by both platforms. */
object MediaNames {
    /** Folder used under Pictures, Movies (Android) and Documents (iOS). */
    const val FOLDER = "Trycorder"

    /** Folder of the original app (DCIM/Camera), still listed when readable. */
    const val LEGACY_FOLDER = "DCIM/Camera"

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "heic", "webp")
    private val VIDEO_EXTENSIONS = setOf("mp4", "mov", "3gp", "m4v", "webm")

    /** IMG_yyyyMMdd_HHmmss.jpg / VID_yyyyMMdd_HHmmss.mp4 from a "yyyyMMdd_HHmmss" [stamp]. */
    fun fileName(type: MediaType, stamp: String, videoExtension: String = "mp4"): String = when (type) {
        MediaType.Image -> "IMG_$stamp.jpg"
        MediaType.Video -> "VID_$stamp.$videoExtension"
    }

    fun mimeType(type: MediaType): String = when (type) {
        MediaType.Image -> "image/jpeg"
        MediaType.Video -> "video/mp4"
    }

    /** The original filtered the folder on ".jpg" / ".mp4"; other camera formats are accepted too. */
    fun matches(name: String, type: MediaType): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (type) {
            MediaType.Image -> ext in IMAGE_EXTENSIONS
            MediaType.Video -> ext in VIDEO_EXTENSIONS
        }
    }

    /** uri.getLastPathSegment(), percent-decoded for file names with spaces. */
    fun lastSegment(uri: String): String = percentDecode(uri.trimEnd('/').substringAfterLast('/'))

    /** Newest first, like the reverse sort of the timestamped names; duplicates removed. */
    fun newestFirst(items: List<MediaItem>): List<MediaItem> =
        items.distinctBy { it.uri }
            .sortedWith(compareByDescending<MediaItem> { it.dateMillis }.thenByDescending { it.name })

    private const val HEX = 16
    private const val ESCAPE_LENGTH = 3

    private fun percentDecode(text: String): String {
        if ('%' !in text) return text
        val bytes = ArrayList<Byte>(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val code = if (c == '%' && i + 2 <= text.lastIndex) {
                text.substring(i + 1, i + ESCAPE_LENGTH).toIntOrNull(HEX)
            } else {
                null
            }
            if (code != null) {
                bytes.add(code.toByte())
                i += ESCAPE_LENGTH
            } else {
                c.toString().encodeToByteArray().forEach { bytes.add(it) }
                i++
            }
        }
        return bytes.toByteArray().decodeToString()
    }
}
