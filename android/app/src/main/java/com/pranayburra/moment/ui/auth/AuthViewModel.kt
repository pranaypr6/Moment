package com.pranayburra.moment.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranayburra.moment.data.remote.AuthResponse
import com.pranayburra.moment.data.remote.UserDto
import com.pranayburra.moment.domain.repository.AuthRepository
import com.pranayburra.moment.domain.repository.MomentRepository
import com.pranayburra.moment.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val momentRepository: MomentRepository,
    private val deviceRepository: com.pranayburra.moment.domain.repository.DeviceRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
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

    private val _deleteAccountState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val deleteAccountState = _deleteAccountState.asStateFlow()

    // Dedicated to updateVibe() only - kept separate from _profileState so a vibe
    // update toast can't accidentally fire from a leftover profile-picture/display-name
    // update (or vice versa), since both used to share the same flow.
    private val _vibeUpdateState = MutableStateFlow<Resource<UserDto>>(Resource.Idle())
    val vibeUpdateState = _vibeUpdateState.asStateFlow()

    private fun registerDeviceToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            val token = task.result
            if (token != null) {
                viewModelScope.launch {
                    deviceRepository.registerDevice(token)
                }
            }
        }
    }

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
                    val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val tempFile = java.io.File.createTempFile("profile_", ".jpg", context.cacheDir)
                        context.contentResolver.openInputStream(imageUri)?.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile
                    }

                    if (file.length() > 0) {
                        val contentType = "image/jpeg"
                        val uploadUrlResult = momentRepository.getUploadUrl(contentType, file.length())
                        
                        if (uploadUrlResult.isSuccess) {
                            val uploadUrls = uploadUrlResult.getOrThrow()
                            val uploadResult = momentRepository.uploadFile(uploadUrls.uploadUrl, file, contentType)
                            if (uploadResult.isSuccess) {
                                profilePictureUrl = uploadUrls.publicUrl
                            } else {
                                _profileState.value = Resource.Error("Failed to upload image")
                                return@launch
                            }
                        } else {
                            _profileState.value = Resource.Error("Failed to get upload URL")
                            return@launch
                        }
                    } else {
                        _profileState.value = Resource.Error("Failed to read image")
                        return@launch
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

    fun updateVibe(vibe: String) {
        viewModelScope.launch {
            _vibeUpdateState.value = Resource.Loading()
            val result = repository.updateVibe(vibe)
            result.onSuccess {
                _vibeUpdateState.value = Resource.Success(it)
                _currentUser.value = Resource.Success(it)
                com.pranayburra.moment.widget.RelationshipWidget.forceUpdate(context)
            }.onFailure {
                _vibeUpdateState.value = Resource.Error(it.message ?: "Failed to update vibe")
            }
        }
    }

    fun resetVibeUpdateState() {
        _vibeUpdateState.value = Resource.Idle()
    }

    fun checkExistingSession() {
        viewModelScope.launch {
            _sessionState.value = Resource.Loading()
            val token = repository.getSessionToken()
            if (!token.isNullOrBlank()) {
                registerDeviceToken()
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
                    repository.saveRefreshToken(it.refreshToken)
                    repository.saveCurrentUserId(it.user.id)
                    registerDeviceToken()
                    _loginState.value = Resource.Success(it)
                } catch (e: Exception) {
                    _loginState.value = Resource.Error("Storage error: ${e.message}")
                }
            }.onFailure {
                _loginState.value = Resource.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun createProfileWithImage(username: String, displayName: String, bio: String?, defaultProfilePictureUrl: String?, imageUri: android.net.Uri?, context: android.content.Context) {
        viewModelScope.launch {
            _profileState.value = Resource.Loading()
            try {
                var profilePictureUrl = defaultProfilePictureUrl
                
                if (imageUri != null) {
                    val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val tempFile = java.io.File.createTempFile("profile_", ".jpg", context.cacheDir)
                        context.contentResolver.openInputStream(imageUri)?.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile
                    }

                    if (file.length() > 0) {
                        val contentType = "image/jpeg"
                        val uploadUrlResult = momentRepository.getUploadUrl(contentType, file.length())
                        
                        if (uploadUrlResult.isSuccess) {
                            val uploadUrls = uploadUrlResult.getOrThrow()
                            val uploadResult = momentRepository.uploadFile(uploadUrls.uploadUrl, file, contentType)
                            if (uploadResult.isSuccess) {
                                profilePictureUrl = uploadUrls.publicUrl
                            } else {
                                _profileState.value = Resource.Error("Failed to upload image")
                                return@launch
                            }
                        } else {
                            _profileState.value = Resource.Error("Failed to get upload URL")
                            return@launch
                        }
                    } else {
                        _profileState.value = Resource.Error("Failed to read image")
                        return@launch
                    }
                }

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
            } catch (e: Exception) {
                _profileState.value = Resource.Error(e.message ?: "Unknown error")
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

    fun deleteAccount() {
        viewModelScope.launch {
            _deleteAccountState.value = Resource.Loading()
            val result = repository.deleteAccount()
            result.onSuccess {
                _deleteAccountState.value = Resource.Success(Unit)
                _sessionState.value = Resource.Error("Logged out")
            }.onFailure {
                _deleteAccountState.value = Resource.Error(it.message ?: "Failed to delete account")
            }
        }
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = Resource.Idle()
    }
}
