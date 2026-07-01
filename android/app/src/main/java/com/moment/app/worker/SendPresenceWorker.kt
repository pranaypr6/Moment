package com.moment.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moment.app.domain.repository.AuthRepository
import com.moment.app.domain.repository.RelationshipRepository
import com.moment.app.data.remote.MomentApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class SendPresenceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val relationshipRepository: RelationshipRepository,
    private val api: MomentApi
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val typeStr = inputData.getString("presenceType") ?: "ThinkingOfYou"
            
            var relationshipStateData = relationshipRepository.relationshipState.firstOrNull()?.data
            if (relationshipStateData == null) {
                // If it's a cold start for the worker, state might be empty. Fetch it.
                relationshipRepository.refreshCurrentRelationship()
                relationshipStateData = relationshipRepository.relationshipState.firstOrNull()?.data
                
                if (relationshipStateData == null) {
                    Log.e("PresenceWorker", "Failed to send presence: relationshipStateData is null even after refresh")
                    return Result.failure()
                }
            }
            
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                Log.e("PresenceWorker", "Failed to send presence: userId is null")
                return Result.failure()
            }

            Log.d("PresenceWorker", "Sending presence signal: $typeStr to relationship: ${relationshipStateData.id}")
            
            val typeInt = when (typeStr) {
                "ThinkingOfYou" -> 0
                "Punch" -> 1
                "Cuddle" -> 2
                "Kiss" -> 3
                "MissYou" -> 4
                else -> 0
            }

            val request = com.moment.app.data.remote.SendPresenceRequest(relationshipStateData.id, typeInt)
            
            val response = api.sendPresenceSignal(request)
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e("PresenceWorker", "Failed to send presence signal: ${response.code()} $errorBody")
                if (response.code() == 429) {
                    return Result.success()
                }
                return Result.retry()
            }
            
            Log.d("PresenceWorker", "Presence signal sent successfully.")
            relationshipRepository.refreshCurrentRelationship()
            Result.success()
        } catch (e: Exception) {
            Log.e("PresenceWorker", "Failed to send presence signal", e)
            if (e.message?.contains("429") == true) {
                // Show a toast or something if rate limited? Worker can't easily show toasts.
                // We'll return success to not retry rate limits.
                Result.success()
            } else {
                Result.retry()
            }
        }
    }
}
