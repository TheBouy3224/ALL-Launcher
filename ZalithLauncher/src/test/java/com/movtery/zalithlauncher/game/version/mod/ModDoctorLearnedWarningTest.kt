package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 本机学到的崩溃警告：修好过一次的事实，必须在**下一次同样的崩溃发生之前**被说出来。
 *
 * 全部走生产代码的纯接缝（学习记录 + 模组目录 → 问题列表）：记录是构造出来的数据，
 * 模组目录是临时目录里的真 jar。不碰 MMKV、不碰网络。
 *
 * 记录的形状逐字取自 2026-09-24 真机 store 里的那一条：
 * `{"action":"disable","actionLogStamp":"52373:1790265461000","actionPreState":"Axiom-6.0.5-for-MC1.21.11.jar",
 *   ...,"instanceName":"1.21.11 Fabric 0.19.5","loader":"Fabric","markerSeenAfterFix":true,
 *   "minecraftVersion":"1.21.11","modSetFingerprint":"axiom@6.0.5,fabric-api@0.141.6+1.21.11,xaeroworldmap@1.46.0",
 *   "ownerModId":"axiom","signatureId":"platform_limit_native","state":"CANDIDATE"}`
 */
class ModDoctorLearnedWarningTest {
    private val instanceName = "1.21.11 Fabric 0.19.5"
    private val minecraftVersion = "1.21.11"
    private val ownerFileName = "Axiom-6.0.5-for-MC1.21.11.jar"

    private fun writeModJar(modsDir: File, fileName: String, id: String, version: String, name: String = id) {
        ZipOutputStream(File(modsDir, fileName).outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("fabric.mod.json"))
            zip.write(
                ("{\"schemaVersion\":1,\"id\":\"$id\",\"version\":\"$version\",\"name\":\"$name\"," +
                    "\"depends\":{\"minecraft\":\"*\"}}").toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()
        }
    }

    /** 真 jar 写进临时 mods 目录，再用生产读取路径读回来；启用/禁用由文件名决定 */
    private fun inspectLocals(vararg jars: Pair<String, String>): Pair<File, List<LocalMod>> {
        val modsDir = Files.createTempDirectory("learned-warning-mods").toFile()
        jars.forEach { (fileName, id) -> writeModJar(modsDir, fileName, id, "6.0.5") }
        val locals = runBlocking { AllModReader(modsDir).readAllLocals() }
        return modsDir to locals
    }

    private fun record(
        ownerModId: String? = "axiom",
        instance: String = instanceName,
        minecraft: String = minecraftVersion,
        loader: String = "Fabric",
        signatureId: String = "platform_limit_native",
        actionPreState: String = ownerFileName,
        markerSeenAfterFix: Boolean = true,
        crashMarkerSeenAfterFix: Boolean = false,
        count: Int = 1,
        state: LearningState = LearningState.CANDIDATE,
        inconclusive: Boolean = false,
        modSetFingerprint: String = "axiom@6.0.5,fabric-api@0.141.6,xaeroworldmap@1.46.0",
    ) = LearningRecord(
        instanceName = instance,
        logPath = "/storage/emulated/0/.minecraft/versions/$instance/ALLLauncher/latest_game.log",
        minecraftVersion = minecraft,
        loader = loader,
        modSetFingerprint = modSetFingerprint,
        signatureId = signatureId,
        outcomeClass = LaunchOutcome.CRASHED_UNEXPECTEDLY.name,
        ownerModId = ownerModId,
        diagnosisType = "platform_limit",
        confidence = "HIGH",
        action = "disable",
        actionPreState = actionPreState,
        actionLogStamp = "52373:1790265461000",
        markerSeenAfterFix = markerSeenAfterFix,
        crashMarkerSeenAfterFix = crashMarkerSeenAfterFix,
        count = count,
        state = state,
        inconclusive = inconclusive,
    )

    private fun problems(
        records: List<LearningRecord>,
        locals: List<LocalMod>,
        loader: ModLoader = ModLoader.FABRIC,
    ) = learnedLocalCrashProblems(records, instanceName, minecraftVersion, loader, locals)

