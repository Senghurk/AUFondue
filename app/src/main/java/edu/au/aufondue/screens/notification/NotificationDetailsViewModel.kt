package edu.au.aufondue.screens.notification

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.au.aufondue.api.RetrofitClient
import edu.au.aufondue.api.models.IssueResponse
import edu.au.aufondue.api.models.UpdateResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

data class NotificationDetailsState(
    val isLoading: Boolean = false,
    val issue: IssueResponse? = null,
    val updates: List<UpdateResponse> = emptyList(),
    val error: String? = null
)

class NotificationDetailsViewModel : ViewModel() {
    private val _state = MutableStateFlow(NotificationDetailsState())
    val state: StateFlow<NotificationDetailsState> = _state.asStateFlow()

    private val TAG = "NotificationDetailsVM"

    fun loadIssueDetails(issueId: Long) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                Log.d(TAG, "Loading issue details for ID: $issueId")

                // Fetch issue details
                val issueResponse = RetrofitClient.apiService.getIssueById(issueId)

                if (!issueResponse.isSuccessful) {
                    val errorBody = issueResponse.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "API Error: ${issueResponse.code()} - $errorBody")
                    throw Exception("Failed to load issue details: ${issueResponse.code()} - $errorBody")
                }

                val responseBody = issueResponse.body()
                if (responseBody == null) {
                    Log.e(TAG, "API returned null response body")
                    throw Exception("Server returned empty response")
                }

                if (!responseBody.success) {
                    Log.e(TAG, "API returned error: ${responseBody.message}")
                    throw Exception("API error: ${responseBody.message}")
                }

                val issue = responseBody.data
                if (issue == null) {
                    Log.e(TAG, "API returned null issue data")
                    throw Exception("Issue data not found")
                }

                Log.d(TAG, "Successfully loaded issue: ${issue.id}")

                // Fetch issue updates
                fetchIssueUpdates(issueId, issue)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading issue details", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unknown error occurred"
                    )
                }
            }
        }
    }

    private suspend fun fetchIssueUpdates(issueId: Long, issue: IssueResponse) {
        try {
            val updatesResponse = RetrofitClient.apiService.getIssueUpdates(issueId)

            if (!updatesResponse.isSuccessful) {
                Log.e(TAG, "Failed to fetch updates: ${updatesResponse.code()}")
                // Continue without updates
                _state.update {
                    it.copy(
                        isLoading = false,
                        issue = issue,
                        updates = emptyList()
                    )
                }
                return
            }

            val updates = updatesResponse.body() ?: emptyList()
            Log.d(TAG, "Loaded ${updates.size} updates")

            _state.update {
                it.copy(
                    isLoading = false,
                    issue = issue,
                    updates = updates.sortedByDescending { update -> update.updateTime }
                )
            }
        } catch (e: Exception) {
            // If updates fail, we still want to show the issue
            Log.e(TAG, "Error loading updates, but issue was loaded", e)
            _state.update {
                it.copy(
                    isLoading = false,
                    issue = issue,
                    updates = emptyList(),
                    error = "Could not load updates: ${e.message}"
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDateTime(dateTime: java.time.LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
        return dateTime.format(formatter)
    }
}