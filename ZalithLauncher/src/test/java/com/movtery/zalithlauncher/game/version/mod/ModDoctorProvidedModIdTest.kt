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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 顶层 `provides` 声明的 id 必须算作「已装」，与真正的 Fabric Loader 一致。
 *
 * 真实用例（4-mod 认证矩阵 1.19.2|fabric 的 cell）：
 * - `fabric-api-0.76.0+1.19.2.jar`：id = `fabric-api`，provides = ["fabric"]
 * - `sodium-extra-0.4.16+mc1.19.2.jar`：要求依赖 id = `fabric`
 * - 真正的加载器能解析这组依赖，但 Mod Doctor 报 MISSING_REQUIRED_DEPENDENCY 并挡住启动
 *
 * 这些用例走真实路径：AllModReader 读目录 → toDoctorMetadata 读 fabric.mod.json → validateModDoctorMetadata。
 */
class ModDoctorProvidedModIdTest {
    private fun writeModJar(dir: File, fileName: String, metadata: String): File {
        val file = File(dir, fileName)
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("fabric.mod.json"))
            zip.write(metadata.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return file
    }

    private fun tempModsDir(): File = Files.createTempDirectory("moddoctor-provides").toFile()

    /** 真实的读取路径：目录 → LocalMod → ModDoctorMetadata。 */
    private fun metadataOf(dir: File): List<ModDoctorMetadata> = runBlocking {
        AllModReader(dir).readAllLocals().map { ModDoctor().toDoctorMetadata(it) }
    }

    private fun fabricApi(provides: String?): String {
        val base = "{\"schemaVersion\":1,\"id\":\"fabric-api\",\"version\":\"0.76.0+1.19.2\"," +
                "\"name\":\"Fabric API\",\"depends\":{\"minecraft\":\"1.19.2\"}"
        return if (provides == null) "$base}" else "$base,\"provides\":$provides}"
    }

    private val sodiumExtra = "{\"schemaVersion\":1,\"id\":\"sodium-extra\"," +
            "\"version\":\"0.4.16+mc1.19.2\",\"name\":\"Sodium Extra\"," +
            "\"depends\":{\"fabric\":\"*\",\"minecraft\":\"1.19.2\"}}"

    private fun missingDependencies(report: ModDoctorReport): List<String> = report.problems
        .filter { it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY }
        .mapNotNull { it.requiredDependencyId }

    /** 与真机一致的一组文件：Fabric API 用 provides 声明旧 id，Sodium Extra 依赖那个旧 id。 */
    @Test
    fun aProvidedIdCountsAsInstalledSoTheDependencyIsNotReportedMissing() {
        val dir = tempModsDir()
        writeModJar(dir, "fabric-api-0.76.0+1.19.2.jar", fabricApi("[\"fabric\"]"))
        writeModJar(dir, "sodium-extra-0.4.16+mc1.19.2.jar", sodiumExtra)

        val metadata = metadataOf(dir)
        val api = metadata.first { it.fileName == "fabric-api-0.76.0+1.19.2.jar" }
        val extra = metadata.first { it.fileName == "sodium-extra-0.4.16+mc1.19.2.jar" }
        //解析层先要读出来：id 自己 + provides 声明的 id
        assertEquals("fabric-api", api.modId)
        assertTrue("provides 里的 id 必须被读出来，实际是 ${api.providedModIds}", api.providedModIds.contains("fabric"))
        assertEquals("sodium-extra", extra.modId)

        val report = validateModDoctorMetadata(metadata, "1.19.2", ModLoader.FABRIC)
        assertTrue(
            "依赖 id 已由 provides 满足，不该再报缺失：${report.problems.map { it.requiredDependencyId }}",
            missingDependencies(report).isEmpty()
        )
    }

    /** 对照：没有 provides 时，缺失依赖必须照旧被发现。 */
    @Test
    fun withoutProvidesTheMissingDependencyIsStillReported() {
        val dir = tempModsDir()
        writeModJar(dir, "fabric-api-0.76.0+1.19.2.jar", fabricApi(null))
        writeModJar(dir, "sodium-extra-0.4.16+mc1.19.2.jar", sodiumExtra)

        val report = validateModDoctorMetadata(metadataOf(dir), "1.19.2", ModLoader.FABRIC)
        assertEquals(listOf("fabric"), missingDependencies(report))
    }

    /** 对照：provides 了别的 id，不能顺带满足 `fabric`。 */
    @Test
    fun anUnrelatedProvidedIdDoesNotSatisfyTheDependency() {
        val dir = tempModsDir()
        writeModJar(dir, "fabric-api-0.76.0+1.19.2.jar", fabricApi("[\"something-else\"]"))
        writeModJar(dir, "sodium-extra-0.4.16+mc1.19.2.jar", sodiumExtra)

        val report = validateModDoctorMetadata(metadataOf(dir), "1.19.2", ModLoader.FABRIC)
        assertEquals(listOf("fabric"), missingDependencies(report))
    }
}
