package com.moment.app.ui.settings

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

@HiltViewModel
class SpaceSettingsViewModel @Inject constructor(
    private val relationshipRepository: RelationshipRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<Resource<RelationshipDto?>>(Resource.Loading())
    val uiState: StateFlow<Resource<RelationshipDto?>> = _uiState.asStateFlow()

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
            relationshipRepository.togglePause()
        }
    }

    fun unpair() {
        viewModelScope.launch {
            relationshipRepository.unpair()
        }
    }
}
