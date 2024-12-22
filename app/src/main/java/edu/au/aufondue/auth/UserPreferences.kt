package edu.au.aufondue.auth

import android.content.Context
import android.content.SharedPreferences

class UserPreferences private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveUserInfo(email: String, username: String) {
        prefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun getUserEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun isLoggedIn(): Boolean = getUserEmail() != null

    fun clearUserInfo() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "user_preferences"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_USERNAME = "username"

        @Volatile
        private var instance: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return instance ?: synchronized(this) {
                instance ?: UserPreferences(context).also { instance = it }
            }
        }
    }
}