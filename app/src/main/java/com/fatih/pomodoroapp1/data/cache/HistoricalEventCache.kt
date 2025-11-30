package com.fatih.pomodoroapp1.data.cache

import android.content.SharedPreferences
import androidx.core.content.edit
import com.fatih.pomodoroapp1.domain.model.HistoricalEvent
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoricalEventCache @Inject constructor(
    private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_CACHE_PREFIX = "historical_events_"
        private const val KEY_LAST_FETCH_PREFIX = "last_fetch_"
        private val CACHE_VALIDITY_MILLIS = TimeUnit.DAYS.toMillis(7)
    }

    fun isCacheValid(date: LocalDate): Boolean {
        val lastFetchMillis = prefs.getLong(getLastFetchKey(date), 0L)

        return lastFetchMillis != 0L &&
                (System.currentTimeMillis() - lastFetchMillis) < CACHE_VALIDITY_MILLIS
    }

    fun getCachedEvents(date: LocalDate): List<HistoricalEvent>? {
        if (!isCacheValid(date)) return null

        val jsonString = prefs.getString(getCacheKey(date), null) ?: return null

        return try {
            parseEvents(jsonString)
        } catch (e: Exception) {
            null
        }
    }

    fun cacheEvents(date: LocalDate, events: List<HistoricalEvent>) {
        val jsonArray = JSONArray().apply {
            events.forEach { event ->
                put(JSONObject().apply {
                    put("year", event.year)
                    put("description", event.description)
                })
            }
        }

        prefs.edit {
            putString(getCacheKey(date), jsonArray.toString())
            putLong(getLastFetchKey(date), System.currentTimeMillis())
        }
    }

    fun clearCache(date: LocalDate) {
        prefs.edit {
            remove(getCacheKey(date))
            remove(getLastFetchKey(date))
        }
    }

    fun clearAllCache() {
        prefs.edit {
            prefs.all.keys
                .filter { it.startsWith(KEY_CACHE_PREFIX) || it.startsWith(KEY_LAST_FETCH_PREFIX) }
                .forEach { remove(it) }
        }
    }

    private fun getCacheKey(date: LocalDate) =
        "$KEY_CACHE_PREFIX${date.monthValue}_${date.dayOfMonth}"

    private fun getLastFetchKey(date: LocalDate) =
        "$KEY_LAST_FETCH_PREFIX${date.monthValue}_${date.dayOfMonth}"

    private fun parseEvents(jsonString: String): List<HistoricalEvent> {
        val jsonArray = JSONArray(jsonString)
        return List(jsonArray.length()) { i ->
            val obj = jsonArray.getJSONObject(i)
            HistoricalEvent(
                year = obj.getInt("year"),
                description = obj.getString("description")
            )
        }
    }
}