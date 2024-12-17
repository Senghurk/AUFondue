package edu.au.aufondue.screens.report

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import edu.au.aufondue.api.RetrofitClient
import edu.au.aufondue.api.models.IssueRequest
import edu.au.aufondue.api.models.LocationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

data class ReportState(
    val title: String = "",           // Added title field
    val description: String = "",
    val category: String = "",
    val customCategory: String = "",
    val selectedPhotos: List<Uri> = emptyList(),
    val location: LocationData? = null,
    val customLocation: String = "",
    val isUsingCustomLocation: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ReportViewModel : ViewModel() {
    private var context: Context? = null
    private val _state = MutableStateFlow(ReportState())
    val state: StateFlow<ReportState> = _state.asStateFlow()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun setContext(context: Context) {
        this.context = context
    }

    fun onTitleChange(title: String) {         // Added title change handler
        _state.update { it.copy(title = title) }
    }

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

    fun onCustomLocationChange(location: String) {
        _state.update { it.copy(
            customLocation = location,
            isUsingCustomLocation = true,
            location = null
        ) }
    }

    fun toggleLocationInputMethod() {
        _state.update { it.copy(
            isUsingCustomLocation = !it.isUsingCustomLocation,
            customLocation = if (!it.isUsingCustomLocation) "" else it.customLocation,
            location = if (it.isUsingCustomLocation) null else it.location
        ) }
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
                ),
                isUsingCustomLocation = false,
                customLocation = ""
            )
        }
    }

    private fun generateTitle(currentState: ReportState): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val category = if (currentState.category == "Custom")
            currentState.customCategory
        else
            currentState.category
        return "$category Issue Report - $timestamp"
    }

    private fun validateInput() {
        val currentState = state.value
        val errors = mutableListOf<String>()

        when {
            currentState.description.isBlank() ->
                errors.add("Description is required")
            currentState.category.isBlank() ->
                errors.add("Please select a category")
            currentState.category == "Custom" && currentState.customCategory.isBlank() ->
                errors.add("Please enter a custom category")
            currentState.selectedPhotos.isEmpty() ->
                errors.add("Please attach at least one photo")
        }

        // Location validation
        when {
            currentState.isUsingCustomLocation && currentState.customLocation.isBlank() ->
                errors.add("Please enter a location description")
            !currentState.isUsingCustomLocation && currentState.location == null ->
                errors.add("Please select a location on the map")
        }

        if (errors.isNotEmpty()) {
            throw IllegalStateException(errors.joinToString("\n"))
        }
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        val ctx = context ?: return null
        try {
            val tempFile = File.createTempFile(
                "upload_${System.currentTimeMillis()}",
                ".jpg",
                ctx.cacheDir
            )

            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            return tempFile
        } catch (e: IOException) {
            Log.e("ReportViewModel", "Error creating temp file: ${e.message}")
            return null
        }
    }

    fun submitReport(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                validateInput()

                val currentState = state.value
                val ctx = context ?: throw IllegalStateException("Context not set")

                // Create the request object
                val issueRequest = IssueRequest(
                    title = generateTitle(currentState),    // Added title generation
                    description = currentState.description,
                    category = if (currentState.category == "Custom")
                        currentState.customCategory
                    else
                        currentState.category,
                    customCategory = if (currentState.category == "Custom")
                        currentState.customCategory
                    else
                        null,
                    latitude = if (!currentState.isUsingCustomLocation)
                        currentState.location?.latitude
                    else
                        null,
                    longitude = if (!currentState.isUsingCustomLocation)
                        currentState.location?.longitude
                    else
                        null,
                    customLocation = if (currentState.isUsingCustomLocation)
                        currentState.customLocation
                    else
                        null,
                    isUsingCustomLocation = currentState.isUsingCustomLocation
                )

                val issueJson = moshi.adapter(IssueRequest::class.java).toJson(issueRequest)
                val issueRequestBody = issueJson.toRequestBody("application/json".toMediaTypeOrNull())

                val photoParts = mutableListOf<MultipartBody.Part>()
                currentState.selectedPhotos.forEach { uri ->
                    try {
                        createTempFileFromUri(uri)?.let { tempFile ->
                            val photoBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                            val part = MultipartBody.Part.createFormData(
                                "photos",
                                "photo_${System.currentTimeMillis()}.jpg",
                                photoBody
                            )
                            photoParts.add(part)
                        }
                    } catch (e: Exception) {
                        Log.e("ReportViewModel", "Error processing photo: ${e.message}")
                    }
                }

                if (photoParts.isEmpty()) {
                    throw IllegalStateException("Failed to process photos")
                }

                Log.d("ReportViewModel", "Submitting report with request: $issueJson")
                Log.d("ReportViewModel", "Photos count: ${photoParts.size}")

                val response = RetrofitClient.apiService.createIssue(issueRequestBody, photoParts)

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("ReportViewModel", "Report submitted successfully")
                    _state.update { ReportState() }
                    onSuccess()
                } else {
                    throw Exception(response.body()?.message ?: "Failed to submit report")
                }
            } catch (e: Exception) {
                Log.e("ReportViewModel", "Error submitting report", e)
                _state.update { it.copy(error = e.message ?: "An unknown error occurred") }
            } finally {
                _state.update { it.copy(isLoading = false) }

                context?.cacheDir?.listFiles()?.forEach { file ->
                    if (file.name.startsWith("upload_")) {
                        file.delete()
                    }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        context?.cacheDir?.listFiles()?.forEach { file ->
            if (file.name.startsWith("upload_")) {
                file.delete()
            }
        }
    }
}