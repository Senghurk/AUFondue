package com.example.aufondue.screens.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import com.example.aufondue.screens.profile.ProfileViewModel.Companion.generateRobotAvatarUrl
import kotlin.random.Random

data class ProfileState(
    val userName: String = "",
    val email: String = "",
    val avatarUrl: String = generateRobotAvatarUrl(),
    val notificationsEnabled: Boolean = false,
    val isLoading: Boolean = false
)

class ProfileViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    fun toggleNotifications() {
        _state.value = _state.value.copy(
            notificationsEnabled = !_state.value.notificationsEnabled
        )
    }

    fun updateAvatar() {
        val newAvatarUrl = generateRobotAvatarUrl()
        Log.d("ProfileViewModel", "Updating avatar with URL: $newAvatarUrl")
        _state.value = _state.value.copy(
            avatarUrl = newAvatarUrl
        )
    }

    fun signOut(onSignedOut: () -> Unit) {
        onSignedOut()
    }

    companion object {
        fun generateRobotAvatarUrl(): String {
            val randomSeed = Random.nextInt(1000)
            val timestamp = System.currentTimeMillis()
            return "https://robohash.org/$randomSeed?set=set3&size=200x200&ts=$timestamp"
        }
    }
}