package com.moment.app.data.repository

import com.moment.app.data.local.MomentDao
import com.moment.app.data.local.MomentEntity
import com.moment.app.data.remote.CreateMomentRequest
import com.moment.app.data.remote.MomentApi
import com.moment.app.domain.repository.MomentRepository
import com.moment.app.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

import javax.inject.Named
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

@Singleton
class MomentRepositoryImpl @Inject constructor(
    private val api: MomentApi,
    private val dao: MomentDao,
    @Named("CleanClient") private val cleanClient: OkHttpClient
) : MomentRepository {

    override fun getScrapbookMoments(relationshipId: String): Flow<List<MomentEntity>> {
        return dao.getMomentsForRelationship(relationshipId)
    }

    override suspend fun refreshScrapbook(relationshipId: String): Resource<Unit> {
        return try {
            val response = api.getScrapbook(relationshipId)
            if (response.isSuccessful && response.body() != null) {
                val moments = response.body()!!.items
                moments.forEach { dto ->
                    dao.insertMoment(
                        MomentEntity(
                            id = dto.id,
                            relationshipId = relationshipId,
                            creatorId = dto.creatorUserId,
                            creatorName = "Partner", // In reality, we map this from API UserDto, but our backend MomentDto doesn't include the User name yet to keep it simple, or we can fetch it from Relationship
                            imageUrl = dto.imageUrl,
                            thumbnailUrl = dto.thumbnailUrl,
                            note = dto.note,
                            wallpaperTarget = dto.wallpaperTarget,
                            isFavorite = dto.isFavorite,
                            status = dto.status,
                            createdAt = try {
                                java.time.Instant.parse(dto.createdAt).toEpochMilli()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                        )
                    )
                }
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to fetch scrapbook")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun createMoment(imageUrl: String, note: String?, wallpaperTarget: String): Resource<Unit> {
        return try {
            val res = api.createMoment(CreateMomentRequest(imageUrl, null, note, wallpaperTarget))
            if (res.isSuccessful && res.body() != null) {
                val dto = res.body()!!
                
                // Immediately insert the newly created moment into the local database
                // so the sender's UI updates instantly without requiring a refresh.
                dao.insertMoment(
                    MomentEntity(
                        id = dto.id,
                        relationshipId = dto.relationshipId,
                        creatorId = dto.creatorUserId,
                        creatorName = "You", // The sender is always "You"
                        imageUrl = dto.imageUrl,
                        thumbnailUrl = dto.thumbnailUrl,
                        note = dto.note,
                        wallpaperTarget = dto.wallpaperTarget,
                        isFavorite = dto.isFavorite,
                        status = dto.status,
                        createdAt = try {
                            java.time.Instant.parse(dto.createdAt).toEpochMilli()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                    )
                )
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to create moment")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun toggleFavorite(momentId: String): Resource<Unit> {
        return try {
            // Optimistic update locally
            dao.toggleFavoriteLocally(momentId)
            val res = api.toggleFavorite(momentId)
            if (res.isSuccessful && res.body() != null) {
                Resource.Success(Unit)
            } else {
                // Revert on failure
                dao.toggleFavoriteLocally(momentId)
                Resource.Error("Failed to toggle favorite")
            }
        } catch (e: Exception) {
            // Revert on failure
            dao.toggleFavoriteLocally(momentId)
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun getUploadUrl(contentType: String): Result<com.moment.app.data.remote.UploadUrlResponse> {
        return try {
            val response = api.getUploadUrl(contentType)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get upload URL: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(uploadUrl: String, bytes: ByteArray, contentType: String): Result<Unit> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val requestBody = bytes.toRequestBody(contentType.toMediaType())
                val request = Request.Builder()
                    .url(uploadUrl)
                    .put(requestBody)
                    .build()

                val response = cleanClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to upload file: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
