package com.fjordflow.ui.screens.reader

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fjordflow.ui.theme.FjordBlue
import com.fjordflow.ui.theme.GlacierTeal
import com.fjordflow.ui.theme.MistGray
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(vm: ReaderViewModel, onBack: () -> Unit) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Reader",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit text",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MistGray, thickness = 1.dp)

            // Readable text area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 32.dp)
            ) {
                ClickableText(
                    text = uiState.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 30.sp,
                        letterSpacing = 0.3.sp
                    ),
                    onWordTap = { word, context ->
                        vm.onWordTapped(word, context)
                        scope.launch { sheetState.show() }
                    },
                    onSentenceTap = { sentence, word ->
                        vm.onSentenceLongPressed(sentence, word)
                        scope.launch { sheetState.show() }
                    }
                )
            }
        }

        // Loading indicator
        if (uiState.isTranslating) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = FjordBlue
            )
        }
    }

    // Translation bottom sheet
    if (uiState.selectedWord != null || uiState.isTranslating) {
        ModalBottomSheet(
            onDismissRequest = { vm.dismissTranslation() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MistGray)
                )
            }
        ) {
            uiState.selectedWord?.let { state ->
                TranslationSheet(
                    state = state,
                    onSave = { vm.saveToFlashcards() },
                    onDismiss = {
                        scope.launch { sheetState.hide() }
                        vm.dismissTranslation()
                    }
                )
            }
        }
    }

    // Paste text dialog
    if (showEditDialog) {
        PasteTextDialog(
            currentText = uiState.text,
            onConfirm = { newText ->
                vm.updateText(newText)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
internal fun TranslationSheet(
    state: TranslationState,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val result = state.result
    val isSentence = state.word.contains(" ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 8.dp)
            .padding(bottom = 32.dp)
    ) {
        // Word + part of speech badge
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = if (isSentence) "Sentence Translation" else state.word,
                style = MaterialTheme.typography.headlineMedium,
                color = FjordBlue,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (result.partOfSpeech.isNotBlank() && result.partOfSpeech != "sentence") {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = result.partOfSpeech,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (isSentence) {
            Text(
                text = state.word,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic
            )
        }

        Spacer(Modifier.height(16.dp))

        HorizontalDivider(color = MistGray)
        Spacer(Modifier.height(16.dp))

        // Translation
        Text(
            text = "TRANSLATION",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = result.translation,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Context Notes
        if (result.contextNotes.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "CONTEXT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = result.contextNotes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(24.dp))

        // Save button
        Button(
            onClick = onSave,
            enabled = !state.isSaving && !state.isSaved,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isSaved) GlacierTeal else FjordBlue
            )
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    if (state.isSaved) Icons.Outlined.Check else Icons.Outlined.AddCard,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (state.isSaved) "Saved to Flashcards" else "Save to Flashcards",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun PasteTextDialog(
    currentText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste French Text", style = MaterialTheme.typography.titleMedium) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                placeholder = { Text("Paste your French text here…") },
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Renders the text as individually tappable word tokens.
 * Punctuation is stripped for lookup but preserved in display.
 * Long press on a sentence translates the whole sentence.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ClickableText(
    text: String,
    style: TextStyle,
    onWordTap: (word: String, sentence: String) -> Unit,
    onSentenceTap: (sentence: String, word: String) -> Unit
) {
    val paragraphs = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        paragraphs.forEach { paragraph ->
            if (paragraph.isBlank()) return@forEach
            
            // Split paragraph into sentences, preserving punctuation context
            val sentences = paragraph.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
            
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                sentences.forEachIndexed { sIdx, sentence ->
                    val tokens = tokenize(sentence)
                    tokens.forEachIndexed { tIdx, token ->
                        // Determine if we need to add a trailing space between sentences
                        val isLastTokenInSentence = tIdx == tokens.lastIndex
                        val isLastSentenceInParagraph = sIdx == sentences.lastIndex
                        val needsInterSentenceSpace = isLastTokenInSentence && !isLastSentenceInParagraph
                        val displayTrailingSpace = if (token.trailingSpace || needsInterSentenceSpace) " " else ""
                        
                        if (token.isWord) {
                            Text(
                                text = token.display + displayTrailingSpace,
                                style = style,
                                modifier = Modifier.combinedClickable(
                                    onClick = { onWordTap(token.clean, sentence) },
                                    onLongClick = { onSentenceTap(sentence, token.display) }
                                )
                            )
                        } else {
                            Text(
                                text = token.display + displayTrailingSpace,
                                style = style,
                                modifier = Modifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = { onSentenceTap(sentence, "") }
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Token(
    val display: String,
    val clean: String,
    val isWord: Boolean,
    val trailingSpace: Boolean
)

private val wordRegex = Regex("[\\p{L}\\p{M}'’]+")
private val elisionPrefixes = setOf("l", "d", "n", "s", "j", "m", "t", "qu", "c")

private fun tokenize(text: String): List<Token> {
    val tokens = mutableListOf<Token>()
    val parts = text.split(Regex("\\s+"))
    
    parts.forEachIndexed { idx, part ->
        val trailingSpace = idx < parts.lastIndex
        if (part.isEmpty()) return@forEachIndexed

        val match = wordRegex.find(part)
        if (match != null) {
            val start = match.range.first
            val end = match.range.last + 1
            
            if (start > 0) {
                val prefix = part.substring(0, start)
                tokens.add(Token(display = prefix, clean = prefix, isWord = false, trailingSpace = false))
            }
            
            val word = match.value
            val suffix = part.substring(end)
            
            // Handle elisions like d'envoyer -> [d', envoyer]
            val splitIndex = word.indexOfAny(charArrayOf('\'', '’'))
            if (splitIndex > 0 && splitIndex < word.length - 1) {
                val prefix = word.substring(0, splitIndex).lowercase()
                if (elisionPrefixes.contains(prefix)) {
                    val elision = word.substring(0, splitIndex + 1)
                    val mainWord = word.substring(splitIndex + 1)
                    
                    tokens.add(Token(display = elision, clean = elision.lowercase(), isWord = true, trailingSpace = false))
                    tokens.add(Token(display = mainWord, clean = mainWord.lowercase(), isWord = true, trailingSpace = suffix.isEmpty() && trailingSpace))
                } else {
                    tokens.add(Token(display = word, clean = word.lowercase(), isWord = true, trailingSpace = suffix.isEmpty() && trailingSpace))
                }
            } else {
                tokens.add(Token(display = word, clean = word.trim('\'', '’').lowercase(), isWord = true, trailingSpace = suffix.isEmpty() && trailingSpace))
            }
            
            if (suffix.isNotEmpty()) {
                tokens.add(Token(display = suffix, clean = suffix, isWord = false, trailingSpace = trailingSpace))
            }
        } else {
            tokens.add(Token(display = part, clean = part, isWord = false, trailingSpace = trailingSpace))
        }
    }
    return tokens
}
