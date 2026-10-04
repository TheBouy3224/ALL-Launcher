package com.movtery.zalithlauncher.all.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup

/**
 * ALL design system foundation (PHASE 4.0): "Windows XP DNA + modern minimal + touch-first".
 *
 * XP DNA = blue title bars, green start, beige window bodies, rounded bevels.
 * Modern/touch = 44-48 dp targets, generous spacing, one radius scale, soft shadows, short press animations.
 * Everything is stateless presentation; behaviour and backends stay in the callers.
 */
object AllColors {
    val Blue = Color(0xFF316AC5)
    val BlueDark = Color(0xFF0842A8)
    val BlueMid = Color(0xFF0B5CD8)
    val BlueLight = Color(0xFF3A8DFF)
    val BluePale = Color(0xFFE8EDF7)
    val Green = Color(0xFF2E8B2E)
    val Danger = Color(0xFFB3261E)
    val Ok = Color(0xFF2E7D32)
    val WindowBody = Color(0xFFF3F1E8)
    val Card = Color.White
    val Border = Color(0xFFB9B4A2)
    val BorderStrong = Color(0xFF7A8BA8)
    val Text = Color(0xFF1B1B1B)
    val TextDim = Color(0xFF6B675C)
    val MenuRight = Color(0xFFD3E5FA)
    val MenuRightBorder = Color(0xFF95BDEE)
    val FooterText = Color(0xCCFFFFFF)   // de-emphasised text on blue bars
}

object AllBrushes {
    val Title = Brush.verticalGradient(listOf(Color(0xFF3A8DFF), Color(0xFF0B5CD8), Color(0xFF0842A8)))
    val Taskbar = Brush.verticalGradient(listOf(Color(0xFF3F7FE6), Color(0xFF1F57C3), Color(0xFF1848A8)))
    val Green = Brush.verticalGradient(listOf(Color(0xFF5CC85C), Color(0xFF2E8B2E)))
    val Button = Brush.verticalGradient(listOf(Color.White, Color(0xFFE3E0D5)))
}

object AllDimens {
    val Radius = 8.dp
    val RadiusWindow = 10.dp
    val Target = 48.dp          // minimum touch target
    val TargetCompact = 40.dp
    val TitleBar = 46.dp
    val Gap = 8.dp
    val CompactWidth = 600.dp   // below this the Desktop uses compact layouts
    val CompactHeight = 400.dp  // Start menu goes single-column below this height
    val StartMenuWidth = 560.dp
    val StartBodyMax = 360.dp
    val CompactTwoColWidth = 420.dp  // compact Start menu uses two columns from this width
    val StartHeaderPadCompact = 4.dp
    val TargetSlim = 36.dp           // low-emphasis footer buttons
}

val AllLabelShadow = Shadow(Color(0xCC000000), Offset(1f, 1.5f), 3f)

enum class AllButtonStyle { Neutral, Primary, Success, Danger }

/** Pressable button with the short scale animation; [compact] for dense rows. */
@Composable
fun AllButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AllButtonStyle = AllButtonStyle.Neutral,
    compact: Boolean = false,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.96f else 1f, tween(80), label = "allButton")
    val (brush, textColor, border) = when (style) {
        AllButtonStyle.Neutral -> Triple(AllBrushes.Button, AllColors.Text, AllColors.BorderStrong)
        AllButtonStyle.Primary -> Triple(SolidColor(AllColors.BluePale), AllColors.Blue, AllColors.Blue)
        AllButtonStyle.Success -> Triple(AllBrushes.Green, Color.White, Color(0xFF1F6B1F))
        AllButtonStyle.Danger -> Triple(SolidColor(Color(0xFFF7E9E7)), AllColors.Danger, AllColors.Danger)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .heightIn(min = if (compact) AllDimens.TargetCompact else AllDimens.Target)
            .clip(RoundedCornerShape(AllDimens.Radius))
            .background(brush)
            .border(1.dp, border, RoundedCornerShape(AllDimens.Radius))
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (compact) 12.dp else 10.dp)
    ) {
        Text(
            text, color = if (enabled) textColor else AllColors.TextDim,
            fontWeight = FontWeight.SemiBold, fontSize = if (compact) 13.sp else 15.sp, maxLines = 1
        )
    }
}

/** Round title-bar button (minimize / close). */
@Composable
fun AllTitleButton(glyph: String, color: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .size(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    ) { Text(glyph, color = Color.White, fontWeight = FontWeight.Bold) }
}

