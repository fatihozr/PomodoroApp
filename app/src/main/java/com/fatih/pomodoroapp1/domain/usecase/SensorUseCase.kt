package com.fatih.pomodoroapp1.domain.usecase

import com.fatih.pomodoroapp1.domain.repository.SensorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ObserveShakeEventsUseCase @Inject constructor(
    private val repository: SensorRepository
) {
    operator fun invoke(): Flow<Result<Unit>> = flow {
        try {
            repository.observeShakeEvents().collect { event ->
                emit(Result.success(event))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

class ObservePhoneOrientationUseCase @Inject constructor(
    private val repository: SensorRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.observePhoneOrientation()
}