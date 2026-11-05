package io.github.cloner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryGreen = Color(0xFF006C50)
val OnPrimaryWhite = Color(0xFFFFFFFF)
val PrimaryContainerGreen = Color(0xFF85F8CE)
val OnPrimaryContainerGreen = Color(0xFF002116)
val BackgroundSurface = Color(0xFFFBFDF9)
val OnSurfaceText = Color(0xFF191C1B)
val FrozenBlue = Color(0xFF0288D1)
val FrozenContainer = Color(0xFFE1F5FE)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnPrimaryWhite,
    primaryContainer = PrimaryContainerGreen,
    onPrimaryContainer = OnPrimaryContainerGreen,
    surface = BackgroundSurface,
    onSurface = OnSurfaceText,
    secondary = FrozenBlue,
    secondaryContainer = FrozenContainer
)

@Composable
fun ClonerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
