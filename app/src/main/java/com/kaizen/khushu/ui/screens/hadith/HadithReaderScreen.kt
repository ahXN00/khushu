package com.kaizen.khushu.ui.screens.hadith

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaizen.khushu.ui.components.ReadingSettingsSheet
import com.kaizen.khushu.ui.components.TranslationPickerSheet
import com.kaizen.khushu.ui.screens.learn.BlockRenderer
import com.kaizen.khushu.ui.screens.settings.SettingsViewModel
import com.kaizen.khushu.ui.theme.BeVietnamPro

// ── Theme helpers ──────────────────────────────────────────────────────────────

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithReaderScreen(
    bookId: String,
    sectionNumber: Int,
    sectionTitle: String,
    bookName: String,
    onBack: () -> Unit,
    viewModel: HadithViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val hadiths by viewModel.currentHadiths
    val isLoading by viewModel.isLoading
    val settings by settingsViewModel.settings.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    
    val scheme = readingColorScheme(settings.readingTheme, settings.dynamicColor)
    var showSettings by remember { mutableStateOf(false) }
    var showTranslationPicker by remember { mutableStateOf(false) }

    LaunchedEffect(bookId, sectionNumber) {
        viewModel.loadHadiths(bookId, sectionNumber, bookName)
    }

    MaterialTheme(colorScheme = scheme) {
        val bg = bgColor(settings.readingTheme)
        val fg = contentColor(settings.readingTheme)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    LargeTopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = sectionTitle,
                                    fontFamily = BeVietnamPro,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = fg,
                                    maxLines = 1
                                )
                                Text(
                                    text = bookName,
                                    fontFamily = BeVietnamPro,
                                    fontSize = 14.sp,
                                    color = fg.copy(alpha = 0.6f)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = fg
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = fg.copy(alpha = 0.7f)
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = bg.copy(alpha = 0.9f),
                            titleContentColor = fg
                        )
                    )
                }
            ) { paddingValues ->
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyColumn(
                        contentPadding = paddingValues,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(hadiths) { block ->
                            BlockRenderer(
                                block = block,
                                settings = settings,
                                fg = fg,
                                bg = bg,
                                onBlockClick = { /* Actions can be added later */ },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        item { Spacer(Modifier.height(100.dp)) }
                    }
                }
            }

            if (showSettings) {
                ReadingSettingsSheet(
                    settings = settings,
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
                    onDownloadAudio = { /* Hadith audio not yet implemented */ }
                )
            }

            if (showTranslationPicker) {
                TranslationPickerSheet(
                    selectedId = settings.selectedTranslationLang,
                    isDownloading = false, // TODO: Implement if needed
                    progress = 0f,
                    onSelect = { meta ->
                        settingsViewModel.setSelectedTranslationLang(meta.id)
                        showTranslationPicker = false
                    },
                    onDismiss = { showTranslationPicker = false }
                )
            }
        }
    }
}
