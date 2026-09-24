package com.forge.gym.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.forge.gym.R

object Forge {
    val Bg = Color(0xFF0A0B0E)
    val Surface = Color(0xFF13151A)
    val Surface2 = Color(0xFF1B1E24)
    val Surface3 = Color(0xFF252932)
    val Stroke = Color(0xFF22262E)
    val StrokeStrong = Color(0xFF30353F)

    val Lime = Color(0xFFC6F432)
    val LimeDim = Color(0xFF2B3510)
    val OnLime = Color(0xFF0A0B0E)
    val Coral = Color(0xFFFF6A4D)
    val Blue = Color(0xFF6E8BFF)
    val Violet = Color(0xFFB08CFF)
    val Amber = Color(0xFFFFC24B)
    val Teal = Color(0xFF3DD9C1)
    val Pink = Color(0xFFFF6FB1)

    val Text1 = Color(0xFFF3F4F6)
    val Text2 = Color(0xFF9CA2AE)
    val Text3 = Color(0xFF5E6470)
}

fun muscleColor(group: String): Color = when (group) {
    "Chest" -> Forge.Coral
    "Back" -> Forge.Blue
    "Shoulders" -> Forge.Violet
    "Biceps" -> Forge.Amber
    "Triceps" -> Forge.Pink
    "Legs" -> Forge.Lime
    "Glutes" -> Forge.Teal
    "Core" -> Color(0xFFFF9F43)
    "Cardio" -> Color(0xFF52C7FF)
    else -> Color(0xFFB0B6C2)
}

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val Display = FontFamily(
    variable(R.font.space_grotesk, 400),
    variable(R.font.space_grotesk, 500),
    variable(R.font.space_grotesk, 600),
    variable(R.font.space_grotesk, 700),
)

val Body = FontFamily(
    variable(R.font.inter, 400),
    variable(R.font.inter, 500),
    variable(R.font.inter, 600),
    variable(R.font.inter, 700),
)

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-0.04).em),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.03).em),
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.03).em),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.02).em),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.01).em),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.em),
    labelSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.06.em),
)

private val colors = darkColorScheme(
    primary = Forge.Lime,
    onPrimary = Forge.OnLime,
    primaryContainer = Forge.LimeDim,
    onPrimaryContainer = Forge.Lime,
    secondary = Forge.Blue,
    onSecondary = Color.White,
    tertiary = Forge.Coral,
    background = Forge.Bg,
    onBackground = Forge.Text1,
    surface = Forge.Surface,
    onSurface = Forge.Text1,
    surfaceVariant = Forge.Surface2,
    onSurfaceVariant = Forge.Text2,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Forge.Bg,
    surfaceContainerLow = Forge.Surface,
    surfaceContainer = Forge.Surface,
    surfaceContainerHigh = Forge.Surface2,
    surfaceContainerHighest = Forge.Surface3,
    outline = Forge.StrokeStrong,
    outlineVariant = Forge.Stroke,
    error = Forge.Coral,
    onError = Color.White,
    scrim = Color(0xCC000000),
)

@Composable
fun ForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
