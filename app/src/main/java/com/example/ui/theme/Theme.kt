package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.abs

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold),
    Font(R.font.outfit, FontWeight.ExtraBold)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium)
)

val SubjectPalettes = listOf(
    Color(0xFFD8B4FE), // Soft Purple
    Color(0xFF7DD3FC), // Soft Blue
    Color(0xFF6EE7B7), // Soft Green
    Color(0xFFFCD34D), // Soft Amber
    Color(0xFFA5B4FC)  // Soft Indigo
)

fun subjectAccentColor(subject: String): Color {
    if (subject.isBlank()) return SubjectPalettes[0]
    val clean = subject.lowercase().trim()
    var hash = 0
    for (ch in clean) {
        hash = ch.code + ((hash shl 5) - hash)
    }
    return SubjectPalettes[abs(hash) % SubjectPalettes.size]
}

@Immutable
data class QuizzMeExtendedColors(
    val background: Color,
    val card: Color,
    val title: Color,
    val text: Color,
    val subtext: Color,
    val border: Color,
    val accent: Color,
    val accentSoft: Color,
    val success: Color,
    val successSoft: Color,
    val danger: Color,
    val dangerSoft: Color
)

private val LightExtendedColors = QuizzMeExtendedColors(
    background = Color(0xFFF8F9FA),
    card = Color(0xFFFFFFFF),
    title = Color(0xFF1A1A1A),
    text = Color(0xFF404040),
    subtext = Color(0xFF8E8E93),
    border = Color(0xFFE5E5EA),
    accent = Color(0xFFA855F7),
    accentSoft = Color(0x26C084FC),
    success = Color(0xFF34C759),
    successSoft = Color(0x244ADE80),
    danger = Color(0xFFFF3B30),
    dangerSoft = Color(0x24F87171)
)

private val DarkExtendedColors = QuizzMeExtendedColors(
    background = Color(0xFF1C1C1E),
    card = Color(0xFF262629),
    title = Color(0xFFFFFFFF),
    text = Color(0xFFE5E5EA),
    subtext = Color(0xFFB1B1B7),
    border = Color(0xFF38383A),
    accent = Color(0xFFC084FC),
    accentSoft = Color(0x26C084FC),
    success = Color(0xFF32D74B),
    successSoft = Color(0x244ADE80),
    danger = Color(0xFFFF453A),
    dangerSoft = Color(0x24F87171)
)

val LocalQuizzMeColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFA855F7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E8FF),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF0C4A6E),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF1F0F5),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE5E5EA),
    error = Color(0xFFFF3B30),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC084FC),
    onPrimary = Color(0xFF1E0B36),
    primaryContainer = Color(0xFF3B1D60),
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF082F49),
    background = Color(0xFF1C1C1E),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF262629),
    onSurface = Color(0xFFE5E5EA),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFB1B1B7),
    outline = Color(0xFF38383A),
    error = Color(0xFFFF453A),
    onError = Color.White
)

val QuizzMeTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp
    )
)

val QuizzMeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun QuizzMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalQuizzMeColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = QuizzMeTypography,
            shapes = QuizzMeShapes,
            content = content
        )
    }
}
