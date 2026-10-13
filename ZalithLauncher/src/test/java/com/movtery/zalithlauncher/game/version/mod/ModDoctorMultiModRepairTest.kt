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

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 有界多模组修复：以 INCOMPATIBLE_MOD_PAIR 的既有计划（目标侧优先、源侧回退）为入口，
 * 只沿 `requires` 方向展开一层，把“依赖不再被满足”的已启用模组一并替换。
 *
 * 现场形状来自真实四模组冲突（中性 id）：破坏方声明 breaks 目标，两个依赖方各自要求破坏方的
 * 当前版本，因此单独替换任意一侧都不成立，必须连同依赖方一起替换才能得到自洽的组合。
 *
 * 所有用例都通过生产入口驱动：检测/校验/身份与版本来源/暂存/复制/回滚全部来自生产代码，
 * 测试只注入权威身份与平台版本列表，不复制算法。
 */
class ModDoctorMultiModRepairTest {
    // ---- A. 真实四模组图：三次替换得到合法计划，目标侧保持不动 ----

    @Test
    fun realFourModGraphRepairsByThreeReplacements() = runBlocking {
        val fixture = RepairFixture("four-mod").apply { fourModGraph() }
        try {
            val outcome = fixture.harness().run(breaksFix())

            val repaired = outcome as? ModDoctorRepairOutcome.Repaired
            assertNotNull("expected a three-replacement plan, got $outcome", repaired)
            assertEquals(
                listOf(BREAKER_FILE, DEP_ONE_FILE, DEP_TWO_FILE),
                repaired?.replacedFileNames
            )
            //目标侧（冲突的另一侧）一个字节都没有被替换
            assertFalse(repaired?.replacedFileNames.orEmpty().contains(TARGET_FILE))
            assertTrue(File(fixture.modsDir, TARGET_FILE).exists())
            assertFalse(File(fixture.modsDir, "$TARGET_FILE.disabled").exists())

            //被替换的原文件全部可逆保留，替换文件按平台文件名落位
            listOf(BREAKER_FILE, DEP_ONE_FILE, DEP_TWO_FILE).forEach { name ->
                assertTrue("$name should be preserved as .disabled", File(fixture.modsDir, "$name.disabled").exists())
            }
            assertTrue(File(fixture.modsDir, "candidate-$BREAKER_PROJECT-8.2.0.jar").exists())
            assertTrue(File(fixture.modsDir, "candidate-$DEP_ONE_PROJECT-4.0.0.jar").exists())
            assertTrue(File(fixture.modsDir, "candidate-$DEP_TWO_PROJECT-2.5.0.jar").exists())
        } finally {
            fixture.cleanup()
        }
    }

    // ---- B. 没有安全计划：只报告，mods 目录逐字节不变 ----

    @Test
    fun noSafePlanStaysReportOnly() = runBlocking {
        //破坏方项目只有当前版本（仍然打断目标）：两侧都没有任何安全替换
        val fixture = RepairFixture("no-plan").apply { fourModGraph(withSafeBreakerBuild = false) }
        try {
            val before = snapshot(fixture.modsDir)
            val harness = fixture.harness()

            val outcome = harness.run(breaksFix())

            assertTrue("expected a safe refusal, got $outcome", outcome is ModDoctorRepairOutcome.NoSafePlan)
            assertEquals(before, snapshot(fixture.modsDir))
            assertTrue(disabledFiles(fixture.modsDir).isEmpty())
            //暂存位在修复结束时被清空
            assertTrue(fixture.stagingDir.listFiles().orEmpty().isEmpty())
        } finally {
            fixture.cleanup()
        }
    }

    // ---- C. 目标侧先试：目标有安全候选时不再碰源侧 ----

    @Test
    fun targetSideIsTriedBeforeSourceSide() = runBlocking {
        //目标项目有一个更新的构建：不再被已安装的破坏方声明命中
        val fixture = RepairFixture("target-first").apply {
            installed(TARGET_FILE, TARGET_PROJECT, TARGET_ID, "4.0.0")
            installed(BREAKER_FILE, BREAKER_PROJECT, BREAKER_ID, "9.0.0", breaks = mapOf(TARGET_ID to "<=4.0.0"))
            project(
                TARGET_PROJECT,
                version(TARGET_PROJECT, "4.1.0"),
            )
            candidate(TARGET_PROJECT, "4.1.0", modJson(TARGET_ID, "4.1.0"))
        }
        try {
            val harness = fixture.harness()

            val outcome = harness.run(breaksFix())

            val repaired = outcome as? ModDoctorRepairOutcome.Repaired
            assertNotNull("expected the target side to be repaired, got $outcome", repaired)
            assertEquals(listOf(TARGET_FILE), repaired?.replacedFileNames)
            assertEquals(listOf("target-$TARGET_PROJECT-4.1.0"), harness.stagedCandidates)
            //目标侧已经给出计划：源侧项目的版本列表一次都没有查询
            assertEquals(listOf(TARGET_PROJECT), harness.versionRequests)
        } finally {
            fixture.cleanup()
        }
    }

    // ---- D. 只替换“依赖不再被满足”的模组：依赖仍然成立的模组不动 ----

    @Test
    fun onlyBlockingDependentsAreReplaced() = runBlocking {
        val fixture = RepairFixture("blocking-only").apply {
            fourModGraph()
            //第二个依赖方接受的版本范围包含新的破坏方版本：它不需要被替换
            installed(
                DEP_TWO_FILE, DEP_TWO_PROJECT, DEP_TWO_ID, "2.0.0",
                requires = mapOf(BREAKER_ID to ">=8.0.0"),
            )
        }
        try {
            val harness = fixture.harness()

            val outcome = harness.run(breaksFix())

            val repaired = outcome as? ModDoctorRepairOutcome.Repaired
            assertNotNull("expected a plan, got $outcome", repaired)
            assertEquals(listOf(BREAKER_FILE, DEP_ONE_FILE), repaired?.replacedFileNames)
            //依赖仍然成立的模组与完全无关的模组都没有被碰过
            assertTrue(File(fixture.modsDir, DEP_TWO_FILE).exists())
            assertTrue(File(fixture.modsDir, UNRELATED_FILE).exists())
            assertFalse(harness.stagedCandidates.any { it.contains(DEP_TWO_PROJECT) })
            assertTrue(harness.fingerprintRequests.none { it == DEP_TWO_FILE })
        } finally {
            fixture.cleanup()
        }
    }

    // ---- E. 依赖方的候选顺序：正式版在前、各自保持最新在前，预发布版最后 ----

