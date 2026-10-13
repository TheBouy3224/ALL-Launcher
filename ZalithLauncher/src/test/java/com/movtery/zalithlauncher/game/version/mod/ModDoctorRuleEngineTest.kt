package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Rule Engine：多条 Known-good 规则、按 Minecraft 版本 + 加载器 + 模组 id 分开、规则可以互相重叠。
 *
 * 要点：
 * - 旧规则文件（rules_version 1）行为完全不变；
 * - Known-good 不等于“别的都不行”：KNOWN_GOOD 模式不一致时不报告、不强制换版本；
 * - 没有证据的候选规则不起作用；
 * - 重叠规则的选择是确定的，冲突时不猜。
 */
class ModDoctorRuleEngineTest {
    private fun rule(
        id: String,
        minecraft: String = "1.21.11",
        loader: String = "fabric",
        present: List<String>,
        allowed: Map<String, String>,
        mode: ModDoctorRuleMode = ModDoctorRuleMode.ENFORCE,
        verified: Boolean = true,
        priority: Int = 0,
    ) = ModDoctorSetRule(minecraft, loader, present, allowed, id = id, mode = mode, verified = verified, priority = priority)

    private val quartet = rule(
        "quartet", present = listOf("sodium", "sodium-extra", "reeses-sodium-options", "iris"),
        allowed = mapOf("sodium" to "0.8.12", "sodium-extra" to "0.9.1", "reeses-sodium-options" to "2.2.3", "iris" to "1.10.7"),
    )
    private val pair = rule("pair", present = listOf("sodium", "iris"), allowed = mapOf("sodium" to "0.8.12", "iris" to "1.10.7"))

    private fun ids(rules: List<ModDoctorSetRule>) = rules.map { it.id }

    private fun select(rules: List<ModDoctorSetRule>, installed: Set<String>, mc: String = "1.21.11", loader: String = "Fabric") =
        selectApplicableRules(rules, mc, loader, installed)

    private fun mod(id: String, version: String?) = ModDoctorMetadata(
        fileName = "$id-$version.jar", modId = id, name = id, loader = ModLoader.FABRIC, version = version,
    )

    private fun problems(rules: List<ModDoctorSetRule>, vararg mods: ModDoctorMetadata) = validateModDoctorMetadata(
        mods = mods.toList(), minecraftVersion = "1.21.11", loader = ModLoader.FABRIC, setRules = rules,
    ).problems.filter { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_SET }

    // ---- 旧 JSON 的向后兼容 ----

    private val shipped get() = parseModDoctorRules(File("src/main/assets/mod_doctor_rules.json").readText())

    @Test
    fun shippedFileKeepsTheConfirmedValuesAndIsKnownGoodNotEnforced() {
        val r = shipped.single()
        assertEquals("1.21.11", r.minecraft)
        assertEquals("fabric", r.loader)
        assertEquals(mapOf("sodium" to "0.8.12", "sodium-extra" to "0.9.1", "reeses-sodium-options" to "2.2.3", "iris" to "1.10.7"), r.allowed)
        assertEquals(listOf("sodium", "sodium-extra", "reeses-sodium-options", "iris"), r.whenPresent)
        assertTrue("the set is verified to launch", r.verified)
        assertEquals("evidence is that the set works, not that other sets fail", ModDoctorRuleMode.KNOWN_GOOD, r.mode)
        assertTrue(r.evidence.isNotBlank())
    }

    @Test
    fun unconfirmedSetsAreNotJudgedUnusableJustBecauseTheyDifferFromKnownGood() {
        //已验证的是 0.8.12 / 0.9.1 / 2.2.3 / 1.10.7；下面这些组合从未被验证为失败，也没有声明冲突
        val untried = listOf(
            arrayOf(mod("sodium", "0.8.15"), mod("sodium-extra", "0.9.4"), mod("reeses-sodium-options", "2.2.4"), mod("iris", "1.10.8")),
            arrayOf(mod("sodium", "0.8.12"), mod("sodium-extra", "0.9.4"), mod("reeses-sodium-options", "2.2.3"), mod("iris", "1.10.7")),
            arrayOf(mod("sodium", "0.9.0"), mod("sodium-extra", "0.9.1"), mod("reeses-sodium-options", "2.2.3"), mod("iris", "1.10.7")),
        )
        untried.forEach { combo ->
            assertTrue("a different set must not be reported as unusable: ${combo.map { it.fileName }}", problems(shipped, *combo).isEmpty())
        }
    }

