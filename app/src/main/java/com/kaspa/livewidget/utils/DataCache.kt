package com.kaspa.livewidget.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.kaspa.livewidget.data.KaspaNetworkData

/**
 * Cache manager for storing and retrieving Kaspa network data
 * Uses SharedPreferences for simple caching
 */
class DataCache(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        CACHE_PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    /**
     * Save network data to cache
     */
    fun saveData(data: KaspaNetworkData) {
        prefs.edit().apply {
            putString(KEY_CACHED_DATA, gson.toJson(data))
            apply()
        }
    }

    /**
     * Get cached network data
     */
    fun getCachedData(): KaspaNetworkData? {
        val json = prefs.getString(KEY_CACHED_DATA, null) ?: return null
        return try {
            gson.fromJson(json, KaspaNetworkData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val CACHE_PREFS_NAME = "kaspa_widget_cache"
        private const val KEY_CACHED_DATA = "cached_data"
    }
}
