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
import com.moment.app.data.remote.UserDto
import com.moment.app.domain.repository.AuthRepository
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.time.DayOfWeek

@HiltViewModel
class UsViewModel @Inject constructor(
    private val relationshipRepository: RelationshipRepository,
    private val momentRepository: MomentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UsUiState>(UsUiState.Loading)
    val uiState: StateFlow<UsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userResult = authRepository.getProfile()
            val currentUser = userResult.getOrNull()

            relationshipRepository.relationshipState.collectLatest { resource ->
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
                                val favorites = moments.filter { it.isFavorite }.take(10)

                                _uiState.value = UsUiState.Success(
                                    relationship = rel,
                                    currentUser = currentUser,
                                    moments = moments,
                                    favorites = favorites
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

sealed class UsUiState {
    object Loading : UsUiState()
    object NotPaired : UsUiState()
    data class Success(
        val relationship: RelationshipDto,
        val currentUser: UserDto?,
        val moments: List<MomentEntity>,
        val favorites: List<MomentEntity>
    ) : UsUiState()
    data class Error(val message: String) : UsUiState()
}
