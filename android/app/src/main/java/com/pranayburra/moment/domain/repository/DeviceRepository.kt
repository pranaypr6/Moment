package com.pranayburra.moment.domain.repository

import com.pranayburra.moment.util.Resource

interface DeviceRepository {
    suspend fun registerDevice(fcmToken: String): Resource<Unit>
}
