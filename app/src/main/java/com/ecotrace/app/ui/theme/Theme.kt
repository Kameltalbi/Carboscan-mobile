package com.ecotrace.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Couleurs ─────────────────────────────────────────────────────────────────
val EcoGreen = Color(0xFF4ADE80)
val EcoGreenDim = Color(0xFF1A3A22)
val EcoBlue = Color(0xFF60A5FA)
val EcoAmber = Color(0xFFF59E0B)
val EcoRed = Color(0xFFEF4444)
val EcoOrange = Color(0xFFF97316)

val BgDark = Color(0xFF0A0F0A)
val SurfaceDark = Color(0xFF111711)
val Surface2Dark = Color(0xFF1A221A)
val BorderDark = Color(0xFF2A3A2A)
val TextDim = Color(0xFF6B8A6B)

// Scope colors
val Scope1Color = EcoGreen
val Scope2Color = EcoBlue
val Scope3Color = EcoAmber

// ── Color Scheme ─────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = EcoGreen,
    onPrimary = Color(0xFF0A0F0A),
    primaryContainer = EcoGreenDim,
    onPrimaryContainer = EcoGreen,
    background = BgDark,
    onBackground = Color(0xFFE8F0E8),
    surface = SurfaceDark,
    onSurface = Color(0xFFE8F0E8),
    surfaceVariant = Surface2Dark,
    onSurfaceVariant = TextDim,
    outline = BorderDark,
    secondary = EcoBlue,
    tertiary = EcoAmber,
    error = EcoRed
)

// ── Theme ────────────────────────────────────────────────────────────────────
@Composable
fun EcoTraceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = EcoTypography,
        content = content
    )
}

val EcoTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 52.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 38.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 1.sp)
)
