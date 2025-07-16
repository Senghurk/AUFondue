package edu.au.aufondue

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.google.firebase.messaging.FirebaseMessaging
import edu.au.aufondue.auth.AuthManager
import edu.au.aufondue.navigation.Screen
import edu.au.aufondue.navigation.getBottomNavigationItems
import edu.au.aufondue.screens.home.HomeScreen
import edu.au.aufondue.screens.login.LoginScreen
import edu.au.aufondue.screens.map.CampusMapScreen
import edu.au.aufondue.screens.notification.NotificationDetailsScreen
import edu.au.aufondue.screens.notification.NotificationScreen
import edu.au.aufondue.screens.profile.ProfileScreen
import edu.au.aufondue.screens.report.ReportScreen
import edu.au.aufondue.ui.theme.AUFondueTheme
import edu.au.aufondue.utils.LanguageManager
import edu.au.aufondue.api.RetrofitClient
import edu.au.aufondue.auth.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AUFondueTheme {
                var isLoggedIn by remember { mutableStateOf(false) }
                val navController = rememberNavController()

                // Check authentication status
                LaunchedEffect(Unit) {
                    isLoggedIn = AuthManager.isLoggedIn(this@MainActivity)
                    if (isLoggedIn) {
                        // Initialize FCM after checking login status
                        initializeFCM()
                        // Handle notification click if app was opened from notification
                        handleNotificationClick(intent)
                    }
                }

                if (isLoggedIn) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    Scaffold(
                        bottomBar = {
                            if (shouldShowBottomBar(currentRoute)) {
                                NavigationBar(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    getBottomNavigationItems().forEach { item ->
                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = item.title
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = item.title,
                                                    overflow = TextOverflow.Ellipsis,
                                                    maxLines = 1
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
                                startDestination = Screen.Home.route
                            ) {
                                composable(Screen.Home.route) {
                                    HomeScreen(
                                        onNavigateToReport = {
                                            navController.navigate(Screen.Report.route)
                                        },
                                        navController = navController
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
                                    CampusMapScreen(
                                        onNavigateBack = {
                                            navController.navigateUp()
                                        },
                                        onBuildingClick = { buildingId ->
                                            // Optional: Navigate to report screen with pre-filled location
                                            // navController.navigate("${Screen.Report.route}?buildingId=$buildingId")
                                        }
                                    )
                                }

                                composable(Screen.Notification.route) {
                                    NotificationScreen(
                                        onNavigateBack = { navController.popBackStack() },
                                        navController = navController
                                    )
                                }

                                composable(Screen.Profile.route) {
                                    ProfileScreen(
                                        onLogout = {
                                            // Remove FCM token before logout
                                            removeFcmTokenFromServer()
                                            AuthManager.logout(this@MainActivity)
                                            isLoggedIn = false
                                        },
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
                } else {
                    LoginScreen(
                        onLoginSuccess = {
                            isLoggedIn = true
                            // Initialize FCM after successful login
                            initializeFCM()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.let { handleNotificationClick(it) }
    }

    override fun attachBaseContext(newBase: Context?) {
        val context = newBase ?: this
        val updatedContext = LanguageManager.setLocale(context, LanguageManager.getSavedLanguage(context))
        super.attachBaseContext(updatedContext)
    }

    private fun initializeFCM() {
        Log.d(TAG, "Initializing FCM...")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Token: $token")

            // Send token to your backend server
            sendTokenToServer(token)
        }
    }

    private fun sendTokenToServer(token: String) {
        val userPreferences = UserPreferences.getInstance(this)
        val userEmail = userPreferences.getUserEmail()

        if (userEmail != null) {
            Log.d(TAG, "Sending FCM token to server for user: $userEmail")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.apiService.updateFcmToken(userEmail, token)
                    if (response.isSuccessful) {
                        Log.d(TAG, "FCM token sent to server successfully")
                    } else {
                        Log.e(TAG, "Failed to send FCM token to server: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending FCM token to server", e)
                }
            }
        } else {
            Log.w(TAG, "User email not found, cannot send FCM token")
        }
    }

    private fun removeFcmTokenFromServer() {
        val userPreferences = UserPreferences.getInstance(this)
        val userEmail = userPreferences.getUserEmail()

        if (userEmail != null) {
            Log.d(TAG, "Removing FCM token from server for user: $userEmail")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.apiService.removeFcmToken(userEmail)
                    if (response.isSuccessful) {
                        Log.d(TAG, "FCM token removed from server successfully")
                    } else {
                        Log.e(TAG, "Failed to remove FCM token from server: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing FCM token from server", e)
                }
            }
        }
    }

    private fun handleNotificationClick(intent: Intent) {
        val issueId = intent.getStringExtra("issueId")
        val navigateToNotifications = intent.getBooleanExtra("navigateToNotifications", false)

        if (navigateToNotifications && issueId != null) {
            Log.d(TAG, "App opened from notification, issue ID: $issueId")

            // You can store this in a shared preference or pass it to your navigation
            // For now, we'll just log it. The actual navigation should happen in the compose UI
            // based on checking for pending navigation actions

            // Store the navigation intent
            val sharedPref = getSharedPreferences("au_fondue_prefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("pending_navigation_issue_id", issueId)
                putBoolean("pending_navigation_to_notification", true)
                apply()
            }
        }
    }

    private fun shouldShowBottomBar(currentRoute: String?): Boolean {
        return when {
            currentRoute == null -> true
            currentRoute.startsWith("notification_details") -> false
            else -> true
        }
    }
}