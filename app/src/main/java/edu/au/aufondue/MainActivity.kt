package edu.au.unimend.aufondue

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import edu.au.unimend.aufondue.auth.AuthManager
import edu.au.unimend.aufondue.navigation.Screen
import edu.au.unimend.aufondue.navigation.getBottomNavigationItems
import edu.au.unimend.aufondue.screens.home.HomeScreen
import edu.au.unimend.aufondue.screens.login.LoginScreen
import edu.au.unimend.aufondue.screens.reports.ReportsListScreen
import edu.au.unimend.aufondue.screens.notification.NotificationDetailsScreen
import edu.au.unimend.aufondue.screens.notification.NotificationScreen
import edu.au.unimend.aufondue.screens.profile.ProfileScreen
import edu.au.unimend.aufondue.screens.report.ReportScreen
import edu.au.unimend.aufondue.ui.theme.AUFondueTheme
import edu.au.unimend.aufondue.utils.LanguageManager
import edu.au.unimend.aufondue.api.RetrofitClient
import edu.au.unimend.aufondue.auth.UserPreferences
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
                MainContent()
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    private fun MainContent() {
        val authManager = remember { AuthManager.getInstance(this@MainActivity) }
        var isLoggedIn by rememberSaveable { 
            val initial = authManager.isSignedIn()
            Log.d(TAG, "Initial isLoggedIn state: $initial")
            mutableStateOf(initial) 
        }
        val navController = rememberNavController()
        
        // Handle notification click only once at startup if initially logged in
        var hasHandledNotification by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (isLoggedIn && !hasHandledNotification) {
                handleNotificationClick(intent)
                hasHandledNotification = true
            }
        }

        Log.d(TAG, "Current isLoggedIn state: $isLoggedIn")
        
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

                                composable(
                                    route = "reports_list/{statusFilter}",
                                    arguments = listOf(navArgument("statusFilter") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val statusFilter = backStackEntry.arguments?.getString("statusFilter") ?: "ALL"
                                    ReportsListScreen(
                                        statusFilter = statusFilter,
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigateToIssueDetails = { issueId ->
                                            navController.navigate("notification_details/${issueId.toLongOrNull() ?: 0L}")
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
                                        onSignOut = {
                                            Log.d(TAG, "Sign out clicked")
                                            
                                            // Perform sign out first to clear Firebase auth
                                            authManager.signOut {
                                                Log.d(TAG, "AuthManager sign out completed")
                                                // Then update login state to trigger UI change
                                                isLoggedIn = false
                                                Log.d(TAG, "isLoggedIn set to false after sign out")
                                            }
                                        }
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

                                composable(
                                    route = "issue_details/{issueId}",
                                    arguments = listOf(navArgument("issueId") { type = NavType.StringType })  // String, not Long
                                ) { backStackEntry ->
                                    val issueId = backStackEntry.arguments?.getString("issueId") ?: ""
                                    // Convert String to Long for the NotificationDetailsScreen
                                    val issueIdLong = issueId.toLongOrNull() ?: 0L
                                    NotificationDetailsScreen(
                                        issueId = issueIdLong,
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
                            // Initialize FCM after successful login (commenting out since FCM not used)
                            // initializeFCM()
                        }
                    )
                }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationClick(intent)
    }

    override fun attachBaseContext(newBase: Context?) {
        val context = newBase ?: this
        val updatedContext = LanguageManager.setLocale(context, LanguageManager.getSavedLanguage(context))
        super.attachBaseContext(updatedContext)
    }

    private fun initializeFCM() {
        Log.d(TAG, "WARNING: initializeFCM called but should be disabled!")

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

            // Store the navigation intent for later use
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
            currentRoute.startsWith("issue_details") -> false
            else -> true
        }
    }
}