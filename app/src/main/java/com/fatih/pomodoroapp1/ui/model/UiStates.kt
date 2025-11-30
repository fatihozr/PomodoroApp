package com.fatih.pomodoroapp1.ui.model

import com.fatih.pomodoroapp1.domain.model.DayData
import com.fatih.pomodoroapp1.domain.model.HistoricalEvent
import com.fatih.pomodoroapp1.domain.model.Settings
import com.fatih.pomodoroapp1.domain.model.Statistics
import com.fatih.pomodoroapp1.domain.model.StatisticsPeriod
import com.fatih.pomodoroapp1.domain.model.TimerPhase
import java.time.LocalDate
import java.time.YearMonth

data class TimerUiState(
    val timeRemainingSeconds: Int = 25 * 60,
    val totalTimeSeconds: Int = 25 * 60,
    val isPaused: Boolean = true,
    val currentPomodoro: Int = 1,
    val totalPomodoros: Int = 4,
    val phase: TimerPhase = TimerPhase.POMODORO,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isShakeSensorEnabled: Boolean = false,
    val isProximitySensorEnabled: Boolean = false
) {
    val progress: Float
        get() = if (totalTimeSeconds > 0) {
            timeRemainingSeconds.toFloat() / totalTimeSeconds.toFloat()
        } else 0f

    val minutes: Int
        get() = timeRemainingSeconds / 60

    val seconds: Int
        get() = timeRemainingSeconds % 60

    val formattedTime: String
        get() = String.format("%02d:%02d", minutes, seconds)
}

data class SettingsUiState(
    val settings: Settings = Settings(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = LocalDate.now(),
    val workDays: Map<LocalDate, DayData> = emptyMap(),
    val historicalEvents: List<HistoricalEvent> = emptyList(),
    val carouselPage: Int = 1,
    val totalCarouselPages: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class StatisticsUiState(
    val selectedPeriod: StatisticsPeriod = StatisticsPeriod.WEEKLY,
    val statistics: Statistics = Statistics(StatisticsPeriod.WEEKLY),
    val isLoading: Boolean = false,
    val error: String? = null
)