package com.movtery.zalithlauncher.all.ui

import androidx.compose.animation.EnterTransition
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
    val Warning = Color(0xFFB7791F)
    val Disabled = Color(0xFFE9E7DE)
    val TitleUnfocused = Color(0xFF8A94A6)
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
    val TitleBar = 44.dp
    val TitleBarCompact = 36.dp
    val TitleButton = 28.dp
    val DialogMaxWidth = 480.dp
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
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.96f else 1f, tween(AllMotion.Press), label = "allButton")
    val (brush, textColor, border) = when {
        !enabled -> Triple(SolidColor(AllColors.Disabled), AllColors.TextDim, AllColors.Border.copy(alpha = 0.6f))
        style == AllButtonStyle.Neutral -> Triple(AllBrushes.Button, AllColors.Text, AllColors.BorderStrong)
        style == AllButtonStyle.Primary -> Triple(SolidColor(AllColors.BluePale), AllColors.Blue, AllColors.Blue)
        style == AllButtonStyle.Success -> Triple(AllBrushes.Green, Color.White, Color(0xFF1F6B1F))
        else -> Triple(SolidColor(Color(0xFFF7E9E7)), AllColors.Danger, AllColors.Danger)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .scale(scale)
            .heightIn(min = if (compact) AllDimens.TargetCompact else AllDimens.Target)
            .clip(RoundedCornerShape(AllDimens.Radius))
            .background(brush)
            .border(1.dp, border, RoundedCornerShape(AllDimens.Radius))
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (compact) 12.dp else 14.dp)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides textColor) {
            androidx.compose.material3.ProvideTextStyle(TextStyle(fontWeight = FontWeight.SemiBold, fontSize = if (compact) 13.sp else 15.sp), content = { Row(verticalAlignment = Alignment.CenterVertically) { content() } })
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
    enabled: Boolean = true
) = AllButtonBox(onClick, modifier, style, compact, enabled) { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) }

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
                .background(if (enabled) Color.White else AllColors.Disabled)
                .border(if (focused) 2.dp else 1.dp, borderColor, RoundedCornerShape(AllDimens.Radius))
                .onFocusChanged { focused = it.isFocused }
                .padding(horizontal = 12.dp, vertical = 14.dp)
        )
        supportingText?.let { androidx.compose.material3.ProvideTextStyle(TextStyle(fontSize = 12.sp, color = if (isError) AllColors.Danger else AllColors.TextDim), it) }
    }
}

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
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(AllDimens.Target)
            .fillMaxHeight()
            .combinedClickable(interactionSource = null, indication = null, onClick = onClick, onLongClick = { if (hint != null) tip = true })
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AllDimens.TitleButton)
                .clip(RoundedCornerShape(6.dp))
                .background(if (close) Color(0xFFD9503A) else Color(0x33FFFFFF))
                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
        ) {
            Canvas(Modifier.size(12.dp)) {
                val w = size.width; val st = 2.dp.toPx()
                when (glyph) {
                    AllGlyph.Minimize -> drawLine(Color.White, Offset(0f, w - st), Offset(w, w - st), st)
                    AllGlyph.Maximize -> drawRect(Color.White, Offset(st / 2, st / 2), androidx.compose.ui.geometry.Size(w - st, w - st), style = androidx.compose.ui.graphics.drawscope.Stroke(st))
                    AllGlyph.Close -> { drawLine(Color.White, Offset(0f, 0f), Offset(w, w), st); drawLine(Color.White, Offset(w, 0f), Offset(0f, w), st) }
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
            .background(AllBrushes.Title)
            .background(AllColors.TitleUnfocused.copy(alpha = 0.55f * dim))
            .padding(start = 10.dp, end = 2.dp)
    ) {
        icon?.let { it(); Spacer(Modifier.width(8.dp)) }
        Text(
            title, color = Color.White.copy(alpha = 1f - 0.2f * dim),
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, shadow = AllLabelShadow),
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        onMinimize?.let { AllTitleButton(AllGlyph.Minimize, it, stringResource(R.string.all_window_minimize)) }
        onMaximize?.let { AllTitleButton(AllGlyph.Maximize, it) }
        AllTitleButton(AllGlyph.Close, onClose, stringResource(R.string.generic_close))
    }
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
    val border by animateColorAsState(if (focused) AllColors.BlueDark else AllColors.BorderStrong, tween(AllMotion.Focus), label = "winBorder")
    Column(
        modifier = modifier
            .shadow(shadow, RoundedCornerShape(AllDimens.RadiusWindow))
            .clip(RoundedCornerShape(AllDimens.RadiusWindow))
            .background(AllColors.WindowBody)
            .border(2.dp, border, RoundedCornerShape(AllDimens.RadiusWindow))
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
        AllDialogKind.Info -> AllColors.Blue
        AllDialogKind.Warning -> AllColors.Warning
        AllDialogKind.Error -> AllColors.Danger
    }
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
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
            Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(14.dp)) { content() }
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