    @Test
    fun dependentWalkIsStableFirstNewestFirst() = runBlocking {
        val fixture = RepairFixture("stable-first").apply {
            fourModGraph()
            //3.5.0 仍要求旧的破坏方版本；更新的 4.0.0 是预发布版；3.4.0 才是可用的正式版
            project(
                DEP_ONE_PROJECT,
                version(DEP_ONE_PROJECT, "4.0.0", prerelease = true),
                version(DEP_ONE_PROJECT, "3.5.0"),
                version(DEP_ONE_PROJECT, "3.4.0"),
            )
            candidate(DEP_ONE_PROJECT, "4.0.0", modJson(DEP_ONE_ID, "4.0.0", requires = mapOf(BREAKER_ID to ">=8.0.0")))
            candidate(DEP_ONE_PROJECT, "3.5.0", modJson(DEP_ONE_ID, "3.5.0", requires = mapOf(BREAKER_ID to "9.0.0")))
            candidate(DEP_ONE_PROJECT, "3.4.0", modJson(DEP_ONE_ID, "3.4.0", requires = mapOf(BREAKER_ID to ">=8.0.0")))
        }
        try {
            val harness = fixture.harness()

            val outcome = harness.run(breaksFix())

            assertTrue("expected a plan, got $outcome", outcome is ModDoctorRepairOutcome.Repaired)
            assertEquals(
                listOf("dep-one-$DEP_ONE_PROJECT-3.5.0", "dep-one-$DEP_ONE_PROJECT-3.4.0"),
                harness.stagedCandidates.filter { it.startsWith("dep-one-") }
            )
            assertTrue(File(fixture.modsDir, "candidate-$DEP_ONE_PROJECT-3.4.0.jar").exists())
        } finally {
            fixture.cleanup()
        }
    }

    // ---- F. 整组结果校验通过之前，mods 目录一个字节都不许改 ----

    @Test
    fun nothingIsMutatedBeforeTheWholeSetValidates() = runBlocking {
        //候选本身可读、版本也正确，逐候选校验看不出问题；但它声明的依赖在整组结果里没有提供者，
        //只有整组校验能发现。发现之前不得禁用原文件、也不得复制任何替换文件。
        val fixture = RepairFixture("validate-first").apply {
            installed(TARGET_FILE, TARGET_PROJECT, TARGET_ID, "4.0.0")
            installed(BREAKER_FILE, BREAKER_PROJECT, BREAKER_ID, "9.0.0", breaks = mapOf(TARGET_ID to "<=4.0.0"))
            project(TARGET_PROJECT, version(TARGET_PROJECT, "5.0.0"))
            candidate(
                TARGET_PROJECT, "5.0.0",
                modJson(TARGET_ID, "5.0.0", requires = mapOf("test-missing-api" to "1.0.0")),
            )
        }
        try {
            val before = snapshot(fixture.modsDir)
            val harness = fixture.harness()

            val outcome = harness.run(breaksFix())

            assertTrue("expected a safe refusal, got $outcome", outcome is ModDoctorRepairOutcome.NoSafePlan)
            assertEquals("the mods directory must not change at all", before, snapshot(fixture.modsDir))
            assertTrue(disabledFiles(fixture.modsDir).isEmpty())
        } finally {
            fixture.cleanup()
        }
    }

    // ---- G. 执行阶段失败：删除副本、恢复全部原文件 ----

    @Test
    fun applyFailureRollsBackCompletely() = runBlocking {
        val fixture = RepairFixture("rollback").apply {
            installed(TARGET_FILE, TARGET_PROJECT, TARGET_ID, "4.0.0")
            installed(BREAKER_FILE, BREAKER_PROJECT, BREAKER_ID, "9.0.0", breaks = mapOf(TARGET_ID to "<=4.0.0"))
        }
        try {
            val before = snapshot(fixture.modsDir)
            val firstStaged = fixture.writeStaged("first", modJson(BREAKER_ID, "8.2.0", breaks = mapOf(TARGET_ID to "<=3.0.0")))
            val secondStaged = fixture.writeStaged("second", modJson(TARGET_ID, "5.0.0"))
            val plan = ModDoctorRepairPlan(
                listOf(
                    ModDoctorPlannedReplacement(
                        fileName = BREAKER_FILE,
                        target = version(BREAKER_PROJECT, "8.2.0"),
                        stagedFile = firstStaged,
                        facts = readFabricModFacts(firstStaged),
                    ),
                    ModDoctorPlannedReplacement(
                        fileName = TARGET_FILE,
                        //平台给出的 sha1 与暂存内容不一致：第二个副本必须让整次执行回滚
                        target = version(TARGET_PROJECT, "5.0.0", sha1 = "0000000000000000000000000000000000000000"),
                        stagedFile = secondStaged,
                        facts = readFabricModFacts(secondStaged),
                    ),
                )
            )

            val outcome = fixture.harness().apply(plan)

            assertTrue("expected a failed execution, got $outcome", outcome is ModDoctorRepairOutcome.Failed)
            assertEquals("every original file must be restored byte for byte", before, snapshot(fixture.modsDir))
            assertTrue(disabledFiles(fixture.modsDir).isEmpty())
            assertFalse(File(fixture.modsDir, "candidate-$BREAKER_PROJECT-8.2.0.jar").exists())
            assertFalse(File(fixture.modsDir, "candidate-$TARGET_PROJECT-5.0.0.jar").exists())
        } finally {
            fixture.cleanup()
        }
    }

    // ---- G2. Phase B 文件安全：目标名冲突 / .disabled 冲突 / 冲突后中途失败 ----

    private fun singleReplacementPlan(fixture: RepairFixture, project: String, ver: String, sha1: String? = null): ModDoctorRepairPlan {
        val staged = fixture.writeStaged("staged-$project-$ver", modJson(TARGET_ID, ver))
        return ModDoctorRepairPlan(
            listOf(
                ModDoctorPlannedReplacement(
                    fileName = TARGET_FILE,
                    target = version(project, ver, sha1 = sha1 ?: sha1Of(staged)),
                    stagedFile = staged,
                    facts = readFabricModFacts(staged),
                )
            )
        )
    }

