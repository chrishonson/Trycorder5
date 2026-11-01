package net.ddns.mlsoftlaberge.trycorder.trycorder

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import net.ddns.mlsoftlaberge.trycorder.resources.earth_still
import net.ddns.mlsoftlaberge.trycorder.resources.startrek_logo
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.AudioPanel
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.FirePanel
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.GravityPanel
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.MagneticPanel
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.MotorPanel
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.OrientationPanel
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.ShieldsPanel
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.TemperaturePanel
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.TractorPanel
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.TransporterPanel
import net.ddns.mlsoftlaberge.trycorder.ui.TrekButton
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFont
import net.ddns.mlsoftlaberge.trycorder.ui.TrekInput
import net.ddns.mlsoftlaberge.trycorder.ui.TrekLog
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import net.ddns.mlsoftlaberge.trycorder.voice.TrekCommand
import org.jetbrains.compose.resources.painterResource

private val WalkieButtonHeight = 32.dp
private const val CHAT_SMALL_SP = 16
private const val PEERS_SP = 18
private const val CHAT_LOG_WEIGHT = 4f
private const val INPUT_WEIGHT = 8f
private const val SEND_WEIGHT = 2f
private const val TALK_WEIGHT = 2f

/** The sensor area of the main screen: one page at a time. */
@Composable
fun SensorArea(state: TrycorderState, onIntent: (TrycorderIntent) -> Unit, modifier: Modifier = Modifier) {
    val full = modifier.fillMaxSize()
    when (state.sensorPage) {
        SensorPage.EarthStill -> Image(painterResource(Res.drawable.earth_still), null, full)
        SensorPage.StartrekLogo -> Image(painterResource(Res.drawable.startrek_logo), null, full)
        SensorPage.Walkie -> WalkiePanel(state.peersText, onIntent, full)
        SensorPage.Chat -> ChatPanel(state.chatInput, state.chatLog, onIntent, full)
        SensorPage.Magnetic, SensorPage.Orientation, SensorPage.Gravity,
        SensorPage.Temperature, SensorPage.AudioWave,
        -> ScanPanel(state, onIntent, full)
        else -> AnimationPanel(state, full)
    }
}

@Composable
private fun ScanPanel(state: TrycorderState, onIntent: (TrycorderIntent) -> Unit, modifier: Modifier) {
    val r = state.readings
    val running = state.activeSensor == state.sensorPage
    when (state.sensorPage) {
        SensorPage.Magnetic -> MagneticPanel(r.magnetic, running, modifier)
        SensorPage.Orientation ->
            OrientationPanel(r.orientation, r.location, running, { onIntent(TrycorderIntent.OpenMap) }, modifier)
        SensorPage.Gravity -> GravityPanel(r.acceleration, running, modifier)
        SensorPage.Temperature -> TemperaturePanel(r.environment, running, modifier)
        else -> AudioPanel(r.audio, running, modifier)
    }
}

@Composable
private fun AnimationPanel(state: TrycorderState, modifier: Modifier) {
    val running = state.activeSensor == state.sensorPage
    when (state.sensorPage) {
        SensorPage.Shields -> with(state.shield) {
            ShieldsPanel(state.shieldMode, freq, force, rotate, running, modifier)
        }
        SensorPage.Fire -> FirePanel(state.fireMode, state.fireDirection, state.fireForce, state.fireShot, modifier)
        SensorPage.Transporter -> TransporterPanel(state.transporterMode, state.transporterPosition, running, modifier)
        SensorPage.Tractor -> with(state.tractor) {
            TractorPanel(state.tractorMode, freq, force, rotate, running, modifier)
        }
        else -> MotorPanel(state.motorMode, running, modifier)
    }
}

/** Intercom panel: speak, push to talk, server on/off, peer list. */
@Composable
fun WalkiePanel(peers: String, onIntent: (TrycorderIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            WalkieButton("SPEAK", TrekColor.Indigo, 1f) { onIntent(TrycorderIntent.Talk) }
            PushToTalk(Modifier.weight(TALK_WEIGHT).height(WalkieButtonHeight), onIntent)
            WalkieButton("CMD", TrekColor.Green, 1f) { onIntent(TrycorderIntent.CommandWindow) }
        }
        Row(Modifier.fillMaxWidth()) {
            WalkieButton("LIST", TrekColor.Indigo, 1f) {
                onIntent(TrycorderIntent.Command(TrekCommand.SpeakList))
            }
            WalkieButton("Srv-On", TrekColor.Orange, 1f) { onIntent(TrycorderIntent.ServerOn) }
            WalkieButton("Srv-Off", TrekColor.Orange, 1f) { onIntent(TrycorderIntent.ServerOff) }
            WalkieButton("LOGS", TrekColor.Green, 1f) {
                onIntent(TrycorderIntent.Command(TrekCommand.LogsConsole))
            }
        }
        TrekLog(
            peers, TrekPalette.HoloGreenLight, Modifier.fillMaxWidth(),
            font = TrekFont.Action, bold = false, fontSize = PEERS_SP,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.WalkieButton(
    text: String,
    color: TrekColor,
    weight: Float,
    onClick: () -> Unit,
) {
    TrekButton(text, color, onClick, Modifier.weight(weight).height(WalkieButtonHeight))
}

/** Push To Talk streams audio while pressed (ACTION_DOWN / ACTION_UP). */
@Composable
private fun PushToTalk(modifier: Modifier, onIntent: (TrycorderIntent) -> Unit) {
    TrekButton(
        text = "Push To Talk",
        color = TrekColor.Red,
        onClick = {},
        maxLines = 2,
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                onIntent(TrycorderIntent.PushToTalkDown)
                waitForUpOrCancellation()
                onIntent(TrycorderIntent.PushToTalkUp)
            }
        },
    )
}

/** Chat panel: canned server requests, message field, chat log. */
@Composable
fun ChatPanel(input: String, log: String, onIntent: (TrycorderIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().weight(1f)) {
            ChatButton("Demo", TrekColor.Green, CHAT_SMALL_SP) { onIntent(TrycorderIntent.ChatRequest("demo")) }
            ChatButton("Logs", TrekColor.Green, CHAT_SMALL_SP) { onIntent(TrycorderIntent.ChatRequest("logs")) }
            ChatButton("City", TrekColor.Green) { onIntent(TrycorderIntent.ChatRequest("citys")) }
            ChatButton("Ver.", TrekColor.Green) { onIntent(TrycorderIntent.ChatRequest("tryversions")) }
            ChatButton("List", TrekColor.Indigo) { onIntent(TrycorderIntent.ChatRequest("listcmds")) }
        }
        Row(Modifier.fillMaxWidth().weight(1f)) {
            TrekInput(input, { onIntent(TrycorderIntent.ChatInput(it)) }, Modifier.weight(INPUT_WEIGHT).fillMaxSize())
            TrekButton(
                "Send", TrekColor.Green, { onIntent(TrycorderIntent.ChatSend) },
                Modifier.weight(SEND_WEIGHT).fillMaxSize(),
            )
        }
        TrekLog(
            text = log.ifEmpty { "Chat messages" },
            color = TrekPalette.HoloGreenLight,
            modifier = Modifier.fillMaxWidth().weight(CHAT_LOG_WEIGHT),
            bold = false,
            wrap = true,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ChatButton(
    text: String,
    color: TrekColor,
    size: Int = PEERS_SP,
    onClick: () -> Unit,
) {
    TrekButton(
        text, color, onClick, Modifier.weight(1f).fillMaxSize(),
        font = TrekFont.Default, bold = false, fontSize = size.sp,
    )
}
