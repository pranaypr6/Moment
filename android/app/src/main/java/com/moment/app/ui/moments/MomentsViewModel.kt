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
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@HiltViewModel
class MomentsViewModel @Inject constructor(
    private val relationshipRepository: RelationshipRepository,
    private val momentRepository: MomentRepository,
    private val api: com.moment.app.data.remote.MomentApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<MomentsUiState>(MomentsUiState.Loading)
    val uiState: StateFlow<MomentsUiState> = _uiState.asStateFlow()

    private val _actionSuccessState = MutableStateFlow<String?>(null)
    val actionSuccessState: StateFlow<String?> = _actionSuccessState.asStateFlow()

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
                                    val groupedMoments = groupMomentsEmotionally(moments)
                                    _uiState.value = MomentsUiState.Success(
                                        partnerId = rel.partner.id,
                                        partnerName = rel.partner.displayName ?: "Partner",
                                        isPausedByPartner = rel.isPausedByPartner,
                                        latestMoment = latestMoment,
                                        groupedMoments = groupedMoments
                                    )
                                }
                        }
                    }
                }
            }
        }
    }

    private fun groupMomentsEmotionally(moments: List<MomentEntity>): Map<String, List<MomentEntity>> {
        if (moments.isEmpty()) return emptyMap()

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val startOfLastWeek = startOfWeek.minusWeeks(1)
        val endOfLastWeek = startOfWeek.minusDays(1)
        
        val lastWeekendStart = startOfLastWeek.plusDays(5) // Saturday of last week
        val lastWeekendEnd = endOfLastWeek // Sunday of last week

        val grouped = mutableMapOf<String, MutableList<MomentEntity>>()

        // Sort moments chronologically descending (newest first)
        val sortedMoments = moments.sortedByDescending { it.createdAt }

        // Find the oldest 2 moments for "Our Beginning"
        val oldestMoments = sortedMoments.takeLast(2).map { it.id }.toSet()

        sortedMoments.forEach { moment ->
            if (oldestMoments.contains(moment.id) && sortedMoments.size > 3) {
                grouped.getOrPut("Our Beginning") { mutableListOf() }.add(moment)
                return@forEach
            }

            val momentDate = Instant.ofEpochMilli(moment.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()

            val groupName = when {
                momentDate == today -> "Today"
                momentDate == yesterday -> "Yesterday"
                momentDate in lastWeekendStart..lastWeekendEnd -> "Last Weekend"
                momentDate.year == today.year && momentDate.month == today.month -> "This Month"
                else -> "${momentDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${momentDate.year}"
            }

            grouped.getOrPut(groupName) { mutableListOf() }.add(moment)
        }

        // Maintain order of groups
        val orderedKeys = listOf("Today", "Yesterday", "Last Weekend", "This Month") +
                grouped.keys.filter { it != "Today" && it != "Yesterday" && it != "Last Weekend" && it != "This Month" && it != "Our Beginning" } +
                listOf("Our Beginning")

        val result = linkedMapOf<String, List<MomentEntity>>()
        orderedKeys.forEach { key ->
            if (grouped.containsKey(key)) {
                result[key] = grouped[key]!!
            }
        }

        return result
    }

    fun toggleFavorite(momentId: String) {
        viewModelScope.launch {
            momentRepository.toggleFavorite(momentId)
        }
    }

    fun sendEmotionalAction(action: com.moment.app.ui.moments.EmotionalAction) {
        val currentState = _uiState.value
        if (currentState is MomentsUiState.Success) {
            viewModelScope.launch {
                try {
                    val typeInt = when (action.actionName) {
                        "ThinkingOfYou" -> 0
                        "Punch" -> 1
                        "Cuddle" -> 2
                        "Kiss" -> 3
                        "MissYou" -> 4
                        else -> 0
                    }
                    
                    val rel = relationshipRepository.relationshipState.firstOrNull()?.data
                    if (rel != null) {
                        val req = com.moment.app.data.remote.SendPresenceRequest(rel.id, typeInt)
                        api.sendPresenceSignal(req)
                        
                        // Show success state
                        _actionSuccessState.value = "Sent ${action.emoji}"
                        kotlinx.coroutines.delay(2000)
                        _actionSuccessState.value = null
                    }
                } catch (e: Exception) {
                    if (e.message?.contains("429") == true) {
                        _actionSuccessState.value = "You're sending too many signals!"
                        kotlinx.coroutines.delay(2000)
                        _actionSuccessState.value = null
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
        val latestMoment: MomentEntity?,
        val groupedMoments: Map<String, List<MomentEntity>>
    ) : MomentsUiState()
    data class Error(val message: String) : MomentsUiState()
}
