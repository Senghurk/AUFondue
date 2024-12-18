package edu.au.aufondue.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.au.aufondue.api.RetrofitClient
import edu.au.aufondue.api.models.IssueResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
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

    // Temporary mock user ID until Azure auth is implemented
    private val mockUserId = 1L

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                // Load submitted reports
                val submittedResponse = RetrofitClient.apiService.getUserSubmittedIssues(
                    userId = mockUserId,
                    page = 0,
                    size = 10
                )

                // Load tracked reports
                val trackedResponse = RetrofitClient.apiService.getAllIssuesTracking(
                    page = 0,
                    size = 10
                )

                if (submittedResponse.isSuccessful && trackedResponse.isSuccessful) {
                    val submittedReports = submittedResponse.body()?.data?.map { it.toReportItem() } ?: emptyList()
                    val trackedReports = trackedResponse.body()?.data?.map { it.toReportItem() } ?: emptyList()

                    _state.value = _state.value.copy(
                        isLoading = false,
                        submittedReports = submittedReports,
                        trackedReports = trackedReports
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load reports"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unknown error occurred"
                )
            }
        }
    }

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

    private fun getTimeAgo(dateTime: LocalDateTime): String {
        val now = LocalDateTime.now()
        val minutes = ChronoUnit.MINUTES.between(dateTime, now)
        val hours = ChronoUnit.HOURS.between(dateTime, now)
        val days = ChronoUnit.DAYS.between(dateTime, now)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            else -> "$days day${if (days > 1) "s" else ""} ago"
        }
    }

    fun refreshData() {
        loadReports()
    }
}