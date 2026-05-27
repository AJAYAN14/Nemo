package com.jian.nemo.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.jian.nemo.core.designsystem.R
import androidx.compose.ui.unit.sp

/**
 * Material3 typography scale for Nemo 2.0
 */
private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val NotoSerifJP = FontFamily(
    Font(
        googleFont = GoogleFont("Noto Serif JP"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Noto Serif JP"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("Noto Serif JP"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Bold
    ),
    Font(
        googleFont = GoogleFont("Noto Serif JP"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.ExtraBold
    )
)

val NotoSerifSC = FontFamily(
    Font(
        googleFont = GoogleFont("Noto Serif SC"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Noto Serif SC"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("Noto Serif SC"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Bold
    ),
    Font(
        googleFont = GoogleFont("Noto Serif SC"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.ExtraBold
    )
)

val Baloo2 = FontFamily(
    Font(
        googleFont = GoogleFont("Baloo 2"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Baloo 2"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("Baloo 2"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Bold
    ),
    Font(
        googleFont = GoogleFont("Baloo 2"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.ExtraBold
    )
)

val Nunito = FontFamily(
    Font(
        googleFont = GoogleFont("Nunito"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Nunito"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("Nunito"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Bold
    ),
    Font(
        googleFont = GoogleFont("Nunito"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.ExtraBold
    )
)

val Rubik = FontFamily(
    Font(
        googleFont = GoogleFont("Rubik"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Rubik"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("Rubik"),
        fontProvider = googleFontsProvider,
        weight = FontWeight.Bold
    )
)

val MochiyPopOne = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.mochiy_pop_one, FontWeight.Normal)
)

val NemoTypography = Typography(
    // Display styles
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // Headline styles
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // Title styles
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Body styles
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // Label styles
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
