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
            putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
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

    /**
     * Check if cached data is still valid
     */
    fun isCacheValid(): Boolean {
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastUpdate) < CACHE_VALIDITY_MS
    }

    /**
     * Get data from cache if valid, otherwise return null
     */
    fun getValidCachedData(): KaspaNetworkData? {
        return if (isCacheValid()) getCachedData() else null
    }

    companion object {
        private const val CACHE_PREFS_NAME = "kaspa_widget_cache"
        private const val KEY_CACHED_DATA = "cached_data"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val CACHE_VALIDITY_MS = 15 * 60 * 1000L // 15 minutes
    }
}
