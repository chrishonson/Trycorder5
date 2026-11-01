package net.ddns.mlsoftlaberge.trycorder.trycorder

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import net.ddns.mlsoftlaberge.trycorder.resources.trek_bottomleft
import net.ddns.mlsoftlaberge.trycorder.resources.trek_topleft
import net.ddns.mlsoftlaberge.trycorder.ui.TrekButton
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFont
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import net.ddns.mlsoftlaberge.trycorder.ui.family
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val BarButtonHeight = 32.dp
private const val CORNER_WEIGHT = 4f
private const val BAR_WEIGHT = 6f
private const val TOP_BUTTON_WEIGHT = 1.5f
private const val STATUS_SP = 18

/** A top or bottom bar button (fixed 32dp high like the original). */
data class BarButton(val text: String, val color: TrekColor, val edge: TrekEdge, val onClick: () -> Unit)

/**
 * Top bar: LCARS corner (tap to talk), Sound/Start/Stop/Setup, top status line.
 * Bottom bar mirrors it: corner (tap to snap), bottom status line, Snap/Photo/Record/Gallery.
 */
@Composable
fun TrekBar(
    top: Boolean,
    status: String,
    buttons: List<BarButton>,
    onCorner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().background(TrekPalette.Background)) {
        CornerImage(
            res = if (top) Res.drawable.trek_topleft else Res.drawable.trek_bottomleft,
            onClick = onCorner,
            modifier = Modifier.weight(CORNER_WEIGHT).fillMaxHeight(),
        )
        Column(modifier = Modifier.weight(BAR_WEIGHT).fillMaxHeight()) {
            if (top) {
                BarButtons(buttons, Modifier.weight(1f))
                StatusLine(status, Modifier.weight(1f))
            } else {
                StatusLine(status, Modifier.weight(1f))
                BarButtons(buttons, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CornerImage(res: DrawableResource, onClick: () -> Unit, modifier: Modifier) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun BarButtons(buttons: List<BarButton>, modifier: Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        buttons.forEach { b ->
            TrekButton(
                text = b.text,
                color = b.color,
                edge = b.edge,
                onClick = b.onClick,
                modifier = Modifier.weight(TOP_BUTTON_WEIGHT).height(BarButtonHeight),
            )
        }
    }
}

@Composable
private fun StatusLine(text: String, modifier: Modifier) {
    Text(
        text = text,
        color = TrekPalette.HoloGreenLight,
        fontFamily = TrekFont.Status.family(),
        fontWeight = FontWeight.Bold,
        fontSize = STATUS_SP.sp,
        maxLines = 1,
        modifier = modifier.fillMaxSize(),
    )
}

/** One button of the left column. */
data class SideButton(val text: String, val color: TrekColor, val group: ButtonGroup, val gapBefore: Float)

/** The left column of group buttons with the spacing weights of the original layout. */
@Composable
fun SideColumn(
    buttons: List<SideButton>,
    lastGap: Float,
    onSelect: (ButtonGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxHeight().background(TrekPalette.Background)) {
        buttons.forEach { b ->
            Spacer(Modifier.weight(b.gapBefore))
            TrekButton(
                text = b.text,
                color = b.color,
                edge = TrekEdge.Right,
                onClick = { onSelect(b.group) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
        Spacer(Modifier.weight(lastGap))
    }
}
