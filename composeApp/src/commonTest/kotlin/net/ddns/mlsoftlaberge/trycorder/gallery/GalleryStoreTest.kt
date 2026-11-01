package net.ddns.mlsoftlaberge.trycorder.gallery

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.media.CaptureResult
import net.ddns.mlsoftlaberge.trycorder.media.MediaCapture
import net.ddns.mlsoftlaberge.trycorder.media.MediaItem
import net.ddns.mlsoftlaberge.trycorder.media.MediaLibrary
import net.ddns.mlsoftlaberge.trycorder.media.MediaType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeMediaLibrary(
    var images: List<MediaItem> = emptyList(),
    var videos: List<MediaItem> = emptyList(),
) : MediaLibrary {
    val opened = mutableListOf<MediaType>()
    val shared = mutableListOf<MediaItem>()
    var galleryAvailable = true
    val thumbnailRequests = mutableListOf<Pair<String, Int>>()

    override suspend fun list(type: MediaType) = if (type == MediaType.Image) images else videos
    override suspend fun delete(item: MediaItem) = false
    override suspend fun read(uri: String): ByteArray? = uri.encodeToByteArray()
    override suspend fun thumbnail(uri: String, size: Int): ByteArray? {
        thumbnailRequests += uri to size
        return "$uri@$size".encodeToByteArray()
    }

    override fun openGallery(type: MediaType): Boolean {
        opened += type
        return galleryAvailable
    }

    override fun share(item: MediaItem): Boolean {
        shared += item
        return true
    }
}

class FakeMediaCapture : MediaCapture {
    var photo = CompletableDeferred<CaptureResult>()
    var video = CompletableDeferred<CaptureResult>()
    override suspend fun takePhoto() = photo.await()
    override suspend fun recordVideo() = video.await()
}

class RecordingSound : SoundPlayer {
    val played = mutableListOf<TrekSound>()
    override fun play(sound: TrekSound) {
        played += sound
    }

