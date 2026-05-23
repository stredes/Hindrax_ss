package com.hindrax.ss.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.hindrax.ss.domain.theme.HindraxThemePreset

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryBlue,
    background = DarkGray,
    surface = SurfaceGray,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryBlue,
    background = Color.White,
    surface = Color(0xFFF5F5F5),
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun HindraxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    preset: HindraxThemePreset = HindraxThemePreset(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = preset.accent.toComposeColor(PrimaryBlue),
            secondary = preset.warning.toComposeColor(SecondaryBlue),
            background = preset.background.toComposeColor(DarkGray),
            surface = preset.surface.toComposeColor(SurfaceGray),
            onPrimary = Color.Black,
            onBackground = preset.text.toComposeColor(Color.White),
            onSurface = preset.text.toComposeColor(Color.White),
            error = preset.danger.toComposeColor(RiskCritical)
        )
    } else {
        lightColorScheme(
            primary = preset.accent.toComposeColor(PrimaryBlue),
            secondary = preset.warning.toComposeColor(SecondaryBlue),
            background = preset.background.toComposeColor(Color.White),
            surface = preset.surface.toComposeColor(Color(0xFFF5F5F5)),
            onPrimary = Color.Black,
            onBackground = preset.text.toComposeColor(Color.Black),
            onSurface = preset.text.toComposeColor(Color.Black),
            error = preset.danger.toComposeColor(RiskCritical)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun String.toComposeColor(fallback: Color): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(this))
    }.getOrDefault(fallback)
}
