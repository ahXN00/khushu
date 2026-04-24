package com.kaizen.khushu.ui.screens.tasbeeh

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaizen.khushu.data.model.TasbeehCollection
import com.kaizen.khushu.data.repository.UserSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private data class StringBeadMetrics(
    val beadRadius: Float,
    val topBottomY: Float,      // canvas-local Y of bottommost top bead (decrement source)
    val bottomTopY: Float,      // canvas-local Y of topmost bottom bead (increment source)
    val incrementLandY: Float,  // canvas-local Y where increment bead lands
    val decrementLandY: Float,  // canvas-local Y where decrement bead lands
)

private fun stringBeadMetrics(
    widget: TasbihWidget.StringBeadWidget,
    canvasHeightPx: Float,
    topCount: Int,
    bottomCount: Int,
    density: Float,
): StringBeadMetrics {
    val beadRadius = BEAD_RADIUS_BASE * widget.beadSizeScale * density
    val gap = beadRadius * 0.4f
    val spacing = beadRadius * 2f + gap
    val topBottomY = beadRadius + (topCount - 1).coerceAtLeast(0) * spacing
    val bottomTopY = canvasHeightPx - beadRadius - (bottomCount - 1).coerceAtLeast(0) * spacing
    val incrementLandY = beadRadius + topCount * spacing
    val decrementLandY = canvasHeightPx - beadRadius - (bottomCount - 1).coerceAtLeast(0) * spacing
    return StringBeadMetrics(beadRadius, topBottomY, bottomTopY, incrementLandY, decrementLandY)
}

