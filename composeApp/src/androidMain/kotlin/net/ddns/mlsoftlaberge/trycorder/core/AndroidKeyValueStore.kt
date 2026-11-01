package net.ddns.mlsoftlaberge.trycorder.core

import android.content.SharedPreferences
import androidx.core.content.edit

class AndroidKeyValueStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun getString(key: String, default: String) = prefs.getString(key, default) ?: default
    override fun getInt(key: String, default: Int) = prefs.getInt(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }
    override fun putString(key: String, value: String) = prefs.edit { putString(key, value) }
    override fun putInt(key: String, value: Int) = prefs.edit { putInt(key, value) }
}
