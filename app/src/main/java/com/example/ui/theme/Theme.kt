package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val ArcadeDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = ObsidianBg,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanNeon,
    secondary = CoralLive,
    onSecondary = Color.White,
    secondaryContainer = CoralContainer,
    onSecondaryContainer = Color(0xFFFFB3C4),
    tertiary = VoltGreen,
    onTertiary = ObsidianBg,
    tertiaryContainer = VoltGreenContainer,
    onTertiaryContainer = VoltGreen,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF1F273D),
    error = CoralLive,
    onError = Color.White
)

private val ArcadeLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDF2FA),
    onPrimaryContainer = Color(0xFF002026),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E1),
    onSecondaryContainer = Color(0xFF3F0014),
    tertiary = LightTertiary,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = Color(0xFF101522),
    surface = LightSurface,
    onSurface = Color(0xFF101522),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF45506E),
    outline = Color(0xFFC8D2E8)
)

val ArcadeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MobiliveArcadeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) ArcadeDarkColorScheme else ArcadeLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(view, darkTheme) {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
            onDispose { }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ArcadeTypography,
        shapes = ArcadeShapes,
        content = content
    )
}
