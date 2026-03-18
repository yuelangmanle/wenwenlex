package com.yueliangmanle.danci.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DanciPrimary,
    secondary = DanciSecondary,
    background = DanciBackground,
)

@Composable
fun DanciTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = DanciTypography,
        content = content,
    )
}
