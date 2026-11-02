package com.movtery.zalithlauncher.all.about

import com.movtery.zalithlauncher.all.ui.AllColors
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.movtery.zalithlauncher.all.ui.AlertDialog
import androidx.compose.material3.Text
import com.movtery.zalithlauncher.all.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.bridge.AllToast
import com.movtery.zalithlauncher.context.readRawContent
import com.movtery.zalithlauncher.game.plugin.PluginLoader
import com.movtery.zalithlauncher.library.libraryData
import com.movtery.zalithlauncher.path.URL_COMMUNITY
import com.movtery.zalithlauncher.path.URL_MCMOD
import com.movtery.zalithlauncher.path.URL_PROJECT
import com.movtery.zalithlauncher.path.URL_SUPPORT
import com.movtery.zalithlauncher.path.URL_WEBLATE
import com.movtery.zalithlauncher.ui.androidText

/**
 * ALL 自己的「关于」窗口内容（PHASE 3.1）。
 *
 * 数据全部沿用 Zalith 既有来源，没有丢任何一项：
 * 启动器名称/版本（BuildConfig + BuildKeys 文案）、作者与致谢（含 FCL / HMCL / PojavLauncher 协议文本）、
 * 第三方库清单（[libraryData]，含版权与协议）、已加载插件（[PluginLoader.allPlugins]）。
 * 协议文本直接在窗口内用对话框显示，链接交给系统浏览器；不再打开 Zalith 的设置/关于页面。
 */

private val AboutSurface: Color get() = AllColors.WindowBody
private val AboutCard: Color get() = AllColors.Card
private val AboutBorder: Color get() = AllColors.Border
private val AboutLink = Color(0xFF8DB4FF)
private val AboutText: Color get() = AllColors.Text
private val AboutDim: Color get() = AllColors.TextDim
private val AboutAccent: Color get() = AllColors.Blue

/** 致谢条目：[title] 为空时用 [titleRes] */
private class Credit(
    val icon: Int,
    val title: String,
    val text: String,
    val url: String? = null,
    val licenseRaw: Int? = null,
    val sponsorUrl: String? = null,
)

