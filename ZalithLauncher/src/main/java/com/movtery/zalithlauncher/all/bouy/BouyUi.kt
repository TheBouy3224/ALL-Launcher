package com.movtery.zalithlauncher.all.bouy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The Studio's one look: dark neutral surfaces, hairline borders, a single blue accent, round pills (minimal modern) */
internal object BouyTheme {
    val surface = Color(0xFA141922)
    val raised = Color(0xFF1E2532)
    val line = Color(0xFF2B3342)
    val text = Color(0xFFE8ECF3)
    val dim = Color(0xFF8B96A8)
    val accent = Color(0xFF5B9DFF)
    val danger = Color(0xFFFF8A80)
}

/** Dark Material colours for the Studio and the message dialogs, so text fields, sliders and switches match the pills */
@Composable
internal fun BouyMaterial(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = BouyTheme.accent, onPrimary = Color(0xFF0B1220), secondary = BouyTheme.accent, onSecondary = Color(0xFF0B1220),
        background = Color(0xFF0F131B), onBackground = BouyTheme.text, surface = Color(0xFF141922), onSurface = BouyTheme.text,
        surfaceVariant = BouyTheme.raised, onSurfaceVariant = BouyTheme.dim, outline = BouyTheme.line,
    ),
    content = content,
)

/** A rounded surface with a hairline border */
@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(BouyTheme.surface).border(1.dp, BouyTheme.line, RoundedCornerShape(18.dp)), content = content)
}

/** The only button style of the Studio: a pill, filled blue when [primary], otherwise a quiet raised chip */
@Composable
internal fun Pill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false, enabled: Boolean = true, color: Color? = null) {
    val fg = when { !enabled -> BouyTheme.dim.copy(alpha = 0.5f); primary -> Color(0xFF0B1220); else -> color ?: BouyTheme.text }
    val filled = primary && enabled // a disabled primary pill looks like any other quiet pill, not a washed-out blue one
    Box(
        modifier.clip(RoundedCornerShape(50)).background(if (filled) BouyTheme.accent else BouyTheme.raised)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = fg, fontSize = 14.sp, maxLines = 1) }
}

/** One row of the "⋯" menu */
@Composable
internal fun MenuRow(text: String, sub: String? = null, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(text, color = BouyTheme.text, fontSize = 14.sp)
        if (sub != null) Text(sub, color = BouyTheme.dim, fontSize = 11.sp)
    }
}

/** Redraw clock for animated (GIF) pictures: ticks ~15 times a second ONLY while the layout contains one, otherwise it never wakes up */
@Composable
internal fun rememberAnimTick(layout: BouyLayout): androidx.compose.runtime.LongState {
    val any = layout.items.any { BouyStyle.isAnimated(it.image) || it.parts.values.any { p -> BouyStyle.isAnimated(p) } }
    val tick = androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(any) { if (any) while (true) { kotlinx.coroutines.delay(66); tick.longValue++ } }
    return tick
}
