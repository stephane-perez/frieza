package com.frieza.freezer.ui.theme

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

private val FriezaBlue = Color(0xFF1565C0)
private val FriezaBlueLight = Color(0xFF5E92F3)
private val FriezaBlueDark = Color(0xFF003C8F)
private val FriezaIce = Color(0xFFE3F2FD)

private val LightColors = lightColorScheme(
    primary = FriezaBlue,
    secondary = FriezaBlueLight,
    tertiary = FriezaBlueDark,
    background = FriezaIce
)

private val DarkColors = darkColorScheme(
    primary = FriezaBlueLight,
    secondary = FriezaBlue,
    tertiary = FriezaBlueDark
)

@Composable
fun FriezaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
