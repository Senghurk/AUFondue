// ApiResponse.kt
package edu.au.aufondue.api.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)