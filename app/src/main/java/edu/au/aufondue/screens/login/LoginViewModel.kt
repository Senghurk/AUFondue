package edu.au.aufondue.screens.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import edu.au.aufondue.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LoginState(
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val isSignUp: Boolean = false,
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
     * Update email field
     */
    fun updateEmail(email: String) {
        _state.value = _state.value.copy(
            email = email,
            error = null
        )
    }

    /**
     * Update password field
     */
    fun updatePassword(password: String) {
        _state.value = _state.value.copy(
            password = password,
            error = null
        )
    }

    /**
     * Update first name field
     */
    fun updateFirstName(firstName: String) {
        _state.value = _state.value.copy(
            firstName = firstName,
            error = null
        )
    }

    /**
     * Update last name field
     */
    fun updateLastName(lastName: String) {
        _state.value = _state.value.copy(
            lastName = lastName,
            error = null
        )
    }

    /**
     * Sign in with email and password
     */
    fun signIn() {
        val authMgr = authManager
        if (authMgr == null) {
            _state.value = _state.value.copy(
                error = "Authentication service not initialized"
            )
            return
        }

        val currentState = _state.value
        if (currentState.email.isEmpty() || currentState.password.isEmpty()) {
            _state.value = _state.value.copy(
                error = "Please fill in all fields"
            )
            return
        }

        _state.value = _state.value.copy(
            isLoading = true,
            error = null
        )

        authMgr.signInWithEmailPassword(
            email = currentState.email.trim(),
            password = currentState.password,
            onSuccess = { token ->
                Log.d(TAG, "Email sign-in successful")
                _state.value = _state.value.copy(
                    isLoading = false,
                    loginSuccess = true
                )
            },
            onError = { exception ->
                Log.e(TAG, "Email sign-in failed", exception)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = getFirebaseErrorMessage(exception)
                )
            }
        )
    }

    /**
     * Create account with email and password
     */
    fun createAccount() {
        val authMgr = authManager
        if (authMgr == null) {
            _state.value = _state.value.copy(
                error = "Authentication service not initialized"
            )
            return
        }

        val currentState = _state.value
        if (currentState.email.isEmpty() || currentState.password.isEmpty() ||
            currentState.firstName.isEmpty() || currentState.lastName.isEmpty()) {
            _state.value = _state.value.copy(
                error = "Please fill in all fields"
            )
            return
        }

        // Additional validation
        if (currentState.password.length < 6) {
            _state.value = _state.value.copy(
                error = "Password must be at least 6 characters long"
            )
            return
        }

        _state.value = _state.value.copy(
            isLoading = true,
            error = null
        )

        // Combine first and last name for display name
        val displayName = "${currentState.firstName.trim()} ${currentState.lastName.trim()}"

        authMgr.createAccountWithEmailPassword(
            email = currentState.email.trim(),
            password = currentState.password,
            displayName = displayName,
            onSuccess = { token ->
                Log.d(TAG, "Account creation successful")
                _state.value = _state.value.copy(
                    isLoading = false,
                    loginSuccess = true
                )
            },
            onError = { exception ->
                Log.e(TAG, "Account creation failed", exception)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = getFirebaseErrorMessage(exception)
                )
            }
        )
    }

    /**
     * Send password reset email
     */
    fun sendPasswordReset() {
        val authMgr = authManager
        if (authMgr == null) {
            _state.value = _state.value.copy(
                error = "Authentication service not initialized"
            )
            return
        }

        val currentState = _state.value
        if (currentState.email.isEmpty()) {
            _state.value = _state.value.copy(
                error = "Please enter your email address"
            )
            return
        }

        _state.value = _state.value.copy(
            isLoading = true,
            error = null
        )

        authMgr.sendPasswordResetEmail(
            email = currentState.email.trim(),
            onSuccess = {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Password reset email sent! Check your inbox."
                )
            },
            onError = { exception ->
                Log.e(TAG, "Password reset failed", exception)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = getFirebaseErrorMessage(exception)
                )
            }
        )
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
            // Authentication disabled error
            exception.message?.contains("disabled") == true ||
                    exception.message?.contains("sign-in provider is disabled") == true ->
                "Email authentication is currently disabled. Please contact support."

            // Invalid email
            exception.message?.contains("badly formatted") == true ||
                    exception.message?.contains("invalid-email") == true ->
                "Please enter a valid email address."

            // Wrong password
            exception.message?.contains("password is invalid") == true ||
                    exception.message?.contains("wrong-password") == true ->
                "Incorrect password. Please try again."

            // User not found
            exception.message?.contains("no user record") == true ||
                    exception.message?.contains("user-not-found") == true ->
                "No account found with this email address."

            // Email already in use
            exception.message?.contains("email address is already in use") == true ||
                    exception.message?.contains("email-already-in-use") == true ->
                "An account with this email already exists."

            // Weak password
            exception.message?.contains("weak-password") == true ->
                "Password is too weak. Please choose a stronger password."

            // Too many requests
            exception.message?.contains("too-many-requests") == true ->
                "Too many failed attempts. Please try again later."

            // Network error
            exception.message?.contains("network") == true ->
                "Network error. Please check your internet connection."

            // Default error message
            else -> exception.message ?: "An error occurred during authentication."
        }
    }
}