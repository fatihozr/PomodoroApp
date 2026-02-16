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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import java.time.DayOfWeek
import java.time.Month

@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val prefs: SharedPreferences,
    private val wikipediaRepository: WikipediaRepository
) : StatisticsRepository {

    companion object {
        private const val KEY_WORK_DAYS = "work_days_data"
        private const val KEY_WEEKLY_GOAL = "weekly_goal"
        private const val KEY_MONTHLY_GOAL = "monthly_goal"
        private const val KEY_YEARLY_GOAL = "yearly_goal"
    }

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun getStatistics(period: StatisticsPeriod): Flow<Statistics> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_WORK_DAYS || key == getGoalKey(period)) {
                trySend(calculateStatistics(period))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(calculateStatistics(period))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun getDayData(date: LocalDate): Result<DayData?> = runCatching {
        getAllWorkDaysMap()[date]
    }

    override fun getAllWorkDays(): Flow<Map<LocalDate, DayData>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_WORK_DAYS) {
                trySend(getAllWorkDaysMap())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getAllWorkDaysMap())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun getHistoricalEvents(date: LocalDate): Result<List<HistoricalEvent>> {
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

    override suspend fun updatePeriodGoal(period: StatisticsPeriod, goalPomodoros: Int): Result<Unit> = runCatching {
        require(goalPomodoros > 0) { "Hedef 0'dan büyük olmalıdır" }

        val key = getGoalKey(period)
        prefs.edit {
            putInt(key, goalPomodoros)
        }
        println("✅ Hedef kaydedildi: $period = $goalPomodoros (key=$key)")
    }

    private fun getGoalKey(period: StatisticsPeriod): String = when (period) {
        StatisticsPeriod.WEEKLY -> KEY_WEEKLY_GOAL
        StatisticsPeriod.MONTHLY -> KEY_MONTHLY_GOAL
        StatisticsPeriod.YEARLY -> KEY_YEARLY_GOAL
    }

    private fun getGoalPomodoros(period: StatisticsPeriod): Int {
        val key = getGoalKey(period)
        val defaultValue = when (period) {
            StatisticsPeriod.WEEKLY -> 20
            StatisticsPeriod.MONTHLY -> 80
            StatisticsPeriod.YEARLY -> 1000
        }
        return prefs.getInt(key, defaultValue)
    }

    private fun getAllWorkDaysMap(): Map<LocalDate, DayData> {
        val json = prefs.getString(KEY_WORK_DAYS, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(json)
            obj.keys().asSequence().mapNotNull { key ->
                try {
                    val date = LocalDate.parse(key, dateFormatter)
                    val data = obj.getJSONObject(key)
                    date to DayData(
                        date = date,
                        totalHours = data.optInt("hours", 0),
                        totalMinutes = data.optInt("minutes", 0),
                        completedPomodoros = data.optInt("pomodoros", 0)
                    )
                } catch (e: Exception) {
                    println("⚠️ Tarih parse hatası: $key - ${e.message}")
                    null
                }
            }.toMap()
        } catch (e: Exception) {
            println("❌ WorkDays parse hatası: ${e.message}")
            emptyMap()
        }
    }

    private fun saveWorkDays(workDays: Map<LocalDate, DayData>) {
        try {
            val obj = JSONObject()
            workDays.forEach { (date, data) ->
                obj.put(date.format(dateFormatter), JSONObject().apply {
                    put("hours", data.totalHours)
                    put("minutes", data.totalMinutes)
                    put("pomodoros", data.completedPomodoros)
                })
            }
            prefs.edit { putString(KEY_WORK_DAYS, obj.toString()) }
        } catch (e: Exception) {
            println("❌ WorkDays kayıt hatası: ${e.message}")
        }
    }

    private fun calculateStatistics(period: StatisticsPeriod): Statistics {
        return try {
            val workDays = getAllWorkDaysMap()
            val today = LocalDate.now()

            println("🔍 calculateStatistics çağrıldı: period=$period, today=$today")
            println("🔍 Toplam workDays: ${workDays.size}")
            workDays.forEach { (date, data) ->
                println("  📅 $date: ${data.completedPomodoros} pomodoro, ${data.totalFocusMinutes} dk")
            }

            // ✅ FIX: Period'a göre doğru tarih aralığını filtrele
            val (periodData, periodStartDate) = when (period) {
                StatisticsPeriod.WEEKLY -> {
                    val weekStart = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                    println("📅 WEEKLY: $weekStart - $today")
                    workDays.filterKeys { !it.isBefore(weekStart) && !it.isAfter(today) } to weekStart
                }
                StatisticsPeriod.MONTHLY -> {
                    val monthStart = today.withDayOfMonth(1)
                    println("📅 MONTHLY: $monthStart - $today")
                    workDays.filterKeys { !it.isBefore(monthStart) && !it.isAfter(today) } to monthStart
                }
                StatisticsPeriod.YEARLY -> {
                    val yearStart = today.withDayOfYear(1)
                    println("📅 YEARLY: $yearStart - $today")
                    workDays.filterKeys { !it.isBefore(yearStart) && !it.isAfter(today) } to yearStart
                }
            }

            println("🔍 Period data size: ${periodData.size}")
            periodData.forEach { (date, data) ->
                println("  ✅ $date: ${data.completedPomodoros} pomodoro, ${data.totalFocusMinutes} dk")
            }

            // ✅ FIX: Period verisinden hesapla
            val totalPomodoros = periodData.values.sumOf { it.completedPomodoros }
            val totalMinutes = periodData.values.sumOf { it.totalFocusMinutes }
            val totalHours = totalMinutes / 60

            println("📊 HESAPLANAN: totalPomodoros=$totalPomodoros, totalMinutes=$totalMinutes, totalHours=$totalHours")

            // ✅ FIX: Ortalama günlük süre hesabı - period içindeki toplam gün sayısına böl
            val daysSincePeriodStart = java.time.temporal.ChronoUnit.DAYS.between(periodStartDate, today).toInt() + 1
            val avgDaily = if (daysSincePeriodStart > 0) totalMinutes / daysSincePeriodStart else 0

            println("📊 avgDaily: $avgDaily (totalMinutes=$totalMinutes / daysSincePeriodStart=$daysSincePeriodStart)")

            // Chart verileri (tüm workDays verisini kullan)
            val weeklyData = calculateWeeklyData(workDays, today)
            val monthlyData = calculateMonthlyData(workDays, today)
            val yearlyData = calculateYearlyData(workDays, today)

            // ✅ FIX: Aktivite dağılımı period verisinden hesapla
            val distribution = calculateActivityDistribution(totalPomodoros, totalMinutes)
            println("📊 distribution: $distribution")

            // Period'a göre hedef
            val goalPomodoros = getGoalPomodoros(period)

            Statistics(
                period = period,
                totalPomodoros = totalPomodoros,
                totalFocusHours = totalHours,
                averageDailyMinutes = avgDaily,
                weeklyFocusData = weeklyData,
                monthlyFocusData = monthlyData,
                yearlyFocusData = yearlyData,
                activityDistribution = distribution,
                periodGoalPomodoros = goalPomodoros,
                periodCompletedPomodoros = totalPomodoros
            )
        } catch (e: Exception) {
            println("❌ İstatistik hesaplama hatası: ${e.message}")
            e.printStackTrace()
            Statistics(period = period)
        }
    }

    // Son 7 gün (tüm günler dahil)
    private fun calculateWeeklyData(workDays: Map<LocalDate, DayData>, today: LocalDate): List<Int> {
        val data = mutableListOf<Int>()

        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            data.add(workDays[date]?.totalFocusMinutes ?: 0)
        }

        return data
    }

    // Son 12 ay (Ocak-Aralık, bugünün ayından geriye)
    private fun calculateMonthlyData(workDays: Map<LocalDate, DayData>, today: LocalDate): List<Int> {
        val data = MutableList(12) { 0 }
        val currentMonth = YearMonth.from(today)

        for (i in 0 until 12) {
            val targetMonth = currentMonth.minusMonths(11 - i.toLong())
            val monthStart = targetMonth.atDay(1)
            val monthEnd = targetMonth.atEndOfMonth()

            val monthlyMinutes = workDays.filterKeys {
                !it.isBefore(monthStart) && !it.isAfter(monthEnd)
            }.values.sumOf { it.totalFocusMinutes }

            data[i] = monthlyMinutes
        }

        return data
    }

    // Son 5 yıl
    private fun calculateYearlyData(workDays: Map<LocalDate, DayData>, today: LocalDate): List<Int> {
        val data = mutableListOf<Int>()
        val currentYear = today.year

        for (i in 4 downTo 0) {
            val targetYear = currentYear - i
            val yearStart = LocalDate.of(targetYear, 1, 1)
            val yearEnd = LocalDate.of(targetYear, 12, 31)

            val yearlyMinutes = workDays.filterKeys {
                !it.isBefore(yearStart) && !it.isAfter(yearEnd)
            }.values.sumOf { it.totalFocusMinutes }

            data.add(yearlyMinutes)
        }

        return data
    }

    private fun calculateActivityDistribution(totalPomodoros: Int, totalMinutes: Int): Map<String, Int> {
        return if (totalMinutes > 0) {
            val estimatedBreakMinutes = (totalPomodoros * 5)
            val totalWithBreaks = totalMinutes + estimatedBreakMinutes

            val workPct = ((totalMinutes.toFloat() / totalWithBreaks) * 100).toInt()
            val breakPct = ((estimatedBreakMinutes.toFloat() / totalWithBreaks) * 100).toInt()
            val otherPct = 100 - workPct - breakPct

            mapOf("Çalışma" to workPct, "Mola" to breakPct, "Diğer" to otherPct)
        } else {
            mapOf("Çalışma" to 0, "Mola" to 0, "Diğer" to 100)
        }
    }
}