// Location: app/src/main/java/edu/au/aufondue/screens/profile/ProfileViewModel.kt
// UPDATE THIS EXISTING FILE - REPLACE ALL CONTENT

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
    val selectedLanguage: String = LanguageManager.ENGLISH
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
                    // Get user info from UserPreferences instead of Microsoft account
                    val prefs = UserPreferences.getInstance(getApplication())
                    val email = prefs.getUserEmail() ?: ""
                    val displayName = prefs.getUsername() ?: email.split("@").firstOrNull() ?: ""

                    // Get saved language preference
                    val savedLanguage = LanguageManager.getSelectedLanguage(getApplication())

                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            displayName = displayName,
                            email = email,
                            selectedLanguage = savedLanguage
                        )
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
                    authManager?.signOut(onSignOutComplete)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error signing out", e)
                onSignOutComplete()
            }
        }
    }
}