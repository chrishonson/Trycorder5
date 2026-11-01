package net.ddns.mlsoftlaberge.trycorder.sensors.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import net.ddns.mlsoftlaberge.trycorder.resources.warpcore
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import org.jetbrains.compose.resources.imageResource

private const val BEAM_CYCLE = 100
private const val MOTOR_STEP = 3

/** MotSensorView ticked every 50 ms for impulse and every 10 ms for warp. */
private const val IMPULSE_TICK_MS = 50L
private const val MOTOR_X_OFFSET = 18f
private const val MOTOR_STROKE = 3f
private const val MOTOR_INNER_RADIUS = 16f
private const val MOTOR_OUTER_RADIUS = 32f
private const val MOTOR_START_SIXTHS = 4
private const val SIXTHS = 6
private const val MOTOR_TRAVEL = 180f

/** Beam lines from the bottom center, as fractions of the spread. */
private val BeamSpread = listOf(0f, -1f, 1f, -0.5f, 0.5f, -0.25f, 0.25f)

/**
 * TrbSensorView: a fan of beams from the bottom center, magenta for push (mode 1), blue for
 * pull (mode 2); its spread grows at a speed set by [freq], [force] is the line width.
 */
@Composable
fun TractorPanel(mode: Int, freq: Int, force: Int, rotate: Boolean, running: Boolean, modifier: Modifier = Modifier) {
    var position by remember { mutableIntStateOf(0) }
    var beamFreq by remember { mutableIntStateOf(freq) }
    val rotating by rememberUpdatedState(rotate)
    LaunchedEffect(freq) { beamFreq = freq }
    LaunchedEffect(running, mode) {
        position = 0
        if (!running || mode == 0) return@LaunchedEffect
        position = 1
        while (true) {
            delay(FAST_TICK_MS)
            position += 1 + beamFreq
            if (position >= BEAM_CYCLE) {
                position = 1
                if (rotating) beamFreq = PanelAnimation.nextFreq(beamFreq)
            }
        }
    }
    Canvas(modifier.clipToBounds().background(TrekPalette.Background)) {
        if (position != 0) drawBeams(if (mode == 1) Magenta else Color.Blue, (1 + force) * px, position * px)
    }
}

private fun DrawScope.drawBeams(color: Color, stroke: Float, spread: Float) {
    val x = (size.width.toInt() / 2).toFloat()
    BeamSpread.forEach { f ->
        drawLine(color, Offset(x, size.height), Offset(x + spread * f, 0f), stroke)
    }
}

/**
 * MotSensorView: the warp core picture with two rings going up, magenta for impulse (mode 1),
 * blue for warp (mode 2).
 */
@Composable
fun MotorPanel(mode: Int, running: Boolean, modifier: Modifier = Modifier) {
    var position by remember { mutableIntStateOf(0) }
    LaunchedEffect(running, mode) {
        position = 0
        if (!running || mode == 0) return@LaunchedEffect
        position = 1
        val tick = if (mode == 1) IMPULSE_TICK_MS else FAST_TICK_MS
        while (true) {
            delay(tick)
            position += MOTOR_STEP
            if (position >= BEAM_CYCLE) position = 1
        }
    }
    val core = imageResource(Res.drawable.warpcore)
    Canvas(modifier.clipToBounds().background(TrekPalette.Background)) {
        val u = px
        drawImage(
            image = core,
            dstOffset = IntOffset((MOTOR_X_OFFSET * u).toInt(), 0),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        )
        if (position == 0) return@Canvas
        val color = if (mode == 1) Magenta else Color.Blue
        val y = (size.height.toInt() / SIXTHS * MOTOR_START_SIXTHS) - size.height / MOTOR_TRAVEL * position
        val ring = Offset((size.width.toInt() / 2).toFloat(), y)
        drawCircle(color, MOTOR_INNER_RADIUS * u, ring, style = Stroke(MOTOR_STROKE * u))
        drawCircle(color, MOTOR_OUTER_RADIUS * u, ring, style = Stroke(MOTOR_STROKE * u))
    }
}
