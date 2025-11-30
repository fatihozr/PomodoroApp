package com.fatih.pomodoroapp1.domain.usecase

import com.fatih.pomodoroapp1.domain.model.TimerPhase
import com.fatih.pomodoroapp1.domain.model.TimerState
import com.fatih.pomodoroapp1.domain.repository.SettingsRepository
import com.fatih.pomodoroapp1.domain.repository.StatisticsRepository
import java.time.LocalDate
import javax.inject.Inject


@Deprecated("Use ObserveSettingsUseCase instead")
class StartTimerUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<TimerState> {
        return settingsRepository.getCurrentSettings().map { settings ->
            TimerState(
                timeRemainingSeconds = settings.pomodoroMinutes * 60,
                totalTimeSeconds = settings.pomodoroMinutes * 60,
                isPaused = true,
                currentPomodoro = 1,
                totalPomodoros = settings.pomodoroCount,
                phase = TimerPhase.POMODORO
            )
        }
    }
}

class GetNextTimerPhaseUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(currentState: TimerState): Result<TimerState> {
        return settingsRepository.getCurrentSettings().map { settings ->
            when (currentState.phase) {
                TimerPhase.POMODORO -> {
                    val isLongBreak = currentState.currentPomodoro >= currentState.totalPomodoros
                    val breakMinutes = if (isLongBreak) settings.longBreakMinutes else settings.shortBreakMinutes
                    currentState.copy(
                        timeRemainingSeconds = breakMinutes * 60,
                        totalTimeSeconds = breakMinutes * 60,
                        isPaused = true,
                        phase = if (isLongBreak) TimerPhase.LONG_BREAK else TimerPhase.SHORT_BREAK
                    )
                }
                TimerPhase.SHORT_BREAK, TimerPhase.LONG_BREAK -> {
                    val nextPomodoro = if (currentState.phase == TimerPhase.LONG_BREAK) 1
                    else currentState.currentPomodoro + 1
                    currentState.copy(
                        timeRemainingSeconds = settings.pomodoroMinutes * 60,
                        totalTimeSeconds = settings.pomodoroMinutes * 60,
                        isPaused = true,
                        currentPomodoro = nextPomodoro,
                        phase = TimerPhase.POMODORO
                    )
                }
            }
        }
    }
}

class SaveCompletedPomodoroUseCase @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(date: LocalDate = LocalDate.now()): Result<Unit> {
        return try {
            // Önce settings'i al
            val settings = settingsRepository.getCurrentSettings().getOrThrow()

            // Sonra pomodoro'yu kaydet
            val saveResult = statisticsRepository.savePomodoroSession(
                date = date,
                durationMinutes = settings.pomodoroMinutes
            )

            // Result'ı kontrol et ve hata varsa fırlat
            saveResult.getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}