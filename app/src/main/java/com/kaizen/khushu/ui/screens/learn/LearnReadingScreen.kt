package com.kaizen.khushu.ui.screens.learn

import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.alpha
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaizen.khushu.data.repository.TranslationRepository
import com.kaizen.khushu.data.model.AVAILABLE_TRANSLATIONS
import com.kaizen.khushu.data.model.TranslationMeta
import com.kaizen.khushu.data.model.ContentBlock
import com.kaizen.khushu.data.model.AyahBlock
import com.kaizen.khushu.data.model.HadithBlock
import com.kaizen.khushu.data.model.HeadingBlock
import com.kaizen.khushu.data.model.ArabicBlock
import com.kaizen.khushu.data.model.LearnTopic
import com.kaizen.khushu.data.model.WordData
import com.kaizen.khushu.data.repository.UserSettings
import com.kaizen.khushu.ui.screens.settings.SettingsViewModel
import com.kaizen.khushu.ui.theme.BeVietnamPro
import com.kaizen.khushu.ui.theme.ScheherazadeNew

import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link

import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune

import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.BorderStroke

import com.kaizen.khushu.ui.components.AyahEndMarker
import com.kaizen.khushu.ui.components.toArabicIndic
import com.kaizen.khushu.ui.components.BlockActionSheet
import com.kaizen.khushu.ui.components.ActionRow
import com.kaizen.khushu.ui.components.ReadingSettingsSheet
import com.kaizen.khushu.ui.components.TranslationPickerSheet

// ── Translation helpers ────────────────────────────────────────────────────────

private val RtlLangs = setOf("ur", "ar", "fa", "he")
private fun translationDirection(lang: String) =
    if (lang in RtlLangs) TextDirection.Rtl else TextDirection.Ltr

// ── Theme helpers ──────────────────────────────────────────────────────────────

private val ThemeDark = Color.Black
private val ThemePaper = Color(0xFFF5E6C8)
private val ThemeLight = Color.White

@Composable
private fun bgColor(theme: String) = when (theme) {
    "PAPER" -> ThemePaper
    "LIGHT" -> ThemeLight
    else -> MaterialTheme.colorScheme.background
}

private fun contentColor(theme: String) = when (theme) {
    "PAPER", "LIGHT" -> Color.Black
    else -> Color.White
}

