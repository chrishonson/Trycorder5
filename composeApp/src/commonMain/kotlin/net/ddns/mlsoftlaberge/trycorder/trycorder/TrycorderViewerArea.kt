package net.ddns.mlsoftlaberge.trycorder.trycorder

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.camera.CameraController
import net.ddns.mlsoftlaberge.trycorder.camera.CameraViewer
import net.ddns.mlsoftlaberge.trycorder.camera.Face
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import net.ddns.mlsoftlaberge.trycorder.resources.earth_still
import net.ddns.mlsoftlaberge.trycorder.resources.federation_logo
import net.ddns.mlsoftlaberge.trycorder.resources.roddenberry_stillwell
import net.ddns.mlsoftlaberge.trycorder.resources.starship_build
import net.ddns.mlsoftlaberge.trycorder.resources.starship_plan
import net.ddns.mlsoftlaberge.trycorder.resources.starship_sideview
import net.ddns.mlsoftlaberge.trycorder.resources.starship_topview
import net.ddns.mlsoftlaberge.trycorder.resources.starship_view
import net.ddns.mlsoftlaberge.trycorder.resources.trek_horizontalline
import net.ddns.mlsoftlaberge.trycorder.resources.universe
import net.ddns.mlsoftlaberge.trycorder.sensors.ui.MemoryStatPanel
import net.ddns.mlsoftlaberge.trycorder.ui.AnimatedGif
import net.ddns.mlsoftlaberge.trycorder.ui.TrekButton
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFont
import net.ddns.mlsoftlaberge.trycorder.ui.TrekInput
import net.ddns.mlsoftlaberge.trycorder.ui.TrekLog
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import net.ddns.mlsoftlaberge.trycorder.ui.TrekSlider
import net.ddns.mlsoftlaberge.trycorder.voice.TrekCommand
import net.ddns.mlsoftlaberge.trycorder.voice.VoiceCommands
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource

private val ModeButtonHeight = 40.dp
private val SliderHeight = 32.dp
private val LineHeight = 4.dp
private const val LABEL_SP = 18
private const val COMMAND_LOG_WEIGHT = 5f
private const val COMMAND_BUTTON_WEIGHT = 2f
private const val COMMAND_INPUT_WEIGHT = 3f
private const val SEEK_WEIGHT = 8f
private const val HEAD_STROKE = 3f
private const val EYE_DIVISOR = 2.5f
private const val HEAD_FACTOR = 1.5f
private const val EYE_OFFSET_DIVISOR = 8f

