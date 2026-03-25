package com.fjordflow.ui.screens.reader

import androidx.lifecycle.*
import com.fjordflow.data.db.entity.BookEntity
import com.fjordflow.data.db.entity.PageEntity
import com.fjordflow.data.repository.BookRepository
import com.fjordflow.data.repository.PageRepository
import com.fjordflow.data.repository.WordRepository
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
    val pageTitle: String = "",
    val selectedWord: TranslationState? = null,
    val isTranslating: Boolean = false
)

data class LibraryUiState(
    val books: List<BookEntity> = emptyList(),
    val isLoading: Boolean = false
)

data class BookDetailUiState(
    val book: BookEntity? = null,
    val pages: List<PageEntity> = emptyList()
)

class ReaderViewModel(
    private val wordRepo: WordRepository,
    private val bookRepo: BookRepository,
    private val pageRepo: PageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val libraryState: StateFlow<LibraryUiState> = bookRepo.getAllBooks()
        .map { LibraryUiState(books = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState(isLoading = true))

    private val _selectedBook = MutableStateFlow<BookEntity?>(null)

    val bookDetailState: StateFlow<BookDetailUiState> = _selectedBook
        .flatMapLatest { book ->
            if (book == null) flowOf(BookDetailUiState())
            else pageRepo.getPagesForBook(book.id)
                .map { pages -> BookDetailUiState(book = book, pages = pages) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookDetailUiState())

    fun selectBook(book: BookEntity) {
        _selectedBook.value = book
    }

    fun openPage(page: PageEntity) {
        val book = _selectedBook.value
        _uiState.update {
            it.copy(
                text = page.content,
                pageTitle = if (book != null) "Page ${page.pageNumber}" else ""
            )
        }
    }

    fun updateText(newText: String) {
        _uiState.update { it.copy(text = newText) }
    }

    fun createBook(title: String) {
        viewModelScope.launch {
            bookRepo.insertBook(BookEntity(title = title))
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            bookRepo.deleteBook(book)
        }
    }

    fun addPage(bookId: Int, content: String) {
        viewModelScope.launch {
            val nextPageNum = (pageRepo.getMaxPageNumber(bookId) ?: 0) + 1
            pageRepo.insertPage(PageEntity(bookId = bookId, pageNumber = nextPageNum, content = content))
        }
    }

    fun deletePage(page: PageEntity) {
        viewModelScope.launch {
            pageRepo.deletePage(page)
        }
    }

    fun onWordTapped(word: String, sentenceContext: String) {
        _uiState.update { it.copy(isTranslating = true, selectedWord = null) }
        viewModelScope.launch {
            val result = TranslationService.translate(word, sentenceContext)
            _uiState.update {
                it.copy(
                    isTranslating = false,
                    selectedWord = TranslationState(word = word, context = sentenceContext, result = result)
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
        private val bookRepo: BookRepository,
        private val pageRepo: PageRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReaderViewModel(wordRepo, bookRepo, pageRepo) as T
    }
}
