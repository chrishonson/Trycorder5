package net.ddns.mlsoftlaberge.trycorder.camera

import android.content.Context

/** Placeholder: replaced by the CameraX implementation. */
class AndroidCameraController(private val context: Context) : CameraController {
    override val isActive: Boolean = false
    override suspend fun snap(): SnapResult? = null
    override fun toString() = "AndroidCameraController(${context.packageName})"
}
