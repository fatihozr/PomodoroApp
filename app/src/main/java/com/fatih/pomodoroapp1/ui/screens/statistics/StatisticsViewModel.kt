package com.fatih.pomodoroapp1.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatih.pomodoroapp1.domain.model.StatisticsPeriod
import com.fatih.pomodoroapp1.domain.usecase.ObserveStatisticsUseCase
import com.fatih.pomodoroapp1.domain.usecase.UpdatePeriodGoalUseCase
import com.fatih.pomodoroapp1.ui.model.StatisticsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val observeStatisticsUseCase: ObserveStatisticsUseCase,
    private val updatePeriodGoalUseCase: UpdatePeriodGoalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var statisticsJob: Job? = null

    init {
        loadStatistics(StatisticsPeriod.WEEKLY)
    }

    fun onPeriodChange(period: StatisticsPeriod) {
        // ✅ Önce state'i güncelle
        _uiState.update { it.copy(selectedPeriod = period, isLoading = true) }

        // ✅ Sonra yeni period için statistics yükle
        loadStatistics(period)
    }

    fun onGoalUpdate(newGoal: Int) {
        val currentPeriod = _uiState.value.selectedPeriod

        viewModelScope.launch {
            try {
                updatePeriodGoalUseCase(currentPeriod, newGoal).fold(
                    onSuccess = {
                        println("✅ Hedef güncellendi: $currentPeriod = $newGoal")
                        // Statistics flow otomatik güncellenecek
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(error = "Hedef güncellenemedi: ${error.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Beklenmeyen hata: ${e.message}")
                }
            }
        }
    }

    private fun loadStatistics(period: StatisticsPeriod) {
        // ✅ Önceki job'ı iptal et
        statisticsJob?.cancel()

        statisticsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // ✅ Her period için yeni flow collect et
                observeStatisticsUseCase(period).collect { statistics ->
                    println("📊 Statistics güncellendi: period=$period, pomodoros=${statistics.totalPomodoros}, hours=${statistics.totalFocusHours}")

                    _uiState.update {
                        it.copy(
                            statistics = statistics,
                            selectedPeriod = period,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                println("❌ Statistics yükleme hatası: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "İstatistikler yüklenemedi: ${e.message}"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        statisticsJob?.cancel()
    }
}