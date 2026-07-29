package com.pranayburra.moment.data.remote

import retrofit2.Response
import retrofit2.http.*

@androidx.annotation.Keep
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

    @POST("api/v1/auth/refresh")
    fun refreshTokenSync(@Body request: RefreshTokenRequest): retrofit2.Call<AuthResponse>

    @DELETE("api/v1/auth/me")
    suspend fun deleteAccount(): Response<Unit>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>
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
    val currentVibe: String?
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
    // Intentionally non-null: the backend's UpdateVibeRequest.Vibe is a non-nullable C#
    // string with nullable-reference-types enabled, so a missing/null "vibe" field fails
    // JSON deserialization there with a 400. Gson drops null fields from the request body
    // by default, so sending null here would silently turn into a missing field server-side
    // and the request would fail. Clearing the vibe should send "" instead - the backend
    // already treats a blank/whitespace string the same as null (see AuthService.UpdateVibeAsync).
    val vibe: String
)
