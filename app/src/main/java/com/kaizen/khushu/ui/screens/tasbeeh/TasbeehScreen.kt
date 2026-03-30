package com.kaizen.khushu.ui.screens.tasbeeh

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaizen.khushu.data.TasbeehCollection

// Sealed type to allow rendering real cards and empty placeholders in the same grid
private sealed interface GridItem {
    data class Real(val collection: TasbeehCollection) : GridItem
    data class Placeholder(val index: Int, val color: Color) : GridItem
}

@Composable
fun TasbeehScreen(
        viewModel: TasbeehViewModel,
        onCollectionTap: (TasbeehCollection) -> Unit,
        onCreateClick: () -> Unit,
        contentPadding: PaddingValues = PaddingValues(),
        modifier: Modifier = Modifier,
) {
    val collections by viewModel.collections.collectAsStateWithLifecycle(initialValue = emptyList())
    var pendingDelete by remember { mutableStateOf<TasbeehCollection?>(null) }

    // Pad to at least 4 (2 placeholder rows), always even (no orphaned grid hole)
    val gridItems: List<GridItem> =
            remember(collections) {
                val real = collections.map { GridItem.Real(it) }
                val targetSize = maxOf(4, if (real.size % 2 == 0) real.size else real.size + 1)
                val placeholders =
                        (real.size until targetSize).mapIndexed { listIndex, i ->
                            GridItem.Placeholder(
                                    index = real.size + listIndex,
                                    color = TasbeehPastelColors[i % TasbeehPastelColors.size],
                            )
                        }
                real + placeholders
            }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding =
                        PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = contentPadding.calculateTopPadding() + 50.dp,
                                bottom = contentPadding.calculateBottomPadding(),
                        ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
        ) {
            // "+ Create" pill — spans both columns via a header-like approach
            item(
                    key = "create_button",
                    span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }
            ) {
                Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                        contentAlignment = Alignment.CenterEnd,
                ) { CreateButton(onClick = onCreateClick) }
            }

            items(
                    gridItems,
                    key = { item ->
                        when (item) {
                            is GridItem.Real -> "real_${item.collection.id}"
                            is GridItem.Placeholder -> "placeholder_${item.index}"
                        }
                    }
            ) { item ->
                when (item) {
                    is GridItem.Real ->
                            CollectionCard(
                                    collection = item.collection,
                                    onTap = { onCollectionTap(item.collection) },
                                    onLongPress = { pendingDelete = item.collection },
                            )
                    is GridItem.Placeholder -> PlaceholderCard(color = item.color)
                }
            }
        }
    }

    // Delete confirmation dialog
    pendingDelete?.let { collection ->
        AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Delete collection?") },
                text = {
                    val name = collection.title?.takeIf { it.isNotBlank() } ?: "This collection"
                    Text("\"$name\" will be permanently deleted.")
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.delete(collection)
                                pendingDelete = null
                            }
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
                },
        )
    }
}

@Composable
private fun CreateButton(onClick: () -> Unit) {
    Button(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(56.dp).widthIn(min = 112.dp),
    ) {
        Text(
                text = "+ Create",
                style = MaterialTheme.typography.labelLarge,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionCard(
        collection: TasbeehCollection,
        onTap: () -> Unit,
        onLongPress: () -> Unit,
) {
    Box(
            modifier =
                    Modifier.aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(collection.colorInt))
                            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
                            .padding(12.dp),
    ) {
        Column {
            if (!collection.title.isNullOrBlank()) {
                Text(
                        text = collection.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
            }

            val displayItems = collection.items.take(3)
            displayItems.forEachIndexed { index, item ->
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                    )
                    Text(
                            text = item.targetCount.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                    )
                }
            }

            if (collection.items.size > 3) {
                Text(
                        text = "...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun PlaceholderCard(color: Color) {
    Box(
            modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(18.dp)).background(color),
    )
}
