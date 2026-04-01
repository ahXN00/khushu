package com.kaizen.khushu.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalView
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Controller to trigger the premium circular reveal transition.
 */
@Stable
class ThemeTransitionController(
    private val onCapture: (Offset, () -> Unit) -> Unit
) {
    fun captureAndChange(revealFrom: Offset, block: () -> Unit) {
        onCapture(revealFrom, block)
    }
}

val LocalThemeTransitionController = staticCompositionLocalOf<ThemeTransitionController> {
    error("No ThemeTransitionController provided")
}

/**
 * High-fidelity theme transition provider using View snapshotting.
 * Works across all Compose versions while delivering the 'Proper' circular reveal.
 */
@Composable
fun ThemeTransitionProvider(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    
    // State lock for "indestructible" UI
    var isTransitioning by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<Bitmap?>(null) }
    var revealFrom by remember { mutableStateOf(Offset.Zero) }
    val progress = remember { Animatable(0f) }

    val controller = remember {
        ThemeTransitionController { offset, block ->
            if (isTransitioning) return@ThemeTransitionController
            
            // 1. Capture current visual state (Old Theme)
            val bitmap = view.drawToBitmap()
            
            isTransitioning = true
            snapshot = bitmap
            revealFrom = offset

            scope.launch {
                // 1. Execute theme change (DataStore update)
                block()

                // 2. Force the animation to wait for the UI thread to finish painting the new colors
                delay(50)

                // 3. Animate expansion using flagship physics
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1000,
//                        easing = FastOutSlowInEasing
                        easing = CubicBezierEasing(0.8f, 0.8f, 0.8f, 0.8f)
                    )
                )

                // 4. Cleanup
                isTransitioning = false
                snapshot = null
            }
        }
    }

    CompositionLocalProvider(LocalThemeTransitionController provides controller) {
        Box(
            modifier = Modifier.drawWithContent {
                // ALWAYS draw live content first (the New Theme)
                drawContent()

                // Draw snapshot (Old Theme) on TOP with expanding hole
                val currentSnapshot = snapshot
                if (isTransitioning && currentSnapshot != null) {
                    val maxRadius = hypot(size.width, size.height)
                    val currentRadius = maxRadius * progress.value

                    clipPath(
                        path = Path().apply {
                            val numPoints = 60
                            for (i in 0..numPoints) {
                                val angle = (i * 2.0 * Math.PI / numPoints).toFloat()

                                // Injects organic water ripples that undulate as the circle expands
                                val rippleAmplitude = 120f * progress.value
                                val ripplePhase = progress.value * 15f
                                val ripple = kotlin.math.sin(angle * 4f + ripplePhase) * rippleAmplitude

                                val r = currentRadius + ripple
                                val x = revealFrom.x + r * kotlin.math.cos(angle)
                                val y = revealFrom.y + r * kotlin.math.sin(angle)

                                if (i == 0) moveTo(x, y) else lineTo(x, y)
                            }
                            close()
                        },
                        clipOp = ClipOp.Difference
                    ) {
                        drawImage(currentSnapshot.asImageBitmap())
                    }
                }
            }
        ) {
            content()
        }
    }
}
