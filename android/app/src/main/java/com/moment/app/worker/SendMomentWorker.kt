package com.moment.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moment.app.data.local.MomentDao
import com.moment.app.domain.repository.MomentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class SendMomentWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MomentRepository,
    private val momentDao: MomentDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val momentId = inputData.getString("momentId") ?: return@withContext Result.failure()
        val imagePath = inputData.getString("imagePath") ?: return@withContext Result.failure()
        val note = inputData.getString("note")
        val wallpaperTarget = inputData.getString("wallpaperTarget") ?: "BOTH"

        Log.d("SendMomentWorker", "Starting offline upload for moment: $momentId")

        try {
            val file = File(imagePath)
            if (!file.exists()) {
                Log.e("SendMomentWorker", "File not found: $imagePath")
                return@withContext Result.failure()
            }

            val contentType = "image/jpeg"
            val fileLength = file.length()

            // 1. Get Upload URL
            val uploadUrlResult = repository.getUploadUrl(contentType, fileLength)
            if (uploadUrlResult.isFailure) {
                Log.e("SendMomentWorker", "Failed to get upload URL", uploadUrlResult.exceptionOrNull())
                return@withContext failOrRetry(momentId)
            }
            val uploadUrls = uploadUrlResult.getOrNull() ?: return@withContext Result.retry()

            // 2. Upload Image using File streaming
            val uploadResult = repository.uploadFile(uploadUrls.uploadUrl, file, contentType)
            if (uploadResult.isFailure) {
                Log.e("SendMomentWorker", "Failed to upload image", uploadResult.exceptionOrNull())
                return@withContext failOrRetry(momentId)
            }

            // 3. Create Moment via API
            val createResult = repository.createMoment(
                imageUrl = uploadUrls.publicUrl,
                note = note,
                wallpaperTarget = wallpaperTarget
            )

            if (createResult is com.moment.app.util.Resource.Success) {
                Log.d("SendMomentWorker", "Moment created successfully: $momentId")
                
                // We could delete the old "PENDING_UPLOAD" local moment since createMoment
                // inserts a new one with the real backend ID. 
                momentDao.deleteMoment(momentId)
                
                // Cleanup temp file
                file.delete()
                
                return@withContext Result.success()
            } else {
                val errorMsg = createResult.message ?: ""
                Log.e("SendMomentWorker", "API CreateMoment failed: $errorMsg")
                if (errorMsg.contains("paused", ignoreCase = true)) {
                    momentDao.deleteMoment(momentId)
                    file.delete()
                    return@withContext Result.failure()
                }
                return@withContext failOrRetry(momentId)
            }
        } catch (e: Exception) {
            Log.e("SendMomentWorker", "Worker failed", e)
            if (runAttemptCount < 5) {
                return@withContext Result.retry()
            } else {
                momentDao.updateStatus(momentId, "FAILED")
                return@withContext Result.failure()
            }
        }
    }

    private suspend fun failOrRetry(momentId: String): Result {
        return if (runAttemptCount < 5) {
            Result.retry()
        } else {
            momentDao.updateStatus(momentId, "FAILED")
            Result.failure()
        }
    }
}
