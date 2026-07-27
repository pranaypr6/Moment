package com.pranayburra.moment.data.repository

import com.pranayburra.moment.data.remote.DeviceApi
import com.pranayburra.moment.data.remote.RegisterDeviceRequest
import com.pranayburra.moment.domain.repository.DeviceRepository
import com.pranayburra.moment.util.Resource
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val api: DeviceApi
) : DeviceRepository {

    override suspend fun registerDevice(fcmToken: String): Resource<Unit> {
        return try {
            val request = RegisterDeviceRequest(fcmToken = fcmToken)
            val response = api.registerDevice(request)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to register device: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}