@Composable
fun TasbeehImmersiveScreen(
    viewModel: TasbeehViewModel,
    canvasViewModel: TasbeehCanvasViewModel,
    collection: TasbeehCollection,
    settings: UserSettings,
    onExit: () -> Unit,
    beadStyle: BeadStyle = BeadStyle.CLASSIC_AMBER,
    customBeadStyle: com.kaizen.khushu.data.model.CustomBeadStyle? = null,
) {
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    val haptics = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val layout by canvasViewModel.layout.collectAsStateWithLifecycle()

    BackHandler(onBack = onExit)

    DisposableEffect(Unit) {
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    var currentItemIndex by remember { mutableIntStateOf(0) }
    var currentCount by remember { mutableIntStateOf(0) }
    val items = collection.items
    val currentItem = items.getOrNull(currentItemIndex)
    val currentTarget = currentItem?.targetCount ?: 33

    var screenWidth by remember { mutableFloatStateOf(0f) }
    var screenHeight by remember { mutableFloatStateOf(0f) }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    // New drag-based bead state
    val dragBeadY = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragIsIncrement by remember { mutableStateOf(true) }

    // String wobble control point
    val controlXAnim = remember { Animatable(0f) }
    val controlYAnim = remember { Animatable(0.5f) }
    val stringSnapSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 1500f)
    val beadSnapSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 2000f)
    val beadLandSpring = spring<Float>(dampingRatio = 0.45f, stiffness = 600f)

    var thumbPosition by remember { mutableStateOf<Offset?>(null) }
    var widgetsVisible by remember { mutableStateOf(true) }
    var resetProgress by remember { mutableFloatStateOf(0f) }
    var resetArmed by remember { mutableStateOf(false) }
    var showResetOverlay by remember { mutableStateOf(false) }

    fun registerIncrement() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        currentCount++
        if (currentCount >= currentTarget) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (currentItemIndex < items.lastIndex) {
                currentItemIndex++
                currentCount = 0
            }
        }
    }

    fun registerDecrement() {
        if (currentCount > 0 || currentItemIndex > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (currentCount > 0) currentCount--
            else { currentItemIndex--; currentCount = items[currentItemIndex].targetCount - 1 }
        }
    }

    fun getStringMetrics(): Pair<TasbihWidget.StringBeadWidget, StringBeadMetrics>? {
        val sw = layout.widgets.filterIsInstance<TasbihWidget.StringBeadWidget>().firstOrNull() ?: return null
        val canvasH = screenHeight * 0.9f * sw.scale
        val tc = minOf(currentCount, sw.topStackLimit).coerceAtLeast(0)
        val bc = minOf(currentTarget - currentCount, sw.bottomStackLimit).coerceAtLeast(0)
        return sw to stringBeadMetrics(sw, canvasH, tc, bc, density)
    }

    fun fireVolumeIncrement() {
        if (isDragging) return
        if (!settings.tasbeehVolumeAnimation) { registerIncrement(); return }
        val pair = getStringMetrics()
        if (pair == null) { registerIncrement(); return }
        val (sw, metrics) = pair
        val bc = minOf(currentTarget - currentCount, sw.bottomStackLimit).coerceAtLeast(0)
        if (bc == 0) { registerIncrement(); return }
        scope.launch {
            isDragging = true; dragIsIncrement = true
            dragBeadY.snapTo(metrics.bottomTopY)
            dragBeadY.animateTo(metrics.incrementLandY, beadLandSpring)
            registerIncrement()
            kotlinx.coroutines.delay(80)
            isDragging = false
        }
    }

    fun fireVolumeDecrement() {
        if (isDragging) return
        if (!settings.tasbeehVolumeAnimation) { registerDecrement(); return }
        val pair = getStringMetrics()
        if (pair == null) { registerDecrement(); return }
        val (sw, metrics) = pair
        val tc = minOf(currentCount, sw.topStackLimit).coerceAtLeast(0)
        if (tc == 0) { registerDecrement(); return }
        scope.launch {
            isDragging = true; dragIsIncrement = false
            dragBeadY.snapTo(metrics.topBottomY)
            dragBeadY.animateTo(metrics.decrementLandY, beadLandSpring)
            registerDecrement()
            kotlinx.coroutines.delay(80)
            isDragging = false
        }
    }

    LaunchedEffect(viewModel.countIncrementSignal) {
        viewModel.countIncrementSignal.collect { fireVolumeIncrement() }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(layout.backgroundColorInt.toLong()))
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (settings.tasbeehVolumeEnabled && event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.VolumeUp -> { fireVolumeIncrement(); true }
                        Key.VolumeDown -> { fireVolumeDecrement(); true }
                        else -> false
                    }
                } else false
            }
            .onSizeChanged { size ->
                screenWidth = size.width.toFloat()
                screenHeight = size.height.toFloat()
            }
    ) {
        if (screenWidth > 0f && screenHeight > 0f) {
            AnimatedVisibility(visible = widgetsVisible, enter = fadeIn(), exit = fadeOut()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    layout.widgets.sortedBy { it.zIndex }.forEach { widget ->
                        Box(
                            modifier = Modifier.graphicsLayer {
                                translationX = (widget.offsetX * screenWidth) - (size.width / 2f)
                                translationY = (widget.offsetY * screenHeight) - (size.height / 2f)
                                scaleX = widget.scale
                                scaleY = widget.scale
                                transformOrigin = TransformOrigin.Center
                            }
                        ) {
                            TasbihWidgetRenderer(
                                widget = widget,
                                currentCount = currentCount,
                                currentItem = currentItem,
                                stringControlXOffset = controlXAnim.value,
                                stringControlYFraction = controlYAnim.value,
                                countedBeads = currentCount,
                                totalBeads = currentTarget,
                                beadStyle = beadStyle,
                                customBeadStyle = customBeadStyle,
                                dragBeadY = if (widget is TasbihWidget.StringBeadWidget && isDragging) dragBeadY.value else null,
                                dragIsIncrement = dragIsIncrement,
                                thumbPosition = if (widget is TasbihWidget.StringBeadWidget) {
                                    thumbPosition?.let { t ->
                                        Offset(t.x - (widget.offsetX * screenWidth), t.y - (widget.offsetY * screenHeight))
                                    }
                                } else null,
                                isTouchActive = thumbPosition != null
                            )
                        }
                    }
                }
            }
        }

        if (screenWidth > 0f && screenHeight > 0f) {
            val stringWidget = layout.widgets.filterIsInstance<TasbihWidget.StringBeadWidget>().firstOrNull()
            val hasString = stringWidget != null

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(screenWidth, screenHeight, layout.widgets, widgetsVisible, settings.tasbeehStealthModeAllowed) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()

                            val startScreenX = down.position.x
                            val startScreenY = down.position.y

                            val stringScreenX = (stringWidget?.offsetX ?: 0.88f) * screenWidth
                            val isNearString = hasString && widgetsVisible &&
                                kotlin.math.abs(startScreenX - stringScreenX) < 70f * density && !showResetOverlay

                            // Canvas Y helpers
                            val sw = stringWidget
                            val canvasH = screenHeight * 0.9f * (sw?.scale ?: 1f)
                            val canvasTopScreenY = (sw?.offsetY ?: 0.5f) * screenHeight - canvasH / 2f
                            fun screenToCanvasY(sy: Float) = sy - canvasTopScreenY

                            val topCountAtStart = minOf(currentCount, sw?.topStackLimit ?: 3).coerceAtLeast(0)
                            val bottomCountAtStart = minOf(currentTarget - currentCount, sw?.bottomStackLimit ?: 7).coerceAtLeast(0)
                            val metricsAtStart = sw?.let {
                                stringBeadMetrics(it, canvasH, topCountAtStart, bottomCountAtStart, density)
                            }

                            val startCanvasY = screenToCanvasY(startScreenY)
                            val hitRadius = (metricsAtStart?.beadRadius ?: 0f) * 2.5f

                            val isIncrementHit = isNearString && metricsAtStart != null && bottomCountAtStart > 0 &&
                                kotlin.math.abs(startCanvasY - metricsAtStart.bottomTopY) < hitRadius
                            val isDecrementHit = isNearString && metricsAtStart != null && topCountAtStart > 0 &&
                                kotlin.math.abs(startCanvasY - metricsAtStart.topBottomY) < hitRadius

                            val startedDrag = !isDragging && (isIncrementHit || isDecrementHit)
                            var dragStarted = false
                            var hasSwiped = false
                            var localResetArmed = false

                            if (startedDrag) {
                                dragStarted = true
                                isDragging = true
                                dragIsIncrement = isIncrementHit
                                val startCanvasYLocal = if (isIncrementHit) metricsAtStart!!.bottomTopY else metricsAtStart!!.topBottomY
                                scope.launch { dragBeadY.snapTo(startCanvasYLocal) }
                            }

                            val holdJob = if (!isNearString) {
                                scope.launch {
                                    kotlinx.coroutines.delay(500)
                                    showResetOverlay = true
                                    resetProgress = 0f; resetArmed = false
                                    for (i in 1..20) { kotlinx.coroutines.delay(40); resetProgress = i / 20f }
                                    resetArmed = true; localResetArmed = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            } else null

                            var prevY = startScreenY
                            var velocity = 0f

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break

                                val currentX = change.position.x
                                val currentY = change.position.y
                                velocity = currentY - prevY
                                prevY = currentY
                                thumbPosition = Offset(currentX, currentY)

                                val targetX = (currentX - stringScreenX).coerceIn(-100f, 100f)
                                scope.launch { controlXAnim.animateTo(targetX, spring(stiffness = androidx.compose.animation.core.Spring.StiffnessHigh)) }
                                scope.launch { controlYAnim.animateTo((currentY / screenHeight).coerceIn(0.1f, 0.9f), spring(stiffness = androidx.compose.animation.core.Spring.StiffnessHigh)) }

                                if (dragStarted) {
                                    val canvasY = screenToCanvasY(currentY).coerceIn(0f, canvasH)
                                    scope.launch { dragBeadY.snapTo(canvasY) }
                                }

                                if (!hasSwiped && !isNearString && kotlin.math.abs(currentY - startScreenY) > 120f) {
                                    hasSwiped = true
                                    holdJob?.cancel()
                                }

                                if (change.positionChanged()) change.consume()
                            }

                            holdJob?.cancel()

                            if (dragStarted && metricsAtStart != null) {
                                val curY = dragBeadY.value
                                val midY = canvasH / 2f
                                if (dragIsIncrement) {
                                    val committed = curY < midY || velocity < -3f
                                    scope.launch {
                                        if (committed) {
                                            dragBeadY.animateTo(metricsAtStart.incrementLandY, beadLandSpring)
                                            registerIncrement()
                                        } else {
                                            dragBeadY.animateTo(metricsAtStart.bottomTopY, beadSnapSpring)
                                        }
                                        kotlinx.coroutines.delay(80)
                                        isDragging = false
                                    }
                                } else {
                                    val committed = curY > midY || velocity > 3f
                                    scope.launch {
                                        if (committed) {
                                            dragBeadY.animateTo(metricsAtStart.decrementLandY, beadLandSpring)
                                            registerDecrement()
                                        } else {
                                            dragBeadY.animateTo(metricsAtStart.topBottomY, beadSnapSpring)
                                        }
                                        kotlinx.coroutines.delay(80)
                                        isDragging = false
                                    }
                                }
                            } else if (!dragStarted) {
                                val didMove = kotlin.math.abs(prevY - startScreenY) > 20f
                                if (!didMove) {
                                    when {
                                        localResetArmed -> {
                                            currentCount = 0
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        showResetOverlay -> {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        !hasString || !widgetsVisible -> {
                                            // In stealth or no-string mode: double-tap toggles stealth, single tap counts.
                                            if (settings.tasbeehStealthModeAllowed) {
                                                val secondTap = withTimeoutOrNull(300L) { awaitFirstDown(requireUnconsumed = false) }
                                                if (secondTap != null) {
                                                    secondTap.consume()
                                                    widgetsVisible = !widgetsVisible
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                } else {
                                                    fireVolumeIncrement()
                                                }
                                            } else {
                                                fireVolumeIncrement()
                                            }
                                        }
                                        settings.tasbeehStealthModeAllowed && !isNearString -> {
                                            // Normal visible mode, not near string: double-tap to enter stealth.
                                            val secondTap = withTimeoutOrNull(300L) { awaitFirstDown(requireUnconsumed = false) }
                                            if (secondTap != null) {
                                                secondTap.consume()
                                                widgetsVisible = !widgetsVisible
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        }
                                    }
                                }
                            }

                            thumbPosition = null
                            showResetOverlay = false
                            scope.launch { controlXAnim.animateTo(0f, stringSnapSpring) }
                            scope.launch { controlYAnim.animateTo(0.5f, stringSnapSpring) }
                        }
                    }
            )
        }

        AnimatedVisibility(
            visible = showResetOverlay,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        progress = { resetProgress },
                        modifier = Modifier.size(24.dp),
                        color = if (resetArmed) MaterialTheme.colorScheme.error else Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = if (resetArmed) "Release to Reset  ·  Swipe Down to Abort" else "Holding to Reset...",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (resetArmed) MaterialTheme.colorScheme.error else Color.White
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !showResetOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp)
        ) {
            OutlinedButton(
                onClick = onExit,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.2f))
            ) {
                Text(text = "Exit", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
