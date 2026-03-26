package com.fjordflow.ui.screens.reader

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showNewBookDialog by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    // PDF import state
    var pendingPdfUri by remember { mutableStateOf<Uri?>(null) }
    var showPdfDialog by remember { mutableStateOf(false) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingPdfUri = uri
            showPdfDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Book") },
                                leadingIcon = { Icon(Icons.Outlined.Book, contentDescription = null) },
                                onClick = { showMenu = false; showNewBookDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Import PDF") },
                                leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    pdfLauncher.launch(arrayOf("application/pdf"))
                                }
                            )
                        }
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

    if (showNewBookDialog) {
        NewBookDialog(
            onConfirm = { title ->
                vm.createBook(title)
                showNewBookDialog = false
            },
            onDismiss = { showNewBookDialog = false }
        )
    }

    // PDF import: show title dialog pre-filled with the filename
    if (showPdfDialog && pendingPdfUri != null) {
        val uri = pendingPdfUri!!
        val suggestedTitle = remember(uri) {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).removeSuffix(".pdf") else ""
            } ?: ""
        }
        PdfImportDialog(
            suggestedTitle = suggestedTitle,
            onConfirm = { title ->
                // Keep read access to this URI across app restarts
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                vm.importPdf(uri = uri, title = title, onReady = { book -> onBookClick(book) })
                showPdfDialog = false
                pendingPdfUri = null
            },
            onDismiss = {
                showPdfDialog = false
                pendingPdfUri = null
            }
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
            "Tap + to create a book or import a PDF.",
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

@Composable
fun PdfImportDialog(
    suggestedTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(suggestedTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Each page will be extracted using Gemini Vision and saved as a readable page.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Book title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title.trim()) },
                enabled = title.isNotBlank()
            ) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
