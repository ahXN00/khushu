package com.kaizen.khushu.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbeehCustomizeScreen(
    viewModel: SettingsViewModel,
    onPreview: () -> Unit,
    onCustomizeBeads: () -> Unit,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { SettingsTopBarTitle("Tasbih Visuals", scrollBehavior) },
                navigationIcon = { SettingsBackButton(onBack) },
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            SectionHeader("Preview")
            MenuSectionItem(
                title = "Open Tasbeeh Screen Editor",
                detail = "Customize your tasbih counter layout",
                onClick = onPreview
            )
            MenuSectionItem(
                title = "Bead Style",
                detail = "Choose Classic Amber or Dark Onyx",
                onClick = onCustomizeBeads
            )

            SectionHeader("Visual Effects")
            SettingsToggle(
                title = "Dynamic Colors",
                subtitle = "Apply collection color automatically based on theme",
                checked = settings.tasbeehDynamicColors,
                onCheckedChange = { viewModel.toggleTasbeehDynamicColors(it) }
            )

            SectionHeader("Interaction")
            SettingsToggle(
                title = "Stealth Mode",
                subtitle = "Tap screen to hide all widgets for private use",
                checked = settings.tasbeehStealthModeAllowed,
                onCheckedChange = { viewModel.toggleTasbeehStealthModeAllowed(it) }
            )
            SettingsToggle(
                title = "Volume Buttons",
                subtitle = "Use physical volume keys to count",
                checked = settings.tasbeehVolumeEnabled,
                onCheckedChange = { viewModel.toggleTasbeehVolumeEnabled(it) }
            )
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MenuSectionItem(title: String, detail: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
