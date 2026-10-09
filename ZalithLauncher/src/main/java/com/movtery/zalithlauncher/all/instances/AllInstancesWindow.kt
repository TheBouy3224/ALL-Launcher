package com.movtery.zalithlauncher.all.instances

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.movtery.zalithlauncher.all.ui.AlertDialog
import com.movtery.zalithlauncher.all.ui.Checkbox
import com.movtery.zalithlauncher.all.ui.OutlinedTextField
import androidx.compose.material3.Text
import com.movtery.zalithlauncher.all.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.bridge.AllDeleteVersionDialog
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.all.download.AllDownloadCategory
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.download.game.GameInstaller
import com.movtery.zalithlauncher.game.version.export.ExportInfo
import com.movtery.zalithlauncher.game.version.export.PackExporter
import com.movtery.zalithlauncher.game.version.export.PackType
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ALL instance management window (PHASE 3.6).
 *
 * Backend is Zalith's, unchanged: [VersionsManager] (versions / currentVersion flows, saveVersion, renameVersion,
 * copyVersion, deleteVersion, isVersionExists), [GameInstaller.updateLoader] for loader changes,
 * [PackExporter] for modpack export, and the delete dialog through the bridge. "Create" opens the ALL Download window
 * (Games category), which already installs instances through GameInstaller.
 * Loader change here supports Fabric only (the Classic UpdateLoader screen also handles Forge/NeoForge/Quilt/etc.).
 */

private val InSurface = Color(0xFFF3F1E8)
private val InCard = Color(0xFFFFFFFF)
private val InBorder = Color(0xFFB9B4A2)
private val InText = Color(0xFF1B1B1B)
private val InDim = Color(0xFF6B675C)
private val InAccent = Color(0xFF316AC5)
private val InDanger = Color(0xFFB3261E)

//same exclusions as Classic's export screen: launcher config, accounts cache, logs, saves, natives, options ...
private val ExportSkipTopLevel = listOf(
    ".fabric", "options.txt", "natives", "downloads", "PCL", BuildKeys.LAUNCHER_IDENTIFIER, "fclversion.cfg",
    "saves", "realms_persistence.json", "usercache.json", ".log", "logs"
)

@Composable
fun AllInstancesWindow(
    onOpenDownload: (AllDownloadCategory) -> Unit,
    onImport: () -> Unit,
    onError: AllSubmitError,
    modifier: Modifier = Modifier
) {
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    val current by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var renaming by remember { mutableStateOf<Version?>(null) }
    var copying by remember { mutableStateOf<Version?>(null) }
    var deleting by remember { mutableStateOf<Version?>(null) }
    var loaderFor by remember { mutableStateOf<Version?>(null) }
    var exportFor by remember { mutableStateOf<Version?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().background(InSurface)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Btn(stringResource(R.string.generic_download) + " / " + stringResource(R.string.all_instances_create), onClick = { onOpenDownload(AllDownloadCategory.GAME) }, color = InAccent)
            Btn(stringResource(R.string.import_modpack), onClick = onImport)
            Btn(stringResource(R.string.generic_refresh), onClick = { VersionsManager.refresh("ALL Instances") })
            Text("${versions.size}", color = InDim, fontSize = 12.sp)
        }
        message?.let { Text(it, color = InDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp)) }
        if (versions.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text(stringResource(R.string.all_desktop_no_instances), color = InDim) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                items(versions, key = { it.getVersionName() }) { v ->
                    val isCur = current?.getVersionName() == v.getVersionName()
                    val info = v.getVersionInfo()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCur) com.movtery.zalithlauncher.all.ui.AllColors.BluePale else InCard)
                            .border(1.dp, if (isCur) com.movtery.zalithlauncher.all.ui.AllColors.Blue else InBorder, RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text(v.getVersionName(), color = InText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(
                                if (isCur) "✓ " + stringResource(R.string.generic_enable) else null,
                                info?.minecraftVersion?.let { stringResource(R.string.all_desktop_version) + " " + it },
                                info?.loaderInfo?.let { it.loader.displayName + " " + it.version },
                                if (!v.isValid()) stringResource(R.string.versions_manage_delete_version_tip_invalid) else null
                            ).joinToString(" · "),
                            color = InDim, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState())
                        ) {
                            if (!isCur && v.isValid()) Btn(stringResource(R.string.all_accounts_select), onClick = { VersionsManager.saveVersion(v) })
                            if (v.isValid()) {
                                Btn(stringResource(R.string.generic_rename), onClick = { renaming = v })
                                Btn(stringResource(R.string.versions_manage_copy_version), onClick = { copying = v })
                                Btn(stringResource(R.string.all_desktop_loader), onClick = { loaderFor = v })
                                Btn(stringResource(R.string.all_instances_export), onClick = { exportFor = v })
                            }
                            Btn(stringResource(R.string.generic_delete), onClick = { deleting = v }, color = InDanger)
                        }
                    }
                }
            }
        }
    }

    renaming?.let { v ->
        AllNameDialog(
            title = stringResource(R.string.versions_manage_rename_version),
            initial = v.getVersionName(),
            allowSame = true,
            onDismiss = { renaming = null },
            onConfirm = { name ->
                renaming = null
                if (name != v.getVersionName()) scope.launch(Dispatchers.IO) { VersionsManager.renameVersion(v, name) }
            }
        )
    }
    copying?.let { v ->
        var copyAll by remember { mutableStateOf(false) }
        AllNameDialog(
            title = stringResource(R.string.versions_manage_copy_version),
            initial = v.getVersionName() + "-copy",
            allowSame = false,
            onDismiss = { copying = null },
            extra = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { copyAll = !copyAll }) {
                    Checkbox(checked = copyAll, onCheckedChange = { copyAll = it })
                    Text(stringResource(R.string.versions_manage_copy_version_all), fontSize = 13.sp)
                }
            },
            onConfirm = { name ->
                copying = null
                scope.launch(Dispatchers.IO) { VersionsManager.copyVersion(v, name, copyAll) }
            }
        )
    }
    deleting?.let { v ->
        AllDeleteVersionDialog(
            version = v,
            onDismissRequest = { deleting = null },
            onConfirm = { _, task -> deleting = null; scope.launch(Dispatchers.IO) { task() } },
            onVersionDeleted = {}
        )
    }
    loaderFor?.let { v -> LoaderDialog(v, onError) { loaderFor = null } }
    exportFor?.let { v -> ExportDialog(v, onError) { exportFor = null } }
}

