package com.movtery.zalithlauncher.all.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.movtery.zalithlauncher.R
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.layout.onGloballyPositioned
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
/** Every ALL colour reads the active palette ([AllTheme]): Luna (light, unchanged) or Midnight (dark). */
object AllColors {
    val Blue: Color get() = AllTheme.current.Blue
    val BlueDark: Color get() = AllTheme.current.BlueDark
    val BlueMid: Color get() = AllTheme.current.BlueMid
    val BlueLight: Color get() = AllTheme.current.BlueLight
    val BluePale: Color get() = AllTheme.current.BluePale
    val Green: Color get() = AllTheme.current.Green
    val Danger: Color get() = AllTheme.current.Danger
    val Ok: Color get() = AllTheme.current.Ok
    val WindowBody: Color get() = AllTheme.current.WindowBody
    val Card: Color get() = AllTheme.current.Card
    val Border: Color get() = AllTheme.current.Border
    val BorderStrong: Color get() = AllTheme.current.BorderStrong
    val Text: Color get() = AllTheme.current.Text
    val TextDim: Color get() = AllTheme.current.TextDim
    val MenuRight: Color get() = AllTheme.current.MenuRight
    val MenuRightBorder: Color get() = AllTheme.current.MenuRightBorder
    val Warning: Color get() = AllTheme.current.Warning
    val Disabled: Color get() = AllTheme.current.Disabled
    val TitleUnfocused: Color get() = AllTheme.current.TitleUnfocused
    val FooterText: Color get() = AllTheme.current.FooterText
}

/** ALL gradients; read the active palette like [AllColors]. */
object AllBrushes {
    val Title: Brush get() = AllTheme.current.TitleBrush
    val Taskbar: Brush get() = AllTheme.current.TaskbarBrush
    val Green: Brush get() = AllTheme.current.GreenBrush
    val Button: Brush get() = AllTheme.current.ButtonBrush
}

object AllDimens {
    val Radius = 8.dp
    val RadiusWindow = 10.dp
    val Target = 48.dp          // minimum touch target
    val TargetCompact = 40.dp
    val TitleBar = 44.dp
    val TitleBarCompact = 36.dp
    val TitleButton = 28.dp
    val DialogMaxWidth = 480.dp
    val Gap = 8.dp
    val CompactWidth = 600.dp   // below this the Desktop uses compact layouts
    val CompactHeight = 400.dp  // Start menu goes single-column below this height
    val StartMenuWidth = 560.dp
    val StartBodyMax = 360.dp
    val StartRowHeight = 54.dp       // Start menu row (touch first)
    val CompactTwoColWidth = 420.dp  // compact Start menu uses two columns from this width
    val StartHeaderPadCompact = 4.dp
    val TargetSlim = 36.dp
    val ChipVisual = 36.dp           // visual height of chips/dense buttons (hit area stays Target)
    val WindowMargin = 24.dp         // gap between floating window and desktop edge (wide)
    val WindowMarginCompact = 8.dp           // low-emphasis footer buttons
}

val AllLabelShadow = Shadow(Color(0xCC000000), Offset(1f, 1.5f), 3f)

/** Background of text fields / selects / unchecked boxes (V3 flat navy chip). */
val AllFieldBg: Color get() = Color(0xFF232B3D)

enum class AllButtonStyle { Neutral, Primary, Success, Danger }

/** Role of the button being composed inside an [AlertDialog] action row (set by the dialog shim). */
val LocalAllButtonStyle = compositionLocalOf { AllButtonStyle.Neutral }

/** Maps a legacy per-window accent colour to a style: reddish = Danger, bluish = Primary, anything else Neutral. */
fun allStyleForColor(c: Color): AllButtonStyle = when {
    c.red > 0.55f && c.green < 0.45f && c.blue < 0.45f -> AllButtonStyle.Danger
    c.blue > 0.55f && c.red < 0.45f -> AllButtonStyle.Primary
    else -> AllButtonStyle.Neutral
}

