package net.ddns.mlsoftlaberge.trycorder.gallery

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ddns.mlsoftlaberge.trycorder.media.VideoPlayer
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import net.ddns.mlsoftlaberge.trycorder.resources.btn_check_on
import net.ddns.mlsoftlaberge.trycorder.resources.picture_frame
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFrameBar
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource

private const val SWITCHER_WEIGHT = 70f
private const val STRIP_WEIGHT = 10f
private const val LIST_WEIGHT = 50f
private const val VIDEO_WEIGHT = 48f
private const val FRAME_CORNER = 1f
private const val FRAME_LINE = 8f
private const val ROW_TEXT_SP = 20
private const val UNSELECTED_ALPHA = 0.85f
private val StripSpacing = 8.dp
private val FramePadding = 4.dp
private val ThumbMinWidth = 40.dp
private val IconSize = 22.dp
private val IconStart = 4.dp
private val IconEnd = 10.dp
private val IconTop = 4.dp

/** ImageSwitcher (fade in/out) over the Gallery strip of thumbnails in picture frames. */
@Composable
fun ColumnScope.PhotoGalleryContent(
    picture: ByteArray?,
    pictureId: Int,
    uris: List<String>,
    thumbnails: Map<String, ByteArray>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Crossfade(targetState = pictureId, modifier = Modifier.weight(SWITCHER_WEIGHT).fillMaxWidth()) { id ->
        val bitmap = remember(id) { decode(picture) }
        Box(Modifier.fillMaxSize().background(TrekPalette.Background), contentAlignment = Alignment.Center) {
            if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(selected) { if (selected >= 0) listState.animateScrollToItem(selected) }
    LazyRow(
        state = listState,
        modifier = Modifier.weight(STRIP_WEIGHT).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(StripSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(uris) { index, uri ->
            Thumbnail(thumbnails[uri], index == selected) { onSelect(index) }
        }
    }
}

@Composable
private fun Thumbnail(bytes: ByteArray?, selected: Boolean, onClick: () -> Unit) {
    val bitmap = remember(bytes) { decode(bytes) }
    Box(
        Modifier.fillMaxHeight()
            .widthIn(min = ThumbMinWidth)
            .paint(painterResource(Res.drawable.picture_frame), contentScale = ContentScale.FillBounds)
            .padding(FramePadding)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alpha = if (selected) 1f else UNSELECTED_ALPHA,
                modifier = Modifier.fillMaxHeight(),
            )
        }
    }
}

/** The list of videos, then the VideoView between two LCARS bars. */
@Composable
fun ColumnScope.VideoGalleryContent(
    names: List<String>,
    selected: Int,
    playerUri: String?,
    playing: Boolean,
    startId: Int,
    onSelect: (Int) -> Unit,
    onStarted: () -> Unit,
) {
    LazyColumn(Modifier.weight(LIST_WEIGHT).fillMaxWidth()) {
        itemsIndexed(names) { index, name ->
            Row(
                Modifier.fillMaxWidth()
                    .background(if (index == selected) TrekPalette.PressedTop else TrekPalette.Background)
                    .clickable { onSelect(index) },
            ) {
                Image(
                    painterResource(Res.drawable.btn_check_on), null,
                    Modifier.padding(start = IconStart, end = IconEnd, top = IconTop).size(IconSize),
                )
                Text(name, color = TrekPalette.HoloBlueLight, fontSize = ROW_TEXT_SP.sp)
            }
        }
    }
    TrekFrameBar(top = true, cornerWeight = FRAME_CORNER, lineWeight = FRAME_LINE)
    Box(Modifier.weight(VIDEO_WEIGHT).fillMaxWidth().background(TrekPalette.Background)) {
        if (playerUri != null && startId > 0) {
            VideoPlayer(playerUri, playing, startId, onStarted, Modifier.fillMaxSize())
        }
    }
    TrekFrameBar(top = false, cornerWeight = FRAME_CORNER, lineWeight = FRAME_LINE)
}

private fun decode(bytes: ByteArray?): ImageBitmap? =
    bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
