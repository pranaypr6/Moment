package com.moment.app.domain.repository

import com.moment.app.data.remote.ConnectionDto
import com.moment.app.data.remote.InviteDto
import com.moment.app.data.remote.UserDto

interface ConnectionRepository {
    suspend fun createInvite(): Result<InviteDto>
    suspend fun getInviteInfo(inviteCode: String): Result<UserDto>
    suspend fun requestConnection(targetUserId: String): Result<ConnectionDto>
    suspend fun respondToRequest(connectionId: String, accept: Boolean): Result<Boolean>
    suspend fun getConnections(): Result<List<ConnectionDto>>
    suspend fun revokeConnection(connectionId: String): Result<Boolean>
}
