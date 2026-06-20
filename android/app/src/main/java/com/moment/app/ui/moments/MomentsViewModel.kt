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

@HiltViewModel
class MomentsViewModel @Inject constructor(
    private val relationshipRepository: RelationshipRepository,
    private val momentRepository: MomentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MomentsUiState>(MomentsUiState.Loading)
    val uiState: StateFlow<MomentsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // First load the relationship
            relationshipRepository.refreshCurrentRelationship()
            
            relationshipRepository.relationshipState.collect { resource ->
                when (resource) {
                    is Resource.Idle -> { /* do nothing */ }
                    is Resource.Loading -> _uiState.value = MomentsUiState.Loading
                    is Resource.Error -> _uiState.value = MomentsUiState.Error(resource.message ?: "Unknown error")
                    is Resource.Success -> {
                        val rel = resource.data
                        if (rel == null) {
                            _uiState.value = MomentsUiState.NotPaired
                        } else {
                            // Fetch moments
                            momentRepository.refreshScrapbook(rel.id)
                            
                            momentRepository.getScrapbookMoments(rel.id)
                                .collect { moments ->
                                    val latestMoment = moments.firstOrNull()
                                    _uiState.value = MomentsUiState.Success(
                                        partnerId = rel.partner.id,
                                        partnerName = rel.partner.displayName ?: "Partner",
                                        isPausedByPartner = rel.isPausedByPartner,
                                        latestMoment = latestMoment
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}

sealed class MomentsUiState {
    object Loading : MomentsUiState()
    object NotPaired : MomentsUiState()
    data class Success(
        val partnerId: String,
        val partnerName: String,
        val isPausedByPartner: Boolean,
        val latestMoment: MomentEntity?
    ) : MomentsUiState()
    data class Error(val message: String) : MomentsUiState()
}
