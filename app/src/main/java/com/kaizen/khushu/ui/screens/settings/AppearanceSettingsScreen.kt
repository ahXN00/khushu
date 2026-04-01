package com.kaizen.khushu.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.kaizen.khushu.ui.theme.BeVietnamPro
import com.kaizen.khushu.ui.theme.LocalThemeTransitionController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val transitionController = LocalThemeTransitionController.current

    // Passive coordinate tracking for indestructible transition origins
    val btnCoords = remember { mutableStateListOf<LayoutCoordinates?>(null, null, null) }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(horizontal = 12.dp, vertical = 32.dp),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Appearance",
                        fontFamily = BeVietnamPro,
                        style = MaterialTheme.typography.displaySmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 17.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            
            SectionHeader("Theme")
            val themeOptions = listOf("System", "Light", "Dark")
            val themeIcons = listOf(
                Icons.Default.Settings,
                Icons.Default.WbSunny,
                Icons.Default.DarkMode
            )
            
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                themeOptions.forEachIndexed { index, label ->
                    SegmentedButton(
                        modifier = Modifier.onGloballyPositioned { btnCoords[index] = it },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                        onClick = {
                            val center = btnCoords[index]?.boundsInRoot()?.center ?: Offset.Zero
                            transitionController.captureAndChange(center) {
                                viewModel.setThemeMode(label)
                            }
                        },
                        selected = settings.themeMode == label,
                        icon = {
                            Icon(
                                imageVector = themeIcons[index],
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = BeVietnamPro,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader("System")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsToggle(
                    title = "Dynamic Color",
                    subtitle = "Sync with system theme (Material You)",
                    checked = settings.dynamicColor,
                    onCheckedChange = { viewModel.toggleDynamicColor(it) }
                )
            }
            val isDarkTheme = when (settings.themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            if (isDarkTheme) {
                SettingsToggle(
                    title = "Pure AMOLED Black",
                    subtitle = "Absolute #000000 background",
                    checked = settings.pureBlack,
                    onCheckedChange = { viewModel.togglePureBlack(it) }
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
