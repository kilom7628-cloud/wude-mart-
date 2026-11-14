package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val WudeDarkColorScheme =
  darkColorScheme(
    primary = CrispWhite,
    onPrimary = PureBlack,
    primaryContainer = CharcoalElevated,
    onPrimaryContainer = DarkSilverText,
    secondary = Color(0xFFC5C9D3),
    onSecondary = PureBlack,
    secondaryContainer = CharcoalHighest,
    onSecondaryContainer = DarkSilverText,
    tertiary = Color(0xFF9EA4B3),
    onTertiary = PureBlack,
    tertiaryContainer = CharcoalElevated,
    onTertiaryContainer = DarkSilverText,
    background = PureBlack,
    onBackground = DarkSilverText,
    surface = GraphiteCharcoal,
    onSurface = DarkSilverText,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = DarkSilverMuted,
    surfaceContainerLow = ObsidianInk,
    surfaceContainer = GraphiteCharcoal,
    surfaceContainerHigh = CharcoalElevated,
    outline = Color(0xFF474B57),
    outlineVariant = DarkGlassBorder,
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF4A0000),
  )

private val WudeLightColorScheme =
  lightColorScheme(
    primary = PureBlack,
    onPrimary = CrispWhite,
    primaryContainer = SilverMist,
    onPrimaryContainer = ObsidianInk,
    secondary = Color(0xFF3E424C),
    onSecondary = CrispWhite,
    secondaryContainer = SilverSurface,
    onSecondaryContainer = ObsidianInk,
    tertiary = SlateMuted,
    onTertiary = CrispWhite,
    tertiaryContainer = SilverMist,
    onTertiaryContainer = ObsidianInk,
    background = AlabasterCanvas,
    onBackground = ObsidianInk,
    surface = CrispWhite,
    onSurface = ObsidianInk,
    surfaceVariant = SilverMist,
    onSurfaceVariant = SlateMuted,
    surfaceContainerLow = CrispWhite,
    surfaceContainer = SilverMist,
    surfaceContainerHigh = SilverSurface,
    outline = Color(0xFFB7BCC8),
    outlineVariant = SilverBorder,
    error = StatusDanger,
    onError = CrispWhite,
  )

val WudeShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

enum class ThemePreference {
  SYSTEM,
  LIGHT,
  DARK,
}

@Composable
fun WudeMartTheme(
  themePreference: ThemePreference = ThemePreference.SYSTEM,
  isAmharic: Boolean = false,
  content: @Composable () -> Unit,
) {
  val darkTheme =
    when (themePreference) {
      ThemePreference.SYSTEM -> isSystemInDarkTheme()
      ThemePreference.LIGHT -> false
      ThemePreference.DARK -> true
    }

  val colorScheme = if (darkTheme) WudeDarkColorScheme else WudeLightColorScheme
  val typography =
    if (isAmharic) {
      WudeTypography.copy(
        displaySmall = WudeTypography.displaySmall.copy(fontFamily = EthiopicFontFamily),
        headlineMedium = WudeTypography.headlineMedium.copy(fontFamily = EthiopicFontFamily),
        headlineSmall = WudeTypography.headlineSmall.copy(fontFamily = EthiopicFontFamily),
        titleLarge = WudeTypography.titleLarge.copy(fontFamily = EthiopicFontFamily),
        titleMedium = WudeTypography.titleMedium.copy(fontFamily = EthiopicFontFamily),
        titleSmall = WudeTypography.titleSmall.copy(fontFamily = EthiopicFontFamily),
        bodyLarge = WudeTypography.bodyLarge.copy(fontFamily = EthiopicFontFamily),
        bodyMedium = WudeTypography.bodyMedium.copy(fontFamily = EthiopicFontFamily),
        bodySmall = WudeTypography.bodySmall.copy(fontFamily = EthiopicFontFamily),
        labelLarge = WudeTypography.labelLarge.copy(fontFamily = EthiopicFontFamily),
        labelMedium = WudeTypography.labelMedium.copy(fontFamily = EthiopicFontFamily),
        labelSmall = WudeTypography.labelSmall.copy(fontFamily = EthiopicFontFamily),
      )
    } else {
      WudeTypography
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = typography,
    shapes = WudeShapes,
    content = content,
  )
}
