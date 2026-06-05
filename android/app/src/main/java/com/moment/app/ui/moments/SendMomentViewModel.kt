package com.moment.app.ui.moments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moment.app.data.remote.MomentDto
import com.moment.app.domain.repository.ConnectionRepository
import com.moment.app.domain.repository.MomentRepository
import com.moment.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SendMomentViewModel @Inject constructor(
    private val momentRepository: MomentRepository,
    private val connectionRepository: ConnectionRepository
) : ViewModel() {

    private val _sendState = MutableStateFlow<Resource<MomentDto>>(Resource.Idle())
    val sendState = _sendState.asStateFlow()

    private val _connections = MutableStateFlow<Resource<List<com.moment.app.data.remote.ConnectionDto>>>(Resource.Idle())
    val connections = _connections.asStateFlow()

    init {
        loadConnections()
    }

    private fun loadConnections() {
        viewModelScope.launch {
            _connections.value = Resource.Loading()
            val result = connectionRepository.getConnections()
            result.onSuccess { list ->
                // Filter to only accepted connections
                _connections.value = Resource.Success(list.filter { it.status == "ACCEPTED" })
            }.onFailure {
                _connections.value = Resource.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun sendMoment(
        context: Context,
        imageUri: Uri,
        receiverUserId: String,
        note: String,
        wallpaperTarget: String
    ) {
        viewModelScope.launch {
            _sendState.value = Resource.Loading()
            try {
                // 1. Read and compress the image from Uri
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap == null) {
                    _sendState.value = Resource.Error("Failed to read image")
                    return@launch
                }

                // Scale down if larger than 1080p width to save bandwidth
                val maxWidth = 1080
                val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                val targetWidth = if (originalBitmap.width > maxWidth) maxWidth else originalBitmap.width
                val targetHeight = (targetWidth / ratio).toInt()
                
                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val bytes = outputStream.toByteArray()

                // 2. Get Upload URL
                val fileName = "${UUID.randomUUID()}.jpg"
                val contentType = "image/jpeg"
                val uploadUrlResult = momentRepository.getUploadUrl(fileName, contentType)
                
                if (uploadUrlResult.isFailure) {
                    _sendState.value = Resource.Error("Failed to get upload URL")
                    return@launch
                }
                val uploadUrls = uploadUrlResult.getOrThrow()

                // 3. Upload to R2
                val uploadResult = momentRepository.uploadFile(uploadUrls.uploadUrl, bytes, contentType)
                if (uploadResult.isFailure) {
                    _sendState.value = Resource.Error("Failed to upload image")
                    return@launch
                }

                // 4. Send Moment to Backend
                val sendResult = momentRepository.sendMoment(
                    receiverUserId = receiverUserId,
                    imageUrl = uploadUrls.publicUrl,
                    thumbnailUrl = null,
                    note = note.ifBlank { null },
                    wallpaperTarget = wallpaperTarget
                )

                sendResult.onSuccess {
                    _sendState.value = Resource.Success(it)
                }.onFailure {
                    _sendState.value = Resource.Error(it.message ?: "Failed to send moment")
                }
            } catch (e: Exception) {
                _sendState.value = Resource.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _sendState.value = Resource.Idle()
    }
}
