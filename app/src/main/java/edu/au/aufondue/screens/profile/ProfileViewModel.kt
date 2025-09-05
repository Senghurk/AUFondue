package edu.au.unimend.aufondue.screens.profile
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.au.unimend.aufondue.auth.AuthManager
import edu.au.unimend.aufondue.auth.UserPreferences
import edu.au.unimend.aufondue.utils.LanguageManager
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
                        // User is signed in with Firebase - get profile data
                        val email = firebaseUser.email ?: ""
                        val displayName = firebaseUser.displayName ?: email.split("@").firstOrNull() ?: ""
                        val profilePhotoUrl = firebaseUser.photoUrl?.toString()

                        // Also update UserPreferences to keep local data in sync
                        UserPreferences.getInstance(getApplication()).saveUserInfo(
                            email = email,
                            username = email.substringBefore("@"),
                            displayName = displayName,
                            profilePhotoUrl = profilePhotoUrl
                        )

                        // Get saved language preference
                        val savedLanguage = LanguageManager.getSelectedLanguage(getApplication())

                        // Try to get high-quality profile picture
                        val profilePictureService = edu.au.unimend.aufondue.api.ProfilePictureService.getInstance()
                        val highQualityProfileUrl = try {
                            profilePictureService.getProfilePictureUrl(getApplication())
                        } catch (e: Exception) {
                            Log.w("ProfileViewModel", "Error getting profile picture", e)
                            null
                        }

                        val finalAvatarUrl = highQualityProfileUrl ?: profilePhotoUrl ?: generateRobotAvatarUrl()

                        withContext(Dispatchers.Main) {
                            _state.value = _state.value.copy(
                                displayName = displayName,
                                email = email,
                                selectedLanguage = savedLanguage,
                                isSignedInWithFirebase = true,
                                avatarUrl = finalAvatarUrl
                            )
                        }
                    } else {
                        // Fall back to UserPreferences
                        val prefs = UserPreferences.getInstance(getApplication())
                        val email = prefs.getUserEmail() ?: ""
                        val displayName = prefs.getDisplayName() ?: prefs.getUsername() ?: ""
                        val profilePhotoUrl = prefs.getProfilePhotoUrl()

                        // Get saved language preference
                        val savedLanguage = LanguageManager.getSelectedLanguage(getApplication())

                        // Try to get high-quality profile picture
                        val profilePictureService = edu.au.unimend.aufondue.api.ProfilePictureService.getInstance()
                        val highQualityProfileUrl = try {
                            profilePictureService.getProfilePictureUrl(getApplication())
                        } catch (e: Exception) {
                            Log.w("ProfileViewModel", "Error getting profile picture", e)
                            null
                        }

                        val finalAvatarUrl = highQualityProfileUrl ?: profilePhotoUrl ?: generateRobotAvatarUrl()

                        withContext(Dispatchers.Main) {
                            _state.value = _state.value.copy(
                                displayName = displayName,
                                email = email,
                                selectedLanguage = savedLanguage,
                                isSignedInWithFirebase = false,
                                avatarUrl = finalAvatarUrl
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
        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "Updating avatar...")
                
                // Try to get the latest profile picture from Microsoft Graph
                val profilePictureService = edu.au.unimend.aufondue.api.ProfilePictureService.getInstance()
                val profileUrl = profilePictureService.getProfilePictureUrl(getApplication())
                
                val newAvatarUrl = if (!profileUrl.isNullOrBlank()) {
                    Log.d("ProfileViewModel", "Using profile picture from Microsoft Graph")
                    profileUrl
                } else {
                    Log.d("ProfileViewModel", "Generating new robot avatar")
                    generateRobotAvatarUrl()
                }
                
                _state.value = _state.value.copy(avatarUrl = newAvatarUrl)
                
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating avatar", e)
                // Fall back to generating a new robot avatar
                val newAvatarUrl = generateRobotAvatarUrl()
                _state.value = _state.value.copy(avatarUrl = newAvatarUrl)
            }
        }
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
                // Perform sign out on IO thread
                withContext(Dispatchers.IO) {
                    authManager?.signOut {
                        // Switch back to Main thread for UI updates
                        viewModelScope.launch(Dispatchers.Main) {
                            onSignOutComplete()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error signing out", e)
                // Still call the completion callback even if there's an error
                viewModelScope.launch(Dispatchers.Main) {
                    onSignOutComplete()
                }
            }
        }
    }
}