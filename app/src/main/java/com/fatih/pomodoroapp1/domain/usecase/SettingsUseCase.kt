package com.fatih.pomodoroapp1.domain.usecase

import com.fatih.pomodoroapp1.domain.model.Settings
import com.fatih.pomodoroapp1.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Settings> = repository.getSettings()
}

class UpdateSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(settings: Settings): Result<Unit> {
        return if (settings.isValid()) {
            repository.saveSettings(settings)
        } else {
            Result.failure(IllegalArgumentException("Invalid settings values"))
        }
    }
}

class GetCurrentSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(): Result<Settings> = repository.getCurrentSettings()
}