package com.kaizen.khushu.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.kaizen.khushu.ui.theme.BeVietnamPro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalahCustomizeScreen(
    viewModel: SettingsViewModel,
    onCustomizeLayout: (Int) -> Unit,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { SettingsTopBarTitle("Salah Visuals", scrollBehavior) },
                navigationIcon = { SettingsBackButton(onBack) },
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
            SectionHeader("Interface")
            SettingsToggle(
                title = "Show Exit Button",
                subtitle = "Keep exit button visible during counting",
                checked = settings.showExitButton,
                onCheckedChange = { viewModel.updateShowExitButton(it) }
            )
            SettingsToggle(
                title = "Show Completion Text",
                subtitle = "Display a custom message when the target is reached",
                checked = settings.showCompletionText,
                onCheckedChange = { viewModel.updateShowCompletionText(it) }
            )

            AnimatedVisibility(visible = settings.showCompletionText) {
                OutlinedTextField(
                    value = if (settings.completionText == "الحمد لله") "" else settings.completionText,
                    onValueChange = { text ->
                        viewModel.updateCompletionText(text.ifBlank { "الحمد لله" })
                    },
                    label = { Text("Completion Message") },
                    placeholder = {
                        Text("الحمد لله", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    singleLine = true
                )
            }

            SettingsToggle(
                title = "Show Step Timer",
                subtitle = "Display elapsed time for each prayer step",
                checked = settings.showStepTimer,
                onCheckedChange = { viewModel.toggleShowStepTimer(it) }
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader("Layout Editor")
            Spacer(Modifier.height(12.dp))

            OutlinedCard(
                onClick = { onCustomizeLayout(4) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Open Canvas Editor",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = BeVietnamPro
                        )
                        Text(
                            "Customize the 'Custom' preset layout",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
