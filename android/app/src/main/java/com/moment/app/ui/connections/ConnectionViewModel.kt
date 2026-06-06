package com.moment.app.ui.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moment.app.data.remote.*
import com.moment.app.domain.repository.AuthRepository
import com.moment.app.domain.repository.ConnectionRepository
import com.moment.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val repository: ConnectionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _inviteState = MutableStateFlow<Resource<InviteDto>>(Resource.Idle())
    val inviteState = _inviteState.asStateFlow()

    private val _connections = MutableStateFlow<Resource<List<ConnectionDto>>>(Resource.Idle())
    val connections = _connections.asStateFlow()

    private val _pendingRequests = MutableStateFlow<Resource<List<ConnectionRequestDto>>>(Resource.Idle())
    val pendingRequests = _pendingRequests.asStateFlow()

    private val _sentRequests = MutableStateFlow<Resource<List<ConnectionRequestDto>>>(Resource.Idle())
    val sentRequests = _sentRequests.asStateFlow()

    private val _inviteInfo = MutableStateFlow<Resource<UserDto>?>(null)
    val inviteInfo = _inviteInfo.asStateFlow()

    init {
        checkPendingReferrer()
        loadConnections()
        loadPendingRequests()
        loadSentRequests()
    }

    private fun checkPendingReferrer() {
        val pendingCode = authRepository.getPendingInviteCode()
        if (pendingCode != null) {
            getInviteInfo(pendingCode)
            authRepository.clearPendingInviteCode()
        }
    }

    fun createInvite() {
        viewModelScope.launch {
            _inviteState.value = Resource.Loading()
            val result = repository.createInvite()
            result.onSuccess {
                _inviteState.value = Resource.Success(it)
            }.onFailure {
                _inviteState.value = Resource.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun getInviteInfo(inviteCode: String) {
        viewModelScope.launch {
            _inviteInfo.value = Resource.Loading()
            val result = repository.getInviteInfo(inviteCode)
            result.onSuccess {
                _inviteInfo.value = Resource.Success(it)
            }.onFailure {
                _inviteInfo.value = Resource.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun loadSentRequests() {
        viewModelScope.launch {
            _sentRequests.value = Resource.Loading()
            val result = repository.getSentRequests()
            result.onSuccess {
                _sentRequests.value = Resource.Success(it)
            }.onFailure {
                _sentRequests.value = Resource.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun requestConnection(targetUserId: String) {
        viewModelScope.launch {
            repository.requestConnection(targetUserId)
            clearInviteInfo()
            loadConnections()
            loadPendingRequests()
            loadSentRequests()
        }
    }

    fun clearInviteInfo() {
        _inviteInfo.value = null
    }

    fun resetInviteStates() {
        _inviteState.value = Resource.Idle()
        _inviteInfo.value = null
    }

    fun respondToRequest(requestId: String, accept: Boolean) {
        viewModelScope.launch {
            _connections.value = Resource.Loading()
            val result = repository.respondToRequest(requestId, accept)
            result.onSuccess {
                loadConnections()
                loadPendingRequests()
                loadSentRequests()
            }.onFailure {
                _connections.value = Resource.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun loadConnections() {
        viewModelScope.launch {
            _connections.value = Resource.Loading()
            val result = repository.getConnections()
            result.onSuccess {
                _connections.value = Resource.Success(it)
            }.onFailure {
                _connections.value = Resource.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun loadPendingRequests() {
        viewModelScope.launch {
            _pendingRequests.value = Resource.Loading()
            val result = repository.getPendingRequests()
            result.onSuccess {
                _pendingRequests.value = Resource.Success(it)
            }.onFailure {
                _pendingRequests.value = Resource.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun revokeConnection(targetUserId: String) {
        viewModelScope.launch {
            repository.revokeConnection(targetUserId)
            loadConnections()
        }
    }
}
