package edu.au.aufondue.screens.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import edu.au.aufondue.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LoginState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private var authManager: AuthManager? = null

    companion object {
        private const val TAG = "LoginViewModel"
    }

    init {
        initializeAuth()
    }

    private fun initializeAuth() {
        authManager = AuthManager.getInstance(getApplication())
    }

    /**
     * Sign in with Microsoft account - Primary authentication method
     */
    fun signInWithMicrosoft() {
        val authMgr = authManager
        if (authMgr == null) {
            _state.value = _state.value.copy(
                error = "Authentication service not initialized"
            )
            return
        }

        _state.value = _state.value.copy(
            isLoading = true,
            error = null
        )

        authMgr.signInWithMicrosoft(
            onSuccess = { _ ->
                Log.d(TAG, "Microsoft sign-in successful")
                _state.value = _state.value.copy(
                    isLoading = false,
                    loginSuccess = true
                )
            },
            onError = { exception ->
                Log.e(TAG, "Microsoft sign-in failed", exception)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = getMicrosoftErrorMessage(exception)
                )
            }
        )
    }

    /**
     * Legacy sign in method - redirects to Microsoft auth for backward compatibility
     */
    fun signIn() {
        signInWithMicrosoft()
    }

    /**
     * Convert Microsoft authentication exceptions to user-friendly messages
     */
    private fun getMicrosoftErrorMessage(exception: Exception): String {
        return when {
            // User cancelled the authentication
            exception.message?.contains("cancelled") == true ||
                    exception.message?.contains("CANCELLED") == true ->
                "Authentication was cancelled. Please try again."

            // Invalid account domain
            exception.message?.contains("domain") == true ||
                    exception.message?.contains("organization") == true ->
                "Please use your university Microsoft account (@au.edu)."

            // Network error
            exception.message?.contains("network") == true ||
                    exception.message?.contains("Network") == true ->
                "Network error. Please check your internet connection."

            // MSAL specific errors
            exception.message?.contains("MSAL") == true ->
                "Microsoft authentication service error. Please try again."

            // Account not found or access denied
            exception.message?.contains("access_denied") == true ||
                    exception.message?.contains("unauthorized") == true ->
                "Access denied. Please ensure you're using an authorized university account."

            // Default error message
            else -> exception.message ?: "Microsoft authentication failed. Please try again."
        }
    }

    /**
     * Clear any error state
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}