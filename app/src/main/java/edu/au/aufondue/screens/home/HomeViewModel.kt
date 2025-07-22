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
import edu.au.aufondue.utils.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    // Load current user reports based on tab selection
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadReports(context: Context, isSubmittedTab: Boolean) {
        this.context = context

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
        return try {
            // Use createOrGetUser which is the correct API endpoint
            val response = RetrofitClient.apiService.createOrGetUser(username, email)
            if (response.isSuccessful) {
                val userResponse = response.body()
                if (userResponse?.success == true) {
                    userResponse.data?.id
                } else {
                    Log.e(TAG, "Failed to get user: ${userResponse?.message}")
                    null
                }
            } else {
                Log.e(TAG, "Failed to get user: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user ID", e)
            null
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
                    error = "Failed to load submitted reports"
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

        // FIXED: Use server timestamp instead of local device timestamp
        // The createdAt field from the server already contains the correct submission time
        val timeAgo = try {
            TimeUtils.formatTimeAgo(this.createdAt)
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting time for report ${this.id}", e)
            "Unknown time"
        }

        Log.d(TAG, "Report ${this.id}: Server createdAt = ${this.createdAt}, timeAgo = $timeAgo")

        return ReportItem(
            id = id.toString(),
            title = title,
            description = description,
            status = status,
            timeAgo = timeAgo
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshData(context: Context, isSubmittedTab: Boolean) {
        loadReports(context, isSubmittedTab)
    }
}