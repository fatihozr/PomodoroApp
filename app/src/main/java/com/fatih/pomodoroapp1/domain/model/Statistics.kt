package com.fatih.pomodoroapp1.domain.model

data class Statistics(
    val period: StatisticsPeriod,
    val totalPomodoros: Int = 0,
    val totalFocusHours: Int = 0,
    val averageDailyMinutes: Int = 0,

    // Haftalık veri (5 gün - Pazartesi'den Cuma'ya)
    val weeklyFocusData: List<Int> = emptyList(),

    // Aylık veri (12 ay - Ocak'tan Aralık'a)
    val monthlyFocusData: List<Int> = emptyList(),

    // Yıllık veri (son N yıl)
    val yearlyFocusData: List<Int> = emptyList(),

    val activityDistribution: Map<String, Int> = emptyMap(),

    // Period'a göre hedef ve tamamlanan
    val periodGoalPomodoros: Int = 20,
    val periodCompletedPomodoros: Int = 0
) {
    val goalProgress: Float
        get() = if (periodGoalPomodoros > 0) {
            periodCompletedPomodoros.toFloat() / periodGoalPomodoros.toFloat()
        } else 0f
}

enum class StatisticsPeriod(val displayName: String) {
    WEEKLY("Haftalık"),
    MONTHLY("Aylık"),
    YEARLY("Yıllık")
}