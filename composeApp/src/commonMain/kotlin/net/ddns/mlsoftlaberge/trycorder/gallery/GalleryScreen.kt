package net.ddns.mlsoftlaberge.trycorder.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge

private const val GALLERY_TOP_SP = 16
private const val GALLERY_STATUS_SP = 20
private const val THIRD = 2f
private const val QUARTER = 1.5f
private const val HALF = 4f

/**
 * Photo gallery ([video] false, TrygalleryFragment) or Videos viewer ([video] true,
 * TryviewerFragment). Going back to the Trycorder mode uses [onBack], the other modes [onNavigate].
 */
@Composable
fun GalleryScreen(services: PlatformServices, video: Boolean, onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    val store = viewModel(key = if (video) "gallery-video" else "gallery-photo") {
        GalleryStore(video, services.mediaLibrary, services.mediaCapture, services.sound) { screen ->
            if (screen == Screen.Trycorder) onBack() else onNavigate(screen)
        }
    }
    LaunchedEffect(store) { store.dispatch(GalleryIntent.Load) }
    val state by store.state.collectAsStateWithLifecycle()
    if (video) VideoGalleryPage(state, store::dispatch) else PhotoGalleryPage(state, store::dispatch)
}

@Composable
private fun PhotoGalleryPage(state: GalleryState, onIntent: (GalleryIntent) -> Unit) {
    LcarsPage(
        topStatus = state.topStatus,
        bottomStatus = state.bottomStatus,
        onCorner = { onIntent(GalleryIntent.Back) },
        topStatusSp = GALLERY_STATUS_SP,
        topButtons = {
            val size = GALLERY_TOP_SP
            LcarsButton("BACK", TrekColor.Indigo, TrekEdge.Center, THIRD, { onIntent(GalleryIntent.Back) }, size)
            val galleryApp = { onIntent(GalleryIntent.OpenGalleryApp) }
            LcarsButton("GAL", TrekColor.Green, TrekEdge.Center, THIRD, galleryApp, size)
            LcarsButton("SEND", TrekColor.Blue, TrekEdge.Right, THIRD, { onIntent(GalleryIntent.Send) }, size)
        },
        bottomButtons = {
            LcarsButton("PhotoSnap", TrekColor.Green, TrekEdge.Center, HALF, { onIntent(GalleryIntent.TakePhoto) })
            LcarsButton("VideoGal", TrekColor.Blue, TrekEdge.Right, HALF, { onIntent(GalleryIntent.OpenVideos) })
        },
    ) {
        PhotoGalleryContent(
            picture = state.picture?.bytes,
            pictureId = state.picture?.id ?: 0,
            uris = state.items.map { it.uri },
            thumbnails = state.thumbnails,
            selected = state.selected,
            onSelect = { onIntent(GalleryIntent.Select(it)) },
        )
    }
}

@Composable
private fun VideoGalleryPage(state: GalleryState, onIntent: (GalleryIntent) -> Unit) {
    LcarsPage(
        topStatus = state.topStatus,
        bottomStatus = state.bottomStatus,
        onCorner = { onIntent(GalleryIntent.Back) },
        topButtons = {
            LcarsButton("Back", TrekColor.Indigo, TrekEdge.Center, QUARTER, { onIntent(GalleryIntent.Back) })
            LcarsButton("Start", TrekColor.Green, TrekEdge.Center, QUARTER, { onIntent(GalleryIntent.StartVideo) })
            LcarsButton("Pause", TrekColor.Red, TrekEdge.Center, QUARTER, { onIntent(GalleryIntent.PauseVideo) })
            LcarsButton("Play", TrekColor.Gray, TrekEdge.Right, QUARTER, { onIntent(GalleryIntent.RestartVideo) })
        },
        bottomButtons = {
            LcarsButton("Record", TrekColor.Red, TrekEdge.Center, HALF, { onIntent(GalleryIntent.RecordVideo) })
            LcarsButton("PhotoGal", TrekColor.Blue, TrekEdge.Right, HALF, { onIntent(GalleryIntent.OpenPhotos) })
        },
    ) {
        VideoGalleryContent(
            names = state.items.map { it.name },
            selected = state.selected,
            playerUri = state.player.uri,
            playing = state.player.playing,
            startId = state.player.startId,
            onSelect = { onIntent(GalleryIntent.Select(it)) },
            onStarted = { onIntent(GalleryIntent.VideoStarted) },
        )
    }
}
