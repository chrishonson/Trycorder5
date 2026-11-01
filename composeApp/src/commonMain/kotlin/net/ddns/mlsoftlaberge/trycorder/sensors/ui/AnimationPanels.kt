package net.ddns.mlsoftlaberge.trycorder.sensors.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette

/** Timer period of the Shi, Fir, Tra and Trb views. */
internal const val FAST_TICK_MS = 10L

private const val SHIELD_STEP = 3
private const val ANIMATION_END = 250
private const val SHIELD_ROTATE_RESTART = 200
private const val GRID_BASE = 10
private const val TORPEDO_STEP = 10f
private const val PHASER_STEP = 5f
private const val DIRECTION_PX = 4f
private const val TARGET_RADIUS = 16f
private const val TARGET_STROKE = 3f
private const val PHASER_MAX_WIDTH = 32f
private const val PERCENT = 100f
private const val PERSON_PARTS = 5
private const val THIRDS = 3

/** ShiSensorView: a grid that fades in (raise) or out (lower); [force] is the line width. */
@Composable
fun ShieldsPanel(mode: Int, freq: Int, force: Int, rotate: Boolean, running: Boolean, modifier: Modifier = Modifier) {
    val raising = mode != 2
    var position by remember { mutableIntStateOf(0) }
    var gridFreq by remember { mutableIntStateOf(freq) }
    val rotating by rememberUpdatedState(rotate)
    LaunchedEffect(freq) { gridFreq = freq }
    LaunchedEffect(running, mode) {
        if (!running) return@LaunchedEffect
        position = 1
        while (position != 0) {
            delay(FAST_TICK_MS)
            position += SHIELD_STEP
            if (position >= ANIMATION_END) {
                if (rotating && raising) {
                    position = SHIELD_ROTATE_RESTART
                    gridFreq = PanelAnimation.nextFreq(gridFreq)
                } else {
                    position = 0
                }
            }
        }
    }
    Canvas(modifier.clipToBounds().background(TrekPalette.Background)) {
        val alpha = PanelAnimation.fadeAlpha(position, raising) / PanelAnimation.MAX_ALPHA.toFloat()
        val color = LightGray.copy(alpha = alpha)
        val stroke = (1 + force) * px
        val grid = GRID_BASE + gridFreq
        val stepY = (size.height.toInt() / grid).toFloat()
        val stepX = (size.width.toInt() / grid).toFloat()
        for (i in 0..grid) {
            drawLine(color, Offset(0f, stepY * i), Offset(size.width, stepY * i), stroke)
            drawLine(color, Offset(stepX * i, 0f), Offset(stepX * i, size.height), stroke)
        }
    }
}

/**
 * FirSensorView: a target at the top, a torpedo (mode 1) or two phaser beams (mode 2) going up
 * toward [direction]. The shot restarts each time [shot] changes.
 */
@Composable
fun FirePanel(mode: Int, direction: Int, force: Int, shot: Int, modifier: Modifier = Modifier) {
    var position by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableIntStateOf(0) }
    val unit by rememberUpdatedState(LocalDensity.current.density / XHDPI_DENSITY)
    val torpedo by rememberUpdatedState(mode == 1)
    LaunchedEffect(shot) {
        if (shot == 0) return@LaunchedEffect
        position = 0f
        do {
            delay(FAST_TICK_MS)
            position += (if (torpedo) TORPEDO_STEP else PHASER_STEP) * unit
        } while (position <= height)
        position = 0f
    }
    Canvas(modifier.onSizeChanged { height = it.height }.clipToBounds().background(TrekPalette.Background)) {
        drawFire(mode, direction, force, position)
    }
}

private fun DrawScope.drawFire(mode: Int, direction: Int, force: Int, position: Float) {
    val w = size.width
    val h = size.height
    val u = px
    val gridStroke = PAINT_STROKE * u
    val left = (w.toInt() / THIRDS).toFloat()
    val right = w / THIRDS * 2f
    val top = (h.toInt() / THIRDS).toFloat()
    val bottom = h / THIRDS * 2f
    drawLine(Color.White, Offset(left, 0f), Offset(left, h), gridStroke)
    drawLine(Color.White, Offset(right, 0f), Offset(right, h), gridStroke)
    drawLine(Color.White, Offset(0f, top), Offset(w, top), gridStroke)
    drawLine(Color.White, Offset(0f, bottom), Offset(w, bottom), gridStroke)
    val aim = direction * DIRECTION_PX * u
    // FILL_AND_STROKE paints grow by half their stroke width.
    drawCircle(Color.Blue, (TARGET_RADIUS + TARGET_STROKE / 2) * u, Offset(w / 2f + aim, TARGET_RADIUS * u))
    if (position == 0f || h <= 0f) return
    val target = Offset(w / 2f + aim * (position / h), h - position)
    when (mode) {
        1 -> drawCircle(Color.Red, (force + 1) * u, target)
        2 -> {
            val width = PHASER_MAX_WIDTH * (force / PERCENT) * u
            drawLine(Color.Red, Offset(w / THIRDS, h), target, width)
            drawLine(Color.Red, Offset(right, h), target, width)
        }
    }
}

/**
 * TraSensorView: a stick person fading in (mode 1, beam in) or out (mode 2, beam out) with a
 * blue transport circle. [position] 0..255 comes from the transporter sliders.
 */
@Composable
fun TransporterPanel(mode: Int, position: Int, running: Boolean, modifier: Modifier = Modifier) {
    val beamIn = mode != 2
    var current by remember { mutableIntStateOf(position) }
    LaunchedEffect(position) { current = position }
    LaunchedEffect(running, mode) {
        if (!running) return@LaunchedEffect
        current = 1
        while (current != 0) {
            delay(FAST_TICK_MS)
            current++
            if (current >= ANIMATION_END) current = 0
        }
    }
    Canvas(modifier.clipToBounds().background(TrekPalette.Background)) {
        drawPerson(PanelAnimation.fadeAlpha(current, beamIn) / PanelAnimation.MAX_ALPHA.toFloat())
        if (current != 0) {
            val radius = (if (beamIn) current else ANIMATION_END - current) * px
            drawCircle(Color.Blue, radius, center, style = Stroke(PAINT_STROKE * px))
        }
    }
}

private fun DrawScope.drawPerson(alpha: Float) {
    val color = LightGray.copy(alpha = alpha)
    val stroke = PAINT_STROKE * px
    val x = (size.width.toInt() / 2).toFloat()
    val y = (size.height.toInt() / 2).toFloat()
    val len = size.height.toInt() / PERSON_PARTS
    val half = (len / 2).toFloat()
    drawLine(color, Offset(x, y - len), Offset(x, y + len), stroke)
    drawLine(color, Offset(x - half, y), Offset(x + half, y), stroke)
    drawLine(color, Offset(x, y + len), Offset(x - half, y + len * 2), stroke)
    drawLine(color, Offset(x, y + len), Offset(x + half, y + len * 2), stroke)
    drawCircle(color, half, Offset(x, y - len - half), style = Stroke(stroke))
}
