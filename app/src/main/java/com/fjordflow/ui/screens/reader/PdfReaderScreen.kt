package com.fjordflow.ui.screens.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fjordflow.ui.theme.FjordBlue
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(vm: ReaderViewModel, onBack: () -> Unit) {
    val bookDetailState by vm.bookDetailState.collectAsState()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val book = bookDetailState.book ?: return
    val sourceUri = book.sourceUri ?: return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // PdfRenderer is not thread-safe: synchronize all page operations on it
    val renderer = remember(sourceUri) {
        val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(Uri.parse(sourceUri), "r")!!
        PdfRenderer(pfd)
    }
    DisposableEffect(sourceUri) {
        onDispose { renderer.close() }
    }

    val pageCount = renderer.pageCount
    // Cache rendered bitmaps so pages don't re-render on scroll
    val pageCache = remember { mutableStateMapOf<Int, Bitmap>() }
    var isDetecting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val pagerState = rememberPagerState(initialPage = book.lastPageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))) { pageCount }

            // Persist reading progress whenever the page settles
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    vm.savePageProgress(book.id, page)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                PdfPageView(
                    pageIndex = pageIndex,
                    renderer = renderer,
                    pageCache = pageCache,
                    onTap = { tapX, tapY, bitmap ->
                        if (!isDetecting && !uiState.isTranslating) {
                            isDetecting = true
                            detectWordAtTap(
                                tapX = tapX,
                                tapY = tapY,
                                bitmap = bitmap,
                                recognizer = recognizer,
                                selectSentence = false,
                                onResult = { word, sentence ->
                                    isDetecting = false
                                    vm.onWordTapped(word, sentence)
                                    scope.launch { sheetState.show() }
                                },
                                onFailure = { isDetecting = false }
                            )
                        }
                    },
                    onLongPress = { tapX, tapY, bitmap ->
                        if (!isDetecting && !uiState.isTranslating) {
                            isDetecting = true
                            detectWordAtTap(
                                tapX = tapX,
                                tapY = tapY,
                                bitmap = bitmap,
                                recognizer = recognizer,
                                selectSentence = true,
                                onResult = { word, sentence ->
                                    isDetecting = false
                                    vm.onSentenceLongPressed(sentence, word)
                                    scope.launch { sheetState.show() }
                                },
                                onFailure = { isDetecting = false }
                            )
                        }
                    }
                )
            }

            // Page indicator
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 2.dp
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / $pageCount",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            if (isDetecting || uiState.isTranslating) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

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
                        .padding(horizontal = 16.dp)
                ) {}
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
}

