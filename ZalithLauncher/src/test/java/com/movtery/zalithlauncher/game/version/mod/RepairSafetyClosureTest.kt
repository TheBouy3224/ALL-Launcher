package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * STEP 17：Preview 区分「问题点名的文件」与「会真正被替换的文件」；
 * 替换 / 缺失依赖在改动 mods 之前，用整组校验器检查「已保留的 jar + 全部新 jar」。
 * 全部是纯函数测试：不下载、不改任何文件。
 */
class RepairSafetyClosureTest {
    private val shippedRules = parseModDoctorRules(File("src/main/assets/mod_doctor_rules.json").readText())

    private fun id(
        name: String, version: String,
        requires: Map<String, List<String>> = emptyMap(), breaks: Map<String, List<String>> = emptyMap(),
    ) = InstalledModIdentity(id = name, version = version, requires = requires, breaks = breaks, fileName = "$name-$version.jar")

    private fun facts(
        name: String, version: String,
        requires: Map<String, List<String>> = emptyMap(), breaks: Map<String, List<String>> = emptyMap(),
    ) = FabricModFacts(fileName = "$name-$version.jar", id = name, version = version, breaks = breaks, requires = requires)

    // ---- Part A: Preview lists the plan's members, not the problem's files ----

    private fun sodium(v: String, breaksIris: Boolean = false) = ModDoctorMetadata(
        fileName = "sodium-fabric-$v+mc1.21.11.jar", modId = "sodium", name = "Sodium", loader = ModLoader.FABRIC, version = "$v+mc1.21.11",
        breakPredicates = if (breaksIris) mapOf("iris" to listOf("<=1.10.7")) else emptyMap(),
    )

    private fun needsSodium(modId: String, name: String, v: String, exact: String? = null) = ModDoctorMetadata(
        fileName = "$modId-$v+mc1.21.11.jar", modId = modId, name = name, loader = ModLoader.FABRIC, version = "$v+mc1.21.11",
        requiredDependencies = listOf("sodium"),
        requiredDependencyPredicates = mapOf("sodium" to listOf(exact ?: ">=0.8.0")),
    )

