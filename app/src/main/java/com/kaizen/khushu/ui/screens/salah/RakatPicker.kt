package com.kaizen.khushu.ui.screens.salah

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val ITEM_HEIGHT = 80.dp
private const val OVERSCROLL_THRESHOLD_DP = 120f

@Composable
fun RakatPicker(
    selectedRakat: Int,
    onRakatSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val thresholdPx = with(density) { OVERSCROLL_THRESHOLD_DP.dp.toPx() }
    val halfItemPx = with(density) { ITEM_HEIGHT.toPx() / 2f }

    // State objects — referenced by closure in NestedScrollConnection (stable identity)
    val rakatItemsState = remember { mutableStateOf((1..4).toList()) }
    val isExpandedState = remember { mutableStateOf(false) }
    val overscrollState = remember { mutableFloatStateOf(0f) }

    val rakatItems = rakatItemsState.value
    val isExpanded = isExpandedState.value
    val fillProgress = (overscrollState.floatValue / thresholdPx).coerceIn(0f, 1f)

    val initialIndex = rakatItems.indexOf(selectedRakat).coerceAtLeast(0)
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    // Center item: whichever item's center is closest to viewport center
    val centeredIndex by remember(halfItemPx) {
        derivedStateOf {
            val offset = lazyListState.firstVisibleItemScrollOffset
            val first = lazyListState.firstVisibleItemIndex
            if (offset > halfItemPx && first < rakatItemsState.value.lastIndex) first + 1 else first
        }
    }

    val atBottom by remember { derivedStateOf { !lazyListState.canScrollForward } }

    // Notify parent when selection changes
    LaunchedEffect(centeredIndex, rakatItems) {
        rakatItems.getOrNull(centeredIndex)?.let(onRakatSelected)
    }

    // Overscroll threshold trigger — captures State objects by stable reference
    val connection = remember(lazyListState) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) {
                    overscrollState.floatValue = 0f
                    return Offset.Zero
                }
                // available.y < 0 = user dragging up past bottom boundary
                if (!lazyListState.canScrollForward && available.y < 0 && !isExpandedState.value) {
                    overscrollState.floatValue =
                        (overscrollState.floatValue + (-available.y)).coerceAtMost(thresholdPx)

                    if (overscrollState.floatValue >= thresholdPx) {
                        val currentValue = rakatItemsState.value
                            .getOrElse(lazyListState.firstVisibleItemIndex) { 4 }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpandedState.value = true
                        rakatItemsState.value = (1..20).toList()
                        overscrollState.floatValue = 0f
                        coroutineScope.launch {
                            lazyListState.scrollToItem(currentValue - 1)
                        }
                    }
                } else {
                    overscrollState.floatValue = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                overscrollState.floatValue = 0f
                return Velocity.Zero
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .width(120.dp)
                .height(ITEM_HEIGHT * 3)
                .nestedScroll(connection),
            contentPadding = PaddingValues(vertical = ITEM_HEIGHT),
            flingBehavior = rememberSnapFlingBehavior(lazyListState),
            userScrollEnabled = true,
        ) {
            itemsIndexed(rakatItems) { index, rakat ->
                val isCenter = index == centeredIndex
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(ITEM_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = rakat.toString(),
                        style = if (isCenter) MaterialTheme.typography.displayMedium
                        else MaterialTheme.typography.displaySmall,
                        color = if (isCenter) Color.White
                        else Color.White.copy(alpha = 0.3f),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Overscroll fill line — visible at bottom when range can expand
        if (!isExpanded) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (atBottom) Color.White.copy(alpha = 0.15f)
                        else Color.Transparent
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillProgress)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}
