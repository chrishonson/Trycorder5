package net.ddns.mlsoftlaberge.trycorder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val ThumbRadius = 10.dp
private val TrackWidth = 3.dp
private val ThumbColor = Color(0xFF33B5E5)

/**
 * The SeekBar / VerticalSeekBar of the original, drawn on a trek button background.
 * Vertical sliders have 0 at the bottom. [onStart]/[onStop] mirror onStart/StopTrackingTouch.
 */
@Composable
fun TrekSlider(
    value: Int,
    max: Int,
    color: TrekColor,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
) {
    val change by rememberUpdatedState(onValueChange)
    val start by rememberUpdatedState(onStart)
    val stop by rememberUpdatedState(onStop)
    fun valueAt(offset: Offset, size: IntSize): Int {
        val fraction = if (vertical) 1f - offset.y / size.height else offset.x / size.width
        return (fraction.coerceIn(0f, 1f) * max).roundToInt()
    }
    Box(
        modifier = modifier
            .trekBackground(color, TrekEdge.Round)
            .pointerInput(max, vertical) {
                detectTapGestures(
                    onPress = { offset ->
                        start()
                        change(valueAt(offset, size))
                        tryAwaitRelease()
                        stop()
                    },
                )
            }
            .pointerInput(max, vertical) {
                detectDragGestures(
                    onDragStart = { offset ->
                        start()
                        change(valueAt(offset, size))
                    },
                    onDragEnd = { stop() },
                    onDragCancel = { stop() },
                ) { pointer, _ -> change(valueAt(pointer.position, size)) }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val fraction = if (max == 0) 0f else value.toFloat() / max
            val radius = ThumbRadius.toPx()
            if (vertical) {
                val x = size.width / 2
                val y = radius + (size.height - 2 * radius) * (1f - fraction)
                drawLine(ThumbColor, Offset(x, size.height - radius), Offset(x, y), TrackWidth.toPx())
                drawCircle(ThumbColor, radius, Offset(x, y))
            } else {
                val y = size.height / 2
                val x = radius + (size.width - 2 * radius) * fraction
                drawLine(ThumbColor, Offset(radius, y), Offset(x, y), TrackWidth.toPx())
                drawCircle(ThumbColor, radius, Offset(x, y))
            }
        }
    }
}
