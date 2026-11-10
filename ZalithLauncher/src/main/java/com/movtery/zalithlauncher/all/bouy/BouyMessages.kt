package com.movtery.zalithlauncher.all.bouy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Message button (BouyItem.msgs): the saved messages, how many fit, and how long one may be */
internal const val MSG_MAX = 8
internal const val MSG_LEN = 100

/** one line, trimmed, at most [MSG_LEN] characters; null = nothing to save */
internal fun cleanMsg(s: String): String? = s.replace('\n', ' ').replace('\r', ' ').trim().take(MSG_LEN).ifEmpty { null }

@Composable
private fun MsgDialog(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false)) {
        BouyMaterial {
            Panel(Modifier.fillMaxWidth(0.7f).fillMaxHeight(0.92f)) {
                ProvideTextStyle(LocalTextStyle.current.copy(fontFamily = remember { BouyFont.family(context) }, color = BouyTheme.text)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, Modifier.weight(1f)); Pill("ปิด", onClose)
                        }
                        Box(Modifier.padding(top = 10.dp).weight(1f).fillMaxWidth()) { content() }
                    }
                }
            }
        }
    }
}

/** In the game, one tap on the button: the saved messages in the owner's order. Tapping one sends it. Nothing else here. */
@Composable
internal fun BouyMsgList(item: BouyItem, onSend: (String) -> Unit, onClose: () -> Unit) {
    MsgDialog("ส่งข้อความ", onClose) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item.msgs.orEmpty().forEach { m ->
                Text(m, Modifier.fillMaxWidth().background(Color(0x22FFFFFF), RoundedCornerShape(8.dp)).clickable { onSend(m) }.padding(horizontal = 16.dp, vertical = 14.dp))
            }
        }
    }
}

/** Studio only ("ข้อความ" in the toolbar): add, edit, delete and re-order the messages (▲ ▼ moves one up / down) */
@Composable
internal fun BouyMsgSettings(item: BouyItem, onChange: (List<String>) -> Unit, onClose: () -> Unit) {
    val msgs = item.msgs.orEmpty()
    var text by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(-1) }
    val full = editing < 0 && msgs.size >= MSG_MAX
    val commit = {
        cleanMsg(text)?.let { m ->
            if (editing >= 0) onChange(msgs.mapIndexed { i, o -> if (i == editing) m else o })
            else if (!full && m !in msgs) onChange(msgs + m)
            text = ""; editing = -1
        }
        Unit
    }
    fun move(i: Int, to: Int) {
        if (to !in msgs.indices) return
        val l = msgs.toMutableList(); val x = l.removeAt(i); l.add(to, x)
        onChange(l)
        if (editing == i) editing = to else if (editing == to) editing = i
    }
    MsgDialog("ข้อความของปุ่มนี้", onClose) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // field + button first: on a landscape phone the keyboard hides the bottom of the dialog
            OutlinedTextField(text, { text = it.take(MSG_LEN) }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(if (editing >= 0) "แก้ข้อความ" else "พิมพ์ข้อความใหม่ เช่น /home") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { commit() }))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(if (editing >= 0) "บันทึกการแก้" else "＋ เพิ่ม", { commit() }, primary = true, enabled = cleanMsg(text) != null && (editing >= 0 || !full))
                if (editing >= 0) Pill("ยกเลิกแก้", { text = ""; editing = -1 })
                Text("${msgs.size}/$MSG_MAX · ไม่เกิน $MSG_LEN ตัวอักษร", color = BouyTheme.dim)
            }
            if (msgs.isEmpty()) Text("ยังไม่มีข้อความ: ตอนนี้กดปุ่มนี้จะเปิดแชทเฉยๆ", color = BouyTheme.dim)
            msgs.forEachIndexed { i, m ->
                Row(Modifier.fillMaxWidth().background(if (i == editing) Color(0x335B9DFF) else Color(0x22FFFFFF), RoundedCornerShape(8.dp)), verticalAlignment = Alignment.CenterVertically) {
                    Text("▲", Modifier.clickable(enabled = i > 0) { move(i, i - 1) }.padding(horizontal = 14.dp, vertical = 12.dp), color = if (i > 0) BouyTheme.text else BouyTheme.dim.copy(alpha = 0.3f))
                    Text("▼", Modifier.clickable(enabled = i < msgs.size - 1) { move(i, i + 1) }.padding(horizontal = 14.dp, vertical = 12.dp), color = if (i < msgs.size - 1) BouyTheme.text else BouyTheme.dim.copy(alpha = 0.3f))
                    Text(m, Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 4.dp))
                    Pill("แก้", { text = m; editing = i })
                    Pill("ลบ", {
                        onChange(msgs.filterIndexed { n, _ -> n != i })
                        if (editing == i) { text = ""; editing = -1 } else if (editing > i) editing--
                    }, color = BouyTheme.danger, modifier = Modifier.padding(start = 6.dp, end = 6.dp))
                }
            }
            Text("ลำดับในรายการคือลำดับที่จะเห็นตอนเล่น (บนสุด = อยู่ข้างบน) กดปุ่มครั้งเดียวเพื่อเปิดรายการ แล้วแตะข้อความที่จะส่ง", color = BouyTheme.dim)
        }
    }
}
