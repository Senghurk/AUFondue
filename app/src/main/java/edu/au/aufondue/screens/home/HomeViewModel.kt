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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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

    private val TAG = "HomeViewModel"

    // Load current user reports based on tab selection
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadReports(context: Context, isSubmittedTab: Boolean) {
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
                    loadSubmittedReports(userId)
                } else {
                    // Load all reports for tracking
                    loadAllReports()
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
    private suspend fun loadSubmittedReports(userId: Long) {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error loading submitted reports", e)
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Error: ${e.message}"
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun loadAllReports() {
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

        return ReportItem(
            id = id.toString(),
            title = title,
            description = description,
            status = status,
            timeAgo = getTimeAgo(createdAt)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getTimeAgo(dateTime: LocalDateTime): String {
        try {
            // Convert both times to Instant (timestamp) for accurate comparison
            val dateTimeInstant = dateTime.atZone(ZoneId.systemDefault()).toInstant()
            val nowInstant = Instant.now()

            // Calculate time differences using timestamps
            val secondsAgo = ChronoUnit.SECONDS.between(dateTimeInstant, nowInstant)
            val minutesAgo = ChronoUnit.MINUTES.between(dateTimeInstant, nowInstant)
            val hoursAgo = ChronoUnit.HOURS.between(dateTimeInstant, nowInstant)
            val daysAgo = ChronoUnit.DAYS.between(dateTimeInstant, nowInstant)

            Log.d(TAG, "Time differences (using Instant) - seconds: $secondsAgo, minutes: $minutesAgo, hours: $hoursAgo, days: $daysAgo")

            if (secondsAgo < 0) {
                return "Just now"
            }

            return when {
                secondsAgo < 60 -> "Just now"
                minutesAgo < 60 -> "$minutesAgo min ago"
                hoursAgo < 24 -> "$hoursAgo hour${if (hoursAgo > 1) "s" else ""} ago"
                daysAgo < 30 -> "$daysAgo day${if (daysAgo > 1) "s" else ""} ago"
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                    dateTime.format(formatter)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating time difference", e)
            return "Unknown time"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshData(context: Context, isSubmittedTab: Boolean) {
        loadReports(context, isSubmittedTab)
    }
}