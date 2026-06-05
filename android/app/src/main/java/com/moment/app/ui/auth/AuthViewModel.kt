package com.moment.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moment.app.data.remote.AuthResponse
import com.moment.app.data.remote.UserDto
import com.moment.app.domain.repository.AuthRepository
import com.moment.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<AuthResponse>>(Resource.Idle())
    val loginState = _loginState.asStateFlow()

    private val _profileState = MutableStateFlow<Resource<UserDto>>(Resource.Idle())
    val profileState = _profileState.asStateFlow()

    private val _usernameAvailable = MutableStateFlow<Boolean?>(null)
    val usernameAvailable = _usernameAvailable.asStateFlow()

    private val _sessionState = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val sessionState = _sessionState.asStateFlow()

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
                repository.saveSessionToken(it.token)
                _loginState.value = Resource.Success(it)
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
                _profileState.value = Resource.Success(it)
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
}
