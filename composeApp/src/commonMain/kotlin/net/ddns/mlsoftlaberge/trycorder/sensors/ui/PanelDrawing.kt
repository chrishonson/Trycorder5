package net.ddns.mlsoftlaberge.trycorder.sensors.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp

/**
 * The original views drew in raw pixels on 2016 xhdpi phones (density 2). Every pixel constant
 * of the port is multiplied by [px] so the panels look the same there and scale elsewhere.
 */
internal const val XHDPI_DENSITY = 2f

/** Paint.setTextSize values of the views, in xhdpi pixels. */
internal const val LABEL_TEXT_PX = 20f
internal const val VALUE_TEXT_PX = 24f

/** Paint stroke width used by most of the views. */
internal const val PAINT_STROKE = 2f

internal val Magenta = Color(0xFFFF00FF)
internal val LightGray = Color(0xFFCCCCCC)
internal val MidGray = Color(0xFFAAAAAA)

/** One original pixel in this draw scope. */
internal val DrawScope.px: Float get() = density / XHDPI_DENSITY

/** The 2 px magenta square every sensor view draws around itself. */
internal fun DrawScope.drawPanelBorder() {
    val w = size.width
    val h = size.height
    val stroke = PAINT_STROKE * px
    drawLine(Magenta, Offset(0f, 0f), Offset(w, 0f), stroke)
    drawLine(Magenta, Offset(0f, 0f), Offset(0f, h), stroke)
    drawLine(Magenta, Offset(w - 1, h - 1), Offset(w - 1, 0f), stroke)
    drawLine(Magenta, Offset(w - 1, h - 1), Offset(0f, h - 1), stroke)
}

/**
 * Canvas.drawText with a STROKE style paint: [x] is the left edge, [baseline] the text baseline,
 * [textPx] the Paint text size in xhdpi pixels.
 */
internal fun DrawScope.drawStrokeText(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    baseline: Float,
    color: Color,
    textPx: Float = VALUE_TEXT_PX,
) {
    val style = TextStyle(
        color = color,
        fontSize = (textPx / XHDPI_DENSITY).sp,
        drawStyle = Stroke(width = PAINT_STROKE * px),
    )
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(x, baseline - layout.firstBaseline))
}
