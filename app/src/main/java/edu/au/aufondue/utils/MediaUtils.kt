package edu.au.aufondue.utils

/**
 * Utility class for media-related operations
 */
object MediaUtils {
    
    // Video file extensions
    private val videoExtensions = setOf(
        "mp4", "avi", "mov", "wmv", "flv", "webm", "mkv", "3gp", "m4v"
    )
    
    // Image file extensions  
    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"
    )
    
    /**
     * Check if the URL points to a video file
     */
    fun isVideoUrl(url: String): Boolean {
        if (url.isBlank()) return false
        
        return try {
            val extension = url.substringAfterLast('.', "").lowercase()
            videoExtensions.contains(extension)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if the URL points to an image file
     */
    fun isImageUrl(url: String): Boolean {
        if (url.isBlank()) return false
        
        return try {
            val extension = url.substringAfterLast('.', "").lowercase()
            imageExtensions.contains(extension)
        } catch (e: Exception) {
            // If we can't determine the extension, assume it's an image (backward compatibility)
            true
        }
    }
    
    /**
     * Get the media type for a URL
     */
    fun getMediaType(url: String): MediaType {
        return when {
            isVideoUrl(url) -> MediaType.VIDEO
            isImageUrl(url) -> MediaType.IMAGE
            else -> MediaType.UNKNOWN
        }
    }
    
    /**
     * Enum representing different media types
     */
    enum class MediaType {
        IMAGE, VIDEO, UNKNOWN
    }
}