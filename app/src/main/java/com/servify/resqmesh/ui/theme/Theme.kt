package com.servify.resqmesh.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class OffGridMode {
    FESTIVAL, EMERGENCY
}

private val FestivalColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = ElectricBlue.copy(alpha = 0.7f),
    tertiary = Pink80,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val EmergencyColorScheme = darkColorScheme(
    primary = ElectricOrange,
    secondary = ElectricOrange.copy(alpha = 0.7f),
    tertiary = Pink40,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun OffGridTheme(
    mode: OffGridMode = OffGridMode.FESTIVAL,
    content: @Composable () -> Unit
) {
    val colorScheme = if (mode == OffGridMode.EMERGENCY) EmergencyColorScheme else FestivalColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}