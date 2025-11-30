package com.fatih.pomodoroapp1.ui.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.fatih.pomodoroapp1.ui.components.Screen
import com.fatih.pomodoroapp1.ui.screens.calendar.CalendarScreen
import com.fatih.pomodoroapp1.ui.screens.calendar.CalendarViewModel
import com.fatih.pomodoroapp1.ui.screens.settings.SettingsScreen
import com.fatih.pomodoroapp1.ui.screens.settings.SettingsViewModel
import com.fatih.pomodoroapp1.ui.screens.statistics.StatisticsScreen
import com.fatih.pomodoroapp1.ui.screens.statistics.StatisticsViewModel
import com.fatih.pomodoroapp1.ui.screens.timer.TimerScreen
import com.fatih.pomodoroapp1.ui.screens.timer.TimerViewModel


fun NavGraphBuilder.timerScreen() {
    composable(Screen.Timer.route) {
        val viewModel: TimerViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        TimerScreen(
            uiState = uiState,
            onPlayPauseClick = viewModel::onPlayPauseClick,
            onSkipClick = viewModel::onSkipClick,
            onRestartClick = viewModel::onRestartClick,
            onToggleShakeSensor = viewModel::toggleShakeSensor,
            onToggleProximitySensor = viewModel::toggleProximitySensor
        )
    }
}

fun NavGraphBuilder.calendarScreen() {
    composable(Screen.Calendar.route) {
        val viewModel: CalendarViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        CalendarScreen(
            uiState = uiState,
            onMonthChange = viewModel::onMonthChange,
            onDateSelected = viewModel::onDateSelected,
            onRefreshEvents = viewModel::refreshHistoricalEvents,
            onNextCarousel = viewModel::nextCarouselPage,
            onPreviousCarousel = viewModel::previousCarouselPage
        )
    }
}

fun NavGraphBuilder.statisticsScreen() {
    composable(Screen.Statistics.route) {
        val viewModel: StatisticsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        StatisticsScreen(
            uiState = uiState,
            onPeriodChange = viewModel::onPeriodChange
        )
    }
}

fun NavGraphBuilder.settingsScreen() {
    composable(Screen.Settings.route) {
        val viewModel: SettingsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        SettingsScreen(
            uiState = uiState,
            onSettingsChange = viewModel::onSettingsChange,
            onSaveClick = viewModel::onSaveClick
        )
    }
}