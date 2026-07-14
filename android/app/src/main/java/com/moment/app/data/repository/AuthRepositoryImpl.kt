package com.moment.app.data.repository

import com.moment.app.data.remote.*
import com.moment.app.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val prefs: android.content.SharedPreferences,
    private val gson: com.google.gson.Gson
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
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to update profile"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun createProfile(username: String, displayName: String, bio: String?, profilePictureUrl: String?): Result<UserDto> {
        return try {
            val response = api.createProfile(CreateProfileRequest(username, displayName, bio, profilePictureUrl))
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
            val request = if (vibe.isEmpty()) UpdateVibeRequest(null) else UpdateVibeRequest(vibe)
            val response = api.updateVibe(request)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body() ?: throw Exception("Empty response body")
                prefs.edit().putString(PREF_KEY, gson.toJson(user)).apply()
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to update vibe"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun upgradeToPremium(): Result<UserDto> {
        return try {
            val response = api.upgradeToPremium()
            if (response.isSuccessful && response.body() != null) {
                Result.success((response.body() ?: throw Exception("Empty response body")))
            } else {
                Result.failure(Exception("Failed to upgrade to premium"))
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
        prefs.edit().remove("session_token").remove("refresh_token").remove("current_user_id").apply()
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
