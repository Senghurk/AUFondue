package edu.au.aufondue.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import edu.au.aufondue.R
import edu.au.aufondue.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthManager private constructor(private val activity: Activity) {
    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        Log.d(TAG, "Initializing MSAL PublicClientApplication...")
        PublicClientApplication.createSingleAccountPublicClientApplication(
            activity.applicationContext,
            R.raw.auth_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    Log.d(TAG, "MSAL application created successfully")
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "Failed to create MSAL application", exception)
                }
            }
        )
    }

    private suspend fun createOrVerifyUser(email: String, username: String) {
        try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Attempting to create/verify user: $username, $email")
                val response = RetrofitClient.apiService.createOrGetUser(username, email)
                Log.d(TAG, "Response: ${response.code()} ${response.message()}")
                Log.d(TAG, "Body: ${response.body()}")

                if (!response.isSuccessful) {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Error body: $errorMsg")
                    throw Exception("Failed to create/verify user: ${response.code()} - $errorMsg")
                } else if (response.body()?.success == true) {
                    withContext(Dispatchers.Main) {
                        UserPreferences.getInstance(activity).saveUserInfo(email, username)
                    }
                } else {
                    throw Exception(response.body()?.message ?: "Failed to create/verify user")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/verifying user", e)
            throw e
        }
    }

    fun signIn(onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        if (mSingleAccountApp == null) {
            Log.e(TAG, "MSAL client not initialized")
            onError(Exception("MSAL client not initialized"))
            return
        }

        // Step 1: Check if an account is already signed in
        mSingleAccountApp?.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(account: IAccount?) {
                if (account != null) {
                    Log.d(TAG, "Existing account found: ${account.username}. Signing out first.")
                    signOutThenSignIn(onSuccess, onError)
                } else {
                    Log.d(TAG, "No existing account found. Proceeding with sign-in.")
                    proceedWithSignIn(onSuccess, onError)
                }
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                Log.d(TAG, "Account changed: Prior=$priorAccount, Current=$currentAccount")
            }

            override fun onError(exception: MsalException) {
                Log.e(TAG, "Error checking current account", exception)
                onError(exception)
            }
        })
    }

    private fun signOutThenSignIn(onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        mSingleAccountApp?.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                Log.d(TAG, "Successfully signed out. Now proceeding with sign-in.")
                proceedWithSignIn(onSuccess, onError)
            }

            override fun onError(exception: MsalException) {
                Log.e(TAG, "Error signing out", exception)
                onError(exception)
            }
        })
    }

    private fun proceedWithSignIn(onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .withScopes(SCOPES.toList())
            .withPrompt(Prompt.SELECT_ACCOUNT) // Ensures user selects an account
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    Log.d(TAG, "Sign-in successful with account: ${authenticationResult.account.username}")
                    coroutineScope.launch {
                        try {
                            val email = authenticationResult.account.username
                            val name = email.substringBefore("@")

                            createOrVerifyUser(email, name)
                            onSuccess(authenticationResult.accessToken)
                        } catch (e: Exception) {
                            onError(e)
                        }
                    }
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "Sign-in error", exception)
                    onError(exception)
                }

                override fun onCancel() {
                    Log.d(TAG, "Sign-in canceled")
                    onError(Exception("Sign-in canceled"))
                }
            })
            .build()

        mSingleAccountApp?.acquireToken(parameters)
    }

    fun signOut(onComplete: () -> Unit) {
        if (mSingleAccountApp == null) {
            Log.e(TAG, "MSAL client not initialized for sign-out")
            onComplete()
            return
        }

        mSingleAccountApp?.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                Log.d(TAG, "Signed out successfully")
                UserPreferences.getInstance(activity).clearUserInfo()
                onComplete()
            }

            override fun onError(exception: MsalException) {
                Log.e(TAG, "Error signing out", exception)
                onComplete()
            }
        })
    }

    companion object {
        private const val TAG = "AuthManager"
        private val SCOPES = arrayOf("openid", "profile", "User.Read")

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(activity: Activity): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(activity).also { instance = it }
            }
        }
    }
}
