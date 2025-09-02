package edu.au.aufondue.api.models
import com.squareup.moshi.JsonClass
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class UpdateResponse(
    val id: Long,
    val issueId: Long,
    val status: String,
    val comment: String?,
    val updateTime: LocalDateTime,
    val photoUrls: List<String>,
    val videoUrls: List<String> = emptyList()
)