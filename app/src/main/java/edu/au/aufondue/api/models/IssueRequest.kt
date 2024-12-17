package edu.au.aufondue.api.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IssueRequest(
    val description: String,
    val title: String,        // Added this field
    val category: String,
    val customCategory: String? = null,
    val latitude: Double? = null,      // Changed: moved from LocationData to top level
    val longitude: Double? = null,      // Changed: moved from LocationData to top level
    val customLocation: String? = null,
    val isUsingCustomLocation: Boolean
)

@JsonClass(generateAdapter = true)
data class LocationData(
    val latitude: Double,
    val longitude: Double
)