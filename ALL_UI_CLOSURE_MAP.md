# Phase 3.8 — ALL Desktop reachable UI dependency map
Physical main: D:\ALL Launcher 0.0.1. Base commit: 8f86215ea169f33a9245d5e676b09443cdd06e44. Audit 2026-10-04.
Source root Z = ZalithLauncher/src/main/java/com/movtery/zalithlauncher/.

| Classification | Exact entry point / dependency | Presentation and contract |
|---|---|---|
| 1 migrate | ALL Desktop instance tile -> instance window Delete, `Z/ui/screens/content/AllDesktopScreen.kt:717` -> `Z/all/bridge/AllBridge.kt:54` -> `Z/ui/screens/content/elements/VersionsManageElements.kt:540` DeleteVersionDialog | Keep shared deleteVersion closure -> onConfirm(title, task), VersionsManager.deleteVersion, onVersionDeleted. Four warning texts retained; ALL confirmation only. |
| 1 migrate | ALL Instances row Delete -> `Z/all/instances/AllInstancesWindow.kt:200` -> same AllDeleteVersionDialog | Identical shared deletion backend; cancel safe runtime gate. |
| 1 migrate | Desktop confirmed deletion -> `Z/ui/screens/content/AllDesktopScreen.kt:738` AllTaskDialog -> `Z/ui/components/Dialogs.kt:586` SimpleTaskDialog | Same rememberSaveable state, coroutine scope/context IO, task/catch/finally/onDismiss. Only ProgressDialog presentation replaced for bridge caller. |
| 1 migrate | Desktop Play -> LaunchGameOperation.RendererNoStoragePermission (`Z/ui/screens/content/elements/LauncherElements.kt:292`) -> `Z/utils/StorageUtils.kt:68` checkStoragePermissions -> showPermissionRequestDialog | Same authorization/ignore callbacks, noncancelable, same OS permission/backend checks; ALL activity-owned presentation. Actual OS permission sheet remains Android. |
| already ALL | Play -> UnsupportedRenderer / UnsupportedPlugins (`LauncherElements.kt:311,343`) | AllAlertDialog since 3.7b; same TryLaunch/RealLaunch/None callbacks. No migration. |
| 1 migrate | Instance Settings -> Support -> Vulkan check (`Z/all/settings/AllSettingsWindow.kt`, callback from `Z/ui/screens/main/MainScreen.kt:569`) -> Event.VulkanCheck -> MainActivity.checkVulkan:626 -> VulkanCheckerViewModel.check -> `Z/ui/vulkan_checker/VulkanChecker.kt:64` Result | ALL tip/result with original state callbacks, full summary/profiles/version/Turnip/extensions/features/ranges. Backend check and result resume unchanged. |
| 1 migrate | Play with Vulkan driver -> VulkanCheckerViewModel.ensureSupported/waitForVulkanChecker -> VCOperation.Tip/Result -> same MainActivity VulkanChecker | Noncancelable tip -> startCheck(version); result -> confirmResult then None. |
| already ALL | Settings Java tab -> `Z/all/launchersettings/AllJavaSettings.kt` delete/import/error/default flows | ALL-owned dialogs/task presentation; RuntimesManager reused; not a legacy UI import. |
| already ALL | ALL Instances -> LoaderDialog / ExportDialog in `Z/all/instances/AllInstancesWindow.kt:209,327` | ALL-owned local form/progress/status; uses GameInstaller.updateLoader / PackExporter. No Classic navigation. Safe open/cancel regression. |
| 2 Classic-only | Renderer/Game settings -> Event.DownloadPlugins -> MainActivity.showDownloadPlugins:768 | Only callers: ui/screens/content/settings/RendererSettingsScreen.kt and GameSettingsScreen.kt. ALL renderer settings has no download-plugin event/button; retain original Material prompt. |
| 2 Classic-only | NormalNavKey.VersionsManager -> VersionsManageScreen / NestedNavKey.VersionExport / VersionSettings.UpdateLoader | ALL Desktop uses independent Instances/service forms; Classic route requires explicit Classic switch. Leave original dialogs/pages. |
| 2 non-presentation adapters | AllBridge AssetsIcon / VersionIconImage / initAll / rememberMultipleUriImportTaskBuilder | Icons, parser/init or task-builder backend adapters, no legacy dialog. Import errors use ALL ErrorViewModel presentation. |
| 3 deferred | Desktop Settings -> Controls -> Edit -> AllBridge.allStartControlEditor -> ControlEditorActivity | Explicit user deferred Control Editor. Do not modify. |
| 3 deferred | Desktop Play -> VMActivity -> GameScreen/JVMScreen/GameBall | Explicit user deferred in-game UI. Do not modify. |
| 3 deferred | Accounts select an existing Microsoft account -> Play -> LaunchGameOperation.AccountRelogin/AccountRefreshFailedDialog | Microsoft/auth/skin/cape/wardrobe flow; explicit user deferred. No token actions. |
| 3 system | Import/export/share -> Android picker/share, permission authorization -> OS settings/sheet | System-owned, excluded. |

