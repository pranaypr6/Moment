package com.moment.app.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moment.app.data.remote.CreatePairingKeyResponse
import com.moment.app.domain.repository.RelationshipRepository
import com.moment.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PairingState {
    object Idle : PairingState()
    object Loading : PairingState()
    data class Created(val pairingKey: String, val expiresAt: String) : PairingState()
    data class Joined(val success: Boolean) : PairingState()
    data class Error(val message: String) : PairingState()
}

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val relationshipRepository: RelationshipRepository
) : ViewModel() {

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    fun createPairingKey() {
        viewModelScope.launch {
            _pairingState.value = PairingState.Loading
            val result = relationshipRepository.createPairingKey()
            if (result is Resource.Success) {
                _pairingState.value = PairingState.Created(result.data!!.pairingKey, result.data.expiresAt)
                // Also trigger a refresh so MainViewModel sees the new "Pending" state
                relationshipRepository.refreshCurrentRelationship()
            } else {
                _pairingState.value = PairingState.Error(result.message ?: "Failed to create pairing key")
            }
        }
    }

    fun joinRelationship(pairingKey: String) {
        viewModelScope.launch {
            _pairingState.value = PairingState.Loading
            val result = relationshipRepository.joinRelationship(pairingKey)
            if (result is Resource.Success) {
                _pairingState.value = PairingState.Joined(true)
                relationshipRepository.refreshCurrentRelationship()
            } else {
                _pairingState.value = PairingState.Error(result.message ?: "Failed to join space")
            }
        }
    }

    fun resetState() {
        _pairingState.value = PairingState.Idle
    }
}
