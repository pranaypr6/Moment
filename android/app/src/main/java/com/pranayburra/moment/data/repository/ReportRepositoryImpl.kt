package com.pranayburra.moment.data.repository

import com.pranayburra.moment.data.remote.CreateReportRequest
import com.pranayburra.moment.data.remote.ReportApi
import com.pranayburra.moment.domain.repository.ReportRepository
import javax.inject.Inject

class ReportRepositoryImpl @Inject constructor(
    private val api: ReportApi
) : ReportRepository {

    override suspend fun reportMoment(momentId: String, reason: String): Result<Unit> {
        return submit(CreateReportRequest(reportedUserId = null, momentId = momentId, reason = reason))
    }

    override suspend fun reportUser(userId: String, reason: String): Result<Unit> {
        return submit(CreateReportRequest(reportedUserId = userId, momentId = null, reason = reason))
    }

    private suspend fun submit(request: CreateReportRequest): Result<Unit> {
        return try {
            val response = api.createReport(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to submit report"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }
}
