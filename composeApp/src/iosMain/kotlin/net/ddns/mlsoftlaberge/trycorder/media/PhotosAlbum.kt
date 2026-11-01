package net.ddns.mlsoftlaberge.trycorder.media

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum
import platform.UIKit.UISaveVideoAtPathToSavedPhotosAlbum
import platform.UIKit.UIVideoAtPathIsCompatibleWithSavedPhotosAlbum

/**
 * The Trycorder files are also added to the Photos library (the original broadcast
 * ACTION_MEDIA_SCANNER_SCAN_FILE so the gallery app saw them), when the user allows it.
 */
@OptIn(ExperimentalForeignApi::class)
internal object PhotosAlbum {
    fun save(image: UIImage) = whenAllowed { UIImageWriteToSavedPhotosAlbum(image, null, null, null) }

    fun saveVideo(file: NSURL) {
        val path = file.path ?: return
        if (UIVideoAtPathIsCompatibleWithSavedPhotosAlbum(path)) {
            whenAllowed { UISaveVideoAtPathToSavedPhotosAlbum(path, null, null, null) }
        }
    }

    private fun whenAllowed(action: () -> Unit) {
        when (PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelAddOnly)) {
            PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> action()
            PHAuthorizationStatusNotDetermined ->
                PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { status ->
                    if (status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited) action()
                }
            else -> Unit
        }
    }
}
