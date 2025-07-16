package edu.au.aufondue.api

import edu.au.aufondue.api.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/users/create")
    suspend fun createOrGetUser(
        @Query("username") username: String,
        @Query("email") email: String
    ): Response<ApiResponse<UserResponse>>

    @Multipart
    @POST("api/issues")
    suspend fun createIssue(
        @Part("issue") issue: RequestBody,
        @Part photos: List<MultipartBody.Part>
    ): Response<ApiResponse<IssueResponse>>

    @GET("api/issues/user/{userId}/submitted")
    suspend fun getUserSubmittedIssues(
        @Path("userId") userId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<IssueResponse>>>

    @GET("api/issues/tracking")
    suspend fun getAllIssuesTracking(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<IssueResponse>>>

    @GET("api/issues")
    suspend fun getAllIssues(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<IssueResponse>>>

    @GET("api/issues/{id}")
    suspend fun getIssueById(
        @Path("id") id: Long
    ): Response<ApiResponse<IssueResponse>>

    @GET("api/issues/{issueId}/updates")
    suspend fun getIssueUpdates(
        @Path("issueId") issueId: Long
    ): Response<List<UpdateResponse>>

    @GET("api/issues/nearby")
    suspend fun getNearbyIssues(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Double
    ): Response<ApiResponse<List<IssueResponse>>>

    @DELETE("api/issues/{id}")
    suspend fun deleteIssue(
        @Path("id") id: Long
    ): Response<ApiResponse<Void>>

    // Add these methods to your existing ApiService interface
    @POST("api/users/update-fcm-token")
    suspend fun updateFcmToken(
        @Query("email") email: String,
        @Query("fcmToken") fcmToken: String
    ): Response<ApiResponse<String>>

    @DELETE("api/users/remove-fcm-token")
    suspend fun removeFcmToken(
        @Query("email") email: String
    ): Response<ApiResponse<String>>
}