package com.moment.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moment.app.data.remote.TimelineResponse
import com.moment.app.domain.repository.AuthRepository
import com.moment.app.domain.repository.TimelineRepository
import com.moment.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: TimelineRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _timelineState = MutableStateFlow<Resource<TimelineResponse>>(Resource.Idle())
    val timelineState = _timelineState.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId = _currentUserId.asStateFlow()

    init {
        viewModelScope.launch {
            _currentUserId.value = authRepository.getCurrentUserId()
        }
    }

    private var currentPage = 1

    fun loadTimeline(refresh: Boolean = false) {
        if (refresh) currentPage = 1
        
        viewModelScope.launch {
            if (currentPage == 1) _timelineState.value = Resource.Loading()
            val result = repository.getTimeline(currentPage, 20)
            result.onSuccess {
                _timelineState.value = Resource.Success(it)
                currentPage++
            }.onFailure {
                _timelineState.value = Resource.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun report(reportedUserId: String?, momentId: String?, reason: String) {
        viewModelScope.launch {
            repository.report(reportedUserId, momentId, reason)
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
