package com.kaizen.khushu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kaizen.khushu.ui.components.KhushuAppBar
import com.kaizen.khushu.ui.components.PillNavBar
import com.kaizen.khushu.ui.navigation.AppDestinations
import com.kaizen.khushu.ui.screens.salah.SalahImmersiveScreen
import com.kaizen.khushu.ui.screens.salah.SalahPickerScreen
import com.kaizen.khushu.ui.screens.salah.SalahPreset
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Screen content — full size, behind app bar and nav bar
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentDestination) {
                AppDestinations.SALAH -> SalahPickerScreen(
                    onStartPrayer = { immersiveRakats = it },
                )
                AppDestinations.TASBEEH -> TasbeehScreen()
                AppDestinations.LEARN -> LearnScreen()
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

@Composable
private fun TasbeehScreen() {
    Box(modifier = Modifier.fillMaxSize())
}

@Composable
private fun LearnScreen() {
    Box(modifier = Modifier.fillMaxSize())
}
