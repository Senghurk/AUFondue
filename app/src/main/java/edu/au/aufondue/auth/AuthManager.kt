package edu.au.aufondue.auth

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import edu.au.aufondue.R
import edu.au.aufondue.api.RetrofitClient
import edu.au.aufondue.utils.LanguageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthManager private constructor(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private var msalApp: IMultipleAccountPublicClientApplication? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val TAG = "AuthManager"
        private var instance: AuthManager? = null

        // University domain validation
        private const val REQUIRED_DOMAIN = "@au.edu"

        // Microsoft OAuth scopes
        private val SCOPES = arrayOf(
            "User.Read",
            "email",
            "openid",
            "profile"
        )

        @Synchronized
        fun getInstance(context: Context): AuthManager {
            return instance ?: AuthManager(context.applicationContext).also { instance = it }
        }
    }

    init {
        initializeMSAL()
    }

    /**
     * Initialize Microsoft Authentication Library (MSAL)
     */
    private fun initializeMSAL() {
        try {
            // Check if auth config file exists first
            val authConfigResource = context.resources.openRawResource(R.raw.auth_config)
            authConfigResource.close()

            PublicClientApplication.createMultipleAccountPublicClientApplication(
                context,
                R.raw.auth_config, // MSAL configuration file
                object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                    override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                        msalApp = application
                        Log.d(TAG, "MSAL initialized successfully")
                    }

                    override fun onError(exception: MsalException) {
                        Log.e(TAG, "Failed to initialize MSAL", exception)
                        // Create a fallback error handler
                        msalApp = null
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MSAL - auth_config.json not found or invalid", e)
            Log.e(TAG, "Make sure auth_config.json is generated in res/raw/ directory")
            msalApp = null
        }
    }

    /**
     * Sign in with Microsoft account
     */
    fun signInWithMicrosoft(
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val app = msalApp
        if (app == null) {
            onError(Exception("MSAL not initialized. Please try again."))
            return
        }

        // Ensure we're running on a FragmentActivity
        if (context !is FragmentActivity) {
            onError(Exception("Microsoft authentication requires FragmentActivity context"))
            return
        }

        coroutineScope.launch {
            try {
                // Check if there's an existing account
                val accounts = app.accounts
                val accountToUse = accounts.firstOrNull()

                val result = if (accountToUse != null) {
                    // Silent authentication for existing account using newer API
                    Log.d(TAG, "Attempting silent authentication")
                    val silentParameters = AcquireTokenSilentParameters.Builder()
                        .withScopes(SCOPES.toList())
                        .forAccount(accountToUse)
                        .build()
                    app.acquireTokenSilent(silentParameters)
                } else {
                    // Interactive authentication for new account
                    Log.d(TAG, "Attempting interactive authentication")

                    // Create acquire token parameters
                    val parameters = AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(context)
                        .withScopes(SCOPES.toList())
                        .withCallback(object : AuthenticationCallback {
                            override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                                authenticationResult?.let { result ->
                                    handleMsalSuccess(result, onSuccess, onError)
                                } ?: onError(Exception("Authentication result is null"))
                            }

                            override fun onError(exception: MsalException?) {
                                Log.e(TAG, "MSAL authentication failed", exception)
                                onError(exception ?: Exception("Unknown MSAL error"))
                            }

                            override fun onCancel() {
                                onError(Exception("Authentication was cancelled"))
                            }
                        })
                        .build()

                    app.acquireToken(parameters)
                    return@launch // Exit here as callback will handle the result
                }

                // Handle silent authentication result
                handleMsalSuccess(result, onSuccess, onError)

            } catch (e: MsalException) {
                Log.e(TAG, "MSAL authentication failed", e)
                when (e.errorCode) {
                    "cancelled" -> onError(Exception("Authentication was cancelled"))
                    "no_network" -> onError(Exception("No network connection available"))
                    else -> onError(Exception("Microsoft authentication failed: ${e.message}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Microsoft sign-in error", e)
                onError(e)
            }
        }
    }

    /**
     * Handle successful MSAL authentication
     */
    private fun handleMsalSuccess(
        result: IAuthenticationResult,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        coroutineScope.launch {
            try {
                // Validate the email domain
                val email = result.account.username
                if (!email.endsWith(REQUIRED_DOMAIN)) {
                    onError(Exception("Please use your university Microsoft account (@au.edu)"))
                    return@launch
                }

                // Sign in to Firebase using Microsoft OAuth token
                val accessToken = result.accessToken
                signInToFirebaseWithMicrosoft(accessToken, email, onSuccess, onError)

            } catch (e: Exception) {
                Log.e(TAG, "Error processing MSAL result", e)
                onError(e)
            }
        }
    }

    /**
     * Sign in to Firebase using Microsoft OAuth token
     * For Android MSAL + Firebase integration, we'll use a different approach
     */
    private suspend fun signInToFirebaseWithMicrosoft(
        accessToken: String,
        email: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            // For Android MSAL integration, we'll create a custom token approach
            // or use the MSAL result directly without Firebase OAuth

            // Option 1: Skip Firebase OAuth and use MSAL token directly
            // This is more appropriate for mobile apps using MSAL

            val displayName = email.substringBefore("@")

            // Create/verify user in backend using the MSAL access token
            createOrVerifyUser(email, displayName)

            // Save user info locally
            withContext(Dispatchers.Main) {
                UserPreferences.getInstance(context).saveUserInfo(
                    email,
                    displayName
                )

                // Set default language to English on first login
                val currentLanguage = LanguageManager.getSelectedLanguage(context)
                if (currentLanguage.isEmpty()) {
                    LanguageManager.setLocale(context, LanguageManager.ENGLISH)
                    Log.d(TAG, "Set default language to English")
                }
            }

            // Use the MSAL access token as our authentication token
            onSuccess(accessToken)

        } catch (e: Exception) {
            Log.e(TAG, "Microsoft authentication processing error", e)
            onError(e)
        }
    }

    /**
     * Sign out from both MSAL and Firebase
     */
    fun signOut(onComplete: () -> Unit) {
        coroutineScope.launch {
            try {
                // Sign out from Firebase
                auth.signOut()

                // Sign out from MSAL
                val app = msalApp
                if (app != null) {
                    val accounts = app.accounts
                    for (account in accounts) {
                        app.removeAccount(account)
                    }
                }

                // Clear local user preferences
                UserPreferences.getInstance(context).clearUserInfo()

                Log.d(TAG, "Sign out successful")
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Sign out error", e)
                onComplete() // Still complete even on error
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
     * Get current user's email
     */
    fun getCurrentUserEmail(): String? = getCurrentUser()?.email

    /**
     * Get current user's display name
     */
    fun getCurrentUserDisplayName(): String? = getCurrentUser()?.displayName

    /**
     * Create or verify user in backend
     */
    private suspend fun createOrVerifyUser(email: String, username: String) {
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
                if (responseBody?.success != true) {
                    throw Exception(responseBody?.message ?: "Failed to create/verify user")
                }

                Log.d(TAG, "User verified successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backend user verification failed", e)
            // Don't throw here - continue with local authentication even if backend fails
        }
    }

    /**
     * Sign in with email and password (disabled - Microsoft only)
     */
    @Suppress("UNUSED_PARAMETER")
    fun signInWithEmailPassword(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        onError(Exception("Please use Microsoft authentication"))
    }

    /**
     * Create account with email and password (disabled - Microsoft only)
     */
    @Suppress("UNUSED_PARAMETER")
    fun createAccountWithEmailPassword(
        email: String,
        password: String,
        displayName: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        onError(Exception("Account creation is disabled. Please use Microsoft authentication"))
    }

    /**
     * Send password reset email (disabled - Microsoft only)
     */
    @Suppress("UNUSED_PARAMETER")
    fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        onError(Exception("Password reset not available. Please contact IT support"))
    }

    /**
     * Delete account (simplified to sign out)
     */
    fun deleteAccount(
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        signOut {
            onSuccess()
        }
    }
}