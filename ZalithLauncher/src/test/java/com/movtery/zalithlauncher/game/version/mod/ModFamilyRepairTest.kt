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
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformFilterCode
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformProject
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * ONE repair plan สำหรับทั้งชุด Sodium / Sodium Extra / Reese's Sodium Options / Iris / Fabric API
 *
 * ข้อมูลทั้งหมดเป็นรูปทรงเดียวกับ metadata จริง (depends / breaks ของ jar) แต่ใช้ id ของม็อดจริงเพราะเป็นกรณี QA
 * ตัว planner ไม่มีกฎรายคู่เวอร์ชัน: ทุกอย่างมาจากช่วงเวอร์ชันที่ jar ประกาศ
 */
class ModFamilyRepairTest {

    // ---- MC 1.21.1: Iris 1.8.8 ชนกับ Sodium 0.8.13 (STEP 39) ----

    @Test
    fun mc1211IrisIsUpgradedInsteadOfDowngradingSodiumWhenSodiumUsersNeedTheNewSodium() = runBlocking {
        val fx = FamFixture("mc1211")
        try {
            fx.sodium0813(); fx.extra094(); fx.reeses224(); fx.iris188()
            //Sodium ย้อนกลับไป 0.6.13 ไม่ได้เพราะ Extra/Reese's ต้องการ 0.8.13; Iris ที่รองรับ Sodium 0.8.x มีรุ่นใหม่กว่า
            fx.project(
                "proj-iris",
                famVersion("proj-iris", "1.8.14-beta.1", "iris-1.8.14-beta.1.jar", prerelease = true),
                famVersion("proj-iris", "1.8.12", "iris-1.8.12.jar"),
                famVersion("proj-iris", "1.8.8", "iris-1.8.8.jar"),
            )
            fx.candidate("proj-iris", "1.8.14-beta.1", famJson("iris", "1.8.14-beta.1", requires = mapOf("sodium" to "0.8.x")))
            fx.candidate("proj-iris", "1.8.12", famJson("iris", "1.8.12", requires = mapOf("sodium" to "0.6.x")))
            fx.project(
                "proj-sodium",
                famVersion("proj-sodium", "0.8.13", "sodium-0.8.13.jar"),
                famVersion("proj-sodium", "0.6.13", "sodium-0.6.13.jar"),
            )
            fx.candidate("proj-sodium", "0.6.13", famJson("sodium", "0.6.13"))

            val outcome = fx.planner().plan()

            val plan = (outcome as? FamilyOutcome.Plan)?.plan
            assertNotNull("expected a plan, got $outcome", plan)
            assertEquals(listOf("iris"), plan!!.changes.map { it.modId })
            assertEquals(FamilyChangeKind.UPGRADE, plan.changes.single().kind)
            assertEquals("1.8.14-beta.1", plan.changes.single().toVersion)
            //ไม่แตะม็อดอื่นเลย และ 1.8.12 ที่ metadata ประกาศว่า sodium ชนกัน ถูกตัดก่อนดาวน์โหลด
            assertTrue(fx.staged.none { it.contains("1.8.12") })
            assertTrue(fx.staged.none { it.contains("0.6.13") && it.contains("sodium") })
        } finally {
            fx.cleanup()
        }
    }

    @Test
    fun anyUserSubsetWorks_onlyIrisAndSodiumInstalled() = runBlocking {
        val fx = FamFixture("subset2")
        try {
            fx.sodium0813(withDependents = false); fx.iris188()
            fx.project("proj-iris", famVersion("proj-iris", "1.8.14-beta.1", "iris-1.8.14-beta.1.jar", prerelease = true))
            fx.candidate("proj-iris", "1.8.14-beta.1", famJson("iris", "1.8.14-beta.1", requires = mapOf("sodium" to "0.8.x")))
            fx.project("proj-sodium", famVersion("proj-sodium", "0.6.13", "sodium-0.6.13.jar"))
            fx.candidate("proj-sodium", "0.6.13", famJson("sodium", "0.6.13"))

            val plan = (fx.planner().plan() as? FamilyOutcome.Plan)?.plan

            assertNotNull(plan)
            //ขั้นแรกลองอัปเกรดก่อนดาวน์เกรด: Iris รุ่นใหม่แก้ได้ จึงไม่ต้องลดรุ่น Sodium
            assertEquals(listOf("iris"), plan!!.changes.map { it.modId })
        } finally {
            fx.cleanup()
        }
    }

