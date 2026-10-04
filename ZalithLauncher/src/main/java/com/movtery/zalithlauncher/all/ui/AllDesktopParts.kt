package com.movtery.zalithlauncher.all.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Icon cell size for a desktop of [widthDp]: compact phones get smaller cells so more icons fit. */
fun allDesktopIconCell(widthDp: Dp): Pair<Dp, Dp> = if (widthDp < 600.dp) 88.dp to 48.dp else 104.dp to 56.dp

/** Desktop shortcut: [icon] over a shadowed label. [cellWidth]/[iconSize] come from [allDesktopIconCell]. */
@Composable
fun AllDesktopIcon(
    label: String,
    selected: Boolean,
    icon: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    cellWidth: Dp = 104.dp,
    iconSize: Dp = 56.dp,
    onOpen: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(90), label = "iconPress")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(cellWidth)
            .scale(scale)
            .clip(RoundedCornerShape(AllDimens.RadiusWindow))
            .background(if (selected) Color(0x553A8DFF) else Color.Transparent)
            .border(1.dp, if (selected) Color(0xAAFFFFFF) else Color.Transparent, RoundedCornerShape(AllDimens.RadiusWindow))
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
            .padding(6.dp)
    ) {
        icon(Modifier.size(iconSize))
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, shadow = AllLabelShadow),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .background(Color(0x33000000), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

/** One open-window button on the taskbar. [front] = pressed look. */
class AllTaskChip(val label: String, val front: Boolean, val icon: (@Composable () -> Unit)? = null, val onClick: () -> Unit)

/** XP Start button: four-colour flag + italic "ALL", green pill, press animation. */
@Composable
fun AllStartButton(open: Boolean, onClick: () -> Unit, compact: Boolean) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(80), label = "startPress")
    val shape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxHeight()
            .scale(scale)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(if (open || pressed) Brush.verticalGradient(listOf(Color(0xFF2E8B2E), Color(0xFF1F6B1F))) else AllBrushes.Green)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (compact) 14.dp else 20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { Pane(Color(0xFFF25022)); Pane(Color(0xFF7FBA00)) }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { Pane(Color(0xFF00A4EF)); Pane(Color(0xFFFFB900)) }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "ALL", color = Color.White,
            style = TextStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, fontSize = 20.sp, shadow = AllLabelShadow)
        )
    }
}

@Composable
private fun Pane(color: Color) = Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(color))

/**
 * Taskbar: Start button, scrollable window chips (front one auto-scrolls into view), tray with
 * task badge and clock (date shown only when wide enough). Tray task badge appears only while tasks exist.
 */
@Composable
fun AllTaskbar(
    startOpen: Boolean,
    onStart: () -> Unit,
    chips: List<AllTaskChip>,
    taskCount: Int,
    onTasks: () -> Unit,
    compact: Boolean,
    taskLabel: String,
    modifier: Modifier = Modifier
) {
    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
        while (true) {
            val now = Date()
            time = timeFormat.format(now)
            date = dateFormat.format(now)
            delay(15_000)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(AllBrushes.Taskbar)
    ) {
        AllStartButton(startOpen, onStart, compact)

        val chipsState = rememberLazyListState()
        val frontIndex = chips.indexOfFirst { it.front }
        LaunchedEffect(frontIndex, chips.size) {
            if (frontIndex >= 0) chipsState.animateScrollToItem(frontIndex)
        }
        LazyRow(
            state = chipsState,
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(end = 8.dp),
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            items(chips.size) { i ->
                val chip = chips[i]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .widthIn(max = if (compact) 150.dp else 220.dp)
                        .heightIn(min = AllDimens.TargetCompact)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (chip.front) Color(0xFF1A4BA8) else Color(0xFF3C81F3))
                        .border(1.dp, if (chip.front) Color(0xFF0E3380) else Color(0xFF8CB6FA), RoundedCornerShape(6.dp))
                        .clickable(onClick = chip.onClick)
                        .padding(horizontal = if (compact) 8.dp else 12.dp)
                ) {
                    chip.icon?.let { it(); Spacer(Modifier.width(6.dp)) }
                    Text(
                        chip.label, color = Color.White,
                        fontWeight = if (chip.front) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Tray
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .background(Brush.verticalGradient(listOf(Color(0xFF14A0F0), Color(0xFF0F7FD8))))
                .border(1.dp, Color(0xFF0B5CD8))
                .padding(horizontal = if (compact) 10.dp else 16.dp)
        ) {
            if (taskCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(AllDimens.Radius))
                        .clickable(onClick = onTasks)
                        .heightIn(min = AllDimens.Target)
                        .padding(horizontal = 6.dp)
                ) {
                    if (compact) Text(
                        taskCount.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clip(CircleShape).background(AllColors.Danger).widthIn(min = 20.dp).padding(horizontal = 4.dp, vertical = 2.dp)
                    ) else Text(taskLabel, color = Color.White, fontSize = 13.sp, maxLines = 1)
                }
                Spacer(Modifier.width(6.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(time, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (!compact) Text(date, color = Color(0xCCFFFFFF), fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}
