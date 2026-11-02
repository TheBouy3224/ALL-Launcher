/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.renderer.renderers

import com.movtery.zalithlauncher.game.renderer.RendererInterface
import com.movtery.zalithlauncher.path.PathManager
import org.json.JSONObject
import java.io.File

/**
 * Built-in MobileGlues (GL 4.x on GLES 3.2), ported from upstream Zalith Launcher 2 commit 737252b2.
 * Needed for Minecraft 26.3+: its OpenGL backend rejects Krypton Wrapper ("glGetError mismatch").
 * ALL keeps upstream's defaults and does not port Zalith's renderer-config UI; a user's own
 * mobileglues/config.json is never overwritten.
 */
object MobileGluesRenderer : RendererInterface {
    override fun getRendererId(): String = "opengles3_mobileglues"

    //same id as upstream so version configs stay portable
    override fun getUniqueIdentifier(): String = "acaf8100-bbaf-4d68-b30d-38bf42fcd640"

    override fun getRendererName(): String = "MobileGlues"

    override fun getRendererSummary(): String = "OpenGL 4.0"

    override fun getMinMCVersion(): String = "1.17"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        val mgDir = File(PathManager.DIR_FILES_PRIVATE, "mobileglues")
        writeDefaultConfig(mgDir)
        invalidateStaleShaderCache(mgDir)
        mapOf(
            "LIBGL_ES" to "3",
            "LIBGL_EGL" to "libmobileglues.so",
            "MG_COUNT_LAUNCH" to "1",
            "MG_DIR_PATH" to mgDir.absolutePath
        )
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libmobileglues.so"

    override fun getRendererEGL(): String = "libmobileglues.so"

    /**
     * Build of the bundled libmobileglues.so. MobileGlues keys glsl_cache.tmp only by the SHA-256 of the
     * input GLSL, so translations made by an older (broken) build would be reused after an update.
     * Bump this whenever ZalithLauncher/libs/MobileGlues-release.aar changes.
     */
    private const val LIB_BUILD = "mg-2.0.0+97558a69+all-mc26.3-glsl-2"

    /** a changed library build sets the old shader cache aside (renamed, never deleted) */
    private fun invalidateStaleShaderCache(mgDir: File) {
        val stamp = File(mgDir, "all-lib-build")
        if (stamp.isFile && stamp.readText() == LIB_BUILD) return
        mgDir.mkdirs()
        val cache = File(mgDir, "glsl_cache.tmp")
        if (cache.isFile) cache.renameTo(File(mgDir, "glsl_cache.tmp.stale"))
        stamp.writeText(LIB_BUILD)
    }

    /** upstream defaults; only written when there is no config yet */
    private fun writeDefaultConfig(mgDir: File) {
        val file = File(mgDir, "config.json")
        if (file.isFile) return
        mgDir.mkdirs()
        file.writeText(
            JSONObject()
                .put("enableANGLE", 1)
                .put("enableNoError", 0)
                .put("customGLVersion", 0)
                .put("fsr1Setting", 0)
                .put("angleDepthClearFixMode", false)
                .put("maxGlslCacheSize", 32)
                .put("enableExtTimerQuery", true)
                .put("enableExtComputeShader", false)
                .put("enableExtDirectStateAccess", false)
                .toString(2)
        )
    }
}
