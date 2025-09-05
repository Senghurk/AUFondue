package edu.au.unimend.aufondue.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import edu.au.unimend.aufondue.MainActivity
import edu.au.unimend.aufondue.api.RetrofitClient
import edu.au.unimend.aufondue.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM"
        private const val CHANNEL_ID = "au_fondue_notifications"
        private const val NOTIFICATION_ID = 1
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Handle data payload
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            val issueId = remoteMessage.data["issueId"]
            val updateType = remoteMessage.data["updateType"]
            val status = remoteMessage.data["status"]

            handleDataMessage(issueId, updateType, status)
        }

        // Handle notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(it.title, it.body, remoteMessage.data)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // Send token to your backend server
        sendTokenToServer(token)
    }

    private fun handleDataMessage(issueId: String?, updateType: String?, status: String?) {
        // Create notification based on update type
        val title = when (updateType) {
            "status_update" -> "Issue Status Updated"
            "comment_added" -> "New Comment Added"
            "issue_completed" -> "Issue Completed"
            else -> "Issue Update"
        }

        val body = when (status) {
            "IN_PROGRESS" -> "Your reported issue is now in progress"
            "COMPLETED" -> "Your reported issue has been resolved"
            "REJECTED" -> "Your reported issue has been reviewed"
            else -> "There's an update on your reported issue"
        }

        showNotification(title, body, mapOf("issueId" to (issueId ?: "")))
    }

    private fun showNotification(title: String?, body: String?, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Pass the issueId to navigate to specific issue
            putExtra("issueId", data["issueId"])
            putExtra("navigateToNotifications", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel()

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AU Fondue Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for issue updates"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendTokenToServer(token: String) {
        // Get user email from SharedPreferences or your auth system
        val sharedPref = getSharedPreferences("au_fondue_prefs", Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("user_email", null)

        if (userEmail != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.apiService.updateFcmToken(userEmail, token)
                    if (response.isSuccessful) {
                        Log.d(TAG, "FCM token sent to server successfully")
                    } else {
                        Log.e(TAG, "Failed to send FCM token to server: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending FCM token to server", e)
                }
            }
        }
    }
}