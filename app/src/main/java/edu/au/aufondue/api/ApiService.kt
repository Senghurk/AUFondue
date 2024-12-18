package edu.au.aufondue.api

import edu.au.aufondue.api.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @Multipart
    @POST("api/issues")
    suspend fun createIssue(
        @Part("issue") issue: RequestBody,  // Changed to RequestBody
        @Part photos: List<MultipartBody.Part>
    ): Response<ApiResponse<IssueResponse>>

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
}