    // ---- MC 1.20.1: Iris 1.6.11 + Sodium 0.5.13 + Extra + Reese's, ไม่มี Fabric API ----

    @Test
    fun mc1201OnePlanInstallsFabricApiAndUpgradesIrisWithoutTouchingSodiumExtraReeses() = runBlocking {
        val fx = FamFixture("mc1201")
        try {
            fx.install("sodium-0.5.13.jar", "proj-sodium", "sodium", "0.5.13", breaks = mapOf("iris" to "<1.7.6"))
            fx.install("sodium-extra-0.5.9.jar", "proj-extra", "sodium-extra", "0.5.9",
                requires = mapOf("fabric-api" to "*", "sodium" to ">=0.5.6"), breaks = mapOf("iris" to "<1.7.0"))
            fx.install("reeses-1.7.2.jar", "proj-reeses", "reeses-sodium-options", "1.7.2",
                requires = mapOf("sodium" to "*"), breaks = mapOf("iris" to "<1.1.4", "sodium-extra" to "<0.5.3", "sodium" to "<0.5.8"))
            fx.install("iris-1.6.11.jar", "proj-iris", "iris", "1.6.11", requires = mapOf("sodium" to "0.5.3"))
            fx.project(
                "proj-iris",
                famVersion("proj-iris", "1.7.6", "iris-1.7.6.jar"),
                famVersion("proj-iris", "1.6.11", "iris-1.6.11-other.jar"),
            )
            fx.candidate("proj-iris", "1.7.6", famJson("iris", "1.7.6", requires = mapOf("sodium" to "0.5.x")))
            fx.dependencyProject("fabric-api", "proj-fabric-api")
            fx.project("proj-fabric-api", famVersion("proj-fabric-api", "0.92.12", "fabric-api-0.92.12.jar"))
            fx.candidate("proj-fabric-api", "0.92.12", famJson("fabric-api", "0.92.12"))
            //Sodium 0.5.3 ที่ iris 1.6.11 ต้องการ ใช้ไม่ได้เพราะ Extra/Reese's ต้องการใหม่กว่า — แต่ก็ไม่ควรถูกลองก่อนการอัปเกรด Iris
            fx.project("proj-sodium", famVersion("proj-sodium", "0.5.3", "sodium-0.5.3.jar"))
            fx.candidate("proj-sodium", "0.5.3", famJson("sodium", "0.5.3"))

            val plan = (fx.planner().plan() as? FamilyOutcome.Plan)?.plan

            assertNotNull(plan)
            assertEquals(setOf("iris", "fabric-api"), plan!!.changes.map { it.modId }.toSet())
            assertEquals(FamilyChangeKind.UPGRADE, plan.changes.first { it.modId == "iris" }.kind)
            assertEquals(FamilyChangeKind.INSTALL, plan.changes.first { it.modId == "fabric-api" }.kind)
            assertTrue(plan.changes.none { it.modId == "sodium" || it.modId == "sodium-extra" || it.modId == "reeses-sodium-options" })
            //ทุกไฟล์ที่ใน plan ถูกอ่าน metadata จริงจากไฟล์ที่ดาวน์โหลดมา
            assertTrue(plan.changes.all { it.stagedFile.isFile && it.sha1.isNotBlank() })
        } finally {
            fx.cleanup()
        }
    }

    // ---- ไม่มีชุดที่เข้ากันได้: ปฏิเสธอย่างปลอดภัย ไม่แตะอะไร ----

    @Test
    fun noCompatibleSetRefusesSafelyAndChangesNothing() = runBlocking {
        val fx = FamFixture("refuse")
        try {
            fx.sodium0813(withDependents = false); fx.iris188()
            //Iris ทุกรุ่นยังต้องการ Sodium 0.6.x และ Sodium ไม่มีรุ่นที่ iris 1.8.8 รับได้
            fx.project("proj-iris", famVersion("proj-iris", "1.8.9", "iris-1.8.9.jar"))
            fx.candidate("proj-iris", "1.8.9", famJson("iris", "1.8.9", requires = mapOf("sodium" to "0.6.x")))
            fx.project("proj-sodium", famVersion("proj-sodium", "0.8.13", "sodium-0.8.13.jar"))
            val before = fx.snapshot()

            val outcome = fx.planner().plan()

            assertTrue("expected a refusal, got $outcome", outcome is FamilyOutcome.Refused)
            assertEquals(before, fx.snapshot())
            assertTrue(fx.stagingFiles().isEmpty())
        } finally {
            fx.cleanup()
        }
    }

