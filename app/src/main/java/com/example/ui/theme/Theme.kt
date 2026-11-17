package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val KhalidLightColorScheme = lightColorScheme(
    primary = PrimaryDeepGreen,
    onPrimary = SurfaceWhite,
    primaryContainer = LightGreen,
    onPrimaryContainer = PrimaryDeepGreen,
    secondary = SecondaryGreen,
    onSecondary = SurfaceWhite,
    secondaryContainer = Color(0xFFFEF9C3),
    onSecondaryContainer = TextDarkGreen,
    tertiary = WarmYellow,
    onTertiary = TextDarkGreen,
    background = BgCream,
    onBackground = TextDarkGreen,
    surface = SurfaceWhite,
    onSurface = TextDarkGreen,
    surfaceVariant = SurfaceVariantCream,
    onSurfaceVariant = TextMutedGreen,
    outline = OutlineGreen,
    error = ErrorRed,
    onError = SurfaceWhite
)

private val KhalidDarkColorScheme = darkColorScheme(
    primary = DarkPrimaryGreen,
    onPrimary = DarkOnPrimary,
    primaryContainer = PrimaryDeepGreen,
    onPrimaryContainer = LightGreen,
    secondary = SecondaryGreen,
    onSecondary = SurfaceWhite,
    tertiary = WarmYellow,
    onTertiary = TextDarkGreen,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA7C4AE),
    outline = Color(0xFF2D4C34),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

val KhalidAgroShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KhalidAgroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KhalidDarkColorScheme else KhalidLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KhalidAgroTypography,
        shapes = KhalidAgroShapes,
        content = content
    )
}
