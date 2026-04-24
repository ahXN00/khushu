package com.kaizen.khushu.ui.screens.tasbeeh

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaizen.khushu.data.model.CustomBeadStyle
import com.kaizen.khushu.data.model.DhikrItem
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

enum class BeadStyle { CLASSIC_AMBER, DARK_ONYX }

@Serializable
sealed interface TasbihWidget {
    val id: String
    val offsetX: Float
    val offsetY: Float
    val scale: Float
    val zIndex: Float
    val width: Float
    val height: Float
    val color: Long
    val alpha: Float

    /** The vertical string of beads. Renders center-right. */
    @Serializable
    data class StringBeadWidget(
        override val id: String = "string",
        override val offsetX: Float = 0.88f,
        override val offsetY: Float = 0.5f,
        override val scale: Float = 1f,
        override val zIndex: Float = 0f,
        override val width: Float = 0f,
        override val height: Float = 0f,
        override val color: Long = 0xFFFFFFFF,
        override val alpha: Float = 1f,
        val beadStyle: BeadStyle = BeadStyle.CLASSIC_AMBER,
        val topStackLimit: Int = 3,
        val bottomStackLimit: Int = 7,
        val beadSizeScale: Float = 1.0f,
        val stringElasticity: Float = 1.8f,
        val wobbleStiffness: Float = 140f,
        val wobbleDampingRatio: Float = 0.25f,
        val beadMicroScale: Float = 1.2f,
    ) : TasbihWidget

    /** Shows the name of the current dhikr. Renders top-left. */
    @Serializable
    data class DhikrNameWidget(
        override val id: String = "name",
        override val offsetX: Float = 0.2f,
        override val offsetY: Float = 0.15f,
        override val scale: Float = 1f,
        override val zIndex: Float = 1f,
        override val width: Float = 0f,
        override val height: Float = 0f,
        override val color: Long = 0xFFFFFFFF,
        override val alpha: Float = 1f,
        val isBold: Boolean = true,
        val hasOutline: Boolean = false,
    ) : TasbihWidget

    /** Shows count + "out of N". Renders center-left. */
    @Serializable
    data class CounterWidget(
        override val id: String = "counter",
        override val offsetX: Float = 0.15f,
        override val offsetY: Float = 0.5f,
        override val scale: Float = 1f,
        override val zIndex: Float = 1f,
        override val width: Float = 0f,
        override val height: Float = 0f,
        override val color: Long = 0xFFFFFFFF,
        override val alpha: Float = 1f,
        val isBold: Boolean = false,
        val hasOutline: Boolean = false,
    ) : TasbihWidget

    /** A circular progress indicator. Renders around counter. */
    @Serializable
    data class ProgressCircleWidget(
        override val id: String = "progress_circle",
        override val offsetX: Float = 0.15f,
        override val offsetY: Float = 0.5f,
        override val scale: Float = 1f,
        override val zIndex: Float = 0.5f,
        override val width: Float = 0f,
        override val height: Float = 0f,
        override val color: Long = 0xFFFFFFFF,
        override val alpha: Float = 1f,
    ) : TasbihWidget

    /** Shows current dhikr meaning/translation. Renders below name. */
    @Serializable
    data class MeaningWidget(
        override val id: String = "meaning",
        override val offsetX: Float = 0.2f,
        override val offsetY: Float = 0.22f,
        override val scale: Float = 1f,
        override val zIndex: Float = 1f,
        override val width: Float = 0f,
        override val height: Float = 0f,
        override val color: Long = 0xFFFFFFFF,
        override val alpha: Float = 0.6f,
        val isBold: Boolean = false,
    ) : TasbihWidget

    /** A customizable text widget. */
    @Serializable
    data class CustomText(
        override val id: String,
        override val offsetX: Float = 0.5f,
        override val offsetY: Float = 0.5f,
        override val scale: Float = 1f,
        override val zIndex: Float = 1f,
        override val width: Float = 0f,
        override val height: Float = 0f,
        override val color: Long = 0xFFFFFFFF,
        override val alpha: Float = 1f,
        val text: String = "Custom Text",
        val fontSize: Float = 18f,
        val isBold: Boolean = false,
        val hasOutline: Boolean = false,
    ) : TasbihWidget
}

