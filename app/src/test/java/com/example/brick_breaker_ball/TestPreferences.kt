package com.example.brick_breaker_ball

import com.badlogic.gdx.Preferences

class TestPreferences : Preferences {
    private val values = linkedMapOf<String, Any>()
    override fun putBoolean(key: String, value: Boolean) = apply { values[key] = value }
    override fun putInteger(key: String, value: Int) = apply { values[key] = value }
    override fun putLong(key: String, value: Long) = apply { values[key] = value }
    override fun putFloat(key: String, value: Float) = apply { values[key] = value }
    override fun putString(key: String, value: String) = apply { values[key] = value }
    override fun put(vals: MutableMap<String, *>?) = apply { vals?.forEach { (key, value) -> if (value != null) values[key] = value } }
    override fun getBoolean(key: String) = getBoolean(key, false)
    override fun getInteger(key: String) = getInteger(key, 0)
    override fun getLong(key: String) = getLong(key, 0L)
    override fun getFloat(key: String) = getFloat(key, 0f)
    override fun getString(key: String) = getString(key, "")
    override fun getBoolean(key: String, defValue: Boolean) = values[key] as? Boolean ?: defValue
    override fun getInteger(key: String, defValue: Int) = values[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long) = values[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float) = values[key] as? Float ?: defValue
    override fun getString(key: String, defValue: String) = values[key] as? String ?: defValue
    override fun get(): MutableMap<String, *> = LinkedHashMap(values)
    override fun contains(key: String) = key in values
    override fun clear() = values.clear()
    override fun remove(key: String) { values.remove(key) }
    override fun flush() = Unit
}
