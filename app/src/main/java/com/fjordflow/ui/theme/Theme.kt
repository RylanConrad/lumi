package com.fjordflow.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary          = FjordBlue,
    onPrimary        = ParchmentWhite,
    primaryContainer = MistGray,
    onPrimaryContainer = FjordBlueDark,
    secondary        = GlacierTeal,
    onSecondary      = ParchmentWhite,
    tertiary         = PineGreen,
    background       = ParchmentWhite,
    onBackground     = FjordBlueDark,
    surface          = ParchmentSurface,
    onSurface        = FjordBlueDark,
    surfaceVariant   = MistGray,
    onSurfaceVariant = StoneGray,
    error            = CoralRed,
    outline          = MistGray
)

private val DarkColorScheme = darkColorScheme(
    primary          = FjordBlueDarkTheme,
    onPrimary        = BackgroundDark,
    primaryContainer = FjordBlueDark,
    secondary        = GlacierTeal,
    background       = BackgroundDark,
    onBackground     = ParchmentWhite,
    surface          = SurfaceDark,
    onSurface        = ParchmentWhite,
    error            = CoralRed
)

@Composable
fun FjordFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = FjordTypography,
        content     = content
    )
}
