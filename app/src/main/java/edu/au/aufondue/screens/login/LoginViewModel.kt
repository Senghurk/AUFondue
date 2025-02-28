package edu.au.aufondue.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.au.aufondue.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _navigationEvent = MutableStateFlow(false)
    val navigationEvent = _navigationEvent.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var authManager: AuthManager? = null

    fun initializeAuth(activity: Context) {
        if (authManager == null) {
            authManager = AuthManager.getInstance(activity)
        }
    }

    fun onMicrosoftLoginClick() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Use the simplified AuthManager that bypasses real authentication
                authManager?.signIn(
                    onSuccess = { token ->
                        _isLoading.value = false
                        _navigationEvent.value = true
                    },
                    onError = { exception ->
                        _isLoading.value = false
                        _error.value = exception.message ?: "Authentication failed"
                    }
                ) ?: run {
                    _isLoading.value = false
                    _error.value = "Authentication service not initialized"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = e.message ?: "An unexpected error occurred"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        authManager = null
    }
}