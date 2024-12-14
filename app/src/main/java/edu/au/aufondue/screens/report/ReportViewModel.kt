package edu.au.aufondue.screens.report

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)

data class ReportState(
    val description: String = "",
    val category: String = "",
    val customCategory: String = "",
    val selectedPhotos: List<Uri> = emptyList(),
    val location: LocationData? = null,
    val dateTime: LocalDateTime? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ReportViewModel : ViewModel() {
    private val _state = MutableStateFlow(ReportState())
    val state: StateFlow<ReportState> = _state.asStateFlow()

    fun onDescriptionChange(description: String) {
        _state.update { it.copy(description = description) }
    }

    fun onCategoryChange(category: String) {
        _state.update { it.copy(
            category = category,
            customCategory = if (category != "Custom") "" else it.customCategory
        ) }
    }

    fun onCustomCategoryChange(customCategory: String) {
        _state.update { it.copy(customCategory = customCategory) }
    }

    fun onDateTimeSelected(dateTime: LocalDateTime) {
        _state.update { it.copy(dateTime = dateTime) }
    }

    fun onPhotoSelected(uri: Uri) {
        _state.update { currentState ->
            currentState.copy(
                selectedPhotos = currentState.selectedPhotos + uri
            )
        }
    }

    fun onLocationSelected(latitude: Double, longitude: Double) {
        _state.update { currentState ->
            currentState.copy(
                location = LocationData(
                    latitude = latitude,
                    longitude = longitude
                )
            )
        }
    }

    fun removePhoto(uri: Uri) {
        _state.update { currentState ->
            currentState.copy(
                selectedPhotos = currentState.selectedPhotos.filter { it != uri }
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun submitReport(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                validateInput()
                // Simulate API delay
                kotlinx.coroutines.delay(1000)
                _state.update { ReportState() }
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun validateInput() {
        val currentState = state.value
        when {
            currentState.description.isBlank() ->
                throw IllegalStateException("Description cannot be empty")
            currentState.category.isBlank() ->
                throw IllegalStateException("Please select a category")
            currentState.category == "Custom" && currentState.customCategory.isBlank() ->
                throw IllegalStateException("Please enter a custom problem type")
            currentState.selectedPhotos.isEmpty() ->
                throw IllegalStateException("Please attach at least one photo")
            currentState.location == null ->
                throw IllegalStateException("Please select a location")
            currentState.dateTime == null ->
                throw IllegalStateException("Please select date and time")
        }
    }
}