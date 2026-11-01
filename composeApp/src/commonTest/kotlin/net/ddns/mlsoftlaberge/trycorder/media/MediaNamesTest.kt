package net.ddns.mlsoftlaberge.trycorder.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaNamesTest {
    @Test
    fun fileNamesOfFileUtils() {
        assertEquals("IMG_20161116_101530.jpg", MediaNames.fileName(MediaType.Image, "20161116_101530"))
        assertEquals("VID_20161116_101530.mp4", MediaNames.fileName(MediaType.Video, "20161116_101530"))
        assertEquals("VID_20161116_101530.mov", MediaNames.fileName(MediaType.Video, "20161116_101530", "mov"))
    }

    @Test
    fun extensionsByType() {
        assertTrue(MediaNames.matches("IMG_1.JPG", MediaType.Image))
        assertTrue(MediaNames.matches("a.heic", MediaType.Image))
        assertFalse(MediaNames.matches("VID_1.mp4", MediaType.Image))
        assertTrue(MediaNames.matches("VID_1.mp4", MediaType.Video))
        assertTrue(MediaNames.matches("clip.MOV", MediaType.Video))
        assertFalse(MediaNames.matches("notes", MediaType.Video))
    }

    @Test
    fun lastSegmentIsDecoded() {
        assertEquals("IMG_1.jpg", MediaNames.lastSegment("file:///var/Documents/Trycorder/IMG_1.jpg"))
        assertEquals("my photo é.jpg", MediaNames.lastSegment("file:///x/my%20photo%20%C3%A9.jpg"))
        assertEquals("100%", MediaNames.lastSegment("content://x/100%"))
    }

    @Test
    fun newestFirstWithoutDuplicates() {
        val a = MediaItem("u1", "IMG_1.jpg", MediaType.Image, 10)
        val b = MediaItem("u2", "IMG_2.jpg", MediaType.Image, 20)
        val c = MediaItem("u3", "IMG_3.jpg", MediaType.Image, 20)
        assertEquals(listOf(c, b, a), MediaNames.newestFirst(listOf(a, b, c, a)))
    }
}
