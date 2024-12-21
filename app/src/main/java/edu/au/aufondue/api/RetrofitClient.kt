package edu.au.aufondue.api

import android.annotation.SuppressLint
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class LocalDateTimeAdapter {
    @SuppressLint("NewApi")
    @ToJson
    fun toJson(value: LocalDateTime): String {
        return value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    @SuppressLint("NewApi")
    @FromJson
    fun fromJson(value: String): LocalDateTime {
        return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}

object RetrofitClient {
//    private const val BASE_URL = "http://10.0.2.2:8080/"

    private const val BASE_URL = "https://aufondue-backend.kindisland-399ef298.southeastasia.azurecontainerapps.io/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        // Add timeouts
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

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
}