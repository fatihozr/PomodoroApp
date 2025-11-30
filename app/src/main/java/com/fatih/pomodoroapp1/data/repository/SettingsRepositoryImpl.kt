package com.fatih.pomodoroapp1.data.repository

import android.content.SharedPreferences
import com.fatih.pomodoroapp1.domain.model.Settings
import com.fatih.pomodoroapp1.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val prefs: SharedPreferences
) : SettingsRepository {

    companion object {
        private const val KEY_POMODORO_MINUTES = "pomodoro_minutes"
        private const val KEY_SHORT_BREAK = "short_break_minutes"
        private const val KEY_LONG_BREAK = "long_break_minutes"
        private const val KEY_POMODORO_COUNT = "pomodoro_count"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
    }

    override fun getSettings(): Flow<Settings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(readSettings())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(readSettings())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun saveSettings(settings: Settings): Result<Unit> = runCatching {
        prefs.edit {
            putInt(KEY_POMODORO_MINUTES, settings.pomodoroMinutes)
            putInt(KEY_SHORT_BREAK, settings.shortBreakMinutes)
            putInt(KEY_LONG_BREAK, settings.longBreakMinutes)
            putInt(KEY_POMODORO_COUNT, settings.pomodoroCount)
            putBoolean(KEY_NOTIFICATIONS, settings.notificationsEnabled)
        }
    }

    override suspend fun getCurrentSettings(): Result<Settings> = runCatching {
        readSettings()
    }

    private fun readSettings(): Settings = Settings(
        pomodoroMinutes = prefs.getInt(KEY_POMODORO_MINUTES, 25),
        shortBreakMinutes = prefs.getInt(KEY_SHORT_BREAK, 5),
        longBreakMinutes = prefs.getInt(KEY_LONG_BREAK, 15),
        pomodoroCount = prefs.getInt(KEY_POMODORO_COUNT, 4),
        notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, false)
    )
}