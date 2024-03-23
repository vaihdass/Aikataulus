package ru.vaihdass.aikataulus.data.local.pref

interface SharedPreferencesManager {
    fun getString(key: String, defaultValue: String): String?
    fun putString(key: String, value: String)

    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)

    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)

    fun getLong(key: String, defaultValue: Long): Long
    fun putLong(key: String, value: Long)

    fun getFloat(key: String, defaultValue: Float): Float
    fun putFloat(key: String, value: Float)

    fun remove(key: String)
    fun clear()
}