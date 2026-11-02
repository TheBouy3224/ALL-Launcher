package com.movtery.zalithlauncher.all.launchersettings

import com.movtery.zalithlauncher.all.ui.AllColors
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.ui.AlertDialog
import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import com.movtery.zalithlauncher.all.ui.TextButton
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.ui.control.mouse.CursorHotspot
import com.movtery.zalithlauncher.ui.control.mouse.MousePointer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val PkText: Color get() = AllColors.Text
private val PkDanger: Color get() = AllColors.Danger
private val PkOk: Color get() = AllColors.Ok

internal fun packLibrary() = CursorPackLibrary(File(PathManager.DIR_MOUSE_POINTER, "packs"))
private fun lastApplyFile() = File(File(PathManager.DIR_MOUSE_POINTER, "packs"), ".last_apply")

private fun displayName(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "file"

/** Decodes every accepted file once; anything that does not render is moved to the skipped list */
internal fun keepRenderable(import: PackImport): PackImport {
    val bad = import.files.filter { (_, b) -> checkCursorBytes(b).error != null }.keys
    bad.forEach { import.files.remove(it); import.skipped += it to PackSkip.INVALID }
    return import
}

/**
 * Installs a pack's role files into the live cursors in one go. Each role goes through [CursorStore] (previous file kept as .bak);
 * what was there before (custom file or built-in, and the hotspot) is written to .last_apply so "Restore" can undo the whole pack.
 * If any role fails, the roles already installed are put back before the error is reported.
 */
internal fun installPack(pack: CursorPack, roles: Map<String, String>) {
    val entries = cursorEntries().associateBy { it.shape.name }
    val done = mutableListOf<Triple<CursorEntry, Boolean, CursorHotspot>>()
    try {
        roles.forEach { (role, file) ->
            val entry = entries.getValue(role)
            val bytes = pack.file(file).readBytes()
            val check = checkCursorBytes(bytes)
            check.error?.let { error("$file: $it") }
            val store = CursorStore(entry.file)
            val had = store.hasCustom()
            val oldHotspot = entry.hotspot.getValue()
            store.stage(bytes)
            store.apply(toDefault = false)
            done += Triple(entry, had, oldHotspot)
            entry.hotspot.save(check.hotspot ?: entry.defaultHotspot)
        }
    } catch (e: Throwable) {
        undo(done)
        throw e
    }
    lastApplyFile().apply {
        parentFile?.mkdirs()
        writeText(done.joinToString("\n") { (e, had, h) -> "${e.shape.name}|$had|${h.xPercent}|${h.yPercent}" })
    }
}

private fun undo(done: List<Triple<CursorEntry, Boolean, CursorHotspot>>) {
    done.asReversed().forEach { (entry, had, hotspot) ->
        val store = CursorStore(entry.file)
        if (had) store.restore() else entry.file.delete()
        entry.hotspot.save(hotspot)
    }
}

internal fun canRestorePack() = lastApplyFile().isFile

/** Puts back every cursor the last pack install changed (custom file or built-in, and its hotspot) */
internal fun restoreLastPack() {
    val f = lastApplyFile()
    if (!f.isFile) return
    val entries = cursorEntries().associateBy { it.shape.name }
    val done = f.readLines().mapNotNull { line ->
        val p = line.split('|')
        val e = entries[p.getOrNull(0)] ?: return@mapNotNull null
        Triple(e, p.getOrNull(1) == "true", CursorHotspot(p.getOrNull(2)?.toIntOrNull() ?: 0, p.getOrNull(3)?.toIntOrNull() ?: 0))
    }
    undo(done)
    f.delete()
}

@Composable
private fun roleTitle(role: String): String = stringResource(cursorEntries().first { it.shape.name == role }.titleRes)

@Composable
private fun skipText(s: PackSkip): String = stringResource(
    when (s) {
        PackSkip.NOT_A_CURSOR -> R.string.all_pack_skip_type
        PackSkip.UNSAFE_NAME -> R.string.all_pack_skip_name
        PackSkip.DUPLICATE -> R.string.all_pack_skip_dup
        PackSkip.TOO_LARGE -> R.string.all_pack_skip_large
        PackSkip.TOO_MANY -> R.string.all_pack_skip_many
        PackSkip.INVALID -> R.string.all_pack_skip_invalid
    }
)

/** "Cursor packs" card of the Cursor tab: import a ZIP or several files, list the library, undo the last install */
@Composable
internal fun CursorPackSection(rev: Int, onOpen: (CursorPack, PackImport?) -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val packs = remember(rev, busy) { packLibrary().list() }
    val restorable = remember(rev, busy) { canRestorePack() }
    val emptyMsg = stringResource(R.string.all_pack_empty)
    val tooBigMsg = stringResource(R.string.all_pack_too_big)
    val unsafeMsg = stringResource(R.string.all_pack_unsafe)
    val restoredMsg = stringResource(R.string.all_pack_restored)
    val defaultName = stringResource(R.string.all_pack_default_name)

    fun runImport(read: () -> PackImport) {
        busy = true
        message = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val imp = keepRenderable(read())
                    if (imp.files.isEmpty()) null else imp to packLibrary().add(imp.name, imp.files, guessCursorRoles(imp.files.keys, imp.infText))
                }
            }
            busy = false
            result.onSuccess { r -> if (r == null) message = emptyMsg to false else onOpen(r.second, r.first) }
                .onFailure { e -> message = when (e) { is PackTooLargeException -> tooBigMsg; is PackUnsafeException -> unsafeMsg; else -> e.message ?: e::class.java.simpleName } to false }
            onChanged()
        }
    }

    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runImport {
            val name = displayName(context, uri).substringBeforeLast('.')
            context.contentResolver.openInputStream(uri)!!.use { readCursorZip(it, name) }
        }
    }
    val filesPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) runImport {
            readCursorFiles(uris.map { u -> displayName(context, u) to { context.contentResolver.openInputStream(u) } }, defaultName)
        }
    }

    Card {
        Text(stringResource(R.string.all_pack_title), color = PkText, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.all_pack_hint), color = StDim, fontSize = 12.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AllButton(stringResource(R.string.all_pack_import_zip), { zipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }, style = AllButtonStyle.Primary, compact = true, enabled = !busy)
            AllButton(stringResource(R.string.all_pack_import_files), { filesPicker.launch(arrayOf("*/*")) }, compact = true, enabled = !busy)
            AllButton(stringResource(R.string.all_pack_restore), {
                runCatching { restoreLastPack() }.onSuccess { message = restoredMsg to true }.onFailure { message = (it.message ?: "?") to false }
                onChanged()
            }, compact = true, enabled = !busy && restorable)
        }
        if (busy) Text(stringResource(R.string.all_pack_reading), color = StAccent, fontSize = 13.sp)
        message?.let { (t, ok) -> Text(t, color = if (ok) PkOk else PkDanger, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        packs.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, color = PkText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.all_pack_counts, p.files.size, p.roles.size), color = StDim, fontSize = 12.sp)
                }
                AllButton(stringResource(R.string.all_pack_open), { onOpen(p, null) }, compact = true, enabled = !busy)
            }
        }
    }
}

