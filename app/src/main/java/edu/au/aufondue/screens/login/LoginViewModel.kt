package edu.au.aufondue.screens.login

import android.app.Application
import android.util.Log
import androidx.activity.ComponentActivity
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
    private var activity: ComponentActivity? = null

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
     * Set the activity context for Microsoft OAuth
     */
    fun setActivity(activity: ComponentActivity) {
        this.activity = activity
    }

    /**
     * Sign in with Microsoft OAuth
     */
    fun signInWithMicrosoft() {
        val authMgr = authManager
        if (authMgr == null) {
            _state.value = _state.value.copy(
                error = "Authentication service not initialized"
            )
            return
        }

        if (activity == null) {
            _state.value = _state.value.copy(
                error = "Activity context required for Microsoft login"
            )
            return
        }

        _state.value = _state.value.copy(
            isLoading = true,
            error = null
        )

        authMgr.signInWithMicrosoft(
            activity = activity!!,
            onSuccess = { userId ->
                Log.d(TAG, "Microsoft sign-in successful, user ID: $userId")
                _state.value = _state.value.copy(
                    isLoading = false,
                    loginSuccess = true
                )
            },
            onError = { exception ->
                Log.e(TAG, "Microsoft sign-in failed", exception)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Microsoft sign-in failed"
                )
            }
        )
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
     * Toggle between sign in and sign up modes
     */
    fun toggleSignUpMode() {
        _state.value = _state.value.copy(
            isSignUp = !_state.value.isSignUp,
            error = null
        )
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}