    @Test
    fun theKnownGoodSetItselfStaysClean() {
        assertTrue(problems(shipped, mod("sodium", "0.8.12"), mod("sodium-extra", "0.9.1"), mod("reeses-sodium-options", "2.2.3"), mod("iris", "1.10.7")).isEmpty())
    }

    @Test
    fun theProvenLoaderRejectionStillBlocksThroughDeclaredBreaks() {
        //真实加载器拒绝的是这一对：Sodium 0.8.14 声明 breaks iris <=1.10.7（NEG_HARD_DEP）；它来自 jar 自己的 metadata，与 Known-good 规则无关
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(fileName = "sodium.jar", modId = "sodium", name = "Sodium", loader = ModLoader.FABRIC, version = "0.8.14+mc1.21.11",
                    breakPredicates = mapOf("iris" to listOf("<=1.10.7"))),
                ModDoctorMetadata(fileName = "iris.jar", modId = "iris", name = "Iris", loader = ModLoader.FABRIC, version = "1.10.7+mc1.21.11"),
                ModDoctorMetadata(fileName = "extra.jar", modId = "sodium-extra", name = "Sodium Extra", loader = ModLoader.FABRIC, version = "0.9.4"),
                ModDoctorMetadata(fileName = "reese.jar", modId = "reeses-sodium-options", name = "Reese", loader = ModLoader.FABRIC, version = "2.2.4"),
            ),
            minecraftVersion = "1.21.11", loader = ModLoader.FABRIC, setRules = shipped,
        )
        assertTrue(report.problems.any { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR })
        assertTrue("the known-good rule itself must not add a second, unproven verdict", report.problems.none { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_SET })
    }

    @Test
    fun versionOneFileWithoutNewFieldsIsVerifiedAndEnforced() {
        val rules = parseModDoctorRules(
            """{"rules_version":1,"compatible_sets":[{"minecraft":"1.21.11","loader":"fabric","when_present":["a","b"],"allowed":{"a":"1","b":"2"}}]}"""
        )
        assertTrue(rules.single().verified)
        assertEquals(ModDoctorRuleMode.ENFORCE, rules.single().mode)
    }

    @Test
    fun versionTwoFileNeedsExplicitVerifiedAndDefaultsToKnownGood() {
        val rules = parseModDoctorRules(
            """{"rules_version":2,"compatible_sets":[
              {"id":"a","minecraft":"1.20.1","loader":"forge","when_present":["x"],"allowed":{"x":"1"}},
              {"id":"b","minecraft":"1.20.1","loader":"forge","when_present":["y"],"allowed":{"y":"1"},"verified":true},
              {"id":"c","minecraft":"1.20.1","loader":"forge","when_present":["z"],"allowed":{"z":"1"},"verified":true,"mode":"enforce"},
              {"id":"d","minecraft":"1.20.1","loader":"forge","when_present":["w"],"allowed":{"w":"1"},"verified":true,"mode":"typo"}
            ]}"""
        ).associateBy { it.id }
        assertFalse("no evidence flag => candidate only", rules.getValue("a").verified)
        assertTrue(rules.getValue("b").verified)
        assertEquals(ModDoctorRuleMode.KNOWN_GOOD, rules.getValue("b").mode)
        assertEquals(ModDoctorRuleMode.ENFORCE, rules.getValue("c").mode)
        assertEquals("an unknown mode must never enforce", ModDoctorRuleMode.KNOWN_GOOD, rules.getValue("d").mode)
    }

    // ---- 多套规则：按 Minecraft 版本 / 加载器分开 ----

    @Test
    fun rulesAreSeparatedByMinecraftVersionAndLoader() {
        val rules = listOf(
            rule("fabric-1.21.11", present = listOf("a"), allowed = mapOf("a" to "1")),
            rule("fabric-1.20.1", minecraft = "1.20.1", present = listOf("a"), allowed = mapOf("a" to "2")),
            rule("forge-1.21.11", loader = "forge", present = listOf("a"), allowed = mapOf("a" to "3")),
            rule("neoforge-1.21.11", loader = "neoforge", present = listOf("a"), allowed = mapOf("a" to "4")),
        )
        assertEquals(listOf("fabric-1.21.11"), ids(select(rules, setOf("a")).applied))
        assertEquals(listOf("fabric-1.20.1"), ids(select(rules, setOf("a"), mc = "1.20.1").applied))
        assertEquals(listOf("forge-1.21.11"), ids(select(rules, setOf("a"), loader = "Forge").applied))
        assertEquals(listOf("neoforge-1.21.11"), ids(select(rules, setOf("a"), loader = "NeoForge").applied))
        assertTrue(select(rules, setOf("a"), mc = "1.19.4").applied.isEmpty())
    }

    @Test
    fun independentSetsOnTheSameVersionAreBothApplied() {
        val rules = listOf(
            rule("one", present = listOf("a", "b"), allowed = mapOf("a" to "1", "b" to "1")),
            rule("two", present = listOf("c", "d"), allowed = mapOf("c" to "1", "d" to "1")),
        )
        assertEquals(listOf("one", "two"), ids(select(rules, setOf("a", "b", "c", "d")).applied))
        assertEquals(listOf("one"), ids(select(rules, setOf("a", "b", "c")).applied))
    }

    // ---- 规则重叠：选择确定、不猜 ----

    @Test
    fun largerCoverageShadowsTheSmallerRule() {
        val sel = select(listOf(pair, quartet), setOf("sodium", "sodium-extra", "reeses-sodium-options", "iris"))
        assertEquals(listOf("quartet"), ids(sel.applied))
        assertEquals(listOf("pair"), ids(sel.shadowed))
        //四件套没装全时，范围小的规则自己生效
        assertEquals(listOf("pair"), ids(select(listOf(pair, quartet), setOf("sodium", "iris")).applied))
    }

    @Test
    fun sameCoverageIsDecidedByPriority() {
        val low = rule("low", present = listOf("a", "b"), allowed = mapOf("a" to "1", "b" to "1"), priority = 0)
        val high = rule("high", present = listOf("a", "b"), allowed = mapOf("a" to "2", "b" to "2"), priority = 5)
        val sel = select(listOf(low, high), setOf("a", "b"))
        assertEquals(listOf("high"), ids(sel.applied))
        assertEquals(listOf("low"), ids(sel.shadowed))
        assertTrue(sel.ambiguous.isEmpty())
    }

    @Test
    fun sameCoverageSamePriorityWithDifferentVersionsIsAmbiguousAndNeitherApplies() {
        val x = rule("x", present = listOf("a", "b"), allowed = mapOf("a" to "1", "b" to "1"))
        val y = rule("y", present = listOf("a", "b"), allowed = mapOf("a" to "2", "b" to "1"))
        val sel = select(listOf(x, y), setOf("a", "b"))
        assertTrue(sel.applied.isEmpty())
        assertEquals(listOf("x", "y"), ids(sel.ambiguous))
    }

    @Test
    fun duplicateIdenticalRulesCollapseToOne() {
        val x = rule("x", present = listOf("a", "b"), allowed = mapOf("a" to "1", "b" to "1"))
        val y = rule("y", present = listOf("a", "b"), allowed = mapOf("a" to "1+build", "b" to "1"))
        val sel = select(listOf(x, y), setOf("a", "b"))
        assertEquals(1, sel.applied.size)
        assertTrue(sel.ambiguous.isEmpty())
    }

    @Test
    fun partiallyOverlappingRulesThatDisagreeAreAmbiguous() {
        val one = rule("one", present = listOf("a", "b"), allowed = mapOf("a" to "1", "b" to "1"))
        val two = rule("two", present = listOf("b", "c"), allowed = mapOf("b" to "2", "c" to "1")) //对 b 要求不同版本
        val sel = select(listOf(one, two), setOf("a", "b", "c"))
        assertTrue(sel.applied.isEmpty())
        assertEquals(listOf("one", "two"), ids(sel.ambiguous))
        //对共有的 b 一致就可以同时生效
        val agree = rule("agree", present = listOf("b", "c"), allowed = mapOf("b" to "1", "c" to "1"))
        assertEquals(listOf("one", "agree"), ids(select(listOf(one, agree), setOf("a", "b", "c")).applied))
    }

    @Test
    fun unverifiedRulesNeverApply() {
        val candidate = rule("candidate", present = listOf("a"), allowed = mapOf("a" to "9"), verified = false)
        assertTrue(select(listOf(candidate), setOf("a")).applied.isEmpty())
        //也不能遮蔽已验证的规则
        val verified = rule("verified", present = listOf("a"), allowed = mapOf("a" to "1"))
        val big = rule("big-candidate", present = listOf("a", "b"), allowed = mapOf("a" to "9", "b" to "9"), verified = false)
        assertEquals(listOf("verified"), ids(select(listOf(verified, big), setOf("a", "b")).applied))
    }

    // ---- 信息不全：保持沉默 ----

    @Test
    fun incompleteInformationStaysSilent() {
        assertTrue(select(listOf(quartet), setOf("sodium", "iris")).applied.isEmpty()) //没装全
        assertTrue(problems(listOf(quartet), mod("sodium", "0.8.14"), mod("iris", "1.10.7")).isEmpty())
        //版本读不出来 -> 不下结论
        assertTrue(
            problems(listOf(quartet), mod("sodium", null), mod("sodium-extra", "0.9.1"), mod("reeses-sodium-options", "2.2.3"), mod("iris", "1.10.7")).isEmpty()
        )
    }

    // ---- Known-good 不等于“别的都不行” ----

    @Test
    fun knownGoodModeNeverReportsOrForcesAChange() {
        val advisory = quartet.copy(mode = ModDoctorRuleMode.KNOWN_GOOD)
        val mismatching = arrayOf(mod("sodium", "0.8.14"), mod("sodium-extra", "0.9.4"), mod("reeses-sodium-options", "2.2.3"), mod("iris", "1.10.7"))
        assertTrue("a working set that differs from a known-good set is not a problem", problems(listOf(advisory), *mismatching).isEmpty())
        //同样的安装，在 ENFORCE 规则下仍然是问题（旧行为）
        assertEquals(1, problems(listOf(quartet), *mismatching).size)
    }

    @Test
    fun enforcedRuleStillFlagsAndTargetsOnlyDifferingMembers() {
        val found = problems(listOf(quartet), mod("sodium", "0.8.14"), mod("sodium-extra", "0.9.1"), mod("reeses-sodium-options", "2.2.3"), mod("iris", "1.10.7")).single()
        assertEquals(listOf("sodium"), found.setFix!!.members.map { it.modId })
    }

    @Test
    fun overlappingEnforcedRulesProduceOnlyTheSpecificOnesProblem() {
        val all = arrayOf(mod("sodium", "0.8.14"), mod("sodium-extra", "0.9.1"), mod("reeses-sodium-options", "2.2.3"), mod("iris", "1.10.7"))
        val result = problems(listOf(pair, quartet), *all)
        assertEquals("the shadowed pair rule must not double-report", 1, result.size)
        assertEquals(setOf("sodium", "sodium-extra", "reeses-sodium-options", "iris"), result.single().setFix!!.allowed.keys)
    }

    // ---- Planner 与规则的衔接 ----

    @Test
    fun plannerIgnoresUnverifiedRulesAndMarksKnownGoodAsAdvisory() {
        val installed = listOf(
            GroupPlanInstalled("sodium", "sodium.jar", "0.8.14"),
            GroupPlanInstalled("iris", "iris.jar", "1.10.7"),
        )
        val cands = mapOf("sodium" to listOf(
            GroupPlanCandidate("sodium", "p", "mc1.21.11-0.8.12-fabric", "s.jar", setOf("1.21.11"), setOf("fabric"), emptySet(), emptySet(), releaseType = GroupPlanReleaseType.RELEASE)
        ))
        val unverified = pair.copy(verified = false)
        assertEquals(GroupPlanStatus.NO_CHANGE, planModGroupRepair("1.21.11", "fabric", installed, unverified, cands).status)

        val advisory = pair.copy(mode = ModDoctorRuleMode.KNOWN_GOOD)
        val plan = planModGroupRepair("1.21.11", "fabric", installed, advisory, cands)
        assertEquals(GroupPlanStatus.NO_CHANGE, plan.status)
        assertTrue("a known-good set is never a repair plan", plan.changes.isEmpty())
        assertTrue(plan.advisoryOnly)
        assertEquals(listOf("sodium"), plan.suggestions.map { it.modId })

        val enforced = planModGroupRepair("1.21.11", "fabric", installed, pair, cands)
        assertFalse(enforced.advisoryOnly)
    }
}
