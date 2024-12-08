package com.example.aufondue.screens.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileState(
    val userName: String = "",
    val email: String = "",
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

    fun signOut(onSignedOut: () -> Unit) {
        // TODO: Implement actual sign out
        onSignedOut()
    }

    fun shareReportLink() {
        // TODO: Implement sharing functionality
    }
}