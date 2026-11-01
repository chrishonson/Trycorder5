package net.ddns.mlsoftlaberge.trycorder

import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

/**
 * The activity currently on screen, for platform services that need one
 * (activity results, camera lifecycle). Held weakly so it never leaks.
 */
object AndroidActivityHolder {
    private var ref: WeakReference<ComponentActivity> = WeakReference(null)

    val activity: ComponentActivity? get() = ref.get()

    fun attach(activity: ComponentActivity) {
        ref = WeakReference(activity)
    }

    fun detach(activity: ComponentActivity) {
        if (ref.get() === activity) ref = WeakReference(null)
    }
}
