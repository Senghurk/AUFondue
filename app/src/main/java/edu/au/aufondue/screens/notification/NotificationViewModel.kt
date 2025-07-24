package edu.au.aufondue.screens.notification

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.au.aufondue.api.NotificationService
import edu.au.aufondue.auth.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Notification(
    val id: String,
    val issueId: Long,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val imageUrl: String? = null,
    val type: NotificationType,
    val hasAction: Boolean = false,
    val isRead: Boolean = false,
    val userEmail: String  // Tracks which user this notification is for
)

enum class NotificationType {
    NEW_REPORT,
    ISSUE_RESOLVED,
    UPDATE_REQUEST,
}

class NotificationViewModel : ViewModel() {
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Store the current user's email
    private var currentUserEmail: String? = null
    private val TAG = "NotificationViewModel"

    // Create notification service
    private val notificationService = NotificationService()

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadNotifications(context: Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Get the current user's email
                currentUserEmail = UserPreferences.getInstance(context).getUserEmail()
                Log.d(TAG, "Loading notifications for user: $currentUserEmail")

                if (currentUserEmail == null) {
                    _error.value = "User not logged in"
                    return@launch
                }

                // Fetch notifications from the service
                val notificationList = notificationService.getNotificationsForUser(currentUserEmail!!)

                if (notificationList.isEmpty()) {
                    Log.d(TAG, "No notifications found for user")
                } else {
                    Log.d(TAG, "Found ${notificationList.size} notifications for user")
                }

                _notifications.value = notificationList
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notifications", e)
                _error.value = e.message ?: "Failed to load notifications"
            } finally {
                _isLoading.value = false
            }
        }
    }
}