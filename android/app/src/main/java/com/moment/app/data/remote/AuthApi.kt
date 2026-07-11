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
}

data class GoogleLoginRequest(val idToken: String)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

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

data class CreateProfileRequest(
    val username: String,
    val displayName: String,
    val bio: String?,
    val profilePictureUrl: String?
)

data class UpdateProfileRequest(
    val displayName: String,
    val profilePictureUrl: String?
)

data class UsernameAvailableResponse(val available: Boolean)

data class UpdateVibeRequest(
    val vibe: String
)
