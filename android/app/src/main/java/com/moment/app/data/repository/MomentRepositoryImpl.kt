package com.moment.app.data.repository

import com.moment.app.data.local.MomentDao
import com.moment.app.data.local.MomentEntity
import com.moment.app.data.remote.*
import com.moment.app.domain.repository.MomentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Named

class MomentRepositoryImpl @Inject constructor(
    private val api: MomentApi,
    private val dao: MomentDao,
    @Named("AuthClient") private val authClient: OkHttpClient,
    @Named("CleanClient") private val cleanClient: OkHttpClient
) : MomentRepository {

    override suspend fun sendMoment(
        receiverUserId: String,
        imageUrl: String,
        thumbnailUrl: String?,
        note: String?,
        wallpaperTarget: String
    ): Result<MomentDto> {
        return try {
            val response = api.sendMoment(
                SendMomentRequest(receiverUserId, imageUrl, thumbnailUrl, note, wallpaperTarget)
            )
            if (response.isSuccessful && response.body() != null) {
                val moment = response.body()!!
                Result.success(moment)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to send moment"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingMoments(): Result<List<MomentDto>> {
        return try {
            val response = api.getPendingMoments()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get pending moments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMomentStatus(momentId: String, status: String): Result<Boolean> {
        return try {
            val response = api.updateMomentStatus(momentId, UpdateMomentStatusRequest(status))
            if (response.isSuccessful) {
                Result.success(response.body()?.success ?: true)
            } else {
                Result.failure(Exception("Failed to update status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerDevice(fcmToken: String, platform: String, deviceName: String?): Result<Boolean> {
        return try {
            val response = api.registerDevice(RegisterDeviceRequest(fcmToken, platform, deviceName))
            if (response.isSuccessful) {
                Result.success(response.body()?.success ?: true)
            } else {
                Result.failure(Exception("Failed to register device"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUploadUrl(fileName: String, contentType: String): Result<UploadUrlResponse> {
        return try {
            val response = api.getUploadUrl(fileName, contentType)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get upload URL"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllMoments(): Flow<List<MomentEntity>> = dao.getAllMoments()

    override suspend fun insertMoment(moment: MomentEntity) = dao.insertMoment(moment)
    
    // Helper for actual upload
    override suspend fun uploadFile(url: String, bytes: ByteArray, contentType: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .put(bytes.toRequestBody(contentType.toMediaTypeOrNull()))
                    .build()
                
                val response = cleanClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Upload failed: ${response.message}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
