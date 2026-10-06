package com.movtery.zalithlauncher.all.overlays

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.movtery.zalithlauncher.all.ui.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.components.MarkdownView
import com.movtery.zalithlauncher.ui.components.defaultRichTextStyle
import com.movtery.zalithlauncher.ui.screens.content.elements.isFilenameInvalid
import com.movtery.zalithlauncher.upgrade.RemoteData
import com.movtery.zalithlauncher.upgrade.findCurrentBody
import com.movtery.zalithlauncher.upgrade.getCurrentCouldDrive
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.utils.formatDate
import java.util.Locale

/** ALL presentation only. Callers retain the original operation transitions and backend callbacks. */
@Composable
fun AllModpackNameDialog(initialName: String, onConfirm: (String) -> Unit, onCancel: () -> Unit) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val invalid = isFilenameInvalid(name)
    val exists = remember(name) { VersionsManager.isVersionExists(name, true) }
    val error = when {
        name.isEmpty() -> stringResource(R.string.generic_cannot_empty)
        invalid != null -> invalid
        exists -> stringResource(R.string.versions_manage_install_exists)
        else -> null
    }
    DlgFrame(stringResource(R.string.download_install_input_version_name_title), {}) {
        OutlinedTextField(name, { name = it }, singleLine = true, isError = error != null, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = Color(0xFFB3261E)) }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DlgButton(stringResource(R.string.generic_cancel), onCancel)
            DlgButton(stringResource(R.string.generic_confirm), { if (error == null) onConfirm(name) }, enabled = error == null)
        }
    }
}

@Composable
fun AllUpgradeDialog(data: RemoteData, onDismiss: () -> Unit, onFiles: () -> Unit, onIgnore: () -> Unit,
                     onLink: (String) -> Unit, onCloud: (RemoteData.CloudDrive) -> Unit) {
    val body = remember(data) { data.findCurrentBody(Locale.getDefault()) ?: data.defaultBody }
    val cloud = remember(data) { data.getCurrentCouldDrive(Locale.getDefault()) }
    val markdown = stringResource(R.string.upgrade_version_change, data.version) + "  \n" +
        stringResource(R.string.upgrade_version_create_at, formatDate(data.createdAt, stringResource(R.string.date_format))) + "  \n\n" + body.markdown
    DlgFrame(stringResource(R.string.upgrade_new), onDismiss) {
        CompositionLocalProvider(LocalUriHandler provides object : UriHandler { override fun openUri(uri: String) = onLink(uri) }) {
            MarkdownView(content = markdown, richTextStyle = defaultRichTextStyle(), modifier = Modifier.fillMaxWidth().heightIn(max = 210.dp).verticalScroll(rememberScrollState()))
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cloud?.let { drive -> DlgButton(stringResource(R.string.upgrade_cloud_drive), {
                when (drive.links.size) { 0 -> onLink(drive.link); 1 -> onLink(drive.links[0].link); else -> onCloud(drive) }
            }) }
            DlgButton(stringResource(R.string.generic_ignore), { onIgnore(); onDismiss() })
            DlgButton(stringResource(R.string.upgrade_more), onFiles)
            DlgButton(stringResource(R.string.generic_close), onDismiss)
        }
    }
}

@Composable
fun AllUpgradeFilesDialog(data: RemoteData, onDismiss: () -> Unit, onFile: (RemoteData.RemoteFile) -> Unit) {
    val arch = remember(data) { when (Architecture.getDeviceArchitecture()) {
        Architecture.ARCH_ARM -> RemoteData.RemoteFile.Arch.ARM
        Architecture.ARCH_ARM64 -> RemoteData.RemoteFile.Arch.ARM64
        Architecture.ARCH_X86 -> RemoteData.RemoteFile.Arch.X86
        Architecture.ARCH_X86_64 -> RemoteData.RemoteFile.Arch.X86_64
        else -> RemoteData.RemoteFile.Arch.ALL
    } }
    var selected by remember(data) { mutableStateOf(data.files.find { it.arch == arch }) }
    DlgFrame(stringResource(R.string.upgrade_files), onDismiss) {
        Column(Modifier.heightIn(max = 210.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            data.files.forEach { file -> DlgButton(
                (if (selected == file) "✓ " else "") + file.fileName + " · " + file.arch + " · " + formatFileSize(file.size),
                { selected = file }, modifier = Modifier.fillMaxWidth(), enabled = file.arch == arch || file.arch == RemoteData.RemoteFile.Arch.ALL)
            }
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DlgButton(stringResource(R.string.generic_cancel), onDismiss)
            DlgButton(stringResource(R.string.generic_download), { selected?.let(onFile) }, enabled = selected != null)
        }
    }
}

@Composable
fun AllCloudDriveDialog(drive: RemoteData.CloudDrive, onDismiss: () -> Unit, onLink: (String) -> Unit) {
    var selected by remember(drive) { mutableStateOf<RemoteData.CloudDrive.Link?>(null) }
    DlgFrame(stringResource(R.string.upgrade_cloud_drive), onDismiss) {
        Column(Modifier.heightIn(max = 210.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            drive.links.forEach { link -> DlgButton((if (selected == link) "✓ " else "") + link.name, { selected = link }, modifier = Modifier.fillMaxWidth()) }
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DlgButton(stringResource(R.string.generic_cancel), onDismiss)
            DlgButton(stringResource(R.string.generic_confirm), { selected?.let { onLink(it.link) }; onDismiss() }, enabled = selected != null)
        }
    }
}

@Composable
fun AllLogMenu(onView: () -> Unit, onShare: () -> Unit, canUpload: Boolean, onUpload: () -> Unit, onClose: () -> Unit) {
    DlgFrame(stringResource(R.string.crash_share_logs), onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DlgButton(stringResource(R.string.generic_view), onView, modifier = Modifier.fillMaxWidth())
            DlgButton(stringResource(R.string.crash_share_logs), onShare, modifier = Modifier.fillMaxWidth())
            DlgButton(stringResource(R.string.crash_link_share_button), onUpload, modifier = Modifier.fillMaxWidth(), enabled = canUpload)
            DlgButton(stringResource(R.string.generic_close), onClose, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun AllLogUploadingDialog(message: String, onCancel: () -> Unit) {
    DlgFrame(stringResource(R.string.crash_link_share_button), {}) {
        Text(message, color = Color(0xFF1B1B1B))
        LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 12.dp))
        DlgButton(stringResource(R.string.generic_cancel), onCancel)
    }
}
