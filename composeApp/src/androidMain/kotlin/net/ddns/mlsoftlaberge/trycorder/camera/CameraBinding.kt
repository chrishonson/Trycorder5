package net.ddns.mlsoftlaberge.trycorder.camera

import android.content.Context
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RenderEffect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutionException

/**
 * One binding of the camera to a [PreviewView] (switchcam() / openCamera()): back or front lens,
 * with the vision effect and scene as Camera2 capture request options when the camera has them,
 * otherwise the effect is drawn as a color filter over the preview.
 */
internal class CameraBinding(
    private val context: Context,
    private val owner: LifecycleOwner,
    private val view: PreviewView,
    private val controller: AndroidCameraController?,
) {
    private var disposed = false
    private var provider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var capture: ImageCapture? = null

    fun bind(front: Boolean, effect: CameraEffect, scene: CameraScene?) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                val cameraProvider = try {
                    future.get()
                } catch (e: ExecutionException) {
                    Log.w(TAG, "No camera provider: ${e.message}")
                    null
                }
                if (!disposed && cameraProvider != null) bindTo(cameraProvider, front, effect, scene)
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun unbind() {
        disposed = true
        capture?.let { controller?.detach(it) }
        val useCases = listOfNotNull(preview, capture)
        if (useCases.isNotEmpty()) provider?.unbind(*useCases.toTypedArray())
        preview = null
        capture = null
    }

    @OptIn(markerClass = [ExperimentalCamera2Interop::class])
    private fun bindTo(
        cameraProvider: ProcessCameraProvider,
        front: Boolean,
        effect: CameraEffect,
        scene: CameraScene?,
    ) {
        val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        val info = selector.filter(cameraProvider.availableCameraInfos).firstOrNull()
        if (info == null) {
            Log.w(TAG, "No ${if (front) "front" else "back"} camera")
            return
        }
        val modes = HardwareModes.of(info, effect, scene)
        val previewBuilder = Preview.Builder()
        val captureBuilder = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        modes.configure(Camera2Interop.Extender(previewBuilder))
        modes.configure(Camera2Interop.Extender(captureBuilder))
        val newPreview = previewBuilder.build().also { it.setSurfaceProvider(view.surfaceProvider) }
        val newCapture = captureBuilder.build()
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(owner, selector, newPreview, newCapture)
            provider = cameraProvider
            preview = newPreview
            capture = newCapture
            controller?.attach(newCapture)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Camera refused the use cases: ${e.message}")
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Camera not available: ${e.message}")
        }
        applyFilter(if (modes.effectMode != null || effect == CameraEffect.None) null else EffectMatrices.of(effect))
    }

    private fun applyFilter(matrix: FloatArray?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(matrix?.let { RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(it)) })
        } else if (matrix == null) {
            view.setLayerType(View.LAYER_TYPE_NONE, null)
        } else {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
        }
    }

    private companion object {
        const val TAG = "Trycorder"
    }
}

/** The Camera2 effect and scene modes a camera supports for the requested vision modes. */
internal class HardwareModes(val effectMode: Int?, private val sceneMode: Int?) {
    @OptIn(markerClass = [ExperimentalCamera2Interop::class])
    fun configure(extender: Camera2Interop.Extender<*>) {
        effectMode?.let { extender.setCaptureRequestOption(CaptureRequest.CONTROL_EFFECT_MODE, it) }
        sceneMode?.let {
            extender.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE)
            extender.setCaptureRequestOption(CaptureRequest.CONTROL_SCENE_MODE, it)
        }
    }

    companion object {
        private val EFFECTS = mapOf(
            CameraEffect.Mono to CaptureRequest.CONTROL_EFFECT_MODE_MONO,
            CameraEffect.Negative to CaptureRequest.CONTROL_EFFECT_MODE_NEGATIVE,
            CameraEffect.Sepia to CaptureRequest.CONTROL_EFFECT_MODE_SEPIA,
            CameraEffect.Aqua to CaptureRequest.CONTROL_EFFECT_MODE_AQUA,
            CameraEffect.Blackboard to CaptureRequest.CONTROL_EFFECT_MODE_BLACKBOARD,
            CameraEffect.Whiteboard to CaptureRequest.CONTROL_EFFECT_MODE_WHITEBOARD,
            CameraEffect.Posterize to CaptureRequest.CONTROL_EFFECT_MODE_POSTERIZE,
            CameraEffect.Solarize to CaptureRequest.CONTROL_EFFECT_MODE_SOLARIZE,
        )
        private val SCENES = mapOf(
            CameraScene.Portrait to CaptureRequest.CONTROL_SCENE_MODE_PORTRAIT,
            CameraScene.NightPortrait to CaptureRequest.CONTROL_SCENE_MODE_NIGHT_PORTRAIT,
        )

        @OptIn(markerClass = [ExperimentalCamera2Interop::class])
        fun of(info: CameraInfo, effect: CameraEffect, scene: CameraScene?): HardwareModes {
            val camera2 = Camera2CameraInfo.from(info)
            val effects = camera2.getCameraCharacteristic(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS)
                ?: IntArray(0)
            val scenes = camera2.getCameraCharacteristic(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)
                ?: IntArray(0)
            return HardwareModes(
                effectMode = EFFECTS[effect]?.takeIf { it in effects },
                sceneMode = scene?.let { SCENES[it] }?.takeIf { it in scenes },
            )
        }
    }
}
