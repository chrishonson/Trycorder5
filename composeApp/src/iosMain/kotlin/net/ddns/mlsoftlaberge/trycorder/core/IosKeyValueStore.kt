package net.ddns.mlsoftlaberge.trycorder.core

import platform.Foundation.NSUserDefaults

class IosKeyValueStore(private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults) : KeyValueStore {
    override fun getBoolean(key: String, default: Boolean) =
        if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)

    override fun getString(key: String, default: String) = defaults.stringForKey(key) ?: default

    override fun getInt(key: String, default: Int) =
        if (defaults.objectForKey(key) == null) default else defaults.integerForKey(key).toInt()

    override fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, key)
    override fun putString(key: String, value: String) = defaults.setObject(value, key)
    override fun putInt(key: String, value: Int) = defaults.setInteger(value.toLong(), key)
}
