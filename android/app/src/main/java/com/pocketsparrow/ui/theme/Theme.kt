package com.pocketsparrow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand Accents
val CyberCyan = Color(0xFF0284C7)      // Sky 600 (Light) / 0xFF00E5FF in Dark
val CyberEmerald = Color(0xFF059669)   // Emerald 600 (Light) / 0xFF10B981 in Dark
val CyberRose = Color(0xFFE11D48)      // Rose 600 (Light) / 0xFFF43F5E in Dark
val CyberAmber = Color(0xFFD97706)     // Amber 600 (Light) / 0xFFF59E0B in Dark

// Dark Palette
val DarkBg = Color(0xFF030712)
val DarkSurface = Color(0xFF0F172A)
val DarkCardBg = Color(0xFF0F172A)
val DarkCardBorder = Color(0xFF1E293B)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextMuted = Color(0xFF64748B)

// Compatibility aliases
val CyberBg = DarkBg
val CyberSurface = DarkSurface

// Light Palette (Crisp, High-Contrast Enterprise Theme)
val LightBg = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightCardBg = Color(0xFFFFFFFF)
val LightCardBorder = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextMuted = Color(0xFF94A3B8)

data class SparrowColorTokens(
    val bg: Color,
    val surface: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primary: Color,
    val emerald: Color,
    val rose: Color,
    val amber: Color,
    val isDark: Boolean
)

val LocalSparrowColors = staticCompositionLocalOf {
    SparrowColorTokens(
        bg = LightBg,
        surface = LightSurface,
        cardBg = LightCardBg,
        cardBorder = LightCardBorder,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        textMuted = LightTextMuted,
        primary = CyberCyan,
        emerald = CyberEmerald,
        rose = CyberRose,
        amber = CyberAmber,
        isDark = false
    )
}

private val LightColorScheme = lightColorScheme(
    primary = CyberCyan,
    secondary = CyberEmerald,
    error = CyberRose,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = Color(0xFFF1F5F9),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFCBD5E1),
    outlineVariant = LightCardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFF10B981),
    error = Color(0xFFF43F5E),
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF1E293B),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF334155),
    outlineVariant = DarkCardBorder
)

@Composable
fun PocketSparrowTheme(
    isDarkTheme: Boolean = false, // Light Theme is default as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme
    val tokens = if (isDarkTheme) {
        SparrowColorTokens(
            bg = DarkBg,
            surface = DarkSurface,
            cardBg = DarkCardBg,
            cardBorder = DarkCardBorder,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            textMuted = DarkTextMuted,
            primary = Color(0xFF00E5FF),
            emerald = Color(0xFF10B981),
            rose = Color(0xFFF43F5E),
            amber = Color(0xFFF59E0B),
            isDark = true
        )
    } else {
        SparrowColorTokens(
            bg = LightBg,
            surface = LightSurface,
            cardBg = LightCardBg,
            cardBorder = LightCardBorder,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textMuted = LightTextMuted,
            primary = CyberCyan,
            emerald = CyberEmerald,
            rose = CyberRose,
            amber = CyberAmber,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalSparrowColors provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