    /** 所有者就在目录里、文件是启用的、记录证明这次修复成功过：一条可修复的问题 */
    @Test
    fun aProvenLocalFixWarnsBeforeTheSameCrashRepeats() {
        val (modsDir, locals) = inspectLocals(ownerFileName to "axiom")
        try {
            val report = ModDoctorReport(
                problems(listOf(record(state = LearningState.CONFIRMED_LOCAL)), locals)
            )

            val problem = report.problems.single()
            assertEquals(ModDoctorProblemType.LEARNED_LOCAL_CRASH, problem.type)
            assertEquals(ownerFileName, problem.fileName)
            assertEquals("axiom", problem.modId)
            assertEquals(ownerFileName, problem.localDisableFix?.fileName)
            assertEquals(listOf("axiom"), problem.reasonArgs)
            assertEquals(
                com.movtery.zalithlauncher.R.string.mod_doctor_reason_local_crash_confirmed,
                problem.reasonRes,
            )
            assertTrue(problem.reason.startsWith("axiom crashed this instance before"))
            assertTrue("a learned local fact is not a general incompatibility claim", problem.reason.contains("does not claim"))
            assertNull("a local disable never resolves a provider replacement", problem.replacementFix)
            assertNull(problem.fixCandidate)
            assertNull(problem.setFix)
            assertNull(problem.breaksFix)
            assertNull(problem.exactRestoreFile)

            assertTrue(report.hasFixableProblems)
            assertEquals(LaunchGateDecision.BLOCK, decideLaunchGate(report))
        } finally {
            modsDir.deleteRecursively()
        }
    }

    /**
     * 修复成功之后 owner 本来就是禁用的：这时必须什么都不产出。
     *
     * 一条提示都不行——提示会让 pre-launch gate 弹出没有取消按钮的对话框，而这是修复成功之后
     * 的每一次启动。
     */
    @Test
    fun aDisabledOwnerWarnsNothingAtAll() {
        val (modsDir, locals) = inspectLocals("$ownerFileName.disabled" to "axiom")
        try {
            val report = ModDoctorReport(problems(listOf(record()), locals))

            assertTrue("no problem: the fix is already in place", report.problems.isEmpty())
            assertTrue("and no advisory: an advisory would block every launch", report.advisories.isEmpty())
            assertTrue(report.isClean)
            assertFalse(report.hasFixableProblems)
            assertEquals(LaunchGateDecision.LAUNCH, decideLaunchGate(report))
        } finally {
            modsDir.deleteRecursively()
        }
    }

    @Test
    fun anAbsentOwnerWarnsNothingAtAll() {
        val (modsDir, locals) = inspectLocals("sodium-0.8.12.jar" to "sodium")
        try {
            val report = ModDoctorReport(problems(listOf(record()), locals))

            assertTrue(report.problems.isEmpty())
            assertTrue(report.advisories.isEmpty())
            assertEquals(LaunchGateDecision.LAUNCH, decideLaunchGate(report))
        } finally {
            modsDir.deleteRecursively()
        }
    }

    @Test
    fun anotherInstanceVersionOrLoaderDoesNotMatch() {
        val (modsDir, locals) = inspectLocals(ownerFileName to "axiom")
        try {
            assertTrue(problems(listOf(record(instance = "1.21.10 Fabric 0.19.5")), locals).isEmpty())
            assertTrue(problems(listOf(record(minecraft = "1.21.10")), locals).isEmpty())
            assertTrue(problems(listOf(record(loader = "NeoForge")), locals).isEmpty())
        } finally {
            modsDir.deleteRecursively()
        }
    }

    /** 记录里存的是日志文本的写法 `Fabric`，实例侧是 `FABRIC`；实例名同样按大小写不敏感比较 */
    @Test
    fun theLoaderAndTheInstanceNameAreComparedCaseInsensitively() {
        val (modsDir, locals) = inspectLocals(ownerFileName to "axiom")
        try {
            assertEquals(1, problems(listOf(record(loader = "fabric")), locals).size)
            assertEquals(1, problems(listOf(record(instance = instanceName.uppercase())), locals).size)
        } finally {
            modsDir.deleteRecursively()
        }
    }

    /** 没有动过文件（actionPreState 为空）就不是一次修复：不警告 */
    @Test
    fun aRecordWithoutAnActionDoesNotWarn() {
        val (modsDir, locals) = inspectLocals(ownerFileName to "axiom")
        try {
            assertTrue(problems(listOf(record(actionPreState = "")), locals).isEmpty())
        } finally {
            modsDir.deleteRecursively()
        }
    }

    /**
     * 不点名任何模组的记录不警告。LOW 可信度的记录就是这一类：分类器不允许 LOW 点位模组，
     * 所以「没有 owner」同时覆盖了 A10 里的两种情形。
     */
    @Test
    fun aRecordWithoutAnOwnerDoesNotWarn() {
        val (modsDir, locals) = inspectLocals(ownerFileName to "axiom")
        try {
            assertTrue(problems(listOf(record(ownerModId = null)), locals).isEmpty())
        } finally {
            modsDir.deleteRecursively()
        }
    }

    /** 修复之后仍然崩了：这次修复是失败的，永远不能拿来警告 */
    @Test
    fun aRecordWhoseFixFailedDoesNotWarn() {
        val (modsDir, locals) = inspectLocals(ownerFileName to "axiom")
        try {
            assertTrue(
                problems(listOf(record(markerSeenAfterFix = true, crashMarkerSeenAfterFix = true)), locals).isEmpty()
            )
            assertTrue(problems(listOf(record(markerSeenAfterFix = false)), locals).isEmpty())
        } finally {
            modsDir.deleteRecursively()
        }
    }