    @Test
    fun consistentSetNeedsNoPlan() = runBlocking {
        val fx = FamFixture("clean")
        try {
            fx.install("sodium-a.jar", "proj-sodium", "sodium", "0.8.13")
            fx.install("iris-a.jar", "proj-iris", "iris", "1.8.14", requires = mapOf("sodium" to "0.8.x"))
            assertEquals(FamilyOutcome.NotApplicable, fx.planner().plan())
        } finally {
            fx.cleanup()
        }
    }

    @Test
    fun packOwnedFilesAreNeverChanged() = runBlocking {
        val fx = FamFixture("pack")
        try {
            fx.sodium0813(withDependents = false); fx.iris188()
            fx.project("proj-iris", famVersion("proj-iris", "1.8.14-beta.1", "iris-1.8.14-beta.1.jar"))
            fx.candidate("proj-iris", "1.8.14-beta.1", famJson("iris", "1.8.14-beta.1", requires = mapOf("sodium" to "0.8.x")))
            fx.packOwned += "iris-1.8.8.jar"

            val outcome = fx.planner().plan()

            assertTrue(outcome is FamilyOutcome.Refused)
            assertTrue(fx.staged.isEmpty())
        } finally {
            fx.cleanup()
        }
    }

    @Test
    fun aCandidateThatDoesNotProvideTheReplacedIdIsRejected() = runBlocking {
        val fx = FamFixture("wrong-id")
        try {
            fx.sodium0813(withDependents = false); fx.iris188()
            fx.project("proj-iris", famVersion("proj-iris", "1.8.14-beta.1", "iris-1.8.14-beta.1.jar"))
            fx.candidate("proj-iris", "1.8.14-beta.1", famJson("not-iris", "1.8.14-beta.1"))

            assertTrue(fx.planner().plan() is FamilyOutcome.Refused)
        } finally {
            fx.cleanup()
        }
    }

    @Test
    fun downloadFailureIsReportedAsFailureNotAsAPlan() = runBlocking {
        val fx = FamFixture("fail")
        try {
            fx.sodium0813(withDependents = false); fx.iris188()
            fx.project("proj-iris", famVersion("proj-iris", "1.8.14-beta.1", "iris-1.8.14-beta.1.jar"))
            //候选没有暂存内容 → 下载失败
            assertTrue(fx.planner().plan() is FamilyOutcome.Failed)
            assertTrue(fx.stagingFiles().isEmpty())
        } finally {
            fx.cleanup()
        }
    }

    // ---- แผนที่เก็บไว้จาก Preview ใช้ซ้ำตอน Fix ได้ก็ต่อเมื่อทุกอย่างยังเหมือนเดิม ----

    private suspend fun FamFixture.irisUpgradePlan(): FamilyRepairPlan {
        sodium0813(withDependents = false); iris188()
        project("proj-iris", famVersion("proj-iris", "1.8.14-beta.1", "iris-1.8.14-beta.1.jar", prerelease = true))
        candidate("proj-iris", "1.8.14-beta.1", famJson("iris", "1.8.14-beta.1", requires = mapOf("sodium" to "0.8.x")))
        return (planner().plan() as FamilyOutcome.Plan).plan
    }

    @Test
    fun cachedPlanIsValidWhenNothingChanged_andMarksPrerelease() = runBlocking {
        val fx = FamFixture("cache-ok")
        try {
            val plan = fx.irisUpgradePlan()
            assertTrue("the beta must be flagged for the Preview", plan.changes.single().prerelease)
            assertTrue(plan.changes.single().replacedSha1?.isNotBlank() == true)
            assertEquals(null, validateFamilyPlan(plan, fx.modsDir, readEnabledModIdentities(fx.modsDir)))
        } finally {
            fx.cleanup()
        }
    }

