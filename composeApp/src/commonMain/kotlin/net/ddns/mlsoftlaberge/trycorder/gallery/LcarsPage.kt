package net.ddns.mlsoftlaberge.trycorder.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

private const val BAR_WEIGHT = 10f
private const val CONTENT_WEIGHT = 80f
private const val CORNER_WEIGHT = 4f
private const val PANEL_WEIGHT = 6f
private const val STATUS_SP = 18
private val ButtonMargin = 2.dp

/** Height of the 32dp buttons of the secondary screens. */
val LcarsButtonHeight: Dp = 32.dp

/**
 * The frame shared by the gallery, viewer and vision fragments (weightSum 100):
 * a 10% top bar (trek_topleft image, buttons, top status), the 80% content and a 10% bottom bar
 * (trek_bottomleft image, bottom status, buttons). The two corner images go back like the original.
 */
@Composable
fun LcarsPage(
    topStatus: String,
    bottomStatus: String,
    onCorner: () -> Unit,
    topButtons: @Composable RowScope.() -> Unit,
    bottomButtons: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    topStatusSp: Int = STATUS_SP,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().background(TrekPalette.Background)) {
        Row(Modifier.weight(BAR_WEIGHT).fillMaxWidth()) {
            Corner(Res.drawable.trek_topleft, onCorner)
            Column(Modifier.weight(PANEL_WEIGHT).fillMaxHeight()) {
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    content = topButtons,
                )
                StatusLine(topStatus, TrekFont.Status, topStatusSp, Modifier.weight(1f))
            }
        }
        Column(Modifier.weight(CONTENT_WEIGHT).fillMaxWidth(), content = content)
        Row(Modifier.weight(BAR_WEIGHT).fillMaxWidth()) {
            Corner(Res.drawable.trek_bottomleft, onCorner)
            Column(Modifier.weight(PANEL_WEIGHT).fillMaxHeight()) {
                StatusLine(bottomStatus, TrekFont.Action, STATUS_SP, Modifier.weight(1f))
                Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.Bottom, content = bottomButtons)
            }
        }
    }
}

/** One of the 32dp buttons of a bar; [weight] is its layout_weight. */
@Composable
fun RowScope.LcarsButton(
    text: String,
    color: TrekColor,
    edge: TrekEdge,
    weight: Float,
    onClick: () -> Unit,
    fontSize: Int = STATUS_SP,
) {
    TrekButton(
        text = text,
        color = color,
        onClick = onClick,
        modifier = Modifier.weight(weight).height(LcarsButtonHeight),
        edge = edge,
        fontSize = fontSize.sp,
    )
}

@Composable
private fun RowScope.Corner(res: DrawableResource, onClick: () -> Unit) {
    Image(
        painter = painterResource(res),
        contentDescription = "Back",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.weight(CORNER_WEIGHT).fillMaxHeight()
            .padding(horizontal = ButtonMargin)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun StatusLine(text: String, font: TrekFont, sizeSp: Int, modifier: Modifier) {
    Box(modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = TrekPalette.HoloGreenLight,
            fontFamily = font.family(),
            fontWeight = FontWeight.Bold,
            fontSize = sizeSp.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
