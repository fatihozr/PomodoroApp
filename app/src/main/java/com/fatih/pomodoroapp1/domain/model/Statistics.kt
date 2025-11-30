package com.fatih.pomodoroapp1.domain.model

data class Statistics(
    val period: StatisticsPeriod,
    val totalPomodoros: Int = 0,
    val totalFocusHours: Int = 0,
    val averageDailyMinutes: Int = 0,
    val weeklyFocusData: List<Int> = emptyList(),
    val activityDistribution: Map<String, Int> = emptyMap(),
    val weeklyGoalPomodoros: Int = 20,
    val weeklyCompletedPomodoros: Int = 0
) {
    val goalProgress: Float
        get() = if (weeklyGoalPomodoros > 0) {
            weeklyCompletedPomodoros.toFloat() / weeklyGoalPomodoros.toFloat()
        } else 0f
}

enum class StatisticsPeriod(val displayName: String) {
    WEEKLY("Haftalık"),
    MONTHLY("Aylık"),
    YEARLY("Yıllık")
}