package net.ddns.mlsoftlaberge.trycorder.media

import android.content.ActivityNotFoundException
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.ddns.mlsoftlaberge.trycorder.AndroidActivityHolder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * startActivityForResult / onActivityResult as a suspend call, through the activity result
 * registry of the activity on screen. Null when there is no activity.
 */
internal object ActivityResults {
    private const val TAG = "Trycorder"
    private val nextKey = AtomicInteger()

    suspend fun <I, O> launch(contract: ActivityResultContract<I, O>, input: I): O? = withContext(Dispatchers.Main) {
        val activity = AndroidActivityHolder.activity ?: return@withContext null
        suspendCancellableCoroutine { cont ->
            var launcher: ActivityResultLauncher<I>? = null
            launcher = activity.activityResultRegistry.register(
                "trycorder_media_${nextKey.incrementAndGet()}",
                contract,
            ) { result ->
                launcher?.unregister()
                if (cont.isActive) cont.resume(result)
            }
            cont.invokeOnCancellation { launcher.unregister() }
            try {
                launcher.launch(input)
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "No activity for ${e.message}")
                launcher.unregister()
                cont.resume(null)
            }
        }
    }
}
