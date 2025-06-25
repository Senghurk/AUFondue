// Location: app/src/main/java/edu/au/aufondue/auth/AuthManager.kt
// UPDATE THIS EXISTING FILE - ADD THE LANGUAGE INITIALIZATION

package edu.au.aufondue.auth

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import edu.au.aufondue.api.RetrofitClient
import edu.au.aufondue.utils.LanguageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthManager private constructor(private val activity: Context) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val TAG = "AuthManager"

    // Mock user credentials - you can customize these
    private val mockEmail = "student@au.edu"
    private val mockUsername = "student"

    fun signIn(onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        Log.d(TAG, "Bypassing Microsoft login")

        coroutineScope.launch {
            try {
                // Create/verify user in backend
                val userId = createOrVerifyUser(mockEmail, mockUsername)

                // Save mock user info locally
                withContext(Dispatchers.Main) {
                    UserPreferences.getInstance(activity).saveUserInfo(mockEmail, mockUsername)

                    // Set default language to English on first login
                    val currentLanguage = LanguageManager.getSelectedLanguage(activity)
                    if (currentLanguage.isEmpty()) {
                        LanguageManager.setLocale(activity, LanguageManager.ENGLISH)
                        Log.d(TAG, "Set default language to English")
                    }
                }

                // Use a mock token for MSAL expectations
                val mockToken = "mock-access-token-for-testing"
                onSuccess(mockToken)

                Log.d(TAG, "Mock sign-in successful with account: $mockEmail")
            } catch (e: Exception) {
                Log.e(TAG, "Error in mock sign-in", e)
                onError(e)
            }
        }
    }

    private suspend fun createOrVerifyUser(email: String, username: String): Long? {
        try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Creating/verifying user in backend: $username, $email")
                val response = RetrofitClient.apiService.createOrGetUser(username, email)
                Log.d(TAG, "Response: ${response.code()} ${response.message()}")

                if (!response.isSuccessful) {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Error body: $errorMsg")
                    throw Exception("Failed to create/verify user: ${response.code()} - $errorMsg")
                }

                val responseBody = response.body()
                if (responseBody?.success != true || responseBody.data == null) {
                    throw Exception(responseBody?.message ?: "Failed to create/verify user")
                }

                return@withContext responseBody.data.id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/verifying user", e)
            throw e
        }

        return null
    }

    fun signOut(onComplete: () -> Unit) {
        Log.d(TAG, "Signing out user")
        UserPreferences.getInstance(activity).clearUserInfo()
        onComplete()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(activity: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(activity).also { instance = it }
            }
        }
    }
}