package com.fatih.pomodoroapp1.data.remote

import com.fatih.pomodoroapp1.data.remote.model.OnThisDayResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface WikipediaApiService {

    /**
     * Wikimedia Feed API - Tarihte Bugün
     * @param month Ay (01-12)
     * @param day Gün (01-31)
     * @return Günün tarihi olayları
     */
    @GET("onthisday/all/{month}/{day}")
    suspend fun getTodayInHistory(
        @Path("month") month: String,
        @Path("day") day: String
    ): Response<OnThisDayResponse>
}