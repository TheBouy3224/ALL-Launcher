package com.movtery.zalithlauncher.all.accounts

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Microsoft's sign-in page, shown inside ALL as a Custom Tab (the same task as ALL, a browser that Microsoft supports, with the
 * user's saved Microsoft session, passkeys and password manager). ALL never shows its own password field.
 *
 * Zalith 2 used an embedded WebView for this. A WebView has no saved session, passkeys or password manager and blocks the
 * Google/GitHub sign-in buttons on Microsoft's page, so the Custom Tab is the supported way to keep the flow "inside" the app.
 * No androidx.browser dependency: these are the public Custom Tabs protocol extras that CustomTabsIntent itself sets.
 * A browser without Custom Tabs support simply opens the page normally (same as before).
 */
private const val EXTRA_SESSION = "android.support.customtabs.extra.SESSION"
private const val EXTRA_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR"
private const val EXTRA_TITLE_VISIBILITY = "android.support.customtabs.extra.TITLE_VISIBILITY"
const val EXTRA_ALL_LOGIN_RETURN = "all.login.return"

fun microsoftPageIntent(url: String): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
    putExtras(Bundle().apply { putBinder(EXTRA_SESSION, null) })
    putExtra(EXTRA_TOOLBAR_COLOR, 0xFF0A5BE0.toInt()) //ALL / XP blue, so it reads as part of ALL
    putExtra(EXTRA_TITLE_VISIBILITY, 1)
}

/** Opens the page in ALL's own task when possible (needed for [returnToAll]); falls back to a new task from a non-activity context */
fun openMicrosoftPage(context: Context, url: String): Boolean {
    val activity = context.findActivity()
    val intent = microsoftPageIntent(url).apply { if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    return runCatching { (activity ?: context).startActivity(intent) }.isSuccess
}

/**
 * Brings ALL back over the Custom Tab after sign-in: the tab lives above ALL in the same task, so CLEAR_TOP closes it.
 * The extra tells the activity this is only a return (no reloads in onNewIntent).
 */
fun returnToAll(context: Context) {
    val activity = context.findActivity() ?: return
    runCatching {
        activity.startActivity(Intent(activity, activity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_ALL_LOGIN_RETURN, true))
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Closing the Microsoft page (back / ✕ on the tab) without signing in counts as cancel, like Zalith's old "left the web screen".
 * ALL went to the background while [waiting] and came back still waiting: give the device-code poll [graceMs] to pick up a sign-in
 * that just finished, then [onCancel]. A successful sign-in leaves [waiting] first, so it is never cancelled.
 */
@androidx.compose.runtime.Composable
//30 s: vendor freezers (TECNO HiOS "Hiber") stop ALL while the tab is open; after the return the poll needs time to reconnect
fun CancelWhenBackFromMicrosoftPage(waiting: Boolean, graceMs: Long = 30_000, onCancel: () -> Unit) {
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var left by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var returned by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val currentWaiting by androidx.compose.runtime.rememberUpdatedState(waiting)
    androidx.compose.runtime.DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP && currentWaiting) left = true
            if (e == androidx.lifecycle.Lifecycle.Event.ON_START && left) { left = false; if (currentWaiting) returned++ }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    androidx.compose.runtime.LaunchedEffect(returned) {
        if (returned == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(graceMs)
        //reopened the page meanwhile (ALL is in the background again): not a cancel; the next return restarts the wait
        if (currentWaiting && owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) onCancel()
    }
}
