package net.ddns.mlsoftlaberge.trycorder.sensors.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import net.ddns.mlsoftlaberge.trycorder.sensors.Vector3
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette

/** MAXVALUES of the trace views. */
private const val MAX_VALUES = 300

/** SensorManager.MAGNETIC_FIELD_EARTH_MAX, the full scale of both trace views. */
private const val EARTH_MAX = 60f

/** GraSensorView multiplied the accelerometer values by 20 on the same scale. */
private const val GRAVITY_GAIN = 20f
private const val TRACE_STROKE = 3f
private const val LABEL_X = 10f
private const val LABEL_BASELINE = 20f
private const val PCM_FULL_SCALE = 32768

/** Red, blue and green at alpha 192: the x, y and z traces. */
private val TraceColors = listOf(
    Color(red = 255, green = 64, blue = 64, alpha = 192),
    Color(red = 64, green = 64, blue = 255, alpha = 192),
    Color(red = 64, green = 255, blue = 64, alpha = 192),
)

/** MagSensorView: the last 300 magnetic field readings as three scrolling traces. */
@Composable
fun MagneticPanel(field: Vector3, running: Boolean, modifier: Modifier = Modifier) =
    TracePanel("Magnetic", field, running, 1f, modifier)

/** GraSensorView: the last 300 accelerometer readings as three scrolling traces. */
@Composable
fun GravityPanel(acceleration: Vector3, running: Boolean, modifier: Modifier = Modifier) =
    TracePanel("Gravity", acceleration, running, GRAVITY_GAIN, modifier)

@Composable
private fun TracePanel(label: String, value: Vector3, running: Boolean, gain: Float, modifier: Modifier) {
    val history = remember { TraceHistory(MAX_VALUES) }
    var trace by remember { mutableStateOf(emptyList<Vector3>()) }
    LaunchedEffect(value, running) {
        if (running) {
            history.add(value)
            trace = history.toList()
        }
    }
    val measurer = rememberTextMeasurer()
    Canvas(modifier.clipToBounds().background(TrekPalette.Background)) {
        drawTraces(trace, label, gain, measurer)
    }
}

private fun DrawScope.drawTraces(values: List<Vector3>, label: String, gain: Float, measurer: TextMeasurer) {
    val w = size.width
    val yOffset = size.height / 2f
    val scale = -(size.height / 2f / EARTH_MAX) * gain
    val speed = w / MAX_VALUES
    drawLine(MidGray, Offset(0f, yOffset), Offset(w, yOffset), px)
    drawPanelBorder()
    drawStrokeText(measurer, label, LABEL_X * px, LABEL_BASELINE * px, Color.Green, LABEL_TEXT_PX)
    if (values.size < 2) return
    TraceColors.forEachIndexed { axis, color ->
        val points = values.mapIndexed { i, v -> Offset(i * speed, yOffset + v.axis(axis) * scale) }
        drawPoints(points, PointMode.Polygon, color, TRACE_STROKE * px)
    }
}

private fun Vector3.axis(index: Int): Float = when (index) {
    0 -> x
    1 -> y
    else -> z
}

/** AudSensorView: the microphone waveform, one sample per pixel, around a white middle line. */
@Composable
fun AudioPanel(samples: ShortArray, running: Boolean, modifier: Modifier = Modifier) {
    // The original view kept showing its last buffer once the recorder stopped.
    var shown by remember { mutableStateOf(samples) }
    LaunchedEffect(samples, running) {
        if (running) shown = samples
    }
    val measurer = rememberTextMeasurer()
    Canvas(modifier.clipToBounds().background(TrekPalette.Background)) {
        drawWave(shown, measurer)
    }
}

private fun DrawScope.drawWave(samples: ShortArray, measurer: TextMeasurer) {
    val w = size.width
    val maxY = (size.height / 2).toInt()
    drawLine(Color.White, Offset(0f, maxY.toFloat()), Offset(w, maxY.toFloat()), px)
    drawPanelBorder()
    drawStrokeText(measurer, "Audio", LABEL_X * px, LABEL_BASELINE * px, Color.Green, LABEL_TEXT_PX)
    val count = minOf(samples.size, w.toInt())
    if (maxY <= 0 || count < 2) return
    // yscale=(32768/maxy)/2: the noise level is amplified, as in the original.
    val yScale = maxOf(1, PCM_FULL_SCALE / maxY / 2)
    val points = ArrayList<Offset>(count)
    points.add(Offset(0f, maxY.toFloat()))
    for (i in 1 until count) {
        points.add(Offset(i.toFloat(), (maxY + samples[i] / yScale).toFloat()))
    }
    drawPoints(points, PointMode.Polygon, Color.Yellow, PAINT_STROKE * px)
}
