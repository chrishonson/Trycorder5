package net.ddns.mlsoftlaberge.trycorder.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.CoreGraphics.CGRectZero
import platform.QuartzCore.CATransaction
import platform.UIKit.UIColor
import platform.UIKit.UIView
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette

/** Hosts the AVCaptureVideoPreviewLayer and keeps it the size of the view. */
@OptIn(ExperimentalForeignApi::class)
private class PreviewView(private val preview: AVCaptureVideoPreviewLayer) : UIView(frame = CGRectZero.readValue()) {
    init {
        backgroundColor = UIColor.blackColor
        layer.addSublayer(preview)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        preview.frame = bounds
        CATransaction.commit()
    }
}

/**
 * The live preview. The vision [effect] and [scene] have no AVFoundation equivalent on the
 * preview layer, so iOS shows the plain picture (the status line still names the mode).
 */
@Composable
actual fun CameraViewer(
    controller: CameraController,
    front: Boolean,
    modifier: Modifier,
    effect: CameraEffect,
    scene: CameraScene?,
) {
    val camera = controller as? IosCameraController
    if (camera == null) {
        Box(modifier.background(TrekPalette.Background))
        return
    }
    DisposableEffect(camera, front) {
        camera.start(front)
        onDispose { camera.stop() }
    }
    UIKitView(factory = { PreviewView(camera.previewLayer) }, modifier = modifier)
}
