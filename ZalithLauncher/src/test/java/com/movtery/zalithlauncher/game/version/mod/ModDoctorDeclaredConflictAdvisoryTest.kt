/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Fabric 的旧键 `conflicts` 是「已声明的冲突」：加载器把它报成启动警告，两个模组照常加载。
 * 因此它必须被读出来、被用户看见，但绝不能阻塞启动，也绝不能进入修复流程。
 *
 * 现场证据（crash-lab，只读）：chatpatches 8.0-alpha.11 声明 `conflicts: { "morechathistory": "*" }`，
 * Fabric Loader 0.19.5 打出 “While this won't prevent you from starting the game…” 的 WARN，
 * 两个模组都加载成功。它不是“加载器忽略了 conflicts”，也不是已证明的崩溃对。
 *
 * 判定与 `breaks` 共用同一套：同一个谓词求值器、同一条「目标缺失或版本读不出来就沉默」的规则，
 * 区别只在落点：`breaks` 进 problems 并阻塞启动，`conflicts` 只进 advisories。
 */
class ModDoctorDeclaredConflictAdvisoryTest {
    private fun mod(
        fileName: String,
        modId: String,
        name: String,
        version: String? = null,
        conflicts: Map<String, List<String>> = emptyMap(),
        breaks: Map<String, List<String>> = emptyMap(),
    ) = ModDoctorMetadata(
        fileName = fileName,
        modId = modId,
        name = name,
        loader = ModLoader.FABRIC,
        version = version,
        breakPredicates = breaks,
        conflictPredicates = conflicts,
    )

    private fun report(mods: List<ModDoctorMetadata>) = validateModDoctorMetadata(
        mods = mods,
        minecraftVersion = INSTANCE_MC,
        loader = ModLoader.FABRIC,
    )

    /** 现场那一对：源声明 `conflicts`，目标是已启用且版本读得出来的真实模组 */
    private fun crashLabPair(predicates: List<String> = listOf("*")) = report(
        listOf(
            mod(
                CHAT_PATCHES_FILE,
                CHAT_PATCHES_ID,
                "Chat Patches",
                CHAT_PATCHES_VERSION,
                conflicts = mapOf(MORE_CHAT_HISTORY_ID to predicates),
            ),
            mod(MORE_CHAT_HISTORY_FILE, MORE_CHAT_HISTORY_ID, "More Chat History", MORE_CHAT_HISTORY_VERSION),
        )
    )

    private fun tempModsDir(): File = Files.createTempDirectory("moddoctor-declared-conflict").toFile()

