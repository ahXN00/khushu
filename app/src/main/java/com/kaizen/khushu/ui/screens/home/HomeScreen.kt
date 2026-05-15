package com.kaizen.khushu.ui.screens.home

import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaizen.khushu.R
import com.kaizen.khushu.ui.components.KhushuAppBar
import com.kaizen.khushu.ui.theme.BeVietnamPro
import dev.chrisbanes.haze.HazeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun findNextPrayer(prayers: List<PrayerInfo>, now: Long): PrayerInfo? {
    if (prayers.isEmpty()) return null

    prayers.firstOrNull { it.rawTime > now }?.let {
        return it
    }

    val fajr = prayers.first()
    return fajr.copy(rawTime = fajr.rawTime + TimeUnit.DAYS.toMillis(1))
}

private fun findCurrentPrayer(prayers: List<PrayerInfo>, now: Long): PrayerInfo? {
    if (prayers.isEmpty()) return null
    return prayers.lastOrNull { it.rawTime <= now } ?: prayers.last()
}

private fun homeDayStamp(epochMillis: Long): String {
    val effectiveMillis = if (epochMillis > 0L) epochMillis else System.currentTimeMillis()
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(effectiveMillis))
}

private fun emptyPrayerDoneStates(prayers: List<PrayerInfo>): Map<String, Boolean> {
    val names =
            prayers.filterNot { it.isExtra }.map { it.name }.ifEmpty {
                listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
            }
    return names.associateWith { false }
}

private data class HomeHijriBadgeParts(
        val dayMonth: String,
        val year: String,
)

private fun splitHijriBadgeParts(raw: String): HomeHijriBadgeParts {
    val compact = raw.trim().replace(Regex("\\s+"), " ")
    if (compact.isBlank()) {
        return HomeHijriBadgeParts(dayMonth = "Hijri Date", year = "")
    }
    val pieces = compact.split(" ")
    return if (pieces.size >= 2) {
        HomeHijriBadgeParts(dayMonth = pieces.dropLast(1).joinToString(" "), year = pieces.last())
    } else {
        HomeHijriBadgeParts(dayMonth = compact, year = "")
    }
}

