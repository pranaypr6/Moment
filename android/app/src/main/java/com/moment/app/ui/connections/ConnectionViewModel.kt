package com.moment.app.ui.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moment.app.data.remote.ConnectionDto
import com.moment.app.data.remote.InviteDto
import com.moment.app.data.remote.UserDto
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
    private val authRepository: com.moment.app.domain.repository.AuthRepository
) : ViewModel() {

    private val _inviteState = MutableStateFlow<Resource<InviteDto>>(Resource.Idle())
    val inviteState = _inviteState.asStateFlow()

    private val _connections = MutableStateFlow<Resource<List<ConnectionDto>>>(Resource.Idle())
    val connections = _connections.asStateFlow()

    private val _inviteInfo = MutableStateFlow<Resource<UserDto>?>(null)
    val inviteInfo = _inviteInfo.asStateFlow()

    init {
        checkPendingReferrer()
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

    fun requestConnection(targetUserId: String) {
        viewModelScope.launch {
            repository.requestConnection(targetUserId)
            clearInviteInfo()
            loadConnections()
        }
    }

    fun clearInviteInfo() {
        _inviteInfo.value = null
    }

    fun respondToRequest(connectionId: String, accept: Boolean) {
        viewModelScope.launch {
            repository.respondToRequest(connectionId, accept)
            loadConnections()
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
}
