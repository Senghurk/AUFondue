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
import edu.au.aufondue.auth.UserPreferences
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

data class ReportState(
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

    private fun getUserInfo(): Pair<String, String> {
        val context = context ?: throw IllegalStateException("Context not set")
        val prefs = UserPreferences.getInstance(context)
        val email = prefs.getUserEmail()
        val username = prefs.getUsername()

        if (email == null || username == null) {
            throw IllegalStateException("User not logged in")
        }

        return email to username
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

    private fun validateInput() {
        val currentState = state.value
        val errors = mutableListOf<String>()

        try {
            getUserInfo() // Validate user is logged in
        } catch (e: IllegalStateException) {
            errors.add("User must be logged in to submit a report")
        }

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

    private fun createIssueRequest(currentState: ReportState): IssueRequest {
        val (userEmail, userName) = getUserInfo()

        return IssueRequest(
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
            isUsingCustomLocation = currentState.isUsingCustomLocation,
            userEmail = userEmail,
            userName = userName
        )
    }

    fun submitReport(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                validateInput()

                val currentState = state.value
                val ctx = context ?: throw IllegalStateException("Context not set")

                val issueRequest = createIssueRequest(currentState)
                val issueJson = moshi.adapter(IssueRequest::class.java).toJson(issueRequest)

                Log.d("ReportViewModel", "Request JSON: $issueJson")

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
                            Log.d("ReportViewModel", "Added photo part: ${tempFile.absolutePath}")
                        } ?: Log.e("ReportViewModel", "Failed to create temp file from URI: $uri")
                    } catch (e: Exception) {
                        Log.e("ReportViewModel", "Error processing photo: ${e.message}", e)
                    }
                }

                if (photoParts.isEmpty()) {
                    throw IllegalStateException("Failed to process photos")
                }

                Log.d("ReportViewModel", "Submitting report with ${photoParts.size} photos")
                Log.d("ReportViewModel", "User info - email=${issueRequest.userEmail}, name=${issueRequest.userName}")

                try {
                    val response = RetrofitClient.apiService.createIssue(issueRequestBody, photoParts)
                    Log.d("ReportViewModel", "Response code: ${response.code()}")

                    if (!response.isSuccessful) {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("ReportViewModel", "API Error: ${response.code()} - $errorBody")
                        throw Exception("API returned error code ${response.code()}: $errorBody")
                    }

                    val responseBody = response.body()
                    if (responseBody == null) {
                        Log.e("ReportViewModel", "API returned null response body")
                        throw Exception("Server returned empty response")
                    }

                    if (!responseBody.success) {
                        Log.e("ReportViewModel", "API returned error: ${responseBody.message}")
                        throw Exception("API error: ${responseBody.message}")
                    }

                    Log.d("ReportViewModel", "Report submitted successfully")
                    _state.update { ReportState() }
                    onSuccess()
                } catch (e: Exception) {
                    Log.e("ReportViewModel", "API call failed", e)
                    throw Exception("Failed to submit report: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("ReportViewModel", "Error submitting report", e)
                _state.update { it.copy(error = e.message ?: "An unknown error occurred") }
            } finally {
                _state.update { it.copy(isLoading = false) }

                // Clean up temporary files
                context?.cacheDir?.listFiles()?.forEach { file ->
                    if (file.name.startsWith("upload_")) {
                        try {
                            if (file.delete()) {
                                Log.d("ReportViewModel", "Deleted temp file: ${file.absolutePath}")
                            } else {
                                Log.w("ReportViewModel", "Failed to delete temp file: ${file.absolutePath}")
                            }
                        } catch (e: Exception) {
                            Log.w("ReportViewModel", "Error deleting temp file: ${e.message}")
                        }
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