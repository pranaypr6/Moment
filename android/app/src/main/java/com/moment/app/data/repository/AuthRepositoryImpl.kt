package com.moment.app.data.repository

import android.content.SharedPreferences
import com.moment.app.data.remote.AuthApi
import com.moment.app.data.remote.AuthResponse
import com.moment.app.data.remote.CreateProfileRequest
import com.moment.app.data.remote.GoogleLoginRequest
import com.moment.app.data.remote.UserDto
import com.moment.app.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val prefs: SharedPreferences
) : AuthRepository {

    override suspend fun loginWithGoogle(idToken: String): Result<AuthResponse> {
        return try {
            val response = api.loginWithGoogle(GoogleLoginRequest(idToken))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createProfile(
        username: String,
        displayName: String,
        bio: String?,
        profilePictureUrl: String?
    ): Result<UserDto> {
        return try {
            val response = api.createProfile(CreateProfileRequest(username, displayName, bio, profilePictureUrl))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Profile creation failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isUsernameAvailable(username: String): Result<Boolean> {
        return try {
            val response = api.isUsernameAvailable(username)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.available)
            } else {
                Result.failure(Exception("Check failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSessionToken(): String? {
        return prefs.getString("session_token", null)
    }

    override suspend fun saveSessionToken(token: String) {
        prefs.edit().putString("session_token", token).apply()
    }

    override suspend fun clearSession() {
        prefs.edit().remove("session_token").apply()
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
