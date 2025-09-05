package edu.au.unimend.aufondue.api.models
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IssueRequest(
    val description: String,
    val category: String,
    val customCategory: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val customLocation: String? = null,
    @Json(name = "isUsingCustomLocation")
    val isUsingCustomLocation: Boolean,
    val userEmail: String,
    val userName: String
)