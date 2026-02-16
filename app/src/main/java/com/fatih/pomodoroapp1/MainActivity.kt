package com.fatih.pomodoroapp1

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            println("✅ Notification izni verildi")
        } else {
            println("❌ Notification izni reddedildi")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ için notification izni iste
        requestNotificationPermission()

        enableEdgeToEdge()
        setContent {
            PomodoroTimerTheme {
                PomodoroApp()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    println("✅ Notification izni zaten var")
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
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