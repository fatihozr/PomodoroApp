package com.fatih.pomodoroapp1.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitBuilder {

    private const val BASE_URL = "https://api.wikimedia.org/feed/v1/wikipedia/tr/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // User-Agent interceptor - Wikipedia için zorunlu
    private val userAgentInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", "PomodoroApp/1.0 (Android; ${System.getProperty("http.agent")})")
            .header("Accept", "application/json")
            .build()
        chain.proceed(requestWithUserAgent)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(userAgentInterceptor)      // User-Agent ekle
        .addInterceptor(loggingInterceptor)
        .retryOnConnectionFailure(true)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val wikipediaApi: WikipediaApiService = retrofit.create(WikipediaApiService::class.java)
}