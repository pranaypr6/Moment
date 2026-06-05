package com.moment.app.data.repository

import com.moment.app.data.remote.*
import com.moment.app.domain.repository.TimelineRepository
import javax.inject.Inject

class TimelineRepositoryImpl @Inject constructor(
    private val api: TimelineApi
) : TimelineRepository {

    override suspend fun getTimeline(page: Int, pageSize: Int): Result<TimelineResponse> {
        return try {
            val response = api.getTimeline(page, pageSize)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get timeline"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun report(reportedUserId: String?, momentId: String?, reason: String): Result<Boolean> {
        return try {
            val response = api.report(ReportRequest(reportedUserId, momentId, reason))
            if (response.isSuccessful) {
                Result.success(response.body()?.success ?: true)
            } else {
                Result.failure(Exception("Failed to report"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Boolean> {
        return try {
            val response = api.deleteAccount()
            if (response.isSuccessful) {
                Result.success(response.body()?.success ?: true)
            } else {
                Result.failure(Exception("Failed to delete account"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