    @Test
    fun cachedPlanIsRejectedWhenTheInstalledFileChanged() = runBlocking {
        val fx = FamFixture("cache-changed")
        try {
            val plan = fx.irisUpgradePlan()
            //ผู้ใช้เปลี่ยนไฟล์ม็อดที่จะถูกแทนที่หลังจากดู Preview
            famWriteJar(fx.modsDir, "iris-1.8.8.jar", famJson("iris", "1.8.8", requires = mapOf("sodium" to "0.6.x"), breaks = mapOf("x" to "*")))
            assertNotNull(validateFamilyPlan(plan, fx.modsDir, readEnabledModIdentities(fx.modsDir)))
        } finally {
            fx.cleanup()
        }
    }

    @Test
    fun cachedPlanIsRejectedWhenAStagedFileWasTamperedWith() = runBlocking {
        val fx = FamFixture("cache-tamper")
        try {
            val plan = fx.irisUpgradePlan()
            plan.changes.single().stagedFile.appendBytes(byteArrayOf(1, 2, 3))
            assertNotNull(validateFamilyPlan(plan, fx.modsDir, readEnabledModIdentities(fx.modsDir)))
        } finally {
            fx.cleanup()
        }
    }

    @Test
    fun cachedPlanIsRejectedWhenTheModSetChangedAfterThePreview() = runBlocking {
        val fx = FamFixture("cache-set")
        try {
            val plan = fx.irisUpgradePlan()
            //ผู้ใช้ลบ Iris เอง: ปัญหาของชุดเปลี่ยนไป แผนเดิมไม่ใช่สิ่งที่ผู้ใช้เห็นอีกต่อไป
            File(fx.modsDir, "iris-1.8.8.jar").delete()
            assertNotNull(validateFamilyPlan(plan, fx.modsDir, readEnabledModIdentities(fx.modsDir)))
        } finally {
            fx.cleanup()
        }
    }

    @Test
    fun planCacheOnlyHandsOutTheApprovedKeyAndExpires() = runBlocking {
        val fx = FamFixture("cache-take")
        try {
            val plan = fx.irisUpgradePlan()
            FamilyPlanCache.put("inst", plan, now = 1_000L)
            assertEquals(null, FamilyPlanCache.take("inst", "another-key", now = 1_100L))
            //กุญแจไม่ตรง = ทิ้งแผนและลบไฟล์ที่พักไปแล้ว
            assertTrue(plan.changes.none { it.stagedFile.exists() })
        } finally {
            fx.cleanup()
        }
        val fx2 = FamFixture("cache-expire")
        try {
            val plan2 = fx2.irisUpgradePlan()
            FamilyPlanCache.put("inst2", plan2, now = 1_000L)
            assertEquals(null, FamilyPlanCache.take("inst2", plan2.key, now = 1_000L + FamilyPlanCache.MAX_AGE_MILLIS + 1))
            FamilyPlanCache.put("inst3", plan2, now = 5_000L)
            FamilyPlanCache.discard("inst3")
            assertEquals(null, FamilyPlanCache.take("inst3", plan2.key, now = 5_001L))
        } finally {
            fx2.cleanup()
        }
    }

    // ---- เส้นทางเดิม: Preview เฉพาะที่ระบุไฟล์ได้แน่ ๆ ล่วงหน้า ไม่สร้าง Preview หลอก ----

    private fun legacyProblem(
        type: ModDoctorProblemType = ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR,
        file: String = "a.jar",
        setFix: ModDoctorSetFix? = null,
        breaksFix: ModDoctorBreaksFix? = null,
        local: ModDoctorLocalDisableFix? = null,
    ) = ModDoctorProblem(type = type, fileName = file, modId = "a", reason = "because", ruleFallbackFix = setFix, breaksFix = breaksFix, localDisableFix = local)

    private val someReplacement = ModDoctorReplacementFix("a.jar", "proj-a", Platform.MODRINTH)

    @Test
    fun legacyRulePlanListsExactlyTheRuleMembers() {
        val rule = ModDoctorSetFix(listOf(ModDoctorSetMember("sodium", "sodium-0.8.14.jar", "0.8.12")), mapOf("sodium" to "0.8.12"))
        val result = buildLegacyRepairPreview(ModDoctorReport(listOf(legacyProblem(setFix = rule))))

        val plan = (result as LegacyPreviewResult.Plan)
        assertEquals(listOf("sodium-0.8.14.jar"), plan.preview.items.map { it.fromFile })
        assertEquals("0.8.12", plan.preview.items.single().toVersion)
        assertEquals(LEGACY_PLAN_KEY, plan.preview.key)
    }

