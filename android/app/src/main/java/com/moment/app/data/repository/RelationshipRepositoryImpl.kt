package com.moment.app.data.repository

import com.moment.app.data.remote.*
import com.moment.app.domain.repository.RelationshipRepository
import com.moment.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelationshipRepositoryImpl @Inject constructor(
    private val api: RelationshipApi
) : RelationshipRepository {

    private val _relationshipState = MutableStateFlow<Resource<RelationshipDto?>>(Resource.Loading())
    override val relationshipState: Flow<Resource<RelationshipDto?>> = _relationshipState

    override suspend fun refreshCurrentRelationship(): Resource<Unit> {
        return try {
            val response = api.getCurrentRelationship()
            if (response.isSuccessful) {
                _relationshipState.value = Resource.Success(response.body())
                Resource.Success(Unit)
            } else if (response.code() == 404) {
                _relationshipState.value = Resource.Success(null)
                Resource.Success(Unit)
            } else {
                _relationshipState.value = Resource.Error("Failed to fetch relationship")
                Resource.Error("Failed to fetch relationship")
            }
        } catch (e: Exception) {
            _relationshipState.value = Resource.Error("Network error: ${e.message}")
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun createPairingKey(): Resource<CreatePairingKeyResponse> {
        return try {
            val res = api.createPairingKey()
            if (res.isSuccessful && res.body() != null) {
                Resource.Success(res.body()!!)
            } else {
                Resource.Error("Failed to create pairing key")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun joinRelationship(pairingKey: String): Resource<Unit> {
        return try {
            val res = api.joinRelationship(JoinRelationshipRequest(pairingKey))
            if (res.isSuccessful && res.body() != null) {
                _relationshipState.value = Resource.Success(res.body())
                Resource.Success(Unit)
            } else {
                val errorMsg = res.errorBody()?.string() ?: "Failed to join relationship"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun updateSpaceName(spaceName: String): Resource<Unit> {
        return try {
            val res = api.updateSpaceName(UpdateSpaceNameRequest(spaceName))
            if (res.isSuccessful && res.body() != null) {
                _relationshipState.value = Resource.Success(res.body())
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to update space name")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun updateTheme(themeId: String): Resource<Unit> {
        return try {
            val res = api.updateTheme(UpdateThemeRequest(themeId))
            if (res.isSuccessful && res.body() != null) {
                _relationshipState.value = Resource.Success(res.body())
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to update theme")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun updateCover(coverMomentId: String): Resource<Unit> {
        return try {
            val res = api.updateCover(UpdateCoverRequest(coverMomentId))
            if (res.isSuccessful && res.body() != null) {
                _relationshipState.value = Resource.Success(res.body())
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to update cover")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun togglePause(): Resource<Unit> {
        return try {
            val res = api.togglePause()
            if (res.isSuccessful && res.body() != null) {
                _relationshipState.value = Resource.Success(res.body())
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to toggle pause")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun unpair(): Resource<Unit> {
        return try {
            val res = api.unpair()
            if (res.isSuccessful) {
                _relationshipState.value = Resource.Success(null)
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to unpair")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}