Other reachable ALL windows: Desktop, Download, Instances, Accounts, launcher Settings (5 tabs), instance Settings, Mods, Files, Packs, Shaders, Worlds, Screenshots, Servers, Tasks, About, wallpaper/UI scale. Their presentation is declared in all/ or AllDesktopScreen; material widgets inside ALL-owned files are ALL presentation, not Classic page routing. Existing launch/modpack/update/log/error/startup ALL routes were verified in Phase 3.7 and are not reimplemented.

Audit method: traced MainScreen ALL LauncherMain branch, AllDesktopScreen window layer wiring and imports, all/ imports through AllBridge, MainActivity event/overlay collectors, LauncherElements launch states, permission helper callers, and Classic navigation entryProvider. Runtime regression and final exact remainder follow in checkpoint.

Additional audit before link migration: Update release/cloud-link and log-upload-success -> Event.OpenLink -> MainActivity -> utils/network/NetWorkUtils.kt:239 Activity.openLink. This was a Zalith Material confirmation (Confirm/Cancel/Copy), group 1. Migrated only for MainActivity/ErrorActivity in ALL mode; exact openLinkInternal ACTION_VIEW and copyText callbacks retained. VMActivity/ControlEditor/Microsoft web flows excluded. External Android browser after confirm remains OS UI.

Audit correction / exact remainders (2026-10-04):
- AllAboutWindow.kt:104 open(url) uses Intent.ACTION_VIEW directly; it does NOT call Activity.openLink. Existing About link buttons stay ALL-owned and the external browser stays Android-owned. No About backend edit.
- Activity.openLink legacy confirmation is reachable via MainActivity.kt:282 Event.OpenLink, :557 log upload result, :572 update release link, :682 cloud-link result; ErrorActivity.kt:180 log upload result. Migrated in NetWorkUtils.kt:244 for ALL Main/Error activities. Runtime invoked the same production helper with synthetic public URL via JDWP; Cancel/Copy verified without network transmission.
- Classic JavaManageScreen.kt:208,217 -> executeJarWithUri -> game/launch/JvmLauncher.kt:130 finalErrorDialog is outside normal ALL Java settings. External jar/game shell remains deferred with VMActivity.

| Remaining visible Zalith UI from ALL Desktop | Exact reachable entry point | Closure decision |
|---|---|---|
| In-game shell | AllDesktopScreen Play -> LauncherElements.kt:609,639 LaunchGameOperation.RealLaunch / launchGameViewModel.start -> VMActivity -> GameScreen / JVMScreen / GameBall | Deferred Phase 3.10; game engine unchanged. |
| Control Editor | ALL launcher Settings Controls -> Edit -> AllControlSettings.kt:168 -> AllBridge.kt:115 allStartControlEditor -> startControlEditor -> ControlEditorActivity | Deferred Phase 3.10; control editor unchanged. |
| Microsoft/Auth | ALL Accounts selects existing account -> Play -> LaunchGameViewModel.kt:64,72 -> LauncherElements.kt:506 MicrosoftReloginDialog / :537 OtherAccountReloginDialog / :580,654 AccountRefreshFailedDialog. New Microsoft/auth-server login and skin/cape/wardrobe require explicit Classic account navigation | Deferred Phase 3.9; no real account action. |
| Classic-only pages | AllDesktopScreen.kt:696 ALL menu explicit Return to Classic, or Appearance interface selector -> MainScreen Classic navigation -> original settings/VersionsManager/loader/export/Java jar tools and plugin-download prompt | Intentional Classic interface; unchanged. |

Android system picker/share/permissions/browser are system UI. Licenses and attribution mentioning Zalith are retained; they are content in ALL-owned windows, not legacy presentation. All normal launcher targets identified above have ALL-owned presentation after 3.8. Runtime scope and unverified hardware/auth/Classic limitations are recorded in ALL_CORE_CHECKPOINT.md.

Account reachability correction: AllAccountsWindow.kt:71,99 offers offline creation/deletion and existing-account selection only. It has no Microsoft login button. The auth remainder reachable from ordinary ALL Play is AccountRelogin (MicrosoftReloginDialog / OtherAccountReloginDialog) and AccountRefreshFailed; login/wardrobe pages require explicit Classic navigation. All were left unchanged and untested on real accounts.
