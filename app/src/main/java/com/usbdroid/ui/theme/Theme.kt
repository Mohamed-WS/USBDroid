package com.usbdroid.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

// Dark industrial / hacker tool aesthetic colors
val Background = Color(0xFF0A0A0F)
val Surface = Color(0xFF111118)
val SurfaceVariant = Color(0xFF1A1A24)
val SurfaceElevated = Color(0xFF222230)
val PrimaryCyan = Color(0xFF00E5FF)
val PrimaryCyanDark = Color(0xFF00B8CC)
val PrimaryCyanLight = Color(0xFF33EBFF)
val SecondaryPurple = Color(0xFFB388FF)
val WarningOrange = Color(0xFFFF6B00)
val ErrorRed = Color(0xFFFF2D55)
val SuccessGreen = Color(0xFF00FF88)
val OnBackground = Color(0xFFE0E0E5)
val OnSurface = Color(0xFFC0C0CC)
val OnSurfaceVariant = Color(0xFF888899)
val Divider = Color(0xFF2A2A35)
val TerminalBackground = Color(0xFF0C0C14)
val TerminalGreen = Color(0xFF00FF41)
val TerminalText = Color(0xFF33FF33)
val Border = Color(0xFF2A2A38)
val BorderFocused = Color(0xFF00E5FF)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = Background,
    primaryContainer = PrimaryCyanDark,
    onPrimaryContainer = PrimaryCyanLight,
    secondary = SecondaryPurple,
    onSecondary = Background,
    secondaryContainer = Color(0xFF2A1F3D),
    onSecondaryContainer = SecondaryPurple,
    tertiary = WarningOrange,
    onTertiary = Background,
    tertiaryContainer = Color(0xFF3D2200),
    onTertiaryContainer = WarningOrange,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = PrimaryCyan,
    inverseSurface = OnBackground,
    inverseOnSurface = Background,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF3D0010),
    onErrorContainer = ErrorRed,
    outline = Border,
    outlineVariant = Divider,
    scrim = Color.Black.copy(alpha = 0.6f)
)

@Composable
fun USBDroidTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Background.toArgb()
        window.navigationBarColor = Background.toArgb()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = USBDroidTypography,
        content = content
    )
}

object USBDroidColors {
    val background @Composable get() = MaterialTheme.colorScheme.background
    val surface @Composable get() = MaterialTheme.colorScheme.surface
    val surfaceVariant @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val primary @Composable get() = MaterialTheme.colorScheme.primary
    val error @Composable get() = MaterialTheme.colorScheme.error
    val success = SuccessGreen
    val warning = WarningOrange
    val terminalBackground = TerminalBackground
    val terminalGreen = TerminalGreen
    val border @Composable get() = MaterialTheme.colorScheme.outline
    val onBackground @Composable get() = MaterialTheme.colorScheme.onBackground
    val onSurface @Composable get() = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
}
