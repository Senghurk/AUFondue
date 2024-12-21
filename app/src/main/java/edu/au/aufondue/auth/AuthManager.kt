package edu.au.aufondue.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.microsoft.identity.client.*
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

                    // Clear any existing accounts
                    application.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                        override fun onAccountLoaded(activeAccount: IAccount?) {
                            activeAccount?.let {
                                application.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                                    override fun onSignOut() {
                                        Log.d(TAG, "Previous account signed out")
                                    }
                                    override fun onError(exception: MsalException) {
                                        Log.e(TAG, "Error signing out previous account", exception)
                                    }
                                })
                            }
                        }
                        override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {}
                        override fun onError(exception: MsalException) {
                            Log.e(TAG, "Error loading current account", exception)
                        }
                    })
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "Failed to create MSAL application", exception)
                }
            }
        )
    }

    fun signIn(onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        mSingleAccountApp?.let { app ->
            // First ensure we're signed out
            app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    // Now proceed with sign in
                    performSignIn(app, onSuccess, onError)
                }
                override fun onError(exception: MsalException) {
                    // Still try to sign in even if sign out fails
                    performSignIn(app, onSuccess, onError)
                }
            })
        } ?: run {
            onError(Exception("MSAL client not initialized"))
        }
    }

    private fun performSignIn(
        app: ISingleAccountPublicClientApplication,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
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
    }

    companion object {
        private const val TAG = "AuthManager"
        private val SCOPES = arrayOf(
            "User.Read",
            "email",
            "profile",
            "openid",
            "offline_access"
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