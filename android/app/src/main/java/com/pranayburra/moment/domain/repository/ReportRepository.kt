package com.pranayburra.moment.domain.repository

interface ReportRepository {
    suspend fun reportMoment(momentId: String, reason: String): Result<Unit>
    suspend fun reportUser(userId: String, reason: String): Result<Unit>
}
