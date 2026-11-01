package net.ddns.mlsoftlaberge.trycorder.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.desktop.WalkieBottomBar
import net.ddns.mlsoftlaberge.trycorder.desktop.WalkieConsole
import net.ddns.mlsoftlaberge.trycorder.desktop.WalkieControls
import net.ddns.mlsoftlaberge.trycorder.desktop.WalkieDeps
import net.ddns.mlsoftlaberge.trycorder.desktop.WalkieIntent
import net.ddns.mlsoftlaberge.trycorder.desktop.WalkieTopBar
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFrameBar
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette

private const val BAR_WEIGHT = 10f
private const val CENTER_WEIGHT = 80f
private const val FRAME_CORNER_WEIGHT = 10f
private const val FRAME_LINE_WEIGHT = 80f
private const val HALF_WEIGHT = 50f

/** The Tryclient walkie-talkie (TryclientActivity/TryclientFragment). Back closes it like finish(). */
@Composable
fun ClientScreen(services: PlatformServices, onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    val store = viewModel { ClientStore(WalkieDeps.from(services), onNavigate, onBack) }
    val state by store.state.collectAsState()
    LifecycleStartEffect(store) {
        store.dispatch(WalkieIntent.Start)
        onStopOrDispose { store.dispatch(WalkieIntent.Stop) }
    }
    ClientContent(
        topStatus = state.topStatus,
        bottomStatus = state.bottomStatus,
        consoleLog = state.consoleLog,
        peersText = state.peersText,
        onIntent = store::dispatch,
    )
}

/** The LCARS layout of tryclient_fragment.xml: walkie controls on top, console log below. */
@Composable
fun ClientContent(
    topStatus: String,
    bottomStatus: String,
    consoleLog: String,
    peersText: String,
    onIntent: (WalkieIntent) -> Unit,
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
            WalkieControls(
                peersText = peersText,
                onSpeak = { onIntent(WalkieIntent.Speak) },
                onTalkDown = { onIntent(WalkieIntent.TalkDown) },
                onTalkUp = { onIntent(WalkieIntent.TalkUp) },
                onScan = { onIntent(WalkieIntent.Scan) },
                onServerOn = { onIntent(WalkieIntent.ServerOn) },
                onServerOff = { onIntent(WalkieIntent.ServerOff) },
                modifier = Modifier.weight(HALF_WEIGHT).fillMaxWidth(),
            )
            TrekFrameBar(top = false, cornerWeight = FRAME_CORNER_WEIGHT, lineWeight = FRAME_LINE_WEIGHT)
            WalkieConsole(consoleLog, Modifier.weight(HALF_WEIGHT).fillMaxWidth())
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
