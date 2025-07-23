package edu.au.aufondue.navigation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import edu.au.aufondue.R

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Report : Screen("report")
    object Map : Screen("map")
    object Notification : Screen("notification")
    object NotificationDetails : Screen("notification_details/{issueId}") {
        fun createRoute(issueId: Long): String = "notification_details/$issueId"
    }
    object IssueDetails : Screen("issue_details/{issueId}") {
        fun createRoute(issueId: Long): String = "issue_details/$issueId"
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun getBottomNavigationItems() = listOf(
    NavigationItem(
        title = stringResource(R.string.nav_home),
        icon = Icons.Default.Home,
        route = Screen.Home.route
    ),
    NavigationItem(
        title = stringResource(R.string.nav_map),
        icon = Icons.Default.Map,
        route = Screen.Map.route
    ),
    NavigationItem(
        title = stringResource(R.string.nav_notifications),
        icon = Icons.Default.Notifications,
        route = Screen.Notification.route
    ),
    NavigationItem(
        title = stringResource(R.string.nav_profile),
        icon = Icons.Default.Person,
        route = Screen.Profile.route
    )
)