package net.ddns.mlsoftlaberge.trycorder.inventory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import net.ddns.mlsoftlaberge.trycorder.resources.trek_topleft
import net.ddns.mlsoftlaberge.trycorder.ui.TrekButton
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFont
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import org.jetbrains.compose.resources.painterResource

/*
 * LCARS pieces shared by the secondary screens ported from activities
 * (inventory, crew): the header with the trek_topleft image and the button row,
 * the black EditText, and a Toast replacement.
 */

private val HeaderButtonHeight = 32.dp
private val ButtonMargin = 2.dp
private const val HEADER_BUTTON_SP = 16
private const val TITLE_SP = 20
private const val TOAST_MILLIS = 2000L
private const val TOAST_SP = 16
private val ToastBackground = Color(0xE0303030)

/**
 * Header of the list/detail activities: the trek_topleft image (also a back button) on the left,
 * a row of buttons over a second row (a holo green title by default) on the right.
 */
@Composable
fun TrekPageHeader(
    leftWeight: Float,
    rightWeight: Float,
    onTopLeft: () -> Unit,
    modifier: Modifier = Modifier,
    buttons: @Composable RowScope.() -> Unit,
    bottom: @Composable RowScope.() -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth().background(TrekPalette.Background)) {
        Image(
            painter = painterResource(Res.drawable.trek_topleft),
            contentDescription = "Back",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .weight(leftWeight)
                .fillMaxHeight()
                .padding(end = ButtonMargin, bottom = ButtonMargin)
                .clickable(onClick = onTopLeft),
        )
        Column(Modifier.weight(rightWeight).fillMaxHeight()) {
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = buttons)
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.Top, content = bottom)
        }
    }
}

/** A 32dp header button with the default typeface, like the activity layouts. */
@Composable
fun RowScope.HeaderButton(text: String, color: TrekColor, edge: TrekEdge, weight: Float, onClick: () -> Unit) {
    TrekButton(
        text = text,
        color = color,
        onClick = onClick,
        modifier = Modifier.weight(weight).height(HeaderButtonHeight),
        edge = edge,
        font = TrekFont.Default,
        fontSize = HEADER_BUTTON_SP.sp,
    )
}

/** The holo green 20sp bold title under the header buttons. */
@Composable
fun HeaderTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TrekPalette.HoloGreenLight,
        fontSize = TITLE_SP.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier,
    )
}

/** The black EditText with white text of the detail layouts. */
@Composable
fun TrekField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    fontSize: Int = TITLE_SP,
    singleLine: Boolean = true,
    numeric: Boolean = false,
) {
    val style = TextStyle(color = TrekPalette.Text, fontSize = fontSize.sp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(TrekPalette.HoloBlueLight),
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
        modifier = modifier.background(TrekPalette.Background).padding(vertical = 4.dp, horizontal = 4.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, style = style.copy(color = Color.Gray))
                inner()
            }
        },
    )
}

/** Replacement of the original Toasts: a short message at the bottom, dismissed after two seconds. */
@Composable
fun BoxScope.TrekToast(message: String?, onDismiss: () -> Unit) {
    if (message == null) return
    LaunchedEffect(message) {
        delay(TOAST_MILLIS)
        onDismiss()
    }
    Text(
        text = message,
        color = TrekPalette.Text,
        fontSize = TOAST_SP.sp,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 48.dp)
            .background(ToastBackground, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
