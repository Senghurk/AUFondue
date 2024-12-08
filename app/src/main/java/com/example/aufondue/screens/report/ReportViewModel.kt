package com.example.aufondue.screens.report

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReportState(
    val description: String = "",
    val location: String = "",
    val category: String = "",
    val dateTime: String = "",
    val priority: Priority = Priority.LOW,
    val isLoading: Boolean = false
)

enum class Priority { LOW, MEDIUM, HIGH }

class ReportViewModel : ViewModel() {
    private val _state = MutableStateFlow(ReportState())
    val state = _state.asStateFlow()

    fun onDescriptionChange(description: String) {
        _state.value = _state.value.copy(description = description)
    }

    fun onLocationChange(location: String) {
        _state.value = _state.value.copy(location = location)
    }

    fun onCategoryChange(category: String) {
        _state.value = _state.value.copy(category = category)
    }

    fun onDateTimeChange(dateTime: String) {
        _state.value = _state.value.copy(dateTime = dateTime)
    }

    fun onPriorityChange(priority: Priority) {
        _state.value = _state.value.copy(priority = priority)
    }

    fun submitReport(onSuccess: () -> Unit) {
        // TODO: Implement actual submission
        _state.value = _state.value.copy(isLoading = true)
        // Simulate API call
        onSuccess()
    }
}