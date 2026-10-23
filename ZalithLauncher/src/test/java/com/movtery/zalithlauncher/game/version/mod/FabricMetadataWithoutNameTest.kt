package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.version.mod.reader.FabricModMetadataReader
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * `name` 和 `version` 在 fabric.mod.json 里是可选的。真实用例：Essential 1.5.0.1（Modrinth，SHA-1 已校验）
 * 的 fabric.mod.json 没有 name；旧代码把 null 塞进非空参数，整个模组被当成“不是模组”，
 * 真机的 Mod Doctor 因此对一个正常的模组报“读取不了元数据”并拦住启动。
 */
class FabricMetadataWithoutNameTest {
    private lateinit var dir: File

    @Before
    fun setUp() { dir = Files.createTempDirectory("fabric-noname").toFile() }

    @After
    fun tearDown() { dir.deleteRecursively() }

    //逐字取自真实 Essential 1.5.0.1 jar 的 fabric.mod.json（没有 name）
    private val essentialJson = """
        {
          "schemaVersion": 1,
          "id": "essential-container",
          "version": "1.0.0",
          "environment": "client",
          "icon": "assets/essential-container/essential.png",
          "custom": { "modmenu": { "badges": ["library"], "parent": { "id": "essential", "name": "Essential" } } },
          "jars": [ { "file": "essential-loader.jar" } ]
        }
    """.trimIndent()

    private fun jar(json: String): File = File(dir, "mod.jar").also { file ->
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("fabric.mod.json")); zip.write(json.toByteArray()); zip.closeEntry()
        }
    }

    @Test
    fun aFabricModWithoutANameIsStillAMod() = runBlocking {
        val local = FabricModMetadataReader.fromLocal(jar(essentialJson))

        assertFalse(local.notMod)
        assertEquals("essential-container", local.id)
        assertEquals("the id is the fallback display name", "essential-container", local.name)
        assertEquals(ModLoader.FABRIC, local.loader)
        assertEquals("1.0.0", local.version)
    }

    @Test
    fun aFabricModWithoutAVersionIsStillAMod() = runBlocking {
        val local = FabricModMetadataReader.fromLocal(jar("""{"schemaVersion":1,"id":"noversion","name":"No Version"}"""))

        assertFalse(local.notMod)
        assertEquals("No Version", local.name)
        assertEquals(null, local.version)
    }

    @Test
    fun aBlankNameFallsBackToTheId() = runBlocking {
        assertEquals("blanky", FabricModMetadataReader.fromLocal(jar("""{"schemaVersion":1,"id":"blanky","name":"  ","version":"1"}""")).name)
    }

    @Test
    fun modDoctorReadsItAsHealthyInsteadOfCorruptMetadata() = runBlocking {
        val local = FabricModMetadataReader.fromLocal(jar(essentialJson))

        val report = validateModDoctorMetadata(
            mods = listOf(ModDoctor().toDoctorMetadata(local, ModLoader.FABRIC)),
            minecraftVersion = "1.21.11",
            loader = ModLoader.FABRIC,
        )

        assertTrue("got ${report.problems}", report.problems.none { it.type == ModDoctorProblemType.CORRUPT_METADATA })
    }
}
