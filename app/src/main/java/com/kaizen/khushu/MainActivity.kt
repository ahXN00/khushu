package com.kaizen.khushu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.compose.runtime.remember
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.kaizen.khushu.ui.components.KhushuAppBar
import com.kaizen.khushu.ui.components.PillNavBar
import com.kaizen.khushu.ui.navigation.AppDestinations
import com.kaizen.khushu.ui.screens.salah.SalahImmersiveScreen
import com.kaizen.khushu.ui.screens.salah.SalahPickerScreen
import com.kaizen.khushu.ui.screens.salah.SalahPreset
import com.kaizen.khushu.ui.theme.KhushuTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars

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
    val hazeState = remember { HazeState() }

    // Pill height (14dp vertical padding × 2 + ~28dp content) + 30dp gap above nav bar
    // Used to block Salah picker from sliding under the nav bar.
    val navBarBottomInset = WindowInsets.navigationBars
    val density = LocalDensity.current
    val navBarBottomDp = with(density) { navBarBottomInset.getBottom(density).toDp() }
    val pillClearance = navBarBottomDp + 30.dp + 56.dp // nav inset + gap + pill height

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Screen content — haze source: full size, behind app bar and nav bar
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
                    contentPadding = PaddingValues(bottom = pillClearance),
                )
                AppDestinations.LEARN -> LearnScreen(
                    contentPadding = PaddingValues(bottom = pillClearance),
                )
            }
        }

        // App bar — transparent, floats on top, respects status bar once
        KhushuAppBar(
            title = currentDestination.label,
            onSettingsClick = { /* TODO: show bottom sheet */ },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding(),
        )

        // Floating pill nav bar — 30dp above navigation bar
        PillNavBar(
            currentDestination = currentDestination,
            onDestinationSelected = { currentDestination = it },
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 30.dp),
        )

        // Immersive counter — full-screen overlay, covers app bar + nav bar
        immersiveRakats?.let { rakats ->
            SalahImmersiveScreen(
                targetRakats = rakats,
                preset = SalahPreset.Minimal,
                onComplete = { immersiveRakats = null },
                onExit = { immersiveRakats = null },
            )
        }
    }
}

// --- Placeholder screens ---
// contentPadding is passed so future LazyColumns can bleed under the nav bar and get blurred.

@Composable
private fun TasbeehScreen(contentPadding: PaddingValues = PaddingValues()) {
    Box(modifier = Modifier.fillMaxSize())
}

@Composable
private fun LearnScreen(contentPadding: PaddingValues = PaddingValues()) {
    Box(modifier = Modifier.fillMaxSize())
}
