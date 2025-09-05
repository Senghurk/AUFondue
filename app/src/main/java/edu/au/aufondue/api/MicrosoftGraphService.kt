package edu.au.unimend.aufondue.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

/**
 * Service for interacting with Microsoft Graph API to fetch user profile data
 */
class MicrosoftGraphService {
    
    companion object {
        private const val TAG = "MicrosoftGraphService"
        private const val GRAPH_API_BASE_URL = "https://graph.microsoft.com/v1.0"
        
        // Microsoft Graph API endpoints
        private const val ME_ENDPOINT = "$GRAPH_API_BASE_URL/me"
        private const val PROFILE_PHOTO_ENDPOINT = "$GRAPH_API_BASE_URL/me/photo/\$value"
        private const val PROFILE_PHOTO_METADATA_ENDPOINT = "$GRAPH_API_BASE_URL/me/photo"
    }
    
    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }
    
    /**
     * Get the stored Microsoft access token
     */
    private fun getMicrosoftAccessToken(context: android.content.Context): String? {
        return try {
            val userPrefs = edu.au.unimend.aufondue.auth.UserPreferences.getInstance(context)
            val accessToken = userPrefs.getMicrosoftAccessToken()
            
            if (accessToken != null) {
                Log.d(TAG, "Found stored Microsoft access token")
                accessToken
            } else {
                Log.w(TAG, "No Microsoft access token found in storage")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Microsoft access token", e)
            null
        }
    }
    
    /**
     * Get user profile information from Microsoft Graph
     */
    suspend fun getUserProfile(context: android.content.Context): MicrosoftUserProfile? {
        val accessToken = getMicrosoftAccessToken(context)
        if (accessToken == null) {
            Log.w(TAG, "No access token available")
            return null
        }
        
        return try {
            val request = Request.Builder()
                .url(ME_ENDPOINT)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .build()
                
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "Profile response: $responseBody")
                // Parse the JSON response here
                // For now, return null until we implement proper JSON parsing
                null
            } else {
                Log.e(TAG, "Failed to get profile: ${response.code} - ${response.message}")
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error getting profile", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting profile", e)
            null
        }
    }
    
    /**
     * Get user profile picture URL from Microsoft Graph
     * This method constructs a URL that can be used to fetch the profile picture
     */
    suspend fun getProfilePictureUrl(context: android.content.Context): String? {
        val accessToken = getMicrosoftAccessToken(context)
        if (accessToken == null) {
            Log.w(TAG, "No access token available for profile picture")
            return null
        }
        
        return try {
            // Check if profile photo exists
            val metadataRequest = Request.Builder()
                .url(PROFILE_PHOTO_METADATA_ENDPOINT)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
                
            val metadataResponse = httpClient.newCall(metadataRequest).execute()
            
            if (metadataResponse.isSuccessful) {
                // If metadata request succeeds, photo exists
                Log.d(TAG, "Profile photo exists, returning URL")
                PROFILE_PHOTO_ENDPOINT
            } else if (metadataResponse.code == 404) {
                Log.d(TAG, "No profile photo found")
                null
            } else {
                Log.e(TAG, "Error checking profile photo: ${metadataResponse.code}")
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error getting profile picture URL", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting profile picture URL", e)
            null
        }
    }
    
    /**
     * Get profile picture as byte array
     */
    suspend fun getProfilePictureBytes(context: android.content.Context): ByteArray? {
        val accessToken = getMicrosoftAccessToken(context)
        if (accessToken == null) {
            Log.w(TAG, "No access token available")
            return null
        }
        
        return try {
            val request = Request.Builder()
                .url(PROFILE_PHOTO_ENDPOINT)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
                
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                Log.d(TAG, "Profile picture downloaded, size: ${bytes?.size} bytes")
                bytes
            } else if (response.code == 404) {
                Log.d(TAG, "No profile picture found")
                null
            } else {
                Log.e(TAG, "Failed to download profile picture: ${response.code}")
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error downloading profile picture", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading profile picture", e)
            null
        }
    }
}

/**
 * Data class representing Microsoft user profile
 */
data class MicrosoftUserProfile(
    val id: String,
    val displayName: String?,
    val givenName: String?,
    val surname: String?,
    val userPrincipalName: String?,
    val mail: String?
)