package com.example.facemark.ui.theme




import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val TealLightColorScheme = lightColorScheme(
    primary = Color(0xFF00796B), // Teal color
    onPrimary = Color.White, // Color used for text/icon on primary color
    secondary = Color(0xFF00796B), // Darker teal
    onSecondary = Color.White,
    background = Color(0xFFFFFFFF), // Background color
    onBackground = Color.Black, // Text color on background
    surface = Color(0xFFFFFFFF), // Surface color (e.g., card)
    onSurface = Color.Black // Text color on surface
)

private val TealDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00796B), // Teal color
    onPrimary = Color.Black, // Text/icon color on primary color
    secondary = Color(0xFF00796B), // Darker teal
    onSecondary = Color.Black,
    background = Color(0xFF121212), // Dark background
    onBackground = Color.White, // Text color on background
    surface = Color(0xFF121212), // Dark surface color (e.g., card)
    onSurface = Color.White // Text color on surface
)


@Composable
fun AttendanceTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        TealDarkColorScheme
    } else {
        TealLightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.secondary.toArgb() // Use primary teal color for status bar
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}