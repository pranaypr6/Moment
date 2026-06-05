package com.moment.app.data.remote

import retrofit2.Response
import retrofit2.http.*

interface ConnectionApi {
    @POST("api/v1/connections/invite")
    suspend fun createInvite(): Response<InviteDto>

    @GET("api/v1/connections/invite/{inviteCode}")
    suspend fun getInviteInfo(@Path("inviteCode") inviteCode: String): Response<UserDto>

    @POST("api/v1/connections/request")
    suspend fun requestConnection(@Body request: ConnectionRequest): Response<ConnectionDto>

    @POST("api/v1/connections/respond")
    suspend fun respondToRequest(@Body request: RespondToConnectionRequest): Response<SuccessResponse>

    @GET("api/v1/connections")
    suspend fun getConnections(): Response<List<ConnectionDto>>

    @DELETE("api/v1/connections/{connectionId}")
    suspend fun revokeConnection(@Path("connectionId") connectionId: String): Response<SuccessResponse>
}

data class InviteDto(
    val inviteCode: String,
    val inviteUrl: String,
    val expiresAt: String
)

data class ConnectionDto(
    val id: String,
    val otherUser: UserDto,
    val status: String,
    val isRequester: Boolean,
    val createdAt: String
)

data class ConnectionRequest(val targetUserId: String)

data class RespondToConnectionRequest(val connectionId: String, val accept: Boolean)

data class SuccessResponse(val success: Boolean)
