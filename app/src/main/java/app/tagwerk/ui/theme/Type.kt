@file:OptIn(ExperimentalTextApi::class)

package app.tagwerk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.tagwerk.R

/** Nunito – rund und freundlich, für alle Texte. */
val Nunito = FontFamily(
    Font(R.font.nunito, weight = FontWeight.Normal),
    Font(R.font.nunito, weight = FontWeight.Medium),
    Font(R.font.nunito, weight = FontWeight.SemiBold),
    Font(R.font.nunito, weight = FontWeight.Bold),
    Font(R.font.nunito, weight = FontWeight.ExtraBold),
)

/** Fraunces mit weicher Achse – für Überschriften. */
val Fraunces = FontFamily(
    Font(
        R.font.fraunces,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(420),
            FontVariation.Setting("SOFT", 100f),
            FontVariation.Setting("opsz", 72f),
        ),
    ),
    Font(
        R.font.fraunces,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.Setting("SOFT", 100f),
            FontVariation.Setting("opsz", 72f),
        ),
    ),
)

private val base = Typography()

val TagwerkTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Fraunces),
    displayMedium = base.displayMedium.copy(fontFamily = Fraunces),
    displaySmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineLarge = base.headlineLarge.copy(fontFamily = Fraunces),
    headlineMedium = base.headlineMedium.copy(fontFamily = Fraunces),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp),
)
