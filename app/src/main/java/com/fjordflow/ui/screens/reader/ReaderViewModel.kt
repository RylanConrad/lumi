package com.fjordflow.ui.screens.reader

import androidx.lifecycle.*
import com.fjordflow.data.db.entity.BookEntity
import com.fjordflow.data.repository.BookRepository
import com.fjordflow.data.repository.WordRepository
import com.fjordflow.data.translation.GeminiClient
import com.fjordflow.data.translation.TranslationResult
import com.fjordflow.data.translation.TranslationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TranslationState(
    val word: String = "",
    val context: String = "",
    val result: TranslationResult = TranslationResult(""),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val sourceWord: String? = null
)

data class ReaderUiState(
    val text: String = "",
    val book: BookEntity? = null,
    val selectedWord: TranslationState? = null,
    val isTranslating: Boolean = false,
    val isReconstructing: Boolean = false
)

data class LibraryUiState(
    val books: List<BookEntity> = emptyList(),
    val isLoading: Boolean = false
)

class ReaderViewModel(
    private val wordRepo: WordRepository,
    private val bookRepo: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val libraryState: StateFlow<LibraryUiState> = bookRepo.getAllBooks()
        .map { LibraryUiState(books = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState(isLoading = true))

    fun openBook(book: BookEntity) {
        _uiState.update { it.copy(book = book, text = book.content) }
        viewModelScope.launch {
            bookRepo.updateBook(book.copy(lastReadAt = System.currentTimeMillis()))
        }
    }

    fun closeBook() {
        _uiState.update { it.copy(book = null, text = "") }
    }

    fun updateText(newText: String) {
        _uiState.update { it.copy(text = newText) }
    }

    fun reconstructAndAddBook(title: String, rawContent: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReconstructing = true) }
            try {
                val cleanContent = GeminiClient.reconstructPage(rawContent)
                addBook(title, cleanContent)
            } catch (e: Exception) {
                // Fallback to raw if AI fails
                addBook(title, rawContent)
            } finally {
                _uiState.update { it.copy(isReconstructing = false) }
            }
        }
    }

    fun addBook(title: String, content: String) {
        viewModelScope.launch {
            val type = when {
                content.startsWith("%PDF") -> "PDF"
                content.contains("mimetypeapplication/epub+zip") -> "EPUB"
                else -> "TEXT"
            }
            bookRepo.insertBook(
                BookEntity(
                    title = title,
                    content = content,
                    type = type
                )
            )
        }
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

    fun onSentenceLongPressed(sentence: String, originalWord: String) {
        _uiState.update { it.copy(isTranslating = true, selectedWord = null) }
        viewModelScope.launch {
            val result = TranslationService.translate(sentence, "")
            _uiState.update {
                it.copy(
                    isTranslating = false,
                    selectedWord = TranslationState(
                        word = sentence,
                        context = "",
                        result = result,
                        sourceWord = originalWord
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
        
        val frontText = if (state.sourceWord != null && state.word.contains(state.sourceWord, ignoreCase = true)) {
            val regex = Regex("(${Regex.escape(state.sourceWord)})", RegexOption.IGNORE_CASE)
            state.word.replace(regex, "**$1**")
        } else {
            "**${state.word}**"
        }

        viewModelScope.launch {
            wordRepo.saveWordAndCreateCard(state.word, state.context, state.result.translation, frontOverride = frontText)
            _uiState.update { it.copy(selectedWord = state.copy(isSaving = false, isSaved = true)) }
        }
    }

    class Factory(
        private val wordRepo: WordRepository,
        private val bookRepo: BookRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReaderViewModel(wordRepo, bookRepo) as T
    }
}
