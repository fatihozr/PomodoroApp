package com.fatih.pomodoroapp1.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatih.pomodoroapp1.domain.model.StatisticsPeriod
import com.fatih.pomodoroapp1.domain.usecase.ObserveStatisticsUseCase
import com.fatih.pomodoroapp1.ui.model.StatisticsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val observeStatisticsUseCase: ObserveStatisticsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics(StatisticsPeriod.WEEKLY)
    }

    fun onPeriodChange(period: StatisticsPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadStatistics(period)
    }

    private fun loadStatistics(period: StatisticsPeriod) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            observeStatisticsUseCase(period).collect { statistics ->
                _uiState.update {
                    it.copy(
                        statistics = statistics,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }
}