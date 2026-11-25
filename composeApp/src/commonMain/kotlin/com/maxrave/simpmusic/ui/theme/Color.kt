package com.maxrave.simpmusic.ui.theme

import androidx.compose.ui.graphics.Color

// ===== Brand =====

/**
 * Brand seed color. The whole Material 3 ColorScheme is generated from this
 * color at runtime — see [AppTheme].
 */
val seed = Color(0xFF00F0FF)

// ===== Semantic colors (not derivable from the color scheme) =====

/** Liked/favorite state (heart buttons, favorite tiles). */
val favoriteColor = Color(0xFFFF2A6D)

/** Currently playing lyric line. */
val lyricActiveColor = Color(0xFF00F0FF)

val shimmerBackground = Color(0x7E383737)
val shimmerLine = Color(0xFF4D4848)

// Light-theme counterparts of the shimmer tokens.
val shimmerBackgroundLight = Color(0x7EDCD8D8)
val shimmerLineLight = Color(0xFFCFC8C8)

val overlay = Color(0x32242424)
val blackMoreOverlay = Color(0x8f242424)

// ===== Desktop shell =====
// Spotify-style layering for the desktop window: the window itself takes the extreme, panels
// step one shade back towards the middle so they read as raised. The light panel is not here —
// it comes from colorScheme.surfaceContainer, which already sits at the right distance.

val desktopWindowDark = Color(0xFF05010D)
val desktopWindowLight = Color(0xFFFFFFFF)
val desktopPanelDark = Color(0xFF0E0820)

// ===== Desktop window controls =====
// The macOS traffic lights. Fixed by convention rather than by theme — users read these by
// colour, so they stay the same in light and dark.

val windowCloseButton = Color(0xFFFF2A6D)
val windowCloseButtonHover = Color(0xFFE54942)
val windowMinimiseButton = Color(0xFFFCEE0A)
val windowMinimiseButtonHover = Color(0xFFE5A93D)
val windowMaximiseButton = Color(0xFF00F0FF)
val windowMaximiseButtonHover = Color(0xFF00B344)

// ===== Legacy — do not add new usages =====

/**
 * Old M3 primary (lavender). Kept only for the SettingScreen storage bar,
 * which stays untouched by owner's decision.
 */
@Deprecated("Legacy storage bar color only — use MaterialTheme.colorScheme.primary in new code")
val md_theme_dark_primary = Color(0xFFB2C5FF)

// ===== Cyberpunk neon palette (CyberSync) =====
val neonCyan = Color(0xFF00F0FF)
val neonMagenta = Color(0xFFFF2A6D)
val neonYellow = Color(0xFFFCEE0A)
val neonVoid = Color(0xFF05010D)
