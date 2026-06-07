package com.moment.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moment.app.data.remote.TimelineResponse
import com.moment.app.data.remote.UserDto
import com.moment.app.data.remote.ReportRequest
import com.moment.app.domain.repository.AuthRepository
import com.moment.app.domain.repository.ConnectionRepository
import com.moment.app.domain.repository.TimelineRepository
import com.moment.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: TimelineRepository,
    private val authRepository: AuthRepository,
    private val connectionRepository: ConnectionRepository
) : ViewModel() {

    private val _timelineState = MutableStateFlow<Resource<TimelineResponse>>(Resource.Idle())
    val timelineState = _timelineState.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId = _currentUserId.asStateFlow()

    private val _currentUser = MutableStateFlow<Resource<UserDto>>(Resource.Idle())
    val currentUser = _currentUser.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowToast(val message: String) : UiEvent()
    }

    init {
        viewModelScope.launch {
            _currentUserId.value = authRepository.getCurrentUserId()
            fetchProfile()
        }
    }

    private var currentPage = 1

    fun fetchProfile() {
        viewModelScope.launch {
            _currentUser.value = Resource.Loading()
            val result = authRepository.getProfile()
            result.onSuccess {
                _currentUser.value = Resource.Success(it)
            }.onFailure {
                _currentUser.value = Resource.Error(it.message ?: "Failed to fetch profile")
            }
        }
    }

    fun loadTimeline(refresh: Boolean = false) {
        if (refresh) currentPage = 1

        viewModelScope.launch {
            if (refresh) _timelineState.value = Resource.Loading()
            val result = repository.getTimeline(currentPage, 20)
            result.onSuccess {
                _timelineState.value = Resource.Success(it)
            }.onFailure {
                _timelineState.value = Resource.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun reportMoment(momentId: String, reason: String, reportedUserId: String?) {
        viewModelScope.launch {
            val result = repository.report(reportedUserId, momentId, reason)
            result.onSuccess {
                _uiEvent.emit(UiEvent.ShowToast("Moment reported"))
            }.onFailure {
                _uiEvent.emit(UiEvent.ShowToast("Failed to report: ${it.message}"))
            }
        }
    }

    fun blockUser(targetUserId: String) {
        viewModelScope.launch {
            val result = connectionRepository.revokeConnection(targetUserId)
            result.onSuccess {
                _uiEvent.emit(UiEvent.ShowToast("User blocked"))
                loadTimeline(refresh = true)
            }.onFailure {
                _uiEvent.emit(UiEvent.ShowToast("Failed to block: ${it.message}"))
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteAccount()
            result.onSuccess {
                if (it) onSuccess()
            }
        }
    }
}
