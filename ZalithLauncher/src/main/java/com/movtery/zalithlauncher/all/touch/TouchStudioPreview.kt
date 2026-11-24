package com.movtery.zalithlauncher.all.touch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.movtery.zalithlauncher.all.ui.AllMaterialTheme

/**
 * The SAME [TouchControlEditor] the game opens, shown from Settings before any game runs.
 * Behind it: a sample game screen (sky, ground, hotbar, crosshair) so buttons are placed against what the game shows.
 * Edits save to [TouchStore] as always, so the game picks them up at launch.
 */
@Composable
fun TouchStudioPreview(onExit: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onExit, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = false)) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                val h = size.height; val w = size.width
                drawRect(Brush.verticalGradient(listOf(Color(0xFF7FB2F0), Color(0xFFBBD9F7)), 0f, h * 0.55f))
                drawRect(Color(0xFF5E9E3C), Offset(0f, h * 0.55f), Size(w, h * 0.12f))
                drawRect(Color(0xFF7A5230), Offset(0f, h * 0.67f), Size(w, h * 0.33f))
                val slot = h * 0.11f; val x0 = (w - slot * 9) / 2; val y0 = h - slot - h * 0.03f
                for (i in 0 until 9) {
                    drawRect(Color(0xAA000000), Offset(x0 + i * slot, y0), Size(slot, slot))
                    drawRect(Color(0xFF8B8B8B), Offset(x0 + i * slot, y0), Size(slot, slot), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
                }
                val c = Offset(w / 2, h / 2); val a = h * 0.02f
                drawLine(Color.White, Offset(c.x - a, c.y), Offset(c.x + a, c.y), 3f)
                drawLine(Color.White, Offset(c.x, c.y - a), Offset(c.x, c.y + a), 3f)
            }
            AllMaterialTheme { TouchControlEditor(remember { TouchStore.load(context) }, onExit, preview = true) }
        }
    }
}
