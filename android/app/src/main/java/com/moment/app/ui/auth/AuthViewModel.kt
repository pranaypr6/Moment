package com.moment.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moment.app.data.remote.AuthResponse
import com.moment.app.data.remote.UserDto
import com.moment.app.domain.repository.AuthRepository
import com.moment.app.domain.repository.MomentRepository
import com.moment.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val momentRepository: MomentRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<AuthResponse>>(Resource.Idle())
    val loginState = _loginState.asStateFlow()

    private val _profileState = MutableStateFlow<Resource<UserDto>>(Resource.Idle())
    val profileState = _profileState.asStateFlow()

    private val _currentUser = MutableStateFlow<Resource<UserDto>>(Resource.Idle())
    val currentUser = _currentUser.asStateFlow()

    private val _usernameAvailable = MutableStateFlow<Boolean?>(null)
    val usernameAvailable = _usernameAvailable.asStateFlow()

    private val _sessionState = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val sessionState = _sessionState.asStateFlow()

    fun fetchProfile() {
        viewModelScope.launch {
            _currentUser.value = Resource.Loading()
            val result = repository.getProfile()
            result.onSuccess {
                _currentUser.value = Resource.Success(it)
            }.onFailure {
                _currentUser.value = Resource.Error(it.message ?: "Failed to fetch profile")
            }
        }
    }

    fun updateProfile(displayName: String, imageUri: android.net.Uri?, context: android.content.Context) {
        viewModelScope.launch {
            _profileState.value = Resource.Loading()
            try {
                var profilePictureUrl: String? = null
                
                if (imageUri != null) {
                    // Upload image first
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        val fileName = "profile_${UUID.randomUUID()}.jpg"
                        val contentType = "image/jpeg"
                        val uploadUrlResult = momentRepository.getUploadUrl(fileName, contentType)
                        
                        if (uploadUrlResult.isSuccess) {
                            val uploadUrls = uploadUrlResult.getOrThrow()
                            val uploadResult = momentRepository.uploadFile(uploadUrls.uploadUrl, bytes, contentType)
                            if (uploadResult.isSuccess) {
                                profilePictureUrl = uploadUrls.publicUrl
                            }
                        }
                    }
                }

                val result = repository.updateProfile(displayName, profilePictureUrl)
                result.onSuccess {
                    _profileState.value = Resource.Success(it)
                    _currentUser.value = Resource.Success(it) // Update current user as well
                }.onFailure {
                    _profileState.value = Resource.Error(it.message ?: "Failed to update profile")
                }
            } catch (e: Exception) {
                _profileState.value = Resource.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun checkExistingSession() {
        viewModelScope.launch {
            _sessionState.value = Resource.Loading()
            val token = repository.getSessionToken()
            if (!token.isNullOrBlank()) {
                _sessionState.value = Resource.Success(true)
            } else {
                _sessionState.value = Resource.Error("No session")
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading()
            val result = repository.loginWithGoogle(idToken)
            result.onSuccess {
                try {
                    repository.saveSessionToken(it.token)
                    repository.saveCurrentUserId(it.user.id)
                    _loginState.value = Resource.Success(it)
                } catch (e: Exception) {
                    _loginState.value = Resource.Error("Storage error: ${e.message}")
                }
            }.onFailure {
                _loginState.value = Resource.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun createProfile(username: String, displayName: String, bio: String?, profilePictureUrl: String?) {
        viewModelScope.launch {
            _profileState.value = Resource.Loading()
            val result = repository.createProfile(username, displayName, bio, profilePictureUrl)
            result.onSuccess {
                try {
                    repository.saveCurrentUserId(it.id)
                    _profileState.value = Resource.Success(it)
                } catch (e: Exception) {
                    _profileState.value = Resource.Error("Storage error: ${e.message}")
                }
            }.onFailure {
                _profileState.value = Resource.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun checkUsername(username: String) {
        if (username.length < 4) {
            _usernameAvailable.value = false
            return
        }
        viewModelScope.launch {
            val result = repository.isUsernameAvailable(username)
            result.onSuccess {
                _usernameAvailable.value = it
            }.onFailure {
                _usernameAvailable.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearSession()
            _sessionState.value = Resource.Error("Logged out")
        }
    }
}
