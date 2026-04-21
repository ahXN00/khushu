package com.kaizen.khushu.ui.screens.tasbeeh

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kaizen.khushu.ui.theme.KhushuColors
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kaizen.khushu.data.model.DhikrItem
import com.kaizen.khushu.data.model.TasbeehCollection
import com.kaizen.khushu.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCollectionSheet(
    viewModel: TasbeehViewModel,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // skipPartiallyExpanded = false allows the "half-open" state.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val settings by settingsViewModel.settings.collectAsState()
    val isImeVisible = WindowInsets.isImeVisible

    val dhikrRows = viewModel.createDhikrRows
    val selectedColor = KhushuColors.Palette[viewModel.createColorIndex]
    val canSave = dhikrRows.any { it.name.isNotBlank() && it.count.toIntOrNull() != null }

    val transparentColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetCreateState()
            onDismiss()
        },
        sheetState = sheetState,
        dragHandle = null,
    ) {
        // .fillMaxHeight(0.95f) allows the sheet to expand to full view when swiped up.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 30.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Tasbih",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                scope.launch {
                                    sheetState.hide()
                                    viewModel.resetCreateState()
                                    onDismiss()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                OutlinedTextField(
                    value = viewModel.createTitle,
                    onValueChange = { viewModel.updateCreateTitle(it) },
                    label = { Text("Tasbih name") },
                    placeholder = { Text("Optional") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }

            if (!settings.tasbeehDynamicColors) {
                item {
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(KhushuColors.Palette) { index, color ->
                            val isSelected = index == viewModel.createColorIndex
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isSelected) Modifier.border(
                                            width = 2.5.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape,
                                        ) else Modifier
                                    )
                                    .clickable { viewModel.updateCreateColorIndex(index) },
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }

            item {
                Text(
                    text = "Dhikr Items",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                key(dhikrRows.size) {
                    ReorderableColumn(
                        list = dhikrRows,
                        onSettle = { from, to ->
                            viewModel.moveDhikrRow(from, to)
                        },
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) { index, row, isDragging ->
                        key(row.id) {
                            ReorderableItem {
                                val focusRequester = remember { FocusRequester() }
                                
                                LaunchedEffect(viewModel.pendingFocusId) {
                                    if (viewModel.pendingFocusId == row.id) {
                                        delay(50)
                                        try {
                                            focusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                        viewModel.pendingFocusId = null
                                    }
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (isDragging)
                                                    Modifier.shadow(
                                                        elevation = 8.dp,
                                                        shape = RoundedCornerShape(12.dp),
                                                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                                    ).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                else Modifier
                                            )
                                            .padding(horizontal = 16.dp),
                                    ) {
                                        TextField(
                                            value = row.name,
                                            onValueChange = { viewModel.updateDhikrName(row.id, it) },
                                            placeholder = { Text("Dhikr name") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Sentences,
                                                imeAction = ImeAction.Next,
                                            ),
                                            colors = transparentColors,
                                            modifier = Modifier
                                                .weight(1f)
                                                .focusRequester(focusRequester),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        TextField(
                                            value = row.count,
                                            onValueChange = { viewModel.updateDhikrCount(row.id, it) },
                                            placeholder = { Text("33") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done,
                                            ),
                                            colors = transparentColors,
                                            modifier = Modifier.width(72.dp),
                                        )
                                    }

                                    if (index < dhikrRows.lastIndex) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { viewModel.addDhikrRow() },
                    modifier = Modifier.padding(start = 4.dp),
                ) { Text("+ Add Dhikr") }
                Spacer(Modifier.height(20.dp))
            }

            item {
                Button(
                    onClick = {
                        val items = dhikrRows
                            .filter { it.name.isNotBlank() && it.count.toIntOrNull() != null }
                            .map { DhikrItem(name = it.name.trim(), targetCount = it.count.toInt()) }
                        val collection = TasbeehCollection(
                            title = viewModel.createTitle.trim().ifBlank { null },
                            colorInt = selectedColor.toArgb(),
                            items = items,
                        )
                        scope.launch {
                            viewModel.insert(collection)
                            viewModel.resetCreateState()
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    enabled = canSave,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) { Text("Save") }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
