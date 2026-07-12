package com.moment.app.domain.repository

import com.moment.app.data.local.MomentEntity
import com.moment.app.util.Resource
import kotlinx.coroutines.flow.Flow

interface MomentRepository {
    fun getScrapbookMoments(relationshipId: String): Flow<List<MomentEntity>>
    suspend fun refreshScrapbook(relationshipId: String): Resource<Unit>
    suspend fun createMoment(imageUrl: String, note: String?, wallpaperTarget: String): Resource<Unit>
    suspend fun toggleFavorite(momentId: String): Resource<Unit>
    suspend fun getUploadUrl(contentType: String, contentLength: Long): Result<com.moment.app.data.remote.UploadUrlResponse>
    suspend fun uploadFile(uploadUrl: String, file: java.io.File, contentType: String): Result<Unit>
    suspend fun syncPendingMoments()
}
