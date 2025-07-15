package edu.au.aufondue.screens.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.au.aufondue.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginState(
    val email: String = "",
    val password: String = "",
    val fullName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val isSignUp: Boolean = false,
    val passwordVisible: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private var authManager: AuthManager? = null
    private val TAG = "LoginViewModel"

    fun initialize(context: Context) {
        authManager = AuthManager.getInstance(context)
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
     * Update full name field
     */
    fun updateFullName(fullName: String) {
        _state.value = _state.value.copy(
            fullName = fullName,
            error = null
        )
    }

    /**
     * Toggle password visibility
     */
    fun togglePasswordVisibility() {
        _state.value = _state.value.copy(
            passwordVisible = !_state.value.passwordVisible
        )
    }

    /**
     * Sign in with email and password
     */
    fun signInWithEmail() {
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
        if (currentState.email.isEmpty() || currentState.password.isEmpty() || currentState.fullName.isEmpty()) {
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

        authMgr.createAccountWithEmailPassword(
            email = currentState.email.trim(),
            password = currentState.password,
            displayName = currentState.fullName.trim(),
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

            // Email validation errors
            exception.message?.contains("badly formatted") == true ||
                    exception.message?.contains("invalid email") == true ->
                "Please enter a valid email address"

            // User not found
            exception.message?.contains("There is no user record") == true ||
                    exception.message?.contains("user not found") == true ->
                "No account found with this email address"

            // Wrong password
            exception.message?.contains("password is invalid") == true ||
                    exception.message?.contains("wrong password") == true ->
                "Incorrect password"

            // Account already exists
            exception.message?.contains("email address is already") == true ||
                    exception.message?.contains("already in use") == true ->
                "An account with this email already exists"

            // Weak password
            exception.message?.contains("weak-password") == true ||
                    exception.message?.contains("password should be at least") == true ->
                "Password should be at least 6 characters"

            // Network errors
            exception.message?.contains("network error") == true ||
                    exception.message?.contains("timeout") == true ->
                "Network error. Please check your connection"

            // Custom domain validation
            exception.message?.contains("AU email") == true ->
                "Please use your AU email address (@au.edu)"

            // Too many requests
            exception.message?.contains("too many requests") == true ->
                "Too many failed attempts. Please try again later"

            // User disabled
            exception.message?.contains("user disabled") == true ->
                "This account has been disabled. Please contact support"

            // Email not verified
            exception.message?.contains("email not verified") == true ->
                "Please verify your email address before signing in"

            // Default case
            else -> {
                Log.e(TAG, "Unhandled Firebase error: ${exception.message}")
                exception.message ?: "Authentication failed. Please try again"
            }
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