package edu.au.aufondue.screens.profile

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.au.aufondue.auth.AuthManager
import edu.au.aufondue.auth.UserPreferences
import edu.au.aufondue.utils.LanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class ProfileState(
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val notificationsEnabled: Boolean = true,
    val selectedLanguage: String = LanguageManager.ENGLISH,
    val isSignedInWithFirebase: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private var authManager: AuthManager? = null

    init {
        initializeAuth()
    }

    private fun initializeAuth() {
        authManager = AuthManager.getInstance(getApplication())
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val authMgr = authManager
                    val firebaseUser = authMgr?.getCurrentUser()

                    if (firebaseUser != null) {
                        // User is signed in with Firebase
                        val email = firebaseUser.email ?: ""
                        val displayName = firebaseUser.displayName ?: email.split("@").firstOrNull() ?: ""

                        // Also update UserPreferences to keep local data in sync
                        UserPreferences.getInstance(getApplication()).saveUserInfo(email, displayName)

                        // Get saved language preference
                        val savedLanguage = LanguageManager.getSelectedLanguage(getApplication())

                        withContext(Dispatchers.Main) {
                            _state.value = _state.value.copy(
                                displayName = displayName,
                                email = email,
                                selectedLanguage = savedLanguage,
                                isSignedInWithFirebase = true
                            )
                        }
                    } else {
                        // Fall back to UserPreferences (shouldn't happen in normal flow)
                        val prefs = UserPreferences.getInstance(getApplication())
                        val email = prefs.getUserEmail() ?: ""
                        val displayName = prefs.getUsername() ?: ""

                        // Get saved language preference
                        val savedLanguage = LanguageManager.getSelectedLanguage(getApplication())

                        withContext(Dispatchers.Main) {
                            _state.value = _state.value.copy(
                                displayName = displayName,
                                email = email,
                                selectedLanguage = savedLanguage,
                                isSignedInWithFirebase = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading user info", e)
            }
        }
    }

    companion object {
        fun generateRobotAvatarUrl(): String {
            val randomSeed = Random.nextInt(1000)
            val timestamp = System.currentTimeMillis()
            return "https://robohash.org/$randomSeed?set=set4&size=200x200&ts=$timestamp"
        }
    }

    fun updateAvatar() {
        val newAvatarUrl = generateRobotAvatarUrl()
        Log.d("ProfileViewModel", "Updating avatar with URL: $newAvatarUrl")
        _state.value = _state.value.copy(
            avatarUrl = newAvatarUrl
        )
    }

    fun toggleNotifications() {
        _state.value = _state.value.copy(
            notificationsEnabled = !_state.value.notificationsEnabled
        )
    }

    fun changeLanguage(context: Context, languageCode: String) {
        viewModelScope.launch {
            try {
                // Save language preference and update locale
                LanguageManager.setLocale(context, languageCode)

                // Update state
                _state.value = _state.value.copy(
                    selectedLanguage = languageCode
                )

                Log.d("ProfileViewModel", "Language changed to: $languageCode")

                // Recreate activity to apply language changes immediately
                if (context is android.app.Activity) {
                    context.recreate()
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error changing language", e)
            }
        }
    }

    fun signOut(onSignOutComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    authManager?.signOut {
                        // This callback runs after sign out completes
                        onSignOutComplete()
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error signing out", e)
                // Still call the completion callback even if there's an error
                onSignOutComplete()
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                authManager?.deleteAccount(
                    onSuccess = onSuccess,
                    onError = { exception ->
                        onError(exception.message ?: "Failed to delete account")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error deleting account", e)
                onError(e.message ?: "An error occurred while deleting account")
            }
        }
    }
}