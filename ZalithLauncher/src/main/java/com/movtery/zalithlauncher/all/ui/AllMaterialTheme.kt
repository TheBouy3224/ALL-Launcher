package com.movtery.zalithlauncher.all.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * The ALL look for Material components that are still shared with Zalith's backend screens (the touch-control editor and its dialogs).
 * It only overrides colours and corner shapes under it, so the editor's behaviour, layout engine and file format are untouched.
 * The palette is [AllColors] (the same one the ALL Desktop windows use), so the editor no longer shows the launcher's colour theme.
 */
@Composable
fun AllMaterialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AllColors.Blue,
            onPrimary = AllColors.Card,
            primaryContainer = AllColors.BluePale,
            onPrimaryContainer = AllColors.Text,
            secondary = AllColors.BlueMid,
            onSecondary = AllColors.Card,
            secondaryContainer = AllColors.MenuRight,
            onSecondaryContainer = AllColors.Text,
            tertiary = AllColors.Green,
            onTertiary = AllColors.Card,
            tertiaryContainer = AllColors.BluePale,
            onTertiaryContainer = AllColors.Text,
            error = AllColors.Danger,
            onError = AllColors.Card,
            errorContainer = AllColors.Danger.copy(alpha = 0.12f),
            onErrorContainer = AllColors.Danger,
            background = AllColors.WindowBody,
            onBackground = AllColors.Text,
            surface = AllColors.WindowBody,
            onSurface = AllColors.Text,
            surfaceVariant = AllColors.Disabled,
            onSurfaceVariant = AllColors.TextDim,
            outline = AllColors.Border,
            outlineVariant = AllColors.Border,
            surfaceDim = AllColors.Disabled,
            surfaceBright = AllColors.Card,
            surfaceContainerLowest = AllColors.Card,
            surfaceContainerLow = AllColors.WindowBody,
            surfaceContainer = AllColors.WindowBody,
            surfaceContainerHigh = AllColors.Card,
            surfaceContainerHighest = AllColors.BluePale,
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(3.dp),
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(6.dp),
            large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(8.dp)
        ),
        content = content
    )
}
