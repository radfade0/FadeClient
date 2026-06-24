package com.retrivedmods.wclient.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Define premium dark red color scheme
private val PremiumDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF0000), // Bold premium red
    onPrimary = Color.Black,
    secondary = Color(0xFFB71C1C), // Darker red
    onSecondary = Color.White,
    background = Color(0xFF121212), // Dark background
    onBackground = Color.White,
    surface = Color(0xFF1A1A1A), // Slightly lighter than background
    onSurface = Color.White,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

// Define premium light red color scheme (if needed)
private val PremiumLightColorScheme = lightColorScheme(
    primary = Color(0xFFFF0000), // Bold red
    onPrimary = Color.White,
    secondary = Color(0xFFB71C1C), // Darker red
    onSecondary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color.Black,
    surface = Color(0xFFFFEBEE), // Soft red tint for a premium feel
    onSurface = Color.Black,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun MuCuteClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> PremiumDarkColorScheme
        else -> PremiumLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
