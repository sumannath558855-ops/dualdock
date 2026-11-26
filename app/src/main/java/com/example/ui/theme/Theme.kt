package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DualDockPastelLightScheme = lightColorScheme(
    primary = JetInk,
    onPrimary = PureWhite,
    primaryContainer = LavenderTint,
    onPrimaryContainer = DeepLavender,
    secondary = DeepLavender,
    onSecondary = PureWhite,
    secondaryContainer = SkyTint,
    onSecondaryContainer = SkyDeep,
    tertiary = PinkDeep,
    onTertiary = PureWhite,
    tertiaryContainer = PinkTint,
    onTertiaryContainer = PinkDeep,
    background = CanvasWhite,
    onBackground = JetInk,
    surface = PureWhite,
    onSurface = JetInk,
    surfaceVariant = SoftCloud,
    onSurfaceVariant = SlateInk,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = CanvasWhite,
    surfaceContainer = SoftCloud,
    surfaceContainerHigh = SoftCloud,
    surfaceContainerHighest = HairlineBorder,
    outline = MutedInk,
    outlineVariant = HairlineBorder,
    error = DangerCoral,
    onError = PureWhite,
    errorContainer = DangerTint,
    onErrorContainer = DangerCoral
)

val DualDockShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun DualDockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DualDockPastelLightScheme,
        typography = DualDockTypography,
        shapes = DualDockShapes,
        content = content
    )
}
