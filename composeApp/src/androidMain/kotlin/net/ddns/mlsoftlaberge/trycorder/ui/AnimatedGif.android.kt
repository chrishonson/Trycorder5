package net.ddns.mlsoftlaberge.trycorder.ui

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import java.nio.ByteBuffer

/** GIFView drew the movie at its own pixel size, centered, on 2016 xhdpi phones. */
private const val XHDPI_DENSITY = 2f

/**
 * GIFView port: AnimatedImageDrawable on API 28+, the first frame as a still picture below
 * (android.graphics.Movie is deprecated and does not draw on hardware canvases).
 */
@Composable
actual fun AnimatedGif(resourcePath: String, modifier: Modifier) {
    val resources = LocalContext.current.resources
    val drawable by produceState<Drawable?>(null, resourcePath) {
        val bytes = Res.readBytes(resourcePath)
        value = withContext(Dispatchers.Default) { decodeGif(bytes, resources) }
    }
    DisposableEffect(drawable) {
        val gif = drawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && gif is AnimatedImageDrawable) gif.start()
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && gif is AnimatedImageDrawable) gif.stop()
        }
    }
    var frameTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(drawable) {
        while (isActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && drawable != null) {
            withFrameNanos { frameTime = it }
        }
    }
    Canvas(modifier.clipToBounds()) {
        // Reading the frame clock here redraws the canvas on every frame.
        val gif = drawable?.takeIf { frameTime >= 0L } ?: return@Canvas
        val scale = density / XHDPI_DENSITY
        val w = gif.intrinsicWidth * scale
        val h = gif.intrinsicHeight * scale
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val saved = native.save()
            native.translate((size.width - w) / 2f, (size.height - h) / 2f)
            native.scale(scale, scale)
            gif.setBounds(0, 0, gif.intrinsicWidth, gif.intrinsicHeight)
            gif.draw(native)
            native.restoreToCount(saved)
        }
    }
}

private fun decodeGif(bytes: ByteArray, resources: Resources): Drawable? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        decodeAnimated(bytes)
    } else {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { BitmapDrawable(resources, it) }
    }

@RequiresApi(Build.VERSION_CODES.P)
private fun decodeAnimated(bytes: ByteArray): Drawable =
    ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(bytes)))
