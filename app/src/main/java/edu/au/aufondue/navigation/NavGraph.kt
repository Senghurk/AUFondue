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
    object NotificationDetails : Screen("notification_details")
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavigationItems = listOf(
    NavigationItem(
        title = "Home",
        icon = Icons.Default.Home,
        route = Screen.Home.route
    ),
    NavigationItem(
        title = "Map",
        icon = Icons.Default.Map,
        route = Screen.Map.route
    ),
    NavigationItem(
        title = "Notifications",
        icon = Icons.Default.Notifications,
        route = Screen.Notification.route
    ),
    NavigationItem(
        title = "Profile",
        icon = Icons.Default.Person,
        route = Screen.Profile.route
    )
)