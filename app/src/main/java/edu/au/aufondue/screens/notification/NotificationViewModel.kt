// NotificationViewModel.kt
package edu.au.aufondue.screens.notification

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Notification(
    val id: String,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val imageUrl: String? = null,
    val type: NotificationType,
    val hasAction: Boolean = false,
    val isRead: Boolean = false
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

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        // Simulated notifications data
        _notifications.value = listOf(
            Notification(
                id = "1",
                sender = "Admin",
                message = "Issue resolved, check now",
                timestamp = System.currentTimeMillis(),
                imageUrl = "map_image_url",
                type = NotificationType.NEW_REPORT
            ),
            Notification(
                id = "2",
                sender = "Admin",
                message = "Issue resolved, check now",
                timestamp = System.currentTimeMillis(),
                imageUrl = null,
                type = NotificationType.ISSUE_RESOLVED,
                hasAction = true
            ),
            Notification(
                id = "3",
                sender = "Admin",
                message = "Please review the update",
                timestamp = System.currentTimeMillis() - 86400000,
                imageUrl = "map_image_url",
                type = NotificationType.UPDATE_REQUEST
            ),
            Notification(
                id = "4",
                sender = "Admin",
                message = "Issue update shared",
                timestamp = System.currentTimeMillis() - 86400000,
                imageUrl = "maintenance_image_url",
                type = NotificationType.SHARE
            ),
            Notification(
                id = "5",
                sender = "Admin",
                message = "Issue resolved successfully",
                timestamp = System.currentTimeMillis() - 172800000,
                imageUrl = "success_icon_url",
                type = NotificationType.SUCCESS
            ),
            Notification(
                id = "6",
                sender = "Admin",
                message = "Please review and respond",
                timestamp = System.currentTimeMillis() - 172800000,
                imageUrl = "review_image_url",
                type = NotificationType.UPDATE_REQUEST
            )
        )
    }
}