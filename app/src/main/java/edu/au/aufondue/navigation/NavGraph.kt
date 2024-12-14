package edu.au.aufondue.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Report : Screen("report")
    object Map : Screen("map")
    object Notification : Screen("notification")
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavigationItems = listOf(
    NavigationItem("Home", Icons.Default.Home, Screen.Home.route),
    NavigationItem("Map", Icons.Default.Map, Screen.Map.route),
    NavigationItem("Notifications", Icons.Default.Notifications, Screen.Notification.route),
    NavigationItem("Profile", Icons.Default.Person, Screen.Profile.route)
    )