/** Button surface with composable content: bevel gradient, 1dp border, press scale; disabled = flat, faded. */
@Composable
fun AllButtonBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AllButtonStyle = AllButtonStyle.Neutral,
    compact: Boolean = false,
    enabled: Boolean = true,
    fill: Boolean = false,   // stretch the visual button to the (weighted / fixed) slot width
    padH: androidx.compose.ui.unit.Dp? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.96f else 1f, tween(AllMotion.Press), label = "allButton")
    val (brush, textColor, border) = when {
        !enabled -> Triple(SolidColor(AllColors.Disabled), AllColors.TextDim, AllColors.Border.copy(alpha = 0.6f))
        style == AllButtonStyle.Neutral -> Triple(AllBrushes.Button, AllColors.Text, AllColors.BorderStrong)
        style == AllButtonStyle.Primary -> Triple(SolidColor(AllColors.BluePale), AllColors.BlueLight, AllColors.Blue)
        style == AllButtonStyle.Success -> Triple(AllBrushes.Green, Color.White, Color(0xFF1F6B1F))
        else -> Triple(SolidColor(Color(0xFF3A2128)), AllColors.Danger, Color(0xFF8A3B45))
    }
    val dense = compact || LocalAllCompact.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .heightIn(min = AllDimens.Target)   // hit area is always >= 48dp
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .then(if (fill) Modifier.fillMaxWidth() else Modifier)
                .heightIn(min = if (dense) AllDimens.ChipVisual else AllDimens.Target)
                .clip(RoundedCornerShape(AllDimens.Radius))
                .background(brush)
                .border(1.dp, border, RoundedCornerShape(AllDimens.Radius))
                .padding(horizontal = padH ?: if (dense) 12.dp else 14.dp)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides textColor) {
                androidx.compose.material3.ProvideTextStyle(TextStyle(fontWeight = FontWeight.SemiBold, fontSize = if (dense) 13.sp else 15.sp), content = { Row(verticalAlignment = Alignment.CenterVertically) { content() } })
            }
        }
    }
}

/** Pressable button with the short scale animation; [compact] for dense rows. */
@Composable
fun AllButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AllButtonStyle = AllButtonStyle.Neutral,
    compact: Boolean = false,
    enabled: Boolean = true,
    fill: Boolean = false,
    icon: Int? = null
) = AllButtonBox(onClick, modifier, style, compact, enabled, fill, padH = if (icon != null) 8.dp else null) {
    if (icon != null) {
        androidx.compose.material3.Icon(painterResource(icon), null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, maxLines = 1, softWrap = false, fontSize = 13.sp)
    } else Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Drop-in for Material3 `TextButton`: renders an [AllButtonBox] whose style comes from the surrounding dialog slot (Neutral elsewhere). */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) = AllButtonBox(onClick, modifier, LocalAllButtonStyle.current, compact = true, enabled = enabled, content = content)

/** Drop-in for Material3 `OutlinedTextField` (the subset the ALL windows use): label above, XP-blue focus border, error text below. */
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    label: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when { isError -> AllColors.Danger; focused -> AllColors.Blue; else -> AllColors.Border }
    Column(modifier) {
        label?.let { androidx.compose.material3.ProvideTextStyle(TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AllColors.TextDim), it) }
        BasicTextField(
            value = value, onValueChange = onValueChange, enabled = enabled, singleLine = singleLine,
            keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            textStyle = TextStyle(color = if (enabled) AllColors.Text else AllColors.TextDim, fontSize = 15.sp),
            cursorBrush = SolidColor(AllColors.Blue),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (label != null) 4.dp else 0.dp)
                .heightIn(min = AllDimens.Target)
                .clip(RoundedCornerShape(AllDimens.Radius))
                .background(if (enabled) AllFieldBg else AllColors.Disabled)
                .border(if (focused) 2.dp else 1.dp, borderColor, RoundedCornerShape(AllDimens.Radius))
                .onFocusChanged { focused = it.isFocused }
                .padding(horizontal = 12.dp, vertical = 14.dp)
        )
        supportingText?.let { androidx.compose.material3.ProvideTextStyle(TextStyle(fontSize = 12.sp, color = if (isError) AllColors.Danger else AllColors.TextDim), it) }
    }
}

