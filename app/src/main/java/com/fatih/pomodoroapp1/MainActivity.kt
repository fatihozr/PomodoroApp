package com.fatih.pomodoroapp1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.fatih.pomodoroapp1.ui.components.PomodoroBottomNavigation
import com.fatih.pomodoroapp1.ui.components.Screen
import com.fatih.pomodoroapp1.ui.navigation.calendarScreen
import com.fatih.pomodoroapp1.ui.navigation.settingsScreen
import com.fatih.pomodoroapp1.ui.navigation.statisticsScreen
import com.fatih.pomodoroapp1.ui.navigation.timerScreen
import com.fatih.pomodoroapp1.ui.theme.PomodoroTimerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PomodoroTimerTheme {
                PomodoroApp()
            }
        }
    }
}

@Composable
fun PomodoroApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            PomodoroBottomNavigation(
                navController = navController,
                items = Screen.items
            )
        }
    ) { innerPadding ->
        PomodoroNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun PomodoroNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Timer.route,
        modifier = modifier
    ) {
        timerScreen()
        calendarScreen()
        statisticsScreen()
        settingsScreen()
    }
}