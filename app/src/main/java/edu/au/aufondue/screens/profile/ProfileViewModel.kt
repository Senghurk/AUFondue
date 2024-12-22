// ProfileViewModel.kt
package edu.au.aufondue.screens.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import edu.au.aufondue.R
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
    val notificationsEnabled: Boolean = true
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null

    init {
        initializeMSAL()
    }

    private fun initializeMSAL() {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            getApplication(),
            R.raw.auth_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    loadUserInfo()
                }

                override fun onError(exception: MsalException) {
                    // Handle error
                }
            }
        )
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    mSingleAccountApp?.getCurrentAccount()?.currentAccount?.let { account ->
                        // Get display name from claims
                        val displayName = account.claims?.get("name") as? String
                            ?: account.claims?.get("given_name") as? String
                            ?: account.username?.split("@")?.first()
                            ?: ""

                        val email = account.username ?: ""

                        withContext(Dispatchers.Main) {
                            _state.value = _state.value.copy(
                                displayName = displayName,
                                email = email
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error
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

    fun signOut(onSignOutComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    mSingleAccountApp?.signOut()
                }
                onSignOutComplete()
            } catch (e: MsalException) {
                // Handle sign out error
            }
        }
    }
}