/** Choice chip for segmented option groups: selected = XP-blue fill; 48dp hit area, 36dp visual. */
@Composable
fun AllChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.heightIn(min = AllDimens.Target).clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = AllDimens.ChipVisual)
                .clip(RoundedCornerShape(AllDimens.Radius))
                .background(if (selected) AllColors.Blue else if (enabled) AllColors.Card else AllColors.Disabled)
                .border(1.dp, if (selected) AllColors.Blue else AllColors.Border, RoundedCornerShape(AllDimens.Radius))
                .padding(horizontal = 12.dp)
        ) {
            Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1,
                color = if (selected) Color.White else if (enabled) AllColors.Text else AllColors.TextDim)
        }
    }
}

/** Select / dropdown trigger field: current value + chevron. Pair with [DropdownMenu]. */
@Composable
fun AllSelect(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = AllDimens.Target)
            .clip(RoundedCornerShape(AllDimens.Radius))
            .background(AllFieldBg)
            .border(1.dp, AllColors.BorderStrong, RoundedCornerShape(AllDimens.Radius))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
    ) {
        Text(text, color = AllColors.Text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        androidx.compose.material3.Icon(painterResource(R.drawable.ic_keyboard_arrow_down), null, tint = AllColors.Blue, modifier = Modifier.size(20.dp))
    }
}

/** Drop-in for Material3 `Switch`: XP-blue pill track, white thumb. */
@Composable
fun Switch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val x by animateDpAsState(if (checked) 22.dp else 2.dp, tween(AllMotion.Focus), label = "switchThumb")
    Box(
        modifier = modifier
            .heightIn(min = AllDimens.Target).widthIn(min = AllDimens.Target)
            .clickable(enabled = enabled && onCheckedChange != null) { onCheckedChange?.invoke(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(width = 46.dp, height = 26.dp).clip(RoundedCornerShape(13.dp))
                .background(if (!enabled) AllColors.Disabled else if (checked) AllColors.Blue else AllColors.BorderStrong.copy(alpha = 0.55f))
                .border(1.dp, if (checked) AllColors.BlueDark else AllColors.BorderStrong, RoundedCornerShape(13.dp))
        ) { Box(Modifier.offset(x = x, y = 2.dp).size(20.dp).clip(CircleShape).background(Color.White)) }
    }
}

/** Drop-in for Material3 `Checkbox`: rounded square, XP-blue when checked, vector tick. */
@Composable
fun Checkbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier.size(AllDimens.Target).clickable(enabled = enabled && onCheckedChange != null) { onCheckedChange?.invoke(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(22.dp).clip(RoundedCornerShape(5.dp))
                .background(if (checked) AllColors.Blue else AllFieldBg)
                .border(1.5.dp, if (checked) AllColors.BlueDark else AllColors.BorderStrong, RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Canvas(Modifier.size(12.dp)) {
                val w = size.width; val st = 2.dp.toPx()
                drawLine(Color.White, Offset(0f, w * 0.55f), Offset(w * 0.4f, w * 0.95f), st)
                drawLine(Color.White, Offset(w * 0.4f, w * 0.95f), Offset(w, w * 0.1f), st)
            }
        }
    }
}

/** Drop-in for Material3 `Slider` (value/range/finished subset) in ALL blue. */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null
) = androidx.compose.material3.Slider(
    value = value, onValueChange = onValueChange, modifier = modifier, valueRange = valueRange, onValueChangeFinished = onValueChangeFinished,
    colors = androidx.compose.material3.SliderDefaults.colors(
        thumbColor = AllColors.Blue, activeTrackColor = AllColors.Blue,
        inactiveTrackColor = AllColors.BorderStrong.copy(alpha = 0.4f)
    )
)

