package com.kaizen.khushu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.kaizen.khushu.data.TasbeehDatabase
import com.kaizen.khushu.ui.components.KhushuAppBar
import com.kaizen.khushu.ui.components.PillNavBar
import com.kaizen.khushu.ui.navigation.AppDestinations
import com.kaizen.khushu.ui.screens.salah.SalahImmersiveScreen
import com.kaizen.khushu.ui.screens.salah.SalahPickerScreen
import com.kaizen.khushu.ui.screens.salah.SalahPreset
import com.kaizen.khushu.ui.screens.settings.SettingsSheet
import com.kaizen.khushu.ui.screens.tasbeeh.CreateCollectionSheet
import com.kaizen.khushu.ui.screens.tasbeeh.TasbeehScreen
import com.kaizen.khushu.ui.screens.tasbeeh.TasbeehViewModel
import com.kaizen.khushu.ui.theme.KhushuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KhushuTheme {
                KhushuApp()
            }
        }
    }
}

@Composable
private fun KhushuApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.SALAH) }
    var immersiveRakats by rememberSaveable { mutableStateOf<Int?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }

    val context = LocalContext.current
    val dao = remember {
        TasbeehDatabase.getInstance(context.applicationContext).tasbeehDao()
    }
    val tasbeehViewModel: TasbeehViewModel = viewModel(factory = TasbeehViewModel.factory(dao))

    val density = LocalDensity.current
    val navBarBottomDp = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val pillClearance = navBarBottomDp + 30.dp + 56.dp

    // Height of the gradient scrim that hides content scrolling behind the transparent app bar.
    // Covers the status bar (dynamic) plus the app bar row (88dp, matching contentPadding.top).
    val statusBarHeightDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val appBarScrimHeight = statusBarHeightDp + 88.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState),
        ) {
            when (currentDestination) {
                AppDestinations.SALAH -> SalahPickerScreen(
                    onStartPrayer = { immersiveRakats = it },
                    navBarClearance = pillClearance,
                )
                AppDestinations.TASBEEH -> TasbeehScreen(
                    viewModel = tasbeehViewModel,
                    onCollectionTap = { /* TODO: immersive counter Phase 4 */ },
                    onCreateClick = { showCreateSheet = true },
                    contentPadding = PaddingValues(top = 88.dp, bottom = pillClearance),
                )
                AppDestinations.LEARN -> LearnScreen(
                    contentPadding = PaddingValues(bottom = pillClearance),
                )
            }
        }

        // Gradient scrim: fades scrolled content to black before it reaches the app bar.
        // Height = status bar + app bar row — computed from live WindowInsets, not hardcoded.
        // The gradient is invisible when no content is scrolled behind it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black, Color.Transparent),
                        startY = 0f,
                        endY = with(density) { appBarScrimHeight.toPx() },
                    )
                )
                .statusBarsPadding()
                .padding(bottom = 88.dp), // reserve the app bar row height
        )

        KhushuAppBar(
            title = currentDestination.label,
            onSettingsClick = { showSettingsSheet = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding(),
        )

        PillNavBar(
            currentDestination = currentDestination,
            onDestinationSelected = { currentDestination = it },
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 30.dp),
        )

        immersiveRakats?.let { rakats ->
            SalahImmersiveScreen(
                targetRakats = rakats,
                preset = SalahPreset.Minimal,
                onComplete = { immersiveRakats = null },
                onExit = { immersiveRakats = null },
            )
        }
    }

    if (showCreateSheet) {
        CreateCollectionSheet(
            viewModel = tasbeehViewModel,
            onDismiss = { showCreateSheet = false },
        )
    }

    if (showSettingsSheet) {
        SettingsSheet(onDismiss = { showSettingsSheet = false })
    }
}

@Composable
private fun LearnScreen(contentPadding: PaddingValues = PaddingValues()) {
    Box(modifier = Modifier.fillMaxSize())
}
