package edu.au.aufondue.api

import android.content.Context
import android.util.Log
import edu.au.aufondue.auth.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Service for fetching user profile pictures from Microsoft Graph API
 * This implementation works with the existing Firebase Auth setup
 */
class ProfilePictureService {
    
    companion object {
        private const val TAG = "ProfilePictureService"
        
        // Microsoft Graph API endpoints for profile pictures
        private const val GRAPH_API_BASE_URL = "https://graph.microsoft.com/v1.0"
        private const val PROFILE_PHOTO_ENDPOINT = "$GRAPH_API_BASE_URL/me/photo/\$value"
        private const val PROFILE_PHOTO_METADATA_ENDPOINT = "$GRAPH_API_BASE_URL/me/photo"
        
        @Volatile
        private var INSTANCE: ProfilePictureService? = null
        
        fun getInstance(): ProfilePictureService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProfilePictureService().also { INSTANCE = it }
            }
        }
    }
    
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .build()
    }
    
    /**
     * Try to get a high-quality profile picture URL
     * This method tries multiple approaches:
     * 1. Microsoft Graph API if access token is available
     * 2. Construct high-quality URL from user ID
     * 3. Fall back to existing Firebase photo URL
     */
    suspend fun getProfilePictureUrl(context: Context): String? {
        return withContext(Dispatchers.IO) {
            val userPreferences = UserPreferences.getInstance(context)
            
            // Try Microsoft Graph API first if we have an access token
            val accessToken = userPreferences.getMicrosoftAccessToken()
            if (accessToken != null) {
                Log.d(TAG, "Attempting to get profile picture via Microsoft Graph API")
                val graphPictureUrl = getMicrosoftGraphPictureUrl(accessToken)
                if (graphPictureUrl != null) {
                    Log.d(TAG, "Successfully got profile picture URL from Microsoft Graph")
                    return@withContext graphPictureUrl
                }
            }
            
            // Fall back to existing Firebase photo URL
            val existingPhotoUrl = userPreferences.getProfilePhotoUrl()
            if (!existingPhotoUrl.isNullOrBlank()) {
                Log.d(TAG, "Using existing Firebase profile photo URL")
                return@withContext existingPhotoUrl
            }
            
            Log.d(TAG, "No profile picture URL available")
            null
        }
    }
    
    /**
     * Get profile picture URL from Microsoft Graph API
     */
    private suspend fun getMicrosoftGraphPictureUrl(accessToken: String): String? {
        return try {
            // First check if a profile photo exists
            val metadataRequest = Request.Builder()
                .url(PROFILE_PHOTO_METADATA_ENDPOINT)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val metadataResponse = httpClient.newCall(metadataRequest).execute()
            
            when (metadataResponse.code) {
                200 -> {
                    // Profile photo exists, return the endpoint URL
                    Log.d(TAG, "Profile photo exists in Microsoft Graph")
                    // We return a special URL that indicates we should use Graph API
                    "graph_api:$PROFILE_PHOTO_ENDPOINT:$accessToken"
                }
                404 -> {
                    Log.d(TAG, "No profile photo found in Microsoft Graph")
                    null
                }
                401 -> {
                    Log.w(TAG, "Access token expired or invalid")
                    null
                }
                else -> {
                    Log.w(TAG, "Unexpected response from Microsoft Graph: ${metadataResponse.code}")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error checking Microsoft Graph profile photo", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Microsoft Graph profile photo", e)
            null
        }
    }
    
    /**
     * Download profile picture bytes from Microsoft Graph API
     */
    suspend fun downloadProfilePictureBytes(context: Context): ByteArray? {
        return withContext(Dispatchers.IO) {
            val accessToken = UserPreferences.getInstance(context).getMicrosoftAccessToken()
            if (accessToken == null) {
                Log.w(TAG, "No access token available for downloading profile picture")
                return@withContext null
            }
            
            try {
                val request = Request.Builder()
                    .url(PROFILE_PHOTO_ENDPOINT)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                when (response.code) {
                    200 -> {
                        val bytes = response.body?.bytes()
                        Log.d(TAG, "Downloaded profile picture, size: ${bytes?.size} bytes")
                        bytes
                    }
                    404 -> {
                        Log.d(TAG, "No profile picture found")
                        null
                    }
                    401 -> {
                        Log.w(TAG, "Access token expired or invalid")
                        null
                    }
                    else -> {
                        Log.w(TAG, "Failed to download profile picture: ${response.code}")
                        null
                    }
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
     * Check if we can use Microsoft Graph API for profile pictures
     */
    fun canUseMicrosoftGraph(context: Context): Boolean {
        val accessToken = UserPreferences.getInstance(context).getMicrosoftAccessToken()
        return !accessToken.isNullOrBlank()
    }
    
    /**
     * Create a custom image request for Coil that can handle Microsoft Graph URLs
     */
    fun createGraphImageRequest(
        context: Context,
        graphUrl: String
    ): coil3.request.ImageRequest? {
        if (!graphUrl.startsWith("graph_api:")) {
            return null
        }
        
        return try {
            val parts = graphUrl.split(":")
            if (parts.size != 3) return null
            
            val endpoint = parts[1]
            // Note: Access token handling would be implemented here for headers
            // but Coil doesn't support custom headers in this simple approach
            
            coil3.request.ImageRequest.Builder(context)
                .data(endpoint)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Graph image request", e)
            null
        }
    }
}