package com.moment.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DeviceApi {
    @POST("api/v1/devices/register")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): Response<Unit>
}

data class RegisterDeviceRequest(
    val fcmToken: String,
    val platform: String = "Android",
    val deviceName: String? = android.os.Build.MODEL
)
