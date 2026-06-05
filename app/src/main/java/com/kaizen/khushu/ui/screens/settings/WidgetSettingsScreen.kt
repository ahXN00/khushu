package com.kaizen.khushu.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaizen.khushu.data.repository.UserSettings
import com.kaizen.khushu.ui.theme.BeVietnamPro
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val extendedColorPalette = listOf(
    "#0D1D0F", "#1A1A1A", "#000000", "#1E2A38", "#2C3E50", "#4A5F63",
    "#2E4053", "#1B2631", "#17202A", "#1C2833", "#141D26", "#0E1621",
    "#FFFFFF", "#FDFEFE", "#F4F6F7", "#D5D8DC", "#ABB2B9", "#808B96",
    "#7ED957", "#27AE60", "#229954", "#1E8449", "#196F3D", "#145A32",
    "#2980B9", "#2471A3", "#1F618D", "#1A5276", "#154360", "#1B4F72",
    "#8E44AD", "#7D3C98", "#6C3483", "#5B2C6F", "#4A235A", "#48C9B0",
    "#F1C40F", "#F39C12", "#E67E22", "#D35400", "#C0392B", "#A93226"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WidgetSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    var showColorPickerFor by remember { mutableStateOf<String?>(null) } // "bg", "panel", "font"

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { SettingsTopBarTitle("Widget Designer", scrollBehavior) },
                navigationIcon = { SettingsBackButton(onBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            
            // --- LIVE PREVIEW ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                WidgetPreview(settings)
            }

            Spacer(Modifier.height(8.dp))

            // --- BACKGROUND CONTROLS ---
            SettingsGroup(title = "Background Layer") {
                ColorPaletteSlider(
                    title = "Color",
                    selectedColor = settings.widgetBackgroundColor,
                    opacity = settings.widgetBackgroundOpacity,
                    onColorChange = {
                        viewModel.updateWidgetCustomization(
                            backgroundColor = it,
                            backgroundOpacity = settings.widgetBackgroundOpacity,
                            panelColor = settings.widgetPanelColor,
                            panelOpacity = settings.widgetPanelOpacity,
                            fontColor = settings.widgetFontColor
                        )
                    },
                    onOpacityChange = {
                        viewModel.updateWidgetCustomization(
                            backgroundColor = settings.widgetBackgroundColor,
                            backgroundOpacity = it,
                            panelColor = settings.widgetPanelColor,
                            panelOpacity = settings.widgetPanelOpacity,
                            fontColor = settings.widgetFontColor
                        )
                    },
                    onCustomClick = { showColorPickerFor = "bg" }
                )
            }

            // --- PANEL CONTROLS ---
            SettingsGroup(title = "Prayer Panel Layer") {
                ColorPaletteSlider(
                    title = "Color",
                    selectedColor = settings.widgetPanelColor,
                    opacity = settings.widgetPanelOpacity,
                    opacityRange = 0f..0.8f,
                    onColorChange = {
                        viewModel.updateWidgetCustomization(
                            backgroundColor = settings.widgetBackgroundColor,
                            backgroundOpacity = settings.widgetBackgroundOpacity,
                            panelColor = it,
                            panelOpacity = settings.widgetPanelOpacity,
                            fontColor = settings.widgetFontColor
                        )
                    },
                    onOpacityChange = {
                        viewModel.updateWidgetCustomization(
                            backgroundColor = settings.widgetBackgroundColor,
                            backgroundOpacity = settings.widgetBackgroundOpacity,
                            panelColor = settings.widgetPanelColor,
                            panelOpacity = it,
                            fontColor = settings.widgetFontColor
                        )
                    },
                    onCustomClick = { showColorPickerFor = "panel" }
                )
            }

            // --- FONT CONTROLS ---
            SettingsGroup(title = "Typography") {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Font Color",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = BeVietnamPro,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        IconButton(onClick = { showColorPickerFor = "font" }) {
                            Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        extendedColorPalette.take(18).forEach { hex ->
                            ColorCircle(
                                hex = hex,
                                isSelected = settings.widgetFontColor == hex,
                                onClick = {
                                    viewModel.updateWidgetCustomization(
                                        backgroundColor = settings.widgetBackgroundColor,
                                        backgroundOpacity = settings.widgetBackgroundOpacity,
                                        panelColor = settings.widgetPanelColor,
                                        panelOpacity = settings.widgetPanelOpacity,
                                        fontColor = hex
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showColorPickerFor != null) {
        val initialHex = when (showColorPickerFor) {
            "bg" -> settings.widgetBackgroundColor
            "panel" -> settings.widgetPanelColor
            else -> settings.widgetFontColor
        }
        
        AdvancedColorPickerSheet(
            initialColor = Color(android.graphics.Color.parseColor(initialHex)),
            onColorSelected = { color ->
                val hex = String.format("#%06X", 0xFFFFFF and color.toArgb())
                when (showColorPickerFor) {
                    "bg" -> viewModel.updateWidgetCustomization(hex, settings.widgetBackgroundOpacity, settings.widgetPanelColor, settings.widgetPanelOpacity, settings.widgetFontColor)
                    "panel" -> viewModel.updateWidgetCustomization(settings.widgetBackgroundColor, settings.widgetBackgroundOpacity, hex, settings.widgetPanelOpacity, settings.widgetFontColor)
                    "font" -> viewModel.updateWidgetCustomization(settings.widgetBackgroundColor, settings.widgetBackgroundOpacity, settings.widgetPanelColor, settings.widgetPanelOpacity, hex)
                }
                showColorPickerFor = null
            },
            onDismiss = { showColorPickerFor = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedColorPickerSheet(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }
    var hexString by remember { mutableStateOf("") }

    LaunchedEffect(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        hexString = String.format("#%06X", 0xFFFFFF and initialColor.toArgb())
    }

    val currentColor = Color.hsv(hue, saturation, value)

    LaunchedEffect(hue, saturation, value) {
        hexString = String.format("#%06X", 0xFFFFFF and currentColor.toArgb())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = Color.White // Match the look from the user's image
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hex Code and Preview Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = hexString,
                    onValueChange = { newHex ->
                        hexString = newHex
                        if (newHex.length == 7 && newHex.startsWith("#")) {
                            try {
                                val parsedColor = Color(android.graphics.Color.parseColor(newHex))
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsedColor.toArgb(), hsv)
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                            } catch (_: Exception) {}
                        }
                    },
                    label = { Text("Hex code", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.LightGray,
                        unfocusedBorderColor = Color.LightGray,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                )
            }

            Spacer(Modifier.height(32.dp))

            // Color Wheel (Hue)
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val pos = change.position
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val angle = atan2(pos.y - centerY, pos.x - centerX) * (180f / Math.PI.toFloat())
                            hue = (angle + 360f) % 360f
                            saturation = (hypot(pos.x - centerX, pos.y - centerY) / (size.width / 2f)).coerceIn(0f, 1f)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2
                    val brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red
                        )
                    )
                    drawCircle(brush = brush, radius = radius)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color.Transparent),
                            radius = radius
                        ),
                        radius = radius
                    )
                    
                    // Selector
                    val selectorRadius = 10.dp.toPx()
                    val angleRad = Math.toRadians(hue.toDouble()).toFloat()
                    val dist = saturation * radius
                    val selectorX = center.x + cos(angleRad) * dist
                    val selectorY = center.y + sin(angleRad) * dist
                    
                    drawCircle(
                        color = Color.White,
                        radius = selectorRadius,
                        center = Offset(selectorX, selectorY),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Value (Brightness) Slider
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            value = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    }
            ) {
                val hsvColorStart = Color.hsv(hue, saturation, 0f)
                val hsvColorEnd = Color.hsv(hue, saturation, 1f)
                drawRect(
                    brush = Brush.horizontalGradient(listOf(hsvColorStart, hsvColorEnd))
                )
                
                // Slider thumb
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(value * size.width, size.height / 2f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            Spacer(Modifier.height(48.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text("Back", color = Color.Gray, fontFamily = BeVietnamPro)
                }
                Button(
                    onClick = { onColorSelected(currentColor) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.LightGray),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Select", color = Color.Gray, fontFamily = BeVietnamPro)
                }
            }
        }
    }
}

@Composable
private fun ColorPaletteSlider(
    title: String,
    selectedColor: String,
    opacity: Float,
    opacityRange: ClosedFloatingPointRange<Float> = 0.1f..1.0f,
    onColorChange: (String) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onCustomClick: () -> Unit
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.SemiBold
                )
            )
            IconButton(onClick = onCustomClick) {
                Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Box(modifier = Modifier.height(110.dp)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(40.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(extendedColorPalette) { hex ->
                    ColorCircle(
                        hex = hex,
                        isSelected = selectedColor == hex,
                        onClick = { onColorChange(hex) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Transparency (${(opacity * 100).toInt()}%)",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.SemiBold
            )
        )
        Slider(
            value = opacity,
            onValueChange = onOpacityChange,
            valueRange = opacityRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ColorCircle(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = Color(android.graphics.Color.parseColor(hex))
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun WidgetPreview(settings: UserSettings) {
    val bgColor = Color(android.graphics.Color.parseColor(settings.widgetBackgroundColor)).copy(alpha = settings.widgetBackgroundOpacity)
    val panelColor = Color(android.graphics.Color.parseColor(settings.widgetPanelColor)).copy(alpha = settings.widgetPanelOpacity)
    val fontColor = Color(android.graphics.Color.parseColor(settings.widgetFontColor))
    val subFontColor = fontColor.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = fontColor, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Fri, 5 June", color = fontColor, fontSize = 11.sp, fontFamily = BeVietnamPro)
            }
            Text("19 Dhul-Hijjah", color = fontColor, fontSize = 11.sp, fontFamily = BeVietnamPro)
        }
        
        Spacer(Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MyLocation, null, tint = subFontColor, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Düsseldorf", color = subFontColor, fontSize = 10.sp, fontFamily = BeVietnamPro)
            }
            Text("Fajr: 03:32:21", color = subFontColor, fontSize = 10.sp, fontFamily = BeVietnamPro)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(panelColor)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Fajr" to "03:48", "Shuruq" to "05:20", "Jumaah" to "13:31", "Asr" to "17:52").forEach { (label, time) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, color = subFontColor, fontSize = 9.sp, fontFamily = BeVietnamPro)
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (label == "Fajr") Color(0xFF7ED957) else Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(time, color = if (label == "Fajr") Color.Black else fontColor, fontSize = 9.sp, fontFamily = BeVietnamPro)
                    }
                }
            }
        }
    }
}