    @Test
    fun anInconclusiveRecordDoesNotWarn() {
        val (modsDir, locals) = inspectLocals(ownerFileName to "axiom")
        try {
            assertTrue(
                problems(listOf(record(count = 3, inconclusive = true)), locals).isEmpty()
            )
        } finally {
            modsDir.deleteRecursively()
        }
    }

    /**
     * 要害用例：模组被禁用之后重新启用，记录里存的模组集指纹里已经没有它了。
     *
     * 消费者按记录自己的身份字段匹配，**不**用当前指纹算键——否则这条记录永远不可达，
     * 也正是真机那条 CANDIDATE 记录今天还能被消费的原因。
     */
    @Test
    fun aStoredFingerprintThatNoLongerContainsTheModStillWarns() {
        val (modsDir, locals) = inspectLocals(ownerFileName to "axiom")
        try {
            val staleFingerprint = record(modSetFingerprint = "fabric-api@0.141.6+1.21.11,sodium@0.8.12")

            assertEquals(1, problems(listOf(staleFingerprint), locals).size)
        } finally {
            modsDir.deleteRecursively()
        }
    }

    /** 同一个 owner 的多条记录只警告一次：留下最好的那条（已确认优先，其次命中次数多的） */
    @Test
    fun severalRecordsForOneOwnerWarnOnce() {
        val (modsDir, locals) = inspectLocals(ownerFileName to "axiom")
        try {
            val confirmed = record(signatureId = "platform_limit_native", count = 1, state = LearningState.CONFIRMED_LOCAL)
            val oftenSeen = record(signatureId = "mixin_apply_failed", count = 7)
            val onceSeen = record(signatureId = "loader_fatal_unattributed", count = 2)

            val chosen = problems(listOf(oftenSeen, onceSeen, confirmed), locals)
            assertEquals(1, chosen.size)
            assertEquals(
                "the confirmed record wins over the higher count",
                com.movtery.zalithlauncher.R.string.mod_doctor_reason_local_crash_confirmed,
                chosen.single().reasonRes,
            )

            val byCount = learnedWarningCandidates(
                listOf(oftenSeen, onceSeen),
                instanceName,
                minecraftVersion,
                ModLoader.FABRIC,
            )
            assertEquals("mixin_apply_failed", byCount.single().signatureId)
        } finally {
            modsDir.deleteRecursively()
        }
    }

    /** 整份列表的上限：四个 owner 只警告三个 */
    @Test
    fun moreThanThreeOwnersAreCapped() {
        val (modsDir, locals) = inspectLocals(
            ownerFileName to "axiom",
            "balm-21.10.11.jar" to "balm",
            "sodium-0.8.12.jar" to "sodium",
            "xaero-1.46.0.jar" to "xaeroworldmap",
        )
        try {
            val records = listOf(
                record(ownerModId = "axiom", signatureId = "sig_a"),
                record(ownerModId = "balm", signatureId = "sig_b"),
                record(ownerModId = "sodium", signatureId = "sig_c"),
                record(ownerModId = "xaeroworldmap", signatureId = "sig_d"),
            )

            val warnings = problems(records, locals)

            assertEquals(LEARNED_WARNING_LIMIT, warnings.size)
            assertEquals(
                setOf("sig_a", "sig_b", "sig_c"),
                learnedWarningCandidates(records, instanceName, minecraftVersion, ModLoader.FABRIC)
                    .map { it.signatureId }
                    .toSet(),
            )
        } finally {
            modsDir.deleteRecursively()
        }
    }

    /** 整合包钉住的文件：本机学到的禁用同样不许自动执行（ALL 不碰包内文件） */
    @Test
    fun aPackOwnedLearnedWarningIsNotApplied() {
        val (modsDir, locals) = inspectLocals(ownerFileName to "axiom")
        try {
            val report = ModDoctorReport(problems(listOf(record()), locals))
            val policy = ModpackPolicy(
                ModpackManifest(
                    name = "Test Pack",
                    mods = listOf(PackOwnedMod(name = ownerFileName, requiredCore = true)),
                )
            )

            val filtered = report.applyPackSafeMode(policy, modsDir)

            assertNull(filtered.problems.single().localDisableFix)
            assertFalse(filtered.hasFixableProblems)
            assertTrue(filtered.problems.single().reason.contains("managed by the modpack"))
            assertEquals(ModDoctorProblemType.LEARNED_LOCAL_CRASH, filtered.problems.single().type)
        } finally {
            modsDir.deleteRecursively()
        }
    }
}
