package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF52A276),
    onPrimary = Color(0xFF072113),
    primaryContainer = Color(0xFF1C3F2B),
    onPrimaryContainer = Color(0xFFA5E6BE),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF3A2800),
    onSecondaryContainer = Color(0xFFFFDFA6),
    tertiary = Color(0xFF70B49B),
    background = Color(0xFF111713),
    onBackground = Color(0xFFE1E7E2),
    surface = Color(0xFF18221C),
    onSurface = Color(0xFFE1E7E2),
    surfaceVariant = Color(0xFF223028),
    onSurfaceVariant = Color(0xFFA8B7AD),
    outline = Color(0xFF3B4E43),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF2D6348),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EEDF),
    onPrimaryContainer = Color(0xFF032112),
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = Color(0xFF3B6E5C),
    background = Color(0xFFF7FAF7),
    onBackground = Color(0xFF131A15),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF131A15),
    surfaceVariant = Color(0xFFE3EDE5),
    onSurfaceVariant = Color(0xFF405046),
    outline = Color(0xFF8B9D91),
  )

@Composable
fun ArolockTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

// Retain alias for template compatibility
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  ArolockTheme(darkTheme = darkTheme, content = content)
}

