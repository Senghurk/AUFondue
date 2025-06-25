// Location: app/src/main/java/edu/au/aufondue/MainActivity.kt
// UPDATE THIS EXISTING FILE - REPLACE THE NAVIGATION SECTION

package edu.au.aufondue

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil3.ImageLoader
import edu.au.aufondue.navigation.Screen
import edu.au.aufondue.navigation.getBottomNavigationItems
import edu.au.aufondue.screens.home.HomeScreen
import edu.au.aufondue.screens.login.LoginScreen
import edu.au.aufondue.screens.map.MapScreen
import edu.au.aufondue.screens.notification.NotificationDetailsScreen
import edu.au.aufondue.screens.notification.NotificationScreen
import edu.au.aufondue.screens.profile.ProfileScreen
import edu.au.aufondue.screens.report.ReportScreen
import edu.au.aufondue.ui.theme.AUFondueTheme
import edu.au.aufondue.utils.LanguageManager

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved language
        LanguageManager.applyLanguage(this)

        ImageLoader.Builder(this).build()

        setContent {
            AUFondueTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val shouldShowBottomBar = currentRoute in setOf(
                    Screen.Home.route,
                    Screen.Map.route,
                    Screen.Notification.route,
                    Screen.Profile.route
                )

                Scaffold(
                    bottomBar = {
                        if (shouldShowBottomBar) {
                            NavigationBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                            ) {
                                val bottomNavigationItems = getBottomNavigationItems()
                                bottomNavigationItems.forEach { item ->
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = null
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.title,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        selected = currentRoute == item.route,
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Login.route
                        ) {
                            composable(Screen.Login.route) {
                                LoginScreen(
                                    onLoginSuccess = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.Home.route) {
                                HomeScreen(
                                    onNavigateToReport = {
                                        navController.navigate(Screen.Report.route)
                                    },
                                    navController = navController
                                )
                            }

                            composable(Screen.Profile.route) {
                                ProfileScreen(
                                    onSignOut = {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.Report.route) {
                                ReportScreen(
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable(Screen.Map.route) {
                                MapScreen(
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable(Screen.Notification.route) {
                                NotificationScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    navController = navController
                                )
                            }

                            composable(
                                route = "notification_details/{issueId}",
                                arguments = listOf(navArgument("issueId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val issueId = backStackEntry.arguments?.getLong("issueId") ?: 0L
                                NotificationDetailsScreen(
                                    issueId = issueId,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageManager.setLocale(newBase ?: this, LanguageManager.getSavedLanguage(newBase ?: this)))
    }
}