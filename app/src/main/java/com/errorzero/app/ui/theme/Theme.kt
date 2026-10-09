package com.errorzero.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 纸、墨、朱砂：克制的三色体系
private val LightScheme = lightColorScheme(
    primary = Color(0xFF1C1B19),
    onPrimary = Color(0xFFF7F6F2),
    primaryContainer = Color(0xFFE9E6DF),
    onPrimaryContainer = Color(0xFF1C1B19),
    inversePrimary = Color(0xFFE9E6DF),
    secondary = Color(0xFF55524C),
    onSecondary = Color(0xFFF7F6F2),
    secondaryContainer = Color(0xFFE9E6DF),
    onSecondaryContainer = Color(0xFF1C1B19),
    tertiary = Color(0xFFA0402C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E3DE),
    onTertiaryContainer = Color(0xFF5A1E12),
    background = Color(0xFFF7F6F2),
    onBackground = Color(0xFF1C1B19),
    surface = Color(0xFFF7F6F2),
    onSurface = Color(0xFF1C1B19),
    surfaceVariant = Color(0xFFEDEBE5),
    onSurfaceVariant = Color(0xFF6E6A62),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF1C1B19),
    inverseOnSurface = Color(0xFFF7F6F2),
    error = Color(0xFFA0402C),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFFCFCBC2),
    outlineVariant = Color(0xFFE4E1D9),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFBFAF7),
    surfaceDim = Color(0xFFE9E6DF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F8F5),
    surfaceContainer = Color(0xFFF7F6F2),
    surfaceContainerHigh = Color(0xFFF7F6F2),
    surfaceContainerHighest = Color(0xFFEDEBE5),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFE9E6DF),
    onPrimary = Color(0xFF131312),
    primaryContainer = Color(0xFF2A2927),
    onPrimaryContainer = Color(0xFFE9E6DF),
    inversePrimary = Color(0xFF1C1B19),
    secondary = Color(0xFFB8B4AB),
    onSecondary = Color(0xFF131312),
    secondaryContainer = Color(0xFF2A2927),
    onSecondaryContainer = Color(0xFFE9E6DF),
    tertiary = Color(0xFFD98670),
    onTertiary = Color(0xFF131312),
    tertiaryContainer = Color(0xFF3A221C),
    onTertiaryContainer = Color(0xFFF3D3C9),
    background = Color(0xFF131312),
    onBackground = Color(0xFFE9E6DF),
    surface = Color(0xFF131312),
    onSurface = Color(0xFFE9E6DF),
    surfaceVariant = Color(0xFF1F1F1D),
    onSurfaceVariant = Color(0xFF9A968D),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFE9E6DF),
    inverseOnSurface = Color(0xFF131312),
    error = Color(0xFFD98670),
    onError = Color(0xFF131312),
    outline = Color(0xFF45433F),
    outlineVariant = Color(0xFF2A2927),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2A2927),
    surfaceDim = Color(0xFF131312),
    surfaceContainerLowest = Color(0xFF0E0E0D),
    surfaceContainerLow = Color(0xFF171716),
    surfaceContainer = Color(0xFF1A1A19),
    surfaceContainerHigh = Color(0xFF1F1F1D),
    surfaceContainerHighest = Color(0xFF262624),
)

private val Serif = FontFamily.Serif

private val AppTypography = Typography().let { t ->
    t.copy(
        displayMedium = t.displayMedium.copy(fontFamily = Serif, fontWeight = FontWeight.Normal),
        displaySmall = t.displaySmall.copy(fontFamily = Serif, fontWeight = FontWeight.Normal),
        headlineLarge = t.headlineLarge.copy(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 30.sp),
        headlineMedium = t.headlineMedium.copy(fontFamily = Serif, fontWeight = FontWeight.Medium),
        headlineSmall = t.headlineSmall.copy(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 30.sp),
        titleLarge = t.titleLarge.copy(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 20.sp),
        bodyLarge = t.bodyLarge.copy(fontSize = 15.sp, lineHeight = 23.sp, letterSpacing = 0.2.sp),
        bodyMedium = t.bodyMedium.copy(lineHeight = 21.sp),
        labelMedium = t.labelMedium.copy(letterSpacing = 0.8.sp),
        labelSmall = t.labelSmall.copy(letterSpacing = 1.2.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

@Composable
fun ErrorZeroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
