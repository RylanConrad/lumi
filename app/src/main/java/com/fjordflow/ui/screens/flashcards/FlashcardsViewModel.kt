package com.fjordflow.ui.screens.flashcards

import androidx.lifecycle.*
import com.fjordflow.data.db.entity.FlashCardEntity
import com.fjordflow.data.repository.FlashCardRepository
import com.fjordflow.data.repository.ReviewQuality
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FlashcardsUiState(
    val dueCards: List<FlashCardEntity> = emptyList(),
    val totalCount: Int = 0,
    val dueCount: Int = 0,
    val isFlipped: Boolean = false,
    val sessionComplete: Boolean = false
)

class FlashcardsViewModel(private val repo: FlashCardRepository) : ViewModel() {

    val uiState: StateFlow<FlashcardsUiState> = combine(
        repo.getDueCards(),
        repo.getTotalCount(),
        repo.getDueCount()
    ) { due, total, dueCount ->
        FlashcardsUiState(
            dueCards = due,
            totalCount = total,
            dueCount = dueCount,
            sessionComplete = due.isEmpty() && total > 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FlashcardsUiState())

    private val _isFlipped = MutableStateFlow(false)
    val isFlipped: StateFlow<Boolean> = _isFlipped.asStateFlow()

    fun flip() { _isFlipped.update { !it } }

    fun review(card: FlashCardEntity, quality: ReviewQuality) {
        _isFlipped.value = false
        viewModelScope.launch {
            repo.reviewCard(card, quality)
        }
    }

    class Factory(private val repo: FlashCardRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FlashcardsViewModel(repo) as T
    }
}
