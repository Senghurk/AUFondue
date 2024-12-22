package edu.au.aufondue.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import edu.au.aufondue.R

class AuthManager private constructor(private val activity: Activity) {
    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null

    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            activity.applicationContext,
            R.raw.auth_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    Log.d(TAG, "MSAL application created successfully")

                    // Clear any existing account on initialization
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

    fun signIn(onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        mSingleAccountApp?.let { app ->
            try {
                // Build parameters with minimal required scopes
                val parameters = AcquireTokenParameters.Builder()
                    .startAuthorizationFromActivity(activity)
                    .withScopes(SCOPES.toList())
                    .withPrompt(Prompt.SELECT_ACCOUNT)
                    .withCallback(object : AuthenticationCallback {
                        override fun onSuccess(authenticationResult: IAuthenticationResult) {
                            Log.d(TAG, "Sign in success")
                            onSuccess(authenticationResult.accessToken)
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

    companion object {
        private const val TAG = "AuthManager"
        // Updated minimal set of scopes
        private val SCOPES = arrayOf(
            "openid",
            "profile",
            "User.Read" // Basic Microsoft Graph API scope
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