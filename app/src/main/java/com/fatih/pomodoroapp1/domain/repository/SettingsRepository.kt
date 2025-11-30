package com.fatih.pomodoroapp1.domain.repository

import com.fatih.pomodoroapp1.domain.model.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<Settings>
    suspend fun saveSettings(settings: Settings): Result<Unit>
    suspend fun getCurrentSettings(): Result<Settings>
}