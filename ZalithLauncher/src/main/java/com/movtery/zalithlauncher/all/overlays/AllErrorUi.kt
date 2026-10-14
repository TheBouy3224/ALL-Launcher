package com.movtery.zalithlauncher.all.overlays

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import com.movtery.zalithlauncher.all.ui.AllColors
import com.movtery.zalithlauncher.all.ui.AllDimens
import android.app.Dialog
import android.content.Context
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.activity.ComponentActivity
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.theme.ZalithLauncherTheme
import com.movtery.zalithlauncher.utils.copyText

/** Activity-owned presentation; noncancelable and retains the caller's close/copy contract. */
fun showAllErrorDialog(context: Context, title: String, message: String, copyDetails: String = message,
                       closeAfterCopy: Boolean = false, onClose: () -> Unit = {}) {
    val activity = context as ComponentActivity
    val dialog = Dialog(context)
    val view = ComposeView(context)
    view.setViewTreeLifecycleOwner(activity)
    view.setViewTreeViewModelStoreOwner(activity)
    view.setViewTreeSavedStateRegistryOwner(activity)
    dialog.setContentView(view)
    dialog.setCancelable(false)
    view.setContent {
        ZalithLauncherTheme {
            AllErrorDialog(title, message, onCopy = {
                copyText(title, copyDetails, context)
                if (closeAfterCopy) { dialog.dismiss(); onClose() }
            }, onClose = { dialog.dismiss(); onClose() })
        }
    }
    dialog.show()
}

@Composable
private fun AllErrorDialog(title: String, message: String, onCopy: () -> Unit, onClose: () -> Unit) {
    // The host Dialog owns dismissal; this frame does not create a second platform window.
    Column(Modifier.widthIn(max = AllDimens.DialogMaxWidth).clip(RoundedCornerShape(AllDimens.Radius)).background(AllColors.WindowBody)) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().background(AllColors.Danger).padding(horizontal = 14.dp, vertical = 10.dp))
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectionContainer { Text(message, color = AllColors.Text, fontSize = 14.sp, modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                DlgButton(stringResource(R.string.generic_copy), onCopy)
                DlgButton(stringResource(R.string.generic_confirm), onClose)
            }
        }
    }
}

@Composable
fun AllErrorScreen(title: String, type: String, diagnosisLines: List<String>, details: String,
                   shareLogs: Boolean, canUpload: Boolean, canRestart: Boolean,
                   repairActionLabel: String?, onRepair: () -> Unit, fixActionLabel: String?,
                   onFix: () -> Unit, onShare: () -> Unit, onUpload: () -> Unit, onRestart: () -> Unit,
                   onExit: () -> Unit, onCopyLog: (() -> Unit)?, body: @Composable ColumnScope.() -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Color(0xFFF3F1E8)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Color(0xFF316AC5))
        Text(type, color = Color(0xFF1B1B1B))
        SelectionContainer(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color.White).padding(12.dp)) {
                CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Color(0xFF1B1B1B)) { body() }
                diagnosisLines.forEach { Text(it, color = Color(0xFF1B1B1B)) }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DlgButton(stringResource(R.string.generic_copy), { copyText(title, details, context) })
            onCopyLog?.let { DlgButton(stringResource(R.string.crash_share_logs) + " / " + stringResource(R.string.generic_copy), it) }
            //安装依赖并修复是主要动作；禁用只是临时的备用动作，排在它后面
            repairActionLabel?.let { AllButton(it, onRepair, style = AllButtonStyle.Primary, compact = true) }
            fixActionLabel?.let { DlgButton(it, onFix) }
            if (shareLogs) DlgButton(stringResource(R.string.crash_share_logs), onShare)
            if (canUpload) DlgButton(stringResource(R.string.crash_link_share_button), onUpload)
            if (canRestart) DlgButton(stringResource(R.string.crash_restart), onRestart)
            DlgButton(stringResource(R.string.crash_exit), onExit)
        }
    }
}
