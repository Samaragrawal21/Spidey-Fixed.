package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SpideyColorScheme = darkColorScheme(
    primary = SpideyCrimsonRed,
    onPrimary = SpideyWhite,
    primaryContainer = SpideyCrimsonDark,
    onPrimaryContainer = SpideyWhite,
    secondary = SpideyDeepBlue,
    onSecondary = SpideyWhite,
    secondaryContainer = SpideyVibrantBlue,
    onSecondaryContainer = SpideyWhite,
    background = SpideyMatteBlack,
    onBackground = SpideyWhite,
    surface = SpideyDarkCharcoal,
    onSurface = SpideyWhite,
    surfaceVariant = SpideyElevatedSurface,
    onSurfaceVariant = SpideyDimGrey,
    outline = SpideyCrimsonRed,
    outlineVariant = SpideyBorderGrey
)

@Composable
fun SpideyReminderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpideyColorScheme,
        typography = Typography,
        content = content
    )
}
