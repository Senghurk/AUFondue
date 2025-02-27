package edu.au.aufondue.api

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

class LocalDateTimeAdapter {
    @RequiresApi(Build.VERSION_CODES.O)
    @ToJson
    fun toJson(value: LocalDateTime): String {
        return value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @FromJson
    fun fromJson(value: String): LocalDateTime {
        return try {
            // First try to parse as ISO datetime with timezone
            try {
                ZonedDateTime.parse(value).toLocalDateTime()
            } catch (e: DateTimeParseException) {
                // If that fails, try parsing as local datetime
                LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }
        } catch (e: Exception) {
            Log.e("LocalDateTimeAdapter", "Error parsing date: $value", e)
            // Fallback to current time if parsing fails
            LocalDateTime.now()
        }
    }
}

object RetrofitClient {
    private const val TAG = "RetrofitClient"

    // Keep both URLs for easy switching during development/debugging
    private const val BASE_URL_LOCAL = "http://10.0.2.2:8080/"
    private const val BASE_URL_PROD = "https://aufondue-backend.kindisland-399ef298.southeastasia.azurecontainerapps.io/"

    // Select which URL to use
    private const val BASE_URL = BASE_URL_PROD

    // Extract domain for image URL fixing
    val DOMAIN = BASE_URL.removeSuffix("/")

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient by lazy {
        createOkHttpClient()
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                Log.d(TAG, "Making request: ${request.method} ${request.url}")

                // Add detailed request logging
                if (request.body != null) {
                    Log.d(TAG, "Request has body")
                }

                try {
                    val response = chain.proceed(request)
                    Log.d(TAG, "Got response: ${response.code} for ${request.url}")

                    // Log response details for debugging
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        Log.e(TAG, "Error response: $errorBody")
                        // Create a new response with the consumed body
                        return@addInterceptor response.newBuilder()
                            .body(ResponseBody.create(response.body?.contentType(), errorBody ?: ""))
                            .build()
                    }

                    response
                } catch (e: Exception) {
                    Log.e(TAG, "Error during API request to ${request.url}", e)
                    throw e
                }
            }
            // Increase timeouts for large file uploads
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .add(LocalDateTimeAdapter())
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    /**
     * Fixes image URL issues by ensuring they have the proper domain
     */
    fun fixImageUrl(url: String): String {
        Log.d(TAG, "Original image URL: $url")

        if (url.isEmpty()) {
            return ""
        }

        // Fix Azure Blob Storage URLs
        if (url.contains("blob.core.windows.net")) {
            val sanitizedUrl = url.replace(" ", "%20").trim()
            Log.d(TAG, "Sanitized Azure blob URL: $sanitizedUrl")
            return sanitizedUrl
        }

        // If the URL already has http/https, it's complete
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url
        }

        // If it starts with a slash, append to domain
        if (url.startsWith("/")) {
            return "$DOMAIN$url"
        }

        // Otherwise, assume it's a relative path
        return "$DOMAIN/$url"
    }
}