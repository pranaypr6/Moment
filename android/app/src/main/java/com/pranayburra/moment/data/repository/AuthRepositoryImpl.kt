package com.pranayburra.moment.data.repository

import com.pranayburra.moment.data.remote.*
import com.pranayburra.moment.domain.repository.AuthRepository
import com.pranayburra.moment.widget.RelationshipWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val api: AuthApi,
    private val prefs: android.content.SharedPreferences,
    private val gson: com.google.gson.Gson,
    private val momentDatabase: com.pranayburra.moment.data.local.MomentDatabase
) : AuthRepository {

    private val PREF_KEY = "current_user_profile"

    override suspend fun loginWithGoogle(idToken: String): Result<AuthResponse> {
        return try {
            val response = api.loginWithGoogle(GoogleLoginRequest(idToken))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body() ?: throw Exception("Empty response body")
                prefs.edit().putString(PREF_KEY, gson.toJson(body.user)).apply()
                Result.success(body)
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getProfile(): Result<UserDto> {
        return try {
            val response = api.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body() ?: throw Exception("Empty response body")
                prefs.edit().putString(PREF_KEY, gson.toJson(user)).apply()
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to fetch profile"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            
            // Fallback to cached profile if network fails
            val cached = getCachedProfile()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override fun getCachedProfile(): UserDto? {
        val cached = prefs.getString(PREF_KEY, null) ?: return null
        return try {
            gson.fromJson(cached, UserDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateProfile(displayName: String, profilePictureUrl: String?): Result<UserDto> {
        return try {
            val response = api.updateProfile(UpdateProfileRequest(displayName, profilePictureUrl))
            if (response.isSuccessful && response.body() != null) {
                val user = response.body() ?: throw Exception("Empty response body")
                prefs.edit().putString(PREF_KEY, gson.toJson(user)).apply()
                // Same gap as updateVibe() had: the widget reads this cached profile for
                // "my" photo/name (RelationshipWidget.kt's provideGlance -> getCachedProfile),
                // but nothing here told it to repaint - so a profile picture or name change
                // sat correct-but-invisible on the widget until an unrelated trigger forced
                // a redraw.
                RelationshipWidget.forceUpdate(context)
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to update profile"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun createProfile(username: String, displayName: String, bio: String?, profilePictureUrl: String?, acceptedTerms: Boolean): Result<UserDto> {
        return try {
            val response = api.createProfile(CreateProfileRequest(username, displayName, bio, profilePictureUrl, acceptedTerms))
            if (response.isSuccessful && response.body() != null) {
                val user = response.body() ?: throw Exception("Empty response body")
                prefs.edit().putString(PREF_KEY, gson.toJson(user)).apply()
                Result.success(user)
            } else {
                Result.failure(Exception("Profile creation failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun isUsernameAvailable(username: String): Result<Boolean> {
        return try {
            val response = api.isUsernameAvailable(username)
            if (response.isSuccessful && response.body() != null) {
                Result.success((response.body() ?: throw Exception("Empty response body")).available)
            } else {
                Result.failure(Exception("Request failed"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun updateVibe(vibe: String): Result<UserDto> {
        return try {
            val request = UpdateVibeRequest(vibe)
            val response = api.updateVibe(request)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body() ?: throw Exception("Empty response body")
                prefs.edit().putString(PREF_KEY, gson.toJson(user)).apply()
                // The widget reads currentVibe from this same cached profile, but nothing
                // here ever told Glance to repaint - unlike RelationshipRepositoryImpl's
                // mutation methods (anniversary, pause, unpair...), which all call this.
                // The cached value was correct immediately; the widget just never redrew
                // until something unrelated (like sending a presence signal) forced it to.
                RelationshipWidget.forceUpdate(context)
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to update vibe"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }



    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val response = api.deleteAccount()
            if (response.isSuccessful) {
                // Backend deletion succeeded - now wipe everything local: encrypted
                // prefs (tokens, cached profile, current user id) and the local Room DB.
                prefs.edit().clear().apply()
                try {
                    momentDatabase.clearAllTables()
                } catch (e: Exception) {
                    // Local cache cleanup failure shouldn't block the account deletion
                    // itself - the account is already gone server-side.
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Account deletion failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getSessionToken(): String? {
        return prefs.getString("session_token", null)
    }

    override suspend fun saveSessionToken(token: String) {
        prefs.edit().putString("session_token", token).apply()
    }

    override suspend fun getRefreshToken(): String? {
        return prefs.getString("refresh_token", null)
    }

    override suspend fun saveRefreshToken(token: String) {
        prefs.edit().putString("refresh_token", token).apply()
    }

    override suspend fun getCurrentUserId(): String? {
        return prefs.getString("current_user_id", null)
    }

    override suspend fun saveCurrentUserId(userId: String) {
        prefs.edit().putString("current_user_id", userId).apply()
    }

    override suspend fun clearSession() {
        // Detached from this suspend function's own caller: HubScreen's logout button
        // calls authViewModel.logout() (which launches this in AuthViewModel's
        // viewModelScope) and then immediately navigates away in the same click handler,
        // tearing down the nav-graph entry that owns that viewModelScope right after. If
        // api.logout() were awaited in-line here, that teardown would very likely cancel
        // it mid-flight before the server-side revoke request completed - so the code to
        // revoke the session on logout existed but often didn't actually get to run.
        // Firing it in its own IO-scoped coroutine (same pattern RelationshipWidget.
        // forceUpdate already uses for the same reason) lets it finish regardless of what
        // happens to the screen that triggered the logout.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                api.logout()
            } catch (e: Exception) {
                // Best-effort - the local logout below must never depend on this succeeding.
            }
        }
        // This used to only clear the token/user-id keys, leaving "current_user_profile"
        // and RelationshipRepositoryImpl's "cached_relationship" (same underlying prefs
        // file - see AppModule's single shared EncryptedSharedPreferences instance) behind.
        // On a shared/family device, the next account to log in on this device could see
        // the *previous* account's cached partner/relationship briefly, since
        // RelationshipRepositoryImpl falls back to that cached value if its next fetch
        // fails offline. Clearing it here (and repainting the widget so a still-pinned
        // widget doesn't keep showing the old partner's photo/vibe) closes the on-disk
        // side of that gap. Note: this does NOT reset RelationshipRepositoryImpl's
        // in-memory state if the app process stays alive across the logout - that gets
        // overwritten by the next login's own relationship fetch, so the residual window
        // is bounded to "between logout and the next login's fetch," not indefinite.
        prefs.edit()
            .remove("session_token")
            .remove("refresh_token")
            .remove("current_user_id")
            .remove(PREF_KEY)
            .remove("cached_relationship")
            .apply()
        RelationshipWidget.forceUpdate(context)
    }

    override fun getPendingInviteCode(): String? {
        return prefs.getString("pending_invite_code", null)
    }

    override fun savePendingInviteCode(code: String) {
        prefs.edit().putString("pending_invite_code", code).apply()
    }

    override fun clearPendingInviteCode() {
        prefs.edit().remove("pending_invite_code").apply()
    }
}
