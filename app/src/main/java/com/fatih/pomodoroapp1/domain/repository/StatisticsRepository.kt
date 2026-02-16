package com.fatih.pomodoroapp1.domain.repository

import com.fatih.pomodoroapp1.domain.model.DayData
import com.fatih.pomodoroapp1.domain.model.HistoricalEvent
import com.fatih.pomodoroapp1.domain.model.Statistics
import com.fatih.pomodoroapp1.domain.model.StatisticsPeriod
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface StatisticsRepository {
    fun getStatistics(period: StatisticsPeriod): Flow<Statistics>

    suspend fun getDayData(date: LocalDate): Result<DayData?>

    fun getAllWorkDays(): Flow<Map<LocalDate, DayData>>

    suspend fun getHistoricalEvents(date: LocalDate): Result<List<HistoricalEvent>>

    suspend fun savePomodoroSession(date: LocalDate, durationMinutes: Int): Result<Unit>

    suspend fun updatePeriodGoal(period: StatisticsPeriod, goalPomodoros: Int): Result<Unit>
}