package edu.au.aufondue.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.au.aufondue.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignUp: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val _navigationEvent = MutableStateFlow(false)
    val navigationEvent = _navigationEvent.asStateFlow()

    private var authManager: AuthManager? = null

    fun initializeAuth(context: Context) {
        if (authManager == null) {
            authManager = AuthManager.getInstance(context)

            // Check if user is already signed in
            if (authManager?.isSignedIn() == true) {
                _navigationEvent.value = true
            }
        }
    }

    /**
     * Sign in with email and password
     */
    fun signInWithEmailPassword(email: String, password: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)

                authManager?.signInWithEmailPassword(
                    email = email.trim(),
                    password = password,
                    onSuccess = { token ->
                        _state.value = _state.value.copy(isLoading = false)
                        _navigationEvent.value = true
                    },
                    onError = { exception ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = getFirebaseErrorMessage(exception)
                        )
                    }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Create account with email and password
     */
    fun createAccount(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)

                authManager?.createAccountWithEmailPassword(
                    email = email.trim(),
                    password = password,
                    displayName = displayName.trim(),
                    onSuccess = { token ->
                        _state.value = _state.value.copy(isLoading = false)
                        _navigationEvent.value = true
                    },
                    onError = { exception ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = getFirebaseErrorMessage(exception)
                        )
                    }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Send password reset email
     */
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)

                authManager?.sendPasswordResetEmail(
                    email = email.trim(),
                    onSuccess = {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Password reset email sent to $email"
                        )
                    },
                    onError = { exception ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = getFirebaseErrorMessage(exception)
                        )
                    }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Toggle between sign in and sign up modes
     */
    fun toggleSignUpMode() {
        _state.value = _state.value.copy(
            isSignUp = !_state.value.isSignUp,
            error = null
        )
    }

    /**
     * Convert Firebase exceptions to user-friendly messages
     */
    private fun getFirebaseErrorMessage(exception: Exception): String {
        return when {
            exception.message?.contains("badly formatted") == true ->
                "Please enter a valid email address"
            exception.message?.contains("There is no user record") == true ->
                "No account found with this email address"
            exception.message?.contains("password is invalid") == true ->
                "Incorrect password"
            exception.message?.contains("email address is already") == true ->
                "An account with this email already exists"
            exception.message?.contains("weak-password") == true ->
                "Password should be at least 6 characters"
            exception.message?.contains("network error") == true ->
                "Network error. Please check your connection"
            exception.message?.contains("AU email") == true ->
                "Please use your AU email address (@au.edu)"
            else -> exception.message ?: "Authentication failed"
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        authManager = null
    }
}