data class TasbihCanvasPreset(
    val id: String,
    val name: String,
    val widgets: List<TasbihWidget>,
)

val DefaultTasbihPreset = TasbihCanvasPreset(
    id = "default",
    name = "Default",
    widgets = listOf(
        TasbihWidget.StringBeadWidget(),
        TasbihWidget.DhikrNameWidget(),
        TasbihWidget.CounterWidget(),
    )
)

private const val BEAD_RADIUS_BASE = 18f

@Composable
fun TasbihWidgetRenderer(
    widget: TasbihWidget,
    currentCount: Int,
    currentItem: DhikrItem?,
    stringControlXOffset: Float = 0f,
    stringControlYFraction: Float = 0.5f,
    countedBeads: Int = 0,
    totalBeads: Int = 33,
    beadStyle: BeadStyle = BeadStyle.CLASSIC_AMBER,
    customBeadStyle: CustomBeadStyle? = null,
    activeBeadProgress: Float? = null,
    thumbPosition: Offset? = null,
    elasticity: Float = 1.8f,
    microScale: Float = 1.2f,
    isTouchActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val baseModifier = modifier.graphicsLayer { alpha = widget.alpha }
    
    // Shader/Brush cache for custom beads
    val noiseShader = remember { createNoiseShader() }
    val noiseBrush = remember(noiseShader) { ShaderBrush(noiseShader) }

    // beadShapeTypeToShape is @Composable — call it at composable scope directly (Compose caches internally)
    val customShape: Shape? = if (customBeadStyle != null) {
        beadShapeTypeToShape(customBeadStyle.shapeType)
    } else null

    when (widget) {
        is TasbihWidget.StringBeadWidget -> {
            val topLimit = widget.topStackLimit
            val bottomLimit = widget.bottomStackLimit
            val beadRadiusBase = BEAD_RADIUS_BASE * widget.beadSizeScale
            
            Canvas(
                modifier = baseModifier
                    .fillMaxHeight(0.9f)
                    .width(120.dp)
            ) {
                val density = this.density
                val beadRadius = beadRadiusBase * density

                val cx = size.width / 2f
                val controlX = cx + stringControlXOffset
                val controlY = size.height * stringControlYFraction

                val path = Path().apply {
                    moveTo(cx, 0f)
                    quadraticTo(controlX, controlY, cx, size.height)
                }

                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.35f * widget.alpha),
                    style = Stroke(
                        width = 2 * density,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    )
                )

                val androidPath = path.asAndroidPath()
                val pm = android.graphics.PathMeasure(androidPath, false)
                val pathLength = pm.length
                if (pathLength <= 0f) return@Canvas

                val pos = FloatArray(2)
                val tan = FloatArray(2)
                val fisheyeRadius = size.height * 0.15f

                var activeCenter: Offset? = null
                if (activeBeadProgress != null) {
                    val activeDist = (activeBeadProgress * pathLength).coerceIn(0f, pathLength)
                    if (pm.getPosTan(activeDist, pos, tan)) {
                        activeCenter = Offset(pos[0], pos[1])
                    }
                }

                val fisheyeCenter: Offset? = activeCenter ?: thumbPosition

                val topPoolAdjustment = if (activeBeadProgress != null && activeBeadProgress < 0.5f) 1 else 0
                val topCount = (minOf(countedBeads, topLimit) - topPoolAdjustment).coerceAtLeast(0)
                
                val topSpacing = beadRadius * 2.4f
                for (i in 0 until topCount) {
                    val dist = beadRadius + i * topSpacing
                    pm.getPosTan(dist, pos, tan)
                    val center = Offset(pos[0], pos[1])
                    val scale = fisheyeScale(center, fisheyeCenter, fisheyeRadius, widget.beadMicroScale)
                    
                    drawBeadWrapper(
                        center = center,
                        radius = beadRadius * scale,
                        legacyStyle = beadStyle,
                        customStyle = customBeadStyle,
                        customShape = customShape,
                        noiseBrush = noiseBrush
                    )
                }

                if (activeCenter != null) {
                    val scale = fisheyeScale(activeCenter, fisheyeCenter, fisheyeRadius, widget.beadMicroScale)
                    drawBeadWrapper(
                        center = activeCenter,
                        radius = beadRadius * scale,
                        legacyStyle = beadStyle,
                        customStyle = customBeadStyle,
                        customShape = customShape,
                        noiseBrush = noiseBrush
                    )
                }

                val remaining = (totalBeads - countedBeads).coerceAtLeast(0)
                val bottomPoolAdjustment = if (activeBeadProgress != null && activeBeadProgress >= 0.5f) 1 else 0
                val poolSize = (remaining - bottomPoolAdjustment).coerceAtLeast(0)
                val visibleBottom = minOf(poolSize, bottomLimit)
                
                val baseSpacing = beadRadius * 2.4f
                var currentDistance = 0f
                for (i in 0 until visibleBottom) {
                    val staticDist = pathLength - beadRadius - (i * baseSpacing)
                    pm.getPosTan(staticDist, pos, tan)
                    val staticCenter = Offset(pos[0], pos[1])
                    
                    val stretchFactor = if (isTouchActive && thumbPosition != null) {
                        val distToThumb = abs(staticCenter.y - thumbPosition.y)
                        val influenceRange = size.height * 0.25f
                        if (distToThumb < influenceRange) {
                            val normalizedDist = distToThumb / influenceRange
                            1f + (widget.stringElasticity - 1f) * exp(-normalizedDist * normalizedDist * 4f)
                        } else 1f
                    } else 1f
                    
                    val dynamicDist = pathLength - beadRadius - currentDistance
                    if (dynamicDist <= 0f) break
                    pm.getPosTan(dynamicDist, pos, tan)
                    val center = Offset(pos[0], pos[1])
                    
                    val scale = fisheyeScale(center, fisheyeCenter, fisheyeRadius, widget.beadMicroScale)
                    drawBeadWrapper(
                        center = center,
                        radius = beadRadius * scale,
                        legacyStyle = beadStyle,
                        customStyle = customBeadStyle,
                        customShape = customShape,
                        noiseBrush = noiseBrush
                    )

                    currentDistance += baseSpacing * stretchFactor
                }
            }
        }

        is TasbihWidget.DhikrNameWidget -> {
            Box(modifier = baseModifier) {
                Text(
                    text = currentItem?.name ?: "سُبْحَانَ اللَّهِ",
                    style = TextStyle(
                        color = Color(widget.color),
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                        fontWeight = if (widget.isBold) FontWeight.Bold else FontWeight.Normal,
                        drawStyle = if (widget.hasOutline) Stroke(width = 4f, join = StrokeJoin.Round) else Fill,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }

        is TasbihWidget.CounterWidget -> {
            Column(modifier = baseModifier) {
                Text(
                    text = currentCount.toString(),
                    style = TextStyle(
                        color = Color(widget.color),
                        fontSize = 96.sp,
                        fontWeight = if (widget.isBold) FontWeight.Bold else FontWeight.Normal,
                        drawStyle = if (widget.hasOutline) Stroke(width = 6f, join = StrokeJoin.Round) else Fill,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Text(
                    text = "out of ${currentItem?.targetCount ?: 0}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(widget.color).copy(alpha = 0.5f),
                )
            }
        }

        is TasbihWidget.ProgressCircleWidget -> {
            val progress = currentCount.toFloat() / (currentItem?.targetCount?.toFloat() ?: 1f)
            Canvas(modifier = baseModifier.size(240.dp)) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f * widget.alpha),
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(widget.color),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        is TasbihWidget.MeaningWidget -> {
            Text(
                text = "Glorified is Allah", 
                style = TextStyle(
                    color = Color(widget.color).copy(alpha = 0.6f),
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    fontWeight = if (widget.isBold) FontWeight.Bold else FontWeight.Normal,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = baseModifier,
            )
        }

        is TasbihWidget.CustomText -> {
            Text(
                text = widget.text,
                style = TextStyle(
                    color = Color(widget.color),
                    fontSize = widget.fontSize.sp,
                    fontWeight = if (widget.isBold) FontWeight.Bold else FontWeight.Normal,
                    drawStyle = if (widget.hasOutline) Stroke(width = 4f, join = StrokeJoin.Round) else Fill,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = baseModifier
            )
        }
    }
}

internal fun DrawScope.drawBeadWrapper(
    center: Offset,
    radius: Float,
    legacyStyle: BeadStyle,
    customStyle: CustomBeadStyle?,
    customShape: Shape?,
    noiseBrush: ShaderBrush?,
) {
    if (customStyle != null && customShape != null) {
        val sizePx = radius * 2f
        val path = createBeadPath(
            customShape,
            androidx.compose.ui.geometry.Size(sizePx, sizePx),
            layoutDirection,
            androidx.compose.ui.unit.Density(density)
        )
        translate(left = center.x - radius, top = center.y - radius) {
            drawPremiumBead(path, customStyle, noiseBrush)
        }
    } else {
        drawBead(center, radius, 1f, legacyStyle)
    }
}

fun DrawScope.drawPremiumBead(path: Path, style: CustomBeadStyle, noiseBrush: ShaderBrush?) {
    val bounds = path.getBounds()
    val pathSize = androidx.compose.ui.geometry.Size(bounds.width, bounds.height)

    // 1. Extrusion
    if (style.is3dEnabled) drawBeadExtrusion(path, style)

    // 2. Base color
    drawBeadBaseColor(path, style)

    // 3. Texture
    drawBeadTexture(path, style, noiseBrush)

    // 4. Specular highlight
    val specularBrush = buildSpecularBrush(style, pathSize)
    drawBeadSpecular(path, specularBrush)

    // 5. Chromatic aberration
    drawBeadChromaticAberration(path, style)

    // 6. Metallic sheen
    val metallicBrush = buildMetallicBrush(style, pathSize)
    drawBeadMetallicSheen(path, metallicBrush)
}

private fun DrawScope.drawBead(
    center: Offset,
    radius: Float,
    alpha: Float,
    style: BeadStyle = BeadStyle.CLASSIC_AMBER,
) {
    when (style) {
        BeadStyle.CLASSIC_AMBER -> {
            val lightCenter = center + Offset(-radius * 0.28f, -radius * 0.28f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFBF4D).copy(alpha = alpha),
                        Color(0xFFD4850A).copy(alpha = alpha),
                        Color(0xFF7A4000).copy(alpha = alpha),
                    ),
                    center = lightCenter,
                    radius = radius * 1.6f,
                ),
                radius = radius,
                center = center,
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.65f),
                radius = radius * 0.22f,
                center = lightCenter + Offset(radius * 0.04f, radius * 0.04f),
            )
        }
        BeadStyle.DARK_ONYX -> {
            val lightCenter = center + Offset(-radius * 0.3f, -radius * 0.3f)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF555555).copy(alpha = alpha),
                        Color(0xFF1A1A1A).copy(alpha = alpha),
                        Color(0xFF000000).copy(alpha = alpha),
                    ),
                    center = lightCenter,
                    radius = radius * 1.5f,
                ),
                topLeft = center + Offset(-radius, -radius * 1.08f),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2.16f),
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.85f),
                radius = radius * 0.18f,
                center = lightCenter + Offset(radius * 0.05f, radius * 0.05f),
            )
        }
    }
}

private fun fisheyeScale(
    beadCenter: Offset,
    thumb: Offset?,
    radiusPx: Float,
    maxScale: Float,
): Float {
    if (thumb == null) return 1f
    val dx = beadCenter.x - thumb.x
    val dy = beadCenter.y - thumb.y
    val dist = sqrt(dx * dx + dy * dy)
    if (dist >= radiusPx) return 1f
    val t = dist / radiusPx
    return 1f + (maxScale - 1f) * (1f - t * t)
}