/** One pack: its files with previews, which file plays which cursor, then Install (Apply) / Cancel / Delete */
@Composable
internal fun CursorPackDetail(pack: CursorPack, import: PackImport?, modifier: Modifier, onClose: () -> Unit) {
    val files = remember(pack) { pack.files }
    var roles by remember(pack) { mutableStateOf(pack.roles) }
    var selected by remember(pack) { mutableStateOf(pack.roles["Arrow"] ?: files.firstOrNull()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxSize().background(StSurface)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(pack.name, color = PkText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            import?.let { imp ->
                Text(stringResource(R.string.all_pack_imported, imp.files.size), color = PkOk, fontSize = 13.sp)
                imp.skipped.take(12).forEach { (n, why) -> Text("• $n — ${skipText(why)}", color = StDim, fontSize = 12.sp) }
                if (imp.skipped.size > 12) Text("… +${imp.skipped.size - 12}", color = StDim, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(120.dp).background(AllColors.Disabled).border(1.dp, StBorder)) {
                    selected?.let { MousePointer(mouseSize = 120.dp, mouseFile = pack.file(it), triggerRefresh = it) }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(selected ?: "-", color = PkText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(stringResource(R.string.all_pack_pick_hint), color = StDim, fontSize = 12.sp)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        files.forEach { f ->
                            Box(
                                Modifier.size(40.dp).background(if (f == selected) Color(0xFFCFE0FA) else AllColors.Disabled)
                                    .border(1.dp, if (f == selected) StAccent else StBorder).clickable { selected = f },
                                contentAlignment = Alignment.Center
                            ) { MousePointer(mouseSize = 32.dp, mouseFile = pack.file(f), centerIcon = true, animate = false) }
                        }
                    }
                }
            }
            Text(stringResource(R.string.all_pack_roles), color = PkText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            CURSOR_ROLES.forEach { role ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(roleTitle(role) + if (role == "Arrow") "  ★" else "", color = PkText, fontSize = 13.sp, fontWeight = if (role == "Arrow") FontWeight.Bold else FontWeight.Normal)
                        Text(roles[role] ?: stringResource(R.string.all_pack_role_none), color = if (roles[role] != null) StAccent else StDim, fontSize = 12.sp)
                    }
                    AllButton(stringResource(R.string.all_pack_use_selected), { selected?.let { roles = roles + (role to it) } }, compact = true, enabled = selected != null)
                    AllButton(stringResource(R.string.all_pack_clear), { roles = roles - role }, compact = true, enabled = roles[role] != null)
                }
            }
            Text(stringResource(R.string.all_pack_future), color = StDim, fontSize = 12.sp)
            message?.let { Text(it, color = PkDanger, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
        Row(
            Modifier.fillMaxWidth().background(StCard).padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AllButton(stringResource(R.string.all_pack_delete), { confirmDelete = true }, style = AllButtonStyle.Danger, compact = true)
            Box(Modifier.weight(1f))
            AllButton(stringResource(R.string.all_setup_cancel), onClose, compact = true)
            AllButton(stringResource(R.string.all_pack_install), {
                runCatching {
                    val saved = pack.withRoles(roles)
                    installPack(saved, roles)
                }.onSuccess { onClose() }.onFailure { message = it.message ?: it::class.java.simpleName }
            }, style = AllButtonStyle.Success, compact = true, enabled = roles.isNotEmpty())
        }
    }

    if (confirmDelete) AlertDialog(
        destructive = true,
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.all_pack_delete)) },
        text = { Text(stringResource(R.string.all_pack_delete_message, pack.name)) },
        confirmButton = { TextButton(onClick = { packLibrary().delete(pack); confirmDelete = false; onClose() }) { Text(stringResource(R.string.generic_confirm), color = PkDanger) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.generic_cancel)) } }
    )
}