    override fun startLoop(sound: TrekSound) = Unit
    override fun stopLoop() = Unit
    override fun release() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryStoreTest {
    private val image1 = MediaItem("content://img/2", "IMG_2.jpg", MediaType.Image, 2)
    private val image2 = MediaItem("content://img/1", "IMG_1.jpg", MediaType.Image, 1)
    private val video1 = MediaItem("content://vid/1", "VID_1.mp4", MediaType.Video, 1)
    private val video2 = MediaItem("content://vid/2", "VID_2.mp4", MediaType.Video, 2)

    private val library = FakeMediaLibrary(listOf(image1, image2), listOf(video1, video2))
    private val capture = FakeMediaCapture()
    private val sound = RecordingSound()
    private val navigated = mutableListOf<Screen>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.store(video: Boolean): GalleryStore {
        val store = GalleryStore(video, library, capture, sound) { navigated += it }
        store.dispatch(GalleryIntent.Load)
        advanceUntilIdle()
        return store
    }

    @Test
    fun photoGalleryShowsTheFirstPictureAndThumbnails() = runTest {
        val store = store(video = false)
        val state = store.state.value
        assertEquals("GALLERY", state.topStatus)
        assertEquals("Ready", state.bottomStatus)
        assertEquals(listOf(image1, image2), state.items)
        assertEquals(0, state.selected)
        assertContentEquals("content://img/2@800".encodeToByteArray(), state.picture?.bytes)
        assertEquals(setOf(image1.uri, image2.uri), state.thumbnails.keys)
        assertTrue(library.thumbnailRequests.contains(image2.uri to GalleryState.THUMBNAIL_SIZE))
    }

    @Test
    fun selectingAPictureShowsIt() = runTest {
        val store = store(video = false)
        val firstId = store.state.value.picture?.id
        store.dispatch(GalleryIntent.Select(1))
        advanceUntilIdle()
        assertEquals(1, store.state.value.selected)
        assertContentEquals("content://img/1@800".encodeToByteArray(), store.state.value.picture?.bytes)
        assertTrue(store.state.value.picture?.id != firstId)
    }

    @Test
    fun backAndModeButtonsNavigateWithASound() = runTest {
        val store = store(video = false)
        store.dispatch(GalleryIntent.Back)
        store.dispatch(GalleryIntent.OpenVideos)
        store.dispatch(GalleryIntent.OpenPhotos)
        assertEquals(listOf(Screen.Trycorder, Screen.VideoGallery, Screen.PhotoGallery), navigated)
        assertEquals(List(3) { TrekSound.KeyOk }, sound.played)
    }

    @Test
    fun galleryAppAndSend() = runTest {
        val store = store(video = false)
        store.dispatch(GalleryIntent.OpenGalleryApp)
        store.dispatch(GalleryIntent.Send)
        assertEquals(listOf(MediaType.Image), library.opened)
        assertEquals(listOf(image1), library.shared)
        library.galleryAvailable = false
        store.dispatch(GalleryIntent.OpenGalleryApp)
        assertEquals("No gallery application", store.state.value.bottomStatus)
    }

    @Test
    fun takePhotoReloadsAndKeepsTheSavedMessage() = runTest {
        val store = store(video = false)
        store.dispatch(GalleryIntent.TakePhoto)
        advanceUntilIdle()
        assertEquals("Open Photo application", store.state.value.bottomStatus)
        val newest = MediaItem("content://img/3", "IMG_3.jpg", MediaType.Image, 3)
        library.images = listOf(newest, image1, image2)
        capture.photo.complete(CaptureResult.Saved(newest.uri))
        advanceUntilIdle()
        val state = store.state.value
        assertEquals("Saved to content://img/3", state.bottomStatus)
        assertEquals(newest, state.items.first())
        assertEquals(0, state.selected)
    }

    @Test
    fun cancelledAndFailedCaptures() = runTest {
        val store = store(video = false)
        store.dispatch(GalleryIntent.TakePhoto)
        capture.photo.complete(CaptureResult.Cancelled)
        advanceUntilIdle()
        assertEquals("Cancelled Photo", store.state.value.bottomStatus)
        store.dispatch(GalleryIntent.RecordVideo)
        capture.video.complete(CaptureResult.Failed)
        advanceUntilIdle()
        assertEquals("Failed Saving Video", store.state.value.bottomStatus)
    }

    @Test
    fun videoViewerLoadsTheListWithoutPlaying() = runTest {
        val store = store(video = true)
        val state = store.state.value
        assertEquals("Videos Viewer", state.topStatus)
        assertEquals("Videos Uris loaded", state.bottomStatus)
        assertEquals(video1.uri, state.player.uri)
        assertEquals(0, state.player.startId)
        assertTrue(library.thumbnailRequests.isEmpty())
    }

    @Test
    fun videoStartPauseRestart() = runTest {
        val store = store(video = true)
        store.dispatch(GalleryIntent.StartVideo)
        assertEquals("Start Video", store.state.value.bottomStatus)
        assertEquals(PlayerState(video1.uri, playing = true, startId = 1), store.state.value.player)
        store.dispatch(GalleryIntent.VideoStarted)
        assertEquals("Video is started", store.state.value.bottomStatus)
        store.dispatch(GalleryIntent.PauseVideo)
        assertEquals("Video is paused", store.state.value.bottomStatus)
        assertEquals(false, store.state.value.player.playing)
        store.dispatch(GalleryIntent.RestartVideo)
        assertEquals("Video is restarted", store.state.value.bottomStatus)
        assertEquals(PlayerState(video1.uri, playing = true, startId = 1), store.state.value.player)
    }

    @Test
    fun clickingAVideoPlaysIt() = runTest {
        val store = store(video = true)
        store.dispatch(GalleryIntent.Select(1))
        assertEquals(PlayerState(video2.uri, playing = true, startId = 1), store.state.value.player)
        assertEquals(1, store.state.value.selected)
    }

    @Test
    fun emptyFolders() = runTest {
        library.images = emptyList()
        library.videos = emptyList()
        val photos = store(video = false)
        assertNull(photos.state.value.picture)
        photos.dispatch(GalleryIntent.Send)
        assertTrue(library.shared.isEmpty())
        val videos = store(video = true)
        videos.dispatch(GalleryIntent.StartVideo)
        assertNull(videos.state.value.player.uri)
        assertEquals(0, videos.state.value.player.startId)
    }
}
