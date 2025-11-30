package com.fatih.pomodoroapp1.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatih.pomodoroapp1.domain.model.HistoricalEvent
import com.fatih.pomodoroapp1.domain.repository.WikipediaRepository
import com.fatih.pomodoroapp1.domain.usecase.ObserveWorkDaysUseCase
import com.fatih.pomodoroapp1.ui.model.CalendarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val observeWorkDaysUseCase: ObserveWorkDaysUseCase,
    private val wikipediaRepository: WikipediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var carouselJob: Job? = null
    private var currentCarouselIndex = 0
    private var cachedAllEvents: List<HistoricalEvent> = emptyList() // Cache eklendi

    companion object {
        private const val EVENTS_PER_PAGE = 5
        private const val CAROUSEL_DELAY_MS = 15_000L
    }

    init {
        loadWorkDays()
        loadHistoricalEvents()
    }

    private fun loadWorkDays() {
        viewModelScope.launch {
            observeWorkDaysUseCase().collect { workDays ->
                _uiState.update { it.copy(workDays = workDays, isLoading = false) }
            }
        }
    }

    fun onMonthChange(yearMonth: YearMonth) {
        _uiState.update { it.copy(currentMonth = yearMonth) }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    private fun loadHistoricalEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            wikipediaRepository.getAllHistoricalEvents().fold(
                onSuccess = { events ->
                    _uiState.update { it.copy(isLoading = false) }

                    if (events.isNotEmpty()) {
                        cachedAllEvents = events // Cache'e al
                        currentCarouselIndex = 0
                        updateCarouselPage(events)
                        startAutoCarousel(events)
                    } else {
                        _uiState.update {
                            it.copy(
                                historicalEvents = emptyList(),
                                error = "Bugün için tarihi olay bulunamadı"
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            historicalEvents = emptyList(),
                            error = "Tarihi olaylar yüklenemedi: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    private fun startAutoCarousel(allEvents: List<*>) {
        carouselJob?.cancel()
        carouselJob = viewModelScope.launch {
            val totalPages = (allEvents.size + EVENTS_PER_PAGE - 1) / EVENTS_PER_PAGE

            while (true) {
                delay(CAROUSEL_DELAY_MS)
                currentCarouselIndex = (currentCarouselIndex + 1) % totalPages
                updateCarouselPage(allEvents)
            }
        }
    }

    private fun updateCarouselPage(allEvents: List<*>) {
        val totalPages = (allEvents.size + EVENTS_PER_PAGE - 1) / EVENTS_PER_PAGE
        val startIndex = currentCarouselIndex * EVENTS_PER_PAGE
        val endIndex = minOf(startIndex + EVENTS_PER_PAGE, allEvents.size)
        val currentEvents = allEvents.subList(startIndex, endIndex)

        _uiState.update {
            it.copy(
                historicalEvents = currentEvents as List<HistoricalEvent>,
                carouselPage = currentCarouselIndex + 1,
                totalCarouselPages = totalPages,
                error = null
            )
        }
    }

    fun nextCarouselPage() {
        if (cachedAllEvents.isEmpty()) return

        carouselJob?.cancel()

        val totalPages = (cachedAllEvents.size + EVENTS_PER_PAGE - 1) / EVENTS_PER_PAGE
        currentCarouselIndex = (currentCarouselIndex + 1) % totalPages
        updateCarouselPage(cachedAllEvents)

        viewModelScope.launch {
            delay(100)
            startAutoCarousel(cachedAllEvents)
        }
    }

    fun previousCarouselPage() {
        if (cachedAllEvents.isEmpty()) return

        carouselJob?.cancel()

        val totalPages = (cachedAllEvents.size + EVENTS_PER_PAGE - 1) / EVENTS_PER_PAGE
        currentCarouselIndex = if (currentCarouselIndex == 0) totalPages - 1 else currentCarouselIndex - 1
        updateCarouselPage(cachedAllEvents)

        viewModelScope.launch {
            delay(100)
            startAutoCarousel(cachedAllEvents)
        }
    }

    fun refreshHistoricalEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            wikipediaRepository.refreshCache().fold(
                onSuccess = {
                    // Refresh başarılı, tüm eventleri yeniden yükle
                    wikipediaRepository.getAllHistoricalEvents().fold(
                        onSuccess = { events ->
                            cachedAllEvents = events
                            currentCarouselIndex = 0
                            updateCarouselPage(events)
                            _uiState.update { it.copy(isLoading = false, error = null) }
                            startAutoCarousel(events)
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Yenileme sonrası yükleme başarısız: ${error.message}"
                                )
                            }
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Yenileme başarısız: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        carouselJob?.cancel()
    }
}