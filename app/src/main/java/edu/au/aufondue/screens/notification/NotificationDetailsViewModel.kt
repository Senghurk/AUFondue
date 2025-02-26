package edu.au.aufondue.screens.notification

import android.util.Log
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

    fun loadIssueDetails(issueId: Long) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }

                // Fetch issue details
                val issueResponse = RetrofitClient.apiService.getIssueById(issueId)
                if (!issueResponse.isSuccessful || issueResponse.body()?.success != true) {
                    throw Exception("Failed to load issue details: ${issueResponse.message()}")
                }

                val issue = issueResponse.body()?.data

                // Fetch issue updates
                val updatesResponse = RetrofitClient.apiService.getIssueUpdates(issueId)
                if (!updatesResponse.isSuccessful) {
                    throw Exception("Failed to load issue updates: ${updatesResponse.message()}")
                }

                val updates = updatesResponse.body() ?: emptyList()

                _state.update {
                    it.copy(
                        isLoading = false,
                        issue = issue,
                        updates = updates.sortedByDescending { update -> update.updateTime }
                    )
                }

            } catch (e: Exception) {
                Log.e("NotificationDetailsVM", "Error loading issue details", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unknown error occurred"
                    )
                }
            }
        }
    }

    fun formatDateTime(dateTime: java.time.LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
        return dateTime.format(formatter)
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}