/** The viewer area of the main screen: one page at a time. */
@Composable
fun ViewerArea(
    state: TrycorderState,
    camera: CameraController,
    onIntent: (TrycorderIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val full = modifier.fillMaxSize()
    when (state.viewerPage) {
        ViewerPage.FederationLogo -> Image(
            painterResource(Res.drawable.federation_logo), null,
            full.clickable { onIntent(TrycorderIntent.FederationLogo) },
        )
        ViewerPage.Camera -> CameraViewer(camera, state.viewerFront, full)
        ViewerPage.LogsConsole -> TrekLog(state.consoleLog, TrekPalette.HoloGreenLight, full, font = TrekFont.Button)
        ViewerPage.LogsInfo, ViewerPage.SystemInfo -> TrekLog(state.infoText, TrekPalette.Text, full, font = TrekFont.Button)
        ViewerPage.Plans -> PlansPage(state.planIndex, { onIntent(TrycorderIntent.NextPlan) }, full)
        ViewerPage.Photo -> PhotoPage(state.picture.bytes, state.picture.faces, full)
        ViewerPage.Animate -> MotorViewer(state.motorAnimation, full)
        ViewerPage.SpeakList -> TrekLog(VoiceCommands.SPEAK_LIST, TrekPalette.Text, full)
        ViewerPage.LogsStat -> MemoryStatPanel(state.readings.memory, full)
        ViewerPage.ModeWindow -> ModeWindow(onIntent, full)
        ViewerPage.TransporterSeek -> TransporterWindow(
            state.transporterSliders.map { it.progress }, state.transporterTextUp, state.transporterTextDown, onIntent, full,
        )
        ViewerPage.FireControl -> FireControlWindow(state.fireDirection, state.fireForce, onIntent, full)
        ViewerPage.TractorBeam -> with(state.beamControls) { TractorBeamWindow(rotate, freq, force, onIntent, full) }
        ViewerPage.CommandControl -> CommandWindow(state.commandLog, state.commandInput, onIntent, full)
    }
}

/** Starship plans: tap for the next picture, each one with the animation of the original. */
@Composable
fun PlansPage(index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val plans = listOf(
        Res.drawable.starship_view, Res.drawable.starship_build, Res.drawable.starship_plan,
        Res.drawable.starship_sideview, Res.drawable.starship_topview, Res.drawable.earth_still, Res.drawable.universe,
    )
    val rotation = remember(index) { Animatable(0f) }
    val alpha = remember(index) { Animatable(1f) }
    val scaleY = remember(index) { Animatable(1f) }
    LaunchedEffect(index) { PlanAnimation.run(index, rotation, alpha, scaleY) }
    Image(
        painter = painterResource(plans[index % plans.size]),
        contentDescription = null,
        modifier = modifier.clickable(onClick = onClick).graphicsLayer {
            rotationZ = rotation.value
            this.alpha = alpha.value
            this.scaleY = scaleY.value
        },
    )
}

/** res/anim: clockwise, fadein, slidein, none, blink, rotateback, slidein. */
private object PlanAnimation {
    private const val TURN = 360f
    private const val CLOCKWISE_MS = 1000
    private const val FADE_MS = 2000
    private const val SLIDE_MS = 500
    private const val BLINK_MS = 600
    private const val ROTATE_BACK_MS = 5000

    suspend fun run(index: Int, rotation: Animatable<Float, *>, alpha: Animatable<Float, *>, scaleY: Animatable<Float, *>) {
        when (index) {
            0 -> rotation.animateTo(TURN, tween(CLOCKWISE_MS))
            1 -> alpha.snapTo(0f).also { alpha.animateTo(1f, tween(FADE_MS)) }
            2, 6 -> scaleY.snapTo(0f).also { scaleY.animateTo(1f, tween(SLIDE_MS, easing = LinearEasing)) }
            4 -> alpha.animateTo(0f, infiniteRepeatable(tween(BLINK_MS), RepeatMode.Reverse))
            5 -> {
                rotation.animateTo(TURN, tween(ROTATE_BACK_MS))
                rotation.animateTo(0f, tween(ROTATE_BACK_MS))
            }
            else -> Unit
        }
    }
}

/** The snapped picture with red circles around heads and blue circles around eyes. */
@Composable
fun PhotoPage(bytes: ByteArray?, faces: List<Face>, modifier: Modifier = Modifier) {
    val bitmap: ImageBitmap? = remember(bytes) { bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }
    if (bitmap == null) {
        Image(painterResource(Res.drawable.roddenberry_stillwell), null, modifier)
        return
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Canvas(Modifier.fillMaxSize()) {
            val scale = minOf(size.width / bitmap.width, size.height / bitmap.height)
            val dx = (size.width - bitmap.width * scale) / 2
            val dy = (size.height - bitmap.height * scale) / 2
            faces.forEach { f ->
                val mid = Offset(dx + f.midX * scale, dy + f.midY * scale)
                val eye = f.eyesDistance * scale
                drawCircle(Color.Red, HEAD_FACTOR * eye, mid, style = Stroke(HEAD_STROKE))
                val eyeY = mid.y - eye / EYE_OFFSET_DIVISOR
                drawCircle(Color.Blue, eye / EYE_DIVISOR, Offset(mid.x - eye / 2, eyeY), style = Stroke(1f))
                drawCircle(Color.Blue, eye / EYE_DIVISOR, Offset(mid.x + eye / 2, eyeY), style = Stroke(1f))
            }
        }
    }
}

/** Still earth, rotating earth (impulse) or warp tunnel (warp). */
@Composable
fun MotorViewer(animation: MotorAnimation, modifier: Modifier = Modifier) {
    when (animation) {
        MotorAnimation.Still -> Image(painterResource(Res.drawable.earth_still), null, modifier)
        MotorAnimation.Impulse -> AnimatedGif("files/earth_rotating.gif", modifier)
        MotorAnimation.Warp -> AnimatedGif("files/warp_animation.gif", modifier)
    }
}

private data class ModeEntry(val text: String, val color: TrekColor, val intent: TrycorderIntent)

private val MODE_ROWS = listOf(
    listOf(
        ModeEntry("Photo Gal.", TrekColor.Green, TrycorderIntent.OpenScreen(Screen.PhotoGallery)),
        ModeEntry("Video Gal.", TrekColor.Orange, TrycorderIntent.OpenScreen(Screen.VideoGallery)),
    ),
    listOf(
        ModeEntry("MultiSensor", TrekColor.Yellow, TrycorderIntent.OpenScreen(Screen.MultiSensor)),
        ModeEntry("Client Mode", TrekColor.Indigo, TrycorderIntent.OpenScreen(Screen.Client)),
    ),
    listOf(
        ModeEntry("Vision Cam1", TrekColor.Green, TrycorderIntent.OpenScreen(Screen.VisionNight)),
        ModeEntry("Vision Cam2", TrekColor.Green, TrycorderIntent.OpenScreen(Screen.VisionDay)),
    ),
    listOf(
        ModeEntry("Desktop", TrekColor.Indigo, TrycorderIntent.OpenScreen(Screen.Desktop)),
        ModeEntry(
            "Auto Destruct", TrekColor.Red,
            TrycorderIntent.Command(TrekCommand.AutoDestruct),
        ),
    ),
)

/** The mode window: switch to the other Trycorder modes. */
@Composable
fun ModeWindow(onIntent: (TrycorderIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        MODE_ROWS.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { e ->
                    TrekButton(e.text, e.color, { onIntent(e.intent) }, Modifier.weight(1f).height(ModeButtonHeight))
                }
            }
        }
    }
}

