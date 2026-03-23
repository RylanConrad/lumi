package com.fjordflow.ui.screens.reader

import androidx.lifecycle.*
import com.fjordflow.data.repository.WordRepository
import com.fjordflow.data.translation.TranslationResult
import com.fjordflow.data.translation.TranslationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val DEFAULT_TEXT = """
Le soleil se couchait sur les montagnes quand Marie décida de partir. Elle prit son manteau, ferma la porte à clé, et marcha vers la gare. La ville était silencieuse à cette heure-là, et ses pas résonnaient sur les pavés froids.

Dans sa poche, elle avait une lettre — une lettre qu'elle n'avait jamais eu le courage d'envoyer. Elle la porta à ses lèvres, puis la glissa dans la première boîte aux lettres qu'elle trouva.

Il était temps de recommencer.
""".trimIndent()

data class TranslationState(
    val word: String = "",
    val context: String = "",
    val result: TranslationResult = TranslationResult(""),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

data class ReaderUiState(
    val text: String = DEFAULT_TEXT,
    val selectedWord: TranslationState? = null,
    val isTranslating: Boolean = false
)

class ReaderViewModel(private val repo: WordRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun updateText(newText: String) {
        _uiState.update { it.copy(text = newText) }
    }

    fun onWordTapped(word: String, sentenceContext: String) {
        _uiState.update { it.copy(isTranslating = true, selectedWord = null) }
        viewModelScope.launch {
            val result = TranslationService.translate(word, sentenceContext)
            _uiState.update {
                it.copy(
                    isTranslating = false,
                    selectedWord = TranslationState(
                        word = word,
                        context = sentenceContext,
                        result = result
                    )
                )
            }
        }
    }

    fun dismissTranslation() {
        _uiState.update { it.copy(selectedWord = null, isTranslating = false) }
    }

    fun saveToFlashcards() {
        val state = _uiState.value.selectedWord ?: return
        _uiState.update { it.copy(selectedWord = state.copy(isSaving = true)) }
        viewModelScope.launch {
            repo.saveWordAndCreateCard(state.word, state.context, state.result.translation)
            _uiState.update { it.copy(selectedWord = state.copy(isSaving = false, isSaved = true)) }
        }
    }

    class Factory(private val repo: WordRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReaderViewModel(repo) as T
    }
}
