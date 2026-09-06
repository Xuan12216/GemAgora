package com.example.gemagora.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

@Suppress("DEPRECATION")
private val CjkPlatformTextStyle = PlatformTextStyle(includeFontPadding = false)

private val CjkLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both
)

private fun TextStyle.withCjkCentering(): TextStyle = this.copy(
    platformStyle = CjkPlatformTextStyle,
    lineHeightStyle = CjkLineHeightStyle,
)

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.withCjkCentering(),
    displayMedium = defaultTypography.displayMedium.withCjkCentering(),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
    ).withCjkCentering(),
    headlineLarge = defaultTypography.headlineLarge.withCjkCentering(),
    headlineMedium = defaultTypography.headlineMedium.withCjkCentering(),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ).withCjkCentering(),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ).withCjkCentering(),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ).withCjkCentering(),
    titleSmall = defaultTypography.titleSmall.withCjkCentering(),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ).withCjkCentering(),
    bodyMedium = defaultTypography.bodyMedium.withCjkCentering(),
    bodySmall = defaultTypography.bodySmall.withCjkCentering(),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ).withCjkCentering(),
    labelMedium = defaultTypography.labelMedium.withCjkCentering(),
    labelSmall = defaultTypography.labelSmall.withCjkCentering(),
)
