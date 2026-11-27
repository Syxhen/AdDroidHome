package com.syxhen.addroidhome.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = AdGuardGreenLight,
    onPrimary = OnAdGuardGreenLight,
    primaryContainer = AdGuardGreenContainerLight,
    onPrimaryContainer = OnAdGuardGreenContainerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = AdGuardGreenDark,
    onPrimary = OnAdGuardGreenDark,
    primaryContainer = AdGuardGreenContainerDark,
    onPrimaryContainer = OnAdGuardGreenContainerDark,
)

@Composable
fun AdDroidHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
