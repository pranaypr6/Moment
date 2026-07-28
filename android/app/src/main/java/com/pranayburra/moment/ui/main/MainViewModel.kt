package com.pranayburra.moment.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranayburra.moment.data.remote.RelationshipDto
import com.pranayburra.moment.domain.repository.RelationshipRepository
import com.pranayburra.moment.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pranayburra.moment.domain.repository.MomentRepository

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
            // Sync any dropped wallpapers from FCM concurrently
            launch {
                try {
                    momentRepository.syncPendingMoments()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    // Ignore sync errors
                }

                // Moment sync and the relationship fetch below are two independent network
                // calls. If the relationship fetch failed earlier (e.g. a transient blip)
                // while this sync succeeded later - it can run on its own retry/backoff via
                // WorkManager - the UI would otherwise stay stuck on AppState.Error forever
                // with nothing left to clear it, even though the app is clearly back online.
                // Give the relationship fetch another chance once sync settles.
                if (_appState.value is AppState.Error) {
                    relationshipRepository.refreshCurrentRelationship()
                }
            }

            launch {
                relationshipRepository.refreshCurrentRelationship()
            }

            // React to a manual "Try Again" tap (from the maintenance screen) by actually
            // re-fetching, not just clearing the offline flag. Skip the initial emission
            // (0) since that's just the flow's starting value, not a real retry request.
            launch {
                com.pranayburra.moment.util.NetworkState.retrySignal.collect { count ->
                    if (count > 0) {
                        relationshipRepository.refreshCurrentRelationship()
                    }
                }
            }

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
