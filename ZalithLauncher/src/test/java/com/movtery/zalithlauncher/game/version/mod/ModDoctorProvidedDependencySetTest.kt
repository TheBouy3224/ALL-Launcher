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

package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 整组校验判定「依赖有没有提供者」时必须与真正的加载器一致：一个文件除了自己的 id，
 * 还提供内嵌模块（`jars` 段里的 jar-in-jar）自己的 id，以及顶层 `provides` 声明的别名 id。
 *
 * 现场形状来自真实四模组冲突（中性 id）：暂存替换 Sodium 时，整组校验报出
 * “modmenu requires fabric-resource-loader-v1, but the resulting set does not provide it”，
 * 而那个模块 id 正是已安装 Fabric API 的内嵌模块（`META-INF/jars/fabric-resource-loader-v1-…jar`）。
 * 加载器能解析这组依赖，只有整组校验看不见它，于是找不到任何计划、整组安全拒绝。
 *
 * 版本规则一字未改：额外提供的 id 只有它自己声明了版本才参与版本判定，
 * 读不出来就保持沉默，绝不借用外层文件的版本。
 */
class ModDoctorProvidedDependencySetTest {
    private fun tempModsDir(): File = Files.createTempDirectory("moddoctor-provided-set").toFile()

    /** 外层模组文件：自己的 `fabric.mod.json`，外加可选的 jar-in-jar 内嵌模块 */
    private fun writeModJar(
        dir: File,
        fileName: String,
        json: String,
        nested: Map<String, String> = emptyMap(),
    ): File {
        val file = File(dir, fileName)
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("fabric.mod.json"))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            nested.forEach { (entryName, nestedJson) ->
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(nestedJarBytes(nestedJson))
                zip.closeEntry()
            }
        }
        return file
    }

    /** 内嵌 jar 的字节内容：它自己的一份 `fabric.mod.json` */
    private fun nestedJarBytes(json: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("fabric.mod.json"))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return bytes.toByteArray()
    }

    private fun quoted(ids: List<String>): String = ids.joinToString(", ", "[", "]") { "\"$it\"" }

    /** 真实形状的 `fabric.mod.json`：顶层 `provides` 报别名，`jars` 段列出内嵌模块 */
    private fun fabricApiJson(provides: List<String>, jarEntries: List<String>): String {
        val jars = jarEntries.joinToString(", ", "[", "]") { entry -> "{\"file\": \"$entry\"}" }
        return "{\"schemaVersion\":1,\"id\":\"$FABRIC_API_ID\",\"version\":\"$FABRIC_API_VERSION\"," +
                "\"name\":\"Fabric API\",\"provides\":${quoted(provides)},\"jars\":$jars," +
                "\"depends\":{\"minecraft\":\"$INSTANCE_MC\"}}"
    }

    private fun moduleJson(id: String, version: String): String =
        "{\"schemaVersion\":1,\"id\":\"$id\",\"version\":\"$version\",\"name\":\"$id\"}"

    /** 已安装的 Fabric API：内嵌模块自己报 id，顶层 `provides` 报别名 */
    private fun fabricApiJar(dir: File): File = writeModJar(
        dir,
        FABRIC_API_FILE,
        fabricApiJson(provides = listOf("fabric"), jarEntries = listOf(NESTED_MODULE_ENTRY)),
        nested = mapOf(NESTED_MODULE_ENTRY to moduleJson(NESTED_MODULE_ID, NESTED_MODULE_VERSION)),
    )

    private fun modMenu(requirements: Map<String, List<String>>): InstalledModIdentity = InstalledModIdentity(
        id = "modmenu",
        version = "11.0.0",
        requires = requirements,
        fileName = MOD_MENU_FILE,
    )

    // ---- 事实层：内嵌模块与顶层 provides 都必须被读出来，且不产生凭空的版本 ----

    @Test
    fun aNestedModuleIsReadWithItsOwnIdAndVersion() {
        val api = readFabricModFacts(fabricApiJar(tempModsDir()))

        assertEquals(FABRIC_API_ID, api.id)
        assertEquals(FABRIC_API_VERSION, api.version)
        assertTrue(
            "内嵌模块自己的 id 必须被读出来，实际是 ${api.providedModIds}",
            api.providedModIds.contains(NESTED_MODULE_ID)
        )
        //内嵌模块自己的版本是唯一能用于版本判定的来源
        assertEquals(NESTED_MODULE_VERSION, api.providedModVersions[NESTED_MODULE_ID])
        assertTrue(
            "顶层 provides 的别名必须被读出来，实际是 ${api.providedModIds}",
            api.providedModIds.contains("fabric")
        )
        assertNull("别名不得借用外层文件的版本", api.providedModVersions["fabric"])
    }

    // ---- 整组校验：提供者存在就必须接受，且内嵌模块自己的版本仍然要参与判定 ----

    @Test
    fun aDependencyProvidedByAnInstalledModuleIsAccepted() {
        val api = InstalledModIdentity(
            id = FABRIC_API_ID,
            version = FABRIC_API_VERSION,
            providedModIds = listOf("fabric", NESTED_MODULE_ID),
            providedModVersions = mapOf(NESTED_MODULE_ID to NESTED_MODULE_VERSION),
            fileName = FABRIC_API_FILE,
        )

        assertNull(
            "内嵌模块 id 已由已安装文件提供，整组校验不该拒绝",
            resultingSetRejection(listOf(modMenu(mapOf(NESTED_MODULE_ID to listOf("*"))), api))
        )
        assertNull(
            "内嵌模块自己的版本满足声明时必须接受",
            resultingSetRejection(listOf(modMenu(mapOf(NESTED_MODULE_ID to listOf(">=1.0.0"))), api))
        )
    }

    /** 提供者是暂存替换文件（计划的一侧）时同样算数 */
    @Test
    fun aDependencyProvidedByAStagedCandidateIsAccepted() {
        val staged = fabricApiJar(tempModsDir())

        val identity = readFabricModFacts(staged).asInstalledIdentity()
        assertNotNull("暂存文件自己的身份必须读得出来", identity)
        assertNull(
            "暂存替换文件提供的模块同样算数",
            resultingSetRejection(listOf(modMenu(mapOf(NESTED_MODULE_ID to listOf("*"))), identity!!))
        )
    }

    // ---- 对照：缺提供者照旧拒绝；显式版本不满足照旧拒绝；版本读不出来保持沉默 ----

    @Test
    fun aMissingProviderStillRejectsTheSet() {
        val rejection = resultingSetRejection(listOf(modMenu(mapOf(NESTED_MODULE_ID to listOf("*")))))

        assertNotNull("没有提供者时整组校验必须拒绝", rejection)
        assertTrue("原因里要说出缺失的依赖 id，实际是 $rejection", rejection!!.contains(NESTED_MODULE_ID))
    }

    @Test
    fun anExplicitlyWrongProvidedVersionIsStillRejected() {
        val api = InstalledModIdentity(
            id = FABRIC_API_ID,
            version = FABRIC_API_VERSION,
            providedModIds = listOf(NESTED_MODULE_ID),
            //内嵌模块自己声明了版本：它明确不满足声明时整组必须拒绝
            providedModVersions = mapOf(NESTED_MODULE_ID to "1.0.10"),
            fileName = FABRIC_API_FILE,
        )

        val rejection = resultingSetRejection(
            listOf(modMenu(mapOf(NESTED_MODULE_ID to listOf(">=2.0.0"))), api)
        )
        assertNotNull("额外提供的 id 带了明确不满足的版本时必须拒绝", rejection)
        assertTrue("原因里要说出这个 id，实际是 $rejection", rejection!!.contains(NESTED_MODULE_ID))
    }

    @Test
    fun aProvidedAliasWithoutAVersionStaysSilent() {
        val api = InstalledModIdentity(
            id = FABRIC_API_ID,
            version = FABRIC_API_VERSION,
            providedModIds = listOf("fabric"),
            fileName = FABRIC_API_FILE,
        )

        //外层文件的版本（0.141.6+1.21.11）不满足这条声明；只有别名没有版本时保持沉默才不会误报，
        //借用外层版本反而会凭空造出一个「不满足」的结论。
        assertNull(
            "别名的版本读不出来时必须保持沉默，不得借用外层文件的版本",
            resultingSetRejection(listOf(modMenu(mapOf("fabric" to listOf(">=2.0.0"))), api))
        )
    }
}

/** 与真实测试实例一致的目标环境 */
private const val INSTANCE_MC = "1.21.11"

private const val FABRIC_API_FILE = "fabric-api-0.141.6+1.21.11.jar"
private const val FABRIC_API_ID = "fabric-api"
private const val FABRIC_API_VERSION = "0.141.6+1.21.11"

private const val NESTED_MODULE_ENTRY = "META-INF/jars/fabric-resource-loader-v1-1.0.10.jar"
private const val NESTED_MODULE_ID = "fabric-resource-loader-v1"
private const val NESTED_MODULE_VERSION = "1.0.10+78c8b4663e"

private const val MOD_MENU_FILE = "modmenu-11.0.0.jar"