@Composable
private fun HomeHijriBadge(
        badge: HomeHijriBadgeParts,
        modifier: Modifier = Modifier,
) {
    val label =
            remember(badge) {
                buildString {
                    append(badge.dayMonth)
                    if (badge.year.isNotBlank()) {
                        append("  |  ")
                        append(badge.year)
                    }
                }
            }

    Surface(
            modifier = modifier,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            // border =
            //         androidx.compose.foundation.BorderStroke(
            //                 0.8.dp,
            //                 MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
            //         )
            ) {
        Text(
                text = label,
                style =
                        MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = BeVietnamPro,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                        ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

private suspend fun resolveLocationLabel(context: Context, lat: Float, lng: Float): String {
    return withContext(Dispatchers.IO) {
        runCatching {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val results = mutableListOf<android.location.Address>()
                                val latch = CountDownLatch(1)
                                geocoder.getFromLocation(lat.toDouble(), lng.toDouble(), 1) { found
                                    ->
                                    results += found
                                    latch.countDown()
                                }
                                latch.await()
                                results
                            } else {
                                geocoder.getFromLocation(lat.toDouble(), lng.toDouble(), 1)
                                        .orEmpty()
                            }

                    val best = addresses.firstOrNull()
                    listOfNotNull(
                                    best?.locality?.takeIf { it.isNotBlank() },
                                    best?.subAdminArea?.takeIf { it.isNotBlank() },
                                    best?.adminArea?.takeIf { it.isNotBlank() }
                            )
                            .firstOrNull()
                }
                .getOrNull()
                ?: "Your area"
    }
}

@Composable
private fun KhushuPullRefreshIndicator(
        progress: Float,
        isRefreshing: Boolean,
        darkTheme: Boolean,
        modifier: Modifier = Modifier,
) {
    val logoRes = if (darkTheme) R.drawable.ic_khushu_logo else R.drawable.ic_khushu_logo_black
    val targetProgress = if (isRefreshing) 1f else progress.coerceIn(0f, 1f)
    val visibleProgress by
            animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = tween(durationMillis = if (isRefreshing) 240 else 120),
                    label = "home_refresh_indicator_progress"
            )
    val indicatorAlpha by
            animateFloatAsState(
                    targetValue = if (isRefreshing || progress > 0f) 1f else 0f,
                    animationSpec = tween(durationMillis = 180),
                    label = "home_refresh_indicator_alpha"
            )
    val pulseScaleBase =
            rememberInfiniteTransition(label = "home_refresh_logo_pulse")
                    .animateFloat(
                            initialValue = 1f,
                            targetValue = 1.10f,
                            animationSpec =
                                    infiniteRepeatable(
                                            animation = tween(
                                                durationMillis = 900,
                                                easing = FastOutSlowInEasing
                                            ),
                                            repeatMode = RepeatMode.Reverse
                                    ),
                            label = "home_refresh_logo_scale"
                    )
                    .value
    val pulseScale = if (isRefreshing) pulseScaleBase else 1f

    Box(
            modifier =
                    modifier.graphicsLayer {
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
                modifier =
                        Modifier.fillMaxSize().drawWithContent {
                            val clipTop = size.height * (1f - visibleProgress)
                            clipRect(top = clipTop) { this@drawWithContent.drawContent() }
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
        onPrayClick: () -> Unit,
        viewModel: HomeViewModel,
        modifier: Modifier = Modifier,
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val refreshThresholdPx = with(density) { 118.dp.toPx() }
    val refreshHoldPx = with(density) { 74.dp.toPx() }
    val maxPullPx = with(density) { 164.dp.toPx() }
    val currentDayStamp = homeDayStamp(uiState.currentTimeMillis)
    var cachedPrayers by remember { mutableStateOf<List<PrayerInfo>>(emptyList()) }
    var cachedExtraTimings by remember { mutableStateOf<List<PrayerInfo>>(emptyList()) }
    var cachedEvents by remember { mutableStateOf<List<IslamicEvent>>(emptyList()) }
    var cachedCalendarEvents by remember { mutableStateOf<List<IslamicEvent>>(emptyList()) }
    var cachedHijriDate by remember { mutableStateOf("") }
    var cachedEventsHeader by remember { mutableStateOf("") }

    val basePrayerList = (uiState.prayers.ifEmpty { cachedPrayers }).filterNot { it.isExtra }
    var doneStates by
            rememberSaveable(currentDayStamp) {
                mutableStateOf(emptyPrayerDoneStates(basePrayerList))
            }
    var showTimeOverrideDialog by remember { mutableStateOf(false) }
    var selectedQuickAction by remember { mutableStateOf<HomeQuickAction?>(null) }
    var previewHourText by remember { mutableStateOf("") }
    var previewMinuteText by remember { mutableStateOf("") }
    var pullOffsetPx by remember { mutableFloatStateOf(0f) }
    var thresholdReached by remember { mutableStateOf(false) }

    LaunchedEffect(
            uiState.prayers,
            uiState.extraTimings,
            uiState.events,
            uiState.calendarEvents,
            uiState.hijriDate,
            uiState.eventsHeader
    ) {
        if (uiState.prayers.isNotEmpty()) {
            cachedPrayers = uiState.prayers
        }
        if (uiState.extraTimings.isNotEmpty() || cachedExtraTimings.isEmpty()) {
            cachedExtraTimings = uiState.extraTimings
        }
        if (uiState.events.isNotEmpty() || cachedEvents.isEmpty()) {
            cachedEvents = uiState.events
        }
        if (uiState.calendarEvents.isNotEmpty() || cachedCalendarEvents.isEmpty()) {
            cachedCalendarEvents = uiState.calendarEvents
        }
        if (uiState.hijriDate.isNotBlank()) {
            cachedHijriDate = uiState.hijriDate
        }
        if (uiState.eventsHeader.isNotBlank()) {
            cachedEventsHeader = uiState.eventsHeader
        }
    }

    LaunchedEffect(basePrayerList) {
        val expected = emptyPrayerDoneStates(basePrayerList)
        if (expected.keys != doneStates.keys) {
            doneStates = expected
        }
    }

    val displayPrayers = uiState.prayers.ifEmpty { cachedPrayers }
    val displayExtraTimings = uiState.extraTimings.ifEmpty { cachedExtraTimings }
    val displayEvents = uiState.events.ifEmpty { cachedEvents }
    val displayCalendarEvents = uiState.calendarEvents.ifEmpty { cachedCalendarEvents }
    val displayHijriDate = uiState.hijriDate.ifBlank { cachedHijriDate }
    val displayEventsHeader = uiState.eventsHeader.ifBlank { cachedEventsHeader }
    val hijriBadge = remember(displayHijriDate) { splitHijriBadgeParts(displayHijriDate) }

    val currentPrayer = findCurrentPrayer(displayPrayers, uiState.currentTimeMillis)
    val homeVisibleTimings =
            if (uiState.showExtraPrayerTimingsOnHome) {
                (displayPrayers + displayExtraTimings).sortedBy { it.rawTime }
            } else {
                displayPrayers
            }
    val nextPrayer = findNextPrayer(homeVisibleTimings, uiState.currentTimeMillis)
    val doneCount = doneStates.values.count { it }
    val locationLabel by
            produceState(
                    initialValue = uiState.locationLabel.ifBlank { "Your area" },
                    context,
                    uiState.locationLat,
                    uiState.locationLng,
                    uiState.locationLabel
            ) {
                value =
                        when {
                            uiState.locationLabel.isNotBlank() -> uiState.locationLabel
                            else ->
                                    resolveLocationLabel(
                                            context.applicationContext,
                                            uiState.locationLat,
                                            uiState.locationLng
                                    )
                        }
            }
    val pullProgress = (pullOffsetPx / refreshThresholdPx).coerceIn(0f, 1f)

    // Detect how much of the PrayerSlab (always the last LazyColumn item) is visible.
    // When ≥80% is visible, reveal the EXPLORE quick-action row.
    val slabVisibilityFraction by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastItem = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf 0f
            if (lastItem.index != info.totalItemsCount - 1) return@derivedStateOf 0f
            val viewportHeight = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
            if (viewportHeight <= 0f) return@derivedStateOf 0f
            val offset = lastItem.offset.coerceAtLeast(0).toFloat()
            ((viewportHeight - offset) / viewportHeight).coerceIn(0f, 1f)
        }
    }
    val showSlabQuickActions = slabVisibilityFraction >= 0.8f
    val animatedPullOffsetPx by
            animateFloatAsState(
                    targetValue =
                            when {
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

    val pullRefreshConnection =
            remember(listState, uiState.isRefreshing) {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                            available: androidx.compose.ui.geometry.Offset,
                            source: NestedScrollSource
                    ): androidx.compose.ui.geometry.Offset {
                        if (source != NestedScrollSource.UserInput)
                                return androidx.compose.ui.geometry.Offset.Zero

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
                            val resistance =
                                    0.55f - ((previous / maxPullPx).coerceIn(0f, 1f) * 0.2f)
                            pullOffsetPx =
                                    (previous + available.y * resistance).coerceAtMost(maxPullPx)

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
        Box(modifier = Modifier.fillMaxSize().nestedScroll(pullRefreshConnection)) {
            LazyColumn(
                    state = listState,
                    contentPadding =
                            PaddingValues(
                                    top = contentPadding.calculateTopPadding() + 22.dp,
                                    bottom = 0.dp
                            ),
                    modifier =
                            Modifier.fillMaxSize().graphicsLayer {
                                translationY = animatedPullOffsetPx
                            }
            ) {
                item {
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NextPrayerCard(
                                currentPrayer = currentPrayer,
                                nextPrayer = nextPrayer,
                                locationLabel = locationLabel,
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
                                sunriseTime = (displayPrayers + displayExtraTimings)
                                        .firstOrNull {
                                            it.name.contains("shuruq", ignoreCase = true) ||
                                            it.name.contains("sunrise", ignoreCase = true)
                                        }?.time ?: "",
                                sunsetTime = displayPrayers
                                        .firstOrNull { it.name.equals("Maghrib", ignoreCase = true) }
                                        ?.time ?: "",
                                pastPrayerTs = displayPrayers
                                        .filterNot { it.isExtra }
                                        .filter { uiState.sunArcT > it.arcT }
                                        .map { it.arcT },
                                modifier = Modifier.weight(1f)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(14.dp)) }

                if (uiState.showUpcomingEventsOnHome) {
                    item {
                        EventsStrip(
                                header = displayEventsHeader,
                                events = displayEvents,
                                calendarEvents = displayCalendarEvents
                        )
                    }

                    item { Spacer(modifier = Modifier.height(14.dp)) }
                }

                item {
                    PrayerSlab(
                            prayers = displayPrayers,
                            extraTimings =
                                    if (uiState.showExtraPrayerTimingsOnHome) displayExtraTimings
                                    else emptyList(),
                            activePrayerName = currentPrayer?.name,
                            doneStates = doneStates,
                            onPrayClick = onPrayClick,
                            onToggleDoneAttempt = { name ->
                                val prayers = displayPrayers.filterNot { it.isExtra }
                                val tappedIndex = prayers.indexOfFirst { it.name == name }
                                if (tappedIndex == -1) {
                                    PrayerToggleOutcome(PrayerToggleResult.REJECTED_OUT_OF_ORDER)
                                } else if (doneStates[name] == true) {
                                    val rewound = doneStates.toMutableMap()
                                    prayers.drop(tappedIndex).forEach { prayer ->
                                        rewound[prayer.name] = false
                                    }
                                    doneStates = rewound
                                    PrayerToggleOutcome(PrayerToggleResult.REWOUND)
                                } else {
                                    val nextCompletable =
                                            prayers
                                                    .firstOrNull { prayer ->
                                                        !(doneStates[prayer.name] ?: false)
                                                    }
                                                    ?.name
                                    when {
                                        nextCompletable == null ->
                                                PrayerToggleOutcome(
                                                        result =
                                                                PrayerToggleResult
                                                                        .REJECTED_TOO_EARLY,
                                                        guidedPrayerName =
                                                                prayers
                                                                        .firstOrNull {
                                                                            !(doneStates[it.name]
                                                                                    ?: false)
                                                                        }
                                                                        ?.name
                                                )
                                        nextCompletable != name ->
                                                PrayerToggleOutcome(
                                                        result =
                                                                PrayerToggleResult
                                                                        .REJECTED_OUT_OF_ORDER,
                                                        guidedPrayerName = nextCompletable
                                                )
                                        else -> {
                                            doneStates =
                                                    doneStates.toMutableMap().apply {
                                                        this[name] = true
                                                    }
                                            PrayerToggleOutcome(PrayerToggleResult.COMPLETED)
                                        }
                                    }
                                }
                            },
                            onQuickActionTap = { action ->
                                when (action) {
                                    HomeQuickAction.EVENTS -> {
                                        if (uiState.showUpcomingEventsOnHome) {
                                            scope.launch { listState.animateScrollToItem(2) }
                                        } else {
                                            selectedQuickAction = action
                                        }
                                    }
                                    else -> selectedQuickAction = action
                                }
                            },
                            //                    ayahText = uiState.ayahText,
                            ayahRef = uiState.ayahRef,
                            darkTheme = darkTheme,
                            showQuickActions = showSlabQuickActions,
                            bottomPadding = contentPadding.calculateBottomPadding(),
                            modifier = Modifier.fillParentMaxHeight()
                    )
                }
            }

            KhushuPullRefreshIndicator(
                    // Lock fill at 100% the instant threshold is reached so there's no
                    // dip during the async gap before isRefreshing becomes true.
                    progress = if (thresholdReached || uiState.isRefreshing) 1f else pullProgress,
                    isRefreshing = uiState.isRefreshing,
                    darkTheme = darkTheme,
                    modifier =
                            Modifier.align(Alignment.TopCenter)
                                    .padding(top = contentPadding.calculateTopPadding() + 14.dp)
                                    .size(52.dp)
                                    .graphicsLayer {
                                        translationY =
                                                (animatedPullOffsetPx * 0.22f) -
                                                        with(density) { 8.dp.toPx() }
                                    }
            )
        }

        KhushuAppBar(
                title = "",
                onSettingsClick = onSettingsClick,
                startContent = { HomeHijriBadge(badge = hijriBadge) },
                modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    selectedQuickAction?.let { action ->
        val title =
                when (action) {
                    HomeQuickAction.QIBLA -> "Qibla Direction"
                    HomeQuickAction.MOSQUES -> "Mosque Directory"
                    HomeQuickAction.EVENTS -> "Events"
                }
        val message =
                when (action) {
                    HomeQuickAction.QIBLA ->
                            "A focused Qibla helper is planned for Khushu. For this release, the action is here so the utility row is no longer a dead tap."
                    HomeQuickAction.MOSQUES ->
                            "Mosque discovery will arrive in a later release. The Home utility row now keeps the affordance visible without pretending the directory already exists."
                    HomeQuickAction.EVENTS ->
                            "Upcoming events are hidden on Home right now. Re-enable them in Prayer settings to jump back here directly."
                }
        AlertDialog(
                onDismissRequest = { selectedQuickAction = null },
                title = { Text(title) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { selectedQuickAction = null }) { Text("Close") }
                }
        )
    }

    if (showTimeOverrideDialog) {
        AlertDialog(
                onDismissRequest = { showTimeOverrideDialog = false },
                title = { Text("Preview Prayer Time") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                                "Set a temporary 24-hour clock time to test next prayer and makruh transitions."
                        )
                        OutlinedTextField(
                                value = previewHourText,
                                onValueChange = {
                                    previewHourText = it.filter(Char::isDigit).take(2)
                                },
                                label = { Text("Hour (0-23)") }
                        )
                        OutlinedTextField(
                                value = previewMinuteText,
                                onValueChange = {
                                    previewMinuteText = it.filter(Char::isDigit).take(2)
                                },
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
                    ) { Text("Apply") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                                onClick = {
                                    viewModel.clearPreviewTime()
                                    showTimeOverrideDialog = false
                                }
                        ) { Text("Reset") }
                        TextButton(onClick = { showTimeOverrideDialog = false }) { Text("Close") }
                    }
                }
        )
    }
}
