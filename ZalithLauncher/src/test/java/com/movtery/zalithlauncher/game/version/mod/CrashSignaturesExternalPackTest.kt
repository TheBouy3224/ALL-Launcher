package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * 外部崩溃规则包：换一份规则不能靠重新打包 APK，但一份写坏的规则包也绝不能让诊断静默失效。
 *
 * 版本门是唯一的安全阀：没有版本、版本为 0、不是数字、比这个启动器新，或者解析出来一条特征都没有，
 * 都必须被拒绝，退回随包规则。
 */
class CrashSignaturesExternalPackTest {
    private val validPack = """
        {
          "signatures_version": 1,
          "signatures": [
            {
              "id": "custom_rule",
              "class": "loader_fatal",
              "match_any": ["Custom fatal line"],
              "confidence": "LOW",
              "action": "none",
              "message_key": "crash_diagnosis_loader_fatal"
            }
          ]
        }
    """.trimIndent()

    private fun packWithVersion(version: String) =
        validPack.replace("\"signatures_version\": 1", "\"signatures_version\": $version")

    private fun tempPack(content: String): Pair<File, File> {
        val dir = Files.createTempDirectory("crash-rule-pack").toFile()
        val file = File(dir, "crash_signatures.json")
        file.writeText(content)
        return dir to file
    }

    /** schema 1 的外部规则包：返回的是它自己的条目，不是随包的那三条 */
    @Test
    fun aSchemaOnePackReplacesTheBundledRules() {
        val signatures = parseExternalRulePack(validPack)

        assertNotNull(signatures)
        assertEquals(listOf("custom_rule"), signatures!!.map { it.id })
        assertEquals("loader_fatal", signatures.single().className)
        assertEquals("crash_diagnosis_loader_fatal", signatures.single().messageKey)
    }

    @Test
    fun aNewerSchemaIsRejected() {
        assertNull(parseExternalRulePack(packWithVersion("99")))
        assertNull(parseExternalRulePack(packWithVersion("2")))
    }

    @Test
    fun aMissingOrZeroVersionIsRejected() {
        assertNull(parseExternalRulePack(packWithVersion("0")))
        assertNull(parseExternalRulePack(validPack.replace("\"signatures_version\": 1,", "")))
    }

    @Test
    fun aNonNumericVersionIsRejected() {
        assertNull(parseExternalRulePack(packWithVersion("\"one\"")))
    }

    @Test
    fun brokenJsonIsRejected() {
        assertNull(parseExternalRulePack("not json at all"))
        assertNull(parseExternalRulePack(""))
        assertNull(parseExternalRulePack("""{"signatures_version":1,"signatures":"nope"}"""))
        assertNull(parseExternalRulePack("""[1,2,3]"""))
    }

    /** 空的条目表会让所有诊断静默失效，所以它同样不算一份有效规则包 */
    @Test
    fun anEmptySignatureListIsRejected() {
        assertNull(parseExternalRulePack("""{"signatures_version":1,"signatures":[]}"""))
        assertNull(parseExternalRulePack("""{"signatures_version":1}"""))
        assertNull(
            parseExternalRulePack(
                """{"signatures_version":1,"signatures":[{"class":"loader_fatal","match_any":["x"]}]}"""
            )
        )
    }

    // ---- 文件这一层：真正被 load() 调用的读取路径 ----

    @Test
    fun aFileThatPassesTheGateIsRead() {
        val (dir, file) = tempPack(validPack)
        try {
            val signatures = readExternalRulePack(file)

            assertNotNull(signatures)
            assertEquals(listOf("custom_rule"), signatures!!.map { it.id })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun aMissingFileYieldsNoExternalRules() {
        val dir = Files.createTempDirectory("crash-rule-pack-missing").toFile()
        try {
            assertNull(readExternalRulePack(File(dir, "crash_signatures.json")))
            assertNull(readExternalRulePack(File(dir, "rules/crash_signatures.json")))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun aFileThatFailsTheGateIsRejected() {
        val (dir, file) = tempPack(packWithVersion("99"))
        try {
            assertNull(readExternalRulePack(file))
        } finally {
            dir.deleteRecursively()
        }
    }

    /** 随包资源仍然按原样解析：外部规则包这条路不能改变它 */
    @Test
    fun theBundledAssetStillParsesToTheSameThreeSignatures() {
        val asset = File("src/main/assets/crash_signatures.json")
        assertTrue("the shipped signature file is missing: ${asset.absolutePath}", asset.isFile)

        val signatures = parseCrashSignatures(asset.readText())

        assertEquals(
            listOf("platform_limit_native", "mixin_apply_failed", "loader_fatal_unattributed"),
            signatures.map { it.id },
        )
        assertEquals(3, signatures.size)
    }
}
