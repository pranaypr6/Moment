package com.moment.app.domain.repository

import com.moment.app.data.remote.RelationshipDto
import com.moment.app.data.remote.CreatePairingKeyResponse
import com.moment.app.util.Resource
import kotlinx.coroutines.flow.Flow

interface RelationshipRepository {
    val relationshipState: Flow<Resource<RelationshipDto?>>

    suspend fun refreshCurrentRelationship(): Resource<Unit>
    suspend fun createPairingKey(): Resource<CreatePairingKeyResponse>
    suspend fun joinRelationship(pairingKey: String): Resource<Unit>
    suspend fun updateSpaceName(spaceName: String): Resource<Unit>
    suspend fun updateTheme(themeId: String): Resource<Unit>
    suspend fun updateCover(coverMomentId: String): Resource<Unit>
    suspend fun togglePause(): Resource<Unit>
    suspend fun unpair(): Resource<Unit>
}