@Composable
private fun readingColorScheme(readingTheme: String, dynamicColor: Boolean): ColorScheme {
    val context = LocalContext.current
    return when {
        readingTheme == "DARK" && dynamicColor -> dynamicDarkColorScheme(context)
        readingTheme == "DARK"                 -> darkColorScheme()
        dynamicColor                           -> dynamicLightColorScheme(context)
        else                                   -> lightColorScheme()
    }
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LearnReadingScreen(
    topic: LearnTopic,
    settingsViewModel: SettingsViewModel,
    learnAudioViewModel: LearnAudioViewModel,
    learnReadingViewModel: LearnReadingViewModel = viewModel(),
    onBack: () -> Unit,
    initialAyahIndex: Int? = null,
    modifier: Modifier = Modifier,
) {
    val settings by settingsViewModel.settings.collectAsState()
    val scheme = readingColorScheme(settings.readingTheme, settings.dynamicColor)

    MaterialTheme(colorScheme = scheme) {
        val audioState by learnAudioViewModel.audioState.collectAsState()
        val blocks by learnReadingViewModel.blocks.collectAsState()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        var showSettings by remember { mutableStateOf(false) }
        var showActionSheet by remember { mutableStateOf(false) }
        var showTranslationPicker by remember { mutableStateOf(false) }
        
        val translationMap by learnReadingViewModel.translationMap
        val tajweedMap by learnReadingViewModel.tajweedMap
        val context = androidx.compose.ui.platform.LocalContext.current

        // Load initial translation
        androidx.compose.runtime.LaunchedEffect(settings.selectedTranslationLang) {
            learnReadingViewModel.loadTranslation(context, settings.selectedTranslationLang)
        }
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

        val bg = bgColor(settings.readingTheme)
        val fg = contentColor(settings.readingTheme)

        val titleFraction = scrollBehavior.state.collapsedFraction
        val titleFontSize = androidx.compose.ui.util.lerp(28f, 20f, titleFraction).sp

        // Kick off block loading for this topic
        androidx.compose.runtime.LaunchedEffect(topic.id) {
            learnReadingViewModel.loadBlocks(topic.id)
        }

        // Track last read topic
        androidx.compose.runtime.LaunchedEffect(topic.id) {
            settingsViewModel.updateLastReadTopicId(topic.id)
        }

        // Scroll to initial index if provided
        androidx.compose.runtime.LaunchedEffect(initialAyahIndex, blocks) {
            if (initialAyahIndex != null && blocks != null) {
                val offset = if (blocks!!.isNotEmpty()) 1 else 0
                listState.animateScrollToItem(initialAyahIndex + offset)
            }
        }

        // WakeLock — hold screen on while reading if pref is set
        val activity = LocalActivity.current
        DisposableEffect(settings.readingKeepScreenOn) {
            if (settings.readingKeepScreenOn) {
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        // Status bar icon colour
        val isDarkReading = settings.readingTheme == "DARK"
        val isSystemDark = isSystemInDarkTheme()
        val appDarkTheme = when (settings.themeMode) {
            "Light" -> false
            "Dark"  -> true
            else    -> isSystemDark
        }
        DisposableEffect(isDarkReading, appDarkTheme) {
            val window = activity?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !isDarkReading
            }
            onDispose {
                if (window != null) {
                    WindowCompat.getInsetsController(window, window.decorView)
                        .isAppearanceLightStatusBars = !appDarkTheme
                }
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            modifier = modifier
                .clip(RoundedCornerShape(28.dp))
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = bg,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = topic.title,
                            fontFamily = BeVietnamPro,
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Normal,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = fg,
                            )
                        }
                    },
                    actions = {
                        if (topic.audioFilename != null) {
                            IconButton(onClick = {
                                when (audioState) {
                                    is LearnAudioViewModel.AudioState.Playing -> learnAudioViewModel.pause()
                                    is LearnAudioViewModel.AudioState.Loading -> { /* do nothing */ }
                                    else -> learnAudioViewModel.play(topic)
                                }
                            }) {
                                when (audioState) {
                                    is LearnAudioViewModel.AudioState.Playing -> {
                                        Icon(
                                            Icons.Default.Pause,
                                            contentDescription = "Pause",
                                            tint = fg.copy(alpha = 0.7f),
                                        )
                                    }
                                    is LearnAudioViewModel.AudioState.Loading -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = fg.copy(alpha = 0.7f),
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = fg.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Reading settings",
                                tint = fg.copy(alpha = 0.7f),
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = bg,
                        scrolledContainerColor = bg,
                        titleContentColor = fg,
                        navigationIconContentColor = fg,
                    ),
                )
            },
        ) { paddingValues ->
            var activeBlock by remember { mutableStateOf<Pair<ContentBlock, Int>?>(null) }

            LazyColumn(
                state = listState,
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize(),
            ) {
                val resolvedBlocks = blocks
                if (resolvedBlocks != null && resolvedBlocks.isNotEmpty()) {
                    // Block-based rendering from JSON assets
                    item { Spacer(Modifier.height(8.dp)) }
                    itemsIndexed(resolvedBlocks!!) { index, block ->
                        BlockRenderer(
                            block = block,
                            settings = settings,
                            fg = fg,
                            bg = bg,
                            translationMap = translationMap,
                            tajweedMap = tajweedMap,
                            onBlockClick = { activeBlock = it to index },
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                } else {
                    // Legacy rendering (topics without JSON or while loading)
                    item {
                        Spacer(Modifier.height(8.dp))
                        AyatBlock(
                            topic = topic,
                            settings = settings,
                            fg = fg,
                            bg = bg,
                            onTap = { showActionSheet = true }
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            if (showTranslationPicker) {
                TranslationPickerSheet(
                    selectedId = settings.selectedTranslationLang,
                    isDownloading = learnReadingViewModel.isDownloading.value,
                    progress = learnReadingViewModel.downloadProgress.floatValue,
                    onSelect = { meta ->
                        if (TranslationRepository.isDownloaded(context, meta.id)) {
                            settingsViewModel.setSelectedTranslationLang(meta.id)
                            showTranslationPicker = false
                        } else {
                            learnReadingViewModel.downloadTranslation(context, meta) {
                                settingsViewModel.setSelectedTranslationLang(meta.id)
                                showTranslationPicker = false
                            }
                        }
                    },
                    onDismiss = { showTranslationPicker = false }
                )
            }

            if (activeBlock != null) {
                val (block, index) = activeBlock!!
                val isBookmarked = settings.bookmarkedAyahs.contains("${topic.id}:$index")
                
                BlockActionSheet(
                    block = block,
                    topicId = topic.id,
                    settings = settings,
                    isBookmarked = isBookmarked,
                    onDismiss = { activeBlock = null },
                    onBookmark = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        settingsViewModel.toggleBookmark(topic.id, index)
                        val msg = if (isBookmarked) "Bookmark removed" else "Bookmark added"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        activeBlock = null
                    }
                )
            }

            if (showActionSheet) {
                val isBookmarked = settings.bookmarkedAyahs.contains("${topic.id}:0")
                val isMastered = settings.masteredTopicIds.contains(topic.id)

                AyahActionSheet(
                    onDismiss = { showActionSheet = false },
                    onPlay = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        learnAudioViewModel.play(topic)
                        showActionSheet = false
                    },
                    onBookmark = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        settingsViewModel.toggleBookmark(topic.id, 0)
                        val msg = if (isBookmarked) "Bookmark removed" else "Bookmark added"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        showActionSheet = false
                    },
                    onMastered = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        settingsViewModel.toggleMastered(topic.id)
                        val msg = if (isMastered) "Marked as incomplete" else "Marked as mastered"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        showActionSheet = false
                    },
                    onShare = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        Toast.makeText(context, "Sharing feature coming soon!", Toast.LENGTH_SHORT).show()
                        showActionSheet = false
                    },
                    isBookmarked = isBookmarked,
                    isMastered = isMastered
                )
            }

            if (showSettings) {
                ReadingSettingsSheet(
                    settings = settings,
                    translationLanguages = topic.translations.keys,
                    onDismiss = { showSettings = false },
                    onThemeChange = { settingsViewModel.setReadingTheme(it) },
                    onArabicSizeChange = { settingsViewModel.setArabicSizeSp(it) },
                    onTranslationSizeChange = { settingsViewModel.setTranslationSizeSp(it) },
                    onShowTranslationChange = { settingsViewModel.toggleShowTranslation(it) },
                    onShowTransliterationChange = { settingsViewModel.toggleShowTransliteration(it) },
                    onShowWordByWordChange = { settingsViewModel.toggleShowWordByWord(it) },
                    onKeepScreenOnChange = { settingsViewModel.toggleReadingKeepScreenOn(it) },
                    onShowTajweedChange = { settingsViewModel.toggleShowTajweed(it) },
                    onTranslationLangChange = { settingsViewModel.setSelectedTranslationLang(it) },
                    onReciterChange = { settingsViewModel.setSelectedReciterId(it) },
                    onScriptChange = { settingsViewModel.setSelectedScript(it) },
                    onOpenTranslationPicker = {
                        showSettings = false
                        showTranslationPicker = true
                    },
                    onDownloadAudio = { /* Learn topics handle their own audio */ }
                )
            }
        }
    }
}

// ── Arabic + Translation block ─────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AyatBlock(
    topic: LearnTopic,
    settings: UserSettings,
    fg: Color,
    bg: Color,
    onTap: () -> Unit,
) {
    val dividerColor = fg.copy(alpha = 0.2f)
    val arabicBg = when (settings.readingTheme) {
        "PAPER" -> Color(0xFFEDD9A3)
        "LIGHT" -> Color(0xFFF0F0F0)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        val marker = "۝"
        if (topic.arabicText.contains(marker) && topic.ayahTranslations.isNotEmpty()) {
            val segments = topic.arabicText.split(marker).filter { it.isNotBlank() }
            val tajweedSegments = if (settings.showTajweed && topic.tajweedMarkup != null) {
                topic.tajweedMarkup.split(marker).filter { it.isNotBlank() }
            } else null

            // Extract surah number for the "Ayah S:A" format
            val surahNumber = topic.referenceNumber?.split(":")?.getOrNull(0) ?: ""

            segments.forEachIndexed { index, segment ->
                // 1. Arabic text card (Elevated)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp), // Full-bleed
                    color = arabicBg,
                    onClick = onTap
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Per-Ayah Reference at top right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            ReferenceBadge(
                                source = null,
                                number = if (surahNumber.isNotEmpty()) "Ayah $surahNumber:${index + 1}" else "Ayah ${index + 1}",
                                fg = fg,
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                if (tajweedSegments != null && index < tajweedSegments.size) {
                                    TajweedText(
                                        markup = tajweedSegments[index].trim(),
                                        fontSize = settings.arabicSizeSp.sp,
                                        lineHeight = (settings.arabicSizeSp * 1.75f).sp,
                                        color = fg,
                                    )
                                } else {
                                    Text(
                                        text = segment.trim(),
                                        fontSize = settings.arabicSizeSp.sp,
                                        lineHeight = (settings.arabicSizeSp * 1.75f).sp,
                                        fontFamily = ScheherazadeNew,
                                        fontWeight = FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        color = fg,
                                        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl, fontFamily = ScheherazadeNew),
                                    )
                                }
                                AyahEndMarker(number = index + 1, fg = fg)
                            }
                        }
                    }
                }

                if (settings.showTranslation) {
                    val ayahTranslation = topic.ayahTranslations[settings.selectedTranslationLang]?.getOrNull(index)
                        ?: topic.ayahTranslations["en"]?.getOrNull(index)
                    if (ayahTranslation != null) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = ayahTranslation,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = BeVietnamPro,
                                fontSize = settings.translationSizeSp.sp,
                                lineHeight = (settings.translationSizeSp * 1.6f).sp,
                                textDirection = translationDirection(settings.selectedTranslationLang),
                            ),
                            color = fg.copy(alpha = 0.85f),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 18.dp).clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onTap
                            )
                        )
                    }
                }

                if (index < segments.size - 1) {
//                    HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp), color = fg.copy(alpha = 0.05f), thickness = 1.2.dp)
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp), // Full-bleed
                color = arabicBg,
                onClick = onTap
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (topic.arabicText.contains(marker)) {
                        val segments = topic.arabicText.split(marker).filter { it.isNotBlank() }
                        val tajweedSegments = if (settings.showTajweed && topic.tajweedMarkup != null) {
                            topic.tajweedMarkup.split(marker).filter { it.isNotBlank() }
                        } else null
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalArrangement = Arrangement.Center) {
                                segments.forEachIndexed { index, segment ->
                                    if (tajweedSegments != null && index < tajweedSegments.size) {
                                        TajweedText(markup = tajweedSegments[index].trim(), fontSize = settings.arabicSizeSp.sp, lineHeight = (settings.arabicSizeSp * 1.75f).sp, color = fg)
                                    } else {
                                        Text(
                                            text = segment.trim(), fontSize = settings.arabicSizeSp.sp, lineHeight = (settings.arabicSizeSp * 1.75f).sp,
                                            fontFamily = ScheherazadeNew, fontWeight = FontWeight.Normal, textAlign = TextAlign.Start, color = fg,
                                            style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl, fontFamily = ScheherazadeNew),
                                        )
                                    }
                                    AyahEndMarker(number = index + 1, fg = fg)
                                }
                            }
                        }
                    } else {
                        if (settings.showTajweed && topic.tajweedMarkup != null) {
                            TajweedText(markup = topic.tajweedMarkup, fontSize = settings.arabicSizeSp.sp, lineHeight = (settings.arabicSizeSp * 1.75f).sp, color = fg, modifier = Modifier.fillMaxWidth())
                        } else {
                            Text(
                                text = topic.arabicText, fontSize = settings.arabicSizeSp.sp, lineHeight = (settings.arabicSizeSp * 1.75f).sp,
                                fontFamily = ScheherazadeNew, fontWeight = FontWeight.Normal, textAlign = TextAlign.Start, color = fg,
                                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl, fontFamily = ScheherazadeNew),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            if (topic.ayahTranslations.isEmpty()) {
                val translation = topic.translations[settings.selectedTranslationLang] ?: topic.translations["en"]
                if (settings.showTranslation && translation != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = BeVietnamPro, fontSize = settings.translationSizeSp.sp, lineHeight = (settings.translationSizeSp * 1.75f).sp, textDirection = translationDirection(settings.selectedTranslationLang)),
                        color = fg.copy(alpha = 0.8f), textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        if (settings.showWordByWord && topic.words.isNotEmpty()) {
            Box(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap)) {
                WordRow(words = topic.words, fg = fg, bg = bg, showTransliteration = settings.showTransliteration)
            }
            Spacer(Modifier.height(16.dp))
        }

        val translit = topic.transliteration["en_latin"]
        if (settings.showTransliteration && translit != null) {
            Text(
                text = translit, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = BeVietnamPro),
                color = fg.copy(alpha = 0.5f), fontWeight = FontWeight.Normal, modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            )
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), color = fg.copy(alpha = 0.05f), thickness = 1.2.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AyahActionSheet(
    onDismiss: () -> Unit, onPlay: () -> Unit, onBookmark: () -> Unit, onMastered: () -> Unit, onShare: () -> Unit,
    isBookmarked: Boolean, isMastered: Boolean
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ayah Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            ActionRow(icon = Icons.Default.PlayArrow, label = "Play Audio", onClick = onPlay)
            ActionRow(icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, label = if (isBookmarked) "Remove Bookmark" else "Add Bookmark", onClick = onBookmark)
            ActionRow(icon = if (isMastered) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, label = if (isMastered) "Mark as Incomplete" else "Mark as Mastered", onClick = onMastered)
            ActionRow(icon = Icons.Default.Share, label = "Share", onClick = onShare)
        }
    }
}

