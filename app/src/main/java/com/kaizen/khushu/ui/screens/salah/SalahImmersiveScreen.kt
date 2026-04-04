package com.kaizen.khushu.ui.screens.salah

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaizen.khushu.data.CanvasPreset
import com.kaizen.khushu.data.CanvasWidget
import com.kaizen.khushu.data.WidgetRenderer
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val SWIPE_THRESHOLD = 100.dp

@Composable
fun SalahImmersiveScreen(
    targetRakats: Int,
    preset: CanvasPreset,
    onComplete: () -> Unit,
    onExit: () -> Unit,
) {
    val view = LocalView.current
    val window = (LocalContext.current as Activity).window
    DisposableEffect(Unit) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }

    var count by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    val isComplete = count >= targetRakats

    val density = LocalDensity.current
    val swipeThresholdPx = remember(density) { with(density) { SWIPE_THRESHOLD.toPx() } }
    var swipeAccum by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isComplete) {
        if (isComplete) {
            delay(5_000)
            onComplete()
        }
    }

    val safeBackgroundColor = Color(preset.backgroundColor.toLong() and 0xFFFFFFFF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(safeBackgroundColor)
            .pointerInput(isComplete, isPaused) {
                detectTapGestures(
                    onTap = {
                        if (isPaused) isPaused = false
                        else if (!isComplete) count++
                    },
                    onLongPress = { if (!isComplete) isPaused = true }
                )
            }
            .draggable(
                state = rememberDraggableState { delta -> if (delta > 0) swipeAccum += delta else swipeAccum = 0f },
                orientation = Orientation.Vertical,
                onDragStarted = { swipeAccum = 0f },
                onDragStopped = {
                    if (swipeAccum > swipeThresholdPx) onExit()
                    swipeAccum = 0f
                }
            )
    ) {
        preset.widgets.sortedBy { it.zIndex }.forEach { widget ->
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = widget.offsetX
                        translationY = widget.offsetY
                        scaleX = widget.scale
                        scaleY = widget.scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                WidgetRenderer(widget = widget, currentRakats = count, isComplete = isComplete)
            }
        }

        AnimatedVisibility(
            visible = isPaused,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Paused", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap anywhere to resume", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                    Spacer(Modifier.height(32.dp))
                    Text("Swipe Down forcefully to exit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
