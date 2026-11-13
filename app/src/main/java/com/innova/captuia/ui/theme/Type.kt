package com.innova.captuia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.innova.captuia.R

/* Tipografías de la landing: Space Grotesk para títulos, Inter para texto. */
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun heading(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Bold) = TextStyle(
    fontFamily = SpaceGrotesk,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = (-0.01).em,
)

private fun body(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

val CaptuTypography = Typography(
    displayLarge = heading(48, 52),
    displayMedium = heading(40, 44),
    displaySmall = heading(34, 38),
    headlineLarge = heading(30, 34),
    headlineMedium = heading(26, 30),
    headlineSmall = heading(22, 28),
    titleLarge = heading(20, 26),
    titleMedium = heading(17, 24, FontWeight.SemiBold),
    titleSmall = heading(15, 20, FontWeight.SemiBold),
    bodyLarge = body(16, 24),
    bodyMedium = body(14, 21),
    bodySmall = body(12, 18),
    labelLarge = body(15, 20, FontWeight.SemiBold),
    labelMedium = body(13, 18, FontWeight.SemiBold),
    labelSmall = body(11, 16, FontWeight.Medium),
)