@Composable
private fun WordRow(words: List<WordData>, fg: Color, bg: Color, showTransliteration: Boolean) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(words, key = { it.id }) { word -> WordChip(word = word, fg = fg, bg = bg, showTransliteration = showTransliteration) }
    }
}

@Composable
private fun WordChip(word: WordData, fg: Color, bg: Color, showTransliteration: Boolean) {
    val chipBg = fg.copy(alpha = 0.06f)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(chipBg).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(text = word.arabic, fontSize = 20.sp, fontFamily = ScheherazadeNew, color = fg, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Rtl, fontFamily = ScheherazadeNew))
        Spacer(Modifier.height(4.dp))
        Text(text = word.translation, fontSize = 11.sp, fontFamily = BeVietnamPro, color = fg.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        if (showTransliteration && word.transliteration != null) {
            Text(text = word.transliteration, fontSize = 10.sp, fontFamily = BeVietnamPro, color = fg.copy(alpha = 0.4f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ReferenceBadge(source: String?, number: String?, fg: Color, modifier: Modifier = Modifier) {
    val formattedLabel = remember(source, number) {
        if (source?.lowercase() == "quran" && number != null) {
            val parts = number.split(":")
            val surahPart = parts.getOrNull(0)
            val ayahPart = parts.getOrNull(1)
            val surahName = when (surahPart) {
                "1" -> "Al-Fatihah"; "2" -> "Al-Baqarah"; "49" -> "Al-Hujurat"; "112" -> "Al-Ikhlas"
                else -> surahPart?.let { "Surah $it" }
            }
            if (ayahPart != null) "$surahName, Ayah $ayahPart" else surahName ?: source
        } else listOfNotNull(source, number).joinToString(" · ")
    }
    if (formattedLabel.isBlank()) return
    Surface(shape = RoundedCornerShape(50), color = fg.copy(alpha = 0.08f), modifier = modifier) {
        Text(text = formattedLabel, style = MaterialTheme.typography.labelMedium.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.Medium, fontSize = 12.sp), color = fg.copy(alpha = 0.5f), textAlign = TextAlign.End, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
