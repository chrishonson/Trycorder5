package net.ddns.mlsoftlaberge.trycorder.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette

@Composable
actual fun CameraViewer(controller: CameraController, front: Boolean, modifier: Modifier) {
    Box(modifier.background(TrekPalette.Background))
}