@Composable
internal fun AllNameDialog(
    title: String,
    initial: String,
    allowSame: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    extra: @Composable () -> Unit = {}
) {
    var name by remember { mutableStateOf(initial) }
    val clean = name.trim()
    //same checks as Classic's dialogs: not empty, no path characters, and renameVersion/copyVersion would wipe an existing folder
    val invalidChars = clean.isEmpty() || clean.any { it in "\\/:*?\"<>|" } || clean == "." || clean == ".." || clean.length > 100
    val exists = clean.isNotEmpty() && !(allowSame && clean == initial) && VersionsManager.isVersionExists(clean, true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    isError = invalidChars || exists,
                    supportingText = { if (exists) Text(stringResource(R.string.versions_manage_install_exists)) }
                )
                extra()
            }
        },
        confirmButton = { TextButton(enabled = !invalidChars && !exists, onClick = { onConfirm(clean) }) { Text(stringResource(R.string.generic_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_cancel)) } }
    )
}

@Composable
private fun LoaderDialog(version: Version, onError: AllSubmitError, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val info = version.getVersionInfo()
    val loader = info?.loaderInfo
    val mc = info?.minecraftVersion.orEmpty()
    val fabricOnly = loader == null || loader.loader == ModLoader.FABRIC
    var list by remember { mutableStateOf<List<FabricVersion>?>(null) }
    var failed by remember { mutableStateOf<String?>(null) }
    var installing by remember { mutableStateOf<GameInstaller?>(null) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(mc) {
        if (fabricOnly && mc.isNotEmpty()) {
            runCatching { FabricVersions.fetchFabricLoaderList(mc) }
                .onSuccess { list = it ?: emptyList() }
                .onFailure { failed = it.getMessageOrToString() }
        }
    }

    AlertDialog(
        onDismissRequest = { if (installing == null) onClose() },
        title = { Text(version.getVersionName()) },
        text = {
            Column(modifier = Modifier.heightIn(max = 300.dp)) {
                Text(
                    stringResource(R.string.all_desktop_version) + ": " + mc + "\n" +
                        stringResource(R.string.all_desktop_loader) + ": " + (loader?.let { it.loader.displayName + " " + it.version } ?: "-"),
                    fontSize = 13.sp
                )
                Spacer(Modifier.padding(top = 6.dp))
                when {
                    done -> Text(stringResource(R.string.versions_update_loader_success_message), color = Color(0xFF2E7D32), fontSize = 13.sp)
                    !fabricOnly -> Text(stringResource(R.string.all_instances_loader_fabric_only), color = InDim, fontSize = 12.sp)
                    installing != null -> Text(stringResource(R.string.generic_loading), color = InAccent)
                    failed != null -> Text(failed!!, color = InDanger, fontSize = 12.sp)
                    list == null -> Text(stringResource(R.string.generic_loading), color = InDim)
                    list!!.isEmpty() -> Text(stringResource(R.string.all_instances_loader_none), color = InDim)
                    else -> LazyColumn {
                        items(list!!.take(20), key = { it.getAddonVersion() }) { fv ->
                            val cur = loader?.version?.let { fv.isVersion(it) } == true
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !cur) {
                                        val installer = GameInstaller(
                                            context,
                                            GameDownloadInfo(gameVersion = mc, customVersionName = version.getVersionName(), fabric = fv),
                                            scope
                                        )
                                        installing = installer
                                        installer.updateLoader(
                                            onInstalled = {
                                                installing = null; done = true
                                                VersionsManager.refresh("ALL Instances loader")
                                            },
                                            onError = { th ->
                                                installing = null
                                                onError(ErrorViewModel.ThrowableMessage(androidText(context.getString(R.string.versions_update_loader)), androidText(th.getMessageOrToString())))
                                            }
                                        )
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(fv.getAddonVersion() + if (cur) "  ✓" else "", color = if (cur) InDim else InText, fontWeight = if (cur) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                installing?.cancelInstall(clearTarget = false)
                installing = null
                onClose()
            }) { Text(stringResource(if (installing != null) R.string.generic_cancel else R.string.generic_close)) }
        }
    )
}

@Composable
private fun ExportDialog(version: Version, onError: AllSubmitError, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(version.getVersionName()) }
    var packVersion by remember { mutableStateOf("1.0") }
    var author by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val loader = version.getVersionInfo()?.loaderInfo

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true; status = context.getString(R.string.generic_loading)
        scope.launch {
            val root = version.getGameDir()
            val skipAll = listOf("${version.getVersionName()}.json", "${version.getVersionName()}.jar")
            val files = withContext(Dispatchers.IO) {
                root.listFiles().orEmpty()
                    .filter { f -> ExportSkipTopLevel.none { f.name.contains(it) } && skipAll.none { f.name.contains(it) } }
                    .flatMap { f -> if (f.isDirectory) f.walkTopDown().filter { it.isFile && skipAll.none { s -> it.name.contains(s) } }.toList() else listOf(f) }
            }
            val info = ExportInfo(
                gamePath = root,
                name = name.trim(),
                version = packVersion.trim(),
                author = author.trim(),
                mcVersion = version.getVersionInfo()?.minecraftVersion.orEmpty(),
                loader = loader?.let { ExportInfo.LoaderVersion(it.loader, it.version) },
                selectedFiles = files,
                packType = PackType.Modrinth,
                packModrinth = false,
                packCurseForge = false
            )
            PackExporter(context, info, scope).startExport(
                outputUri = uri,
                version = version,
                onFinished = { busy = false; status = context.getString(R.string.versions_export_pack_select_output) + " ✓ (${files.size})" },
                onError = { th ->
                    busy = false; status = th.getMessageOrToString()
                    onError(ErrorViewModel.ThrowableMessage(androidText(context.getString(R.string.all_instances_export)), androidText(th.getMessageOrToString())))
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        title = { Text(stringResource(R.string.all_instances_export) + " — " + version.getVersionName()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(stringResource(R.string.versions_export_pack_name)) })
                OutlinedTextField(packVersion, { packVersion = it }, singleLine = true, label = { Text(stringResource(R.string.versions_export_pack_version)) })
                OutlinedTextField(author, { author = it }, singleLine = true, label = { Text(stringResource(R.string.versions_export_pack_author)) })
                Text(stringResource(R.string.all_instances_export_note), color = InDim, fontSize = 11.sp)
                status?.let { Text(it, color = InAccent, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && name.isNotBlank() && packVersion.isNotBlank(), onClick = { picker.launch(null) }) {
                Text(stringResource(R.string.all_instances_export))
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onClose) { Text(stringResource(R.string.generic_close)) } }
    )
}

@Composable
private fun Btn(text: String, onClick: () -> Unit, color: Color = InText) =
    com.movtery.zalithlauncher.all.ui.AllButton(text, onClick, style = com.movtery.zalithlauncher.all.ui.allStyleForColor(color), compact = true)
