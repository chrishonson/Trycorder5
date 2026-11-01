package net.ddns.mlsoftlaberge.trycorder.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.media.CaptureResult
import net.ddns.mlsoftlaberge.trycorder.media.MediaCapture
import net.ddns.mlsoftlaberge.trycorder.media.MediaLibrary
import net.ddns.mlsoftlaberge.trycorder.media.MediaType

/**
 * Store of the Photo gallery (TrygalleryFragment) and of the Videos viewer (TryviewerFragment).
 * [navigate] is the onTrygalleryModeChange / onTryviewerModeChange of the original activity.
 */
class GalleryStore(
    video: Boolean,
    private val library: MediaLibrary,
    private val capture: MediaCapture,
    private val sound: SoundPlayer,
    private val navigate: (Screen) -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(GalleryState(video = video))
    val state: StateFlow<GalleryState> = _state.asStateFlow()

    private val type = if (video) MediaType.Video else MediaType.Image
    private var loadJob: Job? = null
    private var pictureJob: Job? = null
    private var pictureId = 0

    fun dispatch(intent: GalleryIntent) {
        when (intent) {
            GalleryIntent.Load -> load(resetPlayer = true)
            GalleryIntent.Back -> modeChange(Screen.Trycorder)
            is GalleryIntent.Select -> select(intent.index)
            GalleryIntent.OpenGalleryApp -> openGalleryApp()
            GalleryIntent.Send -> send()
            GalleryIntent.TakePhoto -> buttonSound().also { takePhoto() }
            GalleryIntent.OpenVideos -> modeChange(Screen.VideoGallery)
            GalleryIntent.OpenPhotos -> modeChange(Screen.PhotoGallery)
            GalleryIntent.RecordVideo -> buttonSound().also { recordVideo() }
            else -> onPlayer(intent)
        }
    }

    private fun onPlayer(intent: GalleryIntent) {
        when (intent) {
            GalleryIntent.StartVideo -> buttonSound().also { startVideo() }
            GalleryIntent.PauseVideo -> {
                buttonSound()
                _state.update { it.copy(player = it.player.copy(playing = false)) }
                say("Video is paused")
            }
            GalleryIntent.RestartVideo -> {
                buttonSound()
                _state.update { it.copy(player = it.player.copy(playing = true)) }
                say("Video is restarted")
            }
            GalleryIntent.VideoStarted -> say("Video is started")
            else -> Unit
        }
    }

    /** loadimageuris() / filllistview(). */
    private fun load(resetPlayer: Boolean) {
        loadJob?.cancel()
        if (state.value.video) say("Loading videos Uris") else say(GalleryState.READY)
        if (resetPlayer) _state.update { it.copy(player = PlayerState()) }
        loadJob = viewModelScope.launch {
            val items = library.list(type)
            _state.update { it.copy(items = items, selected = -1, picture = null) }
            if (state.value.video) {
                if (items.isNotEmpty()) {
                    _state.update { it.copy(selected = 0, player = it.player.copy(uri = items.first().uri)) }
                }
                say("Videos Uris loaded")
            } else {
                if (items.isNotEmpty()) select(0, sound = false)
                loadThumbnails()
            }
        }
    }

    private suspend fun loadThumbnails() {
        state.value.items.forEach { item ->
            if (item.uri !in state.value.thumbnails) {
                val bytes = library.thumbnail(item.uri, GalleryState.THUMBNAIL_SIZE) ?: return@forEach
                _state.update { it.copy(thumbnails = it.thumbnails + (item.uri to bytes)) }
            }
        }
    }

    /** onItemSelected() of the Gallery, onItemClick() of the videos list. */
    private fun select(index: Int, sound: Boolean = true) {
        val item = state.value.items.getOrNull(index) ?: return
        _state.update { it.copy(selected = index) }
        if (state.value.video) {
            if (sound) buttonSound()
            _state.update { it.copy(player = it.player.copy(uri = item.uri)) }
            startVideo()
            return
        }
        pictureJob?.cancel()
        pictureJob = viewModelScope.launch {
            val bytes = library.thumbnail(item.uri, GalleryState.PICTURE_SIZE)
            if (bytes != null) _state.update { it.copy(picture = GalleryPicture(bytes, ++pictureId)) }
        }
    }

    /** buttongallery(): the external gallery application. */
    private fun openGalleryApp() {
        buttonSound()
        if (!library.openGallery(type)) say("No gallery application")
    }

    /** buttonsend(): share the current picture. */
    private fun send() {
        buttonSound()
        val item = state.value.items.getOrNull(state.value.selected) ?: return
        if (!library.share(item)) say("Cannot send ${item.name}")
    }

    private fun takePhoto() {
        say("Open Photo application")
        viewModelScope.launch {
            when (val result = capture.takePhoto()) {
                is CaptureResult.Saved -> {
                    say("Saved to ${result.uri}")
                    if (!state.value.video) reloadKeepingStatus()
                }
                CaptureResult.Cancelled -> say("Cancelled Photo")
                CaptureResult.Failed -> say("Failed Saving Photo")
            }
        }
    }

    private fun recordVideo() {
        say("Open Video application")
        viewModelScope.launch {
            when (val result = capture.recordVideo()) {
                is CaptureResult.Saved -> {
                    say("Saved to ${result.uri}")
                    if (state.value.video) reloadKeepingStatus()
                }
                CaptureResult.Cancelled -> say("Cancelled Video")
                CaptureResult.Failed -> say("Failed Saving Video")
            }
        }
    }

    /** After a capture the list is reloaded but the "Saved to" message stays. */
    private suspend fun reloadKeepingStatus() {
        val status = state.value.bottomStatus
        load(resetPlayer = false)
        loadJob?.join()
        say(status)
    }

    /** startvideo(): play the current uri from the start. */
    private fun startVideo() {
        say("Start Video")
        _state.update {
            val player = it.player
            if (player.uri == null) it else it.copy(player = player.copy(playing = true, startId = player.startId + 1))
        }
    }

    private fun modeChange(screen: Screen) {
        buttonSound()
        navigate(screen)
    }

    private fun buttonSound() = sound.play(TrekSound.KeyOk)

    private fun say(text: String) = _state.update { it.copy(bottomStatus = text) }
}
