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
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SendMomentViewModel @Inject constructor(
    private val momentRepository: MomentRepository
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

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val bytes = outputStream.toByteArray()

                val fileName = "${UUID.randomUUID()}.jpg"
                val contentType = "image/jpeg"
                val uploadUrlResult = momentRepository.getUploadUrl(fileName, contentType)
                
                if (uploadUrlResult.isFailure) {
                    _sendState.value = Resource.Error("Failed to get upload URL")
                    return@launch
                }
                val uploadUrls = uploadUrlResult.getOrNull()

                if (uploadUrls == null) {
                    _sendState.value = Resource.Error("Failed to get upload URL")
                    return@launch
                }

                val uploadResult = momentRepository.uploadFile(uploadUrls.uploadUrl, bytes, contentType)
                if (uploadResult.isFailure) {
                    _sendState.value = Resource.Error("Failed to upload image")
                    return@launch
                }

                val sendResult = momentRepository.createMoment(
                    imageUrl = uploadUrls.publicUrl,
                    note = note.ifBlank { null },
                    wallpaperTarget = wallpaperTarget
                )

                _sendState.value = sendResult
            } catch (e: Exception) {
                _sendState.value = Resource.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _sendState.value = Resource.Idle()
    }
}