/** Drop-in for Material3 `DropdownMenu`: white rounded surface with XP border. */
@Composable
fun DropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    androidx.compose.material3.DropdownMenu(
        expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier,
        shape = RoundedCornerShape(AllDimens.Radius), containerColor = AllColors.Card,
        border = BorderStroke(1.dp, AllColors.BorderStrong), content = content
    )

/** Drop-in for Material3 `DropdownMenuItem`: 48dp row, XP-blue text on press. */
@Composable
fun DropdownMenuItem(text: @Composable () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) =
    androidx.compose.material3.DropdownMenuItem(
        text = text, onClick = onClick, modifier = modifier,
        colors = androidx.compose.material3.MenuDefaults.itemColors(textColor = AllColors.Text)
    )

/** Small caps section label inside a window body. */
@Composable
fun AllSectionHeader(text: String, modifier: Modifier = Modifier) =
    Text(text.uppercase(), color = AllColors.TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 4.dp, top = 10.dp, bottom = 4.dp))

/** 1dp neutral divider. */
@Composable
fun AllDivider(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(AllColors.Border.copy(alpha = 0.7f)))

/** List row: [selected] = XP-blue tint + border; pressed = slightly darker. Touch height >= 48dp. */
@Composable
fun AllListRow(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(AllDimens.Radius)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AllDimens.Target)
            .clip(shape)
            .background(if (selected) AllColors.BluePale else AllColors.Card)
            .border(1.dp, if (selected) AllColors.Blue else AllColors.Border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = if (LocalAllCompact.current) 2.dp else 6.dp),
        content = content
    )
}

