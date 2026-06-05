package com.moment.app.data.remote

import retrofit2.Response
import retrofit2.http.*

interface MomentApi {
    @POST("api/v1/moments")
    suspend fun sendMoment(@Body request: SendMomentRequest): Response<MomentDto>

    @GET("api/v1/moments/pending")
    suspend fun getPendingMoments(): Response<List<MomentDto>>

    @PATCH("api/v1/moments/{momentId}/status")
    suspend fun updateMomentStatus(
        @Path("momentId") momentId: String,
        @Body request: UpdateMomentStatusRequest
    ): Response<SuccessResponse>

    @POST("api/v1/moments/register-device")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): Response<SuccessResponse>

    @GET("api/v1/moments/upload-url")
    suspend fun getUploadUrl(
        @Query("fileName") fileName: String,
        @Query("contentType") contentType: String
    ): Response<UploadUrlResponse>
}

data class SendMomentRequest(
    val receiverUserId: String,
    val imageUrl: String,
    val thumbnailUrl: String?,
    val note: String?,
    val wallpaperTarget: String
)

data class MomentDto(
    val id: String,
    val sender: UserDto,
    val imageUrl: String,
    val thumbnailUrl: String?,
    val note: String?,
    val wallpaperTarget: String,
    val status: String,
    val createdAt: String
)

data class UploadUrlResponse(val uploadUrl: String, val publicUrl: String)

data class RegisterDeviceRequest(val fcmToken: String, val platform: String, val deviceName: String?)

data class UpdateMomentStatusRequest(val status: String)
