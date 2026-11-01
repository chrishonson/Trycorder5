package net.ddns.mlsoftlaberge.trycorder.camera

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
actual fun CameraViewer(
    controller: CameraController,
    front: Boolean,
    modifier: Modifier,
    effect: CameraEffect,
    scene: CameraScene?,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            // TextureView based, so a color filter can be applied over the preview.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    DisposableEffect(owner, front, effect, scene) {
        val binding = CameraBinding(context, owner, previewView, controller as? AndroidCameraController)
        binding.bind(front, effect, scene)
        onDispose { binding.unbind() }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}