/** Segmented tabs: one rounded strip, selected tab filled XP blue. */
@Composable
fun AllTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AllDimens.Radius)
    Row(modifier.clip(shape).border(1.dp, AllColors.BorderStrong, shape)) {
        labels.forEachIndexed { i, l ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).heightIn(min = AllDimens.Target)
                    .background(if (i == selected) AllColors.Blue else AllColors.Card)
                    .clickable { onSelect(i) }.padding(horizontal = 8.dp)
            ) { Text(l, color = if (i == selected) Color.White else AllColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

/** Compact desktop (set by the Desktop): title bars shrink to save vertical space. */
val LocalAllCompact = compositionLocalOf { false }
/** Whether the window being composed is the focused one (set per window layer by the Desktop). */
val LocalAllWindowFocused = compositionLocalOf { true }

/** Central motion tokens: short and subtle, never flashy. */
object AllMotion {
    const val WindowIn = 140
    const val WindowOut = 120
    const val Dialog = 120
    const val Focus = 100
    const val Press = 80
    val windowEnter: EnterTransition = fadeIn(tween(WindowIn)) + scaleIn(spring(dampingRatio = 0.62f, stiffness = 520f), initialScale = 0.9f)
    val windowExit: ExitTransition = fadeOut(tween(WindowOut)) + scaleOut(tween(WindowOut + 20), targetScale = 0.92f)
}

enum class AllGlyph { Minimize, Maximize, Close }

/**
 * Title-bar button (minimize / maximize / close): 48dp-wide hit area, small vector-drawn glyph (no font dependence).
 * Long-press shows [hint] as a tooltip.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AllTitleButton(glyph: AllGlyph, onClick: () -> Unit, hint: String? = null, modifier: Modifier = Modifier) {
    val close = glyph == AllGlyph.Close
    var tip by remember { mutableStateOf(false) }
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(AllDimens.Target)
            .fillMaxHeight()
            .combinedClickable(interactionSource = src, indication = null, onClick = onClick, onLongClick = { if (hint != null) tip = true })
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AllDimens.TitleButton)
                .clip(RoundedCornerShape(6.dp))
                .background(if (pressed) (if (close) Color(0xFFE5484D) else Color(0x40FFFFFF)) else Color.Transparent)
        ) {
            Canvas(Modifier.size(12.dp)) {
                val w = size.width; val st = 2.dp.toPx()
                when (glyph) {
                    AllGlyph.Minimize -> drawLine(AllDesktopInk, Offset(0f, w - st), Offset(w, w - st), st)
                    AllGlyph.Maximize -> drawRect(AllDesktopInk, Offset(st / 2, st / 2), androidx.compose.ui.geometry.Size(w - st, w - st), style = androidx.compose.ui.graphics.drawscope.Stroke(st))
                    AllGlyph.Close -> { val c = if (pressed) Color.White else AllDesktopInk; drawLine(c, Offset(0f, 0f), Offset(w, w), st); drawLine(c, Offset(w, 0f), Offset(0f, w), st) }
                }
            }
        }
        if (tip && hint != null) Popup(alignment = Alignment.BottomCenter, onDismissRequest = { tip = false }) {
            Text(hint, color = Color.White, fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xE6202020)).padding(horizontal = 10.dp, vertical = 6.dp))
        }
    }
}

/** XP title bar: gradient (muted when the window is not focused), white bold title with shadow, optional icon, window buttons. */
@Composable
fun AllTitleBar(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onMinimize: (() -> Unit)? = null,
    onMaximize: (() -> Unit)? = null,   // ponytail: hook for maximize/snap; Desktop does not pass it yet
    icon: @Composable (() -> Unit)? = null
) {
    val focused = LocalAllWindowFocused.current
    val dim by animateFloatAsState(if (focused) 0f else 1f, tween(AllMotion.Focus), label = "titleDim")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(if (LocalAllCompact.current) AllDimens.TitleBarCompact else AllDimens.TitleBar)
            .background(androidx.compose.ui.graphics.lerp(Color(0xFF223049), Color(0xFF111622), dim))
            .padding(start = 10.dp, end = 2.dp)
    ) {
        icon?.let { it(); Spacer(Modifier.width(8.dp)) }
        Text(
            title, color = AllDesktopInk.copy(alpha = 1f - 0.45f * dim),
            style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        onMinimize?.let { AllTitleButton(AllGlyph.Minimize, it, stringResource(R.string.all_window_minimize)) }
        onMaximize?.let { AllTitleButton(AllGlyph.Maximize, it) }
        AllTitleButton(AllGlyph.Close, onClose, stringResource(R.string.generic_close))
    }
}

/** On-screen bounds (px, screen coordinates) of the focused desktop window; dialogs opened from it centre on it */
object AllFocusedWindowBounds {
    var rect by androidx.compose.runtime.mutableStateOf<androidx.compose.ui.geometry.Rect?>(null)
}

/** Floating window chrome: soft shadow, rounded border (lighter when unfocused), title bar and body. [titleModifier] carries drag handling. */
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
    val focused = LocalAllWindowFocused.current
    val shadow by animateDpAsState(if (focused) 16.dp else 6.dp, tween(AllMotion.Focus), label = "winShadow")
    val border by animateColorAsState(if (focused) AllDesktopBlue else Color(0xFF2D3548), tween(AllMotion.Focus), label = "winBorder")
    Column(
        modifier = modifier
            .then(Modifier.onGloballyPositioned { if (focused) AllFocusedWindowBounds.rect = androidx.compose.ui.geometry.Rect(it.localToScreen(Offset.Zero), it.size.toSize()) })
            .shadow(shadow, RoundedCornerShape(AllDimens.RadiusWindow))
            .clip(RoundedCornerShape(AllDimens.RadiusWindow))
            .background(AllColors.WindowBody)
            .border(1.dp, border, RoundedCornerShape(AllDimens.RadiusWindow))
            .clickable(indication = null, interactionSource = null, onClick = onFocus)
    ) {
        AllTitleBar(title, onClose, titleModifier, onMinimize, null, icon)
        content()
    }
}

enum class AllDialogKind { Info, Warning, Error }

/**
 * Modal dialog: coloured header by [kind] (omitted when [title] is blank), scrollable body, optional fixed [actions] row
 * (right-aligned, dismiss first, main action last). Width capped for tablets, fills phones; short fade/scale on appear.
 */
@Composable
fun AllDialog(
    title: String,
    onDismissRequest: () -> Unit,
    kind: AllDialogKind = AllDialogKind.Info,
    actions: (@Composable RowScope.() -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null,   // composable header (AlertDialog shim); replaces [title]
    content: @Composable ColumnScope.() -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val t by animateFloatAsState(if (shown) 1f else 0f, tween(AllMotion.Dialog), label = "dlgIn")
    val header = when (kind) {
        AllDialogKind.Info -> Color(0xFF223049)
        AllDialogKind.Warning -> AllColors.Warning
        AllDialogKind.Error -> AllColors.Danger
    }
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        //the dialog window shrinks above the Android keyboard (insets alone are not delivered to dialogs in full-screen hosts like Controls Studio)
        (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            ?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        Column(
            modifier = Modifier
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = AllDimens.DialogMaxWidth)
                .fillMaxWidth()
                .graphicsLayer { alpha = t; scaleX = 0.96f + 0.04f * t; scaleY = 0.96f + 0.04f * t }
                .shadow(14.dp, RoundedCornerShape(AllDimens.Radius))
                .clip(RoundedCornerShape(AllDimens.Radius))
                .background(AllColors.WindowBody)
                .border(1.dp, AllColors.BorderStrong, RoundedCornerShape(AllDimens.Radius))
        ) {
            if (titleContent != null) Box(Modifier.fillMaxWidth().background(header).padding(horizontal = 14.dp, vertical = 10.dp)) {
                androidx.compose.material3.ProvideTextStyle(TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, shadow = AllLabelShadow), titleContent)
            } else if (title.isNotBlank()) Text(
                title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = AllLabelShadow),
                modifier = Modifier.fillMaxWidth().background(header).padding(horizontal = 14.dp, vertical = 10.dp)
            )
            //dialog bodies often use bare Text(): give them the palette's text colour (the Material default is near-black)
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides AllColors.Text) {
                Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(14.dp)) { content() }
            }
            actions?.let {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AllDimens.Gap, Alignment.End), verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                ) { it() }
            }
        }
    }
}

