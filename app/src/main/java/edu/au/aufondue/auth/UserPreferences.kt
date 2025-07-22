package edu.au.aufondue.auth

import android.content.Context
import android.content.SharedPreferences

/**
 * UserPreferences utility class for managing user-related data in SharedPreferences
 */
class UserPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "au_fondue_user_prefs"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_FCM_TOKEN = "fcm_token"

        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Save user information to SharedPreferences
     */
    fun saveUserInfo(email: String, username: String) {
        prefs.edit()
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USERNAME, username)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    /**
     * Save user ID
     */
    fun saveUserId(userId: Long) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .apply()
    }

    /**
     * Get user email
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Get username
     */
    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    /**
     * Get user ID
     */
    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1L)
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Save FCM token
     */
    fun saveFcmToken(token: String) {
        prefs.edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
    }

    /**
     * Get FCM token
     */
    fun getFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Clear all user information (for logout)
     */
    fun clearUserInfo() {
        prefs.edit()
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USERNAME)
            .remove(KEY_USER_ID)
            .remove(KEY_FCM_TOKEN)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }

    /**
     * Clear all preferences
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}