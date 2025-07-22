package edu.au.aufondue.auth

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import edu.au.aufondue.api.RetrofitClient
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
     * Sign in with Microsoft OAuth through Firebase (like your web app)
     */
    fun signInWithMicrosoft(
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Starting Microsoft OAuth through Firebase")

                // Create Microsoft OAuth provider (same as web app)
                val provider = OAuthProvider.newBuilder("microsoft.com")
                    .setScopes(listOf("mail.read", "openid", "profile"))
                    .build()

                // Check if context is ComponentActivity
                if (context !is ComponentActivity) {
                    onError(Exception("Microsoft login requires Activity context"))
                    return@launch
                }

                // Start Microsoft OAuth flow
                val result = auth.startActivityForSignInWithProvider(context, provider).await()
                val user = result.user

                if (user != null) {
                    Log.d(TAG, "Microsoft OAuth successful, user: ${user.email}")

                    // Validate AU email domain
                    if (user.email?.endsWith("@au.edu") != true) {
                        Log.w(TAG, "Access denied for non-AU account: ${user.email}")
                        auth.signOut()
                        onError(Exception("Access restricted to AU university accounts only"))
                        return@launch
                    }

                    // Get ID token for backend authentication
                    val idToken = user.getIdToken(false).await().token
                    if (idToken != null) {
                        Log.d(TAG, "Got Firebase ID token")

                        // Create/verify user in backend
                        val userId = createOrVerifyUser(user.email!!, user.displayName ?: user.email!!.substringBefore("@"))

                        // Save user info locally
                        withContext(Dispatchers.Main) {
                            UserPreferences.getInstance(context).saveUserInfo(
                                user.email!!,
                                user.displayName ?: user.email!!.substringBefore("@")
                            )
                            onSuccess(userId.toString())
                        }
                    } else {
                        onError(Exception("Failed to get authentication token"))
                    }
                } else {
                    onError(Exception("Authentication failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Microsoft sign-in error", e)
                onError(e)
            }
        }
    }

    /**
     * Create or verify user in backend with Microsoft account info
     */
    private suspend fun createOrVerifyUser(email: String, username: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.createOrGetUser(username, email)

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
            } catch (e: Exception) {
                Log.e(TAG, "Error creating/verifying user", e)
                throw e
            }
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
     * Re-authenticate user with Microsoft
     */
    fun reauthenticateUser(
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        signInWithMicrosoft(
            onSuccess = { onSuccess() },
            onError = onError
        )
    }
}