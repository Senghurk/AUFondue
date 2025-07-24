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
import java.time.ZoneId
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
            try {
                val zonedDateTime = ZonedDateTime.parse(value)
                Log.d("LocalDateTimeAdapter", "Parsed ZonedDateTime: $zonedDateTime")
                zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
            } catch (e: DateTimeParseException) {
                try {
                    val localDateTime = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    Log.d("LocalDateTimeAdapter", "Parsed LocalDateTime: $localDateTime")
                    localDateTime
                } catch (e2: DateTimeParseException) {

                    try {
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        LocalDateTime.parse(value, formatter)
                    } catch (e3: Exception) {
                        throw e3
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LocalDateTimeAdapter", "Error parsing date: $value", e)
            LocalDateTime.now()
        }
    }
}

object RetrofitClient {
    private const val TAG = "RetrofitClient"

    //  Fast Switching during development/debugging
    private const val BASE_URL_LOCAL = "http://10.0.2.2:8080/"
    private const val BASE_URL_PROD = "https://aufondue-backend.kindisland-399ef298.southeastasia.azurecontainerapps.io/"

    // Select BASE_URL
    private const val BASE_URL = BASE_URL_PROD

    // Extract domain - image URL fixing
    val DOMAIN = BASE_URL.removeSuffix("/")

    // SAS Token for Azure Blob Storage
    private const val SAS_TOKEN = "sv=2024-11-04&ss=bfqt&srt=co&sp=rwdlacupiytfx&se=2027-07-16T22:11:38Z&st=2025-07-16T13:56:38Z&spr=https,http&sig=5xb1czmfngshEckXBdlhtw%2BVe%2B5htYpCnXyhPw9tnHk%3D"
    private const val STORAGE_URL = "https://aufondueblob.blob.core.windows.net/aufondue/"

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

                if (request.body != null) {
                    Log.d(TAG, "Request has body")
                }

                try {
                    val response = chain.proceed(request)
                    Log.d(TAG, "Got response: ${response.code} for ${request.url}")

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


    fun fixImageUrl(url: String): String {
        Log.d(TAG, "Original image URL: $url")

        if (url.isEmpty()) return ""

        // Azure Blob Storage URL handling
        if (url.contains("blob.core.windows.net")) {
            // Extract the filename from the full URL path
            val fileName = url.substringAfterLast("/")

            // Create direct URL to the blob with SAS token
            val directBlobUrl = "$STORAGE_URL$fileName?$SAS_TOKEN"

            Log.d(TAG, "Generated direct blob URL: $directBlobUrl")
            return directBlobUrl
        }

        // Existing URL handling
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url
        }

        return "$DOMAIN/$url"
    }
}