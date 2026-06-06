package com.moment.app.domain.repository

import com.moment.app.data.remote.*

interface ConnectionRepository {
    suspend fun createInvite(): Result<InviteDto>
    suspend fun getInviteInfo(inviteCode: String): Result<UserDto>
    suspend fun requestConnection(targetUserId: String): Result<ConnectionRequestDto>
    suspend fun respondToRequest(requestId: String, accept: Boolean): Result<Boolean>
    suspend fun getConnections(): Result<List<ConnectionDto>>
    suspend fun getPendingRequests(): Result<List<ConnectionRequestDto>>
    suspend fun getSentRequests(): Result<List<ConnectionRequestDto>>
    suspend fun revokeConnection(targetUserId: String): Result<Boolean>
}
