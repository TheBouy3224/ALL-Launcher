package com.movtery.zalithlauncher.all.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Windows XP "Luna" pieces shared by the first-run setup wizard and the login gate */

//Windows XP "Luna" palette
internal val XpDesktop = Color(0xFF3A6EA5)
internal val XpBorder = Color(0xFF0831D9)
internal val XpBody = Color(0xFFECE9D8)
internal val XpInk = Color(0xFF000000)
internal val XpMuted = Color(0xFF55554F)
internal val XpTitle = Brush.verticalGradient(listOf(Color(0xFF3D95FF), Color(0xFF0A5BE0), Color(0xFF0042C8)))
internal val XpGreen = Brush.verticalGradient(listOf(Color(0xFFB4F2A8), Color(0xFF2FC82F), Color(0xFF1E9E1E)))
internal val XpError = Color(0xFFC00000)
internal val XpOk = Color(0xFF1E8E1E)

@Composable
internal fun XpTitleBar(title: String) {
    Row(
        Modifier.fillMaxWidth().height(30.dp).background(XpTitle).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("ALL", color = Color.White, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, fontSize = 16.sp)
        Text(
            title,
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** Desktop-blue backdrop with one rounded, blue-bordered XP window that holds [content] under the title bar */
@Composable
internal fun XpWindow(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(XpDesktop).padding(6.dp)) {
        Column(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .border(2.dp, XpBorder, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(XpBody)
        ) {
            XpTitleBar(title)
            content()
        }
    }
}

/** Classic XP push button; [default] gets the heavier blue border */
@Composable
internal fun XpButton(text: String, onClick: () -> Unit, default: Boolean = false, enabled: Boolean = true) {
    val shape = RoundedCornerShape(3.dp)
    val face = if (enabled) Brush.verticalGradient(listOf(Color.White, Color(0xFFECEBE5), Color(0xFFD6D0C5)))
    else Brush.verticalGradient(listOf(Color(0xFFF5F4EF), Color(0xFFF5F4EF)))
    Box(
        Modifier.widthIn(min = 96.dp).heightIn(min = 38.dp)
            .clip(shape).background(face)
            .border(if (default && enabled) BorderStroke(2.dp, Color(0xFF316AC5)) else BorderStroke(1.dp, if (enabled) Color(0xFF003C74) else Color(0xFFC9C7BA)), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(PaddingValues(horizontal = 14.dp, vertical = 6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) XpInk else Color(0xFF9A9A8E), fontSize = 16.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}
