package net.ddns.mlsoftlaberge.trycorder.trycorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.ui.TrekButton
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFont
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import net.ddns.mlsoftlaberge.trycorder.voice.TrekCommand

private val GroupButtonHeight = 40.dp

/** A button of a group row. */
data class GroupButton(
    val text: String,
    val color: TrekColor,
    val edge: TrekEdge,
    val weight: Float,
    val intent: TrycorderIntent,
)

/** Visual style of a group row: action groups use the finalnew font, not bold, on two lines. */
data class GroupStyle(val font: TrekFont, val bold: Boolean, val maxLines: Int)

private val ScanStyle = GroupStyle(TrekFont.Button, bold = false, maxLines = 2)
private val ActionStyle = GroupStyle(TrekFont.Action, bold = false, maxLines = 2)

private fun cmd(text: String, color: TrekColor, edge: TrekEdge, weight: Float, command: TrekCommand) =
    GroupButton(text, color, edge, weight, TrycorderIntent.Command(command))

private const val W5 = 20f
private const val W4 = 25f
private const val W3 = 33f
private const val W3_MID = 34f
private const val W2 = 50f

/** The ten button groups of the original buttons_layout, by group. */
internal val GROUP_ROWS: Map<ButtonGroup, Pair<GroupStyle, List<GroupButton>>> = mapOf(
    ButtonGroup.Sensor to (
        ScanStyle to listOf(
            cmd("Magn", TrekColor.Yellow, TrekEdge.Left, W5, TrekCommand.Magnetic),
            cmd("Orient", TrekColor.Yellow, TrekEdge.Center, W5, TrekCommand.Orientation),
            cmd("Grav", TrekColor.Yellow, TrekEdge.Center, W5, TrekCommand.Gravity),
            cmd("Temp", TrekColor.Yellow, TrekEdge.Center, W5, TrekCommand.Temperature),
            cmd("OFF", TrekColor.Yellow, TrekEdge.Right, W5, TrekCommand.SensorOff),
        )
        ),
    ButtonGroup.Comm to (
        ActionStyle to listOf(
            cmd("OPEN COMM", TrekColor.Green, TrekEdge.Left, W4, TrekCommand.Hailing),
            cmd("CLOSE COMM", TrekColor.Green, TrekEdge.Center, W4, TrekCommand.HailingClose),
            cmd("INTER COMM", TrekColor.Green, TrekEdge.Center, W4, TrekCommand.Intercom),
            cmd("CHAT COMM", TrekColor.Green, TrekEdge.Right, W4, TrekCommand.ChatComm),
        )
        ),
    ButtonGroup.Shield to (
        ActionStyle to listOf(
            cmd("SHIELDS UP", TrekColor.Orange, TrekEdge.Left, W2, TrekCommand.RaiseShield),
            cmd("SHIELDS DOWN", TrekColor.Orange, TrekEdge.Right, W2, TrekCommand.LowerShield),
        )
        ),
    ButtonGroup.Fire to (
        ActionStyle to listOf(
            cmd("PHASER", TrekColor.Red, TrekEdge.Left, W4, TrekCommand.Phaser),
            GroupButton("Yellow Alert", TrekColor.Yellow, TrekEdge.Center, W4, TrycorderIntent.YellowAlert),
            GroupButton("RED ALERT", TrekColor.Pink, TrekEdge.Center, W4, TrycorderIntent.RedAlert),
            cmd("TORPEDO", TrekColor.Red, TrekEdge.Right, W4, TrekCommand.Torpedo),
        )
        ),
    ButtonGroup.Transporter to (
        ActionStyle to listOf(
            cmd("TRANSPORT OUT", TrekColor.Pink, TrekEdge.Left, W2, TrekCommand.BeamMeDown),
            cmd("TRANSPORT IN", TrekColor.Pink, TrekEdge.Right, W2, TrekCommand.BeamMeUp),
        )
        ),
    ButtonGroup.Tractor to (
        ActionStyle to listOf(
            cmd("TRACTOR PUSH", TrekColor.Indigo, TrekEdge.Left, W3, TrekCommand.TractorPush),
            cmd("TRACTOR OFF", TrekColor.Indigo, TrekEdge.Center, W3_MID, TrekCommand.TractorOff),
            cmd("TRACTOR PULL", TrekColor.Indigo, TrekEdge.Right, W3, TrekCommand.TractorPull),
        )
        ),
    ButtonGroup.Motor to (
        ActionStyle to listOf(
            cmd("MOTOR IMPULSE", TrekColor.Vert, TrekEdge.Left, W3, TrekCommand.ImpulsePower),
            cmd("MOTOR OFF", TrekColor.Vert, TrekEdge.Center, W3_MID, TrekCommand.StayHere),
            cmd("MOTOR WARP", TrekColor.Vert, TrekEdge.Right, W3, TrekCommand.WarpDrive),
        )
        ),
    ButtonGroup.Viewer to (
        ScanStyle to listOf(
            cmd("Main Viewer", TrekColor.Mauve, TrekEdge.Left, W4, TrekCommand.ViewerOn),
            cmd("Local Viewer", TrekColor.Mauve, TrekEdge.Center, W4, TrekCommand.LocalViewer),
            cmd("Viewer Off", TrekColor.Mauve, TrekEdge.Right, W4, TrekCommand.ViewerOff),
            cmd("Viewer Photo", TrekColor.Mauve, TrekEdge.Round, W4, TrekCommand.ViewerPhoto),
        )
        ),
    ButtonGroup.Logs to (
        ScanStyle to listOf(
            cmd("Logs", TrekColor.Blue, TrekEdge.Left, W5, TrekCommand.LogsConsole),
            cmd("Info", TrekColor.Blue, TrekEdge.Center, W5, TrekCommand.LogsInfo),
            cmd("Plan", TrekColor.Blue, TrekEdge.Center, W5, TrekCommand.SystemPlans),
            cmd("Sys", TrekColor.Blue, TrekEdge.Center, W5, TrekCommand.SystemInfo),
            cmd("Stat", TrekColor.Blue, TrekEdge.Right, W5, TrekCommand.SystemStat),
        )
        ),
    ButtonGroup.Mode to (
        ScanStyle to listOf(
            GroupButton(
                "Crew Info / Evaluation", TrekColor.Gray, TrekEdge.Left, W2,
                TrycorderIntent.OpenScreen(Screen.Crew),
            ),
            GroupButton(
                "Ship Inventory", TrekColor.Gray, TrekEdge.Right, W2,
                TrycorderIntent.OpenScreen(Screen.Inventory),
            ),
        )
        ),
)

/** The row of buttons under the sensor area. Pure. */
@Composable
fun GroupRow(buttons: List<GroupButton>, style: GroupStyle, onIntent: (TrycorderIntent) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxSize().background(TrekPalette.Background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        buttons.forEach { b ->
            TrekButton(
                text = b.text,
                color = b.color,
                edge = b.edge,
                font = style.font,
                bold = style.bold,
                maxLines = style.maxLines,
                onClick = { onIntent(b.intent) },
                modifier = Modifier.weight(b.weight).height(GroupButtonHeight),
            )
        }
    }
}
