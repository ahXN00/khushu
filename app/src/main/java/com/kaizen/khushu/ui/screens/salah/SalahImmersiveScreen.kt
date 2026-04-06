package com.kaizen.khushu.ui.screens.salah

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kaizen.khushu.data.CanvasPreset
import com.kaizen.khushu.data.CanvasWidget
import com.kaizen.khushu.data.WidgetRenderer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var resetProgress by remember { mutableFloatStateOf(0f) }
    var resetArmed by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isComplete = count >= targetRakats

    LaunchedEffect(isComplete) {
        if (isComplete) {
            delay(10_000)
            onComplete()
        }
    }

    val safeBackgroundColor = Color(preset.backgroundColor.toLong() and 0xFFFFFFFF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(safeBackgroundColor)
            .pointerInput(isComplete) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isSwiping = false
                    var holdJob: Job? = null

                    if (!isComplete) {
                        holdJob = scope.launch {
                            showOverlay = true
                            resetProgress = 0f
                            resetArmed = false
                            for (i in 1..20) {
                                delay(50)
                                resetProgress = i / 20f
                            }
                            resetArmed = true
                        }
                    }

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.position.y - down.position.y > 100f) {
                            isSwiping = true
                            break
                        }
                    } while (event.changes.any { it.pressed })

                    holdJob?.cancel()

                    if (isSwiping) {
                        if (!resetArmed && resetProgress < 0.3f && count > 0) count--
                    } else {
                        if (resetArmed) count = 0
                        else if (resetProgress < 0.3f && !isComplete) count++
                    }

                    resetProgress = 0f
                    resetArmed = false
                    showOverlay = false
                }
            }
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
            visible = showOverlay,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 500)
            ) + fadeIn(animationSpec = tween(durationMillis = 400)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 500)
            ) + fadeOut(animationSpec = tween(durationMillis = 400)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
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

        OutlinedButton (
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
        ) {
            Text(
                text = "Exit",
                color = Color.White.copy(alpha = 0.2f),
                style = MaterialTheme.typography.bodyLarge // Bigger, more legible font
            )
        }
    }
}
