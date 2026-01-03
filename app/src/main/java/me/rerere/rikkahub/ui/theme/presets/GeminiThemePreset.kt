package me.rerere.rikkahub.ui.theme.presets

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import me.rerere.rikkahub.ui.theme.PresetTheme

val GeminiThemePreset by lazy {
    PresetTheme(
        id = "gemini",
        name = {
            Text("Gemini")
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

// Gemini Light Colors
private val primaryLight = Color(0xFF0B57D0)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFD3E3FD)
private val onPrimaryContainerLight = Color(0xFF041E49)
private val secondaryLight = Color(0xFF00639B)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFC2E7FF) // User Bubble
private val onSecondaryContainerLight = Color(0xFF001D35)
private val tertiaryLight = Color(0xFF6E5E00)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFFFE680)
private val onTertiaryContainerLight = Color(0xFF221B00)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF410002)
private val backgroundLight = Color(0xFFFFFFFF)
private val onBackgroundLight = Color(0xFF1F1F1F)
private val surfaceLight = Color(0xFFFFFFFF)
private val onSurfaceLight = Color(0xFF1F1F1F)
private val surfaceVariantLight = Color(0xFFE1E3E1) // Input bar background
private val onSurfaceVariantLight = Color(0xFF444746)
private val outlineLight = Color(0xFF747775)
private val outlineVariantLight = Color(0xFFC4C7C5)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF303030)
private val inverseOnSurfaceLight = Color(0xFFF2F2F2)
private val inversePrimaryLight = Color(0xFFA8C7FA)

// Gemini Dark Colors
private val primaryDark = Color(0xFFA8C7FA)
private val onPrimaryDark = Color(0xFF003355)
private val primaryContainerDark = Color(0xFF0842A0)
private val onPrimaryContainerDark = Color(0xFFD3E3FD)
private val secondaryDark = Color(0xFF7FCFFF)
private val onSecondaryDark = Color(0xFF003355)
private val secondaryContainerDark = Color(0xFF004A77) // User Bubble
private val onSecondaryContainerDark = Color(0xFFC2E7FF)
private val tertiaryDark = Color(0xFFEBC248)
private val onTertiaryDark = Color(0xFF3A3000)
private val tertiaryContainerDark = Color(0xFF534600)
private val onTertiaryContainerDark = Color(0xFFFFE680)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF000000)
private val onBackgroundDark = Color(0xFFE3E3E3)
private val surfaceDark = Color(0xFF000000)
private val onSurfaceDark = Color(0xFFE3E3E3)
private val surfaceVariantDark = Color(0xFF444746) // Input bar background
private val onSurfaceVariantDark = Color(0xFFC4C7C5)
private val outlineDark = Color(0xFF8E918F)
private val outlineVariantDark = Color(0xFF444746)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFE3E3E3)
private val inverseOnSurfaceDark = Color(0xFF303030)
private val inversePrimaryDark = Color(0xFF0B57D0)


private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
)