@Composable
fun AllAboutWindow(
    showToast: (AllToast) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val short = BuildKeys.LAUNCHER_SHORT_NAME
    var licenseRaw by remember { mutableStateOf<Int?>(null) }

    fun open(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { showToast(androidText(url)) }
    }

    val credits = listOf(
        Credit(R.drawable.img_avatar_bangbang93, "bangbang93", stringResource(R.string.about_acknowledgements_bangbang93_text, short), sponsorUrl = "https://ifdian.net/a/bangbang93"),
        Credit(R.drawable.img_launcher_fcl, "Fold Craft Launcher", stringResource(R.string.about_acknowledgements_fcl_text, short), "https://github.com/FCL-Team/FoldCraftLauncher", R.raw.fcl_license),
        Credit(R.drawable.img_launcher_hmcl, "Hello Minecraft! Launcher", stringResource(R.string.about_acknowledgements_hmcl_text, short), "https://github.com/HMCL-dev/HMCL", R.raw.hmcl_license),
        Credit(R.drawable.img_platform_mcmod, stringResource(R.string.about_acknowledgements_mcmod), stringResource(R.string.about_acknowledgements_mcmod_text, short), URL_MCMOD),
        Credit(R.drawable.img_avatar_mcim, "mcmod-info-mirror", stringResource(R.string.about_acknowledgements_mcim_text, short), sponsorUrl = "https://www.mcimirror.top/sponsor"),
        Credit(R.drawable.img_launcher_pcl2, "Plain Craft Launcher 2", stringResource(R.string.about_acknowledgements_pcl_text, short), "https://github.com/Meloong-Git/PCL"),
        Credit(R.drawable.img_launcher_pojav, "PojavLauncher", stringResource(R.string.about_acknowledgements_pojav_text, short), "https://github.com/PojavLauncherTeam/PojavLauncher", R.raw.lgpl_3_license),
        Credit(R.drawable.ic_github, stringResource(R.string.about_acknowledgements_github_community), stringResource(R.string.about_acknowledgements_github_community_text), URL_COMMUNITY),
        Credit(R.drawable.img_platform_weblate, stringResource(R.string.about_acknowledgements_weblate_community), stringResource(R.string.about_acknowledgements_weblate_community_text), URL_WEBLATE),
    )
    val plugins = remember { PluginLoader.allPlugins }

    var sub by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = sub) { sub = false }
    val upstreamText = stringResource(R.string.all_about_upstream_author_text)

    if (!sub) {
        //Main page: only the app header and the two people behind it. Everything else sits one tap away (licences still require it).
        androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxSize().background(AboutSurface)) {
            val wide = maxWidth >= 520.dp
            val header: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.movtery.zalithlauncher.R.drawable.img_all_logo), "ALL", Modifier.size(68.dp))
                    Text(BuildKeys.LAUNCHER_NAME, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 19.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(stringResource(R.string.about_launcher_version, BuildConfig.VERSION_NAME), color = AboutDim, fontSize = 13.sp)
                    Text(stringResource(R.string.all_unofficial_notice), color = AboutDim, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            val people: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Person(
                        icon = R.drawable.img_launcher, title = "THE_BOUY2", text = stringResource(R.string.all_about_owner_text),
                        links = {}
                    )
                    Person(
                        icon = R.drawable.img_avatar_movtery, title = stringResource(R.string.about_launcher_author_movtery_title), text = upstreamText,
                        links = {
                            Link(stringResource(R.string.all_about_upstream_link)) { open(URL_PROJECT) }
                            Link(stringResource(R.string.about_sponsor)) { open(URL_SUPPORT) }
                        }
                    )
                    CreditsRow { sub = true }
                }
            }
            val footer: @Composable () -> Unit = {
                Text(
                    stringResource(R.string.all_about_upstream_license_text),
                    color = AboutDim, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Link(stringResource(R.string.all_about_license)) { licenseRaw = R.raw.gpl_3_license }
                }
                Text(stringResource(R.string.all_about_source_offer), color = AboutDim, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            if (wide) Row(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(0.8f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { header(); footer() }
                Column(Modifier.weight(1.2f)) { people() }
            } else Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
            ) { header(); people(); footer() }
        }
    } else LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AboutSurface)
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("‹", color = AboutLink, fontSize = 26.sp, modifier = Modifier.clickable { sub = false }.padding(horizontal = 10.dp, vertical = 2.dp))
                Text(stringResource(R.string.all_about_credits_title), color = AboutText, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            }
        }
        item { Section(stringResource(R.string.about_launcher_title)) }
        //technical facts, all read from the running app (nothing invented)
        item {
            Entry(
                icon = null,
                title = BuildKeys.LAUNCHER_NAME,
                text = stringResource(R.string.about_launcher_version, BuildConfig.VERSION_NAME),
                extra = "versionCode ${BuildConfig.VERSION_CODE} · ${BuildConfig.APPLICATION_ID} · ${BuildConfig.BUILD_TYPE}",
                links = { Link(stringResource(R.string.all_about_license)) { licenseRaw = R.raw.gpl_3_license } }
            )
        }
        item { Section(stringResource(R.string.about_acknowledgements_title)) }
        items(credits) { c ->
            Entry(
                icon = c.icon,
                title = c.title,
                text = c.text,
                links = {
                    c.licenseRaw?.let { raw -> Link(stringResource(R.string.all_about_license)) { licenseRaw = raw } }
                    c.url?.let { u -> Link(stringResource(R.string.all_about_link)) { open(u) } }
                    c.sponsorUrl?.let { u -> Link(stringResource(R.string.about_sponsor)) { open(u) } }
                }
            )
        }
        item { Section(stringResource(R.string.about_library_title)) }
        items(libraryData) { info ->
            Entry(
                icon = null,
                title = info.name,
                text = info.copyrightInfo.orEmpty(),
                extra = stringResource(R.string.all_about_licensed_under, info.license.name),
                links = {
                    Link(stringResource(R.string.all_about_license)) { licenseRaw = info.license.raw }
                    Link(stringResource(R.string.all_about_link)) { open(info.webUrl) }
                }
            )
        }
        if (plugins.isNotEmpty()) {
            item { Section(stringResource(R.string.about_plugin_title)) }
            items(plugins) { p ->
                Entry(icon = null, title = p.appName, text = p.packageName, extra = p.appVersion, links = {})
            }
        }
        item { Spacer(Modifier.size(8.dp)) }
    }

    licenseRaw?.let { raw ->
        val text = remember(raw) { runCatching { context.readRawContent(raw) }.getOrElse { it.message ?: "?" } }
        AlertDialog(
            onDismissRequest = { licenseRaw = null },
            confirmButton = { TextButton(onClick = { licenseRaw = null }) { Text(stringResource(R.string.generic_close)) } },
            text = {
                Text(
                    text = text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        color = AboutDim,
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp, start = 4.dp)
    )
}

/** One of the two people on the main page: icon, name, one line, links on the right */
@Composable
private fun Person(icon: Int, title: String, text: String, links: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AboutCard).padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Image(painter = painterResource(icon), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = AboutText, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text, color = AboutDim, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) { links() }
    }
}

@Composable
private fun CreditsRow(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 4.dp)
    ) {
        Text(stringResource(R.string.all_about_credits_title), color = AboutText, modifier = Modifier.weight(1f))
        Text("›", color = AboutDim, fontSize = 20.sp)
    }
}

@Composable
private fun Entry(
    icon: Int?,
    title: String,
    text: String,
    extra: String? = null,
    links: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AboutCard)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        if (icon != null) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AboutText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (text.isNotEmpty()) Text(text, color = AboutDim, fontSize = 12.sp)
            extra?.let { Text(it, color = AboutDim, fontSize = 11.sp) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(6.dp))
            links()
        }
    }
}

@Composable
private fun Link(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = AboutLink,
        fontSize = 13.sp,
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    )
}
