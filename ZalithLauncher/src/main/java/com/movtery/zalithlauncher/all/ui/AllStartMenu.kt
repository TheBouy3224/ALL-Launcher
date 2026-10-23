package com.movtery.zalithlauncher.all.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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

enum class AllStartSection { PINNED, SYSTEM }

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
            .background(if (pressed) AllColors.Blue else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null) { onPick(entry) }
            .heightIn(min = AllDimens.Target)
            .padding(horizontal = 10.dp)
    ) {
        Icon(painterResource(entry.iconRes), null, tint = if (pressed) Color.White else AllColors.BlueMid, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(entry.label, color = if (pressed) Color.White else AllColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
    }
}

@Composable
private fun SectionLabel(text: String, upper: Boolean = true) =
    Text(if (upper) text.uppercase() else text, color = AllColors.TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 2.dp))

@Composable
private fun Rows(entries: List<AllStartEntry>, onPick: (AllStartEntry) -> Unit) = entries.forEach { StartRow(it, onPick) }

/**
 * ALL Start menu: user header, search, pinned + recent (main column), system tools, All programs, footer with Classic.
 * Wide: two columns that scroll independently. Compact (narrow or short): one scrolling column, header/search/footer fixed.
 * Search filters every entry by label; "All programs" swaps the pinned list for the full alphabetical list.
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
    var showAll by remember { mutableStateOf(false) }
    val pick: (AllStartEntry) -> Unit = { AllStartRecents.record(it.id); onClose(); it.action() }
    val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)

    val allProgramsToggle: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { showAll = !showAll }.heightIn(min = AllDimens.Target).padding(horizontal = 16.dp)
        ) {
            Text(if (showAll) "‹  " + stringResource(R.string.all_start_back) else stringResource(R.string.all_start_all_programs) + "  ›",
                color = AllColors.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }

    BoxWithConstraints(modifier = modifier.padding(start = 6.dp, bottom = 4.dp)) {
        val compact = maxMenuHeight < AllDimens.CompactHeight || maxWidth < AllDimens.CompactWidth
        val twoCol = compact && maxWidth >= AllDimens.CompactTwoColWidth
        Column(
            modifier = Modifier
                .then(if (compact) Modifier.fillMaxWidth() else Modifier.width(minOf(AllDimens.StartMenuWidth, maxWidth * 0.62f)))
                .heightIn(max = maxMenuHeight - 4.dp)
                .shadow(18.dp, shape).clip(shape)
                .background(AllColors.WindowBody)
                .border(2.dp, AllColors.BlueDark, shape)
                .clickable(indication = null, interactionSource = null) {}
        ) {
            // Header (fixed). Compact: logo + search share one row (user name dropped) to leave room for the list.
            val searchField: @Composable (Modifier) -> Unit = { m ->
                AllTextField(value = query, onValueChange = { query = it }, placeholder = stringResource(R.string.all_start_search), modifier = m)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(AllBrushes.Title).padding(horizontal = 12.dp, vertical = if (compact) AllDimens.StartHeaderPadCompact else 10.dp)
            ) {
                AllLogo(if (compact) 24.dp else 34.dp)
                Spacer(Modifier.width(10.dp))
                if (compact) searchField(Modifier.weight(1f)) else Text(userName, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, shadow = AllLabelShadow), modifier = Modifier.weight(1f))
            }
            if (!compact) searchField(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp))

            val q = query.trim()
            val bodyModifier = Modifier.weight(1f, fill = false)
            val byLabel = entries.sortedBy { it.label.lowercase() }
            val pinned = entries.filter { it.section == AllStartSection.PINNED }
            val system = entries.filter { it.section == AllStartSection.SYSTEM }
            val recent = AllStartRecents.ids.mapNotNull { id -> entries.find { it.id == id } }

            when {
                q.isNotEmpty() -> {
                    val hits = byLabel.filter { it.label.contains(q, ignoreCase = true) }
                    Column(bodyModifier.verticalScroll(rememberScrollState()).padding(horizontal = 6.dp)) {
                        if (hits.isEmpty()) Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = if (compact) 8.dp else 20.dp)
                        ) {
                            Icon(painterResource(R.drawable.ic_search), null, tint = AllColors.BorderStrong, modifier = Modifier.size(24.dp))
                            Text(stringResource(R.string.all_start_no_results, q), color = AllColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                            Text(stringResource(R.string.all_start_try_other), color = AllColors.TextDim, fontSize = 12.sp)
                        } else {
                            SectionLabel(stringResource(R.string.all_start_results, hits.size), upper = false)
                            Rows(hits, pick)
                        }
                    }
                }
                twoCol -> Row(bodyModifier) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
                        if (showAll) Rows(byLabel, pick) else {
                            SectionLabel(stringResource(R.string.all_start_pinned)); Rows(pinned, pick)
                            if (recent.isNotEmpty()) { SectionLabel(stringResource(R.string.all_start_recent)); Rows(recent, pick) }
                        }
                        allProgramsToggle()
                    }
                    Column(Modifier.weight(1f).background(AllColors.MenuRight).border(1.dp, AllColors.MenuRightBorder).verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
                        SectionLabel(stringResource(R.string.all_start_system)); Rows(system, pick)
                    }
                }
                compact -> Column(bodyModifier.verticalScroll(rememberScrollState()).padding(horizontal = 6.dp)) {
                    if (showAll) Rows(byLabel, pick) else {
                        SectionLabel(stringResource(R.string.all_start_pinned)); Rows(pinned, pick)
                        if (recent.isNotEmpty()) { SectionLabel(stringResource(R.string.all_start_recent)); Rows(recent, pick) }
                        SectionLabel(stringResource(R.string.all_start_system)); Rows(system, pick)
                    }
                    allProgramsToggle()
                }
                else -> Row(bodyModifier.heightIn(max = AllDimens.StartBodyMax)) {
                    Column(Modifier.weight(0.58f).fillMaxHeight().background(Color.White).verticalScroll(rememberScrollState()).padding(6.dp)) {
                        if (showAll) Rows(byLabel, pick) else {
                            SectionLabel(stringResource(R.string.all_start_pinned)); Rows(pinned, pick)
                            if (recent.isNotEmpty()) { SectionLabel(stringResource(R.string.all_start_recent)); Rows(recent, pick) }
                        }
                    }
                    Column(Modifier.weight(0.42f).fillMaxHeight().background(AllColors.MenuRight).border(1.dp, AllColors.MenuRightBorder).verticalScroll(rememberScrollState()).padding(6.dp)) {
                        SectionLabel(stringResource(R.string.all_start_system)); Rows(system, pick)
                    }
                }
            }

            // Footer (fixed): All programs toggle (wide only; compact puts it at the end of the list) + Classic
            if (q.isEmpty() && !compact) allProgramsToggle()
        }
    }
}
