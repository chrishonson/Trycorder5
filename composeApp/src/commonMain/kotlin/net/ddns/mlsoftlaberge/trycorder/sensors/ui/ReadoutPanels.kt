package net.ddns.mlsoftlaberge.trycorder.sensors.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import net.ddns.mlsoftlaberge.trycorder.core.MemoryStats
import net.ddns.mlsoftlaberge.trycorder.sensors.Environment
import net.ddns.mlsoftlaberge.trycorder.sensors.GeoLocation
import net.ddns.mlsoftlaberge.trycorder.sensors.Vector3
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val COMPASS_RADIUS = 0.9f

/** OriSensorView used 3.1416 for pi. */
private const val ORIGINAL_PI = 3.1416
private const val HALF_TURN = 180.0
private const val TEXT_LINE = 32f
private const val MEMORY_LINES = 10
private const val MEMORY_TEXT_LIFT = 8f

/** OriSensorView: compass needle on the azimuth, latitude and longitude. Tap to open the map. */
@Composable
fun OrientationPanel(
    orientation: Vector3,
    location: GeoLocation?,
    running: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The original view kept its last azimuth once the sensor was unregistered.
    var azimuth by remember { mutableStateOf(orientation.x) }
    LaunchedEffect(orientation, running) {
        if (running) azimuth = orientation.x
    }
    val measurer = rememberTextMeasurer()
    Canvas(modifier.clickable(onClick = onClick).clipToBounds().background(TrekPalette.Background)) {
        drawCompass(azimuth, location, measurer)
    }
}

private fun DrawScope.drawCompass(azimuth: Float, location: GeoLocation?, measurer: TextMeasurer) {
    drawPanelBorder()
    val center = min((size.width / 2).toInt(), (size.height / 2).toInt()).toFloat()
    val radius = center * COMPASS_RADIUS
    val stroke = Stroke(PAINT_STROKE * px)
    drawCircle(Color.White, radius, Offset(center, center), style = stroke)
    val angle = -azimuth / HALF_TURN * ORIGINAL_PI
    val tip = Offset(center + radius * sin(angle).toFloat(), center - radius * cos(angle).toFloat())
    drawLine(Color.White, Offset(center, center), tip, PAINT_STROKE * px)
    val textX = center * 2f
    drawStrokeText(measurer, "ORI: $azimuth", textX, center * 2f - TEXT_LINE * px, Color.White)
    val lat = location?.latitude?.toFloat()?.toString() ?: "Not avalaible"
    val lon = location?.longitude?.toFloat()?.toString() ?: "Not avalaible"
    drawStrokeText(measurer, "LAT: $lat", textX, TEXT_LINE * px, Color.White)
    drawStrokeText(measurer, "LON: $lon", textX, 2 * TEXT_LINE * px, Color.White)
}

/** One gauge of TemSensorView: label, range and the value as a red bar. */
private data class Gauge(val label: String, val value: Float, val min: Float, val max: Float)

private const val PRESSURE_MAX = 1200f
private const val TEMPERATURE_MIN = -20f
private const val TEMPERATURE_MAX = 100f
private const val LIGHT_MAX = 200f

/** TemSensorView: pressure, ambient temperature and light gauges side by side. */
@Composable
fun TemperaturePanel(environment: Environment, running: Boolean, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(environment) }
    LaunchedEffect(environment, running) {
        if (running) shown = environment
    }
    val measurer = rememberTextMeasurer()
    Canvas(modifier.clipToBounds().background(TrekPalette.Background)) {
        drawPanelBorder()
        val third = size.width / GAUGES
        val gauges = listOf(
            // The original label said Kpa but showed the hPa value of TYPE_PRESSURE.
            Gauge("Press(Kpa)", shown.pressure ?: 0f, 0f, PRESSURE_MAX),
            Gauge("ATemp(°C)", shown.temperature ?: 0f, TEMPERATURE_MIN, TEMPERATURE_MAX),
            Gauge("Light(Lux)", shown.light ?: 0f, 0f, LIGHT_MAX),
        )
        gauges.forEachIndexed { i, gauge ->
            drawGauge(gauge, third * i, third, measurer)
            if (i > 0) drawLine(Color.White, Offset(third * i, 0f), Offset(third * i, size.height), PAINT_STROKE * px)
        }
    }
}

/** Pixel offsets of TemSensorView.drawSensor. */
private const val GAUGE_MARGIN = 32f
private const val LABEL_BASELINE = 24f
private const val MAX_BASELINE = 56f
private const val SCALE_X = 4f
private const val GAUGES = 3f

/** drawSensor(label, value, min, max, px, 0, nx, height) of TemSensorView. */
private fun DrawScope.drawGauge(gauge: Gauge, left: Float, width: Float, measurer: TextMeasurer) {
    val u = px
    val margin = GAUGE_MARGIN * u
    val bottom = size.height - margin
    val span = bottom - margin
    val range = gauge.max - gauge.min
    val lineLen = span * ((gauge.value - gauge.min) / range)
    val barTop = minOf(bottom - lineLen, bottom)
    val barSize = Size(width / GAUGES, maxOf(bottom - lineLen, bottom) - barTop)
    val barLeft = left + width / GAUGES
    drawRect(Color.Red, Offset(barLeft, barTop), barSize)
    drawRect(Color.Red, Offset(barLeft, barTop), barSize, style = Stroke(PAINT_STROKE * u))
    drawStrokeText(measurer, gauge.label, left + margin, LABEL_BASELINE * u, Color.White)
    drawStrokeText(measurer, FixedFormat.format(gauge.value, 2), barLeft, size.height - SCALE_X * u, Color.White)
    val mid = left + width / 2f
    // center line showing the range, then the top, bottom and zero scale lines
    drawLine(Color.White, Offset(mid, margin), Offset(mid, bottom), PAINT_STROKE * u)
    drawScaleLine(mid, margin)
    drawScaleLine(mid, bottom)
    val zeroLen = span * ((0f - gauge.min) / range)
    if (zeroLen > 0f) {
        drawScaleLine(mid, bottom - zeroLen)
        drawStrokeText(measurer, "0.0", left + SCALE_X * u, bottom - zeroLen, Color.White)
    }
    drawStrokeText(measurer, FixedFormat.format(gauge.max, 0), left + SCALE_X * u, MAX_BASELINE * u, Color.White)
    drawStrokeText(measurer, FixedFormat.format(gauge.min, 0), left + SCALE_X * u, bottom, Color.White)
}

private fun DrawScope.drawScaleLine(mid: Float, y: Float) {
    val half = GAUGE_MARGIN * px
    drawLine(Color.White, Offset(mid - half, y), Offset(mid + half, y), PAINT_STROKE * px)
}

/** LogsStatView: total and free memory over a ten line grid. */
@Composable
fun MemoryStatPanel(memory: MemoryStats, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier.clipToBounds().background(TrekPalette.Background)) {
        val h = size.height.toInt()
        val line = (h / MEMORY_LINES).toFloat()
        for (i in 0 until MEMORY_LINES) {
            drawLine(LightGray, Offset(0f, line * i), Offset(size.width, line * i), PAINT_STROKE * px)
        }
        drawStrokeText(measurer, "Total Mem: ${memory.total}", 0f, line - MEMORY_TEXT_LIFT * px, Color.White)
        drawStrokeText(measurer, "Free Mem: ${memory.free}", 0f, line * 2 - MEMORY_TEXT_LIFT * px, Color.White)
    }
}
