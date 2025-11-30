package com.fatih.pomodoroapp1.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatih.pomodoroapp1.domain.model.Settings
import com.fatih.pomodoroapp1.domain.usecase.ObserveSettingsUseCase
import com.fatih.pomodoroapp1.domain.usecase.UpdateSettingsUseCase
import com.fatih.pomodoroapp1.ui.model.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            observeSettingsUseCase().collect { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun onSettingsChange(settings: Settings) {
        _uiState.update { it.copy(settings = settings, saveSuccess = false) }
    }

    fun onSaveClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveSuccess = false) }

            updateSettingsUseCase(_uiState.value.settings).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = false,
                            error = error.message ?: "Ayarlar kaydedilemedi"
                        )
                    }
                }
            )
        }
    }
}