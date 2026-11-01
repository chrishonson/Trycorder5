package net.ddns.mlsoftlaberge.trycorder.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Live camera preview (the TextureView of the original viewer). Binds [controller] while shown.
 * [effect] and [scene] are the vision screen modes: hardware camera modes where the device has
 * them, otherwise a color filter over the preview; null [scene] keeps the automatic scene.
 */
@Composable
expect fun CameraViewer(
    controller: CameraController,
    front: Boolean,
    modifier: Modifier = Modifier,
    effect: CameraEffect = CameraEffect.None,
    scene: CameraScene? = null,
)
