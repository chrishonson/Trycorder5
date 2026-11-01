package net.ddns.mlsoftlaberge.trycorder.vision

import net.ddns.mlsoftlaberge.trycorder.camera.CameraEffect
import net.ddns.mlsoftlaberge.trycorder.camera.CameraScene
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage

/**
 * State of the vision screens: Vision Cam1 (TryvisionFragment, [day] false, Camera1 color
 * effects cycled by tapping the viewer) and Vision Cam2 (TryviscamFragment, [day] true, Camera2
 * effect and scene modes, tapping the viewer takes a picture).
 */
data class VisionState(
    val day: Boolean = false,
    /** mVisionMode / mVisionEffect, 0 based. */
    val effectIndex: Int = 0,
    /** mVisionScene, 0 based (Cam2 only). */
    val sceneIndex: Int = 0,
    val topStatus: String = if (day) "Vision Camera2" else "Vision",
    val bottomStatus: String = "Ready",
    /** onResume() forced the vision languages to French. */
    val language: TrekLanguage = TrekLanguage.French,
    val talking: Boolean = false,
) {
    val effect: CameraEffect get() = VisionModes.effects(day)[effectIndex]
    val scene: CameraScene? get() = if (day) VisionModes.SCENES[sceneIndex] else null
}

/** The mode tables of the two fragments, in their original order. */
object VisionModes {
    /** switchcam(mode) of TryvisionFragment, modes 1..9. */
    val NIGHT_EFFECTS = listOf(
        CameraEffect.Mono, CameraEffect.Negative, CameraEffect.Sepia, CameraEffect.Aqua, CameraEffect.Blackboard,
        CameraEffect.Posterize, CameraEffect.Solarize, CameraEffect.Whiteboard, CameraEffect.None,
    )

    /** setVisionMode() of TryviscamFragment, effects 1..9. */
    val DAY_EFFECTS = listOf(
        CameraEffect.None, CameraEffect.Mono, CameraEffect.Negative, CameraEffect.Blackboard, CameraEffect.Whiteboard,
        CameraEffect.Sepia, CameraEffect.Solarize, CameraEffect.Posterize, CameraEffect.Aqua,
    )

    val SCENES = listOf(CameraScene.Portrait, CameraScene.NightPortrait)

    fun effects(day: Boolean): List<CameraEffect> = if (day) DAY_EFFECTS else NIGHT_EFFECTS

    /** The status line of setVisionMode(), e.g. "View Normal, Portrait". */
    fun describe(effect: CameraEffect, scene: CameraScene): String {
        val effectName = when (effect) {
            CameraEffect.None -> "View Normal"
            else -> "View ${effect.name}"
        }
        val sceneName = when (scene) {
            CameraScene.Portrait -> "Portrait"
            CameraScene.NightPortrait -> "Night Portrait"
        }
        return "$effectName, $sceneName"
    }
}

sealed interface VisionIntent {
    /** The screen is shown: modes back to 1 like switchfragment() did. */
    data object Load : VisionIntent

    /** Top-left / bottom-left frame images and the Back button. */
    data object Back : VisionIntent
    data object Settings : VisionIntent

    /** Tap on the viewer: next effect (Cam1) or take a picture (Cam2). */
    data object ViewerTap : VisionIntent

    // Cam1
    data object Speak : VisionIntent
    data object TalkDown : VisionIntent
    data object TalkUp : VisionIntent

    // Cam2
    data object NextEffect : VisionIntent
    data object NextScene : VisionIntent
}
