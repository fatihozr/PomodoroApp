package com.fatih.pomodoroapp1.domain.repository

import com.fatih.pomodoroapp1.domain.model.HistoricalEvent

interface WikipediaRepository {
    /**
     * Wikipedia'dan günün tarihsel olaylarını çeker (ilk 5 olay)
     * Cache varsa cache'den döner
     */
    suspend fun getTodayInHistory(): Result<List<HistoricalEvent>>

    /**
     * Cache'deki TÜM olayları döndürür (carousel için)
     */
    suspend fun getAllHistoricalEvents(): Result<List<HistoricalEvent>>

    /**
     * Cache'i yeniler (force refresh)
     */
    suspend fun refreshCache(): Result<Unit>
}