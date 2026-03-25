package com.fjordflow.ui.screens.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fjordflow.data.db.entity.BookEntity
import com.fjordflow.ui.theme.FjordBlue
import com.fjordflow.ui.theme.MistGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    vm: ReaderViewModel,
    onBookClick: (BookEntity) -> Unit
) {
    val uiState by vm.libraryState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = "New book")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.books.isEmpty()) {
            EmptyLibrary(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.books, key = { it.id }) { book ->
                    BookFolderItem(
                        book = book,
                        onClick = { onBookClick(book) },
                        onLongClick = { bookToDelete = book }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        NewBookDialog(
            onConfirm = { title ->
                vm.createBook(title)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete \"${book.title}\"?") },
            text = { Text("This will delete the book and all its scanned pages.") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteBook(book); bookToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookFolderItem(
    book: BookEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FjordBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Book, contentDescription = null, tint = FjordBlue)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Long press to delete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Book,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MistGray
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No books yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Tap + to create a book, then scan pages into it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NewBookDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Book") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Book title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title.trim()) },
                enabled = title.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
