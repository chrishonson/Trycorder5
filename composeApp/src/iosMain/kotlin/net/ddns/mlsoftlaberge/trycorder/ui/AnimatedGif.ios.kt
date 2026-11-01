package net.ddns.mlsoftlaberge.trycorder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image

/** GIFView drew the movie at its own pixel size, centered, on 2016 xhdpi phones. */
private const val XHDPI_DENSITY = 2f

/** Browsers and Android use 100 ms for GIF frames that declare a too short delay. */
private const val MIN_FRAME_MS = 20
private const val DEFAULT_FRAME_MS = 100L

/** GIFView port: Skia decodes one frame at a time into a reused bitmap. */
@Composable
actual fun AnimatedGif(resourcePath: String, modifier: Modifier) {
    val codec by produceState<Codec?>(null, resourcePath) {
        value = Codec.makeFromData(Data.makeFromBytes(Res.readBytes(resourcePath)))
    }
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(codec) {
        val gif = codec ?: return@LaunchedEffect
        val bitmap = Bitmap().apply { allocPixels(gif.imageInfo) }
        var index = 0
        while (isActive) {
            // Frames are decoded in order, so the bitmap already holds the frame they build on.
            if (index == 0) gif.readPixels(bitmap, 0) else gif.readPixels(bitmap, index, index - 1)
            frame = Image.makeFromBitmap(bitmap).toComposeImageBitmap()
            delay(frameDuration(gif, index))
            index = (index + 1) % maxOf(1, gif.frameCount)
        }
    }
    Canvas(modifier.clipToBounds()) {
        val image = frame ?: return@Canvas
        val scale = density / XHDPI_DENSITY
        val w = (image.width * scale).toInt()
        val h = (image.height * scale).toInt()
        val topLeft = Offset((size.width - w) / 2f, (size.height - h) / 2f)
        drawImage(
            image = image,
            dstOffset = IntOffset(topLeft.x.toInt(), topLeft.y.toInt()),
            dstSize = IntSize(w, h),
        )
    }
}

private fun frameDuration(codec: Codec, index: Int): Long {
    val duration = if (codec.frameCount > 1) codec.getFrameInfo(index).duration else 0
    return if (duration < MIN_FRAME_MS) DEFAULT_FRAME_MS else duration.toLong()
}
