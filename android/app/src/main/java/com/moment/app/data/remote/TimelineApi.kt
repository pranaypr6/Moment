package com.moment.app.data.remote

import retrofit2.Response
import retrofit2.http.*

interface TimelineApi {
    @GET("api/v1/timeline")
    suspend fun getTimeline(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Response<TimelineResponse>

    @POST("api/v1/timeline/report")
    suspend fun report(@Body request: ReportRequest): Response<SuccessResponse>

    @DELETE("api/v1/timeline/account")
    suspend fun deleteAccount(): Response<SuccessResponse>
}

data class TimelineResponse(
    val moments: List<MomentDto>,
    val totalCount: Int
)

data class ReportRequest(
    val reportedUserId: String?,
    val momentId: String?,
    val reason: String
)
