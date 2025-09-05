package edu.au.unimend.aufondue.screens.login

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.AndroidViewModel
import edu.au.unimend.aufondue.auth.AuthManager
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
    @SuppressLint("StaticFieldLeak")
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
}