    /** 一个只带 `fabric.mod.json` 的模组文件，内容原样进入真正的读取路径 */
    private fun writeModJar(dir: File, fileName: String, json: String): File {
        val file = File(dir, fileName)
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("fabric.mod.json"))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return file
    }

    /** 真实入口：AllModReader → ModDoctor.toDoctorMetadata → 统一校验 */
    private fun readReport(modsDir: File): ModDoctorReport {
        val doctor = ModDoctor()
        val metadata = runBlocking {
            AllModReader(modsDir).readAllLocals().map { local -> doctor.toDoctorMetadata(local, ModLoader.FABRIC) }
        }
        return report(metadata)
    }

    // ---- 事实层：读得出来、只落在 advisories、判定与 breaks 共用 ----

    @Test
    fun declaredConflictIsReportedAsAnAdvisoryNotAProblem() {
        val detected = crashLabPair()

        assertTrue("声明的冲突不得进入 problems，实际是 ${detected.problems}", detected.problems.isEmpty())
        val advisory = detected.advisories.single()
        assertEquals(ModDoctorProblemType.DECLARED_CONFLICT, advisory.type)
        assertEquals(CHAT_PATCHES_ID, advisory.modId)
        assertEquals("$CHAT_PATCHES_FILE, $MORE_CHAT_HISTORY_FILE", advisory.fileName)
        assertTrue(advisory.reason.contains("Chat Patches $CHAT_PATCHES_VERSION"))
        assertTrue(advisory.reason.contains("More Chat History $MORE_CHAT_HISTORY_VERSION"))
        //加载器证明过这不是崩溃对：提示里不得出现 `breaks` 那条崩溃式措辞
        assertFalse(advisory.reason.contains("cannot be used with"))
        //这一对从未被证明会崩溃：任何崩溃式措辞都不许出现在提示里
        assertFalse(advisory.reason.lowercase().contains("crash"))
    }

    @Test
    fun realChatPatchesMetadataIsReadFromAJarIntoTheAdvisory() {
        val modsDir = tempModsDir()
        try {
            writeModJar(modsDir, CHAT_PATCHES_FILE, CHAT_PATCHES_JSON)
            writeModJar(modsDir, MORE_CHAT_HISTORY_FILE, MORE_CHAT_HISTORY_JSON)

            val detected = readReport(modsDir)

            assertTrue("夹具不该有真问题，实际是 ${detected.problems}", detected.problems.isEmpty())
            val advisory = detected.advisories.single()
            assertEquals(ModDoctorProblemType.DECLARED_CONFLICT, advisory.type)
            assertEquals(CHAT_PATCHES_ID, advisory.modId)
            assertTrue(advisory.reason.contains("Chat Patches $CHAT_PATCHES_VERSION"))
            assertTrue(advisory.reason.contains("More Chat History $MORE_CHAT_HISTORY_VERSION"))
            assertEquals(LaunchGateDecision.LAUNCH, decideLaunchGate(detected))
        } finally {
            modsDir.deleteRecursively()
        }
    }

    // ---- 语义分离：提示不阻塞、不进修复、不吃掉真问题 ----

    @Test
    fun anAdvisoryOnlyReportStillLaunches() {
        val detected = crashLabPair()

        //先证明报告里确实带着那条提示：否则下面的门判定只是在一个空报告上成立
        assertEquals(ModDoctorProblemType.DECLARED_CONFLICT, detected.advisories.single().type)

        assertTrue("提示不是问题", detected.isClean)
        assertFalse("提示不携带任何修复字段", detected.hasFixableProblems)
        assertEquals(LaunchGateDecision.LAUNCH, decideLaunchGate(detected))
    }

    @Test
    fun anAdvisoryDoesNotReachTheRepairPipeline() {
        val detected = crashLabPair()

        //前提：报告里确实有这条提示；否则本用例与「功能整个没做」无法区分
        assertEquals(ModDoctorProblemType.DECLARED_CONFLICT, detected.advisories.single().type)

        //结构性保证：修复管线只读 `problems`，而提示从不进入 problems，所以下面的守卫不可能被触发。
        val guarded = runBlocking {
            detected.withFixCandidates(
                minecraftVersion = INSTANCE_MC,
                loader = ModLoader.FABRIC,
                platform = Platform.MODRINTH,
                directProject = { _, _ -> error("declared conflict must not resolve a dependency project") },
                searchedProject = { _, _, _ -> error("declared conflict must not search a provider") },
                projectVersions = { _, _ -> error("declared conflict must not load provider versions") },
                installedMod = { _ -> error("declared conflict must not fingerprint an installed file") },
            )
        }

        assertSame("提示必须原样返回，不得被解析成任何修复候选", detected, guarded)
    }

    // ---- 对照：沉默规则与丢弃规则与 breaks 一字不差 ----

    @Test
    fun anAbsentConflictTargetProducesNoAdvisory() {
        val detected = report(
            listOf(
                mod(
                    CHAT_PATCHES_FILE,
                    CHAT_PATCHES_ID,
                    "Chat Patches",
                    CHAT_PATCHES_VERSION,
                    conflicts = mapOf(MORE_CHAT_HISTORY_ID to listOf("*")),
                )
            )
        )

        //正的对照：同一份声明，目标一旦在已启用模组里就必须提示一条，否则「沉默」证明不了任何事
        val targetPresent = crashLabPair()
        assertEquals(1, targetPresent.advisories.size)
        assertEquals(ModDoctorProblemType.DECLARED_CONFLICT, targetPresent.advisories.single().type)

        assertTrue("目标不在已启用模组里就保持沉默", detected.advisories.isEmpty())
    }

    @Test
    fun aNonMatchingConflictPredicateProducesNoAdvisory() {
        assertTrue("1.3.1 不满足 >=2.0 时不得提示", crashLabPair(listOf(">=2.0")).advisories.isEmpty())
        assertEquals("1.3.1 命中 <2.0 时必须提示", 1, crashLabPair(listOf("<2.0")).advisories.size)
    }

    @Test
    fun anObjectFormConflictEntryIsNotInvented() {
        val modsDir = tempModsDir()
        val arrayFormDir = tempModsDir()
        try {
            //正的对照：同一份夹具换成数组形式（`["*"]`）就必须读出一个提示，
            //用来证明下面的「没有提示」是刻意丢弃，而不是这份夹具根本读不出来。
            writeModJar(arrayFormDir, CHAT_PATCHES_FILE, ARRAY_FORM_CONFLICT_JSON)
            writeModJar(arrayFormDir, MORE_CHAT_HISTORY_FILE, MORE_CHAT_HISTORY_JSON)
            val arrayForm = readReport(arrayFormDir)
            assertEquals(1, arrayForm.advisories.size)
            assertEquals(ModDoctorProblemType.DECLARED_CONFLICT, arrayForm.advisories.single().type)

            writeModJar(modsDir, CHAT_PATCHES_FILE, OBJECT_FORM_CONFLICT_JSON)
            writeModJar(modsDir, MORE_CHAT_HISTORY_FILE, MORE_CHAT_HISTORY_JSON)

            val detected = readReport(modsDir)

            assertTrue(
                "对象形式的条目必须与 `breaks` 一样被丢弃，实际是 ${detected.advisories}",
                detected.advisories.isEmpty()
            )
        } finally {
            modsDir.deleteRecursively()
            arrayFormDir.deleteRecursively()
        }
    }

    @Test
    fun breaksAndConflictsOnTheSameModStaySeparate() {
        val detected = report(
            listOf(
                mod(
                    "sodium.jar",
                    "sodium",
                    "Sodium",
                    "0.8.14",
                    conflicts = mapOf("iris" to listOf("*")),
                    breaks = mapOf("iris" to listOf("<=1.10.7")),
                ),
                mod("iris.jar", "iris", "Iris", "1.10.7"),
            )
        )

        assertEquals(1, detected.problems.size)
        assertEquals(ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR, detected.problems.single().type)
        assertEquals(1, detected.advisories.size)
        assertEquals(ModDoctorProblemType.DECLARED_CONFLICT, detected.advisories.single().type)
        assertEquals("breaks 照旧阻塞", LaunchGateDecision.BLOCK, decideLaunchGate(detected))
    }

    @Test
    fun packSafeModeKeepsTheAdvisoryVisible() {
        val detected = crashLabPair()
        val modsDir = Files.createTempDirectory("moddoctor-declared-conflict-pack").toFile()
        try {
            val guarded = detected.applyPackSafeMode(
                ModpackPolicy(
                    ModpackManifest(
                        name = "QA",
                        mods = listOf(
                            PackOwnedMod(name = CHAT_PATCHES_FILE, requiredCore = true),
                            PackOwnedMod(name = MORE_CHAT_HISTORY_FILE, requiredCore = true),
                        ),
                        modsComplete = true,
                    )
                ),
                modsDir,
            )

            assertTrue("整合包安全模式不得吞掉这条提示", guarded.problems.isEmpty())
            assertEquals(1, guarded.advisories.size)
            assertEquals(LaunchGateDecision.LAUNCH, decideLaunchGate(guarded))
        } finally {
            modsDir.deleteRecursively()
        }
    }

    /**
     * 真实字节的对照：只在环境变量指向 crash-lab 的 mods 缓存目录时运行。
     * 只断言「读得出来、且不是 breaks 阻塞对」：真实文件缺 fabric-api 与 yacl，真问题与本语义无关。
     */
    @Test
    fun theRealCrashLabPairProducesTheAdvisory() {
        val sourceDir = System.getenv(REAL_PAIR_DIR_ENV)
        if (sourceDir.isNullOrBlank()) {
            println("ModDoctorDeclaredConflictAdvisoryTest: $REAL_PAIR_DIR_ENV not set, nothing to do")
            return
        }
        val chatPatches = File(sourceDir, CHAT_PATCHES_FILE)
        val moreChatHistory = File(sourceDir, MORE_CHAT_HISTORY_FILE)
        require(chatPatches.isFile && moreChatHistory.isFile) {
            "the real pair is missing under $sourceDir"
        }

        val modsDir = tempModsDir()
        try {
            chatPatches.copyTo(File(modsDir, CHAT_PATCHES_FILE))
            moreChatHistory.copyTo(File(modsDir, MORE_CHAT_HISTORY_FILE))

            val detected = readReport(modsDir)

            val advisory = detected.advisories.single()
            assertTrue(advisory.reason.contains("Chat Patches $CHAT_PATCHES_VERSION"))
            assertTrue(advisory.reason.contains("More Chat History $MORE_CHAT_HISTORY_VERSION"))
            assertTrue(
                "真实字节上不得出现阻塞性的不兼容模组对，实际是 ${detected.problems}",
                detected.problems.none { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR }
            )
        } finally {
            modsDir.deleteRecursively()
        }
    }
}

