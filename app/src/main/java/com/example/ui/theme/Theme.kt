package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalThemeMode = compositionLocalOf { false }

private val DarkColorScheme =
  darkColorScheme(
      primary = AccentIndigo,
      secondary = AccentTeal,
      tertiary = Pink80,
      background = BackgroundDark,
      surface = BackgroundDark,
      surfaceVariant = SurfaceGlass,
      onSurfaceVariant = TextSecondary,
      onBackground = TextPrimary,
      onSurface = TextPrimary
  )

private val LightColorScheme =
  lightColorScheme(
      primary = AccentIndigo,
      secondary = AccentTeal,
      tertiary = Pink40,
      background = Color(0xFFF8FAFC), // slate-50
      surface = Color(0xFFF1F5F9), // slate-100
      surfaceVariant = Color(0xFFE2E8F0), // slate-200
      onSurfaceVariant = Color(0xFF475569), // slate-600
      onBackground = Color(0xFF0F172A), // slate-900
      onSurface = Color(0xFF0F172A) // slate-900
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  CompositionLocalProvider(LocalThemeMode provides darkTheme) {
      MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
