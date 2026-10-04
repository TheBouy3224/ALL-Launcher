package com.movtery.zalithlauncher.all.overlays

import android.app.Dialog
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.theme.ZalithLauncherTheme
import com.movtery.zalithlauncher.ui.vulkan_checker.VCOperation
import com.movtery.zalithlauncher.utils.device.VulkanDependency
import com.movtery.zalithlauncher.utils.device.VulkanRequirements
import com.movtery.zalithlauncher.utils.device.profileSupport
import com.movtery.zalithlauncher.utils.device.supports

/** SimpleTaskDialog still owns its task/state/catch/finally. This replaces only its busy presentation. */
@Composable
fun AllBusyDialog(title: String, text: String?) {
    DlgFrame(title, {}) {
        text?.let { Text(it, color = Color(0xFF1B1B1B)) }
        LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
    }
}

/** Same noncancelable authorization/ignore contract as the original storage prompt. */
fun showAllPermissionDialog(activity: ComponentActivity, title: String, message: String,
                            onRequest: () -> Unit, onCancel: () -> Unit) =
    showActionDialog(activity, title, message, false, listOf(
        activity.getString(R.string.generic_ignore) to onCancel,
        activity.getString(R.string.generic_authorization) to onRequest))

/** Normal launcher links retain their open/cancel/copy actions and ACTION_VIEW backend. */
fun showAllLinkDialog(activity: ComponentActivity, link: String, onOpen: () -> Unit, onCopy: () -> Unit) =
    showActionDialog(activity, activity.getString(R.string.generic_open_link), link, true, listOf(
        activity.getString(R.string.generic_copy) to onCopy,
        activity.getString(R.string.generic_cancel) to {},
        activity.getString(R.string.generic_confirm) to onOpen))

private fun showActionDialog(activity: ComponentActivity, title: String, message: String,
                             cancelable: Boolean, actions: List<Pair<String, () -> Unit>>) {
    val dialog = Dialog(activity)
    val view = ComposeView(activity)
    view.setViewTreeLifecycleOwner(activity)
    view.setViewTreeViewModelStoreOwner(activity)
    view.setViewTreeSavedStateRegistryOwner(activity)
    dialog.setContentView(view)
    dialog.setCancelable(cancelable)
    view.setContent {
        ZalithLauncherTheme {
            Column(Modifier.widthIn(max = 600.dp).background(Color(0xFFF3F1E8))) {
                Text(title, color = Color.White, modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF316AC5)).padding(12.dp))
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(message, color = Color(0xFF1B1B1B),
                        modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        actions.forEach { (label, action) -> DlgButton(label, { action(); dialog.dismiss() }) }
                    }
                }
            }
        }
    }
    dialog.show()
}

/** Existing Vulkan check state/callbacks; no capability detection, driver changes or persistence here. */
@Composable
fun AllVulkanChecker(operation: VCOperation, onChange: (VCOperation) -> Unit,
                     startCheck: (Version) -> Unit, confirmResult: () -> Unit) {
    when (operation) {
        VCOperation.None -> Unit
        is VCOperation.Tip -> DlgFrame(stringResource(R.string.game_vulkan_check_title), {}) {
            Text(stringResource(R.string.game_vulkan_check_text), color = Color(0xFF1B1B1B))
            DlgButton(stringResource(R.string.generic_confirm), { startCheck(operation.version) },
                modifier = Modifier.padding(top = 12.dp))
        }
        is VCOperation.Result -> DlgFrame(stringResource(R.string.game_vulkan_check_title), {}) {
            Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val data = operation.data
                if (data == null) {
                    Detail(stringResource(R.string.game_vulkan_check_failed))
                } else {
                    val profiles = data.profileSupport()
                    Detail(when {
                        profiles.all { it.supported } -> stringResource(R.string.game_vulkan_check_supp, profiles.first().since)
                        profiles.none { it.supported } -> stringResource(R.string.game_vulkan_check_unsupp)
                        else -> stringResource(R.string.game_vulkan_check_partial)
                    })
                    if (profiles.distinctBy { it.supported }.size > 1) {
                        Detail(stringResource(R.string.game_vulkan_check_versions))
                        profiles.forEach { p -> Detail("Minecraft ${p.versionRangeText}: " + stringResource(
                            if (p.supported) R.string.game_vulkan_check_profile_supp else R.string.game_vulkan_check_profile_unsupp)) }
                    }
                    Detail(stringResource(R.string.game_vulkan_check_version, data.versionString))
                    Detail(stringResource(R.string.game_vulkan_check_turnip, operation.useTurnip))
                    Detail(stringResource(R.string.game_vulkan_check_extensions))
                    VulkanRequirements.EXTENSIONS.forEach { Dependency(it, data.supports(it)) }
                    Detail(stringResource(R.string.game_vulkan_check_features))
                    VulkanRequirements.FEATURES.forEach { Dependency(it, data.supports(it)) }
                }
            }
            DlgButton(stringResource(R.string.generic_confirm), { confirmResult(); onChange(VCOperation.None) },
                modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun Detail(text: String) = Text(text, color = Color(0xFF1B1B1B))

@Composable
private fun Dependency(dependency: VulkanDependency, supported: Boolean) {
    Text(dependency.name + if (supported) "" else stringResource(R.string.game_vulkan_check_item_missing),
        color = if (supported) Color(0xFF1B1B1B) else Color(0xFFB3261E))
    if (dependency.requiredIn.isNotEmpty()) Detail(stringResource(R.string.game_vulkan_check_dep_required) + " " +
        dependency.requiredIn.joinToString("  ") { it.displayText })
    if (dependency.optionalIn.isNotEmpty()) Detail(stringResource(R.string.game_vulkan_check_dep_optional) + " " +
        dependency.optionalIn.joinToString("  ") { it.displayText })
}