    @Test
    fun theConflictPreviewNamesTheFilesThatChangeAndNeverIris() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                sodium("0.8.14", breaksIris = true), needsSodium("sodium-extra", "Sodium Extra", "0.9.1"),
                needsSodium("iris", "Iris", "1.10.7"), needsSodium("reeses-sodium-options", "RSO", "2.2.4", exact = "=0.8.14+mc1.21.11"),
            ),
            minecraftVersion = "1.21.11", loader = ModLoader.FABRIC, setRules = shippedRules,
        )
        val pair = report.problems.single { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR }

        val changes = pair.ruleFallbackFix!!.plannedChanges()

        assertEquals(2, changes.size)
        assertTrue(changes.any { it == "sodium-fabric-0.8.14+mc1.21.11.jar → 0.8.12" })
        assertTrue(changes.any { it == "reeses-sodium-options-2.2.4+mc1.21.11.jar → 2.2.3" })
        assertFalse("Iris is named by the problem but is not changed by the plan", changes.any { it.contains("iris") })
        assertTrue("the problem still names both conflicting files", pair.fileName.contains("iris"))
    }

    @Test
    fun aPlanWithoutARuleHasNoPlannedChangesLineSoGenericPreviewsKeepTheirOwnText() {
        val genericProblem = ModDoctorProblem(ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION, "a.jar", "a", "r")

        assertTrue(genericProblem.ruleFallbackFix?.plannedChanges().orEmpty().isEmpty())
    }

    // ---- Part B: whole-set validation for direct-install paths ----

    @Test
    fun multiDependencyThatIsSafeAsAWholeIsAccepted() {
        //A 要 B 和 C；B 又要 C >=1.0。逐个看 B 会「缺 C」，整组看是安全的
        val enabled = listOf(id("a", "1.0", requires = mapOf("b" to listOf(">=1.0"), "c" to listOf(">=1.0"))))
        val b = facts("b", "1.0", requires = mapOf("c" to listOf(">=1.0")))
        val c = facts("c", "1.2")

        assertNull(planResultRejection(enabled, emptyList(), listOf(b, c)))
        assertNotNull("alone, B would leave its own requirement unmet", planResultRejection(enabled, emptyList(), listOf(b)))
    }

    @Test
    fun theUnionIsRefusedWhenOneNewDependencyBreaksTheSet() {
        val enabled = listOf(id("a", "1.0", requires = mapOf("b" to listOf(">=1.0"), "c" to listOf(">=1.0"))))
        val badB = facts("b", "1.0", requires = mapOf("c" to listOf(">=2.0")))
        val c = facts("c", "1.2")

        assertNotNull("B needs C >=2.0 but the plan brings 1.2", planResultRejection(enabled, emptyList(), listOf(badB, c)))
    }

    @Test
    fun aNewDependencyThatBreaksAnInstalledModIsRefused() {
        val enabled = listOf(id("a", "1.0", requires = mapOf("b" to listOf(">=1.0"))), id("other", "5.0"))
        val b = facts("b", "1.0", breaks = mapOf("other" to listOf("<=5.0")))

        val reason = planResultRejection(enabled, emptyList(), listOf(b))

        assertNotNull(reason)
        assertTrue(reason!!, reason.contains("breaks other"))
    }

    @Test
    fun anAlreadyBrokenUnrelatedModDoesNotBlockAnUnrelatedDependency() {
        val enabled = listOf(
            id("a", "1.0", requires = mapOf("b" to listOf(">=1.0"))),
            id("litematica", "0.26.16", requires = mapOf("malilib" to listOf(">=0.27.19"))), //修复前就缺 malilib
        )

        assertNull(planResultRejection(enabled, emptyList(), listOf(facts("b", "1.0"))))
    }

    @Test
    fun aStagedFileWhoseMetadataCannotBeReadIsNeverTreatedAsSafe() {
        val enabled = listOf(id("a", "1.0", requires = mapOf("b" to listOf(">=1.0"))))
        val unreadable = FabricModFacts(fileName = "b.jar", id = null, version = null, breaks = emptyMap(), requires = emptyMap())

        val reason = planResultRejection(enabled, emptyList(), listOf(unreadable))

        assertNotNull(reason)
        assertTrue(reason!!, reason.contains("could not be read"))
    }

    @Test
    fun wrongMinecraftVersionReplacementIsCheckedAgainstTheWholeResultingSet() {
        //X 1.0 是错误 MC 版本的旧构建；Y 要 X >=2.0（修复前就不满足）
        val enabled = listOf(id("x", "1.0"), id("y", "1.0", requires = mapOf("x" to listOf(">=2.0"))))

        assertNull("X 2.0 resolves Y's requirement", planResultRejection(enabled, listOf("x-1.0.jar"), listOf(facts("x", "2.0"))))
        assertNotNull("X 1.5 still does not satisfy Y: a different, new reason", planResultRejection(enabled, listOf("x-1.0.jar"), listOf(facts("x", "1.5"))))
    }

    @Test
    fun aReplacementThatDoesNotProvideTheSameModIsRefused() {
        val enabled = listOf(id("x", "1.0"), id("y", "1.0", requires = mapOf("x" to listOf(">=1.0"))))

        val reason = planResultRejection(enabled, listOf("x-1.0.jar"), listOf(facts("other-mod", "9.0")))

        assertNotNull("Y would lose its provider of x", reason)
    }

    @Test
    fun multipleReplacementsAreJudgedTogetherNotOneByOne() {
        //Y 旧版要 X <2.0；新 Y 要 X >=2.0。只换 X 不安全，X 与 Y 一起换才安全
        val enabled = listOf(id("x", "1.0"), id("y", "1.0", requires = mapOf("x" to listOf("<2.0"))))
        val newX = facts("x", "2.0")
        val newY = facts("y", "2.0", requires = mapOf("x" to listOf(">=2.0")))

        assertNotNull(planResultRejection(enabled, listOf("x-1.0.jar"), listOf(newX)))
        assertNull(planResultRejection(enabled, listOf("x-1.0.jar", "y-1.0.jar"), listOf(newX, newY)))
    }

    @Test
    fun refusalHappensBeforeAnyFileIsTouched() {
        //纯函数：不接收也不修改任何文件；调用方只在返回 null 之后才安装
        val dir = java.nio.file.Files.createTempDirectory("closure").toFile()
        val file = File(dir, "x-1.0.jar").apply { writeBytes(ByteArray(64) { it.toByte() }) }
        val before = file.readBytes().toList()

        planResultRejection(listOf(id("x", "1.0"), id("y", "1.0", requires = mapOf("x" to listOf("<2.0")))), listOf("x-1.0.jar"), listOf(facts("x", "2.0")))

        assertEquals(before, file.readBytes().toList())
        dir.deleteRecursively()
    }
}
