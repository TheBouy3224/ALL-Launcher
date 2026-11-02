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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import com.movtery.zalithlauncher.R
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

/** Flat desktop palette (no XP gradients): navy ground, one blue accent. */
internal val AllDesktopBlue = Color(0xFF2F6FED)
internal val AllDesktopBar = Color(0xFF11151F)
internal val AllDesktopChip = Color(0xFF232B3D)
internal val AllDesktopChipFront = Color(0xFF30415F)
internal val AllDesktopInk = Color(0xFFD5DBE8)

/** Icon cell size for a desktop of [widthDp]: compact phones get smaller cells so more icons fit. */
fun allDesktopIconCell(widthDp: Dp): Pair<Dp, Dp> = if (widthDp < 600.dp) 76.dp to 36.dp else 84.dp to 40.dp

/** Desktop shortcut: [icon] over a shadowed label. [cellWidth]/[iconSize] come from [allDesktopIconCell]. */
@Composable
fun AllDesktopIcon(
    label: String,
    selected: Boolean,
    icon: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    cellWidth: Dp = 84.dp,
    iconSize: Dp = 40.dp,
    onOpen: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(90), label = "iconPress")
    //格子宽度固定，但选中框只包住图标 + 名字（wrap content），不再铺满整格
    Box(modifier = modifier.width(cellWidth), contentAlignment = Alignment.TopCenter) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(max = cellWidth)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AllDesktopBlue.copy(alpha = 0.22f) else Color.Transparent)
            .border(1.dp, if (selected) AllDesktopBlue else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
            .padding(horizontal = 4.dp, vertical = 5.dp)
    ) {
        icon(Modifier.size(iconSize))
        Spacer(Modifier.height(4.dp))
        Text(
            //ICU will not break after a hyphen that precedes a digit ("snapshot-6"), so it broke before the hyphen; a zero-width space after every hyphen makes the break come after it
            text = label.replace("-", "-​"),
            color = Color(0xFFE6EAF2),
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, lineHeight = 14.sp),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    }
}

/** One open-window button on the taskbar. [front] = pressed look. */
class AllTaskChip(val label: String, val front: Boolean, val icon: (@Composable () -> Unit)? = null, val onClick: () -> Unit)

/** Start button: flat blue pill with "ALL", press animation. */
@Composable
fun AllStartButton(open: Boolean, onClick: () -> Unit, compact: Boolean) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(80), label = "startPress")
    val shape = RoundedCornerShape(8.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(start = 8.dp)
            .height(32.dp)
            .scale(scale)
            .clip(shape)
            .background(if (open || pressed) Color(0xFF2559C4) else AllDesktopBlue)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (compact) 14.dp else 18.dp)
    ) {
        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.movtery.zalithlauncher.R.drawable.img_all_logo), "ALL", Modifier.size(26.dp))
    }
}

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
    downloadLabel: String,
    downloadActive: Boolean,
    onDownload: () -> Unit,
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
            .height(44.dp)
            .background(AllDesktopBar)
            .drawBehind { drawLine(Color(0xFF2D3548), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
    ) {
        AllStartButton(startOpen, onStart, compact)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 8.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (downloadActive) AllDesktopChipFront else AllDesktopChip)
                .clickable(onClick = onDownload)
                .padding(horizontal = if (compact) 10.dp else 12.dp)
        ) {
            Icon(painterResource(R.drawable.ic_download), null, tint = AllDesktopInk, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(downloadLabel, color = AllDesktopInk, fontSize = 12.sp, maxLines = 1)
        }

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
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (chip.front) AllDesktopChipFront else AllDesktopChip)
                        .border(1.dp, if (chip.front) AllDesktopBlue else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable(onClick = chip.onClick)
                        .padding(horizontal = if (compact) 8.dp else 12.dp)
                ) {
                    chip.icon?.let { it(); Spacer(Modifier.width(6.dp)) }
                    Text(
                        chip.label, color = AllDesktopInk, fontSize = 12.sp,
                        fontWeight = if (chip.front) FontWeight.Medium else FontWeight.Normal,
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
                .padding(horizontal = if (compact) 10.dp else 14.dp)
        ) {
            val micOn = com.movtery.zalithlauncher.utils.microphone.MicLoopback.enabled.value
            Icon(
                painterResource(if (micOn) R.drawable.ic_all_mic else R.drawable.ic_all_mic_off), null,
                tint = if (micOn) Color(0xFFFF6B6B) else Color(0xFFAEB7CA), modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
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
                        modifier = Modifier.clip(CircleShape).background(Color(0xFFE5484D)).widthIn(min = 20.dp).padding(horizontal = 4.dp, vertical = 2.dp)
                    ) else Text(taskLabel, color = Color.White, fontSize = 13.sp, maxLines = 1)
                }
                Spacer(Modifier.width(6.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(time, color = AllDesktopInk, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (!compact) Text(date, color = Color(0xFF8C97AE), fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}
