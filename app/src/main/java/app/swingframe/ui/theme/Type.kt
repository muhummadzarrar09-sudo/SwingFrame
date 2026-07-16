package app.swingframe.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val EditorialSans = FontFamily.SansSerif

val SwingFrameTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.35).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.65).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.15).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.05.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.15.sp,
    ),
)