/** XP title bar: gradient, white bold title with shadow, optional icon, minimize + close. */
@Composable
fun AllTitleBar(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onMinimize: (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(AllDimens.TitleBar)
            .background(AllBrushes.Title)
            .padding(start = 10.dp, end = 4.dp)
    ) {
        icon?.let { it(); Spacer(Modifier.width(8.dp)) }
        Text(
            title, color = Color.White,
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, shadow = AllLabelShadow),
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        onMinimize?.let { AllTitleButton("–", Color(0xFF2F6FE0), it) }
        AllTitleButton("✕", Color(0xFFE0442E), onClose)
    }
}

/** Floating window chrome: shadow, rounded border, title bar and body. [titleModifier] carries drag handling. */
@Composable
fun AllWindow(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onMinimize: (() -> Unit)? = null,
    titleModifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    onFocus: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(18.dp, RoundedCornerShape(AllDimens.RadiusWindow))
            .clip(RoundedCornerShape(AllDimens.RadiusWindow))
            .background(AllColors.WindowBody)
            .border(2.dp, AllColors.BlueDark, RoundedCornerShape(AllDimens.RadiusWindow))
            .clickable(indication = null, interactionSource = null, onClick = onFocus)
    ) {
        AllTitleBar(title, onClose, titleModifier, onMinimize, icon)
        content()
    }
}

/** Modal dialog in the same chrome (blue header, beige body). */
@Composable
fun AllDialog(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AllDimens.Radius))
                .background(AllColors.WindowBody)
                .border(1.dp, AllColors.Border, RoundedCornerShape(AllDimens.Radius))
        ) {
            Text(
                title, color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().background(AllColors.Blue).padding(horizontal = 12.dp, vertical = 10.dp)
            )
            Column(modifier = Modifier.padding(12.dp)) { content() }
        }
    }
}

/** White rounded card (list rows, settings groups). [highlight] = selected/active state. */
@Composable
fun AllCard(
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AllDimens.Radius))
            .background(if (highlight) Color(0xFFE8F1E8) else AllColors.Card)
            .border(1.dp, if (highlight) AllColors.Ok else AllColors.Border, RoundedCornerShape(AllDimens.Radius))
            .padding(10.dp),
        content = content
    )
}

/** Enable/disable toggle button (touch-sized). */
@Composable
fun AllToggle(checked: Boolean, onChange: (Boolean) -> Unit, onLabel: String, offLabel: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = AllDimens.TargetCompact)
            .clip(RoundedCornerShape(AllDimens.Radius))
            .background(if (checked) AllColors.Blue else AllColors.Card)
            .border(1.dp, if (checked) AllColors.Blue else AllColors.Border, RoundedCornerShape(AllDimens.Radius))
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp)
    ) {
        Text(if (checked) onLabel else offLabel, color = if (checked) Color.White else AllColors.Text, fontWeight = FontWeight.Bold)
    }
}

/** Single-line text input. */
@Composable
fun AllTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value, onValueChange = onValueChange, singleLine = true,
        textStyle = TextStyle(color = AllColors.Text, fontSize = 15.sp),
        cursorBrush = SolidColor(AllColors.Blue),
        modifier = modifier
            .heightIn(min = AllDimens.Target)
            .clip(RoundedCornerShape(AllDimens.Radius))
            .background(Color.White)
            .border(1.dp, AllColors.Border, RoundedCornerShape(AllDimens.Radius))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = AllColors.TextDim, fontSize = 15.sp)
                inner()
            }
        }
    )
}

/** Progress bar: negative / null = indeterminate. */
@Composable
fun AllProgress(progress: Float?, modifier: Modifier = Modifier) {
    if (progress == null || progress < 0f) {
        LinearProgressIndicator(modifier = modifier.fillMaxWidth(), color = AllColors.Blue)
    } else {
        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = modifier.fillMaxWidth(), color = AllColors.Blue)
    }
}

/** One row of a menu (Start menu etc.): icon slot + label, touch-sized. */
@Composable
fun AllMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 46.dp)
            .padding(horizontal = 12.dp)
    ) {
        icon?.let { it(); Spacer(Modifier.width(12.dp)) }
        Text(label, color = AllColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Vertical menu surface with the XP "beige/blue" split available through [background]. */
@Composable
fun AllMenu(modifier: Modifier = Modifier, background: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.background(background).padding(6.dp), content = content)
}

/** Small tooltip bubble shown while the content is long-pressed / held. */
@Composable
fun AllTooltip(text: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var show by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.combinedClickableSafe(onLongClick = { show = !show })
    ) {
        content()
        if (show) {
            Popup(alignment = Alignment.TopCenter, onDismissRequest = { show = false }) {
                Text(
                    text, color = Color.White, fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xE6202020))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .widthIn(max = 260.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableSafe(onLongClick: () -> Unit): Modifier =
    this.then(Modifier.combinedClickable(interactionSource = null, indication = null, onClick = {}, onLongClick = onLongClick))
