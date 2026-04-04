package com.kaizen.khushu.ui.screens.salah

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.AlignHorizontalLeft
import androidx.compose.material.icons.automirrored.filled.AlignHorizontalRight
import androidx.compose.material.icons.filled.AlignHorizontalCenter
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.input.pointer.pointerInput
import android.app.Activity
import android.util.Log

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.ColorFilter
import androidx.core.view.WindowCompat
import com.kaizen.khushu.ui.theme.BeVietnamPro
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaizen.khushu.data.CanvasWidget
import com.kaizen.khushu.data.resolveFontFamily
import com.kaizen.khushu.ui.theme.Antonio
import com.kaizen.khushu.ui.theme.KhushuColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private data class WidgetData(
    val opacity: Float,
    val isOutline: Boolean,
    val color: Int,
    val fontSize: Float,
    val fontWeight: Int,
    val text: String?,
    val italic: Boolean,
    val textAlign: String,
    val verticalAlign: String
)

data class CanvasPreset(
    val id: String,
    val name: String,
    val backgroundColor: Int,
    val widgets: List<CanvasWidget>,
    val isDeletable: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalahCanvasScreen(
    targetRakats: Int,
    viewModel: SalahCanvasViewModel,
    onSave: () -> Unit,
    onExit: () -> Unit,
) {
    val view = LocalView.current
    val window = (LocalContext.current as Activity).window
    DisposableEffect(Unit) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val workingWidgets by viewModel.workingWidgets.collectAsStateWithLifecycle()
    val workingBackground by viewModel.workingBackgroundColor.collectAsStateWithLifecycle()
    val selectedWidgetId by viewModel.selectedWidgetId.collectAsStateWithLifecycle()
    val isUiVisible by viewModel.isUiVisible.collectAsStateWithLifecycle()
    
    val layout by viewModel.layout.collectAsStateWithLifecycle()

    val canvasBackgroundColor = Color(workingBackground.toLong())

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .background(canvasBackgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (selectedWidgetId != null) {
                            viewModel.selectWidget(null)
                            viewModel.showUi()
                        } else {
                            viewModel.toggleUiVisibility()
                        }
                    }
                )
            },
        contentAlignment = Alignment.TopStart
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        // Update canvas size for alignment calculations
        LaunchedEffect(screenWidth, screenHeight) {
            viewModel.setCanvasSize(screenWidth, screenHeight)
        }

        workingWidgets.sortedBy { it.zIndex }.forEach { widget ->
            CanvasWidgetItem(
                widget = widget,
                isSelected = widget.id == selectedWidgetId,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                currentRakats = targetRakats,
                onUpdate = { viewModel.updateWidget(it) },
                onTap = {
                    viewModel.selectWidget(widget.id)
                    viewModel.showUi()
                },
                onSizeMeasured = { id, w, h -> viewModel.updateWidgetSize(id, w, h) }
            )
        }

        // Action Buttons (Save/Exit/Dev)
        AnimatedVisibility(
            visible = isUiVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = {
                    val dump = buildString {
                        appendLine("val presets = listOf(")
                        appendLine("    \"Custom\" to listOf(")
                        workingWidgets.forEach { widget ->
                            append("        ")
                            when (widget) {
                                is CanvasWidget.RakatCount -> {
                                    append("CanvasWidget.RakatCount(")
                                    append("offsetX = ${widget.offsetX}f, offsetY = ${widget.offsetY}f, ")
                                    append("scale = ${widget.scale}f, color = ${widget.color}, ")
                                    append("opacity = ${widget.opacity}f, fontSizeSp = ${widget.fontSizeSp}f, ")
                                    append("fontWeight = ${widget.fontWeight}, isOutline = ${widget.isOutline}, ")
                                    append("fontName = \"${widget.fontName}\"")
                                    append(")")
                                }
                                is CanvasWidget.ClockWidget -> {
                                    append("CanvasWidget.ClockWidget(")
                                    append("offsetX = ${widget.offsetX}f, offsetY = ${widget.offsetY}f, ")
                                    append("scale = ${widget.scale}f, color = ${widget.color}, ")
                                    append("opacity = ${widget.opacity}f, fontSizeSp = ${widget.fontSizeSp}f, ")
                                    append("showSeconds = ${widget.showSeconds}, use24Hour = ${widget.use24Hour}, ")
                                    append("isOutline = ${widget.isOutline}, ")
                                    append("fontName = \"${widget.fontName}\"")
                                    append(")")
                                }
                                is CanvasWidget.CustomText -> {
                                    append("CanvasWidget.CustomText(")
                                    append("offsetX = ${widget.offsetX}f, offsetY = ${widget.offsetY}f, ")
                                    append("scale = ${widget.scale}f, text = \"${widget.text}\", color = ${widget.color}, ")
                                    append("opacity = ${widget.opacity}f, fontSizeSp = ${widget.fontSizeSp}f, ")
                                    append("fontWeight = ${widget.fontWeight}, italic = ${widget.italic}, ")
                                    append("textAlign = \"${widget.textAlign}\", verticalAlign = \"${widget.verticalAlign}\", ")
                                    append("isOutline = ${widget.isOutline}, ")
                                    append("fontName = \"${widget.fontName}\"")
                                    append(")")
                                }
                            }
                            appendLine(",")
                        }
                        appendLine("    ),")
                        appendLine(")")
                    }
                    Log.d("PRESET_DUMP", dump)
                }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Dev Dump",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
                TextButton(onClick = onExit, colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))) {
                    Text("Exit")
                }
                Button(
                    onClick = { viewModel.saveLayout(workingBackground); onSave() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            }
        }

        // FABs - Add Widget and Presets
        var showAddMenu by remember { mutableStateOf(false) }
        var showPresetsMenu by remember { mutableStateOf(false) }
        
        AnimatedVisibility(
            visible = isUiVisible && selectedWidgetId == null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 30.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Presets FAB (left)
                FloatingActionButton(
                    onClick = { showPresetsMenu = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Presets")
                }
                
                // Add Widget FAB (right)
                ExtendedFloatingActionButton(
                    onClick = { showAddMenu = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Add Widget") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        if (showAddMenu) {
            AddWidgetSheet(
                currentBackground = workingBackground,
                onAdd = { widget ->
                    viewModel.addWidget(widget)
                    showAddMenu = false
                },
                onBackgroundChange = { 
                    viewModel.updateBackgroundColor(it)
                },
                onDismiss = { showAddMenu = false }
            )
        }

        if (showPresetsMenu) {
            PresetsSheet(
                viewModel = viewModel,
                actualScreenWidth = screenWidth,
                actualScreenHeight = screenHeight,
                onDismiss = { showPresetsMenu = false },
                onLoadPreset = { widgets, bgColor ->
                    viewModel.clearWidgets()
                    widgets.forEach { viewModel.addWidget(it) }
                    viewModel.updateBackgroundColor(bgColor)
                    showPresetsMenu = false
                }
            )
        }

        // Selection / Config
        selectedWidgetId?.let { id ->
            workingWidgets.find { it.id == id }?.let { selectedWidget ->
                WidgetConfigSheet(
                    widget = selectedWidget,
                    onUpdate = { viewModel.updateWidget(it) },
                    onDelete = { viewModel.removeWidget(id) },
                    onDismiss = { viewModel.selectWidget(null) },
                    onAlign = { h, v -> viewModel.alignSelectedWidget(h, v) },
                )
            }
        }
    }
}

@Composable
fun CanvasWidgetItem(
    widget: CanvasWidget,
    isSelected: Boolean,
    screenWidth: Float,
    screenHeight: Float,
    currentRakats: Int,
    onUpdate: (CanvasWidget) -> Unit,
    onTap: () -> Unit,
    onSizeMeasured: (String, Float, Float) -> Unit,
) {
    val currentWidget by rememberUpdatedState(widget)
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    val currentOnSizeMeasured by rememberUpdatedState(onSizeMeasured)

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = widget.offsetX
                translationY = widget.offsetY
                scaleX = widget.scale
                scaleY = widget.scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .then(
                if (isSelected) Modifier.border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                else Modifier
            )
            .pointerInput(widget.id) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(widget.id) {
                detectTransformGestures(
                    onGesture = { _, pan, zoom, _ ->
                        val w = currentWidget
                        val newOffsetX = (w.offsetX + pan.x).coerceAtLeast(0f)
                        val newOffsetY = (w.offsetY + pan.y).coerceAtLeast(0f)
                        val newScale = (w.scale * zoom).coerceIn(0.2f, 5f)
                        
                        val updated = when (w) {
                            is CanvasWidget.RakatCount -> w.copy(offsetX = newOffsetX, offsetY = newOffsetY, scale = newScale)
                            is CanvasWidget.ClockWidget -> w.copy(offsetX = newOffsetX, offsetY = newOffsetY, scale = newScale)
                            is CanvasWidget.CustomText -> w.copy(offsetX = newOffsetX, offsetY = newOffsetY, scale = newScale)
                        }
                        currentOnUpdate(updated)
                    }
                )
            }
            .onGloballyPositioned { coordinates ->
                currentOnSizeMeasured(widget.id, coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
            }
    ) {
        // Render widgets directly without remember block to avoid recomposition lag
        val widgetFontFamily = when (widget) {
            is CanvasWidget.RakatCount -> widget.fontName.resolveFontFamily()
            is CanvasWidget.ClockWidget -> widget.fontName.resolveFontFamily()
            is CanvasWidget.CustomText -> widget.fontName.resolveFontFamily()
        }
        when (widget) {
            is CanvasWidget.RakatCount -> {
                Text(
                    text = currentRakats.toString(),
                    fontFamily = widgetFontFamily,
                    fontSize = widget.fontSizeSp.sp,
                    fontWeight = FontWeight(widget.fontWeight),
                    style = TextStyle(
                        color = Color(widget.color).copy(alpha = widget.opacity),
                        drawStyle = if (widget.isOutline) Stroke(width = 4f, join = StrokeJoin.Round) else Fill,
                        lineHeight = widget.fontSizeSp.sp,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )
            }
            is CanvasWidget.ClockWidget -> {
                LiveClockText(widget, widget.opacity, widget.isOutline)
            }
            is CanvasWidget.CustomText -> {
                Box(
                    contentAlignment = when (widget.verticalAlign) {
                        "Top" -> Alignment.TopCenter
                        "Bottom" -> Alignment.BottomCenter
                        else -> Alignment.Center
                    }
                ) {
                    Text(
                        text = widget.text,
                        fontFamily = widgetFontFamily,
                        fontSize = widget.fontSizeSp.sp,
                        fontWeight = FontWeight(widget.fontWeight),
                        fontStyle = if (widget.italic) FontStyle.Italic else FontStyle.Normal,
                        textAlign = when (widget.textAlign) {
                            "Left" -> TextAlign.Left
                            "Right" -> TextAlign.Right
                            else -> TextAlign.Center
                        },
                        style = TextStyle(
                            color = Color(widget.color).copy(alpha = widget.opacity),
                            drawStyle = if (widget.isOutline) Stroke(width = 4f, join = StrokeJoin.Round) else Fill,
                            lineHeight = widget.fontSizeSp.sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveClockText(widget: CanvasWidget.ClockWidget, opacity: Float, isOutline: Boolean) {
    var timeStr by remember { mutableStateOf("") }
    LaunchedEffect(widget.showSeconds, widget.use24Hour) {
        while (true) {
            val now = java.time.LocalTime.now()
            val h = if (widget.use24Hour) now.hour else now.hour.let { if (it == 0) 12 else it }
            val m = now.minute
            val s = now.second
            timeStr = if (widget.showSeconds) "%02d:%02d:%02d".format(h, m, s)
                      else "%02d:%02d".format(h, m)
            delay(1000)
        }
    }
    val fontFamily = widget.fontName.resolveFontFamily()
    Text(
        text = timeStr,
        fontFamily = fontFamily,
        fontSize = widget.fontSizeSp.sp,
        style = TextStyle(
            color = Color(widget.color).copy(alpha = opacity),
            drawStyle = if (isOutline) Stroke(width = 4f, join = StrokeJoin.Round) else Fill,
            lineHeight = widget.fontSizeSp.sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both
            )
        )
    )
}

@Composable
private fun LiveClockTextPreview(widget: CanvasWidget.ClockWidget) {
    val fontFamily = widget.fontName.resolveFontFamily()
    Text(
        text = if (widget.showSeconds) "12:00:00" else "12:00",
        fontFamily = fontFamily,
        fontSize = widget.fontSizeSp.sp,
        style = TextStyle(
            color = Color(widget.color).copy(alpha = widget.opacity),
            drawStyle = if (widget.isOutline) Stroke(width = 4f, join = StrokeJoin.Round) else Fill,
            lineHeight = widget.fontSizeSp.sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both
            )
        )
    )
}

private val CANVAS_BACKGROUNDS = listOf(
    Pair(Color(0xFF000000), "True Black"),        // True black for OLED
    Pair(Color(0xFF121212), "Soft Black"),       // Soft black 
    Pair(Color(0xFF16213E), "Midnight Blue"),    // Dark Blue
    Pair(Color(0xFF1B4332), "Dark Green"),       // Dark Green
    Pair(Color(0xFF2C3E50), "Charcoal"),         // Dark Slate
)

private val WIDGET_COLORS = listOf(
    Color.White,
    Color(0xFFD4AF37), // Gold
    Color(0xFF90EE90), // Light Green
    Color(0xFF87CEEB), // Sky Blue
    Color(0xFFFFB6C1), // Light Pink
    Color(0xFFE6E6FA), // Lavender
    Color(0xFFFFD700), // Bright Gold
    Color(0xFF98FB98), // Pale Green
    Color(0xFFADD8E6), // Light Blue
    Color(0xFFFFB347), // Pastel Orange
    Color(0xFFFF6961), // Pastel Red
    Color(0xFFB19CD9), // Pastel Purple
    Color(0xFF779ECB), // Steel Blue
    Color(0xFFAEC6CF), // Pastel Blue
    Color(0xFFFFDAB9), // Peach
    Color(0xFF98D8C8), // Mint
)

private fun Color.toIntArgb(): Int {
    val a = (alpha * 255).toInt()
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

private fun Int.toColor(): Color = Color(this.toLong())

private fun Color.toSolidInt(): Int {
    return 0xFF000000.toInt() or toIntArgb()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWidgetSheet(
    currentBackground: Int,
    onAdd: (CanvasWidget) -> Unit,
    onBackgroundChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var showColorPicker by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Canvas Background", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(CANVAS_BACKGROUNDS) { (color, name) ->
                    val colorInt = color.toSolidInt()
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .border(
                                width = if (currentBackground == colorInt) 2.dp else 1.dp,
                                color = if (currentBackground == colorInt) Color.White else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onBackgroundChange(colorInt) }
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(Color.Red, Color.Green, Color.Blue)
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { showColorPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Custom Color",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (showColorPicker) {
                Spacer(Modifier.height(16.dp))
                ColorPicker(
                    onColorSelected = { colorInt ->
                        onBackgroundChange(colorInt)
                        showColorPicker = false
                    },
                    onDismiss = { showColorPicker = false }
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(Modifier.height(16.dp))

            Text("Add Widgets", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            
            ListItem(
                headlineContent = { Text("Rakat Counter") },
                supportingContent = { Text("Display the current rakat number") },
                modifier = Modifier.clickable { onAdd(CanvasWidget.RakatCount()) }
            )
            ListItem(
                headlineContent = { Text("Clock") },
                supportingContent = { Text("Keep track of prayer time") },
                modifier = Modifier.clickable { onAdd(CanvasWidget.ClockWidget()) }
            )
            ListItem(
                headlineContent = { Text("Custom Text") },
                supportingContent = { Text("Add specific dhikr or reminders") },
                modifier = Modifier.clickable { onAdd(CanvasWidget.CustomText()) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetsSheet(
    viewModel: SalahCanvasViewModel,
    actualScreenWidth: Float,
    actualScreenHeight: Float,
    onDismiss: () -> Unit,
    onLoadPreset: (List<CanvasWidget>, Int) -> Unit,
) {
    // Preset layouts
    val presets = listOf(
        CanvasPreset(
            id = "custom",
            name = "Custom",
            backgroundColor = 0xFF000000.toInt(),
            widgets = listOf(
                CanvasWidget.RakatCount(offsetX = 549.7927f, offsetY = 789.03436f, scale = 2.6846793f, color = -1, opacity = 1.0f, fontSizeSp = 180.0f, fontWeight = 400, isOutline = true, fontName = "Antonio"),
                CanvasWidget.ClockWidget(offsetX = 53.81276f, offsetY = 345.4276f, scale = 1.8795846f, color = -1, opacity = 1.0f, fontSizeSp = 48.0f, showSeconds = false, use24Hour = true, isOutline = false, fontName = "BeVietnamPro"),
                CanvasWidget.CustomText(offsetX = 92.394714f, offsetY = 999.25824f, scale = 2.133892f, text = "صلاة", color = -1, opacity = 1.0f, fontSizeSp = 32.0f, fontWeight = 400, italic = false, textAlign = "Center", verticalAlign = "Center", isOutline = false, fontName = "BeVietnamPro"),
            ),
            isDeletable = false
        ),
    )

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    
    LaunchedEffect(Unit) {
        sheetState.expand()
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        sheetState = sheetState,
        containerColor = Color(0xFF0A0A0A),
        dragHandle = null
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Presets",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = com.kaizen.khushu.ui.theme.BeVietnamPro
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Swipe to browse presets",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = com.kaizen.khushu.ui.theme.BeVietnamPro,
                color = Color.White.copy(alpha = 0.6f)
            )
            
            Spacer(Modifier.height(24.dp))
                     // Premium Preset Carousel with HorizontalPager
            val pagerState = rememberPagerState(pageCount = { presets.size })
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Single, massive reflection Box behind the pager
                val currentPreset = presets[pagerState.currentPage]
                val dominantColor = currentPreset.widgets.firstOrNull()?.let { 
                    when (it) {
                        is CanvasWidget.RakatCount -> Color(it.color)
                        is CanvasWidget.ClockWidget -> Color(it.color)
                        is CanvasWidget.CustomText -> Color(it.color)
                    }
                } ?: Color(0xFF1A1A1A)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .blur(radius = 80.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    dominantColor.copy(alpha = 0.6f),
                                    dominantColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                radius = 400f
                            )
                        )
                )

                HorizontalPager(
                    state = pagerState,
                    key = { index -> presets[index].id },
                    contentPadding = PaddingValues(horizontal = 60.dp),
                    pageSpacing = 16.dp,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val preset = presets[page]
                    val pageOffset = pagerState.currentPageOffsetFraction
                    
                    val cardScale = 1f - (kotlin.math.abs(pageOffset) * 0.15f).coerceIn(0f, 0.15f)
                    val cardAlpha = 1f - (kotlin.math.abs(pageOffset) * 0.5f).coerceIn(0f, 0.5f)
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = cardScale
                                scaleY = cardScale
                                alpha = cardAlpha
                            }
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .width(220.dp)
                                .height(420.dp)
                        ) {
                            val previewScale = constraints.maxWidth.toFloat() / actualScreenWidth

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Color(preset.backgroundColor))
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(32.dp)
                                    )
                                    .pointerInput(preset.id) {
                                        detectTapGestures(
                                            onTap = {
                                                viewModel.clearWidgets()
                                                preset.widgets.forEach { viewModel.addWidget(it) }
                                                viewModel.updateBackgroundColor(preset.backgroundColor)
                                                onDismiss()
                                            },
                                            onLongPress = {
                                                if (preset.isDeletable) {
                                                    // Menu for deletion/edit
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.TopStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = previewScale
                                            scaleY = previewScale
                                            transformOrigin = TransformOrigin(0f, 0f)
                                        }
                                ) {
                                    preset.widgets.forEach { widget ->
                                        val widgetFontFamily = when (widget) {
                                            is CanvasWidget.RakatCount -> widget.fontName.resolveFontFamily()
                                            is CanvasWidget.ClockWidget -> widget.fontName.resolveFontFamily()
                                            is CanvasWidget.CustomText -> widget.fontName.resolveFontFamily()
                                        }
                                        
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
                                            when (widget) {
                                                is CanvasWidget.RakatCount -> {
                                                    Text(
                                                        text = "4",
                                                        fontFamily = widgetFontFamily,
                                                        fontSize = widget.fontSizeSp.sp,
                                                        fontWeight = FontWeight(widget.fontWeight),
                                                        style = TextStyle(
                                                            color = Color(widget.color).copy(alpha = widget.opacity),
                                                            drawStyle = if (widget.isOutline) Stroke(width = 4f, join = StrokeJoin.Round) else Fill,
                                                            lineHeight = widget.fontSizeSp.sp,
                                                            lineHeightStyle = LineHeightStyle(
                                                                alignment = LineHeightStyle.Alignment.Center,
                                                                trim = LineHeightStyle.Trim.Both
                                                            )
                                                        )
                                                    )
                                                }
                                                is CanvasWidget.ClockWidget -> {
                                                    LiveClockTextPreview(widget)
                                                }
                                                is CanvasWidget.CustomText -> {
                                                    Box(
                                                        contentAlignment = when (widget.verticalAlign) {
                                                            "Top" -> Alignment.TopCenter
                                                            "Bottom" -> Alignment.BottomCenter
                                                            else -> Alignment.Center
                                                        }
                                                    ) {
                                                        Text(
                                                            text = widget.text,
                                                            fontFamily = widgetFontFamily,
                                                            fontSize = widget.fontSizeSp.sp,
                                                            fontWeight = FontWeight(widget.fontWeight),
                                                            fontStyle = if (widget.italic) FontStyle.Italic else FontStyle.Normal,
                                                            textAlign = when (widget.textAlign) {
                                                                "Left" -> TextAlign.Left
                                                                "Right" -> TextAlign.Right
                                                                else -> TextAlign.Center
                                                            },
                                                            style = TextStyle(
                                                                color = Color(widget.color).copy(alpha = widget.opacity),
                                                                drawStyle = if (widget.isOutline) Stroke(width = 4f, join = StrokeJoin.Round) else Fill,
                                                                lineHeight = widget.fontSizeSp.sp,
                                                                lineHeightStyle = LineHeightStyle(
                                                                    alignment = LineHeightStyle.Alignment.Center,
                                                                    trim = LineHeightStyle.Trim.Both
                                                                )
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Preset name outside card
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = BeVietnamPro,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Page indicator and bottom buttons
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(presets.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 8.dp else 6.dp)
                            .background(
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.clearWidgets()
                        viewModel.updateBackgroundColor(0xFF000000.toInt())
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create Blank")
                }
                
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigSheet(
    widget: CanvasWidget,
    onUpdate: (CanvasWidget) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onAlign: (Alignment.Horizontal?, Alignment.Vertical?) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when(widget) {
                        is CanvasWidget.RakatCount -> "Rakat Counter"
                        is CanvasWidget.ClockWidget -> "Clock"
                        is CanvasWidget.CustomText -> "Custom Text"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(Modifier.height(16.dp))

            // Common Color Picker
            Text("Color", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            val widgetColor = when (widget) {
                is CanvasWidget.RakatCount -> widget.color
                is CanvasWidget.ClockWidget -> widget.color
                is CanvasWidget.CustomText -> widget.color
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(WIDGET_COLORS) { color ->
                    val colorInt = color.toIntArgb()
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .border(1.dp, if (colorInt == widgetColor) Color.White else Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .clickable {
                                val updated = when (widget) {
                                    is CanvasWidget.RakatCount -> widget.copy(color = colorInt)
                                    is CanvasWidget.ClockWidget -> widget.copy(color = colorInt)
                                    is CanvasWidget.CustomText -> widget.copy(color = colorInt)
                                }
                                onUpdate(updated)
                            }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Opacity Control
            val widgetOpacity = when (widget) {
                is CanvasWidget.RakatCount -> widget.opacity
                is CanvasWidget.ClockWidget -> widget.opacity
                is CanvasWidget.CustomText -> widget.opacity
            }
            Text("Opacity", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = widgetOpacity,
                    onValueChange = { 
                        val updated = when (widget) {
                            is CanvasWidget.RakatCount -> widget.copy(opacity = it)
                            is CanvasWidget.ClockWidget -> widget.copy(opacity = it)
                            is CanvasWidget.CustomText -> widget.copy(opacity = it)
                        }
                        onUpdate(updated)
                    },
                    valueRange = 0.1f..1f,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Text("${(widgetOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))

            // Outline Toggle (for all widgets)
            val isOutline = when (widget) {
                is CanvasWidget.RakatCount -> widget.isOutline
                is CanvasWidget.ClockWidget -> widget.isOutline
                is CanvasWidget.CustomText -> widget.isOutline
            }
            if (widget is CanvasWidget.RakatCount || widget is CanvasWidget.ClockWidget || widget is CanvasWidget.CustomText) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Outline", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(
                        checked = isOutline,
                        onCheckedChange = {
                            val updated = when (widget) {
                                is CanvasWidget.RakatCount -> widget.copy(isOutline = it)
                                is CanvasWidget.ClockWidget -> widget.copy(isOutline = it)
                                is CanvasWidget.CustomText -> widget.copy(isOutline = it)
                                else -> widget
                            }
                            onUpdate(updated)
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Alignment Controls (Figma-style)
            Text("Alignment", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Horizontal alignment row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onAlign(Alignment.Start, null) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.AlignHorizontalLeft, contentDescription = "Left", modifier = Modifier.size(20.dp))
                }
                FilledTonalButton(
                    onClick = { onAlign(Alignment.CenterHorizontally, null) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.AlignHorizontalCenter, contentDescription = "Center H", modifier = Modifier.size(20.dp))
                }
                FilledTonalButton(
                    onClick = { onAlign(Alignment.End, null) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.AlignHorizontalRight, contentDescription = "Right", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(4.dp))

            // Vertical alignment row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onAlign(null, Alignment.Top) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.VerticalAlignTop, contentDescription = "Top", modifier = Modifier.size(20.dp))
                }
                FilledTonalButton(
                    onClick = { onAlign(null, Alignment.CenterVertically) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.VerticalAlignCenter, contentDescription = "Center V", modifier = Modifier.size(20.dp))
                }
                FilledTonalButton(
                    onClick = { onAlign(null, Alignment.Bottom) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.VerticalAlignBottom, contentDescription = "Bottom", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Specific controls
            when (widget) {
                is CanvasWidget.RakatCount -> {
                    SliderControl("Size", widget.fontSizeSp, 48f, 300f) {
                        onUpdate(widget.copy(fontSizeSp = it))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bold", Modifier.weight(1f))
                        Switch(
                            checked = widget.fontWeight == 700,
                            onCheckedChange = { onUpdate(widget.copy(fontWeight = if (it) 700 else 400)) }
                        )
                    }
                }
                is CanvasWidget.ClockWidget -> {
                    SliderControl("Size", widget.fontSizeSp, 24f, 120f) {
                        onUpdate(widget.copy(fontSizeSp = it))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Show Seconds", Modifier.weight(1f))
                        Switch(checked = widget.showSeconds, onCheckedChange = { onUpdate(widget.copy(showSeconds = it)) })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("24H Format", Modifier.weight(1f))
                        Switch(checked = widget.use24Hour, onCheckedChange = { onUpdate(widget.copy(use24Hour = it)) })
                    }
                }
                is CanvasWidget.CustomText -> {
                    OutlinedTextField(
                        value = widget.text,
                        onValueChange = { onUpdate(widget.copy(text = it)) },
                        label = { Text("Text Content") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    SliderControl("Size", widget.fontSizeSp, 12f, 120f) {
                        onUpdate(widget.copy(fontSizeSp = it))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bold", Modifier.weight(1f))
                        Switch(
                            checked = widget.fontWeight == 700,
                            onCheckedChange = { onUpdate(widget.copy(fontWeight = if (it) 700 else 400)) }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Italic", Modifier.weight(1f))
                        Switch(
                            checked = widget.italic,
                            onCheckedChange = { onUpdate(widget.copy(italic = it)) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Horizontal Alignment", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Left" to Icons.AutoMirrored.Filled.AlignHorizontalLeft,
                            "Center" to Icons.Default.AlignHorizontalCenter,
                            "Right" to Icons.AutoMirrored.Filled.AlignHorizontalRight
                        ).forEach { (align, icon) ->
                            IconButton(
                                onClick = { onUpdate(widget.copy(textAlign = align)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (widget.textAlign == align)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else Color.Transparent
                                    )
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = align,
                                    tint = if (widget.textAlign == align)
                                        MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Text("Vertical Alignment", style = MaterialTheme.typography.labelMedium)
                    val currentVerticalAlign = widget.verticalAlign
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            Pair("Top", Icons.Default.KeyboardArrowUp),
                            Pair("Center", Icons.Default.Close),
                            Pair("Bottom", Icons.Default.KeyboardArrowDown)
                        )
                        for ((align, icon) in options) {
                            FilledTonalButton(
                                onClick = { onUpdate(widget.copy(verticalAlign = align)) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (currentVerticalAlign == align)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(icon, contentDescription = align, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderControl(label: String, value: Float, min: Float, max: Float, onUpdate: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = onUpdate, valueRange = min..max)
    }
}

private fun Color.toArgb(): Int {
    return (alpha * 255.0f + 0.5f).toInt() shl 24 or
           (red * 255.0f + 0.5f).toInt() shl 16 or
           (green * 255.0f + 0.5f).toInt() shl 8 or
           (blue * 255.0f + 0.5f).toInt()
}

@Composable
private fun ColorPicker(
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var red by remember { mutableFloatStateOf(0.1f) }
    var green by remember { mutableFloatStateOf(0.1f) }
    var blue by remember { mutableFloatStateOf(0.1f) }
    
    val selectedColor = Color(red, green, blue)
    val selectedColorInt = selectedColor.toSolidInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            "Custom Background Color",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Color preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(selectedColor)
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Red slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
            Spacer(Modifier.width(8.dp))
            Slider(
                value = red,
                onValueChange = { red = it },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            Text("${(red * 255).toInt()}", style = MaterialTheme.typography.bodySmall)
        }
        
        // Green slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Green)
            )
            Spacer(Modifier.width(8.dp))
            Slider(
                value = green,
                onValueChange = { green = it },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            Text("${(green * 255).toInt()}", style = MaterialTheme.typography.bodySmall)
        }
        
        // Blue slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Blue)
            )
            Spacer(Modifier.width(8.dp))
            Slider(
                value = blue,
                onValueChange = { blue = it },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            Text("${(blue * 255).toInt()}", style = MaterialTheme.typography.bodySmall)
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { onColorSelected(selectedColorInt) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply")
            }
        }
    }
}
