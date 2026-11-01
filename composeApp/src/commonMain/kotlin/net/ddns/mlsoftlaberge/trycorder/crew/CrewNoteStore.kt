package net.ddns.mlsoftlaberge.trycorder.crew

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import net.ddns.mlsoftlaberge.trycorder.inventory.FileAccessor

/**
 * Crew notes kept in app storage, keyed by contact identifier, in the same text format as the
 * Android contact Note field. Used where the address book note cannot be written (iOS).
 */
class CrewNoteStore(
    private val file: FileAccessor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val mutex = Mutex()
    private val serializer = MapSerializer(String.serializer(), String.serializer())
    private val json = Json { ignoreUnknownKeys = true }
    private var notes: Map<String, String>? = null

    suspend fun get(id: String): String = mutex.withLock { loaded()[id].orEmpty() }

    suspend fun all(): Map<String, String> = mutex.withLock { loaded() }

    /** Stores [text] for [id]; an empty text removes the entry. Returns whether an entry existed. */
    suspend fun put(id: String, text: String): Boolean = mutex.withLock {
        val current = loaded()
        val existed = id in current
        val next = if (text.isEmpty()) current - id else current + (id to text)
        withContext(dispatcher) { file.write(json.encodeToString(serializer, next)) }
        notes = next
        existed
    }

    private suspend fun loaded(): Map<String, String> =
        notes ?: withContext(dispatcher) { decode(file.read()) }.also { notes = it }

    private fun decode(text: String?): Map<String, String> = if (text == null) {
        emptyMap()
    } else {
        try {
            json.decodeFromString(serializer, text)
        } catch (expected: SerializationException) {
            emptyMap()
        } catch (expected: IllegalArgumentException) {
            emptyMap()
        }
    }
}
