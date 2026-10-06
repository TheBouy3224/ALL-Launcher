package com.movtery.zalithlauncher.all.about

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

private val AboutSurface = Color(0xFFF3F1E8)
private val AboutCard = Color(0xFFFFFFFF)
private val AboutBorder = Color(0xFFB9B4A2)
private val AboutText = Color(0xFF1B1B1B)
private val AboutDim = Color(0xFF6B675C)
private val AboutAccent = Color(0xFF316AC5)

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AboutSurface)
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { Section(stringResource(R.string.about_launcher_title)) }
        item {
            Entry(
                icon = R.drawable.img_launcher,
                title = BuildKeys.LAUNCHER_NAME,
                text = stringResource(R.string.about_launcher_version, BuildConfig.VERSION_NAME),
                extra = "versionCode ${BuildConfig.VERSION_CODE} · ${BuildConfig.APPLICATION_ID} · ${BuildConfig.BUILD_TYPE}\nBased on ZalithLauncher2 (GPL-3.0)",
                links = { Link(stringResource(R.string.about_launcher_project_link)) { open(URL_PROJECT) } }
            )
        }
        item {
            Entry(
                icon = R.drawable.img_avatar_movtery,
                title = stringResource(R.string.about_launcher_author_movtery_title),
                text = stringResource(R.string.about_launcher_author_movtery_text, short),
                links = { Link(stringResource(R.string.about_sponsor)) { open(URL_SUPPORT) } }
            )
        }
        item { Section(stringResource(R.string.about_acknowledgements_title)) }
        items(credits) { c ->
            Entry(
                icon = c.icon,
                title = c.title,
                text = c.text,
                links = {
                    c.licenseRaw?.let { raw -> Link("License") { licenseRaw = raw } }
                    c.url?.let { u -> Link("Link") { open(u) } }
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
                extra = "Licensed under the ${info.license.name}",
                links = {
                    Link("License") { licenseRaw = info.license.raw }
                    Link("Link") { open(info.webUrl) }
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
        color = AboutText,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
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
            .clip(RoundedCornerShape(6.dp))
            .background(AboutCard)
            .border(1.dp, AboutBorder, RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        if (icon != null) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AboutText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        color = AboutAccent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    )
}
