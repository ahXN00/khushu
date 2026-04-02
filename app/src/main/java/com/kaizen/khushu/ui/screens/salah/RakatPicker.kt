package com.kaizen.khushu.ui.screens.salah

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaizen.khushu.ui.theme.Antonio
import kotlin.math.abs
import kotlinx.coroutines.launch

private val ITEM_HEIGHT = 145.dp
private const val OVERSCROLL_THRESHOLD_DP = 270f

private const val MAX_SCALE = 0.8f
private const val MIN_SCALE = 0.40f
private val BASE_TEXT_STYLE =
        TextStyle(
                fontFamily = Antonio,
                fontSize = 220.sp,
                lineHeight = 260.sp,
                letterSpacing = 0.sp
        )

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

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

    val rakatItemsState = remember { mutableStateOf((1..4).toList()) }
    val isExpandedState = remember { mutableStateOf(false) }
    val overscrollState = remember { mutableFloatStateOf(0f) }

    val rakatItems = rakatItemsState.value
    val isExpanded = isExpandedState.value
    val fillProgress = (overscrollState.floatValue / thresholdPx).coerceIn(0f, 1f)

    val initialIndex = rakatItems.indexOf(selectedRakat).coerceAtLeast(0)
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    val centeredIndex by remember(halfItemPx) {
        derivedStateOf {
            val offset = lazyListState.firstVisibleItemScrollOffset
            val first = lazyListState.firstVisibleItemIndex
            if (offset > halfItemPx && first < rakatItemsState.value.lastIndex) first + 1
            else first
        }
    }

    val atBottom by remember { derivedStateOf { centeredIndex == rakatItemsState.value.lastIndex } }

    LaunchedEffect(centeredIndex, rakatItems) {
        rakatItems.getOrNull(centeredIndex)?.let(onRakatSelected)
    }

    // onPreScroll intercepts drag BEFORE the list sees it.
    // This is essential — onPostScroll doesn't work here because contentPadding
    // causes the list to consume upward drag even when the last item is centered,
    // leaving nothing in `available` for us to accumulate.
    val connection = remember(lazyListState) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (isExpandedState.value) return Offset.Zero
                // Only intercept upward drag (available.y < 0)
                if (available.y >= 0f) return Offset.Zero

                val lastIndex = rakatItemsState.value.lastIndex
                val first = lazyListState.firstVisibleItemIndex
                val offset = lazyListState.firstVisibleItemScrollOffset
                val centered = if (offset > halfItemPx && first < lastIndex) first + 1 else first

                if (centered == lastIndex) {
                    // Consume the drag entirely — list stays still, bar fills
                    overscrollState.floatValue =
                        (overscrollState.floatValue + (-available.y)).coerceAtMost(thresholdPx)

                    if (overscrollState.floatValue >= thresholdPx) {
                        val currentValue = rakatItemsState.value.getOrElse(first) { 4 }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpandedState.value = true
                        rakatItemsState.value = (1..20).toList()
                        overscrollState.floatValue = 0f
                        coroutineScope.launch {
                            lazyListState.scrollToItem(currentValue - 1)
                        }
                    }
                    return Offset(0f, available.y) // fully consumed
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Reset bar if user drags back down
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
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
                modifier = Modifier.width(180.dp).height(ITEM_HEIGHT * 3).nestedScroll(connection),
                contentPadding = PaddingValues(vertical = ITEM_HEIGHT),
                flingBehavior = rememberSnapFlingBehavior(lazyListState),
                userScrollEnabled = true,
        ) {
            itemsIndexed(rakatItems) { index, rakat ->
                Box(
                        modifier = Modifier.height(ITEM_HEIGHT).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                ) {
                    Text(
                            text = rakat.toString(),
                            style = BASE_TEXT_STYLE,
                            color = MaterialTheme.colorScheme.onSurface,
                            softWrap = false,
                            modifier = Modifier.wrapContentHeight(unbounded = true).graphicsLayer {
                                val info = lazyListState.layoutInfo
                                val viewportCenter =
                                        (info.viewportStartOffset + info.viewportEndOffset) / 2f
                                val itemInfo =
                                        info.visibleItemsInfo.firstOrNull { it.index == index }
                                if (itemInfo != null) {
                                    val itemCenter = itemInfo.offset + (itemInfo.size / 2f)
                                    val distance = abs(itemCenter - viewportCenter)
                                    val t =
                                            (distance / itemInfo.size.toFloat())
                                                    .coerceIn(0f, 1f)
                                    scaleX = lerp(MAX_SCALE, MIN_SCALE, t)
                                    scaleY = lerp(MAX_SCALE, MIN_SCALE, t)
                                    alpha = lerp(1f, 0.3f, t)
                                    rotationX = lerp(0f, 25f, t)
                                    transformOrigin = TransformOrigin.Center
                                }
                            },
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (!isExpanded) {
            Box(
                    modifier =
                            Modifier.width(37.dp)
                                    .height(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                            if (atBottom)
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
                                            else Color.Transparent
                                    ),
            ) {
                Box(
                        modifier =
                                Modifier.fillMaxHeight()
                                        .fillMaxWidth(fillProgress)
                                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            Spacer(Modifier.height(8.dp))

            val hintAlpha by animateFloatAsState(
                    targetValue = if (atBottom) 0.4f else 0f,
                    animationSpec = tween(durationMillis = 300),
                    label = "hintAlpha",
            )
            Text(
                    text = "Scroll up for more",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = hintAlpha),
            )
        }
    }
}
