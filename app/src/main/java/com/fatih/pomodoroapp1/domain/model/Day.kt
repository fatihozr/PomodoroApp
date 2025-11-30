package com.fatih.pomodoroapp1.domain.model

import java.time.LocalDate

data class DayData(
    val date: LocalDate,
    val totalHours: Int = 0,
    val totalMinutes: Int = 0,
    val completedPomodoros: Int = 0
) {
    val totalFocusMinutes: Int
        get() = (totalHours * 60) + totalMinutes
}

data class HistoricalEvent(
    val year: Int,
    val description: String
)