package com.pranayburra.moment.domain.repository

import com.pranayburra.moment.data.remote.RelationshipDto
import com.pranayburra.moment.data.remote.CreatePairingKeyResponse
import com.pranayburra.moment.util.Resource
import kotlinx.coroutines.flow.Flow

interface RelationshipRepository {
    val relationshipState: Flow<Resource<RelationshipDto?>>

    suspend fun refreshCurrentRelationship(): Resource<Unit>
    suspend fun createPairingKey(): Resource<CreatePairingKeyResponse>
    suspend fun joinRelationship(pairingKey: String): Resource<Unit>
    suspend fun updateSpaceName(spaceName: String): Resource<Unit>
    suspend fun updateTheme(themeId: String): Resource<Unit>
    suspend fun updateCover(coverMomentId: String): Resource<Unit>
    suspend fun updateAnniversary(anniversaryDate: String): Resource<Unit>
    suspend fun setPause(isPaused: Boolean): Resource<Unit>
    suspend fun unpair(): Resource<Unit>
    suspend fun block(): Resource<Unit>
}
