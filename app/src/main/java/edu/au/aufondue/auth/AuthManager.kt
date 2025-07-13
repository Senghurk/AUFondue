package edu.au.aufondue.auth

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import edu.au.aufondue.api.RetrofitClient
import edu.au.aufondue.utils.LanguageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthManager private constructor(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val TAG = "AuthManager"

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Get current Firebase user
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Check if user is currently signed in
     */
    fun isSignedIn(): Boolean = getCurrentUser() != null

    /**
     * Sign in with email and password
     */
    fun signInWithEmailPassword(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        coroutineScope.launch {
            try {
                // Validate email domain
                if (!email.endsWith("@au.edu")) {
                    onError(Exception("Only AU email addresses are allowed"))
                    return@launch
                }

                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user

                if (user != null) {
                    val idToken = user.getIdToken(false).await().token
                    if (idToken != null) {
                        // Create/verify user in backend
                        createOrVerifyUser(email, user.displayName ?: email.substringBefore("@"))

                        // Save user info locally
                        withContext(Dispatchers.Main) {
                            UserPreferences.getInstance(context).saveUserInfo(
                                email,
                                user.displayName ?: email.substringBefore("@")
                            )

                            // Set default language to English on first login
                            val currentLanguage = LanguageManager.getSelectedLanguage(context)
                            if (currentLanguage.isEmpty()) {
                                LanguageManager.setLocale(context, LanguageManager.ENGLISH)
                                Log.d(TAG, "Set default language to English")
                            }
                        }

                        onSuccess(idToken)
                    } else {
                        onError(Exception("Failed to get authentication token"))
                    }
                } else {
                    onError(Exception("Sign in failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Email/password sign in error", e)
                onError(e)
            }
        }
    }

    /**
     * Create account with email and password
     */
    fun createAccountWithEmailPassword(
        email: String,
        password: String,
        displayName: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        coroutineScope.launch {
            try {
                // Validate email domain
                if (!email.endsWith("@au.edu")) {
                    onError(Exception("Only AU email addresses are allowed"))
                    return@launch
                }

                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user

                if (user != null) {
                    // Update user profile with display name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()

                    user.updateProfile(profileUpdates).await()

                    val idToken = user.getIdToken(false).await().token
                    if (idToken != null) {
                        // Create/verify user in backend
                        createOrVerifyUser(email, displayName)

                        // Save user info locally
                        withContext(Dispatchers.Main) {
                            UserPreferences.getInstance(context).saveUserInfo(email, displayName)

                            // Set default language to English on first login
                            LanguageManager.setLocale(context, LanguageManager.ENGLISH)
                            Log.d(TAG, "Set default language to English")
                        }

                        onSuccess(idToken)
                    } else {
                        onError(Exception("Failed to get authentication token"))
                    }
                } else {
                    onError(Exception("Account creation failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Account creation error", e)
                onError(e)
            }
        }
    }

    /**
     * Send password reset email
     */
    fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        coroutineScope.launch {
            try {
                if (!email.endsWith("@au.edu")) {
                    onError(Exception("Only AU email addresses are allowed"))
                    return@launch
                }

                auth.sendPasswordResetEmail(email).await()
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Password reset error", e)
                onError(e)
            }
        }
    }

    /**
     * Update user profile
     */
    fun updateUserProfile(
        displayName: String? = null,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        coroutineScope.launch {
            try {
                val user = getCurrentUser()
                if (user != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()

                    displayName?.let {
                        profileUpdates.setDisplayName(it)
                    }

                    user.updateProfile(profileUpdates.build()).await()

                    // Update local preferences
                    UserPreferences.getInstance(context).saveUserInfo(
                        user.email ?: "",
                        displayName ?: user.displayName ?: ""
                    )

                    onSuccess()
                } else {
                    onError(Exception("No user signed in"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Profile update error", e)
                onError(e)
            }
        }
    }

    /**
     * Change user password
     */
    fun changePassword(
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        coroutineScope.launch {
            try {
                val user = getCurrentUser()
                if (user != null) {
                    user.updatePassword(newPassword).await()
                    onSuccess()
                } else {
                    onError(Exception("No user signed in"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Password change error", e)
                onError(e)
            }
        }
    }

    /**
     * Create or verify user in backend
     */
    private suspend fun createOrVerifyUser(email: String, username: String): Long? {
        return try {
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

                responseBody.data.id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/verifying user", e)
            throw e
        }
    }

    /**
     * Sign out user
     */
    fun signOut(onComplete: () -> Unit) {
        Log.d(TAG, "Signing out user")

        // Sign out from Firebase
        auth.signOut()

        // Clear local user info
        UserPreferences.getInstance(context).clearUserInfo()
        onComplete()
    }

    /**
     * Delete user account
     */
    fun deleteAccount(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        coroutineScope.launch {
            try {
                val user = getCurrentUser()
                if (user != null) {
                    // Delete from Firebase
                    user.delete().await()

                    // Clear local user info
                    UserPreferences.getInstance(context).clearUserInfo()
                    onSuccess()
                } else {
                    onError(Exception("No user to delete"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting account", e)
                onError(e)
            }
        }
    }

    /**
     * Re-authenticate user (required for sensitive operations)
     */
    fun reauthenticateUser(
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        coroutineScope.launch {
            try {
                val user = getCurrentUser()
                if (user?.email != null) {
                    val credential = com.google.firebase.auth.EmailAuthProvider
                        .getCredential(user.email!!, password)

                    user.reauthenticate(credential).await()
                    onSuccess()
                } else {
                    onError(Exception("No user signed in"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Re-authentication error", e)
                onError(e)
            }
        }
    }
}