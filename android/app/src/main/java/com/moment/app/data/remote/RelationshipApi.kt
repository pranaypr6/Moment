package com.moment.app.data.remote

import retrofit2.Response
import retrofit2.http.*

interface RelationshipApi {
    @GET("api/relationship/current")
    suspend fun getCurrentRelationship(): Response<RelationshipDto>

    @POST("api/relationship/pairing-key")
    suspend fun createPairingKey(): Response<CreatePairingKeyResponse>

    @POST("api/relationship/join")
    suspend fun joinRelationship(@Body request: JoinRelationshipRequest): Response<RelationshipDto>

    @PUT("api/relationship/space-name")
    suspend fun updateSpaceName(@Body request: UpdateSpaceNameRequest): Response<RelationshipDto>

    @PUT("api/relationship/theme")
    suspend fun updateTheme(@Body request: UpdateThemeRequest): Response<RelationshipDto>

    @PUT("api/relationship/cover")
    suspend fun updateCover(@Body request: UpdateCoverRequest): Response<RelationshipDto>

    @POST("api/relationship/pause")
    suspend fun togglePause(): Response<RelationshipDto>

    @POST("api/relationship/unpair")
    suspend fun unpair(): Response<Unit>
}

data class PartnerDto(
    val id: String,
    val displayName: String,
    val profilePictureUrl: String?
)

data class RelationshipDto(
    val id: String,
    val partner: PartnerDto,
    val spaceName: String,
    val themeId: String,
    val coverMomentId: String?,
    val isPausedByMe: Boolean,
    val isPausedByPartner: Boolean,
    val status: String,
    val createdAt: String,
    val pairedAt: String?
)

data class CreatePairingKeyResponse(val pairingKey: String, val expiresAt: String)
data class JoinRelationshipRequest(val pairingKey: String)
data class UpdateSpaceNameRequest(val spaceName: String)
data class UpdateThemeRequest(val themeId: String)
data class UpdateCoverRequest(val coverMomentId: String)
