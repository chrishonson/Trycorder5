package net.ddns.mlsoftlaberge.trycorder.sensors.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.ddns.mlsoftlaberge.trycorder.core.MemoryStats
import net.ddns.mlsoftlaberge.trycorder.sensors.Environment
import net.ddns.mlsoftlaberge.trycorder.sensors.GeoLocation
import net.ddns.mlsoftlaberge.trycorder.sensors.Vector3
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette

// Placeholder panels. The sensor feature replaces each body with the port of the matching
// *SensorView.java canvas drawing. Signatures are the contract used by TrycorderScreen.

@Composable
fun MagneticPanel(field: Vector3, running: Boolean, modifier: Modifier = Modifier) =
    Placeholder("MAG $field $running", modifier)

@Composable
fun OrientationPanel(
    orientation: Vector3,
    location: GeoLocation?,
    running: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Placeholder("ORI $orientation $location $running", modifier.clickable(onClick = onClick))

@Composable
fun GravityPanel(acceleration: Vector3, running: Boolean, modifier: Modifier = Modifier) =
    Placeholder("GRA $acceleration $running", modifier)

@Composable
fun TemperaturePanel(environment: Environment, running: Boolean, modifier: Modifier = Modifier) =
    Placeholder("TEM $environment $running", modifier)

@Composable
fun AudioPanel(samples: ShortArray, running: Boolean, modifier: Modifier = Modifier) =
    Placeholder("AUD ${samples.size} $running", modifier)

/** mode 1 = raise, 2 = lower. */
@Composable
fun ShieldsPanel(mode: Int, freq: Int, force: Int, rotate: Boolean, running: Boolean, modifier: Modifier = Modifier) =
    Placeholder("SHI $mode $freq $force $rotate $running", modifier)

/** mode 1 = torpedo, 2 = phaser. [shot] changes each time the weapon fires. */
@Composable
fun FirePanel(mode: Int, direction: Int, force: Int, shot: Int, modifier: Modifier = Modifier) =
    Placeholder("FIR $mode $direction $force $shot", modifier)

/** mode 1 = beam in, 2 = beam out; [position] 0..255 from the transporter sliders. */
@Composable
fun TransporterPanel(mode: Int, position: Int, running: Boolean, modifier: Modifier = Modifier) =
    Placeholder("TRA $mode $position $running", modifier)

/** mode 0 = off, 1 = push, 2 = pull. */
@Composable
fun TractorPanel(mode: Int, freq: Int, force: Int, rotate: Boolean, running: Boolean, modifier: Modifier = Modifier) =
    Placeholder("TRB $mode $freq $force $rotate $running", modifier)

/** mode 0 = off, 1 = impulse, 2 = warp. */
@Composable
fun MotorPanel(mode: Int, running: Boolean, modifier: Modifier = Modifier) =
    Placeholder("MOT $mode $running", modifier)

/** Logs > Stat page. */
@Composable
fun MemoryStatPanel(memory: MemoryStats, modifier: Modifier = Modifier) =
    Placeholder("MEM ${memory.total} ${memory.free}", modifier)

@Composable
private fun Placeholder(text: String, modifier: Modifier) {
    Box(modifier.background(TrekPalette.Background), contentAlignment = Alignment.Center) {
        Text(text, color = TrekPalette.HoloGreenLight)
    }
}
