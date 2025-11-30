package com.fatih.pomodoroapp1.data.repository

import android.content.SharedPreferences
import com.fatih.pomodoroapp1.domain.model.*
import com.fatih.pomodoroapp1.domain.repository.StatisticsRepository
import com.fatih.pomodoroapp1.domain.repository.WikipediaRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import java.time.DayOfWeek

@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val prefs: SharedPreferences,
    private val wikipediaRepository: WikipediaRepository // Wikipedia repo eklendi
) : StatisticsRepository {

    companion object {
        private const val KEY_WORK_DAYS = "work_days_data"
    }

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun getStatistics(period: StatisticsPeriod): Flow<Statistics> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(calculateStatistics(period))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(calculateStatistics(period))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun getDayData(date: LocalDate): Result<DayData?> = runCatching {
        getAllWorkDaysMap()[date]
    }

    override fun getAllWorkDays(): Flow<Map<LocalDate, DayData>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(getAllWorkDaysMap())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getAllWorkDaysMap())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun getHistoricalEvents(date: LocalDate): Result<List<HistoricalEvent>> {
        // Wikipedia'dan gerçek verileri çek
        return wikipediaRepository.getTodayInHistory()
    }

    override suspend fun savePomodoroSession(date: LocalDate, durationMinutes: Int): Result<Unit> = runCatching {
        val workDays = getAllWorkDaysMap().toMutableMap()
        val existing = workDays[date] ?: DayData(date)

        val totalMinutes = existing.totalFocusMinutes + durationMinutes
        val updated = existing.copy(
            totalHours = totalMinutes / 60,
            totalMinutes = totalMinutes % 60,
            completedPomodoros = existing.completedPomodoros + 1
        )
        workDays[date] = updated
        saveWorkDays(workDays)
    }

    private fun getAllWorkDaysMap(): Map<LocalDate, DayData> {
        val json = prefs.getString(KEY_WORK_DAYS, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(json)
            obj.keys().asSequence().associate { key ->
                val date = LocalDate.parse(key, dateFormatter)
                val data = obj.getJSONObject(key)
                date to DayData(
                    date = date,
                    totalHours = data.getInt("hours"),
                    totalMinutes = data.getInt("minutes"),
                    completedPomodoros = data.getInt("pomodoros")
                )
            }
        } catch (e: Exception) { emptyMap() }
    }

    private fun saveWorkDays(workDays: Map<LocalDate, DayData>) {
        val obj = JSONObject()
        workDays.forEach { (date, data) ->
            obj.put(date.format(dateFormatter), JSONObject().apply {
                put("hours", data.totalHours)
                put("minutes", data.totalMinutes)
                put("pomodoros", data.completedPomodoros)
            })
        }
        prefs.edit { putString(KEY_WORK_DAYS, obj.toString()) }
    }

    private fun calculateStatistics(period: StatisticsPeriod): Statistics {
        val workDays = getAllWorkDaysMap()
        val today = LocalDate.now()

        val filtered = workDays.filterKeys { date ->
            when (period) {
                StatisticsPeriod.WEEKLY -> date.isAfter(today.minusWeeks(1)) || date.isEqual(today)
                StatisticsPeriod.MONTHLY -> date.isAfter(today.minusMonths(1)) || date.isEqual(today)
                StatisticsPeriod.YEARLY -> date.isAfter(today.minusYears(1)) || date.isEqual(today)
            }
        }

        val totalPomodoros = filtered.values.sumOf { it.completedPomodoros }
        val totalMinutes = filtered.values.sumOf { it.totalFocusMinutes }
        val avgDaily = if (filtered.isNotEmpty()) totalMinutes / filtered.size else 0

        // Son 5 iş günü için veri (Pazartesi-Cuma)
        val weeklyData = mutableListOf<Int>()
        var currentDate = today
        var daysAdded = 0

        while (daysAdded < 5) {
            // Sadece hafta içi günleri al
            if (currentDate.dayOfWeek != DayOfWeek.SATURDAY && currentDate.dayOfWeek != DayOfWeek.SUNDAY) {
                weeklyData.add(0, filtered[currentDate]?.totalFocusMinutes ?: 0)
                daysAdded++
            }
            currentDate = currentDate.minusDays(1)
        }

        // Bu haftanın başlangıcı (Pazartesi)
        val weekStart = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
        val weeklyCompleted = workDays.filterKeys {
            !it.isBefore(weekStart) && !it.isAfter(today)
        }.values.sumOf { it.completedPomodoros }

        // Aktivite dağılımı hesapla (gerçek verilerle)
        val totalWorkMinutes = filtered.values.sumOf { it.totalFocusMinutes }
        val workPercentage = if (totalWorkMinutes > 0) {
            // Mola süresini hesapla (kısa ve uzun molalar)
            val estimatedBreakMinutes = (totalPomodoros * 5) // Basit bir hesaplama
            val totalMinutesWithBreaks = totalWorkMinutes + estimatedBreakMinutes

            val workPct = (totalWorkMinutes.toFloat() / totalMinutesWithBreaks * 100).toInt()
            val breakPct = (estimatedBreakMinutes.toFloat() / totalMinutesWithBreaks * 100).toInt()
            val otherPct = 100 - workPct - breakPct

            mapOf("Çalışma" to workPct, "Mola" to breakPct, "Diğer" to otherPct)
        } else {
            mapOf("Çalışma" to 0, "Mola" to 0, "Diğer" to 100)
        }

        return Statistics(
            period = period,
            totalPomodoros = totalPomodoros,
            totalFocusHours = totalMinutes / 60,
            averageDailyMinutes = avgDaily,
            weeklyFocusData = weeklyData,
            activityDistribution = workPercentage,
            weeklyGoalPomodoros = 20,
            weeklyCompletedPomodoros = weeklyCompleted
        )
    }
}