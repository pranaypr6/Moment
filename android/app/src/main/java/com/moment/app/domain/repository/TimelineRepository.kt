package com.moment.app.domain.repository

import com.moment.app.data.remote.TimelineResponse

interface TimelineRepository {
    suspend fun getTimeline(page: Int, pageSize: Int): Result<TimelineResponse>
    suspend fun report(reportedUserId: String?, momentId: String?, reason: String): Result<Boolean>
    suspend fun deleteAccount(): Result<Boolean>
}
