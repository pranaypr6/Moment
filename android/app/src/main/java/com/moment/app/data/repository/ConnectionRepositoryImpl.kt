package com.moment.app.data.repository

import com.moment.app.data.remote.*
import com.moment.app.domain.repository.ConnectionRepository
import javax.inject.Inject

class ConnectionRepositoryImpl @Inject constructor(
    private val api: ConnectionApi
) : ConnectionRepository {

    override suspend fun createInvite(): Result<InviteDto> {
        return try {
            val response = api.createInvite()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create invite"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInviteInfo(inviteCode: String): Result<UserDto> {
        return try {
            val response = api.getInviteInfo(inviteCode)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get invite info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun requestConnection(targetUserId: String): Result<ConnectionRequestDto> {
        return try {
            val response = api.requestConnection(ConnectionRequest(targetUserId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to request connection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun respondToRequest(requestId: String, accept: Boolean): Result<Boolean> {
        return try {
            val response = api.respondToRequest(RespondToConnectionRequest(requestId, accept))
            if (response.isSuccessful) {
                Result.success(response.body()?.success ?: true)
            } else {
                Result.failure(Exception("Failed to respond to request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getConnections(): Result<List<ConnectionDto>> {
        return try {
            val response = api.getConnections()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get connections"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingRequests(): Result<List<ConnectionRequestDto>> {
        return try {
            val response = api.getPendingRequests()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get pending requests"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSentRequests(): Result<List<ConnectionRequestDto>> {
        return try {
            val response = api.getSentRequests()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get sent requests"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun revokeConnection(targetUserId: String): Result<Boolean> {
        return try {
            val response = api.revokeConnection(targetUserId)
            if (response.isSuccessful) {
                Result.success(response.body()?.success ?: true)
            } else {
                Result.failure(Exception("Failed to revoke connection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
