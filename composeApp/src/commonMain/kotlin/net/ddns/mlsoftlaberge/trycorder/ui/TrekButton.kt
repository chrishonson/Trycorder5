package net.ddns.mlsoftlaberge.trycorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Corner layout of the trekbutton drawables: `_left`, `_right`, `_center` or plain. */
enum class TrekEdge { Round, Left, Right, Center }

private val CornerRadius = 20.dp
private val ButtonMargin = 2.dp
private val StrokeWidth = 1.dp
private val TextPadding = 2.dp
private const val DEFAULT_TEXT_SP = 18

fun TrekEdge.shape(): Shape = when (this) {
    TrekEdge.Round -> RoundedCornerShape(CornerRadius)
    TrekEdge.Left -> RoundedCornerShape(topStart = CornerRadius, bottomStart = CornerRadius)
    TrekEdge.Right -> RoundedCornerShape(topEnd = CornerRadius, bottomEnd = CornerRadius)
    TrekEdge.Center -> RoundedCornerShape(0.dp)
}

/** Background of a trek button: gradient, 1dp stroke, pressed state like the original selectors. */
@Composable
fun Modifier.trekBackground(color: TrekColor, edge: TrekEdge, pressed: Boolean = false): Modifier {
    val shape = edge.shape()
    val brush = if (pressed) {
        Brush.verticalGradient(listOf(TrekPalette.PressedTop, TrekPalette.PressedBottom))
    } else {
        Brush.verticalGradient(listOf(color.top, color.bottom))
    }
    return this
        .clip(shape)
        .background(brush, shape)
        .border(StrokeWidth, TrekPalette.Stroke, shape)
}

/**
 * The LCARS-style button used everywhere in the Trycorder.
 * Pure: takes raw values and a click lambda.
 */
@Composable
fun TrekButton(
    text: String,
    color: TrekColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    edge: TrekEdge = TrekEdge.Round,
    font: TrekFont = TrekFont.Button,
    fontSize: TextUnit = DEFAULT_TEXT_SP.sp,
    bold: Boolean = true,
    maxLines: Int = 1,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .padding(ButtonMargin)
            .trekBackground(color, edge, pressed)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = TrekPalette.ButtonText,
            fontFamily = font.family(),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(horizontal = TextPadding),
        )
    }
}
