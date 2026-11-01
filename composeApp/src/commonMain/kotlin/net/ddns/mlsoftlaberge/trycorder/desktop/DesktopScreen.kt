package net.ddns.mlsoftlaberge.trycorder.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.camera.CameraViewer
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.ui.TrekButton
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFrameBar
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import net.ddns.mlsoftlaberge.trycorder.ui.TrekVerticalLine

private const val BAR_WEIGHT = 10f
private const val CENTER_WEIGHT = 80f
private const val FRAME_CORNER_WEIGHT = 10f
private const val FRAME_LINE_WEIGHT = 80f
private const val HALF_WEIGHT = 50f
private const val MODE_COLUMN_WEIGHT = 20f
private const val CONTROLS_WEIGHT = 80f
private const val MODE_GAP_WEIGHT = 6f
private val LineWidth = 4.dp
private val CommEndMargin = 6.dp

/**
 * The Desktop mode (TrydesktopFragment): walkie-talkie console beside the front camera.
 * It is a Trycorder mode, so its Back returns to the Trycorder mode through [onNavigate]
 * (onTrydesktopModeChange(1)) instead of popping a back stack.
 */
@Suppress("UnusedParameter") // signature fixed by App; a mode screen leaves through onNavigate
@Composable
fun DesktopScreen(services: PlatformServices, onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    val store = viewModel { DesktopStore(WalkieDeps.from(services), onNavigate) }
    val state by store.state.collectAsState()
    LifecycleStartEffect(store) {
        store.dispatch(WalkieIntent.Start)
        onStopOrDispose { store.dispatch(WalkieIntent.Stop) }
    }
    DesktopContent(
        topStatus = state.topStatus,
        bottomStatus = state.bottomStatus,
        consoleLog = state.consoleLog,
        peersText = state.peersText,
        onIntent = store::dispatch,
        camera = { modifier -> CameraViewer(services.camera, front = true, modifier = modifier) },
    )
}

/** The LCARS layout of trydesktop_fragment.xml. */
@Composable
fun DesktopContent(
    topStatus: String,
    bottomStatus: String,
    consoleLog: String,
    peersText: String,
    onIntent: (WalkieIntent) -> Unit,
    camera: @Composable (Modifier) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(TrekPalette.Background)) {
        WalkieTopBar(
            status = topStatus,
            onCorner = { onIntent(WalkieIntent.Back) },
            onBack = { onIntent(WalkieIntent.Back) },
            onSettings = { onIntent(WalkieIntent.Settings) },
            modifier = Modifier.weight(BAR_WEIGHT),
        )
        Column(Modifier.weight(CENTER_WEIGHT).fillMaxWidth()) {
            TrekFrameBar(top = true, cornerWeight = FRAME_CORNER_WEIGHT, lineWeight = FRAME_LINE_WEIGHT)
            Row(Modifier.weight(HALF_WEIGHT).fillMaxWidth()) {
                WalkieConsole(consoleLog, Modifier.weight(HALF_WEIGHT).fillMaxHeight())
                Box(Modifier.weight(HALF_WEIGHT).fillMaxHeight()) { camera(Modifier.fillMaxSize()) }
            }
            TrekFrameBar(top = false, cornerWeight = FRAME_CORNER_WEIGHT, lineWeight = FRAME_LINE_WEIGHT)
            Row(Modifier.weight(HALF_WEIGHT).fillMaxWidth()) {
                Column(Modifier.weight(MODE_COLUMN_WEIGHT).fillMaxHeight()) {
                    TrekButton(
                        "COMM", TrekColor.Green, { onIntent(WalkieIntent.CommMode) },
                        Modifier.weight(1f).fillMaxWidth().padding(end = CommEndMargin),
                        edge = TrekEdge.Right,
                    )
                    Spacer(Modifier.weight(MODE_GAP_WEIGHT))
                }
                TrekVerticalLine(Modifier.width(LineWidth))
                WalkieControls(
                    peersText = peersText,
                    onSpeak = { onIntent(WalkieIntent.Speak) },
                    onTalkDown = { onIntent(WalkieIntent.TalkDown) },
                    onTalkUp = { onIntent(WalkieIntent.TalkUp) },
                    onScan = { onIntent(WalkieIntent.Scan) },
                    onServerOn = { onIntent(WalkieIntent.ServerOn) },
                    onServerOff = { onIntent(WalkieIntent.ServerOff) },
                    modifier = Modifier.weight(CONTROLS_WEIGHT).fillMaxHeight(),
                )
            }
        }
        WalkieBottomBar(
            status = bottomStatus,
            onCorner = { onIntent(WalkieIntent.Back) },
            onView = { onIntent(WalkieIntent.View) },
            onSend = { onIntent(WalkieIntent.Send) },
            modifier = Modifier.weight(BAR_WEIGHT),
        )
    }
}
