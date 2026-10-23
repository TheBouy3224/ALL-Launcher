package com.movtery.zalithlauncher.all.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import com.movtery.zalithlauncher.all.ui.AllDialog
import com.movtery.zalithlauncher.all.ui.OutlinedTextField

private val Blue = Color(0xFF316AC5)
private val Dim = Color(0xFF55554F)

/**
 * "กำหนดเอง": pick one key or a combo (Ctrl+Z) by search, name the button, see "name → keys" before confirming.
 * Used both for a new button and for changing an existing button's binding.
 */
@Composable
fun CustomKeyDialog(
    title: String,
    initialName: String,
    initialEvents: List<ClickEvent>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, events: List<ClickEvent>) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var keys by remember { mutableStateOf(orderCombo(initialEvents)) }
    var query by remember { mutableStateOf("") }
    val results = remember(query) { searchKeys(query) }
    AllDialog(
        title = title,
        onDismissRequest = onDismiss,
        actions = {
            AllButton(stringResource(R.string.generic_cancel), onDismiss, compact = true)
            AllButton(stringResource(R.string.generic_confirm), { onConfirm(name.trim(), orderCombo(keys)) }, compact = true,
                style = AllButtonStyle.Primary, enabled = keys.isNotEmpty())
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(24) }, singleLine = true,
                    label = { Text(stringResource(R.string.all_studio_custom_name)) }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = query, onValueChange = { query = it.take(20) }, singleLine = true,
                    label = { Text("🔍 " + stringResource(R.string.all_studio_custom_search)) }, modifier = Modifier.weight(1f))
            }
            //preview: what the button will show and send
            val shown = name.trim().ifEmpty { if (keys.isEmpty()) "…" else comboLabel(keys) }
            Text(
                if (keys.isEmpty()) androidx.compose.ui.text.AnnotatedString(stringResource(R.string.all_studio_custom_pick)) else androidx.compose.ui.text.buildAnnotatedString {
                    append(shown)
                    //"→" is missing from the app font (shows as a stray glyph); "▶" is the arrow the Studio toolbar already uses
                    append("  ▶  ")
                    append(comboLabel(keys))
                },
                color = if (keys.isEmpty()) Dim else Blue, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFFDDEBFF)).padding(horizontal = 8.dp, vertical = 5.dp)
            )
            //chosen keys: tap to remove
            if (keys.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                keys.forEach { e -> Chip(keyLabel(e) + "  ✕", selected = true) { keys = keys - e } }
                Text(stringResource(R.string.all_studio_custom_combo_hint), color = Dim, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
            }
            Column(Modifier.heightIn(max = 132.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (results.isEmpty()) Text(stringResource(R.string.all_studio_custom_none), color = Dim, fontSize = 12.sp)
                results.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { key ->
                            val on = key.event in keys
                            Box(Modifier.weight(1f)) { Chip(key.label, selected = on, full = true) { keys = if (on) keys - key.event else orderCombo(keys + key.event) } }
                        }
                        repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, full: Boolean = false, onClick: () -> Unit) {
    Text(
        text, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center, color = if (selected) Color.White else Color(0xFF1E2A3A),
        modifier = (if (full) Modifier.fillMaxWidth() else Modifier).heightIn(min = 34.dp).clip(RoundedCornerShape(5.dp))
            .background(if (selected) Blue else Color(0xFFF7F5EE)).border(1.dp, Color(0xFF8DA2C0), RoundedCornerShape(5.dp))
            .clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp)
    )
}
