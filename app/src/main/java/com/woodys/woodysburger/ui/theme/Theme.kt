package com.woodys.woodysburger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorPalette = lightColorScheme(
    primary = Color(0xFFFBC825),
    secondary = Color(0xFF6F4216),
    background = Color(0xFFECE8DC), // Replace with your background color
    surface = Color.White,
    // Add other colors as needed
)

@Composable
fun WoodysBurgerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorPalette,
        content = content
    )
}
