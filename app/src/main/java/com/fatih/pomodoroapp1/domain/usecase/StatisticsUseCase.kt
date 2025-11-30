package com.fatih.pomodoroapp1.domain.usecase

import com.fatih.pomodoroapp1.domain.model.*
import com.fatih.pomodoroapp1.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject


class ObserveStatisticsUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    operator fun invoke(period: StatisticsPeriod): Flow<Statistics> {
        return repository.getStatistics(period)
    }
}

class ObserveWorkDaysUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    operator fun invoke(): Flow<Map<LocalDate, DayData>> {
        return repository.getAllWorkDays()
    }
}

class GetHistoricalEventsUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    suspend operator fun invoke(date: LocalDate): Result<List<HistoricalEvent>> {
        return repository.getHistoricalEvents(date)
    }
}