/** 与真实测试实例一致的目标环境 */
private const val INSTANCE_MC = "1.21.11"

private const val CHAT_PATCHES_FILE = "chatpatches-8.0-alpha.11+1.21.11-fabric.jar"
private const val CHAT_PATCHES_ID = "chatpatches"
private const val CHAT_PATCHES_VERSION = "8.0-alpha.11"

private const val MORE_CHAT_HISTORY_FILE = "morechathistory-1.3.1.jar"
private const val MORE_CHAT_HISTORY_ID = "morechathistory"
private const val MORE_CHAT_HISTORY_VERSION = "1.3.1"

/**
 * 真实身份取自 crash-lab 的 jar dump。
 * `depends` 故意省略：真实的 depends 指向 fabric-api 内嵌模块与 yacl，两文件夹具无法满足，
 * 只会多出一条与本次语义无关的缺失依赖问题。
 */
private const val CHAT_PATCHES_JSON =
    "{\"schemaVersion\":1,\"id\":\"chatpatches\",\"version\":\"8.0-alpha.11\"," +
            "\"name\":\"Chat Patches\",\"environment\":\"client\"," +
            "\"conflicts\":{\"morechathistory\":\"*\"}}"

/** 同一个文件，但冲突条目写成对象形式：与 `breaks` 一样不得凭空造出一个版本 */
private const val OBJECT_FORM_CONFLICT_JSON =
    "{\"schemaVersion\":1,\"id\":\"chatpatches\",\"version\":\"8.0-alpha.11\"," +
            "\"name\":\"Chat Patches\",\"environment\":\"client\"," +
            "\"conflicts\":{\"morechathistory\":{\"version\":\"*\"}}}"

/** 同一个文件，冲突条目写成数组形式：与 `breaks` 共用同一套读法，必须读出一个谓词 */
private const val ARRAY_FORM_CONFLICT_JSON =
    "{\"schemaVersion\":1,\"id\":\"chatpatches\",\"version\":\"8.0-alpha.11\"," +
            "\"name\":\"Chat Patches\",\"environment\":\"client\"," +
            "\"conflicts\":{\"morechathistory\":[\"*\"]}}"

private const val MORE_CHAT_HISTORY_JSON =
    "{\"schemaVersion\":1,\"id\":\"morechathistory\",\"version\":\"1.3.1\"," +
            "\"name\":\"More Chat History\",\"environment\":\"client\"," +
            "\"depends\":{\"minecraft\":\">=1.20.5\",\"fabricloader\":\">=0.15.0\"}}"

/** crash-lab 的 mods 缓存目录；未设置时真实字节那条测试直接返回 */
private const val REAL_PAIR_DIR_ENV = "MOD_DOCTOR_REAL_PAIR_DIR"
