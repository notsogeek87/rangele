package com.rangele.inventory.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rangele.inventory.R

// Kawaii Pastel Pop uses Varela Round everywhere (display, body and labels alike).
val VarelaRound = FontFamily(Font(R.font.varela_round, FontWeight.Normal))

val Typography =
    Typography(
        headlineLarge =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 42.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 25.6.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 22.4.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.3.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.3.sp,
            ),
    )