/** Three vertical transporter sliders. */
@Composable
fun TransporterWindow(
    progress: List<Int>,
    textUp: String,
    textDown: String,
    onIntent: (TrycorderIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Label(textUp, Modifier.weight(1f).fillMaxWidth())
        Row(Modifier.weight(SEEK_WEIGHT).fillMaxWidth()) {
            progress.forEachIndexed { i, p ->
                TrekSlider(
                    value = p,
                    max = TrycorderState.TRANSPORTER_MAX,
                    color = TrekColor.Yellow,
                    vertical = true,
                    onValueChange = { onIntent(TrycorderIntent.TransporterSlide(i, it)) },
                    onStart = { onIntent(TrycorderIntent.TransporterSlideStart(i)) },
                    onStop = { onIntent(TrycorderIntent.TransporterSlideStop(i)) },
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
            }
        }
        Label(textDown, Modifier.weight(1f).fillMaxWidth())
    }
}

/** Phaser/torpedo direction and force. */
@Composable
fun FireControlWindow(direction: Int, force: Int, onIntent: (TrycorderIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Labels("Direction", "$direction", "")
        TrekSlider(
            direction + TrycorderState.FIRE_DIRECTION_MAX / 2, TrycorderState.FIRE_DIRECTION_MAX, TrekColor.Green,
            { onIntent(TrycorderIntent.FireDirection(it)) }, Modifier.fillMaxWidth().height(SliderHeight),
        )
        Labels("Left", "Center", "Right")
        HorizontalLine()
        Labels("Force", "$force/${TrycorderState.FIRE_FORCE_MAX}", "")
        TrekSlider(
            force, TrycorderState.FIRE_FORCE_MAX, TrekColor.Orange,
            { onIntent(TrycorderIntent.FireForce(it)) }, Modifier.fillMaxWidth().height(SliderHeight),
        )
        Labels("Low", "Medium", "Max")
        HorizontalLine()
    }
}

/** Rotation, frequency and strength of the tractor beam (also used by the shields). */
@Composable
fun TractorBeamWindow(rotate: Boolean, freq: Int, force: Int, onIntent: (TrycorderIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = rotate,
                onCheckedChange = { onIntent(TrycorderIntent.BeamRotate(it)) },
                colors = CheckboxDefaults.colors(uncheckedColor = TrekPalette.HoloGreenLight, checkedColor = TrekPalette.HoloGreenLight),
            )
            Text("Rotating Freq.", color = TrekPalette.HoloGreenLight)
        }
        Label("Freq Control", Modifier.fillMaxWidth(), TrekPalette.HoloGreenLight, TextAlign.Center)
        TrekSlider(
            freq, TrycorderState.BEAM_FREQ_MAX, TrekColor.Indigo,
            { onIntent(TrycorderIntent.BeamFreq(it)) }, Modifier.fillMaxWidth().height(SliderHeight),
        )
        Label("Strength Control", Modifier.fillMaxWidth(), TrekPalette.HoloGreenLight, TextAlign.Center)
        TrekSlider(
            force, TrycorderState.BEAM_FORCE_MAX, TrekColor.Orange,
            { onIntent(TrycorderIntent.BeamForce(it)) }, Modifier.fillMaxWidth().height(SliderHeight),
        )
    }
}

/** Command log, send button and command field. */
@Composable
fun CommandWindow(log: String, input: String, onIntent: (TrycorderIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        TrekLog(log.ifEmpty { "Command log\n------------------\n" }, TrekPalette.Text, Modifier.weight(COMMAND_LOG_WEIGHT).fillMaxWidth())
        TrekButton(
            "Send Command/Message", TrekColor.Green, { onIntent(TrycorderIntent.CommandSend) },
            Modifier.weight(COMMAND_BUTTON_WEIGHT).fillMaxWidth(), font = TrekFont.Default,
        )
        TrekInput(input, { onIntent(TrycorderIntent.CommandInput(it)) }, Modifier.weight(COMMAND_INPUT_WEIGHT).fillMaxWidth(), singleLine = false)
    }
}

@Composable
private fun Labels(left: String, center: String, right: String) {
    Row(Modifier.fillMaxWidth()) {
        Label(left, Modifier.weight(1f), align = TextAlign.Start)
        Label(center, Modifier.weight(1f), align = TextAlign.Center)
        Label(right, Modifier.weight(1f), align = TextAlign.End)
    }
}

@Composable
private fun Label(text: String, modifier: Modifier, color: Color = TrekPalette.Text, align: TextAlign = TextAlign.Start) {
    Text(text, color = color, fontSize = LABEL_SP.sp, textAlign = align, modifier = modifier)
}

@Composable
private fun HorizontalLine() {
    Image(
        painter = painterResource(Res.drawable.trek_horizontalline),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxWidth().height(LineHeight),
    )
}
