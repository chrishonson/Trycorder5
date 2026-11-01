package net.ddns.mlsoftlaberge.trycorder.core

/** Persistent key/value storage. Android: default SharedPreferences. iOS: NSUserDefaults. */
interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getString(key: String, default: String): String
    fun getInt(key: String, default: Int): Int
    fun putBoolean(key: String, value: Boolean)
    fun putString(key: String, value: String)
    fun putInt(key: String, value: Int)
}

/** In-memory store for tests and previews. */
class InMemoryKeyValueStore(initial: Map<String, Any> = emptyMap()) : KeyValueStore {
    private val values = initial.toMutableMap()
    override fun getBoolean(key: String, default: Boolean) = values[key] as? Boolean ?: default
    override fun getString(key: String, default: String) = values[key] as? String ?: default
    override fun getInt(key: String, default: Int) = values[key] as? Int ?: default
    override fun putBoolean(key: String, value: Boolean) { values[key] = value }
    override fun putString(key: String, value: String) { values[key] = value }
    override fun putInt(key: String, value: Int) { values[key] = value }
}
