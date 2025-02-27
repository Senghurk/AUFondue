package edu.au.aufondue.screens.home

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.au.aufondue.api.RetrofitClient
import edu.au.aufondue.api.models.IssueResponse
import edu.au.aufondue.auth.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class HomeScreenState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val submittedReports: List<ReportItem> = emptyList(),
    val trackedReports: List<ReportItem> = emptyList()
)

data class ReportItem(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val timeAgo: String
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()
    private var context: Context? = null
    private val TAG = "HomeViewModel"

    // Store report creation timestamps locally
    private val reportTimestampMap = mutableMapOf<String, Long>()

    // Load current user reports based on tab selection
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadReports(context: Context, isSubmittedTab: Boolean) {
        this.context = context
        // Load any saved timestamps
        loadTimestamps(context)

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                // Get the current user's information
                val prefs = UserPreferences.getInstance(context)
                val userEmail = prefs.getUserEmail()
                val username = prefs.getUsername()

                if (userEmail == null || username == null) {
                    // User isn't logged in
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "User not logged in, please log in first"
                    )
                    return@launch
                }

                // Get user ID for API calls
                val userId = getUserId(userEmail, username)

                if (userId == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Could not retrieve user information"
                    )
                    return@launch
                }

                Log.d(TAG, "User ID: $userId, loading ${if (isSubmittedTab) "submitted" else "tracked"} reports")

                if (isSubmittedTab) {
                    // Load only reports submitted by this user
                    loadSubmittedReports(userId, context)
                } else {
                    // Load all reports for tracking
                    loadAllReports(context)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unknown error occurred"
                )
                Log.e(TAG, "Error loading reports", e)
            }
        }
    }

    private suspend fun getUserId(email: String, username: String): Long? {
        try {
            // Create or get user to ensure we have the ID
            val response = RetrofitClient.apiService.createOrGetUser(username, email)

            if (!response.isSuccessful) {
                Log.e(TAG, "API error: ${response.code()}")
                return null
            }

            val userResponse = response.body()
            if (userResponse?.success != true || userResponse.data == null) {
                Log.e(TAG, "API error: ${userResponse?.message}")
                return null
            }

            Log.d(TAG, "Retrieved user ID: ${userResponse.data.id}")
            return userResponse.data.id
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user ID", e)
            return null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun loadSubmittedReports(userId: Long, context: Context) {
        try {
            val response = RetrofitClient.apiService.getUserSubmittedIssues(userId)

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to load submitted reports: ${response.code()}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load your reports"
                )
                return
            }

            val issuesResponse = response.body()
            if (issuesResponse?.success != true) {
                Log.e(TAG, "API error: ${issuesResponse?.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = issuesResponse?.message ?: "Failed to load reports"
                )
                return
            }

            val issues = issuesResponse.data ?: emptyList()
            Log.d(TAG, "Loaded ${issues.size} submitted reports")

            val reportItems = issues.map {
                // Log the date-time for debugging
                Log.d(TAG, "Report ${it.id} created at: ${it.createdAt}")
                it.toReportItem()
            }

            _state.value = _state.value.copy(
                isLoading = false,
                submittedReports = reportItems
            )

            // Save timestamps
            saveTimestamps(context)

        } catch (e: Exception) {
            Log.e(TAG, "Error loading submitted reports", e)
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Error: ${e.message}"
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun loadAllReports(context: Context) {
        try {
            val response = RetrofitClient.apiService.getAllIssuesTracking()

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to load tracked reports: ${response.code()}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load tracked reports"
                )
                return
            }

            val issuesResponse = response.body()
            if (issuesResponse?.success != true) {
                Log.e(TAG, "API error: ${issuesResponse?.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = issuesResponse?.message ?: "Failed to load tracked reports"
                )
                return
            }

            val issues = issuesResponse.data ?: emptyList()
            Log.d(TAG, "Loaded ${issues.size} tracked reports")

            val reportItems = issues.map { it.toReportItem() }

            _state.value = _state.value.copy(
                isLoading = false,
                trackedReports = reportItems
            )

            // Save timestamps
            saveTimestamps(context)

        } catch (e: Exception) {
            Log.e(TAG, "Error loading tracked reports", e)
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Error: ${e.message}"
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun IssueResponse.toReportItem(): ReportItem {
        val title = when {
            category == "Custom" -> customCategory ?: "Custom Issue"
            else -> "$category Issue"
        }

        // Get the stored timestamp or store current time if this is the first time seeing this report
        val reportKey = "report_${id}"
        val timestamp = reportTimestampMap.getOrPut(reportKey) {
            System.currentTimeMillis()
        }

        return ReportItem(
            id = id.toString(),
            title = title,
            description = description,
            status = status,
            timeAgo = getTimeAgo(timestamp)
        )
    }

    private fun getTimeAgo(timestamp: Long): String {
        try {
            val now = System.currentTimeMillis()
            val diffMillis = now - timestamp

            val secondsAgo = diffMillis / 1000
            val minutesAgo = secondsAgo / 60
            val hoursAgo = minutesAgo / 60
            val daysAgo = hoursAgo / 24

            Log.d(TAG, "Time differences - seconds: $secondsAgo, minutes: $minutesAgo, hours: $hoursAgo, days: $daysAgo")

            return when {
                secondsAgo < 60 -> "Just now"
                minutesAgo < 60 -> "$minutesAgo min ago"
                hoursAgo < 24 -> "$hoursAgo hour${if (hoursAgo > 1) "s" else ""} ago"
                daysAgo < 30 -> "$daysAgo day${if (daysAgo > 1) "s" else ""} ago"
                else -> {
                    // For older than a month, format as date
                    val formatter = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                    formatter.format(java.util.Date(timestamp))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating time difference", e)
            return "Unknown time"
        }
    }

    // Functions to save and load timestamps from SharedPreferences
    private fun saveTimestamps(context: Context) {
        val prefs = context.getSharedPreferences("report_timestamps", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        reportTimestampMap.forEach { (key, timestamp) ->
            editor.putLong(key, timestamp)
        }

        editor.apply()
        Log.d(TAG, "Saved ${reportTimestampMap.size} timestamps to preferences")
    }

    private fun loadTimestamps(context: Context) {
        val prefs = context.getSharedPreferences("report_timestamps", Context.MODE_PRIVATE)

        prefs.all.forEach { (key, value) ->
            if (value is Long) {
                reportTimestampMap[key] = value
            }
        }

        Log.d(TAG, "Loaded ${reportTimestampMap.size} timestamps from preferences")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshData(context: Context, isSubmittedTab: Boolean) {
        loadReports(context, isSubmittedTab)
    }
}