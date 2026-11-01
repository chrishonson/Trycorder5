package net.ddns.mlsoftlaberge.trycorder.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Live camera preview (the TextureView of the original viewer). Binds [controller] while shown. */
@Composable
expect fun CameraViewer(controller: CameraController, front: Boolean, modifier: Modifier = Modifier)
