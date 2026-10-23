package com.movtery.zalithlauncher.all.controls

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.movtery.layer_controller.layout.loadLayoutFromFile
import com.movtery.zalithlauncher.all.ui.AllMaterialTheme
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.base.BaseAppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

private const val EXTRA_FILE = "ALL_STUDIO_FILE"

fun startControlsStudio(context: Context, file: File) {
    context.startActivity(Intent(context, AllControlsStudioActivity::class.java).putExtra(EXTRA_FILE, file.absolutePath))
}

/** Full-screen host for ALL Controls Studio (same full-screen rules as the game, so the editing area has the game's size). */
@AndroidEntryPoint
class AllControlsStudioActivity : BaseAppCompatActivity() {
    override fun isIgnoreNotch(): Boolean = AllSettings.gameFullScreen.getValue()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val file = intent.getStringExtra(EXTRA_FILE)?.let(::File)?.takeIf { it.isFile } ?: return finish()
        val layout = runCatching { loadLayoutFromFile(file) }.getOrNull() ?: return finish()
        setContent { AllMaterialTheme { AllControlsStudio(file, layout, onExit = { finish() }) } }
    }
}
