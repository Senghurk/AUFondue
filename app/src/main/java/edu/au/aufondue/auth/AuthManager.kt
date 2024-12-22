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
        PublicClientApplication.createSingleAccountPublicClientApplication(
            activity.applicationContext,
            R.raw.auth_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    Log.d(TAG, "MSAL application created successfully")
                    clearCurrentAccount()
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "Failed to create MSAL application", exception)
                }
            }
        )
    }

    private fun clearCurrentAccount() {
        mSingleAccountApp?.let { app ->
            try {
                app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                    override fun onSignOut() {
                        Log.d(TAG, "Signed out successfully")
                    }
                    override fun onError(exception: MsalException) {
                        Log.e(TAG, "Error signing out", exception)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error during sign out", e)
            }
        }
    }

    private suspend fun createOrVerifyUser(email: String, username: String) {
        try {
            withContext(Dispatchers.IO) {
                val response = RetrofitClient.apiService.createOrGetUser(username, email)
                if (response.isSuccessful && response.body()?.success == true) {
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
        mSingleAccountApp?.let { app ->
            try {
                val parameters = AcquireTokenParameters.Builder()
                    .startAuthorizationFromActivity(activity)
                    .withScopes(SCOPES.toList())
                    .withPrompt(Prompt.SELECT_ACCOUNT)
                    .withCallback(object : AuthenticationCallback {
                        override fun onSuccess(authenticationResult: IAuthenticationResult) {
                            Log.d(TAG, "Sign in success")
                            coroutineScope.launch {
                                try {
                                    val email = authenticationResult.account.username
                                    val name = authenticationResult.account.username.substringBefore("@")

                                    createOrVerifyUser(email, name)
                                    onSuccess(authenticationResult.accessToken)
                                } catch (e: Exception) {
                                    onError(e)
                                }
                            }
                        }

                        override fun onError(exception: MsalException) {
                            Log.e(TAG, "Sign in error", exception)
                            onError(exception)
                        }

                        override fun onCancel() {
                            Log.d(TAG, "Sign in canceled")
                            onError(Exception("Sign in canceled"))
                        }
                    })
                    .build()

                app.acquireToken(parameters)
            } catch (e: Exception) {
                Log.e(TAG, "Error during sign in", e)
                onError(e)
            }
        } ?: run {
            onError(Exception("MSAL client not initialized"))
        }
    }

    fun signOut(onComplete: () -> Unit) {
        mSingleAccountApp?.let { app ->
            app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    UserPreferences.getInstance(activity).clearUserInfo()
                    onComplete()
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "Error signing out", exception)
                    onComplete()
                }
            })
        } ?: onComplete()
    }

    companion object {
        private const val TAG = "AuthManager"
        private val SCOPES = arrayOf(
            "openid",
            "profile",
            "User.Read"
        )

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