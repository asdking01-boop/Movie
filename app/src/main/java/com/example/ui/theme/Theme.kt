package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val KrasDarkColorScheme = darkColorScheme(
    primary = KrasRed,
    onPrimary = KrasWhite,
    primaryContainer = KrasRedContainer,
    onPrimaryContainer = KrasWhite,
    secondary = KrasGold,
    onSecondary = KrasBlack,
    secondaryContainer = KrasSurfaceElevated,
    onSecondaryContainer = KrasWhite,
    tertiary = KrasTeal,
    onTertiary = KrasBlack,
    background = KrasBlack,
    onBackground = KrasWhite,
    surface = KrasObsidian,
    onSurface = KrasWhite,
    surfaceVariant = KrasCharcoal,
    onSurfaceVariant = KrasSilver,
    outline = KrasCardBorder,
    error = KrasError,
    onError = KrasWhite
)

val KrasShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KrasMoviesTheme(
    languageCode: String = "en",
    content: @Composable () -> Unit
) {
    val isArabic = languageCode.lowercase().startsWith("ar")
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = KrasDarkColorScheme,
            typography = krasTypography(isArabic = isArabic),
            shapes = KrasShapes,
            content = content
        )
    }
}