@Composable
private fun PdfPageView(
    pageIndex: Int,
    renderer: PdfRenderer,
    pageCache: MutableMap<Int, Bitmap>,
    onTap: (tapX: Int, tapY: Int, bitmap: Bitmap) -> Unit,
    onLongPress: (tapX: Int, tapY: Int, bitmap: Bitmap) -> Unit
) {
    var bitmap by remember(pageIndex) { mutableStateOf(pageCache[pageIndex]) }

    LaunchedEffect(pageIndex) {
        if (bitmap == null) {
            val rendered = withContext(Dispatchers.IO) {
                synchronized(renderer) {
                    val page = renderer.openPage(pageIndex)
                    // 2× scale gives ML Kit enough resolution to read individual words
                    val bmp = Bitmap.createBitmap(
                        page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888
                    ).also { it.eraseColor(android.graphics.Color.WHITE) }
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bmp
                }
            }
            pageCache[pageIndex] = rendered
            bitmap = rendered
        }
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        // Pair: (screen-px position, isLongPress)
        var tapIndicator by remember { mutableStateOf<Pair<Offset, Boolean>?>(null) }
        LaunchedEffect(tapIndicator) {
            if (tapIndicator != null) {
                delay(350)
                tapIndicator = null
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    // aspectRatio sets the correct height without needing BoxWithConstraints
                    .aspectRatio(currentBitmap.width.toFloat() / currentBitmap.height.toFloat())
                    .pointerInput(currentBitmap) {
                        // PointerInputScope.size gives the rendered element dimensions —
                        // capture once here before entering the gesture loop.
                        val elementWidthPx = size.width.toFloat()
                        val elementHeightPx = size.height.toFloat()
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downPos = down.position
                            try {
                                val upChange = withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                    waitForUpOrCancellation()
                                }
                                // Finger lifted before timeout = tap
                                if (upChange != null) {
                                    tapIndicator = Pair(downPos, false)
                                    val bitmapX = (downPos.x / elementWidthPx * currentBitmap.width).toInt()
                                        .coerceIn(0, currentBitmap.width - 1)
                                    val bitmapY = (downPos.y / elementHeightPx * currentBitmap.height).toInt()
                                        .coerceIn(0, currentBitmap.height - 1)
                                    onTap(bitmapX, bitmapY, currentBitmap)
                                }
                            } catch (_: TimeoutCancellationException) {
                                // Timeout = long press
                                tapIndicator = Pair(downPos, true)
                                val bitmapX = (downPos.x / elementWidthPx * currentBitmap.width).toInt()
                                    .coerceIn(0, currentBitmap.width - 1)
                                val bitmapY = (downPos.y / elementHeightPx * currentBitmap.height).toInt()
                                    .coerceIn(0, currentBitmap.height - 1)
                                onLongPress(bitmapX, bitmapY, currentBitmap)
                                waitForUpOrCancellation()
                            }
                        }
                    }
            )

            // Tap/long-press feedback circle — offset lambda has Density receiver so dp→px works directly
            tapIndicator?.let { (pos, isLong) ->
                Box(
                    modifier = Modifier
                        .offset {
                            val sizePx = if (isLong) 80.dp.toPx() else 56.dp.toPx()
                            IntOffset(
                                (pos.x - sizePx / 2).roundToInt(),
                                (pos.y - sizePx / 2).roundToInt()
                            )
                        }
                        .size(if (isLong) 80.dp else 56.dp)
                        .background(
                            color = FjordBlue.copy(alpha = if (isLong) 0.35f else 0.22f),
                            shape = CircleShape
                        )
                )
            }
        }
    } else {
        // Placeholder while page renders
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.77f),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Crops a horizontal strip around the tap point from the rendered page bitmap
 * and runs ML Kit text recognition on it. Since PDF pages are flat, high-contrast
 * printed text, ML Kit is very accurate here.
 *
 * The strip is wide enough to capture the full line (for sentence context) and
 * tall enough to capture ascenders and descenders.
 */
private fun detectWordAtTap(
    tapX: Int,
    tapY: Int,
    bitmap: Bitmap,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    selectSentence: Boolean = false,
    onResult: (word: String, sentence: String) -> Unit,
    onFailure: () -> Unit
) {
    val hPad = 600  // horizontal padding — captures full line for sentence context
    val vPad = 80   // vertical padding — captures line height including ascenders/descenders

    val left   = (tapX - hPad).coerceAtLeast(0)
    val top    = (tapY - vPad).coerceAtLeast(0)
    val right  = (tapX + hPad).coerceAtMost(bitmap.width)
    val bottom = (tapY + vPad).coerceAtMost(bitmap.height)

    if (right <= left || bottom <= top) { onFailure(); return }

    val crop = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    val tapXInCrop = tapX - left

    val image = InputImage.fromBitmap(crop, 0)
    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            // Find the element whose horizontal center is closest to the tap x position
            val tappedElement = visionText.textBlocks
                .flatMap { it.lines }
                .flatMap { it.elements }
                .minByOrNull { elem ->
                    val box = elem.boundingBox ?: return@minByOrNull Int.MAX_VALUE
                    abs(box.centerX() - tapXInCrop)
                }

            // Get the full line containing the tapped element for translation context
            val line = visionText.textBlocks
                .flatMap { it.lines }
                .find { line -> line.elements.any { it == tappedElement } }

            val sentence = line?.text ?: ""

            val word = if (selectSentence) {
                sentence
            } else {
                tappedElement?.text
                    ?.trim('.', ',', ';', ':', '!', '?', '"', '"', '"', '\'', '(', ')', '«', '»')
                    ?: ""
            }

            if (word.isNotBlank()) {
                onResult(word, sentence)
            } else {
                onFailure()
            }
        }
        .addOnFailureListener { e ->
            Log.e("PdfReader", "Word detection failed", e)
            onFailure()
        }
}
