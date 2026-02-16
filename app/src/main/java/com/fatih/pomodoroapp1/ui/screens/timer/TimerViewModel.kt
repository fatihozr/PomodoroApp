package com.fatih.pomodoroapp1.ui.screens.timer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatih.pomodoroapp1.domain.model.TimerPhase
import com.fatih.pomodoroapp1.domain.usecase.*
import com.fatih.pomodoroapp1.service.TimerActionReceiver
import com.fatih.pomodoroapp1.service.TimerNotificationService
import com.fatih.pomodoroapp1.ui.model.TimerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val getNextTimerPhaseUseCase: GetNextTimerPhaseUseCase,
    private val saveCompletedPomodoroUseCase: SaveCompletedPomodoroUseCase,
    private val observeShakeEventsUseCase: ObserveShakeEventsUseCase,
    private val observePhoneOrientationUseCase: ObservePhoneOrientationUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var shakeJob: Job? = null
    private var proximityCheckJob: Job? = null

    init {
        observeSettings()
        setupNotificationActions()
    }

    private fun setupNotificationActions() {
        TimerActionReceiver.setActionListener { action ->
            when (action) {
                TimerNotificationService.ACTION_PLAY_PAUSE -> onPlayPauseClick()
                TimerNotificationService.ACTION_RESET -> onRestartClick()
            }
        }
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
        if (_uiState.value.isPaused) {
            startTimer()
        } else {
            pauseTimer()
        }
    }

    private fun startTimer() {
        _uiState.update { it.copy(isPaused = false, error = null) }

        if (_uiState.value.isProximitySensorEnabled) {
            startProximityMonitoring()
        }

        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemainingSeconds > 0 && !_uiState.value.isPaused) {
                delay(1000)
                _uiState.update {
                    it.copy(timeRemainingSeconds = it.timeRemainingSeconds - 1)
                }
                updateNotification()
            }

            if (_uiState.value.timeRemainingSeconds == 0) {
                onTimerComplete()
            }
        }

        updateNotification()
    }

    private fun pauseTimer() {
        _uiState.update { it.copy(isPaused = true) }
        timerJob?.cancel()
        proximityCheckJob?.cancel()
        updateNotification()
    }

    private fun onTimerComplete() {
        val currentState = _uiState.value

        viewModelScope.launch {
            if (currentState.phase == TimerPhase.POMODORO) {
                // Mola bildirimi göster
                showToast("🎉 Harika iş! Şimdi mola vermelisin!")

                saveCompletedPomodoroUseCase().fold(
                    onSuccess = {
                        println("✅ Pomodoro başarıyla kaydedildi")
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(error = "Pomodoro kaydedilemedi: ${error.message}")
                        }
                    }
                )
            } else {
                // Mola bitti, çalışma zamanı
                showToast("⏰ Mola bitti! Çalışmaya hazır mısın?")
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
                updateNotification()
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
                        updateNotification()
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
                val wasPaused = _uiState.value.isPaused
                onPlayPauseClick()

                // Toast message göster
                val message = if (wasPaused) "Shake Algılandı: Play" else "Shake Algılandı: Pause"
                showToast(message)
            }
        }
    }

    private fun stopShakeDetection() {
        shakeJob?.cancel()
        shakeJob = null
    }

    // Proximity Sensor - OPTIMIZE EDİLMİŞ
    fun toggleProximitySensor() {
        val currentState = _uiState.value
        val newState = !currentState.isProximitySensorEnabled

        println("🔄 Proximity Sensor: ${if (newState) "AÇIK" else "KAPALI"}")

        if (newState) {
            if (!currentState.isPaused) {
                println("⏸️ Proximity açıldı - Timer durduruluyor")
                pauseTimer()
            }
            _uiState.update { it.copy(isProximitySensorEnabled = true) }
        } else {
            _uiState.update { it.copy(isProximitySensorEnabled = false) }
            stopProximityDetection()
        }
    }

    private fun startProximityMonitoring() {
        proximityCheckJob = viewModelScope.launch {
            println("⏳ İlk 5 saniye: Her saniye kontrol ediliyor...")

            // İLK 5 SANİYE: Her saniye kontrol et
            repeat(5) { second ->
                delay(1000) // 1 saniye bekle

                val isFaceDown = measureOrientation()
                println("🔍 ${second + 1}. saniye: isFaceDown=$isFaceDown")
            }

            // 5 saniye sonunda SON KONTROL
            println("✅ 5 saniye geçti, son durum kontrol ediliyor...")
            val finalCheck = measureOrientation()
            println("🔍 5. saniye sonu kontrolü: isFaceDown=$finalCheck")

            if (!finalCheck) {
                println("❌ Telefon yüzü aşağı değil - Timer durduruluyor")
                pauseTimer()
                return@launch
            }

            println("✅ Telefon yüzü aşağı! Timer devam ediyor. Artık her 3 saniyede kontrol edilecek...")

            // 5 saniye sonrası: Her 3 saniyede bir kontrol
            var checkCount = 1
            while (_uiState.value.isProximitySensorEnabled && !_uiState.value.isPaused) {
                delay(3000)
                checkCount++

                val currentOrientation = measureOrientation()
                println("🔍 Kontrol #$checkCount: isFaceDown=$currentOrientation")

                if (!currentOrientation) {
                    println("❌ Telefon kaldırıldı - Timer durduruluyor")
                    pauseTimer()
                    break
                }
            }
        }
    }

    // Tek bir ölçüm al ve hemen kapat (PERFORMANS OPTİMİZASYONU)
    private suspend fun measureOrientation(): Boolean {
        return withTimeoutOrNull(500) {
            observePhoneOrientationUseCase().first()
        } ?: false
    }

    private fun stopProximityDetection() {
        proximityCheckJob?.cancel()
        proximityCheckJob = null
    }

    private fun updateNotification() {
        val state = _uiState.value

        try {
            TimerNotificationService.startService(
                context = context,
                timeRemaining = state.timeRemainingSeconds,
                isPaused = state.isPaused
            )
        } catch (e: Exception) {
            println("❌ Notification hatası: ${e.message}")
        }
    }

    private fun stopNotification() {
        TimerNotificationService.stopService(context)
    }

    private fun showToast(message: String) {
        viewModelScope.launch {
            try {
                android.widget.Toast.makeText(
                    context,
                    message,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                println("❌ Toast hatası: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        stopShakeDetection()
        stopProximityDetection()
        TimerActionReceiver.clearActionListener()
        stopNotification()
    }
}