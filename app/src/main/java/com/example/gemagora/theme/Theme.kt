package com.example.gemagora.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = GoldLight,
    onPrimary = Night,
    primaryContainer = AgoraDarkGold,
    onPrimaryContainer = PaleGold,
    secondary = SlateLight,
    onSecondary = Night,
    secondaryContainer = SlateDark,
    onSecondaryContainer = MarbleWhite,
    tertiary = Terracotta,
    background = Night,
    surface = Night,
    onBackground = MarbleWhite,
    onSurface = MarbleWhite,
    onSurfaceVariant = SlateLight,
    surfaceContainerLowest = Night,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    outlineVariant = AgoraDarkGold,
)

private val LightColorScheme = lightColorScheme(
    primary = AgoraDarkGold,
    onPrimary = MarbleWhite,
    primaryContainer = PaleGold,
    onPrimaryContainer = Color(0xFF382600),
    secondary = SlateDark,
    onSecondary = MarbleWhite,
    secondaryContainer = ParchmentBorder,
    onSecondaryContainer = SlateDark,
    tertiary = Terracotta,
    background = Canvas,
    surface = Canvas,
    onBackground = Color(0xFF1B1B18),
    onSurface = Color(0xFF1B1B18),
    onSurfaceVariant = OliveMuted,
    surfaceContainerLowest = MarbleWhite,
    surfaceContainerLow = Color(0xFFEFEADF),
    surfaceContainer = Color(0xFFE8E2D4),
    surfaceContainerHigh = Color(0xFFE1DACB),
    surfaceContainerHighest = Color(0xFFD9D1C1),
    outline = Color(0xFF8C8270),
    outlineVariant = ParchmentBorder,
)

private val GemAgoraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun GemAgoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    customHue: Int? = null,
    customSaturation: Float? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        customHue != null -> remember(customHue, customSaturation, darkTheme) {
            customColorScheme(hue = customHue, dark = darkTheme, saturation = customSaturation ?: 0.52f)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = GemAgoraShapes,
        content = content,
    )
}
