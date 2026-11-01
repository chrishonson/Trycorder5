package net.ddns.mlsoftlaberge.trycorder.multisensor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.camera.CameraController
import net.ddns.mlsoftlaberge.trycorder.camera.CameraViewer
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.AudioPanel
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.GravityPanel
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.MagneticPanel
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.OrientationPanel
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.TemperaturePanel
import net.ddns.mlsoftlaberge.trycorder.trycorder.BarButton
import net.ddns.mlsoftlaberge.trycorder.trycorder.TrekBar
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette

/** layout_weight values of trysensor_fragment.xml. */
private const val BAR_WEIGHT = 10f
private const val CENTER_WEIGHT = 80f
private const val ROW_WEIGHT = 33f
private const val ROWS_REST = 1f
private const val NARROW_WEIGHT = 30f
private const val WIDE_WEIGHT = 70f
private const val HALF_WEIGHT = 50f

/**
 * The MultiSensor mode. Its Back button and LCARS corners switch to the Trycorder mode like the
 * original onTrysensorModeChange(1), which is also what the system back does from this mode.
 */
@Suppress("UnusedParameter") // App's contract; Back here is mode 1 through onNavigate, like the original
@Composable
fun MultiSensorScreen(services: PlatformServices, onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    val store = viewModel { MultiSensorStore(MultiSensorDeps.from(services), onNavigate) }
    val state by store.state.collectAsState()
    LifecycleResumeEffect(store) {
        store.dispatch(MultiSensorIntent.Resume)
        onPauseOrDispose { store.dispatch(MultiSensorIntent.Pause) }
    }
    MultiSensorLayout(state, services.camera, store::dispatch)
}

/**
 * trysensor_fragment.xml: top bar, three rows of sensors (magnetic | orientation,
 * gravity | temperature, audio | front camera), bottom bar.
 */
@Composable
fun MultiSensorLayout(
    state: MultiSensorState,
    camera: CameraController,
    onIntent: (MultiSensorIntent) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(TrekPalette.Background)) {
        TrekBar(
            top = true,
            status = state.topStatus,
            buttons = listOf(
                BarButton("Back", TrekColor.Indigo, TrekEdge.Center) { onIntent(MultiSensorIntent.Back) },
                BarButton("Settings", TrekColor.Gray, TrekEdge.Right) { onIntent(MultiSensorIntent.Settings) },
            ),
            onCorner = { onIntent(MultiSensorIntent.Back) },
            modifier = Modifier.weight(BAR_WEIGHT),
        )
        Column(Modifier.weight(CENTER_WEIGHT).fillMaxWidth()) {
            val running = state.running
            Row(Modifier.weight(ROW_WEIGHT).fillMaxWidth()) {
                MagneticPanel(state.magnetic, running, Modifier.weight(NARROW_WEIGHT).fillMaxHeight())
                OrientationPanel(
                    state.orientation, state.location, running, { onIntent(MultiSensorIntent.OpenMap) },
                    Modifier.weight(WIDE_WEIGHT).fillMaxHeight(),
                )
            }
            Row(Modifier.weight(ROW_WEIGHT).fillMaxWidth()) {
                GravityPanel(state.acceleration, running, Modifier.weight(NARROW_WEIGHT).fillMaxHeight())
                TemperaturePanel(state.environment, running, Modifier.weight(WIDE_WEIGHT).fillMaxHeight())
            }
            Row(Modifier.weight(ROW_WEIGHT).fillMaxWidth()) {
                AudioPanel(state.audio.samples, running, Modifier.weight(HALF_WEIGHT).fillMaxHeight())
                CameraViewer(camera, front = true, modifier = Modifier.weight(HALF_WEIGHT).fillMaxHeight())
            }
            Spacer(Modifier.weight(ROWS_REST))
        }
        TrekBar(
            top = false,
            status = state.bottomStatus,
            buttons = listOf(
                BarButton("Speak", TrekColor.Indigo, TrekEdge.Center) { onIntent(MultiSensorIntent.Speak) },
                BarButton("Send", TrekColor.Blue, TrekEdge.Right) { onIntent(MultiSensorIntent.Send) },
            ),
            onCorner = { onIntent(MultiSensorIntent.Back) },
            modifier = Modifier.weight(BAR_WEIGHT),
        )
    }
}
