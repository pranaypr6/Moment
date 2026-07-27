package com.pranayburra.moment.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@androidx.annotation.Keep
interface ReportApi {
    @POST("api/v1/reports")
    suspend fun createReport(@Body request: CreateReportRequest): Response<ReportDto>
}

@androidx.annotation.Keep
data class CreateReportRequest(
    val reportedUserId: String?,
    val momentId: String?,
    val reason: String
)

@androidx.annotation.Keep
data class ReportDto(
    val id: String,
    val reportedUserId: String?,
    val momentId: String?,
    val reason: String,
    val createdAt: String
)
