package edu.au.aufondue.api

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import edu.au.aufondue.api.models.UpdateResponse
import edu.au.aufondue.screens.notification.Notification
import edu.au.aufondue.screens.notification.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/**
 * Service responsible for fetching notifications from the backend API
 */
class NotificationService {
    private val TAG = "NotificationService"

    /**
     * Fetch notifications for a specific user
     *
     * @param userEmail The email of the user for whom to fetch notifications
     * @return List of notifications for the user
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getNotificationsForUser(userEmail: String): List<Notification> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching submitted issues for user: $userEmail")

                // Get user ID from email
                val userId = getUserId(userEmail)
                if (userId == null) {
                    Log.e(TAG, "Could not retrieve user ID for email: $userEmail")
                    return@withContext emptyList<Notification>()
                }

                // Fetch submitted issues for the user
                val response = RetrofitClient.apiService.getUserSubmittedIssues(userId)

                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch user's submitted issues: ${response.code()}")
                    return@withContext emptyList<Notification>()
                }

                val issuesResponse = response.body()
                if (issuesResponse?.success != true || issuesResponse.data == null) {
                    Log.e(TAG, "API error: ${issuesResponse?.message}")
                    return@withContext emptyList<Notification>()
                }

                val userIssues = issuesResponse.data
                Log.d(TAG, "Fetched ${userIssues.size} issues for user")

                // Now fetch updates for each issue and convert to notifications
                val notifications = mutableListOf<Notification>()

                for (issue in userIssues) {
                    try {
                        val updatesResponse = RetrofitClient.apiService.getIssueUpdates(issue.id)

                        if (updatesResponse.isSuccessful) {
                            val updates = updatesResponse.body() ?: emptyList()

                            // Convert each update to a notification
                            updates.forEach { update ->
                                val notification = createNotificationFromUpdate(update, issue.id, userEmail)
                                notifications.add(notification)
                            }
                        } else {
                            Log.e(TAG, "Failed to fetch updates for issue ${issue.id}: ${updatesResponse.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching updates for issue ${issue.id}", e)
                    }
                }

                // Sort by timestamp (most recent first)
                return@withContext notifications.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching notifications", e)
                return@withContext emptyList<Notification>()
            }
        }
    }

    /**
     * Get user ID from email by making an API call
     */
    private suspend fun getUserId(email: String): Long? {
        try {
            // Split email to get username
            val username = email.substringBefore("@")

            // Create or get user
            val response = RetrofitClient.apiService.createOrGetUser(username, email)

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to get user ID: ${response.code()}")
                return null
            }

            val userResponse = response.body()
            if (userResponse?.success != true || userResponse.data == null) {
                Log.e(TAG, "API error: ${userResponse?.message}")
                return null
            }

            return userResponse.data.id
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user ID", e)
            return null
        }
    }

    /**
     * Create a notification object from an update
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationFromUpdate(
        update: UpdateResponse,
        issueId: Long,
        userEmail: String
    ): Notification {
        val notificationType = when (update.status) {
            "COMPLETED" -> NotificationType.ISSUE_RESOLVED
            "IN PROGRESS" -> NotificationType.UPDATE_REQUEST
            else -> NotificationType.NEW_REPORT
        }

        val message = when (update.status) {
            "COMPLETED" -> "Issue resolved, check now"
            "IN PROGRESS" -> "Your issue is in progress"
            else -> update.comment ?: "Update on your report"
        }

        val timestamp = update.updateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return Notification(
            id = "update_${update.id}",
            issueId = issueId,
            sender = "Admin",
            message = message,
            timestamp = timestamp,
            imageUrl = update.photoUrls.firstOrNull(),
            type = notificationType,
            hasAction = true,
            isRead = false,
            userEmail = userEmail
        )
    }
}