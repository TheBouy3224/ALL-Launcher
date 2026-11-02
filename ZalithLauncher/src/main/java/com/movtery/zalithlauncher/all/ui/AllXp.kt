package com.movtery.zalithlauncher.all.ui

import androidx.compose.foundation.layout.size
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

/** First-run setup wizard and login gate pieces in the ALL V3 look (flat navy). The Xp* names are kept so callers did not change. */
internal val XpDesktop = Color(0xFF0E1626)
internal val XpBorder = Color(0xFF6FA8FF)      //accent text
internal val XpBody = Color(0xFF0E1626)
internal val XpInk = Color(0xFFD5DBE8)
internal val XpMuted = Color(0xFF8C97AE)
internal val XpTitle = Brush.verticalGradient(listOf(Color(0xFF223049), Color(0xFF223049)))
internal val XpError = Color(0xFFFF8A8A)
internal val XpOk = Color(0xFF5BD08A)
internal val XpBlue = Color(0xFF2F6FED)
internal val XpCard = Color(0xFF161B28)
internal val XpLine = Color(0xFF2D3548)
internal val XpChip = Color(0xFF232B3D)
/** progress blocks of the setup wizard */
internal val XpGreen = Color(0xFF2F6FED)

@Composable
internal fun XpTitleBar(title: String) {
    Row(
        Modifier.fillMaxWidth().height(40.dp).background(XpTitle).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.movtery.zalithlauncher.R.drawable.img_all_logo), "ALL", Modifier.size(22.dp))
        Text(
            title,
            color = XpInk, fontWeight = FontWeight.Medium, fontSize = 14.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

/** Navy backdrop that holds [content]; the slim title bar is shown only when [title] is not blank */
@Composable
internal fun XpWindow(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(XpDesktop)) {
        if (title.isNotBlank()) XpTitleBar(title)
        content()
    }
}

/** Flat button: [default] = the one solid blue action of the screen */
@Composable
internal fun XpButton(text: String, onClick: () -> Unit, default: Boolean = false, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Box(
        modifier.widthIn(min = 96.dp).heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (!enabled) Color(0xFF1A2030) else if (default) XpBlue else XpChip)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(PaddingValues(horizontal = 14.dp, vertical = 8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = if (!enabled) Color(0xFF5A6680) else if (default) Color.White else XpInk,
            fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1
        )
    }
}
