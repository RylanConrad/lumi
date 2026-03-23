package com.fjordflow.ui.screens.roadmap

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.fjordflow.data.db.entity.RoadmapNodeEntity
import com.fjordflow.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapScreen(vm: RoadmapViewModel) {
    val state by vm.uiState.collectAsState()
    val selectedNode by vm.selectedNode.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Text(
                "Grammar Roadmap",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            val completed = state.nodes.count { it.isCompleted }
            Text(
                "$completed of ${state.nodes.size} milestones completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (state.nodes.isEmpty()) 0f else completed.toFloat() / state.nodes.size },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = FjordBlue,
                trackColor = MistGray
            )
        }

        HorizontalDivider(color = MistGray)

        // Path
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 24.dp, horizontal = 24.dp)
        ) {
            itemsIndexed(state.nodes) { index, node ->
                RoadmapNodeRow(
                    node = node,
                    index = index,
                    isLast = index == state.nodes.lastIndex,
                    isAlternate = index % 2 == 1,
                    onClick = {
                        vm.selectNode(node)
                    }
                )
            }
        }
    }

    // Node detail sheet
    selectedNode?.let { node ->
        ModalBottomSheet(
            onDismissRequest = { vm.dismissNode() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            NodeDetailSheet(
                node = node,
                onComplete = { vm.completeNode(node) },
                onDismiss = { vm.dismissNode() }
            )
        }
    }
}

@Composable
private fun RoadmapNodeRow(
    node: RoadmapNodeEntity,
    index: Int,
    isLast: Boolean,
    isAlternate: Boolean,
    onClick: () -> Unit
) {
    val nodeColor = when {
        node.isCompleted -> GlacierTeal
        node.isUnlocked  -> FjordBlue
        else             -> StoneGray.copy(alpha = 0.4f)
    }
    val rowAlignment = if (isAlternate) Arrangement.End else Arrangement.Start

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = rowAlignment,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAlternate) {
                NodeLabel(node = node, isAlternate = true, onClick = onClick)
                Spacer(Modifier.width(12.dp))
            }

            // Node circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(nodeColor)
                    .then(if (node.isUnlocked && !node.isCompleted) Modifier.border(2.dp, FjordBlue.copy(alpha = 0.3f), CircleShape) else Modifier)
                    .clickable(enabled = node.isUnlocked) { onClick() }
            ) {
                when {
                    node.isCompleted -> Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    node.isUnlocked  -> Text("${index + 1}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    else             -> Icon(Icons.Outlined.Lock, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }

            if (!isAlternate) {
                Spacer(Modifier.width(12.dp))
                NodeLabel(node = node, isAlternate = false, onClick = onClick)
            }
        }

        // Connector line
        if (!isLast) {
            Box(
                modifier = Modifier
                    .padding(start = if (!isAlternate) 28.dp else 0.dp, end = if (isAlternate) 28.dp else 0.dp)
                    .fillMaxWidth()
                    .height(32.dp),
                contentAlignment = if (isAlternate) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Canvas(modifier = Modifier.size(2.dp, 32.dp)) {
                    drawLine(
                        color = MistGray,
                        start = Offset(1.dp.toPx(), 0f),
                        end = Offset(1.dp.toPx(), size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeLabel(node: RoadmapNodeEntity, isAlternate: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = { if (node.isUnlocked) onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (node.isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shadowElevation = if (node.isUnlocked) 2.dp else 0.dp,
        modifier = Modifier.widthIn(max = 220.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = if (isAlternate) Alignment.End else Alignment.Start
        ) {
            Text(
                text = node.category,
                style = MaterialTheme.typography.labelSmall,
                color = if (node.isUnlocked) GlacierTeal else StoneGray,
                letterSpacing = 0.8.sp
            )
            Text(
                text = node.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (node.isUnlocked) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun NodeDetailSheet(
    node: RoadmapNodeEntity,
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp)
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = node.category,
            style = MaterialTheme.typography.labelSmall,
            color = GlacierTeal,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(node.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(node.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        if (!node.isCompleted && node.isUnlocked) {
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FjordBlue)
            ) {
                Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Mark as Complete", style = MaterialTheme.typography.labelLarge)
            }
        } else if (node.isCompleted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlacierTeal.copy(alpha = 0.1f))
                    .padding(16.dp)
            ) {
                Icon(Icons.Outlined.CheckCircle, null, tint = GlacierTeal, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Milestone completed", style = MaterialTheme.typography.labelLarge, color = GlacierTeal)
            }
        }
    }
}
