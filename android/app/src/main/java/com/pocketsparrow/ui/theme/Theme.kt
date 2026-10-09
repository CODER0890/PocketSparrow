package com.pocketsparrow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CyberBg = Color(0xFF030712)
val CyberSurface = Color(0xFF0B132B)
val CyberCyan = Color(0xFF00E5FF)
val CyberEmerald = Color(0xFF10B981)
val CyberRose = Color(0xFFF43F5E)
val CyberAmber = Color(0xFFF59E0B)

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = CyberEmerald,
    error = CyberRose,
    background = CyberBg,
    surface = CyberSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun PocketSparrowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
