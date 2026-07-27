package com.pranayburra.moment.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranayburra.moment.data.remote.RelationshipDto
import com.pranayburra.moment.domain.repository.RelationshipRepository
import com.pranayburra.moment.domain.repository.ReportRepository
import com.pranayburra.moment.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpaceSettingsViewModel @Inject constructor(
    private val relationshipRepository: RelationshipRepository,
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<Resource<RelationshipDto?>>(Resource.Loading())
    val uiState: StateFlow<Resource<RelationshipDto?>> = _uiState.asStateFlow()

    private val _reportState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val reportState: StateFlow<Resource<Unit>> = _reportState.asStateFlow()

    private val _blockState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val blockState: StateFlow<Resource<Unit>> = _blockState.asStateFlow()

    init {
        viewModelScope.launch {
            relationshipRepository.relationshipState.collect { res ->
                _uiState.value = res
            }
        }
    }

    fun updateSpaceName(newName: String) {
        viewModelScope.launch {
            relationshipRepository.updateSpaceName(newName)
        }
    }

    fun updateTheme(newThemeId: String) {
        viewModelScope.launch {
            relationshipRepository.updateTheme(newThemeId)
        }
    }

    fun togglePause() {
        viewModelScope.launch {
            val currentState = uiState.value.data ?: return@launch
            val newPauseState = !currentState.isPausedByMe
            relationshipRepository.setPause(newPauseState)
        }
    }

    fun unpair() {
        viewModelScope.launch {
            relationshipRepository.unpair()
        }
    }

    fun reportPartner(reason: String) {
        val partnerId = uiState.value.data?.partner?.id ?: return
        viewModelScope.launch {
            _reportState.value = Resource.Loading()
            val result = reportRepository.reportUser(partnerId, reason)
            result.onSuccess {
                _reportState.value = Resource.Success(Unit)
            }.onFailure {
                _reportState.value = Resource.Error(it.message ?: "Failed to submit report")
            }
        }
    }

    fun resetReportState() {
        _reportState.value = Resource.Idle()
    }

    fun blockPartner() {
        viewModelScope.launch {
            _blockState.value = Resource.Loading()
            val result = relationshipRepository.block()
            if (result is Resource.Success) {
                _blockState.value = Resource.Success(Unit)
            } else if (result is Resource.Error) {
                _blockState.value = Resource.Error(result.message ?: "Failed to block user")
            }
        }
    }

    fun resetBlockState() {
        _blockState.value = Resource.Idle()
    }
}
