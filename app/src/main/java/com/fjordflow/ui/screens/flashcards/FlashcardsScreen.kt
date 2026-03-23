package com.fjordflow.ui.screens.flashcards

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import com.fjordflow.data.db.entity.FlashCardEntity
import com.fjordflow.data.repository.ReviewQuality
import com.fjordflow.ui.theme.*
import kotlin.math.abs

@Composable
fun FlashcardsScreen(vm: FlashcardsViewModel) {
    val state by vm.uiState.collectAsState()
    val isFlipped by vm.isFlipped.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Flashcards",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (state.totalCount > 0) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "${state.dueCount} due · ${state.totalCount} total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = MistGray)

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                state.totalCount == 0 -> EmptyState()
                state.sessionComplete -> SessionComplete()
                state.dueCards.isNotEmpty() -> {
                    val card = state.dueCards.first()
                    CardStack(
                        cards = state.dueCards,
                        isFlipped = isFlipped,
                        onFlip = { vm.flip() },
                        onReview = { quality -> vm.review(card, quality) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardStack(
    cards: List<FlashCardEntity>,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onReview: (ReviewQuality) -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }
    val animatedDrag by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "drag"
    )
    val card = cards.first()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = if (isFlipped) "Translation" else "Tap to reveal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(12.dp))

        // Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .graphicsLayer {
                    translationX = animatedDrag
                    rotationZ = animatedDrag / 30f
                }
                .pointerInput(card.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                dragOffset > 200f  -> { onReview(ReviewQuality.GOOD); dragOffset = 0f }
                                dragOffset < -200f -> { onReview(ReviewQuality.AGAIN); dragOffset = 0f }
                                else               -> dragOffset = 0f
                            }
                        },
                        onDragCancel = { dragOffset = 0f }
                    ) { _, delta -> dragOffset += delta }
                }
                .clickable { onFlip() }
        ) {
            // Background shadow cards
            if (cards.size > 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .offset(y = 8.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MistGray.copy(alpha = 0.6f))
                )
            }
            if (cards.size > 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .offset(y = 4.dp)
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MistGray.copy(alpha = 0.8f))
                )
            }

            // Main card
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = isFlipped,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(150))
                        },
                        label = "card_flip"
                    ) { flipped ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            val content = if (flipped) card.back else card.front
                            Text(
                                text = parseMarkdownBold(content),
                                style = if (flipped) MaterialTheme.typography.titleLarge
                                        else MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (flipped) {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "FR → EN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Swipe hint overlays
                    if (abs(animatedDrag) > 60f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (animatedDrag > 0) GlacierTeal.copy(alpha = 0.15f)
                                    else CoralRed.copy(alpha = 0.15f)
                                )
                        )
                        Text(
                            text = if (animatedDrag > 0) "GOOD" else "AGAIN",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (animatedDrag > 0) GlacierTeal else CoralRed,
                            modifier = Modifier.align(
                                if (animatedDrag > 0) Alignment.CenterStart else Alignment.CenterEnd
                            ).padding(horizontal = 24.dp).rotate(if (animatedDrag > 0) -15f else 15f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // Review buttons (shown after flip)
        AnimatedVisibility(visible = isFlipped) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ReviewButton("Again", CoralRed, Modifier.weight(1f)) { onReview(ReviewQuality.AGAIN) }
                ReviewButton("Hard",  StoneGray, Modifier.weight(1f)) { onReview(ReviewQuality.HARD) }
                ReviewButton("Good",  GlacierTeal, Modifier.weight(1f)) { onReview(ReviewQuality.GOOD) }
                ReviewButton("Easy",  PineGreen, Modifier.weight(1f)) { onReview(ReviewQuality.EASY) }
            }
        }

        if (!isFlipped) {
            Spacer(Modifier.height(16.dp))
            Text(
                "← swipe to fail · swipe to pass →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Converts markdown-style **bold** text into an AnnotatedString with Bold FontWeight.
 */
private fun parseMarkdownBold(text: String): AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Odd parts are the text inside ** **
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = FjordBlue)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}

@Composable
private fun ReviewButton(label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun EmptyState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Outlined.Style,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MistGray
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No flashcards yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap words in the Reader to save them here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SessionComplete() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = GlacierTeal
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Session Complete",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "All cards reviewed. Come back tomorrow.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
