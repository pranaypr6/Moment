package com.moment.app.data.remote

import retrofit2.Response
import retrofit2.http.*

interface ConnectionApi {
    @POST("api/v1/connections/invite")
    suspend fun createInvite(): Response<InviteDto>

    @GET("api/v1/connections/invite/{inviteCode}")
    suspend fun getInviteInfo(@Path("inviteCode") inviteCode: String): Response<UserDto>

    @POST("api/v1/connections/request")
    suspend fun requestConnection(@Body request: ConnectionRequest): Response<ConnectionRequestDto>

    @POST("api/v1/connections/respond")
    suspend fun respondToRequest(@Body request: RespondToConnectionRequest): Response<SuccessResponse>

    @GET("api/v1/connections")
    suspend fun getConnections(): Response<List<ConnectionDto>>

    @GET("api/v1/connections/requests/pending")
    suspend fun getPendingRequests(): Response<List<ConnectionRequestDto>>

    @GET("api/v1/connections/requests/sent")
    suspend fun getSentRequests(): Response<List<ConnectionRequestDto>>

    @DELETE("api/v1/connections/{targetUserId}")
    suspend fun revokeConnection(@Path("targetUserId") targetUserId: String): Response<SuccessResponse>
}

data class InviteDto(
    val inviteCode: String,
    val inviteUrl: String,
    val expiresAt: String
)

data class ConnectionDto(
    val targetUserId: String,
    val otherUser: UserDto,
    val alias: String?,
    val isMuted: Boolean,
    val isPinned: Boolean,
    val connectedAt: String
)

data class ConnectionRequestDto(
    val id: String,
    val otherUser: UserDto,
    val status: String,
    val createdAt: String
)

data class ConnectionRequest(val targetUserId: String)

data class RespondToConnectionRequest(val requestId: String, val accept: Boolean)

data class SuccessResponse(val success: Boolean)
