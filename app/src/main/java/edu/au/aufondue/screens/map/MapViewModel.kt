package edu.au.aufondue.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapIssue(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val status: String = "Open",
    val timestamp: Long = System.currentTimeMillis()
)

class MapViewModel : ViewModel() {
    private val _issues = MutableStateFlow<List<MapIssue>>(emptyList())
    val issues: StateFlow<List<MapIssue>> = _issues.asStateFlow()

    private val _selectedIssue = MutableStateFlow<MapIssue?>(null)
    val selectedIssue: StateFlow<MapIssue?> = _selectedIssue.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadIssues() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Simulate network delay
                kotlinx.coroutines.delay(1000)
                _issues.value = generateSampleIssues()
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

    private fun generateSampleIssues(): List<MapIssue> {
        return listOf(
            MapIssue(
                id = "1",
                title = "Broken Pipe",
                description = "Water pipe leakage in building A",
                category = "Broken",
                latitude = 13.850943,
                longitude = 100.567900
            ),
            MapIssue(
                id = "2",
                title = "Cracked Wall",
                description = "Wall damage in corridor",
                category = "Cracked",
                latitude = 13.851943,
                longitude = 100.568900
            ),
            MapIssue(
                id = "3",
                title = "Water Leak",
                description = "Ceiling leak in room 101",
                category = "Leaking",
                latitude = 13.849143,
                longitude = 100.566900
            )
        )
    }
}