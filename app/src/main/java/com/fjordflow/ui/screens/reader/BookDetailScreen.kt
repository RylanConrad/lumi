package com.fjordflow.ui.screens.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fjordflow.data.db.entity.BookEntity
import com.fjordflow.data.db.entity.PageEntity
import com.fjordflow.ui.theme.FjordBlue
import com.fjordflow.ui.theme.MistGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    vm: ReaderViewModel,
    onPageClick: (PageEntity) -> Unit,
    onBack: () -> Unit
) {
    val uiState by vm.bookDetailState.collectAsState()
    val book = uiState.book ?: return

    var showScanner by remember { mutableStateOf(false) }
    var scannedText by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pageToDelete by remember { mutableStateOf<PageEntity?>(null) }

    if (showScanner) {
        CameraScanner(
            onTextScanned = { text ->
                scannedText = text
                showScanner = false
                showConfirmDialog = true
            },
            onDismiss = { showScanner = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(book.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${uiState.pages.size} page${if (uiState.pages.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showScanner = true }) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = "Scan page")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.pages.isEmpty()) {
            EmptyBookDetail(
                modifier = Modifier.padding(padding),
                onScan = { showScanner = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.pages, key = { it.id }) { page ->
                    PageItem(
                        page = page,
                        onClick = { onPageClick(page) },
                        onLongClick = { pageToDelete = page }
                    )
                }
            }
        }
    }

    // Confirm adding scanned page
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false; scannedText = "" },
            title = { Text("Add Page ${(uiState.pages.size + 1)}") },
            text = {
                Column {
                    Text(
                        "Preview:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = scannedText.take(400).let { if (scannedText.length > 400) "$it…" else it },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addPage(book.id, scannedText)
                    showConfirmDialog = false
                    scannedText = ""
                }) { Text("Add Page") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false; scannedText = "" }) {
                    Text("Discard")
                }
            }
        )
    }

    // Confirm deleting page
    pageToDelete?.let { page ->
        AlertDialog(
            onDismissRequest = { pageToDelete = null },
            title = { Text("Delete Page ${page.pageNumber}?") },
            text = { Text("This page's content will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = { vm.deletePage(page); pageToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pageToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageItem(
    page: PageEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateStr = remember(page.addedAt) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(page.addedAt))
    }

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
                    .background(FjordBlue.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${page.pageNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = FjordBlue,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = page.content.take(80).replace('\n', ' '),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Added $dateStr · Long press to delete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyBookDetail(
    modifier: Modifier = Modifier,
    onScan: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MistGray
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No pages yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onScan) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Scan First Page")
        }
    }
}
