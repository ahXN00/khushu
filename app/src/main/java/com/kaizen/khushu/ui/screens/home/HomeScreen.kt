package com.kaizen.khushu.ui.screens.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.kaizen.khushu.ui.components.KhushuAppBar
import com.kaizen.khushu.R
import dev.chrisbanes.haze.HazeState
import java.util.concurrent.TimeUnit

private fun findNextPrayer(prayers: List<PrayerInfo>, now: Long): PrayerInfo? {
    if (prayers.isEmpty()) return null

    prayers.firstOrNull { it.rawTime > now }?.let { return it }

    val fajr = prayers.first()
    return fajr.copy(rawTime = fajr.rawTime + TimeUnit.DAYS.toMillis(1))
}

@Composable
private fun KhushuPullRefreshIndicator(
    progress: Float,
    isRefreshing: Boolean,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val logoRes = if (darkTheme) R.drawable.ic_khushu_logo else R.drawable.ic_khushu_logo_black
    val visibleProgress = if (isRefreshing) 1f else progress.coerceIn(0f, 1f)
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isRefreshing || progress > 0f) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "home_refresh_indicator_alpha"
    )
    val pulseScaleBase = rememberInfiniteTransition(label = "home_refresh_logo_pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "home_refresh_logo_scale"
    ).value
    val pulseScale = if (isRefreshing) pulseScaleBase else 1f

    Box(
        modifier = modifier.graphicsLayer {
            alpha = indicatorAlpha
            scaleX = pulseScale
            scaleY = pulseScale
        },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = logoRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
            modifier = Modifier.fillMaxSize()
        )
        Icon(
            painter = painterResource(id = logoRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val clipTop = size.height * (1f - visibleProgress)
                    clipRect(top = clipTop) {
                        this@drawWithContent.drawContent()
                    }
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    hazeState: HazeState,
    contentPadding: PaddingValues,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val darkTheme = isSystemInDarkTheme()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val refreshThresholdPx = with(density) { 118.dp.toPx() }
    val refreshHoldPx = with(density) { 74.dp.toPx() }
    val maxPullPx = with(density) { 164.dp.toPx() }

    var doneStates by remember {
        mutableStateOf(
            mapOf(
                "Fajr" to true,
                "Dhuhr" to false,
                "Asr" to false,
                "Maghrib" to false,
                "Isha" to false
            )
        )
    }
    var showTimeOverrideDialog by remember { mutableStateOf(false) }
    var previewHourText by remember { mutableStateOf("") }
    var previewMinuteText by remember { mutableStateOf("") }
    var pullOffsetPx by remember { mutableFloatStateOf(0f) }
    var thresholdReached by remember { mutableStateOf(false) }

    val nextPrayer = findNextPrayer(uiState.prayers, uiState.currentTimeMillis)
    val doneCount = doneStates.values.count { it }
    val pullProgress = (pullOffsetPx / refreshThresholdPx).coerceIn(0f, 1f)
    val animatedPullOffsetPx by animateFloatAsState(
        targetValue = when {
            uiState.isRefreshing -> refreshHoldPx
            else -> pullOffsetPx
        },
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.88f),
        label = "home_pull_offset"
    )

    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            pullOffsetPx = 0f
            thresholdReached = false
        }
    }

    val pullRefreshConnection = remember(listState, uiState.isRefreshing) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (source != NestedScrollSource.UserInput) return androidx.compose.ui.geometry.Offset.Zero

                if (available.y < 0f && pullOffsetPx > 0f) {
                    val previous = pullOffsetPx
                    pullOffsetPx = (pullOffsetPx + available.y).coerceAtLeast(0f)
                    if (pullOffsetPx < refreshThresholdPx) {
                        thresholdReached = false
                    }
                    return androidx.compose.ui.geometry.Offset(0f, pullOffsetPx - previous)
                }

                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (source != NestedScrollSource.UserInput || uiState.isRefreshing) {
                    return androidx.compose.ui.geometry.Offset.Zero
                }

                if (available.y > 0f && !listState.canScrollBackward) {
                    val previous = pullOffsetPx
                    val resistance = 0.55f - ((previous / maxPullPx).coerceIn(0f, 1f) * 0.2f)
                    pullOffsetPx = (previous + available.y * resistance).coerceAtMost(maxPullPx)

                    if (pullOffsetPx >= refreshThresholdPx && !thresholdReached) {
                        thresholdReached = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else if (pullOffsetPx < refreshThresholdPx) {
                        thresholdReached = false
                    }

                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }

                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                if (!uiState.isRefreshing && pullOffsetPx >= refreshThresholdPx) {
                    pullOffsetPx = refreshHoldPx
                    viewModel.refreshPrayerData()
                } else if (!uiState.isRefreshing) {
                    pullOffsetPx = 0f
                    thresholdReached = false
                }

                return Velocity.Zero
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullRefreshConnection)
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 28.dp,
                    bottom = 0.dp
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = animatedPullOffsetPx }
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NextPrayerCard(
                            prayer = nextPrayer,
                            doneCount = doneCount,
                            source = uiState.calculationSource,
                            usingPreviewTime = uiState.usingPreviewTime,
                            onTimeClick = {
                                showTimeOverrideDialog = true
                                previewHourText = ""
                                previewMinuteText = ""
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SunArcCard(
                            sunT = uiState.sunArcT,
                            nextT = nextPrayer?.arcT,
                            nextName = nextPrayer?.name.orEmpty(),
                            makruhZones = uiState.makruhZones,
                            darkTheme = darkTheme,
                            hijriDate = uiState.hijriDate,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(14.dp)) }

                item {
                    EventsStrip(events = uiState.events)
                }

                item { Spacer(modifier = Modifier.height(14.dp)) }

                item {
                    PrayerSlab(
                        prayers = uiState.prayers,
                        doneStates = doneStates,
                        onToggleDone = { name ->
                            doneStates = doneStates.toMutableMap().apply {
                                this[name] = !(this[name] ?: false)
                            }
                        },
//                    ayahText = uiState.ayahText,
                        ayahRef = uiState.ayahRef,
                        darkTheme = darkTheme,
                        bottomPadding = contentPadding.calculateBottomPadding()
                    )
                }
            }

            KhushuPullRefreshIndicator(
                progress = pullProgress,
                isRefreshing = uiState.isRefreshing,
                darkTheme = darkTheme,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding() + 14.dp)
                    .size(52.dp)
                    .graphicsLayer {
                        translationY = (animatedPullOffsetPx * 0.22f) - with(density) { 8.dp.toPx() }
                    }
            )
        }

        KhushuAppBar(
            title = "Khushu", // Antonio font handled internally
            onSettingsClick = onSettingsClick,
//            hazeState = hazeState, // Unused in newer version of KhushuAppBar
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (showTimeOverrideDialog) {
        AlertDialog(
            onDismissRequest = { showTimeOverrideDialog = false },
            title = { Text("Preview Prayer Time") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Set a temporary 24-hour clock time to test next prayer and makruh transitions.")
                    OutlinedTextField(
                        value = previewHourText,
                        onValueChange = { previewHourText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Hour (0-23)") }
                    )
                    OutlinedTextField(
                        value = previewMinuteText,
                        onValueChange = { previewMinuteText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Minute (0-59)") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hour = previewHourText.toIntOrNull()
                        val minute = previewMinuteText.toIntOrNull()
                        if (hour != null && minute != null) {
                            viewModel.setPreviewTime(hour, minute)
                            showTimeOverrideDialog = false
                        }
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearPreviewTime()
                            showTimeOverrideDialog = false
                        }
                    ) {
                        Text("Reset")
                    }
                    TextButton(onClick = { showTimeOverrideDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}
