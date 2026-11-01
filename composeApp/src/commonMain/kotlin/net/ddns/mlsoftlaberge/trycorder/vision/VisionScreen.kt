package net.ddns.mlsoftlaberge.trycorder.vision

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.camera.CameraController
import net.ddns.mlsoftlaberge.trycorder.camera.CameraEffect
import net.ddns.mlsoftlaberge.trycorder.camera.CameraScene
import net.ddns.mlsoftlaberge.trycorder.camera.CameraViewer
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.gallery.LcarsButton
import net.ddns.mlsoftlaberge.trycorder.gallery.LcarsButtonHeight
import net.ddns.mlsoftlaberge.trycorder.gallery.LcarsPage
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFont
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import net.ddns.mlsoftlaberge.trycorder.ui.family
import net.ddns.mlsoftlaberge.trycorder.ui.trekBackground

private const val HALF_TOP = 3f
private const val HALF_BOTTOM = 4f
private const val BUTTON_SP = 18
private val ButtonMargin = 2.dp

/**
 * Vision Cam1 ([day] false, TryvisionFragment) and Vision Cam2 ([day] true, TryviscamFragment).
 * Going back to the Trycorder mode uses [onBack], the settings [onNavigate].
 */
@Composable
fun VisionScreen(services: PlatformServices, day: Boolean, onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    val store = viewModel(key = if (day) "vision-day" else "vision-night") {
        val visionServices = VisionServices(
            camera = services.camera,
            sound = services.sound,
            speaker = services.speaker,
            listener = services.listener,
            link = services.link,
        )
        VisionStore(day, visionServices) { screen -> if (screen == Screen.Trycorder) onBack() else onNavigate(screen) }
    }
    LaunchedEffect(store) { store.dispatch(VisionIntent.Load) }
    val state by store.state.collectAsStateWithLifecycle()
    VisionPage(
        day = state.day,
        topStatus = state.topStatus,
        bottomStatus = state.bottomStatus,
        camera = services.camera,
        effect = state.effect,
        scene = state.scene,
        onIntent = store::dispatch,
    )
}

/** The LCARS frame with the camera viewer and the buttons of the selected vision fragment. */
@Composable
fun VisionPage(
    day: Boolean,
    topStatus: String,
    bottomStatus: String,
    camera: CameraController,
    effect: CameraEffect,
    scene: CameraScene?,
    onIntent: (VisionIntent) -> Unit,
) {
    LcarsPage(
        topStatus = topStatus,
        bottomStatus = bottomStatus,
        onCorner = { onIntent(VisionIntent.Back) },
        topButtons = {
            LcarsButton("Back", TrekColor.Indigo, TrekEdge.Center, HALF_TOP, { onIntent(VisionIntent.Back) })
            LcarsButton("Settings", TrekColor.Gray, TrekEdge.Right, HALF_TOP, { onIntent(VisionIntent.Settings) })
        },
        bottomButtons = {
            if (day) {
                val effectClick = { onIntent(VisionIntent.NextEffect) }
                LcarsButton("EFFECT", TrekColor.Yellow, TrekEdge.Center, HALF_BOTTOM, effectClick)
                LcarsButton("SCENE", TrekColor.Gray, TrekEdge.Right, HALF_BOTTOM, { onIntent(VisionIntent.NextScene) })
            } else {
                LcarsButton("Speak", TrekColor.Indigo, TrekEdge.Center, HALF_BOTTOM, { onIntent(VisionIntent.Speak) })
                PushButton(
                    "Talk", TrekColor.Red, TrekEdge.Right, HALF_BOTTOM,
                    onDown = { onIntent(VisionIntent.TalkDown) },
                    onUp = { onIntent(VisionIntent.TalkUp) },
                )
            }
        },
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            CameraViewer(camera, front = false, modifier = Modifier.fillMaxSize(), effect = effect, scene = scene)
            // The preview is a platform view that eats touches: a transparent layer takes the taps.
            Box(Modifier.fillMaxSize().clickable { onIntent(VisionIntent.ViewerTap) })
        }
    }
}

/** A trek button that reports press and release (the OnTouchListener of the Talk button). */
@Composable
private fun RowScope.PushButton(
    text: String,
    color: TrekColor,
    edge: TrekEdge,
    weight: Float,
    onDown: () -> Unit,
    onUp: () -> Unit,
) {
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.weight(weight).height(LcarsButtonHeight).padding(ButtonMargin)
            .trekBackground(color, edge, pressed)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    down()
                    tryAwaitRelease()
                    pressed = false
                    up()
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = TrekPalette.ButtonText,
            fontFamily = TrekFont.Button.family(),
            fontWeight = FontWeight.Bold,
            fontSize = BUTTON_SP.sp,
            maxLines = 1,
        )
    }
}
