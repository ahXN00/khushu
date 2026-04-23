package com.kaizen.khushu.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamicColorScheme
import com.materialkolor.PaletteStyle

// Map for O(1) lookups
val colorSeeds = mapOf(
    "default" to Color(0xFF424746), // Neutral Charcoal for Default/System fallback
    "teal"    to Color(0xFF004D40),
    "green"   to Color(0xFF1B5E20),
    "amber"   to Color(0xFF795548),
    "red"     to Color(0xFFB71C1C),
    "blue"    to Color(0xFF0D47A1),
    "slate"   to Color(0xFF263238),
)

// The fallback for Android 11 or when dynamic is off
val DefaultSeedColor = Color(0xFF424746)

@Composable
fun KhushuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    pureBlack: Boolean = false,
    colorSeed: String = "default",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    var colorScheme = when {
        // If Dynamic Color is ON, wallpaper always wins (on supported devices)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            // Use selected seed with TonalSpot for accurate color matching
            val seedColor = colorSeeds[colorSeed] ?: DefaultSeedColor
            dynamicColorScheme(
                seedColor = seedColor,
                isDark = darkTheme,
                isAmoled = pureBlack && darkTheme,
                style = PaletteStyle.TonalSpot // Standard M3 feel, less color shifting
            )
        }
    }

    // Manual overrides for true OLED black
    if (darkTheme && pureBlack) {
        colorScheme = colorScheme.copy(
            surface = Color.Black,
            background = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerLow = Color(0xFF080808),
            surfaceContainerLowest = Color.Black,
            surfaceContainerHigh = Color(0xFF121212),
            surfaceContainerHighest = Color(0xFF1A1212),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
