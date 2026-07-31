package com.pranayburra.moment.data.repository

import com.pranayburra.moment.data.remote.*
import com.pranayburra.moment.domain.repository.RelationshipRepository
import com.pranayburra.moment.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import android.content.SharedPreferences
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.glance.appwidget.updateAll
import com.pranayburra.moment.widget.RelationshipWidget

@Singleton
class RelationshipRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: RelationshipApi,
    private val prefs: SharedPreferences
) : RelationshipRepository {

    private val gson = Gson()
    private val PREF_KEY = "cached_relationship"

    private val _relationshipState = MutableStateFlow<Resource<RelationshipDto?>>(getInitialState())
    override val relationshipState: Flow<Resource<RelationshipDto?>> = _relationshipState

    private fun getInitialState(): Resource<RelationshipDto?> {
        val cachedJson = prefs.getString(PREF_KEY, null)
        if (cachedJson != null) {
            try {
                return Resource.Success(gson.fromJson(cachedJson, RelationshipDto::class.java))
            } catch (e: Exception) {}
        }
        return Resource.Loading()
    }

    override suspend fun refreshCurrentRelationship(): Resource<Unit> {
        return try {
            val response = api.getCurrentRelationship()
            if (response.isSuccessful) {
                val rel = response.body()
                _relationshipState.value = Resource.Success(rel)
                prefs.edit().putString(PREF_KEY, gson.toJson(rel)).apply()
                // The widget's "days together" text is computed live from the paired/anniversary
                // date whenever it's actually re-rendered, but Glance widgets only re-render on
                // an explicit update call - nothing else was triggering one on a normal
                // successful relationship fetch (only specific actions like unpair/pause/
                // anniversary did), so the count could sit stale for days until one of those
                // happened to fire. A plain app-open refresh should keep it current too.
                RelationshipWidget.forceUpdate(context)
                Resource.Success(Unit)
            } else if (response.code() == 404) {
                _relationshipState.value = Resource.Success(null)
                prefs.edit().remove(PREF_KEY).apply()
                RelationshipWidget.forceUpdate(context)
                Resource.Success(Unit)
            } else {
                _relationshipState.value = Resource.Error("Failed to fetch relationship")
                Resource.Error("Failed to fetch relationship")
            }
        } catch (e: Exception) {
            val cachedJson = prefs.getString(PREF_KEY, null)
            if (cachedJson != null) {
                try {
                    val cachedRel = gson.fromJson(cachedJson, RelationshipDto::class.java)
                    _relationshipState.value = Resource.Success(cachedRel)
                    return Resource.Success(Unit)
                } catch (jsonEx: Exception) {
                    // Ignore JSON parsing errors
                }
            }
            _relationshipState.value = Resource.Error("Network error: ${e.message}")
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun createPairingKey(): Resource<CreatePairingKeyResponse> {
        return try {
            val res = api.createPairingKey()
            if (res.isSuccessful && res.body() != null) {
                Resource.Success((res.body() ?: throw Exception("Empty response body")))
            } else {
                Resource.Error("Failed to create pairing key")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error(e.message ?: "Network error")
    }
    }

    override suspend fun joinRelationship(pairingKey: String): Resource<Unit> {
        return try {
            val res = api.joinRelationship(JoinRelationshipRequest(pairingKey))
            if (res.isSuccessful && res.body() != null) {
                val rel = (res.body() ?: throw Exception("Empty response body"))
                _relationshipState.value = Resource.Success(rel)
                prefs.edit().putString(PREF_KEY, gson.toJson(rel)).apply()
                Resource.Success(Unit)
            } else {
                val errorMsg = try {
                    val raw = res.errorBody()?.string()
                    if (!raw.isNullOrBlank()) {
                        val json = org.json.JSONObject(raw)
                        json.optString("message", raw)
                    } else null
                } catch (e: Exception) { null } ?: "Invalid or expired invite code."
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error(e.message ?: "Network error")
    }
    }

    override suspend fun updateSpaceName(spaceName: String): Resource<Unit> {
        return try {
            val res = api.updateSpaceName(UpdateSpaceNameRequest(spaceName))
            if (res.isSuccessful && res.body() != null) {
                val rel = (res.body() ?: throw Exception("Empty response body"))
                _relationshipState.value = Resource.Success(rel)
                prefs.edit().putString(PREF_KEY, gson.toJson(rel)).apply()
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to update space name")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error(e.message ?: "Network error")
    }
    }

    override suspend fun updateTheme(themeId: String): Resource<Unit> {
        return try {
            val res = api.updateTheme(UpdateThemeRequest(themeId))
            if (res.isSuccessful && res.body() != null) {
                val rel = (res.body() ?: throw Exception("Empty response body"))
                _relationshipState.value = Resource.Success(rel)
                prefs.edit().putString(PREF_KEY, gson.toJson(rel)).apply()
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to update theme")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error(e.message ?: "Network error")
    }
    }

    override suspend fun updateCover(coverMomentId: String): Resource<Unit> {
        return try {
            val res = api.updateCover(UpdateCoverRequest(coverMomentId))
            if (res.isSuccessful && res.body() != null) {
                val rel = (res.body() ?: throw Exception("Empty response body"))
                _relationshipState.value = Resource.Success(rel)
                prefs.edit().putString(PREF_KEY, gson.toJson(rel)).apply()
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to update cover")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error(e.message ?: "Network error")
    }
    }

    override suspend fun updateAnniversary(anniversaryDate: String): Resource<Unit> {
        return try {
            val res = api.updateAnniversary(UpdateAnniversaryRequest(anniversaryDate))
            if (res.isSuccessful && res.body() != null) {
                val rel = (res.body() ?: throw Exception("Empty response body"))
                _relationshipState.value = Resource.Success(rel)
                prefs.edit().putString(PREF_KEY, gson.toJson(rel)).apply()
                RelationshipWidget.forceUpdate(context)
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to update anniversary date")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun setPause(isPaused: Boolean): Resource<Unit> {
        return try {
            val res = api.togglePause(PauseRequest(isPaused))
            if (res.isSuccessful && res.body() != null) {
                val rel = (res.body() ?: throw Exception("Empty response body"))
                _relationshipState.value = Resource.Success(rel)
                prefs.edit().putString(PREF_KEY, gson.toJson(rel)).apply()
                RelationshipWidget.forceUpdate(context)
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to toggle pause")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error(e.message ?: "Network error")
    }
    }

    override suspend fun unpair(): Resource<Unit> {
        return try {
            val res = api.unpair()
            if (res.isSuccessful) {
                _relationshipState.value = Resource.Success(null)
                prefs.edit().remove(PREF_KEY).apply()
                RelationshipWidget.forceUpdate(context)
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to unpair")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error(e.message ?: "Network error")
    }
    }

    override suspend fun block(): Resource<Unit> {
        return try {
            val res = api.block()
            if (res.isSuccessful) {
                _relationshipState.value = Resource.Success(null)
                prefs.edit().remove(PREF_KEY).apply()
                RelationshipWidget.forceUpdate(context)
                Resource.Success(Unit)
            } else {
                // A 429 (rate limited) or other error with an empty-but-non-null body would
                // make errorBody()?.string() return "" rather than null, silently skipping
                // the fallback message and toasting a blank error - ifBlank catches that too.
                val errorMsg = res.errorBody()?.string()?.ifBlank { null } ?: "Failed to block user"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error(e.message ?: "Network error")
        }
    }
}
