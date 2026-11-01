package net.ddns.mlsoftlaberge.trycorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private const val LOG_SP = 16
private const val INPUT_SP = 18

/** A scrolling log TextView (setHorizontallyScrolling + ScrollingMovementMethod). */
@Composable
fun TrekLog(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    font: TrekFont = TrekFont.Default,
    bold: Boolean = true,
    fontSize: Int = LOG_SP,
    wrap: Boolean = false,
) {
    val inner = if (wrap) Modifier else Modifier.horizontalScroll(rememberScrollState())
    Box(modifier = modifier.background(TrekPalette.Background).verticalScroll(rememberScrollState())) {
        Text(
            text = text,
            color = color,
            fontFamily = font.family(),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = fontSize.sp,
            softWrap = wrap,
            modifier = inner,
        )
    }
}

/** The white EditText with black bold text used by the chat and command windows. */
@Composable
fun TrekInput(text: String, onTextChange: (String) -> Unit, modifier: Modifier = Modifier, singleLine: Boolean = true) {
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        singleLine = singleLine,
        textStyle = TextStyle(color = Color.Black, fontSize = INPUT_SP.sp, fontWeight = FontWeight.Bold),
        modifier = modifier.background(Color.White),
    )
}
