package com.moment.app.ui.moments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moment.app.domain.repository.MomentRepository
import com.moment.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

import com.moment.app.data.local.MomentDao
import com.moment.app.data.local.MomentEntity
import com.moment.app.domain.repository.AuthRepository
import com.moment.app.domain.repository.RelationshipRepository

@HiltViewModel
class SendMomentViewModel @Inject constructor(
    private val momentRepository: MomentRepository,
    private val momentDao: MomentDao,
    private val authRepository: AuthRepository,
    private val relationshipRepository: RelationshipRepository
) : ViewModel() {

    private val _sendState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val sendState = _sendState.asStateFlow()

    fun sendMoment(
        context: Context,
        imageUri: Uri,
        note: String,
        wallpaperTarget: String
    ) {
        viewModelScope.launch {
            _sendState.value = Resource.Loading()
            try {
                val creatorId = authRepository.getCurrentUserId() ?: throw Exception("Not logged in")
                val relationshipId = (relationshipRepository.relationshipState.first() as? Resource.Success)?.data?.id 
                    ?: throw Exception("No active relationship")

                val inputStream = context.contentResolver.openInputStream(imageUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap == null) {
                    _sendState.value = Resource.Error("Failed to read image")
                    return@launch
                }

                val maxWidth = 1080
                val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                val targetWidth = if (originalBitmap.width > maxWidth) maxWidth else originalBitmap.width
                val targetHeight = (targetWidth / ratio).toInt()
                
                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

                // Save to cache dir for offline outbox
                val tempId = UUID.randomUUID().toString()
                val file = java.io.File(context.cacheDir, "outbox_$tempId.jpg")
                val outputStream = java.io.FileOutputStream(file)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.close()

                // Insert into local DB as PENDING_UPLOAD so it appears immediately
                val entity = MomentEntity(
                    id = tempId,
                    relationshipId = relationshipId,
                    creatorId = creatorId,
                    creatorName = "You",
                    imageUrl = file.absolutePath, // use local path temporarily
                    thumbnailUrl = null,
                    note = note,
                    wallpaperTarget = wallpaperTarget,
                    isFavorite = false,
                    status = "PENDING_UPLOAD",
                    createdAt = System.currentTimeMillis()
                )
                momentDao.insertMoment(entity)

                val workData = androidx.work.Data.Builder()
                    .putString("momentId", tempId)
                    .putString("imagePath", file.absolutePath)
                    .putString("note", note)
                    .putString("wallpaperTarget", wallpaperTarget)
                    .build()

                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()

                val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.moment.app.worker.SendMomentWorker>()
                    .setInputData(workData)
                    .setConstraints(constraints)
                    .build()

                androidx.work.WorkManager.getInstance(context).enqueue(workRequest)

                _sendState.value = Resource.Success(Unit)
            } catch (e: Exception) {
                _sendState.value = Resource.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _sendState.value = Resource.Idle()
    }
}
