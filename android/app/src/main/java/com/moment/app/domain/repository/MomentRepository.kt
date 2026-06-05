package com.moment.app.domain.repository

import com.moment.app.data.remote.MomentDto
import com.moment.app.data.remote.UploadUrlResponse
import kotlinx.coroutines.flow.Flow
import com.moment.app.data.local.MomentEntity

interface MomentRepository {
    suspend fun sendMoment(receiverUserId: String, imageUrl: String, thumbnailUrl: String?, note: String?, wallpaperTarget: String): Result<MomentDto>
    suspend fun getPendingMoments(): Result<List<MomentDto>>
    suspend fun updateMomentStatus(momentId: String, status: String): Result<Boolean>
    suspend fun registerDevice(fcmToken: String, platform: String, deviceName: String?): Result<Boolean>
    suspend fun getUploadUrl(fileName: String, contentType: String): Result<UploadUrlResponse>
    suspend fun uploadFile(url: String, bytes: ByteArray, contentType: String): Result<Unit>
    
    // Local
    fun getAllMoments(): Flow<List<MomentEntity>>
    suspend fun insertMoment(moment: MomentEntity)
}
