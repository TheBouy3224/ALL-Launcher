package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

/**
 * STEP 19 事务安全（F1–F4）：真实文件 + 真实 [ModDoctorMultiModRepair.apply] + 真实 [ModRepairSnapshots]。
 * 不 mock 回滚；只有「平台版本」是桩（apply 只读它的文件名与 sha1）。
 */
class TransactionSafetyTest {
    private lateinit var root: File
    private lateinit var versionPath: File
    private lateinit var modsDir: File
    private lateinit var stagingDir: File

    @Before
    fun setUp() {
        root = Files.createTempDirectory("txn-safety").toFile()
        versionPath = File(root, "version").also { it.mkdirs() }
        modsDir = File(versionPath, "mods").also { it.mkdirs() }
        stagingDir = File(root, "staging").also { it.mkdirs() }
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun sha1(f: File): String = MessageDigest.getInstance("SHA-1").digest(f.readBytes()).joinToString("") { "%02x".format(it) }
    private fun hashes(): Map<String, String> = modsDir.listFiles().orEmpty().filter { it.isFile }.associate { it.name to sha1(it) }

    private fun jar(dir: File, name: String, id: String, version: String, depends: String = "{}"): File {
        val file = File(dir, name)
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("fabric.mod.json"))
            zip.write("""{"schemaVersion":1,"id":"$id","version":"$version","depends":$depends}""".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("filler.bin"))
            zip.write(Random(name.hashCode()).nextBytes(2048))
            zip.closeEntry()
        }
        return file
    }

    private fun executor() = ModDoctorMultiModRepair(
        minecraftVersion = "1.21.11", loader = ModLoader.FABRIC, modsDir = modsDir,
        installedIdentity = { null }, projectVersions = { _, _ -> emptyList() }, stageCandidate = { null },
    )

    private fun entry(fileName: String, staged: File, platformSha1: String? = sha1(staged), id: String = "x", version: String = "1.0") =
        ModDoctorPlannedReplacement(
            fileName = fileName,
            target = StubVersion(staged.name, platformSha1),
            stagedFile = staged,
            facts = FabricModFacts(fileName = staged.name, id = id, version = version, breaks = emptyMap(), requires = emptyMap()),
        )

    // ---- F1: the file that was validated is the file that is installed, and it is re-proven right before ----

    @Test
    fun aPureAdditionInstallsTheStagedFileItselfWithItsSha1() = runBlocking {
        val staged = jar(stagingDir, "malilib-0.27.20.jar", "malilib", "0.27.20")
        val before = hashes()

        val outcome = executor().apply(ModDoctorRepairPlan(listOf(entry("", staged, id = "malilib"))))

        assertTrue(outcome is ModDoctorRepairOutcome.Repaired)
        assertEquals(sha1(staged), sha1(File(modsDir, "malilib-0.27.20.jar")))
        assertEquals("only the new file was added", before.keys + "malilib-0.27.20.jar", hashes().keys)
    }

    @Test
    fun anAdditionPlusAReplacementIsOneTransaction() = runBlocking {
        val original = File(modsDir, "old-1.0.jar").apply { writeBytes(Random(5).nextBytes(512)) }
        val originalHash = sha1(original)
        val newMain = jar(stagingDir, "new-2.0.jar", "x", "2.0")
        val dep = jar(stagingDir, "dep-1.0.jar", "dep", "1.0")

        val outcome = executor().apply(ModDoctorRepairPlan(listOf(entry("old-1.0.jar", newMain), entry("", dep, id = "dep"))))

        assertTrue(outcome is ModDoctorRepairOutcome.Repaired)
        assertEquals("the original is kept, disabled", originalHash, sha1(File(modsDir, "old-1.0.jar.disabled")))
        assertTrue(File(modsDir, "new-2.0.jar").isFile && File(modsDir, "dep-1.0.jar").isFile)
    }

    @Test
    fun aHashMismatchOnTheSecondFileRollsEverythingBackAndNothingIsLeftBehind() = runBlocking {
        File(modsDir, "old-1.0.jar").writeBytes(Random(5).nextBytes(512))
        val before = hashes()
        val good = jar(stagingDir, "new-2.0.jar", "x", "2.0")
        val dep = jar(stagingDir, "dep-1.0.jar", "dep", "1.0")

        val outcome = executor().apply(
            ModDoctorRepairPlan(listOf(entry("old-1.0.jar", good), entry("", dep, platformSha1 = "0".repeat(40), id = "dep")))
        )

        assertTrue("a sha1 mismatch is a failure", outcome is ModDoctorRepairOutcome.Failed)
        assertEquals("every original is back and no new file remains (all or nothing)", before, hashes())
        assertFalse(File(modsDir, "new-2.0.jar").exists())
        assertFalse(File(modsDir, "dep-1.0.jar").exists())
    }

    @Test
    fun aStagedFileThatChangesAfterValidationIsRefusedBeforeAnyInstall() = runBlocking {
        val staged = jar(stagingDir, "dep-1.0.jar", "dep", "1.0")
        val validatedSha1 = sha1(staged)
        val validatedFacts = FabricModFacts(staged.name, "dep", "1.0", emptyMap(), emptyMap())

        assertNull("untouched file passes", stagedFileMismatch(staged, validatedSha1, validatedFacts))

        //同路径被换成另一个模组（内容与元数据都变了）
        jar(stagingDir, "dep-1.0.jar", "evil", "9.9")
        val reason = stagedFileMismatch(staged, validatedSha1, validatedFacts)
        assertNotNull(reason)
        assertTrue(reason!!, reason.contains("SHA-1 changed"))
    }

    @Test
    fun sameBytesButUnreadableOrMissingIsNeverTreatedAsVerified() = runBlocking {
        val missing = File(stagingDir, "gone.jar")
        assertNotNull(stagedFileMismatch(missing, "0".repeat(40), FabricModFacts("gone.jar", "x", "1", emptyMap(), emptyMap())))

        val notAJar = File(stagingDir, "junk.jar").apply { writeBytes(ByteArray(64) { 7 }) }
        val reason = stagedFileMismatch(notAJar, sha1(notAJar), FabricModFacts("junk.jar", "x", "1", emptyMap(), emptyMap()))
        assertNotNull("the bytes match but the metadata cannot be proven", reason)
    }

    // ---- F3 / F4: failure with a PARTIAL snapshot is rolled back for real and reported truthfully ----

    private fun partialSnapshot(): RepairSnapshot {
        File(modsDir, "orig-a.jar").writeBytes(Random(1).nextBytes(1024))
        File(modsDir, "keep.jar").writeBytes(Random(2).nextBytes(1024))
        var snapshot = (ModRepairSnapshots.create(versionPath, modsDir) as SnapshotCreateResult.Created).snapshot
        //修复做了一半：原文件被保留为 .disabled、新文件已落盘，之后失败
        assertNotNull(preserveSafely(File(modsDir, "orig-a.jar")))
        File(modsDir, "new-a.jar").writeBytes(Random(3).nextBytes(1024))
        snapshot = ModRepairSnapshots.finalize(versionPath, snapshot, modsDir, succeeded = false)
        assertEquals(SnapshotStatus.PARTIAL, snapshot.status)
        return snapshot
    }

    @Test
    fun aFailedMultiFileInstallLeftPartialIsRolledBackAutomaticallyAndHashesComeBack() = runBlocking {
        val before = mapOf("orig-a.jar" to sha1Of(Random(1).nextBytes(1024)), "keep.jar" to sha1Of(Random(2).nextBytes(1024)))
        val snapshot = partialSnapshot()
        val failed = ModDoctorFixResult(ModDoctorReport(emptyList()), installedFileNames = listOf("new-a.jar"), failureReason = "The dependency download/install failed.")

        val result = failWithRollbackIfPartial(failed, snapshot, versionPath, modsDir)

        assertNotNull("still a failure, never a success", result.failureReason)
        assertTrue(result.failureReason!!, result.failureReason!!.contains("rolled back automatically"))
        assertTrue(result.installedFileNames.isEmpty())
        assertEquals("the original files are back byte for byte", before, hashes())
        assertEquals(SnapshotStatus.ROLLED_BACK, ModRepairSnapshots.all(versionPath).first().status)
    }

    @Test
    fun whenTheAutomaticRollbackCannotRestoreEverythingItSaysSoAndKeepsTheUsersFile() = runBlocking {
        val snapshot = partialSnapshot()
        //用户（或别的程序）又放回了同名但内容不同的原文件：不得覆盖
        val userFile = File(modsDir, "orig-a.jar").apply { writeBytes(Random(99).nextBytes(10)) }
        val userHash = sha1(userFile)
        val failed = ModDoctorFixResult(ModDoctorReport(emptyList()), installedFileNames = listOf("new-a.jar"), failureReason = "boom")

        val result = failWithRollbackIfPartial(failed, snapshot, versionPath, modsDir)

        assertTrue(result.failureReason!!, result.failureReason!!.contains("ROLLED_BACK_PARTIAL"))
        assertFalse("never claims the files are back", result.failureReason!!.contains("original files are back"))
        assertEquals(listOf("new-a.jar"), result.installedFileNames)
        assertEquals(userHash, sha1(File(modsDir, "orig-a.jar")))
        assertEquals(SnapshotStatus.ROLLED_BACK_PARTIAL, ModRepairSnapshots.all(versionPath).first().status)
    }

    @Test
    fun aFailureWithNoFileChangesIsNotRolledBackAndKeepsItsReason() = runBlocking {
        File(modsDir, "a.jar").writeBytes(Random(1).nextBytes(256))
        var snapshot = (ModRepairSnapshots.create(versionPath, modsDir) as SnapshotCreateResult.Created).snapshot
        snapshot = ModRepairSnapshots.finalize(versionPath, snapshot, modsDir, succeeded = false)
        assertEquals(SnapshotStatus.NO_CHANGE, snapshot.status)
        val failed = ModDoctorFixResult(ModDoctorReport(emptyList()), failureReason = "No safe plan")

        assertEquals(failed, failWithRollbackIfPartial(failed, snapshot, versionPath, modsDir))
    }

    private fun sha1Of(bytes: ByteArray) = MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }
}

private class StubVersion(private val fileName: String, private val sha1: String?) : PlatformVersion {
    override suspend fun initFile(currentProjectId: String) = true
    override fun platform() = Platform.MODRINTH
    override fun platformId() = fileName
    override fun platformProjectId() = "proj"
    override fun platformDisplayName() = fileName
    override fun platformFileName() = fileName
    override fun platformGameVersion() = arrayOf("1.21.11")
    override fun platformLoaders(): List<PlatformDisplayLabel> = listOf(object : PlatformDisplayLabel {
        override fun getDisplayName() = "Fabric"
        override fun index() = 0
    })
    override fun platformReleaseType() = PlatformReleaseType.RELEASE
    override fun platformDependencies() = emptyList<PlatformVersion.PlatformDependency>()
    override fun platformDownloadCount() = 0L
    override fun platformDownloadUrl() = ""
    override fun platformDatePublished() = Instant.parse("2026-07-28T08:48:35Z")
    override fun platformSha1() = sha1
    override fun platformFileSize() = 0L
    override fun platformVersion() = fileName
}
