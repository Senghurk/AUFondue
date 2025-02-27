package edu.au.aufondue.screens.map

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.au.aufondue.api.RetrofitClient
import edu.au.aufondue.api.models.IssueResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId

data class MapIssue(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MapViewModel : ViewModel() {
    private val _issues = MutableStateFlow<List<MapIssue>>(emptyList())
    val issues: StateFlow<List<MapIssue>> = _issues.asStateFlow()

    private val _selectedIssue = MutableStateFlow<MapIssue?>(null)
    val selectedIssue: StateFlow<MapIssue?> = _selectedIssue.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val TAG = "MapViewModel"

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadIssues() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Fetch data from the real API
                val response = RetrofitClient.apiService.getAllIssuesTracking()

                if (!response.isSuccessful) {
                    _error.value = "Failed to load issues: ${response.code()}"
                    return@launch
                }

                val issuesResponse = response.body()
                if (issuesResponse?.success != true || issuesResponse.data == null) {
                    _error.value = "API error: ${issuesResponse?.message ?: "Unknown error"}"
                    return@launch
                }

                val apiIssues = issuesResponse.data.filter {
                    // Only include issues with valid coordinates
                    !it.usingCustomLocation && it.latitude != null && it.longitude != null
                }

                Log.d(TAG, "Loaded ${apiIssues.size} issues with valid coordinates")

                // Convert API responses to our MapIssue model
                val mapIssues = apiIssues.map { issue ->
                    MapIssue(
                        id = issue.id.toString(),
                        title = issue.category,
                        description = issue.description,
                        category = issue.category,
                        latitude = issue.latitude ?: 0.0,
                        longitude = issue.longitude ?: 0.0,
                        status = issue.status,
                        timestamp = issue.createdAt?.atZone(ZoneId.systemDefault())?.toEpochSecond() ?: System.currentTimeMillis()                    )
                }

                _issues.value = mapIssues
            } catch (e: Exception) {
                Log.e(TAG, "Error loading issues", e)
                _error.value = e.message ?: "An unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectIssue(issue: MapIssue) {
        _selectedIssue.value = issue
    }

    fun clearSelectedIssue() {
        _selectedIssue.value = null
    }
}