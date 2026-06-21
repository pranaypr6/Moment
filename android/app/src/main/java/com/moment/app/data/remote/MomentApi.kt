package com.moment.app.data.remote

import retrofit2.Response
import retrofit2.http.*

interface MomentApi {
    @GET("api/relationship/{relationshipId}/scrapbook")
    suspend fun getScrapbook(
        @Path("relationshipId") relationshipId: String,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<PaginatedResponse<MomentDto>>

    @GET("api/moments/upload-url")
    suspend fun getUploadUrl(
        @Query("fileName") fileName: String,
        @Query("contentType") contentType: String
    ): Response<UploadUrlResponse>

    @POST("api/moments")
    suspend fun createMoment(@Body request: CreateMomentRequest): Response<MomentDto>

    @PUT("api/moments/{id}/favorite")
    suspend fun toggleFavorite(@Path("id") id: String): Response<MomentDto>

    @POST("api/v1/presence/signal")
    suspend fun sendPresenceSignal(@Body request: Map<String, Any>): Response<Any>
}

data class CreateMomentRequest(
    val imageUrl: String,
    val thumbnailUrl: String?,
    val note: String?,
    val wallpaperTarget: String
)

data class MomentDto(
    val id: String,
    val relationshipId: String,
    val creatorUserId: String,
    val imageUrl: String,
    val thumbnailUrl: String?,
    val note: String?,
    val wallpaperTarget: String,
    val isFavorite: Boolean,
    val status: String,
    val createdAt: String,
    val deliveredAt: String?,
    val appliedAt: String?
)

data class PaginatedResponse<T>(
    val items: List<T>,
    val hasMore: Boolean,
    val nextCursor: String?
)

data class UploadUrlResponse(
    val uploadUrl: String,
    val publicUrl: String
)
