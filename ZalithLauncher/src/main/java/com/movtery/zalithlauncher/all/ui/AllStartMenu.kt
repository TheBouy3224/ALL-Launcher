package com.movtery.zalithlauncher.all.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R

enum class AllStartSection { PINNED, TOOLS, SYSTEM }

/** One Start-menu row. The Desktop builds the list (registry apps + built-in tools); the menu only presents it. */
class AllStartEntry(val id: String, val label: String, val iconRes: Int, val section: AllStartSection, val action: () -> Unit)

/** Session-only "recent" list (most recent first). ponytail: not persisted; move to AllSettings when a backend exists. */
object AllStartRecents {
    const val MAX = 3
    val ids = mutableStateListOf<String>()
    fun record(id: String) { ids.remove(id); ids.add(0, id); while (ids.size > MAX) ids.removeAt(ids.lastIndex) }
}

/** ALL brand mark: four skewed panes (XP flag nod) in ALL colours, drawn as vectors so it never depends on fonts. */
@Composable
fun AllLogo(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val gap = w * 0.06f; val pw = (w - gap) / 2; val ph = (h - gap) / 2; val skew = w * 0.1f
        fun pane(x: Float, y: Float, c: Color) = drawPath(Path().apply {
            moveTo(x + skew, y); lineTo(x + pw + skew, y); lineTo(x + pw, y + ph); lineTo(x, y + ph); close()
        }, c)
        pane(0f, 0f, Color(0xFFF0B044)); pane(pw + gap, 0f, AllColors.BlueLight)
        pane(0f, ph + gap, AllColors.Ok); pane(pw + gap, ph + gap, Color.White)
    }
}

@Composable
private fun StartRow(entry: AllStartEntry, onPick: (AllStartEntry) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (pressed) AllDesktopBlue else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null) { onPick(entry) }
            .heightIn(min = AllDimens.StartRowHeight)
            .padding(horizontal = 8.dp)
    ) {
        //icon tile: a soft rounded square so rows read as touch targets, not bare glyphs
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(if (pressed) Color(0x33FFFFFF) else AllDesktopChip)
        ) {
            Icon(painterResource(entry.iconRes), null, tint = if (pressed) Color.White else AllDesktopInk, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(entry.label, color = if (pressed) Color.White else AllDesktopInk, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
    }
}

/** Dark search field for the flat Start menu (the shared AllTextField is the light one). */
@Composable
private fun StartSearch(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier) {
    BasicTextField(
        value = value, onValueChange = onValueChange, singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
        cursorBrush = SolidColor(AllDesktopBlue),
        modifier = modifier.heightIn(min = AllDimens.Target).clip(RoundedCornerShape(10.dp)).background(AllDesktopChip).padding(horizontal = 12.dp, vertical = 14.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = Color(0xFF8C97AE), fontSize = 15.sp)
                inner()
            }
        }
    )
}

@Composable
private fun SectionLabel(text: String, upper: Boolean = true) =
    Text(if (upper) text.uppercase() else text, color = Color(0xFF8C97AE), fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 2.dp))

@Composable
private fun Rows(entries: List<AllStartEntry>, onPick: (AllStartEntry) -> Unit) = entries.forEach { StartRow(it, onPick) }

/** Pinned shortcut tile (icon over label). */
@Composable
private fun StartTile(entry: AllStartEntry, modifier: Modifier, onPick: (AllStartEntry) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed) AllDesktopBlue else AllDesktopChip)
            .clickable(interactionSource = interaction, indication = null) { onPick(entry) }
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Icon(painterResource(entry.iconRes), null, tint = if (pressed) Color.White else AllDesktopInk, modifier = Modifier.size(24.dp))
        Text(entry.label, color = AllDesktopInk, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}

/**
 * ALL Start menu: one small column floating above the ALL button.
 * Search, 3 pinned tiles (download / mods / controls), short list (games / settings / wallpaper / UI size),
 * "More" for everything else, account footer. Search filters every entry by label.
 */
@Composable
fun AllStartMenu(
    userName: String,
    entries: List<AllStartEntry>,
    maxMenuHeight: Dp,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var showMore by remember { mutableStateOf(false) }
    val pick: (AllStartEntry) -> Unit = { AllStartRecents.record(it.id); onClose(); it.action() }
    val shape = RoundedCornerShape(14.dp)
    val tileIds = listOf("download", "mods", "controls")
    val listIds = listOf("instances", "settings", "wallpaper", "uiscale")
    val tiles = tileIds.mapNotNull { id -> entries.find { it.id == id } }
    val main = listIds.mapNotNull { id -> entries.find { it.id == id } }
    val more = entries.filter { it.id !in tileIds && it.id !in listIds && it.id != "accounts" }
    val accounts = entries.find { it.id == "accounts" }

    BoxWithConstraints(modifier = modifier.padding(start = 6.dp, bottom = 4.dp).imePadding()) {
        val menuMax = minOf(maxMenuHeight, maxHeight) - 4.dp
        Column(
            modifier = Modifier
                .width(minOf(AllDimens.StartMenuWidth * 0.62f, maxWidth))
                .heightIn(max = menuMax)
                .shadow(10.dp, shape).clip(shape)
                .background(Color(0xFF161B28))
                .border(1.dp, Color(0xFF2D3548), shape)
                .clickable(indication = null, interactionSource = null) {}
        ) {
            StartSearch(query, { query = it }, stringResource(R.string.all_start_search), Modifier.fillMaxWidth().padding(10.dp))
            val q = query.trim()
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 6.dp)) {
                if (q.isNotEmpty()) {
                    val hits = entries.sortedBy { it.label.lowercase() }.filter { it.label.contains(q, ignoreCase = true) }
                    if (hits.isEmpty()) Text(stringResource(R.string.all_start_no_results, q), color = AllDesktopInk, fontSize = 14.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(12.dp))
                    else Rows(hits, pick)
                } else {
                    if (tiles.isNotEmpty() && !showMore) Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tiles.forEach { StartTile(it, Modifier.weight(1f), pick) }
                    }
                    if (showMore) Rows(more, pick) else Rows(main, pick)
                    if (more.isNotEmpty()) Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { showMore = !showMore }.heightIn(min = AllDimens.Target).padding(horizontal = 10.dp)
                    ) {
                        Text(if (showMore) "‹  " + stringResource(R.string.all_start_back) else stringResource(R.string.all_start_all_programs) + "  ›",
                            color = Color(0xFF6F9BFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            // Footer: account
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1B2232))
                    .clickable(enabled = accounts != null) { accounts?.let(pick) }
                    .heightIn(min = AllDimens.Target).padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(painterResource(R.drawable.ic_person_outlined), null, tint = AllDesktopInk, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(userName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.all_unofficial_short), color = Color(0xFF8A93A6), fontSize = 10.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