    @Test
    fun legacyFixesThatCannotListTheirFilesAreRefused() {
        val report = ModDoctorReport(listOf(legacyProblem(breaksFix = ModDoctorBreaksFix(someReplacement))))
        assertTrue(buildLegacyRepairPreview(report) is LegacyPreviewResult.NoFix)
    }

    @Test
    fun legacyPlanDropsUnlistableActionsSoApplyCannotDoMoreThanThePreview() {
        val rule = ModDoctorSetFix(listOf(ModDoctorSetMember("sodium", "sodium-0.8.14.jar", "0.8.12")), mapOf("sodium" to "0.8.12"))
        val problem = legacyProblem(setFix = rule, breaksFix = ModDoctorBreaksFix(someReplacement))

        val plan = buildLegacyRepairPreview(ModDoctorReport(listOf(problem))) as LegacyPreviewResult.Plan

        assertEquals(null, plan.preview.report.problems.single().breaksFix)
        assertNotNull(plan.preview.report.problems.single().ruleFallbackFix)
        assertEquals(listOf("a.jar"), plan.refused)
    }

    @Test
    fun localDisableIsListedAsADisable() {
        val problem = legacyProblem(type = ModDoctorProblemType.LEARNED_LOCAL_CRASH, local = ModDoctorLocalDisableFix("bad.jar"))
        val plan = buildLegacyRepairPreview(ModDoctorReport(listOf(problem))) as LegacyPreviewResult.Plan
        assertEquals(FamilyChangeKind.DISABLE, plan.preview.items.single().kind)
        assertEquals("bad.jar", plan.preview.items.single().fromFile)
    }

    @Test
    fun combinationsAreStableAndCoverAllSubsets() {
        assertEquals(listOf(emptyList<Int>()), combinations(listOf(1, 2, 3), 0))
        assertEquals(listOf(listOf(1, 2), listOf(1, 3), listOf(2, 3)), combinations(listOf(1, 2, 3), 2))
        assertTrue(combinations(listOf(1), 2).isEmpty())
    }
}

// ------------------------------------------------------------------------------------------------------------

private class FamFixture(name: String) {
    private val root: File = Files.createTempDirectory("moddoctor-family-$name").toFile()
    val modsDir: File = File(root, "mods").apply { mkdirs() }
    private val stagingDir: File = File(root, "staging").apply { mkdirs() }

    private val identities = mutableMapOf<String, InstalledMod>()
    private val versions = mutableMapOf<String, List<FamVersion>>()
    private val candidateJson = mutableMapOf<String, String>()
    private val dependencyProjects = mutableMapOf<String, String>()
    val packOwned = mutableSetOf<String>()
    val staged = mutableListOf<String>()

    fun install(
        fileName: String,
        projectId: String,
        modId: String,
        version: String,
        requires: Map<String, String> = emptyMap(),
        breaks: Map<String, String> = emptyMap(),
    ) {
        famWriteJar(modsDir, fileName, famJson(modId, version, requires, breaks))
        identities[fileName] = InstalledMod(Platform.MODRINTH, projectId, "installed-$modId-$version", version)
    }

    fun sodium0813(withDependents: Boolean = true) {
        install("sodium-0.8.13.jar", "proj-sodium", "sodium", "0.8.13", breaks = mapOf("iris" to "<1.8.13"))
    }

    fun extra094() = install(
        "sodium-extra-0.9.4.jar", "proj-extra", "sodium-extra", "0.9.4",
        requires = mapOf("sodium" to ">=0.8.13"), breaks = mapOf("iris" to "<1.7.0"),
    )

    fun reeses224() = install(
        "reeses-2.2.4.jar", "proj-reeses", "reeses-sodium-options", "2.2.4",
        requires = mapOf("sodium" to "=0.8.13"), breaks = mapOf("iris" to "<1.1.4", "sodium-extra" to "<0.5.3"),
    )

    fun iris188() = install("iris-1.8.8.jar", "proj-iris", "iris", "1.8.8", requires = mapOf("sodium" to "0.6.x"))

    fun project(projectId: String, vararg candidates: FamVersion) {
        versions[projectId] = candidates.toList()
    }

    fun candidate(projectId: String, version: String, json: String) {
        candidateJson["$projectId/$version"] = json
    }

    fun dependencyProject(slug: String, projectId: String) {
        dependencyProjects[slug] = projectId
    }

