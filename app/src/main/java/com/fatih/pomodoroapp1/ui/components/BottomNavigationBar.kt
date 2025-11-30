package com.fatih.pomodoroapp1.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun PomodoroBottomNavigation(
    navController: NavHostController,
    items: List<Screen>
) {
    NavigationBar(
        containerColor = Color.White, // Bottom bar arka plan rengi
        contentColor = Color.Black
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { screen ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) screen.icon else screen.icon,
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) },
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        // Geri tuşuna basıldığında uygulamanın başlangıcına dönmesini sağlar
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Aynı ekrana tekrar tıklandığında yeniden oluşturulmasını engeller
                        launchSingleTop = true
                        // State'i korur (örneğin scroll pozisyonu)
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White, // Seçili ikon rengi
                    selectedTextColor = Color.Black, // Seçili yazı rengi
                    indicatorColor = Color.Black, // Hap rengi (beyaz)
                    unselectedIconColor = Color.Gray, // Seçili olmayan ikon rengi
                    unselectedTextColor = Color.Gray // Seçili olmayan yazı rengi
                )
            )
        }
    }
}