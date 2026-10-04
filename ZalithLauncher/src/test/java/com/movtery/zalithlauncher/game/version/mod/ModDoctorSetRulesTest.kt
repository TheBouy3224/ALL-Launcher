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
 * 已知兼容成套规则的回归。
 *
 * 模组自己声明的 depends/breaks 覆盖不了的一件事：一组模组各自的声明都能被满足，版本却仍然不是
 * 加载器接受的那一套。这里的四件套版本来自真实发布记录（详见 assets/mod_doctor_rules.json）。
 */
class ModDoctorSetRulesTest {
    private val quartetRule = ModDoctorSetRule(
        minecraft = "1.21.11",
        loader = "fabric",
        whenPresent = listOf("sodium", "sodium-extra", "reeses-sodium-options", "iris"),
        allowed = mapOf(
            "sodium" to "0.8.12",
            "sodium-extra" to "0.9.1",
            "reeses-sodium-options" to "2.2.3",
            "iris" to "1.10.7",
        ),
    )

    private fun mod(id: String, version: String?) = ModDoctorMetadata(
        fileName = "$id-$version.jar",
        modId = id,
        name = id,
        loader = ModLoader.FABRIC,
        version = version,
    )

    private fun report(vararg mods: ModDoctorMetadata) = validateModDoctorMetadata(
        mods = mods.toList(),
        minecraftVersion = "1.21.11",
        loader = ModLoader.FABRIC,
        setRules = listOf(quartetRule),
    )

    @Test
    fun shippedRuleFileParses() {
        val asset = File("src/main/assets/mod_doctor_rules.json")
        assertTrue("the shipped rule file is missing: ${asset.absolutePath}", asset.isFile)

        val rules = parseModDoctorRules(asset.readText())

        assertEquals(1, rules.size)
        val rule = rules.single()
        assertEquals("1.21.11", rule.minecraft)
        assertEquals("fabric", rule.loader)
        assertEquals(
            setOf("sodium", "sodium-extra", "reeses-sodium-options", "iris"),
            rule.whenPresent.toSet(),
        )
        assertEquals("0.8.12", rule.allowed["sodium"])
        assertEquals("0.9.1", rule.allowed["sodium-extra"])
        assertEquals("2.2.3", rule.allowed["reeses-sodium-options"])
        assertEquals("1.10.7", rule.allowed["iris"])
    }

    @Test
    fun unknownRuleShapeParsesToNothing() {
        assertEquals(emptyList<ModDoctorSetRule>(), parseModDoctorRules("not json at all"))
        assertEquals(emptyList<ModDoctorSetRule>(), parseModDoctorRules("""{"rules_version":1}"""))
    }

    @Test
    fun ruleIsInactiveWhenACompanionIsAbsent() {
        val onlySodium = report(mod("sodium", "0.8.14+mc1.21.11"))
        assertTrue("Sodium alone must not be flagged", onlySodium.isClean)

        val sodiumAndIris = report(mod("sodium", "0.8.14+mc1.21.11"), mod("iris", "1.10.7+mc1.21.11"))
        assertTrue("a missing companion keeps the rule silent", sodiumAndIris.isClean)
    }

    @Test
    fun allowedQuartetPasses() {
        val quartet = report(
            mod("sodium", "0.8.12+mc1.21.11"),
            mod("sodium-extra", "0.9.1+mc1.21.11"),
            mod("reeses-sodium-options", "2.2.3+mc1.21.11"),
            mod("iris", "1.10.7+mc1.21.11"),
        )
        assertTrue(quartet.isClean)
    }

    @Test
    fun incompatibleQuartetReportsOneProblem() {
        val broken = report(
            mod("sodium", "0.8.14+mc1.21.11"),
            mod("sodium-extra", "0.9.4+mc1.21.11"),
            mod("reeses-sodium-options", "2.2.4+mc1.21.11"),
            mod("iris", "1.10.7+mc1.21.11"),
        )

        val setProblems = broken.problems.filter { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_SET }
        assertEquals("one problem for the whole set, not one per pair", 1, setProblems.size)
        assertNotNull(setProblems.single().setFix)
    }

    @Test
    fun fixTargetsOnlyTheMembersThatDiffer() {
        val partlyCorrect = report(
            mod("sodium", "0.8.14+mc1.21.11"),
            mod("sodium-extra", "0.9.1+mc1.21.11"),
            mod("reeses-sodium-options", "2.2.3+mc1.21.11"),
            mod("iris", "1.10.7+mc1.21.11"),
        )

        val fix = partlyCorrect.problems.single { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_SET }.setFix
        assertNotNull(fix)
        // only Sodium is off; the three members already on the allowed version must not be touched
        assertEquals(listOf("sodium"), fix!!.members.map { it.modId })
        assertEquals("0.8.12", fix.members.single().targetVersion)
        assertEquals("sodium-0.8.14+mc1.21.11.jar", fix.members.single().fileName)
    }

    @Test
    fun ruleIgnoresAnotherMinecraftVersionOrLoader() {
        val otherMinecraft = validateModDoctorMetadata(
            mods = listOf(mod("sodium", "0.8.14"), mod("iris", "1.10.7")),
            minecraftVersion = "1.21.1",
            loader = ModLoader.FABRIC,
            setRules = listOf(quartetRule),
        )
        assertTrue(otherMinecraft.isClean)

        // Quilt 能装 Fabric 模组，所以这里只剩「规则只适用于它自己那个加载器」这一件事
        val otherLoader = validateModDoctorMetadata(
            mods = listOf(mod("sodium", "0.8.14"), mod("iris", "1.10.7")),
            minecraftVersion = "1.21.11",
            loader = ModLoader.QUILT,
            setRules = listOf(quartetRule),
        )
        assertTrue(otherLoader.isClean)
    }

    @Test
    fun unreadableVersionStaysSilent() {
        // 版本读不出来就不下结论：与项目里其它版本判定同一条规矩
        val unreadable = report(
            mod("sodium", null),
            mod("sodium-extra", "0.9.1"),
            mod("reeses-sodium-options", "2.2.3"),
            mod("iris", "1.10.7"),
        )
        assertFalse(unreadable.problems.any { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_SET })
    }

    @Test
    fun platformVersionStringsAreRecognisedInBothShapes() {
        // 模组 jar 自己写的版本号
        assertTrue("0.8.12+mc1.21.11".matchesModVersion("0.8.12"))
        // Modrinth 给 Sodium 这个项目实际使用的版本号写法
        assertTrue("mc1.21.11-0.8.12-fabric".matchesModVersion("0.8.12"))
        // 其它版本不能被认成目标版本
        assertFalse("0.8.120".matchesModVersion("0.8.12"))
        assertFalse("mc1.21.11-0.8.13-fabric".matchesModVersion("0.8.12"))
        assertFalse("0.9.1+mc1.21.11".matchesModVersion("0.8.12"))
        assertFalse("anything".matchesModVersion(""))
    }

    @Test
    fun buildMetadataIsIgnoredWhenComparing() {
        val withBuild = report(
            mod("sodium", "0.8.12+mc1.21.11"),
            mod("sodium-extra", "0.9.1+mc1.21.11"),
            mod("reeses-sodium-options", "2.2.3+mc1.21.11"),
            mod("iris", "1.10.7+mc1.21.11"),
        )
        assertTrue("+build metadata must not count as a different version", withBuild.isClean)
        assertNull(withBuild.problems.firstOrNull { it.setFix != null })
        assertEquals("0.8.12", "0.8.12+mc1.21.11".baseModVersion())
    }
}
