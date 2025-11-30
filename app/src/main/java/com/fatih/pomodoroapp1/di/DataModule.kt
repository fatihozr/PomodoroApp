package com.fatih.pomodoroapp1.di

import android.content.Context
import android.content.SharedPreferences
import com.fatih.pomodoroapp1.data.cache.HistoricalEventCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences(
            "pomodoro_preferences",
            Context.MODE_PRIVATE
        )
    }

    @Provides
    @Singleton
    fun provideHistoricalEventCache(
        sharedPreferences: SharedPreferences
    ): HistoricalEventCache {
        return HistoricalEventCache(sharedPreferences)
    }
}