package net.ddns.mlsoftlaberge.trycorder.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import net.ddns.mlsoftlaberge.trycorder.resources.trek_bottomleft
import net.ddns.mlsoftlaberge.trycorder.resources.trek_topleft
import net.ddns.mlsoftlaberge.trycorder.ui.TrekButton
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFont
import net.ddns.mlsoftlaberge.trycorder.ui.TrekLog
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import net.ddns.mlsoftlaberge.trycorder.ui.family
import net.ddns.mlsoftlaberge.trycorder.ui.trekBackground
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val ButtonHeight = 32.dp
private val ButtonMargin = 2.dp
private const val TEXT_SP = 18
private const val CORNER_WEIGHT = 4f
private const val BAR_WEIGHT = 6f
private const val TALK_WEIGHT = 2f

/** Top bar of trydesktop/tryclient_fragment.xml: LCARS corner, Back and Settings, top status. */
@Composable
fun WalkieTopBar(
    status: String,
    onCorner: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().background(TrekPalette.Background)) {
        CornerImage(Res.drawable.trek_topleft, onCorner, Modifier.weight(CORNER_WEIGHT).fillMaxHeight())
        Column(Modifier.weight(BAR_WEIGHT).fillMaxHeight()) {
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TrekButton(
                    "Back", TrekColor.Indigo, onBack, Modifier.weight(1f).height(ButtonHeight),
                    edge = TrekEdge.Center, bold = false,
                )
                TrekButton(
                    "Settings", TrekColor.Gray, onSettings, Modifier.weight(1f).height(ButtonHeight),
                    edge = TrekEdge.Right,
                )
            }
            StatusText(status, TrekFont.Status, Modifier.weight(1f))
        }
    }
}

/** Bottom bar: LCARS corner, bottom status, View and Send. */
@Composable
fun WalkieBottomBar(
    status: String,
    onCorner: () -> Unit,
    onView: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().background(TrekPalette.Background)) {
        CornerImage(Res.drawable.trek_bottomleft, onCorner, Modifier.weight(CORNER_WEIGHT).fillMaxHeight())
        Column(Modifier.weight(BAR_WEIGHT).fillMaxHeight()) {
            StatusText(status, TrekFont.Action, Modifier.weight(1f))
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                val button = Modifier.weight(1f).height(ButtonHeight)
                TrekButton("View", TrekColor.Red, onView, button, edge = TrekEdge.Center)
                TrekButton("Send", TrekColor.Blue, onSend, button, edge = TrekEdge.Right)
            }
        }
    }
}

/** The walkie controls: SPEAK, Push To Talk, SCAN, Srv-On, Srv-Off and the list of local Trycorders. */
@Suppress("LongParameterList") // one lambda per button of the original layout
@Composable
fun WalkieControls(
    peersText: String,
    onSpeak: () -> Unit,
    onTalkDown: () -> Unit,
    onTalkUp: () -> Unit,
    onScan: () -> Unit,
    onServerOn: () -> Unit,
    onServerOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            TrekButton("SPEAK", TrekColor.Indigo, onSpeak, Modifier.weight(1f).height(ButtonHeight))
            PushToTalkButton(onTalkDown, onTalkUp, Modifier.weight(TALK_WEIGHT).height(ButtonHeight))
            TrekButton("SCAN", TrekColor.Green, onScan, Modifier.weight(1f).height(ButtonHeight))
        }
        Row(Modifier.fillMaxWidth()) {
            TrekButton("Srv-On", TrekColor.Orange, onServerOn, Modifier.weight(1f).height(ButtonHeight))
            TrekButton("Srv-Off", TrekColor.Orange, onServerOff, Modifier.weight(1f).height(ButtonHeight))
        }
        TrekLog(
            text = peersText,
            color = TrekPalette.HoloGreenLight,
            font = TrekFont.Action,
            bold = false,
            fontSize = TEXT_SP,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

/** The console log (logs_console): finalold, bold, holo green. */
@Composable
fun WalkieConsole(text: String, modifier: Modifier = Modifier) {
    TrekLog(
        text = text,
        color = TrekPalette.HoloGreenLight,
        font = TrekFont.Button,
        fontSize = TEXT_SP,
        modifier = modifier,
    )
}

/** The red "Push To Talk" button: streams while it is held (ACTION_DOWN / ACTION_UP). */
@Composable
fun PushToTalkButton(onDown: () -> Unit, onUp: () -> Unit, modifier: Modifier = Modifier) {
    var pressed by remember { mutableStateOf(false) }
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    Box(
        modifier = modifier
            .padding(ButtonMargin)
            .trekBackground(TrekColor.Red, TrekEdge.Round, pressed)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        down()
                        tryAwaitRelease()
                        pressed = false
                        up()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Push To Talk",
            color = TrekPalette.ButtonText,
            fontFamily = TrekFont.Button.family(),
            fontWeight = FontWeight.Bold,
            fontSize = TEXT_SP.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun CornerImage(res: DrawableResource, onClick: () -> Unit, modifier: Modifier) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier.padding(horizontal = ButtonMargin).clickable(onClick = onClick),
    )
}

@Composable
private fun StatusText(text: String, font: TrekFont, modifier: Modifier) {
    Text(
        text = text,
        color = TrekPalette.HoloGreenLight,
        fontFamily = font.family(),
        fontWeight = FontWeight.Bold,
        fontSize = TEXT_SP.sp,
        maxLines = 1,
        modifier = modifier.fillMaxSize(),
    )
}
