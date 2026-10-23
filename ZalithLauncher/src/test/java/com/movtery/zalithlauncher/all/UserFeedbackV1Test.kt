package com.movtery.zalithlauncher.all

import com.movtery.zalithlauncher.all.download.visibleGameTypes
import com.movtery.zalithlauncher.all.overlays.problemModNames
import com.movtery.zalithlauncher.game.account.wardrobe.SkinModelType
import com.movtery.zalithlauncher.game.account.wardrobe.getLocalUUIDWithSkinModel
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersion
import com.movtery.zalithlauncher.game.version.mod.ModDoctorProblem
import com.movtery.zalithlauncher.game.version.mod.ModDoctorProblemType
import com.movtery.zalithlauncher.setting.ALL_DESKTOP_UI_SCALE_DEFAULT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** USER FEEDBACK V1：不依赖 Android 的部分（界面本身在真机上截图验证） */
class UserFeedbackV1Test {
    private fun strings(path: String): Map<String, String> =
        Regex("""<string name="([^"]+)"[^>]*>([^<]*)</string>""").findAll(File(path).readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    private val th = strings("src/main/res/values-th/strings.xml")
    private val en = strings("src/main/res/values/strings.xml")

    // 1. UI Scale
    @Test
    fun uiScaleDefaultsTo75WithinTheSliderRange() {
        assertEquals(75, ALL_DESKTOP_UI_SCALE_DEFAULT)
        assertTrue(ALL_DESKTOP_UI_SCALE_DEFAULT in 75..150)
        //默认值写在 intSetting 的 default 参数里：MMKV 里已有的用户值优先，所以只影响从未设置过的用户
        assertTrue(File("src/main/java/com/movtery/zalithlauncher/setting/AllSettings.kt").readText()
            .contains("intSetting(\"allDesktopUiScale\", ALL_DESKTOP_UI_SCALE_DEFAULT, 75..150)"))
    }

    // 2. Download header
    @Test
    fun theDownloadHeaderNoLongerShowsTheInstallTargetText() {
        val source = File("src/main/java/com/movtery/zalithlauncher/all/download/AllDownloadWindow.kt").readText()
        assertFalse(source.contains("all_dl_install_target_label"))
        //Download V5: the target is still chosen explicitly by the user (target-game dropdown -> selectTarget) and Install
        //stays disabled until a target is chosen; nothing is pre-selected for the user
        assertTrue(source.contains("viewModel.selectTarget(iv.getVersionName())"))
        assertTrue(source.contains("(modpack || targets.isNotEmpty())"))
    }

    // 4. Minecraft versions
    @Test
    fun onlyReleasesAreShownUntilTestVersionsAreEnabled() {
        assertEquals(setOf(MinecraftVersion.Type.Release), visibleGameTypes(false))
        val all = visibleGameTypes(true)
        assertTrue(MinecraftVersion.Type.Snapshot in all && MinecraftVersion.Type.OldBeta in all &&
            MinecraftVersion.Type.OldAlpha in all && MinecraftVersion.Type.AprilFools in all)
    }

    // 5. Accounts: the offline UUID keeps the Classic / Slim model, and every account has its own skin file name
    @Test
    fun offlineProfileIdsEncodeTheSkinModelAndStayStable() {
        val classic = getLocalUUIDWithSkinModel("Steve_QA", SkinModelType.STEVE)
        val slim = getLocalUUIDWithSkinModel("Steve_QA", SkinModelType.ALEX)
        assertNotEquals(classic, slim)
        assertEquals(classic, getLocalUUIDWithSkinModel("Steve_QA", SkinModelType.STEVE))
        assertEquals("classic", SkinModelType.STEVE.modelType)
        assertEquals("slim", SkinModelType.ALEX.modelType)
    }

    @Test
    fun skinFilesAreKeyedByTheAccountUuidNotByTheName() {
        val source = File("src/main/java/com/movtery/zalithlauncher/game/account/Account.kt").readText()
        assertTrue(source.contains("File(PathManager.DIR_ACCOUNT_SKIN, \"\$uniqueUUID.png\")"))
    }

    @Test
    fun theAccountsWindowReusesTheExistingSkinStackAndKeepsTheSevenTapGate() {
        val window = File("src/main/java/com/movtery/zalithlauncher/all/accounts/AllAccountsWindow.kt").readText()
        assertTrue(window.contains("AccountSkinOperation("))
        assertTrue(window.contains("PlayerSkin"))
        //登录与令牌逻辑不在这个文件里被改动：创建离线账号仍然走原来的 localLogin
        assertTrue(window.contains("localLogin("))
        val sevenTap = File("src/main/java/com/movtery/zalithlauncher/ui/screens/main/MainScreen.kt").readText()
        assertTrue(sevenTap.contains("AccountsManager.unlockOfflineMode()"))
    }

    // 6. Thai wording
    @Test
    fun thaiMenuAndWindowNamesUseTheRequestedWords() {
        assertEquals("จัดการม็อด", th["all_desktop_mods"])
        assertEquals("จัดการไฟล์", th["all_desktop_files"])
        assertEquals("เลือกตัวเกม", th["all_desktop_pick_instance"])
        assertEquals("เกมที่ติดตั้ง", th["all_instances_title"])
        assertTrue("no UI string may still say อินสแตนซ์", th.values.none { it.contains("อินสแตนซ์") })
        assertTrue("เลือก Instance" !in th.values)
    }

    @Test
    fun internalNamesAreUnchanged() {
        //只改显示文字：字符串键（技术名）保持原样
        assertTrue(th.keys.containsAll(listOf("all_desktop_mods", "all_desktop_files", "all_desktop_pick_instance", "all_instances_title")))
    }

    // 7. Launch box
    @Test
    fun theLaunchBoxHasItsOwnCompactStringAndTheSharedOneIsUntouched() {
        assertEquals("กำลังเข้าเกม...", th["all_game_entering"])
        assertEquals("Entering game...", en["all_game_entering"])
        assertTrue(th.containsKey("game_loading"))
    }

    // 8. Mod Doctor dialog
    private fun problem(file: String, type: ModDoctorProblemType = ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY) =
        ModDoctorProblem(type = type, fileName = file, modId = null, reason = "x")

    @Test
    fun problemSummaryCountsDistinctModsAndSplitsConflictPairs() {
        val names = problemModNames(listOf(
            problem("a.jar"), problem("a.jar"), problem("b.jar", ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR),
            problem("sodium.jar, iris.jar", ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR),
        ))
        assertEquals(listOf("a.jar", "b.jar", "sodium.jar", "iris.jar"), names)
    }

    @Test
    fun theDialogKeepsEveryOriginalActionAndAddsTheCompactSummary() {
        val source = File("src/main/java/com/movtery/zalithlauncher/all/overlays/AllLaunchDialogs.kt").readText()
        listOf("R.string.generic_cancel", "R.string.mod_doctor_fix", "R.string.generic_anyway", "GroupPlanPreview(", "all_mod_problem_show_details")
            .forEach { assertTrue("$it must remain", source.contains(it)) }
        assertTrue(th.containsKey("all_mod_problem_summary"))
    }
}
