package com.pranayburra.moment.domain.repository

import com.pranayburra.moment.data.local.MomentEntity
import com.pranayburra.moment.util.Resource
import kotlinx.coroutines.flow.Flow

interface MomentRepository {
    fun getScrapbookMoments(relationshipId: String): Flow<List<MomentEntity>>
    suspend fun refreshScrapbook(relationshipId: String): Resource<Unit>
    suspend fun createMoment(imageUrl: String, note: String?, wallpaperTarget: String): Resource<Unit>
    suspend fun toggleFavorite(momentId: String): Resource<Unit>
    suspend fun getUploadUrl(contentType: String, contentLength: Long): Result<com.pranayburra.moment.data.remote.UploadUrlResponse>
    suspend fun uploadFile(uploadUrl: String, file: java.io.File, contentType: String): Result<Unit>
    suspend fun syncPendingMoments()
    suspend fun markMomentApplied(momentId: String)
}
