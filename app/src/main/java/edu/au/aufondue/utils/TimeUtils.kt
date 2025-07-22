package edu.au.aufondue.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtils {

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatTimeAgo(serverDateTime: LocalDateTime): String {
        return try {
            // CRITICAL FIX: Properly handle server time conversion
            // The server timestamp is already in the correct timezone from LocalDateTimeAdapter
            // Convert to Instant for accurate duration calculation
            val serverInstant = serverDateTime.atZone(ZoneId.systemDefault()).toInstant()
            val currentInstant = Instant.now()

            val duration = Duration.between(serverInstant, currentInstant)

            val seconds = duration.seconds
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            when {
                seconds < 0 -> "Just now" // Handle edge case where server time might be slightly ahead
                seconds < 60 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days < 30 -> "${days}d ago"
                else -> {
                    // For older than a month, format as date
                    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                    serverDateTime.format(formatter)
                }
            }
        } catch (e: Exception) {
            // Log the error but provide a fallback
            android.util.Log.e("TimeUtils", "Error formatting time ago for: $serverDateTime", e)
            "Unknown time"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatTimeAgoDetailed(serverDateTime: LocalDateTime): String {
        return try {
            val serverInstant = serverDateTime.atZone(ZoneId.systemDefault()).toInstant()
            val currentInstant = Instant.now()

            val duration = Duration.between(serverInstant, currentInstant)

            val seconds = duration.seconds
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            when {
                seconds < 0 -> "Just now"
                seconds < 60 -> "Just now"
                minutes == 1L -> "1 minute ago"
                minutes < 60 -> "${minutes} minutes ago"
                hours == 1L -> "1 hour ago"
                hours < 24 -> "${hours} hours ago"
                days == 1L -> "1 day ago"
                days < 30 -> "${days} days ago"
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                    serverDateTime.format(formatter)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TimeUtils", "Error formatting detailed time ago for: $serverDateTime", e)
            "Unknown time"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDateTime(dateTime: LocalDateTime): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
            dateTime.format(formatter)
        } catch (e: Exception) {
            android.util.Log.e("TimeUtils", "Error formatting datetime: $dateTime", e)
            "Invalid date"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDate(dateTime: LocalDateTime): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            dateTime.format(formatter)
        } catch (e: Exception) {
            android.util.Log.e("TimeUtils", "Error formatting date: $dateTime", e)
            "Invalid date"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatTime(dateTime: LocalDateTime): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            dateTime.format(formatter)
        } catch (e: Exception) {
            android.util.Log.e("TimeUtils", "Error formatting time: $dateTime", e)
            "Invalid time"
        }
    }

    // Helper function to check if a timestamp is recent (within last 5 minutes)
    @RequiresApi(Build.VERSION_CODES.O)
    fun isRecent(serverDateTime: LocalDateTime): Boolean {
        return try {
            val serverInstant = serverDateTime.atZone(ZoneId.systemDefault()).toInstant()
            val currentInstant = Instant.now()
            val duration = Duration.between(serverInstant, currentInstant)
            duration.toMinutes() <= 5
        } catch (e: Exception) {
            false
        }
    }

    // Helper function to get the duration between server time and current time
    @RequiresApi(Build.VERSION_CODES.O)
    fun getDurationSinceServer(serverDateTime: LocalDateTime): Duration? {
        return try {
            val serverInstant = serverDateTime.atZone(ZoneId.systemDefault()).toInstant()
            val currentInstant = Instant.now()
            Duration.between(serverInstant, currentInstant)
        } catch (e: Exception) {
            android.util.Log.e("TimeUtils", "Error calculating duration since server time: $serverDateTime", e)
            null
        }
    }
}

// Extension function for easier usage
@RequiresApi(Build.VERSION_CODES.O)
fun LocalDateTime.formatTimeAgo(): String = TimeUtils.formatTimeAgo(this)

@RequiresApi(Build.VERSION_CODES.O)
fun LocalDateTime.formatTimeAgoDetailed(): String = TimeUtils.formatTimeAgoDetailed(this)

@RequiresApi(Build.VERSION_CODES.O)
fun LocalDateTime.formatDateTime(): String = TimeUtils.formatDateTime(this)

@RequiresApi(Build.VERSION_CODES.O)
fun LocalDateTime.formatDate(): String = TimeUtils.formatDate(this)

@RequiresApi(Build.VERSION_CODES.O)
fun LocalDateTime.formatTime(): String = TimeUtils.formatTime(this)

@RequiresApi(Build.VERSION_CODES.O)
fun LocalDateTime.isRecent(): Boolean = TimeUtils.isRecent(this)