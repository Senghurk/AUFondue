package com.example.aufondue.screens.map

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
    val priority: String,
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

    fun loadIssues() {
        viewModelScope.launch {
            // TODO: Replace with actual API call
            _issues.value = generateSampleIssues()
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
                priority = "High",
                latitude = -33.865143,
                longitude = 151.209900
            ),
            MapIssue(
                id = "2",
                title = "Cracked Wall",
                description = "Wall damage in corridor",
                category = "Cracked",
                priority = "Medium",
                latitude = -33.866943,
                longitude = 151.208900
            ),
            MapIssue(
                id = "3",
                title = "Water Leak",
                description = "Ceiling leak in room 101",
                category = "Leaking",
                priority = "High",
                latitude = -33.864143,
                longitude = 151.210900
            )
        )
    }
}