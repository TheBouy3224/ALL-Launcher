package com.movtery.zalithlauncher.all.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.DarkMode

/**
 * One complete set of ALL colours. [Luna] is the Windows XP Luna look that ships today (values unchanged);
 * [Midnight] is the dark variant (navy / charcoal surfaces, same blue and green accents).
 */
class AllPalette(
    val Blue: Color, val BlueDark: Color, val BlueMid: Color, val BlueLight: Color, val BluePale: Color,
    val Green: Color, val Danger: Color, val Ok: Color,
    val WindowBody: Color, val Card: Color, val Border: Color, val BorderStrong: Color,
    val Text: Color, val TextDim: Color, val MenuRight: Color, val MenuRightBorder: Color,
    val Warning: Color, val Disabled: Color, val TitleUnfocused: Color,
    val FooterText: Color,
    val TitleBrush: Brush, val TaskbarBrush: Brush, val GreenBrush: Brush, val ButtonBrush: Brush,
)

val LunaPalette = AllPalette(
    Blue = Color(0xFF316AC5), BlueDark = Color(0xFF0842A8), BlueMid = Color(0xFF0B5CD8), BlueLight = Color(0xFF3A8DFF),
    BluePale = Color(0xFFE8EDF7), Green = Color(0xFF2E8B2E), Danger = Color(0xFFB3261E), Ok = Color(0xFF2E7D32),
    WindowBody = Color(0xFFF3F1E8), Card = Color.White, Border = Color(0xFFB9B4A2), BorderStrong = Color(0xFF7A8BA8),
    Text = Color(0xFF1B1B1B), TextDim = Color(0xFF6B675C), MenuRight = Color(0xFFD3E5FA), MenuRightBorder = Color(0xFF95BDEE),
    Warning = Color(0xFFB7791F), Disabled = Color(0xFFE9E7DE), TitleUnfocused = Color(0xFF8A94A6),
    FooterText = Color(0xCCFFFFFF),
    TitleBrush = Brush.verticalGradient(listOf(Color(0xFF3A8DFF), Color(0xFF0B5CD8), Color(0xFF0842A8))),
    TaskbarBrush = Brush.verticalGradient(listOf(Color(0xFF3F7FE6), Color(0xFF1F57C3), Color(0xFF1848A8))),
    GreenBrush = Brush.verticalGradient(listOf(Color(0xFF5CC85C), Color(0xFF2E8B2E))),
    ButtonBrush = Brush.verticalGradient(listOf(Color.White, Color(0xFFE3E0D5))),
)

/** Midnight = ALL Desktop V3: flat navy surfaces, one blue accent, no gradients (the XP Luna look is retired). */
private val V3Title = Color(0xFF223049)
val MidnightPalette = AllPalette(
    Blue = Color(0xFF2F6FED), BlueDark = Color(0xFF2559C4), BlueMid = Color(0xFF3A7BF0), BlueLight = Color(0xFF6F9BFF),
    BluePale = Color(0xFF1F335B), Green = Color(0xFF3FA34D), Danger = Color(0xFFFF6B6B), Ok = Color(0xFF5BD08A),
    WindowBody = Color(0xFF161B28), Card = Color(0xFF1B2232), Border = Color(0xFF2D3548), BorderStrong = Color(0xFF3A4660),
    Text = Color(0xFFD5DBE8), TextDim = Color(0xFF8C97AE), MenuRight = Color(0xFF1B2232), MenuRightBorder = Color(0xFF2D3548),
    Warning = Color(0xFFF0A93A), Disabled = Color(0xFF202838), TitleUnfocused = Color(0xFF111622),
    FooterText = Color(0xCCFFFFFF),
    TitleBrush = Brush.verticalGradient(listOf(V3Title, V3Title)),
    TaskbarBrush = Brush.verticalGradient(listOf(Color(0xFF11151F), Color(0xFF11151F))),
    GreenBrush = Brush.verticalGradient(listOf(Color(0xFF3FA34D), Color(0xFF3FA34D))),
    ButtonBrush = Brush.verticalGradient(listOf(Color(0xFF232B3D), Color(0xFF232B3D))),
)

/**
 * The single switch for ALL's theme. Reads of [current] are observable state, so every ALL composable that reads
 * [AllColors] / [AllBrushes] recomposes when the mode changes, without a restart.
 */
object AllTheme {
    //first frame already uses the saved mode (no light flash in dark mode); later changes arrive through AllThemeSync
    private val dark = mutableStateOf(resolveDark(systemDark = (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES))

    val isDark: Boolean get() = dark.value
    val current: AllPalette get() = if (dark.value) MidnightPalette else LunaPalette

    fun update(isDark: Boolean) {
        if (dark.value != isDark) dark.value = isDark
    }
}

/**
 * Keeps [AllTheme] in step with the launcher's existing dark-mode setting (Zalith's `launcherDarkMode`, shown in the ALL
 * launcher settings): Enable = dark, Disable = light, FollowSystem = the Android system night mode.
 * Put it at the top of an ALL composition root. It writes the switch in composition, before the content reads it.
 */
@Composable
fun AllThemeSync() {
    val mode = AllSettings.launcherDarkMode.state
    val system = isSystemInDarkTheme()
    //written after the composition is applied, so readers never see a half-updated palette
    SideEffect { AllTheme.update(resolveDark(mode, system)) }
}

//V3 is dark-only: the light Luna palette is kept in the file but never selected, whatever the old dark-mode setting says
@Suppress("UNUSED_PARAMETER")
private fun resolveDark(mode: DarkMode = AllSettings.launcherDarkMode.state, systemDark: Boolean = false): Boolean = true
