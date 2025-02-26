package edu.au.aufondue.screens.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.au.aufondue.api.RetrofitClient
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
    val userEmail: String  // Added to track which user this notification is for
)

enum class NotificationType {
    NEW_REPORT,
    ISSUE_RESOLVED,
    UPDATE_REQUEST,
    SHARE,
    SUCCESS
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

    fun loadNotifications(context: android.content.Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Get the current user's email
                currentUserEmail = UserPreferences.getInstance(context).getUserEmail()
                Log.d(TAG, "Loading notifications for user: $currentUserEmail")

                if (currentUserEmail == null) {
                    _error.value = "User not logged in"
                    return@launch
                }

                // In a real application, we would fetch notifications from an API
                // For now, we'll use simulated data filtered by the current user's email
                val allNotifications = getSimulatedNotifications()

                // Filter notifications to only show those for the current user
                val filteredNotifications = allNotifications.filter {
                    it.userEmail == currentUserEmail
                }

                Log.d(TAG, "Found ${filteredNotifications.size} notifications for user")
                _notifications.value = filteredNotifications
                _error.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notifications", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * In a real application, this would be replaced with an API call.
     * For now, we simulate notifications for different users.
     */
    private fun getSimulatedNotifications(): List<Notification> {
        return listOf(
            // Notifications for u6440041@au.edu
            Notification(
                id = "1",
                issueId = 39L,
                sender = "Admin",
                message = "Issue resolved, check now",
                timestamp = System.currentTimeMillis(),
                imageUrl = null,
                type = NotificationType.ISSUE_RESOLVED,
                userEmail = "u6440041@au.edu"
            ),
            Notification(
                id = "2",
                issueId = 40L,
                sender = "Admin",
                message = "Your issue is in progress",
                timestamp = System.currentTimeMillis() - 86400000,
                imageUrl = null,
                type = NotificationType.UPDATE_REQUEST,
                userEmail = "u6440041@au.edu"
            ),

            // Notifications for another user
            Notification(
                id = "3",
                issueId = 41L,
                sender = "Admin",
                message = "Issue update for another user",
                timestamp = System.currentTimeMillis(),
                imageUrl = null,
                type = NotificationType.UPDATE_REQUEST,
                userEmail = "another@au.edu"
            ),

            // More notifications for u6440041@au.edu
            Notification(
                id = "4",
                issueId = 39L,
                sender = "Admin",
                message = "Issue resolved successfully",
                timestamp = System.currentTimeMillis() - 172800000,
                imageUrl = null,
                type = NotificationType.SUCCESS,
                userEmail = "u6440041@au.edu"
            )
        )
    }

    fun clearError() {
        _error.value = null
    }
}