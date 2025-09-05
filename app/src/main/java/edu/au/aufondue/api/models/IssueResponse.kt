package edu.au.unimend.aufondue.api.models
import com.squareup.moshi.JsonClass
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class IssueResponse(
    val id: Long,
    val description: String,
    val category: String,
    val customCategory: String?,
    val customLocation: String?,
    val status: String,
    val photoUrls: List<String>,
    val videoUrls: List<String> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val reportedBy: UserResponse?,
    val usingCustomLocation: Boolean = false
)

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String
)