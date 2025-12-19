package com.example.univent.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

// === DARK MODE COLORS ===
private val DarkColorScheme = darkColorScheme(
    primary = NavyBlue,
    onPrimary = White,

    secondary = RedPrimary,
    onSecondary = White,

    tertiary = YellowAccent,
    onTertiary = Black,

    background = Color(0xFF121212),
    onBackground = White,

    surface = Color(0xFF1E1E1E),
    onSurface = White
)

// === LIGHT MODE COLORS ===
private val LightColorScheme = lightColorScheme(
    primary = NavyBlue,
    onPrimary = White,

    secondary = RedPrimary,
    onSecondary = White,

    tertiary = YellowAccent,
    onTertiary = Black,

    background = White,
    onBackground = Black,

    surface = LightGray,
    onSurface = Black
)

@Composable
fun UniVentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // ❗ Disable for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme)
                androidx.compose.material3.dynamicDarkColorScheme(context)
            else
                androidx.compose.material3.dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
