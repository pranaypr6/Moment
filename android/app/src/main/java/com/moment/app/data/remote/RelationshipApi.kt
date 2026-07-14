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
    
    @PUT("api/relationship/anniversary")
    suspend fun updateAnniversary(@Body request: UpdateAnniversaryRequest): Response<RelationshipDto>

    @PUT("api/relationship/pause")
    suspend fun togglePause(@Body request: PauseRequest): Response<RelationshipDto>

    @POST("api/relationship/unpair")
    suspend fun unpair(): Response<Unit>
}

@androidx.annotation.Keep
data class PartnerDto(
    val id: String,
    val displayName: String,
    val profilePictureUrl: String?,
    val currentVibe: String?,
    val isPremium: Boolean = false
)

@androidx.annotation.Keep
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
    val pairedAt: String?,
    val anniversaryDate: String?,
    val totalMoments: Int? = 0,
    val signalsCount: Map<String, Int>? = emptyMap()
)

@androidx.annotation.Keep
data class CreatePairingKeyResponse(val pairingKey: String, val expiresAt: String)
@androidx.annotation.Keep
data class JoinRelationshipRequest(val pairingKey: String)
@androidx.annotation.Keep
data class UpdateSpaceNameRequest(val spaceName: String)
@androidx.annotation.Keep
data class UpdateThemeRequest(val themeId: String)
@androidx.annotation.Keep
data class UpdateCoverRequest(val coverMomentId: String)
@androidx.annotation.Keep
data class UpdateAnniversaryRequest(val anniversaryDate: String)
@androidx.annotation.Keep
data class PauseRequest(val isPaused: Boolean)
