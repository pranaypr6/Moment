package com.moment.app.domain.repository

import com.moment.app.data.remote.AuthResponse
import com.moment.app.data.remote.UserDto

interface AuthRepository {
    suspend fun loginWithGoogle(idToken: String): Result<AuthResponse>
    suspend fun getProfile(): Result<UserDto>
    fun getCachedProfile(): UserDto?
    suspend fun updateProfile(displayName: String, profilePictureUrl: String?): Result<UserDto>
    suspend fun createProfile(username: String, displayName: String, bio: String?, profilePictureUrl: String?): Result<UserDto>
    suspend fun isUsernameAvailable(username: String): Result<Boolean>
    suspend fun updateVibe(vibe: String): Result<UserDto>
    suspend fun upgradeToPremium(): Result<UserDto>
    suspend fun getSessionToken(): String?
    suspend fun saveSessionToken(token: String)
    suspend fun getRefreshToken(): String?
    suspend fun saveRefreshToken(token: String)
    suspend fun getCurrentUserId(): String?
    suspend fun saveCurrentUserId(userId: String)
    suspend fun clearSession()
    fun getPendingInviteCode(): String?
    fun savePendingInviteCode(code: String)
    fun clearPendingInviteCode()
}
