package org.freemind.mmx.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Seed = Color(0xFF1B5E4B)
private val OnSeed = Color(0xFFF4F7F5)
private val SurfaceLight = Color(0xFFF2F5F3)
private val SurfaceDark = Color(0xFF121A17)

private val LightColors = lightColorScheme(
    primary = Seed,
    onPrimary = OnSeed,
    secondary = Color(0xFF3E6B5C),
    background = SurfaceLight,
    surface = SurfaceLight,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BC6B0),
    onPrimary = Color(0xFF003828),
    secondary = Color(0xFFA5C4B6),
    background = SurfaceDark,
    surface = SurfaceDark,
)

@Composable
fun FreeMindMmxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
