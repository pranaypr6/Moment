package com.moment.app.ui.moments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moment.app.data.local.MomentEntity
import com.moment.app.domain.repository.MomentRepository
import com.moment.app.domain.repository.RelationshipRepository
import com.moment.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.moment.app.data.remote.RelationshipDto

@HiltViewModel
class UsViewModel @Inject constructor(
    private val relationshipRepository: RelationshipRepository,
    private val momentRepository: MomentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UsUiState>(UsUiState.Loading)
    val uiState: StateFlow<UsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            relationshipRepository.relationshipState.collect { resource ->
                when (resource) {
                    is Resource.Idle -> { /* do nothing */ }
                    is Resource.Loading -> _uiState.value = UsUiState.Loading
                    is Resource.Error -> _uiState.value = UsUiState.Error(resource.message ?: "Unknown error")
                    is Resource.Success -> {
                        val rel = resource.data
                        if (rel == null) {
                            _uiState.value = UsUiState.NotPaired
                        } else {
                            // Get scrapbook moments
                            momentRepository.getScrapbookMoments(rel.id).collect { moments ->
                                _uiState.value = UsUiState.Success(
                                    relationship = rel,
                                    moments = moments
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun toggleFavorite(momentId: String) {
        viewModelScope.launch {
            momentRepository.toggleFavorite(momentId)
        }
    }
}

sealed class UsUiState {
    object Loading : UsUiState()
    object NotPaired : UsUiState()
    data class Success(
        val relationship: RelationshipDto,
        val moments: List<MomentEntity>
    ) : UsUiState()
    data class Error(val message: String) : UsUiState()
}
