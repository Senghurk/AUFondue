package edu.au.aufondue.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IssueRequest(
    val description: String,
    val category: String,
    val customCategory: String? = null,
    val latitude: Double? = null,      // Changed: moved from LocationData to top level
    val longitude: Double? = null,      // Changed: moved from LocationData to top level
    val customLocation: String? = null,
    @Json(name = "isUsingCustomLocation") // ensure exact property name match
    val isUsingCustomLocation: Boolean
)

@JsonClass(generateAdapter = true)
data class LocationData(
    val latitude: Double,
    val longitude: Double
)