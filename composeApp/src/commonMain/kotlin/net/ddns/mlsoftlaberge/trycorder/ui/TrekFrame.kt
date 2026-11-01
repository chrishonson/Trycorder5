package net.ddns.mlsoftlaberge.trycorder.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import net.ddns.mlsoftlaberge.trycorder.resources.trek_bottomleft
import net.ddns.mlsoftlaberge.trycorder.resources.trek_bottomright
import net.ddns.mlsoftlaberge.trycorder.resources.trek_horizontalline
import net.ddns.mlsoftlaberge.trycorder.resources.trek_topleft
import net.ddns.mlsoftlaberge.trycorder.resources.trek_topright
import net.ddns.mlsoftlaberge.trycorder.resources.trek_verticalline
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val CornerHeight = 8.dp
private val LineHeight = 4.dp

/**
 * Horizontal bar of the LCARS frame: corner image, stretched line, corner image.
 * [cornerWeight]/[lineWeight] reproduce the layout_weight values of the original XML.
 */
@Composable
fun TrekFrameBar(top: Boolean, cornerWeight: Float, lineWeight: Float, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        FrameImage(if (top) Res.drawable.trek_topleft else Res.drawable.trek_bottomleft, cornerWeight, CornerHeight)
        FrameImage(Res.drawable.trek_horizontalline, lineWeight, LineHeight)
        FrameImage(if (top) Res.drawable.trek_topright else Res.drawable.trek_bottomright, cornerWeight, CornerHeight)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.FrameImage(
    res: DrawableResource,
    weight: Float,
    height: androidx.compose.ui.unit.Dp,
) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.weight(weight).height(height),
    )
}

/** Vertical LCARS line used on both sides of the sensor area. */
@Composable
fun TrekVerticalLine(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.trek_verticalline),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier.fillMaxHeight(),
    )
}

/** A full LCARS box: top bar, content, bottom bar (the viewer area of the original layout). */
@Composable
fun TrekBox(
    modifier: Modifier = Modifier,
    cornerWeight: Float = 1f,
    lineWeight: Float = 8f,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(modifier = modifier.background(TrekPalette.Background)) {
        TrekFrameBar(top = true, cornerWeight = cornerWeight, lineWeight = lineWeight)
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), content = content)
        TrekFrameBar(top = false, cornerWeight = cornerWeight, lineWeight = lineWeight)
    }
}
