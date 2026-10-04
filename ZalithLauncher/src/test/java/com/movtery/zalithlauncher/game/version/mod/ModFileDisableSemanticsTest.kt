package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * 错误加载器修复所依赖的可逆禁用语义回归：
 * 原文件必须被保留（重命名为 .disabled）并能完整恢复，字节内容不得变化。
 */
class ModFileDisableSemanticsTest {
    @Test
    fun disablingAndRestoringAModFileKeepsTheOriginalBytes() {
        val dir = Files.createTempDirectory("allmod-disable").toFile()
        try {
            val original = File(dir, "balm-neoforge-1.21.11-21.11.9.1.jar")
            val content = ByteArray(64) { index -> index.toByte() }
            original.writeBytes(content)

            val disabled = original.disableModFile()
            assertTrue(disabled != null)
            assertEquals("balm-neoforge-1.21.11-21.11.9.1.jar.disabled", disabled?.name)
            assertTrue(!original.exists())
            assertTrue(disabled!!.isDisabled())
            assertTrue(disabled.readBytes().contentEquals(content))

            val restored = disabled.enableModFile()
            assertTrue(restored != null)
            assertEquals("balm-neoforge-1.21.11-21.11.9.1.jar", restored?.name)
            assertTrue(restored!!.isEnabled())
            assertTrue(original.exists())
            assertTrue(original.readBytes().contentEquals(content))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun filesAlreadyInTheRequestedStateAreLeftAlone() {
        val dir = Files.createTempDirectory("allmod-disable-noop").toFile()
        try {
            val enabled = File(dir, "enabled.jar").apply { writeBytes(byteArrayOf(1)) }
            assertNull(enabled.enableModFile())
            assertTrue(enabled.exists())

            val disabled = File(dir, "disabled.jar.disabled").apply { writeBytes(byteArrayOf(2)) }
            assertNull(disabled.disableModFile())
            assertTrue(disabled.exists())
        } finally {
            dir.deleteRecursively()
        }
    }
}