/**
 * Drop-in for Material3 `AlertDialog` (same parameter names) so ALL windows get the ALL dialog language without call-site
 * rewrites. Passed-in Material TextButtons pick up ALL blue through the local colour scheme.
 */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    destructive: Boolean = false,   // confirm renders as Danger (callers that used a red confirm text)
) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = androidx.compose.material3.MaterialTheme.colorScheme.copy(primary = AllColors.Blue, onSurface = AllColors.Text, onSurfaceVariant = AllColors.Text)
    ) {
        AllDialog(
            title = "", onDismissRequest = onDismissRequest,
            actions = {
                androidx.compose.runtime.CompositionLocalProvider(LocalAllButtonStyle provides AllButtonStyle.Neutral) { dismissButton?.invoke() }
                androidx.compose.runtime.CompositionLocalProvider(LocalAllButtonStyle provides (if (destructive) AllButtonStyle.Danger else AllButtonStyle.Primary)) { confirmButton() }
            },
            titleContent = title,
            content = {
                text?.let { androidx.compose.material3.ProvideTextStyle(TextStyle(fontSize = 14.sp, color = AllColors.Text), it) }
            }
        )
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
            .background(if (highlight) AllColors.BluePale else AllColors.Card)
            .border(1.dp, if (highlight) AllColors.Blue else AllColors.Border, RoundedCornerShape(AllDimens.Radius))
            .padding(if (LocalAllCompact.current) 6.dp else 10.dp),
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
            .background(AllFieldBg)
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
fun AllMenu(modifier: Modifier = Modifier, background: Color = AllColors.Card, content: @Composable ColumnScope.() -> Unit) {
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
