package com.jlong.miccheck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = DeepOrange200,
    primaryVariant = DeepOrange200Dark,
    secondary = Color(0xff956353),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    surface = Color.Black
)

private val LightColorPalette = lightColors(
    primary = DeepOrange200,
    primaryVariant = DeepOrange200Dark,
    secondary = Color(0xffFDCDC1),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    surface = DeepOrange50,
    onSurface = Color.Black
    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun MicCheckTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable() () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}