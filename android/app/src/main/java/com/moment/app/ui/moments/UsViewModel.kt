package com.moment.app.ui.moments

import kotlinx.coroutines.ExperimentalCoroutinesApi

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

@OptIn(ExperimentalCoroutinesApi::class)
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

            relationshipRepository.relationshipState
                .flatMapLatest { resource ->
                    when (resource) {
                        is Resource.Idle -> flowOf(UsUiState.Loading)
                        is Resource.Loading -> flowOf(UsUiState.Loading)
                        is Resource.Error -> flowOf(UsUiState.Error(resource.message ?: "Unknown error"))
                        is Resource.Success -> {
                            val rel = resource.data
                            if (rel == null) {
                                flowOf(UsUiState.NotPaired)
                            } else {
                                momentRepository.getScrapbookMoments(rel.id).map { moments ->
                                    val favorites = moments.filter { it.isFavorite }.take(10)
                                    UsUiState.Success(
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
                .onEach { state -> _uiState.value = state }
                .launchIn(viewModelScope)
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
            val currentState = (uiState.value as? UsUiState.Success)?.relationship ?: return@launch
            val newPauseState = !currentState.isPausedByMe
            relationshipRepository.setPause(newPauseState)
        }
    }

    fun unpair() {
        viewModelScope.launch {
            relationshipRepository.unpair()
        }
    }

    fun updateAnniversaryDate(anniversaryDate: String) {
        viewModelScope.launch {
            relationshipRepository.updateAnniversary(anniversaryDate)
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
