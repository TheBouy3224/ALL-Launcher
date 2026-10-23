package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Sodium / Sodium Extra / Iris / Reese's Sodium Options，使用项目里已经发布的规则文件
 * （assets/mod_doctor_rules.json，known_good）与 jar 自己声明的 depends/breaks，不新增规则：
 * - 只检查真实存在的成员；不要求四个都装
 * - 已验证组合（known_good）只是“可参考”，版本不同不构成问题，也不会被强制降级
 * - 真正会被加载器拒绝的情况（Sodium 0.8.14 声明 breaks Iris <=1.10.7、缺少必需依赖）仍然照常报告
 */
class SodiumQuartetSubsetTest {
    private val shippedRules = parseModDoctorRules(File("src/main/assets/mod_doctor_rules.json").readText())

    private fun sodium(version: String, breaksIris: Boolean = false) = ModDoctorMetadata(
        fileName = "sodium-fabric-$version+mc1.21.11.jar", modId = "sodium", name = "Sodium", loader = ModLoader.FABRIC,
        version = "$version+mc1.21.11",
        breakPredicates = if (breaksIris) mapOf("iris" to listOf("<=1.10.7")) else emptyMap(),
    )

    private fun needsSodium(id: String, name: String, version: String) = ModDoctorMetadata(
        fileName = "$id-$version+mc1.21.11.jar", modId = id, name = name, loader = ModLoader.FABRIC,
        version = "$version+mc1.21.11",
        requiredDependencies = listOf("sodium"),
        requiredDependencyPredicates = mapOf("sodium" to listOf(">=0.8.0")),
    )

    private fun extra(v: String = "0.9.1") = needsSodium("sodium-extra", "Sodium Extra", v)
    private fun iris(v: String = "1.10.7") = needsSodium("iris", "Iris", v)
    private fun rso(v: String = "2.2.3") = needsSodium("reeses-sodium-options", "Reese's Sodium Options", v)

    private fun report(vararg mods: ModDoctorMetadata) = validateModDoctorMetadata(
        mods = mods.toList(), minecraftVersion = "1.21.11", loader = ModLoader.FABRIC, setRules = shippedRules,
    )

    private fun ModDoctorReport.types() = problems.map { it.type }

    @Test
    fun theShippedRuleIsKnownGoodAndNeverEnforces() {
        assertEquals(ModDoctorRuleMode.KNOWN_GOOD, shippedRules.single().mode)
    }

    @Test
    fun sodiumAloneAndEverySubsetThatWorksAreClean() {
        assertTrue(report(sodium("0.8.14")).isClean)
        assertTrue(report(sodium("0.8.14"), extra("0.9.4")).isClean)
        assertTrue(report(sodium("0.8.14"), rso("2.2.4")).isClean)
        assertTrue(report(sodium("0.8.14"), extra("0.9.4"), rso("2.2.4")).isClean)
        assertTrue(report(sodium("0.8.12"), iris("1.10.7")).isClean)
        assertTrue(report(sodium("0.8.12"), iris("1.10.7"), extra()).isClean)
        assertTrue(report(sodium("0.8.12"), iris("1.10.7"), rso()).isClean)
    }

    @Test
    fun irisAloneOnlyReportsItsRealMissingDependencyNeverASetOrPairProblem() {
        val r = report(iris())

        assertEquals(listOf(ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY), r.types())
        assertEquals("sodium", r.problems.single().requiredDependencyId)
    }

    @Test
    fun differentVersionsOfAFullQuartetAreNotAProblemAndNothingIsForcedToDowngrade() {
        //与已验证组合不同，但 jar 自己声明的约束都满足（Iris 1.10.8 不在 Sodium 0.8.14 的 breaks 范围内）
        val r = report(sodium("0.8.14", breaksIris = true), extra("0.9.4"), rso("2.2.4"), iris("1.10.8"))

        assertTrue("got ${r.problems}", r.isClean)
        assertTrue(r.problems.none { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_SET })
    }

    @Test
    fun theRealDeclaredConflictIsStillReportedAsAPairWithTheExistingRepairNotAQuartetRule() {
        val r = report(sodium("0.8.14", breaksIris = true), iris("1.10.7"), extra("0.9.4"))

        val pair = r.problems.single { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR }
        assertEquals("sodium-fabric-0.8.14+mc1.21.11.jar", pair.breaksSourceFileName)
        assertEquals("iris-1.10.7+mc1.21.11.jar", pair.breaksTargetFileName)
        assertTrue("no quartet rule problem", r.problems.none { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_SET })
        //Sodium Extra 与本冲突无关：不在问题涉及的文件里
        assertTrue(pair.fileName.split(", ").none { it.startsWith("sodium-extra") })
    }

    @Test
    fun aMissingSodiumForExtraOrRsoIsTheOnlyProblemReported() {
        val r = report(extra(), rso())

        assertEquals(2, r.problems.size)
        assertTrue(r.problems.all { it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY && it.requiredDependencyId == "sodium" })
    }

    @Test
    fun unreadableVersionsStaySilent() {
        val r = report(sodium("0.8.14").copy(version = null), extra().copy(version = null), iris().copy(version = null))

        assertTrue("got ${r.problems}", r.isClean)
    }

    // ---- Reese's Sodium Options 2.2.4: real metadata (cached jar sha1 295623b7...) ----
    //   depends: sodium =0.8.14+mc1.21.11   breaks: iris <1.1.4, sodium-extra <0.5.3, sodium <0.5.8
    private fun rso224() = needsSodium("reeses-sodium-options", "Reese's Sodium Options", "2.2.4")
        .copy(requiredDependencyPredicates = mapOf("sodium" to listOf("=0.8.14+mc1.21.11")))

    @Test
    fun rso224WithSodium0812IsOneRealProblemAndTheRuleOffersTheFix() {
        val r = report(sodium("0.8.12"), extra("0.9.1"), iris("1.10.7"), rso224())

        val problem = r.problems.single()
        assertEquals(ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION, problem.type)
        assertEquals("reeses-sodium-options", problem.modId)
        val fallback = problem.ruleFallbackFix
        org.junit.Assert.assertNotNull("the published known-good rule must offer a fix", fallback)
        //只换与规则不一致的成员：Reese's 2.2.4 -> 2.2.3；Sodium / Extra / Iris 已在规则版本上，不动
        assertEquals(listOf("reeses-sodium-options"), fallback!!.members.map { it.modId })
        assertEquals("2.2.3", fallback.members.single().targetVersion)
        assertTrue(r.hasFixableProblems)
    }

    @Test
    fun rso224WithItsExactSodiumAndNoIrisIsClean() {
        assertTrue(report(sodium("0.8.14"), rso224()).isClean)
        assertTrue(report(sodium("0.8.14"), extra("0.9.4"), rso224()).isClean)
    }

    @Test
    fun noProblemMeansNoFallbackIsAttached() {
        val r = report(sodium("0.8.12"), extra("0.9.1"), iris("1.10.7"), rso("2.2.3"))

        assertTrue(r.isClean)
    }

    @Test
    fun aRuleThatDoesNotNameTheModsNeverOffersAFallback() {
        //Reese's 单独 + 非 0.8.14 的 Sodium，但没有规则点名全部成员（缺 Iris）：问题照报，但不凭空给后备
        val r = report(sodium("0.8.12"), rso224())

        val problem = r.problems.single()
        assertEquals(ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION, problem.type)
        org.junit.Assert.assertNull(problem.ruleFallbackFix)
    }
}
