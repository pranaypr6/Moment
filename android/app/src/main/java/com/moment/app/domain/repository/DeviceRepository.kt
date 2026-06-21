package com.moment.app.domain.repository

import com.moment.app.util.Resource

interface DeviceRepository {
    suspend fun registerDevice(fcmToken: String): Resource<Unit>
}
