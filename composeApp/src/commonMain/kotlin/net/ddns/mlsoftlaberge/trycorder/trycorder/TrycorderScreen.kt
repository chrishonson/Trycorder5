package net.ddns.mlsoftlaberge.trycorder.trycorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import net.ddns.mlsoftlaberge.trycorder.camera.CameraController
import net.ddns.mlsoftlaberge.trycorder.ui.TrekBox
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import net.ddns.mlsoftlaberge.trycorder.ui.TrekVerticalLine

private const val BAR_WEIGHT = 10f
private const val MIDDLE_WEIGHT = 80f
private const val SIDE_WEIGHT = 2f
private const val CENTER_WEIGHT = 8f
private const val SENSOR_BOX_WEIGHT = 40f
private const val GROUP_WEIGHT = 10f
private const val VIEWER_BOX_WEIGHT = 50f
private const val SENSOR_CORNER_WEIGHT = 10f
private const val SENSOR_LINE_WEIGHT = 80f
private const val VLINE_WEIGHT = 3f
private const val SENSOR_WEIGHT = 94f
private const val GAP = 0.15f
private const val WIDE_GAP = 0.25f
private const val LAST_GAP = 0.10f

private val SIDE_BUTTONS = listOf(
    SideButton("SENSORS", TrekColor.Yellow, ButtonGroup.Sensor, GAP),
    SideButton("COMM", TrekColor.Green, ButtonGroup.Comm, GAP),
    SideButton("SHIELD", TrekColor.Orange, ButtonGroup.Shield, GAP),
    SideButton("FIRE", TrekColor.Red, ButtonGroup.Fire, GAP),
    SideButton("TRANSP", TrekColor.Pink, ButtonGroup.Transporter, GAP),
    SideButton("TRACTOR", TrekColor.Indigo, ButtonGroup.Tractor, GAP),
    SideButton("MOTOR", TrekColor.Vert, ButtonGroup.Motor, WIDE_GAP),
    SideButton("VIEWER", TrekColor.Mauve, ButtonGroup.Viewer, WIDE_GAP),
    SideButton("LOGS", TrekColor.Blue, ButtonGroup.Logs, GAP),
    SideButton("MODE", TrekColor.Gray, ButtonGroup.Mode, WIDE_GAP),
)

/** Container of the main Trycorder screen: holds the store and forwards lifecycle events. */
@Composable
fun TrycorderScreen(store: TrycorderStore, camera: CameraController) {
    val state by store.state.collectAsState()
    LifecycleStartEffect(store) {
        store.dispatch(TrycorderIntent.Start)
        onStopOrDispose { store.dispatch(TrycorderIntent.Stop) }
    }
    LifecycleResumeEffect(store) {
        store.dispatch(TrycorderIntent.Resume)
        onPauseOrDispose { store.dispatch(TrycorderIntent.Pause) }
    }
    TrycorderLayout(state, camera, store::dispatch)
}

/** The LCARS layout of trycorder_fragment.xml. */
@Composable
fun TrycorderLayout(state: TrycorderState, camera: CameraController, onIntent: (TrycorderIntent) -> Unit) {
    Column(Modifier.fillMaxSize().background(TrekPalette.Background)) {
        TrekBar(
            top = true,
            status = state.topStatus,
            buttons = listOf(
                BarButton("Sound", if (state.musicPlaying) TrekColor.Yellow else TrekColor.Gray, TrekEdge.Center) {
                    onIntent(TrycorderIntent.ToggleSound)
                },
                BarButton("Start", TrekColor.Green, TrekEdge.Center) { onIntent(TrycorderIntent.StartSensors) },
                BarButton("Stop", TrekColor.Red, TrekEdge.Center) { onIntent(TrycorderIntent.StopSensors) },
                BarButton("Setup", TrekColor.Blue, TrekEdge.Right) { onIntent(TrycorderIntent.OpenSettings) },
            ),
            onCorner = { onIntent(TrycorderIntent.Talk) },
            modifier = Modifier.weight(BAR_WEIGHT),
        )
        Row(Modifier.weight(MIDDLE_WEIGHT).fillMaxWidth()) {
            SideColumn(
                buttons = SIDE_BUTTONS,
                lastGap = LAST_GAP,
                onSelect = { onIntent(TrycorderIntent.SelectGroup(it)) },
                modifier = Modifier.weight(SIDE_WEIGHT),
            )
            Column(Modifier.weight(CENTER_WEIGHT).fillMaxHeight()) {
                TrekBox(Modifier.weight(SENSOR_BOX_WEIGHT), SENSOR_CORNER_WEIGHT, SENSOR_LINE_WEIGHT) {
                    Row(Modifier.fillMaxSize()) {
                        TrekVerticalLine(Modifier.weight(VLINE_WEIGHT))
                        SensorArea(state, onIntent, Modifier.weight(SENSOR_WEIGHT))
                        TrekVerticalLine(Modifier.weight(VLINE_WEIGHT))
                    }
                }
                Row(Modifier.weight(GROUP_WEIGHT).fillMaxWidth()) {
                    GROUP_ROWS[state.buttonGroup]?.let { (style, buttons) -> GroupRow(buttons, style, onIntent) }
                }
                TrekBox(Modifier.weight(VIEWER_BOX_WEIGHT)) {
                    ViewerArea(state, camera, onIntent)
                }
            }
        }
        TrekBar(
            top = false,
            status = state.bottomStatus,
            buttons = listOf(
                BarButton("Snap", TrekColor.Gray, TrekEdge.Center) { onIntent(TrycorderIntent.Snap) },
                BarButton("Photo", TrekColor.Green, TrekEdge.Center) { onIntent(TrycorderIntent.TakePhoto) },
                BarButton("Record", TrekColor.Orange, TrekEdge.Center) { onIntent(TrycorderIntent.RecordVideo) },
                BarButton("Gallery", TrekColor.Blue, TrekEdge.Right) { onIntent(TrycorderIntent.OpenGallery) },
            ),
            onCorner = { onIntent(TrycorderIntent.Snap) },
            modifier = Modifier.weight(BAR_WEIGHT),
        )
    }
}