    @Test
    fun existingFileAtTheTargetNameIsPreservedNotDeleted() = runBlocking {
        val fixture = RepairFixture("target-collision").apply {
            installed(TARGET_FILE, TARGET_PROJECT, TARGET_ID, "4.0.0")
        }
        try {
            val collisionName = "candidate-$TARGET_PROJECT-5.0.0.jar"
            val collision = File(fixture.modsDir, collisionName).apply { writeText("PRE-EXISTING USER FILE") }
            val collisionSha = sha1Of(collision)

            val outcome = fixture.harness().apply(singleReplacementPlan(fixture, TARGET_PROJECT, "5.0.0"))

            assertTrue("expected success, got $outcome", outcome is ModDoctorRepairOutcome.Repaired)
            //新文件在位；原来占用这个名字的文件没有丢：以 .disabled 保留，字节完全一致
            assertTrue(File(fixture.modsDir, collisionName).isFile)
            assertTrue(sha1Of(File(fixture.modsDir, collisionName)) != collisionSha)
            val kept = File(fixture.modsDir, "$collisionName.disabled")
            assertTrue("the colliding file must be kept", kept.isFile)
            assertEquals(collisionSha, sha1Of(kept))
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun existingDisabledFileBlocksTheRepairAndNothingChanges() = runBlocking {
        val fixture = RepairFixture("disabled-collision").apply {
            installed(TARGET_FILE, TARGET_PROJECT, TARGET_ID, "4.0.0")
        }
        try {
            //原文件的 .disabled 名字已被占用：禁用会覆盖它，必须拒绝
            File(fixture.modsDir, "$TARGET_FILE.disabled").writeText("EARLIER DISABLED COPY")
            val before = snapshot(fixture.modsDir)

            val outcome = fixture.harness().apply(singleReplacementPlan(fixture, TARGET_PROJECT, "5.0.0"))

            assertTrue("expected a safe failure, got $outcome", outcome is ModDoctorRepairOutcome.Failed)
            assertEquals("no file may change", before, snapshot(fixture.modsDir))
            assertFalse(File(fixture.modsDir, "candidate-$TARGET_PROJECT-5.0.0.jar").exists())
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun failureAfterACollisionRestoresTheCollidingFileUnderItsOwnName() = runBlocking {
        val fixture = RepairFixture("collision-then-fail").apply {
            installed(TARGET_FILE, TARGET_PROJECT, TARGET_ID, "4.0.0")
        }
        try {
            File(fixture.modsDir, "candidate-$TARGET_PROJECT-5.0.0.jar").writeText("PRE-EXISTING USER FILE")
            val before = snapshot(fixture.modsDir)

            //sha1 与暂存内容不一致：复制之后的核对失败，整次执行必须回滚（包含冲突文件被保留的那一步）
            val outcome = fixture.harness().apply(
                singleReplacementPlan(fixture, TARGET_PROJECT, "5.0.0", sha1 = "0000000000000000000000000000000000000000")
            )

            assertTrue("expected a failed execution, got $outcome", outcome is ModDoctorRepairOutcome.Failed)
            assertEquals("every file, including the colliding one, must be back byte for byte", before, snapshot(fixture.modsDir))
            assertTrue(disabledFiles(fixture.modsDir).isEmpty())
        } finally {
            fixture.cleanup()
        }
    }

    // ---- H. 替换必须提供被替换的 id ----

    @Test
    fun replacementMustProvideTheReplacedId() = runBlocking {
        val fixture = RepairFixture("wrong-id").apply {
            installed(TARGET_FILE, TARGET_PROJECT, TARGET_ID, "4.0.0")
            installed(BREAKER_FILE, BREAKER_PROJECT, BREAKER_ID, "9.0.0", breaks = mapOf(TARGET_ID to "<=4.0.0"))
            project(TARGET_PROJECT, version(TARGET_PROJECT, "5.0.0"))
            //文件来自目标项目，但自己声明的 id 是另一个模组：不能替代被替换的文件
            candidate(TARGET_PROJECT, "5.0.0", modJson("some-other-mod", "5.0.0"))
        }
        try {
            val before = snapshot(fixture.modsDir)

            val outcome = fixture.harness().run(breaksFix())

            assertTrue("expected a safe refusal, got $outcome", outcome is ModDoctorRepairOutcome.NoSafePlan)
            assertEquals(before, snapshot(fixture.modsDir))
            assertTrue(disabledFiles(fixture.modsDir).isEmpty())
        } finally {
            fixture.cleanup()
        }
    }

    // ---- I. 整合包文件永远不进入计划 ----

    @Test
    fun packOwnedFilesAreNeverReplaced() = runBlocking {
        val plain = RepairFixture("pack-control").apply { fourModGraph() }
        val packOwned = RepairFixture("pack-owned").apply {
            fourModGraph()
            //一个必须连带替换的依赖方属于整合包：计划里不允许出现它
            this.packOwned += DEP_ONE_FILE
        }
        try {
            //对照：同一个图在没有整合包归属时是能修好的
            assertTrue(plain.harness().run(breaksFix()) is ModDoctorRepairOutcome.Repaired)

            val before = snapshot(packOwned.modsDir)
            val outcome = packOwned.harness().run(breaksFix())

            assertTrue("a pack-owned file must never enter a plan, got $outcome", outcome is ModDoctorRepairOutcome.NoSafePlan)
            assertEquals(before, snapshot(packOwned.modsDir))
            assertTrue(disabledFiles(packOwned.modsDir).isEmpty())
        } finally {
            plain.cleanup()
            packOwned.cleanup()
        }
    }

    // ---- J. 界限：越界只做安全拒绝，不抛异常 ----

    @Test
    fun boundsAreEnforced() = runBlocking {
        //(1) 超过 MAX_REPAIR_DEPENDENTS 个连带依赖方：不展开，安全拒绝
        val tooManyDependents = RepairFixture("bound-dependents").apply {
            installed(TARGET_FILE, TARGET_PROJECT, TARGET_ID, "4.0.0")
            installed(BREAKER_FILE, BREAKER_PROJECT, BREAKER_ID, "9.0.0", breaks = mapOf(TARGET_ID to "<=4.0.0"))
            for (index in 1..(MAX_REPAIR_DEPENDENTS + 1)) {
                installed(
                    "test-dep-$index-1.0.0.jar", "proj-dep-$index", "test-dep-$index", "1.0.0",
                    requires = mapOf(BREAKER_ID to "9.0.0"),
                )
            }
            project(BREAKER_PROJECT, version(BREAKER_PROJECT, "8.2.0"))
            candidate(BREAKER_PROJECT, "8.2.0", modJson(BREAKER_ID, "8.2.0"))
        }
        try {
            val before = snapshot(tooManyDependents.modsDir)
            val outcome = tooManyDependents.harness().run(breaksFix())

            assertTrue("more than $MAX_REPAIR_DEPENDENTS dependents must refuse safely", outcome is ModDoctorRepairOutcome.NoSafePlan)
            assertEquals(before, snapshot(tooManyDependents.modsDir))
            assertTrue(disabledFiles(tooManyDependents.modsDir).isEmpty())
        } finally {
            tooManyDependents.cleanup()
        }

        //(2) 根候选走查受 MAX_REPAIR_ROOT_CANDIDATES 限制，暂存下载不超过 MAX_REPAIR_STAGED_DOWNLOADS
        val manyCandidates = RepairFixture("bound-root").apply {
            installed(TARGET_FILE, TARGET_PROJECT, TARGET_ID, "4.0.0")
            installed(BREAKER_FILE, BREAKER_PROJECT, BREAKER_ID, "9.0.0", breaks = mapOf(TARGET_ID to "<=4.0.0"))
            val candidates = (1..(MAX_REPAIR_ROOT_CANDIDATES + 2)).map { index ->
                version(BREAKER_PROJECT, "8.$index")
            }
            project(BREAKER_PROJECT, *candidates.toTypedArray())
            candidates.forEach { candidate ->
                //每个候选仍然打断目标：整组校验永远不成立，走查只能靠界限停下
                candidate(BREAKER_PROJECT, candidate.platformVersion(), modJson(BREAKER_ID, candidate.platformVersion(), breaks = mapOf(TARGET_ID to "<=9.0.0")))
            }
        }
        try {
            val harness = manyCandidates.harness()
            val outcome = harness.run(breaksFix())

            assertTrue("expected a safe refusal, got $outcome", outcome is ModDoctorRepairOutcome.NoSafePlan)
            assertEquals(MAX_REPAIR_ROOT_CANDIDATES, harness.stagedCandidates.size)
            assertTrue(harness.stagedCandidates.size <= MAX_REPAIR_STAGED_DOWNLOADS)

            //收紧暂存下载界限：命中界限只是安全拒绝，不抛异常
            manyCandidates.bounds = ModDoctorRepairBounds(stagedDownloads = 3)
            val tightHarness = manyCandidates.harness()
            val tightOutcome = tightHarness.run(breaksFix())

            assertTrue("expected a safe refusal, got $tightOutcome", tightOutcome is ModDoctorRepairOutcome.NoSafePlan)
            assertEquals(3, tightHarness.stagedCandidates.size)
        } finally {
            manyCandidates.cleanup()
        }

        //(3) 单个连带模组的候选尝试次数受 MAX_REPAIR_DEPENDENT_ATTEMPTS 限制
        val dependentWalk = RepairFixture("bound-attempts").apply {
            installed(TARGET_FILE, TARGET_PROJECT, TARGET_ID, "4.0.0")
            installed(BREAKER_FILE, BREAKER_PROJECT, BREAKER_ID, "9.0.0", breaks = mapOf(TARGET_ID to "<=4.0.0"))
            installed(DEP_ONE_FILE, DEP_ONE_PROJECT, DEP_ONE_ID, "3.0.0", requires = mapOf(BREAKER_ID to "9.0.0"))
            project(BREAKER_PROJECT, version(BREAKER_PROJECT, "8.2.0"))
            candidate(BREAKER_PROJECT, "8.2.0", modJson(BREAKER_ID, "8.2.0"))
            val candidates = (1..(MAX_REPAIR_DEPENDENT_ATTEMPTS + 2)).map { index ->
                version(DEP_ONE_PROJECT, "1.$index")
            }
            project(DEP_ONE_PROJECT, *candidates.toTypedArray())
            candidates.forEach { candidate ->
                //每一个候选都还要求旧的破坏方版本：界限内全部被拒后就只能安全拒绝
                candidate(DEP_ONE_PROJECT, candidate.platformVersion(), modJson(DEP_ONE_ID, candidate.platformVersion(), requires = mapOf(BREAKER_ID to "9.0.0")))
            }
        }
        try {
            val harness = dependentWalk.harness()
            val outcome = harness.run(breaksFix())

            assertTrue("expected a safe refusal, got $outcome", outcome is ModDoctorRepairOutcome.NoSafePlan)
            assertEquals(
                MAX_REPAIR_DEPENDENT_ATTEMPTS,
                harness.stagedCandidates.count { it.startsWith("dep-one-") }
            )
        } finally {
            dependentWalk.cleanup()
        }
    }

    // ---- K. 内嵌模块与 provides 别名提供的 id 算「已装」：整组校验不得把它们判成缺失 ----

    @Test
    fun aModuleProvidedByAnInstalledFileKeepsTheWholeSetConsistent() = runBlocking {
        //真实形状：Fabric API 的模块以内嵌 jar 的形式随外层文件一起提供，依赖它的模组只声明那个模块 id。
        //加载器能解析这组依赖；看不见内嵌模块 id 的整组校验会误报缺失、放弃整个计划。
        val fixture = RepairFixture("provided-module").apply {
            fourModGraph()
            installed(
                FABRIC_API_FILE, "proj-fabric-api", FABRIC_API_ID, "0.141.6+1.21.11",
                provides = listOf("fabric"),
                nestedModules = mapOf(NESTED_MODULE_ENTRY to modJson(NESTED_MODULE_ID, NESTED_MODULE_VERSION)),
            )
            installed(
                MOD_MENU_FILE, "proj-mod-menu", MOD_MENU_ID, "11.0.0",
                requires = mapOf(NESTED_MODULE_ID to "*"),
            )
        }
        try {
            val harness = fixture.harness()

            val outcome = harness.run(breaksFix())

            val repaired = outcome as? ModDoctorRepairOutcome.Repaired
            assertNotNull("内嵌模块 id 已由已安装文件提供：整组校验不得因此拒绝，实际是 $outcome", repaired)
            //计划与没有这两个文件时完全一致：它们只影响整组校验，不参与冲突替换
            assertEquals(listOf(BREAKER_FILE, DEP_ONE_FILE, DEP_TWO_FILE), repaired?.replacedFileNames)
            assertTrue(File(fixture.modsDir, FABRIC_API_FILE).exists())
            assertFalse(File(fixture.modsDir, "$FABRIC_API_FILE.disabled").exists())
        } finally {
            fixture.cleanup()
        }
    }

    // ---- L. 真实现场：带构建元数据的依赖谓词必须参与「卡住」判定，三个文件一起进事务 ----

    @Test
    fun buildMetadataRequirementsExpandEveryBlockedDependentIntoThePlan() = runBlocking {
        val fixture = RepairFixture("build-metadata").apply { sodiumLikeGraph() }
        try {
            val before = snapshot(fixture.modsDir)
            val harness = fixture.harness()

            val outcome = harness.run(sodiumLikeFix())

            val repaired = outcome as? ModDoctorRepairOutcome.Repaired
            assertNotNull("expected a three-file plan, got $outcome", repaired)
            //被替换的破坏方 + 两个「依赖不再被满足」的已启用模组；目标侧保持不动
            assertEquals(
                listOf(SODIUM_LIKE_FILE, SODIUM_LIKE_EXTRA_FILE, SODIUM_LIKE_REESES_FILE),
                repaired?.replacedFileNames,
            )
            //事务真的动了这三个文件：原文件可逆保留，替换文件按平台文件名落位
            listOf(SODIUM_LIKE_FILE, SODIUM_LIKE_EXTRA_FILE, SODIUM_LIKE_REESES_FILE).forEach { name ->
                assertTrue("$name should be preserved as .disabled", File(fixture.modsDir, "$name.disabled").exists())
                assertFalse("$name must not stay enabled", File(fixture.modsDir, name).exists())
            }
            assertTrue(File(fixture.modsDir, "candidate-$BREAKER_PROJECT-0.8.12+mc1.21.11.jar").exists())
            assertTrue(File(fixture.modsDir, "candidate-$DEP_ONE_PROJECT-0.9.2+mc1.21.11.jar").exists())
            assertTrue(File(fixture.modsDir, "candidate-$DEP_TWO_PROJECT-2.2.3+mc1.21.11.jar").exists())
            //两个依赖方被真正走查过：更新的构建因为谓词不成立而被跳过
            assertEquals(
                listOf(
                    "dep-one-$DEP_ONE_PROJECT-0.9.4+mc1.21.11",
                    "dep-one-$DEP_ONE_PROJECT-0.9.3+mc1.21.11",
                    "dep-one-$DEP_ONE_PROJECT-0.9.2+mc1.21.11",
                    "dep-two-$DEP_TWO_PROJECT-2.2.4+mc1.21.11",
                    "dep-two-$DEP_TWO_PROJECT-2.2.3+mc1.21.11",
                ),
                harness.stagedCandidates.filter { it.startsWith("dep-") },
            )
            //目标侧一个字节都没有被替换
            assertFalse(repaired?.replacedFileNames.orEmpty().contains(SODIUM_LIKE_IRIS_FILE))
            assertFalse(File(fixture.modsDir, "$SODIUM_LIKE_IRIS_FILE.disabled").exists())
            assertEquals(before[SODIUM_LIKE_IRIS_FILE], sha1Of(File(fixture.modsDir, SODIUM_LIKE_IRIS_FILE)))
        } finally {
            fixture.cleanup()
        }
    }

    // ---- M. 最坏走查：暂存上限不得把有效的第 4 次尝试挡在门外 ----

    @Test
    fun worstCaseWalkStagesTheValidFourthAttempt() = runBlocking {
        val fixture = RepairFixture("worst-case-walk").apply { worstCaseWalkGraph() }
        try {
            val before = snapshot(fixture.modsDir)
            val harness = fixture.harness()

            val outcome = harness.run(breaksFix())

            val staged = harness.stagedCandidates
            val dependentAttempts = staged.count { it.startsWith("dep-two-") }
            val repaired = outcome as? ModDoctorRepairOutcome.Repaired
            assertNotNull(
                "the valid fourth attempt must be reachable " +
                        "(staged=${staged.size}, dep-two attempts=$dependentAttempts, outcome=$outcome)",
                repaired,
            )
            assertEquals(listOf(BREAKER_FILE, DEP_ONE_FILE, DEP_TWO_FILE), repaired?.replacedFileNames)
            //12 个无解目标候选 + 2 个仍在打断目标的源候选 + 1 个安全根候选 + 2 次 + 4 次连带尝试
            assertEquals("every attempt inside the two-pass walk must be staged", 21, staged.size)
            assertEquals("the fourth attempt of the second dependent must be staged", 4, dependentAttempts)
            //目标侧一个字节都没有被动过
            assertEquals(before[TARGET_FILE], sha1Of(File(fixture.modsDir, TARGET_FILE)))
            assertFalse(File(fixture.modsDir, "$TARGET_FILE.disabled").exists())
            //三个替换都真的落了位：原文件可逆保留，替换文件按平台文件名落位
            listOf(BREAKER_FILE, DEP_ONE_FILE, DEP_TWO_FILE).forEach { name ->
                assertTrue("$name should be preserved as .disabled", File(fixture.modsDir, "$name.disabled").exists())
            }
            assertTrue(File(fixture.modsDir, "candidate-$BREAKER_PROJECT-0.8.12+mc1.21.11.jar").exists())
            assertTrue(File(fixture.modsDir, "candidate-$DEP_ONE_PROJECT-2.2.3+mc1.21.11.jar").exists())
            assertTrue(File(fixture.modsDir, "candidate-$DEP_TWO_PROJECT-0.9.1+mc1.21.11.jar").exists())
        } finally {
            fixture.cleanup()
        }
    }

    // ---- N. 界限本身：暂存上限必须容得下最坏走查 ----

    @Test
    fun stagedDownloadLimitCoversTheWorstCaseWalk() {
        //两个冲突侧各自最多走查 12 个根候选；每个根候选 1 次暂存，外加最多 3 个连带模组 × 4 次尝试
        val worstCaseStaged = 2 * MAX_REPAIR_ROOT_CANDIDATES *
                (1 + MAX_REPAIR_DEPENDENTS * MAX_REPAIR_DEPENDENT_ATTEMPTS)

        assertEquals("2 * 12 * (1 + 3 * 4)", 312, worstCaseStaged)
        assertEquals(
            "the staged-download limit must not cut the worst-case walk short",
            worstCaseStaged,
            MAX_REPAIR_STAGED_DOWNLOADS,
        )
    }
}

/** 冲突双方：目标侧与源侧（真实图里被 `breaks` 命中的目标优先，声明 `breaks` 的源模组回退） */
private fun breaksFix() = ModDoctorBreaksFix(
    target = ModDoctorReplacementFix(TARGET_FILE, TARGET_PROJECT, Platform.MODRINTH),
    source = ModDoctorReplacementFix(BREAKER_FILE, BREAKER_PROJECT, Platform.MODRINTH),
)

/** 真实现场的同一条入口：目标侧（被打断的一方）优先，源侧（声明 breaks 的一方）回退 */
private fun sodiumLikeFix() = ModDoctorBreaksFix(
    target = ModDoctorReplacementFix(SODIUM_LIKE_IRIS_FILE, TARGET_PROJECT, Platform.MODRINTH),
    source = ModDoctorReplacementFix(SODIUM_LIKE_FILE, BREAKER_PROJECT, Platform.MODRINTH),
)

/**
 * 一次修复的测试夹具：真实 jar 落在临时 mods 目录里，权威身份与平台版本列表由测试注入，
 * 其余全部走生产代码（读本地元数据、排序、整组校验、复制与回滚）。
 */
private class RepairFixture(name: String) {
    private val root: File = Files.createTempDirectory("moddoctor-multimod-$name").toFile()
    val modsDir: File = File(root, "mods").apply { mkdirs() }
    val stagingDir: File = File(root, "staging").apply { mkdirs() }

    private val identities = mutableMapOf<String, InstalledMod>()
    private val versions = mutableMapOf<String, List<RepairVersion>>()
    private val candidateFacts = mutableMapOf<String, String>()

    val packOwned = mutableSetOf<String>()
    var bounds = ModDoctorRepairBounds()

    /** 一个已安装的启用模组：真实 jar + 权威身份 */
    fun installed(
        fileName: String,
        projectId: String,
        modId: String,
        modVersion: String,
        requires: Map<String, String> = emptyMap(),
        breaks: Map<String, String> = emptyMap(),
        provides: List<String> = emptyList(),
        nestedModules: Map<String, String> = emptyMap(),
    ) {
        //`jars` 段必须列出内嵌 jar，与真实的 Fabric API 文件一致
        val json = modJson(modId, modVersion, requires, breaks, provides, nestedModules.keys)
        writeModJar(modsDir, fileName, json, nestedModules)
        identities[fileName] = InstalledMod(
            platform = Platform.MODRINTH,
            projectId = projectId,
            versionId = "installed-$modId-$modVersion",
            versionName = modVersion,
        )
    }

    /** 某个权威项目在平台上的候选列表（平台给出的顺序：最新在前） */
    fun project(projectId: String, vararg candidates: RepairVersion) {
        versions[projectId] = candidates.toList()
    }

    /** 某个候选的暂存内容（它自己的 fabric.mod.json） */
    fun candidate(projectId: String, version: String, json: String) {
        candidateFacts[RepairVersion.stagingKey(projectId, version)] = json
    }

    /** 直接写入一个暂存文件（用于驱动 apply 阶段） */
    fun writeStaged(label: String, json: String): File = writeModJar(stagingDir, "$label.jar", json)

    /**
     * 真实四模组冲突的最小复刻（中性 id）：
     * - 破坏方 9.0.0 声明 breaks 目标 ≤ 4.0.0，目标 4.0.0 已安装 → INCOMPATIBLE_MOD_PAIR
     * - 两个依赖方各自要求破坏方恰好 9.0.0
     * - [withSafeBreakerBuild] 为 true 时，破坏方项目还提供不再命中目标的 8.2.0
     */
    fun fourModGraph(withSafeBreakerBuild: Boolean = true) {
        installed(TARGET_FILE, TARGET_PROJECT, TARGET_ID, "4.0.0")
        installed(BREAKER_FILE, BREAKER_PROJECT, BREAKER_ID, "9.0.0", breaks = mapOf(TARGET_ID to "<=4.0.0"))
        installed(DEP_ONE_FILE, DEP_ONE_PROJECT, DEP_ONE_ID, "3.0.0", requires = mapOf(BREAKER_ID to "9.0.0"))
        installed(DEP_TWO_FILE, DEP_TWO_PROJECT, DEP_TWO_ID, "2.0.0", requires = mapOf(BREAKER_ID to "9.0.0"))
        installed(UNRELATED_FILE, "proj-unrelated", "test-unrelated", "1.0.0")

        //目标项目只有已安装的构建：它仍被破坏方命中，没有安全替换
        project(TARGET_PROJECT, version(TARGET_PROJECT, "4.0.0"))
        candidate(TARGET_PROJECT, "4.0.0", modJson(TARGET_ID, "4.0.0"))

        val breakerCandidates = mutableListOf(version(BREAKER_PROJECT, "9.0.0"))
        candidate(BREAKER_PROJECT, "9.0.0", modJson(BREAKER_ID, "9.0.0", breaks = mapOf(TARGET_ID to "<=4.0.0")))
        if (withSafeBreakerBuild) {
            breakerCandidates += version(BREAKER_PROJECT, "8.2.0")
            candidate(BREAKER_PROJECT, "8.2.0", modJson(BREAKER_ID, "8.2.0", breaks = mapOf(TARGET_ID to "<=3.0.0")))
        }
        project(BREAKER_PROJECT, *breakerCandidates.toTypedArray())

        //两个依赖方：只有更新的构建才接受 8.2.0
        project(DEP_ONE_PROJECT, version(DEP_ONE_PROJECT, "4.0.0"), version(DEP_ONE_PROJECT, "3.0.0"))
        candidate(DEP_ONE_PROJECT, "4.0.0", modJson(DEP_ONE_ID, "4.0.0", requires = mapOf(BREAKER_ID to ">=8.0.0")))
        candidate(DEP_ONE_PROJECT, "3.0.0", modJson(DEP_ONE_ID, "3.0.0", requires = mapOf(BREAKER_ID to "9.0.0")))

        project(DEP_TWO_PROJECT, version(DEP_TWO_PROJECT, "2.5.0"), version(DEP_TWO_PROJECT, "2.0.0"))
        candidate(DEP_TWO_PROJECT, "2.5.0", modJson(DEP_TWO_ID, "2.5.0", requires = mapOf(BREAKER_ID to ">=8.0.0")))
        candidate(DEP_TWO_PROJECT, "2.0.0", modJson(DEP_TWO_ID, "2.0.0", requires = mapOf(BREAKER_ID to "9.0.0")))
    }

    /**
     * 真实 QA 现场（中性 id、真实版本串与真实谓词写法）：
     * - 0.8.14 声明 breaks 目标 ≤ 1.10.7，目标已安装 → INCOMPATIBLE_MOD_PAIR
     * - 源侧候选 0.8.12 不再打断目标，但两个依赖方的谓词（`=0.8.14+mc1.21.11`、`>=0.8.14+mc1.21.11`）
     *   都要求 0.8.14，因此安全计划必须同时带上这两个依赖方
     */
    fun sodiumLikeGraph() {
        installed(SODIUM_LIKE_IRIS_FILE, TARGET_PROJECT, TARGET_ID, "1.10.7+mc1.21.11")
        installed(
            SODIUM_LIKE_FILE, BREAKER_PROJECT, BREAKER_ID, "0.8.14+mc1.21.11",
            breaks = mapOf(TARGET_ID to "<=1.10.7"),
        )
        installed(
            SODIUM_LIKE_EXTRA_FILE, DEP_ONE_PROJECT, DEP_ONE_ID, "0.9.4+mc1.21.11",
            requires = mapOf(BREAKER_ID to ">=0.8.14+mc1.21.11"),
        )
        installed(
            SODIUM_LIKE_REESES_FILE, DEP_TWO_PROJECT, DEP_TWO_ID, "2.2.4+mc1.21.11",
            requires = mapOf(BREAKER_ID to "=0.8.14+mc1.21.11"),
        )

        //目标侧只有已安装的构建：它仍被 0.8.14 命中，没有安全替换
        project(TARGET_PROJECT, version(TARGET_PROJECT, "1.10.7+mc1.21.11"))
        candidate(TARGET_PROJECT, "1.10.7+mc1.21.11", modJson(TARGET_ID, "1.10.7+mc1.21.11"))

        //源侧：0.8.12 不再打断目标
        project(
            BREAKER_PROJECT,
            version(BREAKER_PROJECT, "0.8.14+mc1.21.11"),
            version(BREAKER_PROJECT, "0.8.12+mc1.21.11"),
        )
        candidate(
            BREAKER_PROJECT, "0.8.14+mc1.21.11",
            modJson(BREAKER_ID, "0.8.14+mc1.21.11", breaks = mapOf(TARGET_ID to "<=1.10.7")),
        )
        candidate(BREAKER_PROJECT, "0.8.12+mc1.21.11", modJson(BREAKER_ID, "0.8.12+mc1.21.11"))

        //依赖方一：0.9.4 与 0.9.3 都要求 0.8.14/0.8.13，只有 0.9.2 接受 0.8.12
        project(
            DEP_ONE_PROJECT,
            version(DEP_ONE_PROJECT, "0.9.4+mc1.21.11"),
            version(DEP_ONE_PROJECT, "0.9.3+mc1.21.11"),
            version(DEP_ONE_PROJECT, "0.9.2+mc1.21.11"),
        )
        candidate(
            DEP_ONE_PROJECT, "0.9.4+mc1.21.11",
            modJson(DEP_ONE_ID, "0.9.4+mc1.21.11", requires = mapOf(BREAKER_ID to ">=0.8.14+mc1.21.11")),
        )
        candidate(
            DEP_ONE_PROJECT, "0.9.3+mc1.21.11",
            modJson(DEP_ONE_ID, "0.9.3+mc1.21.11", requires = mapOf(BREAKER_ID to ">=0.8.13+mc1.21.11")),
        )
        candidate(
            DEP_ONE_PROJECT, "0.9.2+mc1.21.11",
            modJson(DEP_ONE_ID, "0.9.2+mc1.21.11", requires = mapOf(BREAKER_ID to ">=0.8.12+mc1.21.11")),
        )

        //依赖方二：2.2.4 要求恰好 0.8.14，2.2.3 只要求 ≥ 0.8.11
        project(
            DEP_TWO_PROJECT,
            version(DEP_TWO_PROJECT, "2.2.4+mc1.21.11"),
            version(DEP_TWO_PROJECT, "2.2.3+mc1.21.11"),
        )
        candidate(
            DEP_TWO_PROJECT, "2.2.4+mc1.21.11",
            modJson(DEP_TWO_ID, "2.2.4+mc1.21.11", requires = mapOf(BREAKER_ID to "=0.8.14+mc1.21.11")),
        )
        candidate(
            DEP_TWO_PROJECT, "2.2.3+mc1.21.11",
            modJson(DEP_TWO_ID, "2.2.3+mc1.21.11", requires = mapOf(BREAKER_ID to ">=0.8.11")),
        )
    }

    /**
     * 走查上限的最坏形状（现场四模组图的中性放大）：
     * - 目标项目列出 12 个构建，每一个都仍被已安装破坏方的 `<=1.10.7` 命中：目标侧永远不成立；
     * - 破坏方项目列出 3 个构建，前两个（含当前版本）仍然打断目标，第三个才安全；
     * - 两个连带方都要求破坏方 ≥0.8.13，因此安全根候选必须连带替换它们：先被走查的一方第 2 次尝试
     *   即成立，后被走查的一方 4 次尝试里只有第 4 次接受 0.8.12。
     * 修复前 12 + 2 + 1 + 2 + 3 = 20 次暂存正好把上限用满，那个第 4 次尝试被拒绝。
     */
    fun worstCaseWalkGraph() {
        installed(TARGET_FILE, TARGET_PROJECT, TARGET_ID, "1.10.7+mc1.21.11")
        installed(
            BREAKER_FILE, BREAKER_PROJECT, BREAKER_ID, "0.8.14+mc1.21.11",
            breaks = mapOf(TARGET_ID to "<=1.10.7"),
        )
        installed(
            DEP_ONE_FILE, DEP_ONE_PROJECT, DEP_ONE_ID, "2.2.4+mc1.21.11",
            requires = mapOf(BREAKER_ID to ">=0.8.13+mc1.21.11"),
        )
        installed(
            DEP_TWO_FILE, DEP_TWO_PROJECT, DEP_TWO_ID, "0.9.4+mc1.21.11",
            requires = mapOf(BREAKER_ID to ">=0.8.13+mc1.21.11"),
        )

        //目标侧：12 个构建全部仍被 <=1.10.7 命中
        val targetBuilds = listOf(
            "1.10.7+mc1.21.11", "1.10.6+mc1.21.11", "1.10.5+mc1.21.11", "1.10.4+mc1.21.11",
            "1.10.3+mc1.21.11", "1.10.2+mc1.21.11", "1.10.1+mc1.21.11", "1.10.0+mc1.21.11",
            "1.9.9+mc1.21.11", "1.9.8+mc1.21.11", "1.9.7+mc1.21.11", "1.9.6+mc1.21.11",
        )
        project(TARGET_PROJECT, *targetBuilds.map { build -> version(TARGET_PROJECT, build) }.toTypedArray())
        targetBuilds.forEach { build -> candidate(TARGET_PROJECT, build, modJson(TARGET_ID, build)) }

        //源侧：前两个仍在打断目标，第三个（0.8.12）才安全
        project(
            BREAKER_PROJECT,
            version(BREAKER_PROJECT, "0.8.14+mc1.21.11"),
            version(BREAKER_PROJECT, "0.8.13+mc1.21.11"),
            version(BREAKER_PROJECT, "0.8.12+mc1.21.11"),
        )
        candidate(
            BREAKER_PROJECT, "0.8.14+mc1.21.11",
            modJson(BREAKER_ID, "0.8.14+mc1.21.11", breaks = mapOf(TARGET_ID to "<=1.10.7")),
        )
        candidate(
            BREAKER_PROJECT, "0.8.13+mc1.21.11",
            modJson(BREAKER_ID, "0.8.13+mc1.21.11", breaks = mapOf(TARGET_ID to "<=1.10.7")),
        )
        candidate(BREAKER_PROJECT, "0.8.12+mc1.21.11", modJson(BREAKER_ID, "0.8.12+mc1.21.11"))

        //先被走查的连带方：2 次尝试，第 2 次接受 0.8.12
        project(DEP_ONE_PROJECT, version(DEP_ONE_PROJECT, "2.2.4+mc1.21.11"), version(DEP_ONE_PROJECT, "2.2.3+mc1.21.11"))
        candidate(
            DEP_ONE_PROJECT, "2.2.4+mc1.21.11",
            modJson(DEP_ONE_ID, "2.2.4+mc1.21.11", requires = mapOf(BREAKER_ID to ">=0.8.13+mc1.21.11")),
        )
        candidate(
            DEP_ONE_PROJECT, "2.2.3+mc1.21.11",
            modJson(DEP_ONE_ID, "2.2.3+mc1.21.11", requires = mapOf(BREAKER_ID to ">=0.8.11")),
        )

        //后被走查的连带方：4 次尝试，只有第 4 次接受 0.8.12
        project(
            DEP_TWO_PROJECT,
            version(DEP_TWO_PROJECT, "0.9.4+mc1.21.11"),
            version(DEP_TWO_PROJECT, "0.9.3+mc1.21.11"),
            version(DEP_TWO_PROJECT, "0.9.2+mc1.21.11"),
            version(DEP_TWO_PROJECT, "0.9.1+mc1.21.11"),
        )
        candidate(
            DEP_TWO_PROJECT, "0.9.4+mc1.21.11",
            modJson(DEP_TWO_ID, "0.9.4+mc1.21.11", requires = mapOf(BREAKER_ID to ">=0.8.14+mc1.21.11")),
        )
        candidate(
            DEP_TWO_PROJECT, "0.9.3+mc1.21.11",
            modJson(DEP_TWO_ID, "0.9.3+mc1.21.11", requires = mapOf(BREAKER_ID to ">=0.8.13+mc1.21.11")),
        )
        candidate(
            DEP_TWO_PROJECT, "0.9.2+mc1.21.11",
            modJson(DEP_TWO_ID, "0.9.2+mc1.21.11", requires = mapOf(BREAKER_ID to "=0.8.14+mc1.21.11")),
        )
        candidate(
            DEP_TWO_PROJECT, "0.9.1+mc1.21.11",
            modJson(DEP_TWO_ID, "0.9.1+mc1.21.11", requires = mapOf(BREAKER_ID to ">=0.8.12+mc1.21.11")),
        )
    }

    fun harness(): RepairHarness = RepairHarness(
        modsDir = modsDir,
        stagingDir = stagingDir,
        identities = identities,
        versions = versions,
        candidateFacts = candidateFacts,
        packOwned = packOwned,
        bounds = bounds,
    )

    fun cleanup() {
        root.deleteRecursively()
    }
}

/** 注入权威身份/平台版本列表，其余走生产代码；记录实际发生的查询与暂存 */
private class RepairHarness(
    modsDir: File,
    private val stagingDir: File,
    private val identities: Map<String, InstalledMod>,
    private val versions: Map<String, List<RepairVersion>>,
    private val candidateFacts: Map<String, String>,
    packOwned: Set<String>,
    bounds: ModDoctorRepairBounds,
) {
    val fingerprintRequests = mutableListOf<String>()
    val versionRequests = mutableListOf<String>()
    val stagedCandidates = mutableListOf<String>()

    private val repair = ModDoctorMultiModRepair(
        minecraftVersion = INSTANCE_MINECRAFT_VERSION,
        loader = ModLoader.FABRIC,
        modsDir = modsDir,
        isPackOwned = { fileName -> fileName in packOwned },
        installedIdentity = { fileName ->
            fingerprintRequests += fileName
            identities[fileName]
        },
        projectVersions = { projectId, _ ->
            versionRequests += projectId
            versions[projectId].orEmpty()
        },
        stageCandidate = { candidate ->
            stagedCandidates += candidate.platformId()
            candidateFacts[candidate.platformId()]?.let { json ->
                writeModJar(stagingDir, "${candidate.platformId()}.jar", json)
            }
        },
        bounds = bounds,
    )

    suspend fun run(fix: ModDoctorBreaksFix): ModDoctorRepairOutcome = repair.repair(fix)

    suspend fun apply(plan: ModDoctorRepairPlan): ModDoctorRepairOutcome = repair.apply(plan)
}

/** 平台版本桩：自带 id/项目/文件名/发布类型/sha1 */
private class RepairVersion(
    private val id: String,
    private val projectId: String,
    private val fileName: String,
    private val version: String,
    private val prerelease: Boolean = false,
    private val sha1: String? = null,
) : PlatformVersion {
    override suspend fun initFile(currentProjectId: String) = true
    override fun platform() = Platform.MODRINTH
    override fun platformId() = id
    override fun platformProjectId() = projectId
    override fun platformDisplayName() = version
    override fun platformFileName() = fileName
    override fun platformGameVersion() = arrayOf(INSTANCE_MINECRAFT_VERSION)
    override fun platformLoaders(): List<PlatformDisplayLabel> = listOf(RepairLoaderLabel("Fabric"))
    override fun platformReleaseType() = if (prerelease) PlatformReleaseType.BETA else PlatformReleaseType.RELEASE
    override fun platformDependencies() = emptyList<PlatformVersion.PlatformDependency>()
    override fun platformDownloadCount() = 0L
    override fun platformDownloadUrl() = ""
    override fun platformDatePublished() = Instant.parse("2026-07-28T08:48:35Z")
    override fun platformSha1() = sha1
    override fun platformFileSize() = 0L
    override fun platformVersion() = version

    companion object {
        /** 暂存键：与测试里声明的候选内容一一对应，同时让走查顺序可以被断言 */
        fun stagingKey(projectId: String, version: String): String =
            "${projectId.removePrefix("proj-")}-$projectId-$version"
    }
}

private class RepairLoaderLabel(private val name: String) : PlatformDisplayLabel {
    override fun getDisplayName() = name
    override fun index() = 0
}

/** 与真实测试实例一致的目标环境 */
private const val INSTANCE_MINECRAFT_VERSION = "1.21.11"

private const val TARGET_FILE = "test-target-4.0.0.jar"
private const val TARGET_PROJECT = "proj-target"
private const val TARGET_ID = "test-target"

private const val BREAKER_FILE = "test-breaker-9.0.0.jar"
private const val BREAKER_PROJECT = "proj-breaker"
private const val BREAKER_ID = "test-breaker"

private const val DEP_ONE_FILE = "test-dep-one-3.0.0.jar"
private const val DEP_ONE_PROJECT = "proj-dep-one"
private const val DEP_ONE_ID = "test-dep-one"

private const val DEP_TWO_FILE = "test-dep-two-2.0.0.jar"
private const val DEP_TWO_PROJECT = "proj-dep-two"
private const val DEP_TWO_ID = "test-dep-two"

private const val UNRELATED_FILE = "test-unrelated-1.0.0.jar"

/** 真实 QA 现场用到的版本串：破坏方与两个依赖方都用「基础版本+构建元数据」的写法 */
private const val SODIUM_LIKE_IRIS_FILE = "test-target-1.10.7+mc1.21.11.jar"
private const val SODIUM_LIKE_FILE = "test-breaker-0.8.14+mc1.21.11.jar"
private const val SODIUM_LIKE_EXTRA_FILE = "test-dep-one-0.9.4+mc1.21.11.jar"
private const val SODIUM_LIKE_REESES_FILE = "test-dep-two-2.2.4+mc1.21.11.jar"

/** 真实形状的 Fabric API：内嵌模块自己报 id，顶层 `provides` 报别名 */
private const val FABRIC_API_FILE = "fabric-api-0.141.6+1.21.11.jar"
private const val FABRIC_API_ID = "fabric-api"

private const val MOD_MENU_FILE = "modmenu-11.0.0.jar"
private const val MOD_MENU_ID = "modmenu"

private const val NESTED_MODULE_ENTRY = "META-INF/jars/fabric-resource-loader-v1-1.0.10.jar"
private const val NESTED_MODULE_ID = "fabric-resource-loader-v1"
private const val NESTED_MODULE_VERSION = "1.0.10+78c8b4663e"

/** 平台候选桩：正式版与预发布版都支持当前 Minecraft + Fabric */
private fun version(
    projectId: String,
    version: String,
    prerelease: Boolean = false,
    sha1: String? = null,
): RepairVersion = RepairVersion(
    id = RepairVersion.stagingKey(projectId, version),
    projectId = projectId,
    fileName = "candidate-$projectId-$version.jar",
    version = version,
    prerelease = prerelease,
    sha1 = sha1,
)

/** 候选自带的 fabric.mod.json：只有测试用到的字段 */
private fun modJson(
    id: String,
    version: String,
    requires: Map<String, String> = emptyMap(),
    breaks: Map<String, String> = emptyMap(),
    provides: List<String> = emptyList(),
    jarEntries: Collection<String> = emptyList(),
): String {
    val members = mutableListOf("\"id\": \"$id\"", "\"version\": \"$version\"")
    if (requires.isNotEmpty()) members += "\"depends\": { ${predicateMap(requires)} }"
    if (breaks.isNotEmpty()) members += "\"breaks\": { ${predicateMap(breaks)} }"
    if (provides.isNotEmpty()) members += "\"provides\": [ ${provides.joinToString(", ") { "\"$it\"" }} ]"
    if (jarEntries.isNotEmpty()) {
        members += "\"jars\": [ ${jarEntries.joinToString(", ") { entry -> "{ \"file\": \"$entry\" }" }} ]"
    }
    return "{ ${members.joinToString(", ")} }"
}

private fun predicateMap(predicates: Map<String, String>): String =
    predicates.entries.joinToString(", ") { (id, predicate) -> "\"$id\": \"$predicate\"" }

private fun writeModJar(
    dir: File,
    fileName: String,
    json: String,
    nestedModules: Map<String, String> = emptyMap(),
): File {
    val file = File(dir, fileName)
    ZipOutputStream(file.outputStream()).use { zip ->
        zip.putNextEntry(ZipEntry("fabric.mod.json"))
        zip.write(json.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        nestedModules.forEach { (entryName, nestedJson) ->
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

/** mods 目录的快照：文件名 → SHA-1 */
private fun snapshot(dir: File): Map<String, String> =
    dir.listFiles().orEmpty()
        .filter { it.isFile }
        .associate { it.name to sha1Of(it) }

private fun disabledFiles(dir: File): List<String> =
    dir.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(".disabled") }.map { it.name }

private fun sha1Of(file: File): String {
    val digest = MessageDigest.getInstance("SHA-1")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
