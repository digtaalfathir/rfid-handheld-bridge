package com.example.chainwayrfidbridge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val BluePrimary = Color(0xFF1565C0)
val BlueDark = Color(0xFF0D47A1)
val LightGrayBackground = Color(0xFFF4F6F9)
val SurfaceGray = Color(0xFFEDF1F6)
val SuccessGreen = Color(0xFF2E7D32)
val ErrorRed = Color(0xFFC62828)
val WarningAmber = Color(0xFFEF6C00)
// Deliberately distinct from BluePrimary (RFID) — the main visual cue that Barcode mode is active.
val BarcodeAccent = Color(0xFF6A1B9A)
val NewTagHighlight = Color(0xFFE3F2FD)

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = NewTagHighlight,
    secondary = BlueDark,
    background = LightGrayBackground,
    surface = Color.White,
    surfaceVariant = SurfaceGray,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82B1FF),
    secondary = Color(0xFF64B5F6),
    background = Color(0xFF121417),
    surface = Color(0xFF1C1F23),
    surfaceVariant = Color(0xFF262A2F),
    error = Color(0xFFEF9A9A)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun ChainwayRfidTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        shapes = AppShapes,
        typography = Typography(),
        content = content
    )
}