    fun planner(bounds: FamilyRepairBounds = FamilyRepairBounds()) = FamilyRepairPlanner(
        minecraftVersion = "1.21.1",
        loader = ModLoader.FABRIC,
        modsDir = modsDir,
        isPackOwned = { it in packOwned },
        installedIdentity = { identities[it] },
        projectVersions = { projectId, _ -> versions[projectId].orEmpty() },
        dependencyProject = { slug -> dependencyProjects[slug]?.let { FamProject(it, slug) } },
        stageCandidate = { candidate ->
            staged += "${candidate.platformProjectId()}/${candidate.platformVersion()}"
            candidateJson["${candidate.platformProjectId()}/${candidate.platformVersion()}"]?.let { json ->
                famWriteJar(stagingDir, "${candidate.platformId()}.jar", json)
            }
        },
        bounds = bounds,
    )

    fun snapshot(): Map<String, Long> = modsDir.listFiles().orEmpty().associate { it.name to it.length() }

    fun stagingFiles(): List<File> = stagingDir.listFiles().orEmpty().toList()

    fun cleanup() {
        root.deleteRecursively()
    }
}

private fun famVersion(projectId: String, version: String, fileName: String, prerelease: Boolean = false) =
    FamVersion("$projectId-$version", projectId, fileName, version, prerelease)

private class FamVersion(
    private val id: String,
    private val projectId: String,
    private val fileName: String,
    private val version: String,
    private val prerelease: Boolean,
) : PlatformVersion {
    override suspend fun initFile(currentProjectId: String) = true
    override fun platform() = Platform.MODRINTH
    override fun platformId() = id
    override fun platformProjectId() = projectId
    override fun platformDisplayName() = version
    override fun platformFileName() = fileName
    override fun platformGameVersion() = arrayOf("1.21.1", "1.20.1")
    override fun platformLoaders(): List<PlatformDisplayLabel> = listOf(object : PlatformDisplayLabel {
        override fun getDisplayName() = "Fabric"
        override fun index() = 0
    })
    override fun platformReleaseType() = if (prerelease) PlatformReleaseType.BETA else PlatformReleaseType.RELEASE
    override fun platformDependencies() = emptyList<PlatformVersion.PlatformDependency>()
    override fun platformDownloadCount() = 0L
    override fun platformDownloadUrl() = ""
    override fun platformDatePublished(): Instant = Instant.parse("2026-07-28T08:48:35Z")
    override fun platformSha1(): String? = null
    override fun platformFileSize() = 0L
    override fun platformVersion() = version
}

private class FamProject(private val id: String, private val slug: String) : PlatformProject {
    override fun platform() = Platform.MODRINTH
    override fun platformId() = id
    override fun platformClasses(defaultClasses: PlatformClasses) = defaultClasses
    override fun platformSlug() = slug
    override fun platformIconUrl(): String? = null
    override fun platformTitle() = slug
    override fun platformSummary(): String? = null
    override fun platformAuthor(): String? = null
    override fun platformDownloadCount() = 0L
    override fun platformFollows(): Long? = null
    override fun platformModLoaders(): List<PlatformDisplayLabel>? = null
    override fun checkClasses() = Unit
    override fun platformCategories(classes: PlatformClasses): List<PlatformFilterCode>? = null
    override fun platformUrls(defaultClasses: PlatformClasses) = PlatformProject.Urls()
    override fun platformScreenshots(): List<PlatformProject.Screenshot> = emptyList()
}

private fun famJson(
    id: String,
    version: String,
    requires: Map<String, String> = emptyMap(),
    breaks: Map<String, String> = emptyMap(),
): String {
    val members = mutableListOf("\"id\": \"$id\"", "\"version\": \"$version\"")
    if (requires.isNotEmpty()) members += "\"depends\": { ${requires.entries.joinToString(", ") { (k, v) -> "\"$k\": \"$v\"" }} }"
    if (breaks.isNotEmpty()) members += "\"breaks\": { ${breaks.entries.joinToString(", ") { (k, v) -> "\"$k\": \"$v\"" }} }"
    return "{ ${members.joinToString(", ")} }"
}

private fun famWriteJar(dir: File, fileName: String, json: String): File {
    val file = File(dir, fileName)
    ZipOutputStream(file.outputStream()).use { zip ->
        zip.putNextEntry(ZipEntry("fabric.mod.json"))
        zip.write(json.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
    return file
}
