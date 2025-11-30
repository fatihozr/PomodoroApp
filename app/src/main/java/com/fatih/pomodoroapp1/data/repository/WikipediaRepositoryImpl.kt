package com.fatih.pomodoroapp1.data.repository

import com.fatih.pomodoroapp1.data.cache.HistoricalEventCache
import com.fatih.pomodoroapp1.data.remote.RetrofitBuilder
import com.fatih.pomodoroapp1.domain.model.HistoricalEvent
import com.fatih.pomodoroapp1.domain.repository.WikipediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WikipediaRepositoryImpl @Inject constructor(
    private val cache: HistoricalEventCache
) : WikipediaRepository {

    private val api = RetrofitBuilder.wikipediaApi

    companion object {
        private const val API_TIMEOUT_MS = 15_000L
    }

    override suspend fun getTodayInHistory(): Result<List<HistoricalEvent>> = withContext(Dispatchers.IO) {
        runCatching {
            val today = LocalDate.now()

            cache.getCachedEvents(today)?.let { cachedEvents ->
                return@withContext Result.success(cachedEvents.take(5))
            }

            fetchAndCacheEvents(today).take(5)
        }
    }

    override suspend fun getAllHistoricalEvents(): Result<List<HistoricalEvent>> = withContext(Dispatchers.IO) {
        runCatching {
            val today = LocalDate.now()
            cache.getCachedEvents(today) ?: fetchAndCacheEvents(today)
        }
    }

    override suspend fun refreshCache(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val today = LocalDate.now()

            withTimeout(API_TIMEOUT_MS) {
                cache.clearCache(today)
                fetchAndCacheEvents(today)
            }
            Unit
        }.onFailure { error ->
            when (error) {
                is kotlinx.coroutines.TimeoutCancellationException ->
                    throw Exception("Bağlantı zaman aşımına uğradı")
                else -> throw error
            }
        }
    }

    private suspend fun fetchAndCacheEvents(date: LocalDate): List<HistoricalEvent> {
        val month = date.monthValue.toString().padStart(2, '0')
        val day = date.dayOfMonth.toString().padStart(2, '0')

        val response = api.getTodayInHistory(month, day)

        if (!response.isSuccessful) {
            val errorMessage = when (response.code()) {
                403 -> "Erişim engellendi. Lütfen internet bağlantınızı kontrol edin."
                404 -> "Tarihsel veri bulunamadı"
                429 -> "Çok fazla istek gönderildi. Lütfen bekleyin."
                500, 502, 503 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                else -> "API hatası: ${response.code()}"
            }
            throw Exception(errorMessage)
        }

        val data = response.body() ?: throw Exception("İçerik bulunamadı")

        val allEvents = mutableListOf<HistoricalEvent>()

        // 1. Selected (öne çıkan olaylar)
        data.selected?.forEach { item ->
            val year = item.year ?: 0
            val description = item.text ?: ""
            if (year > 0 && description.isNotEmpty()) {
                allEvents.add(HistoricalEvent(year, description))
            }
        }

        // 2. Events (diğer önemli olaylar)
        data.events?.forEach { item ->
            val year = item.year ?: 0
            val description = item.text ?: ""
            if (year > 0 && description.isNotEmpty()) {
                allEvents.add(HistoricalEvent(year, description))
            }
        }

        // 3. Births (doğumlar) - ilk 3
        data.births?.take(3)?.forEach { item ->
            val year = item.year ?: 0
            val name = item.text ?: ""
            if (year > 0 && name.isNotEmpty()) {
                allEvents.add(HistoricalEvent(year, "$name doğdu"))
            }
        }

        // 4. Deaths (ölümler) - ilk 3
        data.deaths?.take(3)?.forEach { item ->
            val year = item.year ?: 0
            val name = item.text ?: ""
            if (year > 0 && name.isNotEmpty()) {
                allEvents.add(HistoricalEvent(year, "$name öldü"))
            }
        }

        val sortedEvents = allEvents.sortedBy { it.year }
        cache.cacheEvents(date, sortedEvents)

        return sortedEvents
    }
}