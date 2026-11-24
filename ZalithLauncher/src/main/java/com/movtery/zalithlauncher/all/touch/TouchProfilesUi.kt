package com.movtery.zalithlauncher.all.touch

import com.movtery.zalithlauncher.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.movtery.layer_controller.layout.loadLayoutFromString
import com.movtery.layer_controller.utils.saveToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** What the small dialogs of the profile list show (one at a time) */
private sealed interface Sheet {
    object New : Sheet
    object Export : Sheet
    data class Row(val p: TouchStore.Profile) : Sheet
    /** [kind] = new (arg = template) | rename (arg = profile id) | copy (arg = profile id) */
    data class Name(val kind: String, val arg: String, val initial: String) : Sheet
}

/** One small dialog: a title, the rows, and a quiet cancel */
@Composable
private fun SheetDialog(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.ui.window.Dialog(onClose) {
        TouchMaterial {
            Panel {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = TouchTheme.text, fontSize = 17.sp, modifier = Modifier.padding(bottom = 6.dp))
                    content()
                    Pill(stringResource(R.string.all_t_7d3094b8), onClose, Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

/**
 * Profile list shared by the Studio (manage = true: new / import / export, and rename / copy / delete behind a long press or the dots) and the in-game Quick Menu (manage = false: switch only).
 * [onSwitched] fires after the ACTIVE profile changed, so the caller reloads the layout (which releases every held input).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TouchProfilesPanel(manage: Boolean, textColor: Color, onSwitched: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var confirmDelete by remember { mutableStateOf<TouchStore.Profile?>(null) }
    val list = remember(tick) { TouchStore.profiles(context) }
    val active = remember(tick) { TouchStore.activeId(context) }
    fun switchTo(id: String) { if (TouchStore.setActive(context, id)) { tick++; onSwitched() } }
    var report by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun lines(w: List<String>) = if (w.isEmpty()) "" else "\n\nแปลงไม่ได้ / ต่างจากเดิม:\n" + w.joinToString("\n") { "• " + it }
    // Zalith 2 -> ALL: always a NEW profile, read with LayerController's own loader
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            report = "กำลังนำเข้า…"
            scope.launch {
                // a big file (a 2 MB FCL layout = 800+ buttons) takes seconds: never on the main thread (it froze the game into an ANR)
                val result = withContext(Dispatchers.Default) {
                    runCatching {
                        val raw = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                        if (TouchZip.isZip(raw)) { // ALL profile ZIP (pictures / GIFs inside): same Studio, new profile
                            val (zid, zrep) = TouchStore.importZip(context, raw)
                            return@runCatching zid to ("นำเข้าแล้ว: โปรไฟล์ ALL (ZIP) " + zrep + " | โปรไฟล์เดิมไม่ถูกแตะ")
                        }
                        val text = raw.toString(Charsets.UTF_8)
                        val screen = screenOf(context)
                        val (pname, layout, warnings) = if (LayoutMapper.isPojav(text)) // Pojav (mControlDataList), FCL (viewGroups) or Zalith 2 (layers) file
                            LayoutMapper.fromPojav(text, screen) { aspectOf(context, it) }.let { Triple(it.value.first, it.value.second, it.warnings) }
                        else if (LayoutMapper.isFcl(text))
                            LayoutMapper.fromFcl(text, screen) { aspectOf(context, it) }.let { Triple(it.value.first, it.value.second, it.warnings) }
                        else {
                            val z = loadLayoutFromString(text)
                            LayoutMapper.fromZalith(z, screen) { aspectOf(context, it) }.let { Triple(z.info.name.translate(), it.value, it.warnings) }
                        }
                        val shown = pname.ifBlank { "นำเข้า" }
                        TouchStore.createFrom(context, shown, layout) to
                            ("นำเข้าแล้ว: โปรไฟล์ใหม่ \"" + shown.take(30) + "\" (" + layout.items.size + " รายการ) โปรไฟล์เดิมไม่ถูกแตะ" + lines(warnings))
                    }
                }
                result.onSuccess { (id, msg) -> switchTo(id); report = msg }
                    .onFailure { report = "นำเข้าไม่สำเร็จ: " + (it.message ?: it.javaClass.simpleName) }
            }
        }
    }
    // ALL (active profile) -> ZIP with its pictures / GIFs
    val zipExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            report = "กำลังส่งออก…"
            report = withContext(Dispatchers.Default) { runCatching {
                val n = withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")!!.use { TouchStore.exportZip(context, it) } }
                "ส่งออกแล้ว: ZIP โปรไฟล์ ALL (รูป/GIF " + n + " ไฟล์) ใช้นำเข้าใน ALL ได้ทุกเครื่อง"
            }.getOrElse { "ส่งออกไม่สำเร็จ: " + (it.message ?: it.javaClass.simpleName) } }
        }
    }
    // ALL (active profile) -> Zalith 2 (current v12) / Zalith 1 / Pojav / FCL file
    var exportKind by remember { mutableStateOf("zl") } // zl | zl1 | pojav | fcl
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            report = "กำลังส่งออก…"
            report = withContext(Dispatchers.Default) { runCatching { // heavy for big layouts: off the main thread
                val layout = TouchStore.load(context); val pname = TouchStore.activeName(context); val screen = screenOf(context)
                if (exportKind == "pojav" || exportKind == "zl1") {
                    val m = if (exportKind == "zl1") LayoutMapper.toZalith1(layout, pname.ifBlank { "ALL" }, screen) { aspectOf(context, it) } else LayoutMapper.toPojav(layout, screen) { aspectOf(context, it) }
                    withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(m.value.toByteArray(Charsets.UTF_8)) } }
                    "ส่งออกแล้ว: ไฟล์ " + (if (exportKind == "zl1") "Zalith 1" else "Pojav") + " (" + layout.items.size + " รายการ)" + lines(m.warnings)
                } else if (exportKind == "fcl") {
                    val m = LayoutMapper.toFcl(layout, pname, screen) { aspectOf(context, it) }
                    withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(m.value.toByteArray(Charsets.UTF_8)) } }
                    "ส่งออกแล้ว: ไฟล์ FCL (" + layout.items.size + " รายการ)" + lines(m.warnings)
                } else {
                    val m = LayoutMapper.toZalith(layout, pname, screen, aspectOf = { aspectOf(context, it) })
                    val tmp = java.io.File(context.cacheDir, "all_export_zl2.json")
                    withContext(Dispatchers.IO) {
                        m.value.saveToFile(tmp)
                        context.contentResolver.openOutputStream(uri, "wt")!!.use { out -> tmp.inputStream().use { it.copyTo(out) } }
                        tmp.delete()
                    }
                    "ส่งออกแล้ว: ไฟล์ Zalith 2 (" + m.value.layers.sumOf { it.normalButtons.size + it.joystickButtons.size } + " รายการ) ต้องใช้ Zalith 2 รุ่น 2.4.11 ขึ้นไป" + lines(m.warnings)
                }
            }.getOrElse { "ส่งออกไม่สำเร็จ: " + (it.message ?: it.javaClass.simpleName) } }
        }
    }
    fun fileName(suffix: String) = TouchStore.activeName(context).ifBlank { "ALL" } + suffix

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        list.forEach { p ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (if (p.id == active) "● " else "○ ") + p.name,
                    Modifier.weight(1f).combinedClickable(onClick = { if (p.id != active) switchTo(p.id) }, onLongClick = { if (manage) sheet = Sheet.Row(p) }).padding(vertical = 12.dp),
                    color = textColor,
                )
                if (manage && p.id != active) Pill("เลือก", { switchTo(p.id) })
                if (manage && p.id != active && list.size > 1) Pill(stringResource(R.string.all_t_221622d8), { confirmDelete = p }, Modifier.padding(start = 6.dp), color = TouchTheme.danger)
                if (manage) Text("⋯", Modifier.combinedClickable(onClick = { sheet = Sheet.Row(p) }).padding(horizontal = 14.dp, vertical = 12.dp), color = TouchTheme.dim)
            }
        }
        if (manage) {
            Text(stringResource(R.string.all_t_3b96c07f), color = TouchTheme.dim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(stringResource(R.string.all_t_096af55f), { sheet = Sheet.New })
                Pill(stringResource(R.string.all_t_1610efd7), { importer.launch(arrayOf("*/*")) })
                Pill(stringResource(R.string.all_t_f91eb405), { sheet = Sheet.Export }, primary = true)
            }
        }
    }

    sheet?.let { s ->
        val close = { sheet = null }
        when (s) {
            Sheet.New -> SheetDialog("โปรไฟล์ใหม่", close) {
                MenuRow("D-Pad", "ปุ่มเดินแบบกากบาท") { sheet = Sheet.Name("new", "dpad", "") }
                MenuRow("Joystick กลม") { sheet = Sheet.Name("new", "joy_round", "") }
                MenuRow("Joystick เหลี่ยม") { sheet = Sheet.Name("new", "joy_square", "") }
            }
            Sheet.Export -> SheetDialog("ส่งออก \"" + TouchStore.activeName(context) + "\" ไปยัง", close) {
                MenuRow("Zalith 2", "รุ่น 2.4.11 ขึ้นไป") { close(); exportKind = "zl"; exporter.launch(fileName(".json")) }
                MenuRow("Zalith 1") { close(); exportKind = "zl1"; exporter.launch(fileName("_zalith1.json")) }
                MenuRow("Pojav") { close(); exportKind = "pojav"; exporter.launch(fileName("_pojav.json")) }
                MenuRow("FCL") { close(); exportKind = "fcl"; exporter.launch(fileName("_fcl.json")) }
                MenuRow("ALL (ใช้กันเอง)", "ZIP · รวมรูป/GIF · ไม่มีอะไรหาย") { close(); zipExporter.launch(fileName(".zip")) }
            }
            is Sheet.Row -> SheetDialog(s.p.name, close) {
                MenuRow("เปลี่ยนชื่อ") { sheet = Sheet.Name("rename", s.p.id, s.p.name) }
                MenuRow("คัดลอก") { sheet = Sheet.Name("copy", s.p.id, s.p.name + " สำเนา") }
                MenuRow("ลบ", if (list.size > 1) null else "ต้องมีอย่างน้อย 1 โปรไฟล์") { if (list.size > 1) { confirmDelete = s.p; close() } }
            }
            is Sheet.Name -> {
                var nm by remember(s) { mutableStateOf(s.initial) }
                SheetDialog(if (s.kind == "new") "ชื่อโปรไฟล์ใหม่" else if (s.kind == "rename") "เปลี่ยนชื่อ" else "คัดลอกเป็น", close) {
                    OutlinedTextField(nm, { nm = it.take(30) }, Modifier.fillMaxWidth(), singleLine = true)
                    Pill(stringResource(R.string.all_t_55c86611), {
                        close()
                        when (s.kind) {
                            "new" -> switchTo(TouchStore.create(context, nm, null, s.arg))
                            "rename" -> { TouchStore.rename(context, s.arg, nm); tick++ }
                            else -> switchTo(TouchStore.create(context, nm, s.arg))
                        }
                    }, Modifier.padding(top = 8.dp), primary = true, enabled = nm.isNotBlank() || s.kind == "new")
                }
            }
        }
    }

    report?.let { text ->
        val head = text.substringBefore("\n\n"); val details = text.substringAfter("\n\n", "")
        var open by remember(text) { mutableStateOf(false) }
        androidx.compose.ui.window.Dialog({ report = null }) {
            TouchMaterial {
                Panel {
                    Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(head, color = TouchTheme.text)
                        if (details.isNotEmpty()) {
                            if (open) Text(details, color = TouchTheme.dim)
                            else Text("มี " + details.lines().count { it.startsWith("• ") } + " ข้อที่แปลงไม่ได้ / ต่างจากเดิม", color = TouchTheme.dim)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (details.isNotEmpty() && !open) Pill(stringResource(R.string.all_t_4f361629), { open = true })
                            Pill(stringResource(R.string.all_t_1b6d61ea), { report = null }, primary = true)
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { p ->
        androidx.compose.ui.window.Dialog({ confirmDelete = null }) {
            TouchMaterial {
                Panel {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ลบโปรไฟล์ \"${p.name}\"?", color = TouchTheme.text)
                        Text(stringResource(R.string.all_t_4af2580d), color = TouchTheme.dim)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill(stringResource(R.string.all_t_7d3094b8), { confirmDelete = null })
                            Pill(stringResource(R.string.all_t_221622d8), {
                                val wasActive = p.id == active
                                TouchStore.delete(context, p.id); confirmDelete = null; tick++
                                if (wasActive) onSwitched()
                            }, color = TouchTheme.danger)
                        }
                    }
                }
            }
        }
    }
}

/** the game and Studio run in landscape: width = the long side */
private fun screenOf(context: android.content.Context): LayoutMapper.Screen =
    context.resources.displayMetrics.let { LayoutMapper.Screen(maxOf(it.widthPixels, it.heightPixels), minOf(it.widthPixels, it.heightPixels), it.density) }

private fun aspectOf(context: android.content.Context, sprite: String): Float =
    TouchSprites.get(context, sprite)?.let { it.height.toFloat() / it.width } ?: 1f
