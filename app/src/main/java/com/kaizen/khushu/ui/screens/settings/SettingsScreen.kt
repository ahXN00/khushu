package com.kaizen.khushu.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.kaizen.khushu.ui.theme.BeVietnamPro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateGeneral: () -> Unit,
    onNavigateCounter: () -> Unit,
    onNavigateAppearance: () -> Unit,
    onBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings", fontFamily = BeVietnamPro) },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            MenuSectionItem(
                title = "General",
                detail = "Screen, Awake & System",
                imageVector = Icons.Default.Tune,
                onClick = onNavigateGeneral
            )

            MenuSectionItem(
                title = "Counter",
                detail = "Haptics, Volume Keys & Feedback",
                imageVector = Icons.Default.TouchApp,
                onClick = onNavigateCounter
            )

            MenuSectionItem(
                title = "Appearance",
                detail = "Theme, Dynamic Color & AMOLED",
                imageVector = Icons.Default.ColorLens,
                onClick = onNavigateAppearance
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
