package com.moment.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moment.app.data.remote.RelationshipDto
import com.moment.app.domain.repository.RelationshipRepository
import com.moment.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.moment.app.domain.repository.MomentRepository

sealed class AppState {
    object Loading : AppState()
    object Error : AppState()
    object None : AppState()
    data class Active(val relationship: RelationshipDto) : AppState()
    data class PostUnpair(val relationship: RelationshipDto) : AppState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val relationshipRepository: RelationshipRepository,
    private val momentRepository: MomentRepository
) : ViewModel() {

    private val _appState = MutableStateFlow<AppState>(AppState.Loading)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        viewModelScope.launch {
            // Sync any dropped wallpapers from FCM first
            try {
                momentRepository.syncPendingMoments()
            } catch (e: Exception) {
                // Ignore sync errors
            }

            relationshipRepository.refreshCurrentRelationship()
            relationshipRepository.relationshipState.collect { resource ->
                when (resource) {
                    is Resource.Idle -> { /* do nothing */ }
                    is Resource.Loading -> _appState.value = AppState.Loading
                    is Resource.Error -> _appState.value = AppState.Error
                    is Resource.Success -> {
                        val rel = resource.data
                        if (rel == null) {
                            _appState.value = AppState.None
                        } else {
                            when (rel.status.uppercase()) {
                                "ACTIVE" -> _appState.value = AppState.Active(rel)
                                "UNPAIRED" -> _appState.value = AppState.PostUnpair(rel)
                                else -> _appState.value = AppState.None
                            }
                        }
                    }
                }
            }
        }
    }

    fun acknowledgeUnpair() {
        // User clicked "Continue" on the PostUnpair screen.
        // We can locally set the state to None to allow them to pair again.
        _appState.value = AppState.None
    }

    fun checkStatus() {
        viewModelScope.launch {
            relationshipRepository.refreshCurrentRelationship()
        }
    }
}
