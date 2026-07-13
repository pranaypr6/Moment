package com.moment.app.data.remote

import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("api/v1/auth/login/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/profile")
    suspend fun createProfile(@Body request: CreateProfileRequest): Response<UserDto>

    @GET("api/v1/auth/profile")
    suspend fun getProfile(): Response<UserDto>

    @PUT("api/v1/auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserDto>

@GET("api/v1/auth/username-available")
    suspend fun isUsernameAvailable(@Query("username") username: String): Response<UsernameAvailableResponse>

    @PUT("api/v1/auth/vibe")
    suspend fun updateVibe(@Body request: UpdateVibeRequest): Response<UserDto>

    @POST("api/v1/auth/premium")
    suspend fun upgradeToPremium(): Response<UserDto>

    @POST("api/v1/auth/refresh")
    fun refreshTokenSync(@Body request: RefreshTokenRequest): retrofit2.Call<AuthResponse>
}

@androidx.annotation.Keep
data class GoogleLoginRequest(val idToken: String)

@androidx.annotation.Keep
data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val user: UserDto
)

@androidx.annotation.Keep
data class RefreshTokenRequest(
    val refreshToken: String
)

@androidx.annotation.Keep
data class UserDto(
    val id: String,
    val email: String,
    val username: String?,
    val displayName: String?,
    val profilePictureUrl: String?,
    val bio: String?,
    val currentVibe: String?,
    val isPremium: Boolean = false
)

@androidx.annotation.Keep
data class CreateProfileRequest(
    val username: String,
    val displayName: String,
    val bio: String?,
    val profilePictureUrl: String?
)

@androidx.annotation.Keep
data class UpdateProfileRequest(
    val displayName: String,
    val profilePictureUrl: String?
)

@androidx.annotation.Keep
data class UsernameAvailableResponse(val available: Boolean)

@androidx.annotation.Keep
data class UpdateVibeRequest(
    val vibe: String
)
