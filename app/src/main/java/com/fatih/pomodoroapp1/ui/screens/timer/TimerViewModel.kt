package com.fatih.pomodoroapp1.ui.screens.timer

import kotlinx.coroutines.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatih.pomodoroapp1.domain.model.TimerPhase
import com.fatih.pomodoroapp1.domain.usecase.*
import com.fatih.pomodoroapp1.ui.model.TimerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val getNextTimerPhaseUseCase: GetNextTimerPhaseUseCase,
    private val saveCompletedPomodoroUseCase: SaveCompletedPomodoroUseCase,
    private val observeShakeEventsUseCase: ObserveShakeEventsUseCase,
    private val observePhoneOrientationUseCase: ObservePhoneOrientationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var shakeJob: Job? = null
    private var proximityCheckJob: Job? = null

    private var timerStartTime = 0L
    private var latestPhoneOrientation: Boolean = false
    private val PROXIMITY_CHECK_INTERVAL = 3000L // 3 saniye
    private val INITIAL_CHECK_DURATION = 5000L // İlk 5 saniye
    private val INITIAL_POLL_INTERVAL = 1000L // İlk 5 saniyede 1 saniyede bir

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            observeSettingsUseCase().collect { settings ->
                val currentState = _uiState.value

                if (currentState.isPaused && currentState.phase == TimerPhase.POMODORO) {
                    val pomodoroSeconds = settings.pomodoroMinutes * 60
                    _uiState.update {
                        it.copy(
                            timeRemainingSeconds = pomodoroSeconds,
                            totalTimeSeconds = pomodoroSeconds,
                            totalPomodoros = settings.pomodoroCount,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            totalPomodoros = settings.pomodoroCount,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun onPlayPauseClick() {
        val currentState = _uiState.value

        if (currentState.isPaused) {
            startTimer()
        } else {
            pauseTimer()
        }
    }

    private fun startTimer() {
        _uiState.update { it.copy(isPaused = false, error = null) }
        timerStartTime = System.currentTimeMillis()

        // Proximity sensör aktifse monitoring başlat
        if (_uiState.value.isProximitySensorEnabled) {
            startProximityMonitoring()
        }

        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemainingSeconds > 0 && !_uiState.value.isPaused) {
                delay(1000)
                _uiState.update {
                    it.copy(timeRemainingSeconds = it.timeRemainingSeconds - 1)
                }
            }

            if (_uiState.value.timeRemainingSeconds == 0) {
                onTimerComplete()
            }
        }
    }

    private fun pauseTimer() {
        _uiState.update { it.copy(isPaused = true) }
        timerJob?.cancel()
        stopProximityMonitoring()
    }

    private fun onTimerComplete() {
        val currentState = _uiState.value

        viewModelScope.launch {
            if (currentState.phase == TimerPhase.POMODORO) {
                saveCompletedPomodoroUseCase().fold(
                    onSuccess = { /* Başarılı */ },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(error = "Pomodoro kaydedilemedi: ${error.message}")
                        }
                    }
                )
            }

            moveToNextPhase()
        }
    }

    fun onSkipClick() {
        pauseTimer()
        viewModelScope.launch {
            moveToNextPhase()
        }
    }

    private suspend fun moveToNextPhase() {
        val currentState = _uiState.value

        val domainState = com.fatih.pomodoroapp1.domain.model.TimerState(
            timeRemainingSeconds = currentState.timeRemainingSeconds,
            totalTimeSeconds = currentState.totalTimeSeconds,
            isPaused = true,
            currentPomodoro = currentState.currentPomodoro,
            totalPomodoros = currentState.totalPomodoros,
            phase = currentState.phase
        )

        getNextTimerPhaseUseCase(domainState).fold(
            onSuccess = { nextState ->
                _uiState.update {
                    it.copy(
                        timeRemainingSeconds = nextState.timeRemainingSeconds,
                        totalTimeSeconds = nextState.totalTimeSeconds,
                        isPaused = true,
                        currentPomodoro = nextState.currentPomodoro,
                        phase = nextState.phase,
                        error = null
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(error = "Sonraki faza geçilemedi: ${error.message}")
                }
            }
        )
    }

    fun onRestartClick() {
        pauseTimer()

        viewModelScope.launch {
            try {
                var settingsReceived = false

                observeSettingsUseCase().collect { settings ->
                    if (!settingsReceived) {
                        settingsReceived = true
                        val pomodoroSeconds = settings.pomodoroMinutes * 60
                        _uiState.update {
                            TimerUiState(
                                timeRemainingSeconds = pomodoroSeconds,
                                totalTimeSeconds = pomodoroSeconds,
                                isPaused = true,
                                currentPomodoro = 1,
                                totalPomodoros = settings.pomodoroCount,
                                phase = TimerPhase.POMODORO,
                                error = null,
                                isShakeSensorEnabled = it.isShakeSensorEnabled,
                                isProximitySensorEnabled = it.isProximitySensorEnabled
                            )
                        }
                        throw CancellationException()
                    }
                }
            } catch (e: CancellationException) {
                // Beklenen durum
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Timer sıfırlanamadı: ${e.message}")
                }
            }
        }
    }

    // Shake Sensor
    fun toggleShakeSensor() {
        val newState = !_uiState.value.isShakeSensorEnabled
        _uiState.update { it.copy(isShakeSensorEnabled = newState) }

        if (newState) {
            startShakeDetection()
        } else {
            stopShakeDetection()
        }
    }

    private fun startShakeDetection() {
        shakeJob = viewModelScope.launch {
            observeShakeEventsUseCase().collect {
                onPlayPauseClick()
            }
        }
    }

    private fun stopShakeDetection() {
        shakeJob?.cancel()
        shakeJob = null
    }

    // Proximity Sensor
    fun toggleProximitySensor() {
        val currentState = _uiState.value
        val newState = !currentState.isProximitySensorEnabled

        _uiState.update { it.copy(isProximitySensorEnabled = newState) }

        if (newState) {
            // Sensör açıldı
            if (!currentState.isPaused) {
                // Timer zaten çalışıyorsa, timer'ı durdur
                pauseTimer()
            }
        } else {
            // Sensör kapatıldı
            stopProximityMonitoring()
        }
    }

    private fun startProximityMonitoring() {
        stopProximityMonitoring() // Önceki monitoring varsa durdur

        proximityCheckJob = viewModelScope.launch {
            var elapsedTime = 0L

            // İLK 5 SANİYE: Her 1 saniyede bir kontrol
            while (elapsedTime < INITIAL_CHECK_DURATION) {
                // Tek seferlik orientation oku
                latestPhoneOrientation = getSingleOrientationValue()

                delay(INITIAL_POLL_INTERVAL)
                elapsedTime += INITIAL_POLL_INTERVAL

                // Timer durdu mu kontrol
                if (_uiState.value.isPaused) {
                    return@launch
                }
            }

            // 5 saniye sonunda kontrol - telefon yüz üstü mü?
            if (!latestPhoneOrientation) {
                pauseTimer()
                return@launch
            }

            // 5 SANİYE SONRASI: Her 3 saniyede bir kontrol
            while (_uiState.value.isProximitySensorEnabled && !_uiState.value.isPaused) {
                delay(PROXIMITY_CHECK_INTERVAL)

                // Tek seferlik orientation oku
                latestPhoneOrientation = getSingleOrientationValue()

                if (!latestPhoneOrientation) {
                    pauseTimer()
                    break
                }
            }
        }
    }

    private suspend fun getSingleOrientationValue(): Boolean {
        var result = false
        try {
            observePhoneOrientationUseCase().collect { isFaceDown ->
                result = isFaceDown
                throw CancellationException() // İlk değeri al ve hemen çık
            }
        } catch (e: CancellationException) {
            // Beklenen durum - flow'dan çıktık
        }
        return result
    }

    private fun stopProximityMonitoring() {
        proximityCheckJob?.cancel()
        proximityCheckJob = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        stopShakeDetection()
        stopProximityMonitoring()
    }
}