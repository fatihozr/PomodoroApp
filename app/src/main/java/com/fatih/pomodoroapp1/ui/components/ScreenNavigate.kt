package com.fatih.pomodoroapp1.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Timer : Screen("timer", "Timer", Icons.Default.Timer)
    object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarToday)
    object Statistics : Screen("statistics", "Stats", Icons.Default.BarChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    //Statik Liste
    companion object {
        val items = listOf(Timer, Calendar, Statistics, Settings)
    }
}
