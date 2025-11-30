package com.fatih.pomodoroapp1.di

import com.fatih.pomodoroapp1.data.repository.SensorRepositoryImpl
import com.fatih.pomodoroapp1.data.repository.SettingsRepositoryImpl
import com.fatih.pomodoroapp1.data.repository.StatisticsRepositoryImpl
import com.fatih.pomodoroapp1.data.repository.WikipediaRepositoryImpl
import com.fatih.pomodoroapp1.domain.repository.SensorRepository
import com.fatih.pomodoroapp1.domain.repository.SettingsRepository
import com.fatih.pomodoroapp1.domain.repository.StatisticsRepository
import com.fatih.pomodoroapp1.domain.repository.WikipediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindStatisticsRepository(
        impl: StatisticsRepositoryImpl
    ): StatisticsRepository

    @Binds
    @Singleton
    abstract fun bindSensorRepository(
        impl: SensorRepositoryImpl
    ): SensorRepository

    @Binds
    @Singleton
    abstract fun bindWikipediaRepository(
        impl: WikipediaRepositoryImpl
    ): WikipediaRepository
}