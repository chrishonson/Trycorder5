package net.ddns.mlsoftlaberge.trycorder.gallery

import net.ddns.mlsoftlaberge.trycorder.media.MediaItem

/** The picture shown big in the photo gallery (the ImageSwitcher). Compared by [id] only. */
class GalleryPicture(val bytes: ByteArray, val id: Int) {
    override fun equals(other: Any?) = other is GalleryPicture && other.id == id
    override fun hashCode() = id
}

/** The VideoView of the videos viewer: [startId] changes each time a video is (re)started. */
data class PlayerState(val uri: String? = null, val playing: Boolean = false, val startId: Int = 0)

/**
 * State of the Photo gallery (TrygalleryFragment, [video] false) and of the Videos viewer
 * (TryviewerFragment, [video] true).
 */
data class GalleryState(
    val video: Boolean = false,
    val items: List<MediaItem> = emptyList(),
    val selected: Int = -1,
    val picture: GalleryPicture? = null,
    /** Small pictures of the Gallery strip, by uri. */
    val thumbnails: Map<String, ByteArray> = emptyMap(),
    val player: PlayerState = PlayerState(),
    val bottomStatus: String = READY,
) {
    val topStatus: String get() = if (video) "Videos Viewer" else "GALLERY"

    companion object {
        const val READY = "Ready"

        /** decodeFile(f, 800) of the ImageSwitcher and decodeFile(f, 100) of the Gallery strip. */
        const val PICTURE_SIZE = 800
        const val THUMBNAIL_SIZE = 100
    }
}

sealed interface GalleryIntent {
    /** The screen is shown: reload the folder (onCreateView). */
    data object Load : GalleryIntent

    /** Top-left / bottom-left frame images and the Back button: back to the Trycorder mode. */
    data object Back : GalleryIntent

    /** Gallery item selected / video list row clicked. */
    data class Select(val index: Int) : GalleryIntent

    // Photo gallery
    data object OpenGalleryApp : GalleryIntent
    data object Send : GalleryIntent
    data object TakePhoto : GalleryIntent
    data object OpenVideos : GalleryIntent

    // Videos viewer
    data object StartVideo : GalleryIntent
    data object PauseVideo : GalleryIntent
    data object RestartVideo : GalleryIntent
    data object VideoStarted : GalleryIntent
    data object RecordVideo : GalleryIntent
    data object OpenPhotos : GalleryIntent
}
