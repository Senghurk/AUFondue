package edu.au.unimend.aufondue.screens.notification

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.au.unimend.aufondue.api.NotificationService
import edu.au.unimend.aufondue.auth.UserPreferences
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
    
    // SharedPreferences for storing read notifications
    private var sharedPreferences: SharedPreferences? = null
    private val PREFS_NAME = "notification_prefs"
    private val READ_NOTIFICATIONS_KEY = "read_notifications"

    // Create notification service
    private val notificationService = NotificationService()

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadNotifications(context: Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Initialize SharedPreferences
                if (sharedPreferences == null) {
                    sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }

                // Get the current user's email
                currentUserEmail = UserPreferences.getInstance(context).getUserEmail()
                Log.d(TAG, "Loading notifications for user: $currentUserEmail")

                if (currentUserEmail == null) {
                    _error.value = "User not logged in"
                    return@launch
                }

                // Get read notification IDs from SharedPreferences
                val readNotificationIds = getReadNotificationIds()

                // Fetch notifications from the service
                val notificationList = notificationService.getNotificationsForUser(currentUserEmail!!)
                    .map { notification ->
                        // Check if this notification was previously marked as read
                        if (readNotificationIds.contains(notification.id)) {
                            notification.copy(isRead = true)
                        } else {
                            notification
                        }
                    }

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

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                // Update the notification in the list to mark it as read
                _notifications.value = _notifications.value.map { notification ->
                    if (notification.id == notificationId) {
                        notification.copy(isRead = true)
                    } else {
                        notification
                    }
                }
                
                // Save to SharedPreferences for persistence
                saveReadNotificationId(notificationId)
                
                // Here you would typically also update this on the backend
                // notificationService.markAsRead(notificationId)
                Log.d(TAG, "Marked notification $notificationId as read")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notification as read", e)
            }
        }
    }
    
    private fun getReadNotificationIds(): Set<String> {
        val key = "${READ_NOTIFICATIONS_KEY}_$currentUserEmail"
        return sharedPreferences?.getStringSet(key, emptySet()) ?: emptySet()
    }
    
    private fun saveReadNotificationId(notificationId: String) {
        val key = "${READ_NOTIFICATIONS_KEY}_$currentUserEmail"
        val currentReadIds = getReadNotificationIds().toMutableSet()
        currentReadIds.add(notificationId)
        sharedPreferences?.edit()?.putStringSet(key, currentReadIds)?.apply()
    }
}