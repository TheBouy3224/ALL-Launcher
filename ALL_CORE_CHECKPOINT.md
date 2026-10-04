# ALL LAUNCHER — CORE CHECKPOINT
Updated: 2026-09-19 00:25 +07:00

## Project
- Root: `D:\ALL Launcher 0.0.1`
- Branch: `all-0.0.1`
- Product: ALL Launcher
- ALL version: `0.0.1` / versionCode `1`
- Upstream base: ZalithLauncher2 `2.5.3`
- Upstream commit: `b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e`

## Baseline policy
- `D:\All Launcher` and `D:\All New` are reference-only; do not modify or delete them.
- Start from clean ZalithLauncher2 2.5.3 and add only what ALL actually needs.
- Ponytail Full: reuse/native/minimum diff; no speculative architecture.
- Do not port the old Mod Doctor/resolver wholesale.
- Every feature must pass a real runnable check before the next feature.

## Completed  — SUPERSEDED (records the 2026-09-16 state; see CURRENT STATE below)
- Clean upstream tag 2.5.3 cloned and verified.
- New branch `all-0.0.1` created.
- Minimal identity changed in `ZalithLauncher/gradle.properties`:
  `ALLLauncher`, `ALL Launcher`, `ALL`, versionCode `1`, versionName `0.0.1`.
- `git diff --check` PASS.

## Next  — SUPERSEDED (both lines were completed long ago; see CURRENT STATE below)
- Build a baseline x86_64 debug APK without adding features.
- If build passes: install on approved LDPlayer target and confirm ALL Launcher starts.

## CURRENT STATE — 2026-09-18 +07:00 (authoritative; supersedes the two blocks above)
**[2026-09-20 — สาย ARM64 shipping ปิดที่ Phase 5D; artifact ที่อยู่ในแรงคือ `3BD3E79D…` แทน `A253286D…` (บรรทัดด้านล่างอธิบายสาย LDPlayer/x86_64 ซึ่งยังถูกต้องในสายนั้น)]**
- Branch `all-0.0.1` @ HEAD `b4c9415b` (upstream 2.5.3). **Nothing is committed**; 27 working-tree entries
  (19 modified tracked + 8 untracked), `git diff --check` rc 0.
  **[UPDATED 2026-09-24 23:5x — NOT 27 any more, and the artifact named in this block is superseded. Measured
  by shell, central: 36 modified tracked + 20 untracked = 56 entries, nothing staged, HEAD still `b4c9415b`.
  The APK the tree builds is `ALLLauncher-Debug-0.0.2-beta1-x86_64.apk` from versionCode 2 / versionName
  `0.0.2-beta1` (see `gradle.properties`), not the `0.0.1` / versionCode 1 line below; the frozen ARM64
  release in `D:\ALL Launcher Releases\0.0.2-beta1\ALLLauncher-0.0.2-beta1-arm64-v8a.apk` re-hashes to
  187,188,460 B / `66E7552C97F7142FE1A1729FAD0940D700A7496B8E46CD1FBCC1BA14E9AFF5F4` and the protected
  `0.0.1` release to `3BD3E79D…715BAB9` (187,174,100 B) — both untouched on 2026-09-24. The `27 entries`
  figure below is left as the historical record of that date.]**
- ALL version `0.0.1` / versionCode `1`; release and debug are `com.all.launcher.app` (+`.debug`).
- **Release APK in force: `A253286D5FB3D2FB8CAA16A849AF7A2E16902238488C6408EC87186E5A835793`** — 193,000,665 B,
  mtime 2026-09-18 23:36:08, built from the LOGGING revision with `minifyReleaseWithR8` + lintVital +
  packageRelease executed and ALL signing (`b72b76bd…2177`); `mapping.txt` 163,062,576 B at 23:35:54.
  It **supersedes `A2F3FBB3…`** (193,000,621 B, 2026-09-18 22:47:47, built from `ModDoctor.kt 7ce70230…`),
  which superseded `530B2804…` (193,000,813 B, 13:53:40), which superseded `920b6a70…` (193,000,893 B,
  2026-09-17 19:13:17). Every earlier release section below still names one of those as the current artifact.
  The binary installed on the device hashes to the same digest (`a253286d…`).
  **PROVENANCE IS REVISION-SCOPED — read this before reusing any anchor below.** The R8 `mapping.txt` entry
  `…modDoctorDependencyRepairPlatform(…Platform):929:929 -> f0` binds ONLY the now-superseded `A2F3FBB3…` and is
  a **DEAD discriminator** for the artifact in force, whose own mapping reads `…:937:937`. The shipped logging
  revision is additionally bound by a dex string probe (`inspect finished` and `post-fix re-check` are present
  in `classes.dex`), which needs no obfuscation-surviving symbol.
  **[UPDATED 2026-09-19 00:56 — a FOURTH Release, `B9821D1C…` (193,000,737 B, 00:50:41), was built and installed
  for the CurseForge source-selection fix recorded in the newest section; it supersedes `A253286D…` as the
  artifact in force, while `A253286D…` remains the binary the accepted Mod Doctor milestone was proven on.
  `firstInstallTime` 2026-09-17 19:30:54 is unchanged across all four installs.]**
- **Mod Doctor platform rule in force (corrected 2026-09-18, later run):** the user's Search Platform
  (`AllSettings.searchModPlatform`, default `CURSEFORGE`) governs user-facing browse/search/download **only**.
  Every provider call inside Mod Doctor is pinned to `Platform.MODRINTH` by two helpers that ignore their
  user-preference argument — `modDoctorFingerprintPlatform(...)` (`ModDoctor.kt:928`; installed-file identity,
  wired at `:155` and `:513`) and `modDoctorDependencyRepairPlatform(...)` (`ModDoctor.kt:937`;
  missing-dependency project/version resolution, applied at `:531`). The residual this block used to name —
  that the missing-dependency lookup still followed the user setting — is **discharged**.
  **ANCHORS ARE REVISION-SCOPED.** The logging change of 2026-09-18 moved them a second time. Measured in the
  current revision: `ModDoctor.kt :36` (TAG) · `:143` (inspect started) · `:144` (val result = try) · `:155`
  (fingerprint wiring) · `:174` (inspect finished) · `:175` (return result) · `:509` (the inert seam parameter)
  · `:513` (installedMod default) · `:531` (the repair rule applied) · `:928` (fingerprint helper) · `:937`
  (repair helper); `LauncherElements.kt :125` (TAG) · `:360` (launch gate) · `:396` (post-fix marker) · `:397`
  (re-check). Shifts from the previous revision: `:920→:928`, `:929→:937`, `:501→:509`, `:505→:513`,
  `:523→:531`, `:149→:155`; `:357→:360`, `:393→:397`. Totals: `ModDoctor.kt` 997 lines (LF-only);
  `LauncherElements.kt` 848 (CRLF throughout).
- Device: LDPlayer `127.0.0.1:5555` (port owned by `Ld9BoxHeadless.exe`; `emulator-5554` never used).
  Release installed as an update over the 09-17 build with data preserved; Search Platform CURSEFORGE.
  **[UPDATED 2026-09-18 22:50:01 — the Release has since been updated AGAIN, to `A2F3FBB3…`, and used for the
  Release missing-dependency E2E; its `firstInstallTime` 2026-09-17 19:30:54 is unchanged throughout.]**
  **[UPDATED AGAIN 2026-09-18 23:37:19 — the Release has since been updated a THIRD time, to `A253286D…`, for
  the observability run. `firstInstallTime` is still 2026-09-17 19:30:54 across all three installs.]**
  The DEBUG package's Search Platform now sits at **CURSEFORGE** — it was Modrinth until the 22:09-22:22 run
  changed it through the UI because that milestone required it; the Release was already CURSEFORGE.
  **[SUPERSEDED 2026-09-18 22:12 +07 — the debug package HAS since been updated and started.** It was
  installed over (`lastUpdateTime` 2026-09-18 22:12:42) and run for the missing-dependency detect+fix E2E;
  the debug APK named below as `8AAAD6AD…` was also superseded, by `8823BEC0…`. See the newest section.**]
- `test-artifacts/` is **untracked and NOT git-ignored** — see the correction at old line 1170.

## MICRO-RUN — upstream source completeness audit — PASS

Timestamp: `2026-09-16`.

### Evidence
- Target root: `D:\\ALL Launcher 0.0.1`; branch: `all-0.0.1`.
- Local HEAD/tag: `b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e` / `2.5.3`.
- Official remote `refs/tags/2.5.3` resolves to the same commit.
- Upstream tree: `1544` paths; local index: `1544`; path sets equal.
- Missing tracked worktree paths: `0`; submodules: none; sparse checkout: unset.
- Only tracked modification is the intentional identity/version edit in `ZalithLauncher/gradle.properties`; checkpoint is the only untracked file.
- No APK release asset is present in this source checkout; release binaries are separate artifacts and were not counted as source files.
- `git diff --check`: PASS.

### Verdict
`PASS — source repository files are complete for upstream tag 2.5.3. ALL Launcher adds only the recorded identity/version customization; no upstream source file is missing.`

### Exact Next Action
`Build a baseline x86_64 debug APK from D:\\ALL Launcher 0.0.1 without adding features.`

## MICRO-RUN — clean ALL Launcher 0.0.1 baseline on LDPlayer — PASS

Timestamp: `2026-09-16`.

### Milestone / result
- Verified and installed the existing x86_64 debug APK on the real LDPlayer target.
- No source code, account, Minecraft launch, or mod installation was performed.

### Exact commands / actions
- `adb devices -l` — selected `127.0.0.1:5555`; did not use `emulator-5554`.
- Verified `127.0.0.1:5555` is owned by `Ld9BoxHeadless.exe` PID `14568` listening on TCP port `5555`.
- `adb -s 127.0.0.1:5555 install -r <APK>` — first attempt returned `INSTALL_FAILED_VERSION_DOWNGRADE` because the installed package was newer.
- Read installed package state with `pm path` and `dumpsys package`.
- `adb -s 127.0.0.1:5555 install -r -d <APK>` — `Success`; `-d` was required to preserve data while downgrading.
- Launched with `adb -s 127.0.0.1:5555 shell monkey -p com.movtery.zalithlauncher.v2.debug -c android.intent.category.LAUNCHER 1`.
- Verified with `pidof`, `dumpsys activity activities`, `uiautomator dump`, and `logcat`.

### Installed package / verification
- Package: `com.movtery.zalithlauncher.v2.debug`
- versionCode: `1`
- versionName: `0.0.1-debug` (core version `0.0.1`)
- Running PID remained `13143` across repeated checks.
- `mResumedActivity` remained `SplashActivity`.
- UI hierarchy visibly contained `ALL Launcher` and the launcher setup screen.
- No package-specific crash/fatal marker was found; Minecraft was not launched.

### APK evidence
- Path: `ZalithLauncher/build/outputs/apk/debug/ALLLauncher-Debug-0.0.1-x86_64.apk`
- Size: `220763483` bytes
- SHA-256: `9ebaa9c217f3fdce450a7c5b191728ea19c157748d601f8857cb0cea037b4875`

### Changed files
- Source: none.
- APK: not modified.
- Checkpoint: this file updated with this micro-run.

### Exact Next Action
`MICRO-RUN NEXT: inspect only the initial ALL Launcher UI state on LDPlayer; do not launch Minecraft, add accounts, install mods, or edit source.`

## MICRO-RUN — repeat clean ALL Launcher 0.0.1 baseline on LDPlayer — PASS

Timestamp: `2026-09-16`.

### Milestone / result
- Repeated the complete clean baseline verification on the real LDPlayer target.
- No source code, account, Minecraft launch, or mod installation was performed.

### Exact commands / actions
- Read this checkpoint before starting.
- `adb devices -l` — selected `127.0.0.1:5555`; did not use `emulator-5554`.
- Verified TCP port `5555` is owned by `Ld9BoxHeadless.exe` PID `14568`.
- Force-stopped only `com.movtery.zalithlauncher.v2.debug` before reinstall.
- `adb -s 127.0.0.1:5555 install -r <APK>` — `Success`.
- Read installed state with `adb shell dumpsys package`.
- Launched with `adb shell monkey -p com.movtery.zalithlauncher.v2.debug -c android.intent.category.LAUNCHER 1`.
- Verified with `pidof`, `dumpsys activity activities`, `uiautomator dump`, and `logcat`.

### Installed package / verification
- Package: `com.movtery.zalithlauncher.v2.debug`
- versionCode: `1`
- versionName: `0.0.1-debug` (core version `0.0.1`)
- Running PID: `13716` after launch.
- `mResumedActivity`: `SplashActivity`.
- UI hierarchy contained `ALL Launcher` and the launcher setup screen.
- Package-specific crash/fatal markers: `0`.
- Minecraft was not launched.

### APK evidence
- Path: `ZalithLauncher/build/outputs/apk/debug/ALLLauncher-Debug-0.0.1-x86_64.apk`
- Size: `220763483` bytes
- SHA-256: `9ebaa9c217f3fdce450a7c5b191728ea19c157748d601f8857cb0cea037b4875`

### Changed files
- Source: none.
- APK: not modified.
- Checkpoint: this file updated with this repeat run.

### Exact Next Action
`MICRO-RUN NEXT: inspect only the initial ALL Launcher UI state on LDPlayer; do not launch Minecraft, add accounts, install mods, or edit source.`

## MICRO-RUN — Initial UI Baseline for ALL Launcher 0.0.1 on LDPlayer — PASS

Timestamp: `2026-09-16`.

### Result
- Inspected the initial user-facing UI on LDPlayer serial `127.0.0.1:5555`.
- Navigation inspection was read-only; each opened screen was returned to the main screen safely.

### Visible UI baseline
- App-bar branding: `ALLLauncher` (exact visible text, no space).
- Warning card: `คำเตือน`; experimental ALL Launcher warning is visible.
- Account state: `Add Account`; no account is present or created.
- Minecraft version state: `No installed versions`; no version is selected or installed.
- Launch button: `เริ่มเกม` visible at the lower right; no launch was pressed.
- Main entries visible: folder/file manager, multiplayer (`Terracotta`), download, and settings.
- File manager screen: `FileManagerActivity`, showing the launcher files root; no file operation performed.
- Multiplayer screen: `Terracotta` / `โหมดผู้เล่นหลายคน`; no multiplayer activation performed.
- Download screen: `ดาวน์โหลด - เกม` with categories `เกม`, `มอดแพ็ค`, `มอด`, `รีซอร์สแพ็ค`, `โลก`, and `เชเดอร์แพ็ก`; version list was visible.
- Settings screen: `การตั้งค่า - Renderer` with sections `Renderer`, `Game`, `Controls`, `Gamepad`, `Launcher`, `Java`, and `About`; no setting was changed.
- User-facing `Zalith Launcher` branding: none found in the captured UI text. `Terracotta` remains visible as an upstream module name on the multiplayer screen.

### Evidence paths
- `test-artifacts/baseline-ui/initial-ui.xml`
- `test-artifacts/baseline-ui/initial-ui.png`
- `test-artifacts/baseline-ui/file-manager.xml`
- `test-artifacts/baseline-ui/multiplayer.xml`
- `test-artifacts/baseline-ui/download.xml`
- `test-artifacts/baseline-ui/settings.xml`
- `test-artifacts/baseline-ui/final-ui.xml`

### Final verification
- `MainActivity` remained resumed; app PID `13716` remained alive.
- Package-specific crash/fatal markers: `0`.
- Final UI still showed `Add Account` and `No installed versions`.
- No Minecraft launch, account creation, mod installation, or source edit occurred.
- Git source diff remained limited to the pre-existing intentional `ZalithLauncher/gradle.properties` identity/version edit; no source file changed in this run.

### Source changes
- None.

### Exact Next Action
`MICRO-RUN NEXT: choose one explicit ALL Launcher UI/branding change from this baseline; do not begin source editing until that single scope is selected.`

## MICRO-RUN — Change applicationId/package identity — PASS

Timestamp: `2026-09-16`.

### Result / root cause
- Required release identity: `com.all.launcher.app`.
- Required debug identity: `com.all.launcher.app.debug`.
- Initial build exposed that the old single variable controlled both namespace and applicationId; moving namespace caused `R/BuildConfig/BuildKeys` compile failures.
- Corrected by preserving namespace `com.movtery.zalithlauncher`, introducing `launcherApplicationId`, using it only for `applicationId`, and removing the old default `.v2` suffix while retaining debug suffix `.debug`.

### Exact file changed
- `ZalithLauncher/build.gradle.kts` only for package identity:
  - namespace remains `com.movtery.zalithlauncher`
  - applicationId base is `com.all.launcher.app`
  - debug suffix is `.debug`
  - release resolves to `com.all.launcher.app`
  - debug resolves to `com.all.launcher.app.debug`
- `ZalithLauncher/gradle.properties` was not changed in this run; its pre-existing ALL identity/version edit remains.

### Build / install
- Build alias verified: `D:\\ALL001` resolves to the same Git root.
- Command: `gradlew.bat :ZalithLauncher:assembleDebug -Darch=x86_64 --no-daemon --console=plain`.
- Final build result: `BUILD SUCCESSFUL in 3m 49s`.
- `git diff --check`: PASS.
- APK: `ZalithLauncher/build/outputs/apk/debug/ALLLauncher-Debug-0.0.1-x86_64.apk`.
- APK SHA-256: `1e8efb7b69c1051269f32f55f11b3e4d8662e18429ee533fc66135279ad35f2a`.

### Installed package / runtime verification
- Real LDPlayer serial: `127.0.0.1:5555`.
- `adb install -r`: `Success`.
- Installed package: `com.all.launcher.app.debug`.
- versionCode: `1`.
- versionName: `0.0.1-debug`.
- App label: `ALL Launcher`.
- Launched package process PID `15263`; `SplashActivity` remained resumed on the first-run component setup screen.
- UI showed `ALL Launcher`; package-specific crash/fatal markers: `0`.
- No account was created and no Minecraft version was installed; new `.minecraft`, `versions`, and `accounts` directories were absent.

### Android data path
- New path exists: `/sdcard/Android/data/com.all.launcher.app.debug/`.
- New app files path exists: `/sdcard/Android/data/com.all.launcher.app.debug/files/`.
- New package APK path: `/data/app/com.all.launcher.app.debug-DB_x6dEJUiamKVP4p1Rwmg==/base.apk`.
- New package did not use the old package identity/path; old path remains only for the preserved old installation.

### Old package status
- `com.movtery.zalithlauncher.v2.debug` remains installed and was not deleted.
- Old data path remains: `/sdcard/Android/data/com.movtery.zalithlauncher.v2.debug/`.
- Temporary incorrect package `com.all.launcher.app.v2.debug` was removed after verification; it is not installed.

### Evidence
- `test-artifacts/package-identity/initial-ui.xml`
- `test-artifacts/package-identity/initial-ui.png`
- `test-artifacts/package-identity/final-ui.xml`
- `test-artifacts/package-identity/identity-check.txt`
- `test-artifacts/package-identity/data-path-final.txt`

### Exact Next Action
`MICRO-RUN NEXT: verify the new package's first-run UI/data initialization on LDPlayer without adding accounts, installing Minecraft/mods, or changing source.`

## MICRO-RUN — Restore hidden Offline Account 7-tap unlock — PASS

Timestamp: `2026-09-16`.

### Result
- PASS — restored the old hidden Offline Account access behavior on real LDPlayer `127.0.0.1:5555`.
- `D:\\All New` remained read-only and was not modified.

### Old implementation / logic reused
- Old 7-tap source: `D:\\All New\\ZalithLauncher2\\ZalithLauncher\\src\\main\\java\\com\\movtery\\zalithlauncher\\ui\\screens\\main\\MainScreen.kt`.
- Reused `rememberSaveable` tap count/timestamp, `SystemClock.uptimeMillis()`, 3-second reset window, seventh title/top-left tap, existing `sendToast`, and navigation to `NormalNavKey.AccountManager(FirstLoginMenu.LOCAL)`.
- Old account entry source: `D:\\All New\\ZalithLauncher2\\ZalithLauncher\\src\\main\\java\\com\\movtery\\zalithlauncher\\ui\\screens\\content\\AccountManageScreen.kt`.
- Reused `FirstLoginMenu.LOCAL -> UpdateLocalLoginOp(LocalLoginOperation.Edit)`.
- Reused the old `AccountsManager.checkLimit()` Local-account guard so a created Local account can remain current/selectable.
- Added a session-only `AccountsManager.offlineModeUnlocked` state; process restart resets it.

### Current upstream flow traced
- Main title/top-left click target -> seven-tap counter -> existing Toast -> `AccountManager(LOCAL)`.
- `AccountManager(LOCAL)` -> existing `LocalLoginOperation.Edit` -> upstream `LocalLoginDialog`.
- Dialog confirm -> existing `CreateLocalAccount` intent -> existing `AccountsManager` save/database flow.
- After session unlock, normal Add Account -> existing `LoginMenuDialog` -> Offline -> same upstream Local dialog.
- Microsoft login and the Offline backend were not replaced or changed.

### Exact files changed
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/main/MainScreen.kt`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/AccountManageScreen.kt`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/account/AccountsManager.kt`
- `ZalithLauncher/src/main/res/values/strings.xml`

### Minimal diff summary
- Added the old seven-tap state/callback and attached it to the existing top-bar title click target.
- Added `FirstLoginMenu.LOCAL` and its existing Local dialog entry.
- Added one Local-account condition to the shared `checkLimit()` guard.
- Added one session-only shared unlock state and used it in the Add Account visibility guard.
- Added only the missing default `offline_mode_unlocked` string.

### Build / install evidence
- Build alias `D:\\ALL001` resolved to Git root `D:\\ALL Launcher 0.0.1`.
- `:ZalithLauncher:compileDebugKotlin`: `BUILD SUCCESSFUL`.
- `:ZalithLauncher:assembleDebug -Darch=x86_64 --no-daemon --console=plain`: `BUILD SUCCESSFUL in 2m 9s`.
- APK: `D:\\ALL001\\ZalithLauncher\\build\\outputs\\apk\\debug\\ALLLauncher-Debug-0.0.1-x86_64.apk`.
- APK size: `224487990` bytes; SHA-256: `1fc50e869a7fd646752853e912023c126bb60667bc2a8cacc26f097ca5332bae`.
- APK contained `lib/x86_64`; `aapt` metadata: package `com.all.launcher.app.debug`, versionCode `1`, versionName `0.0.1-debug`.
- `adb install -r`: `Success`.

### Real UI verification
- First-run launcher components were installed only to reach the requested main UI; no Minecraft or mods were installed.
- Before unlock on a fresh package state: Add Account showed only `Add Microsoft Account`; `Offline` was hidden.
- 7-tap: seven taps on the visible main title/top-left area opened `Create Offline Account` with `Username`, `Advanced Settings`, `ตกลง`, and `ยกเลิก`.
- After canceling the direct dialog, normal Add Account showed `Microsoft`, `Offline`, and `Add Auth` before any account was created.
- Offline creation: selected `Offline`, entered `ALLQA001` through the real dialog and confirmed; UI showed `ALLQA001`, `Offline`, and selected/current radio state.
- Selecting `Offline` reopened the existing upstream `Create Offline Account` dialog.
- Final main UI showed selected/current `ALLQA001` and `No installed versions`.
- App remained alive: PID `17020`; resumed `MainActivity`; package crash markers `0`.
- `/sdcard/Android/data/com.all.launcher.app.debug/files/.minecraft/versions` absent.
- `/sdcard/Android/data/com.all.launcher.app.debug/files/.minecraft/mods` absent.
- `git diff --check`: `PASS`.

### Minimal QA evidence
- `test-artifacts/offline-account-7tap/QA_SUMMARY.txt`
- `test-artifacts/offline-account-7tap/19-locked-fresh-menu.png`
- `test-artifacts/offline-account-7tap/20-after-7tap-fresh.png`
- `test-artifacts/offline-account-7tap/21-unlocked-menu-before-create.png`
- `test-artifacts/offline-account-7tap/22-offline-dialog-fresh.png`
- `test-artifacts/offline-account-7tap/23-username-entered.png`
- `test-artifacts/offline-account-7tap/24-after-offline-create-fresh.png`
- `test-artifacts/offline-account-7tap/25-final-main-fresh.png`

### Exact Next Action
`MICRO-RUN NEXT: verify the new package's first-run UI/data initialization on LDPlayer without adding accounts, installing Minecraft/mods, or changing source.`

## MICRO-RUN — Set default ALL Launcher visual branding — PASS

Timestamp: `2026-09-16`.

### Result
- PASS — installed `com.all.launcher.app.debug` shows the intended built-in ALL branding artwork on real LDPlayer `127.0.0.1:5555`.
- `D:\\All New` remained read-only and was not modified.
- Existing account, Minecraft version, launch logic, account logic, Offline 7-tap logic, and Mod Doctor were not changed in this run.

### User-verified playable baseline
- PASS — user verified before this run that the current app can launch Fabric `1.21.11` successfully.
- This run did not launch Minecraft; it verified that the installed `Fabric 1.21.11 Fabric 0.19.5` entry remained visible after the branding APK was installed with `adb install -r`.

### Asset source / resolution path
- Reused from `D:\\All New\\ZalithLauncher2\\ZalithLauncher\\src\\main\\res\\drawable-nodpi\\`:
  - `all_launcher_default_background.png` — built-in full-screen ALL fallback artwork.
  - `all_launcher_cover.png` — ALL artwork used by splash branding.
  - `all_launcher_cover_safe.png` — safe-zone padded ALL artwork for adaptive launcher icon foreground.
- Main visual path remains the existing theme/media system:
  - `MainActivity.onCreate()` calls `BackgroundViewModel.ensureDefault(this)` after `refreshData()`.
  - `ensureDefault()` copies the built-in `all_launcher_default_background` only when `PathManager.FILE_LAUNCHER_BACKGROUND` (`files/background/background01.file`) does not exist.
  - The existing `Background()` renderer continues reading that same file and keeps its existing image/video handling.
  - An existing user-selected image/video therefore remains in place and takes precedence; the ALL artwork is only the no-custom-media fallback.
- Custom theme/media storage and import/delete logic were not overwritten or redirected.

### Exact files changed
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/viewmodel/BackgroundViewModel.kt`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/activities/MainActivity.kt`
- `ZalithLauncher/src/main/res/drawable/splash_launcher.xml`
- `ZalithLauncher/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `ZalithLauncher/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `ZalithLauncher/src/main/res/drawable-nodpi/all_launcher_default_background.png`
- `ZalithLauncher/src/main/res/drawable-nodpi/all_launcher_cover.png`
- `ZalithLauncher/src/main/res/drawable-nodpi/all_launcher_cover_safe.png`

### Minimal diff / behavior change
- Added only the missing built-in fallback seed and caller.
- Switched adaptive icon foreground and splash drawable to the reused ALL artwork.
- Default/fallback behavior changed: absent custom media now seeds the ALL default.
- Custom image/video/theme replacement path remains intact and unchanged.

### Build / install evidence
- `:ZalithLauncher:compileDebugKotlin`: `BUILD SUCCESSFUL`.
- `:ZalithLauncher:assembleDebug -Darch=x86_64 --no-daemon --console=plain`: `BUILD SUCCESSFUL in 6m 1s`.
- APK: `D:\\ALL001\\ZalithLauncher\\build\\outputs\\apk\\debug\\ALLLauncher-Debug-0.0.1-x86_64.apk`.
- APK size: `233266194` bytes; SHA-256: `bbd0a5050088e6748ffc6c35a61207c9fe622fb03373a38acfc4d5e8b3259a9d`.
- APK ZIP integrity: PASS; x86_64 ABI: present; adaptive icon resource: present; default asset: present.
- `adb install -r`: `Success`.
- `git diff --check`: `PASS`.

### Real LDPlayer verification
- New package activity ownership verified from the home icon: ALL anime/logo icon opened `com.all.launcher.app.debug`; preserved cube icon opened old `com.movtery.zalithlauncher.v2.debug`.
- New package remained `com.all.launcher.app.debug`; installed metadata remained versionCode `1`, versionName `0.0.1-debug`.
- App label remains `ALL Launcher`.
- Main UI showed the intended ALL default artwork with no custom media selected.
- Main UI retained `ALLQA001` as current Offline account.
- Main UI retained the installed `1.21.11 Fabric 0.19.5` entry.
- Private fallback file `files/background/background01.file` was present after launch and its SHA-256 matched the source default asset exactly: `6315470001463efbc8c017b2bcb9b8ca67f9d5d39fe026fd25cc6d1deb274563`.
- Runtime remained healthy: PID `19379`, `MainActivity` resumed.
- `FATAL EXCEPTION`, `Process: com.all.launcher.app.debug`, `SIGSEGV`, and `Fatal signal` markers: `0`.

### Minimal QA evidence
- `test-artifacts/branding-default/01-before-branding.png`
- `test-artifacts/branding-default/02-after-install.png`
- `test-artifacts/branding-default/02-installed-default-background.png`
- `test-artifacts/branding-default/03-home-icon.png`
- `test-artifacts/branding-default/04-home-icon-reloaded.png`
- `test-artifacts/branding-default/05-final-branding.png`

### Exact Next Action
`MICRO-RUN NEXT: verify custom image/video theme replacement on the new package using one disposable user-selected media file, without changing source or launching Minecraft.`

## MICRO-RUN — Auto Dependencies V1 for mod installation — PASS

Timestamp: `2026-09-16`.

### User-verified baselines carried into this run
- Theme/background replacement: `PASS` — user personally verified that custom background replacement still works.
- Fabric playable baseline: `PASS` — user verified that Fabric Minecraft `1.21.11` / Fabric Loader `0.19.5` launches successfully.
- This run did not change theme/background, account, Minecraft launch logic, Offline 7-tap logic, or Mod Doctor.

### Current install flow traced
- Real UI Mod downloader -> provider project search -> project version list -> selected `PlatformVersion` -> `platformDependencies()` metadata -> `DownloadSingleOperation.Install`.
- `DownloadModScreen` invokes the existing `downloadSingleForVersions()` for the selected mod and `downloadDependenciesForVersions()` for selected required dependency requests.
- `DownloadAssetsScreen` reads dependency metadata from the provider response and builds `DependencyEntry` objects.
- The dependency pipeline resolves the target Minecraft version/loader, filters `PlatformDependencyType.REQUIRED`, recursively expands required dependencies, and sends resolved files through the same `downloadSingleForVersions()` install path.
- Destination verified: `/sdcard/Android/data/com.all.launcher.app.debug/files/.minecraft/versions/1.21.11 Fabric 0.19.5/mods/`.

### Provider / metadata
- Provider used: `Modrinth`.
- The selected Modrinth version response exposed required dependency metadata in `ModrinthVersion.dependencies`; conversion is performed by `platformDependencies()`.
- Optional/incompatible dependency handling was not added or tested.

### Real test
- Mod A: `Mod Menu v17.0.0 for Minecraft 1.21.11 Fabric`.
- Required dependency B: `Text Placeholder API`.
- Additional required dependency already installed: `Fabric API`.
- Before install: only `fabric-api-0.141.6+1.21.11.jar` existed; Mod Menu and Placeholder API were absent.
- Installed Mod A once through the real UI. After completion, the mods directory contained exactly:
  - `modmenu-17.0.0.jar`
  - `placeholder-api-2.8.2+1.21.10.jar`
  - existing `fabric-api-0.141.6+1.21.11.jar`
- Result: `PASS` — required Text Placeholder API was automatically installed through the existing dependency/download pipeline; no metadata or mod file was fabricated.

### Duplicate guard
- Repeated the same Mod Menu install action through the real UI.
- The dialog marked Text Placeholder API and Fabric API as installed and unchecked.
- Final inventory remained exactly one copy of each intended file.
- SHA-1 values remained unchanged:
  - Mod Menu: `544a8b340ac3918d85bcc7d02eaff5372814354b`
  - Text Placeholder API: `bbdc920ecff993148cd6e81a208536132245ce40`
  - Fabric API: `c98467cbbaf4d197377266795ae015f4130d65b6`
- Repeat log contained one `DownloadSingle` cache cleanup for the selected Mod Menu and no dependency download/resolve task; source guard `DependencyContext.isInstalled()` skipped already-installed dependency projects.
- Result: `PASS` — required dependency was not downloaded or duplicated on repeat.

### Implementation / regression check
- No production source change was necessary: current ZalithLauncher2 2.5.3 already contains and wires the required V1 behavior in:
  - `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/download/assets/_Download.Dependency.Tasks.kt`
  - `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/download/assets/_Download.Single.Tasks.kt`
  - `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/download/DownloadModScreen.kt`
  - `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/download/assets/download/DownloadAssetsScreen.kt`
  - `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/download/assets/elements/_Download.Single.kt`
- Existing duplicate guard uses `scanModFingerprints()` + `matchInstalledMods()` in the target version's mods directory; no generalized resolver, Mod Doctor architecture, optional dependency solver, conflict repair, or new abstraction was added.
- The real Modrinth install/repeat E2E is the runnable regression check for this unchanged production path; no separate test file was added because no new logic was introduced.

### Build / install
- Command: `:ZalithLauncher:assembleDebug -Darch=x86_64 --no-daemon --console=plain` from `D:\\ALL001`.
- Result: `BUILD SUCCESSFUL in 1m 36s`; `123 actionable tasks: 2 executed, 121 up-to-date`.
- `adb install -r`: `Success` on `127.0.0.1:5555`.
- Package remained `com.all.launcher.app.debug`; versionCode `1`; versionName `0.0.1-debug`.
- `git diff --check`: `PASS`.

### Game launch result
- Optional startup smoke completed after the first/repeat install.
- `VMActivity` became resumed and the real Minecraft screen reached the main menu for `Minecraft 1.21.11` with Fabric, without an immediate mod startup failure.
- After reinstall, `MainActivity` resumed with PID `21349`; package-specific fatal markers: `0`.

### Minimal QA evidence
- `test-artifacts/auto-dependencies-v1/01-main-before-auto-deps.png`
- `test-artifacts/auto-dependencies-v1/03-mod-category.png`
- `test-artifacts/auto-dependencies-v1/07-modmenu-search.png`
- `test-artifacts/auto-dependencies-v1/08-modmenu-detail.png`
- `test-artifacts/auto-dependencies-v1/10-install-dialog.png`
- `test-artifacts/auto-dependencies-v1/10-mods-before.txt`
- `test-artifacts/auto-dependencies-v1/11-inventory-after-first.txt`
- `test-artifacts/auto-dependencies-v1/11-sha1-after-first.txt`
- `test-artifacts/auto-dependencies-v1/17-inventory-after-duplicate.txt`
- `test-artifacts/auto-dependencies-v1/17-repeat-install-log.txt`
- `test-artifacts/auto-dependencies-v1/21-launch-activity.txt`
- `test-artifacts/auto-dependencies-v1/22-game-startup.png`
- `test-artifacts/auto-dependencies-v1/23-final-main-after-install.png`
- `test-artifacts/auto-dependencies-v1/23-final-inventory.txt`
- `test-artifacts/auto-dependencies-v1/23-final-sha1.txt`

### Exact Next Action
`MICRO-RUN NEXT: verify custom image/video theme replacement on the new package using one disposable user-selected media file, without changing source or launching Minecraft.`

## MICRO-RUN — READ-ONLY Core Systems Audit — PASS

Timestamp: `2026-09-16`.

### Scope / method
- Audited the current `ZalithLauncher` source against ZalithLauncher2 2.5.3 at upstream commit `b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e`.
- Read the checkpoint first.
- Traced source implementations and caller/user-flow seams for accounts, versions/loaders, mod download/install/management, pre-launch validation, repair, Java/runtime, crash/error handling, controls, theme/background, file manager, and downloads.
- Checked for current `ModDoctor*`, backup, conflict, and validator source files.
- `D:\\All New` and `D:\\All Launcher` were reference-only and not modified.

### Audit result
- Audit: `PASS`.
- Audit artifact: `D:\\ALL Launcher 0.0.1\\ALL_SYSTEM_AUDIT.md`.
- Source code modified during audit: `None`.
- No build, APK install, feature implementation, refactor, or old-code port was performed.
- Current HEAD remained `b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e` on branch `all-0.0.1`; pre-existing ALL source changes remain outside this audit.

### Major systems confirmed EXISTS
- Microsoft OAuth/Xbox/Minecraft authentication, cached credential validation/refresh/relogin, local/offline accounts, persistence, selection, and deletion.
- Minecraft version discovery, installation, overwrite/loader update, deletion, isolation/custom paths, and game-file verify/repair.
- Modrinth and CurseForge download/install, provider metadata, required dependency resolution, compatible MC/loader filtering, and duplicate/installed detection.
- Mod metadata/details, enable/disable, delete, and confirmed compatible mod updates.
- Controls editor/layout import-export/delete and gamepad remapping.
- ALL default background fallback while custom image/video remains higher priority.
- File manager/game-data browsing and operations.
- Crash logs, game/launcher error UI, log share/upload/restart/exit, and task/download progress/failover/checksum handling.

### Genuine PARTIAL systems
- Mod management: no first-class arbitrary installed-mod version change/downgrade action.
- Pre-launch validation: account and Minecraft file checks exist; no mod graph gate for missing dependency, wrong loader/MC version, incompatible mods, duplicate logical mods, or corrupt/unreadable mod files.
- Repair/Fix: game-file repair and game-install rollback exist; mod conflict/dependency repair and instance backup/rollback are absent.
- Java/runtime: install/select/delete and broken/version/architecture detection exist; no automatic runtime repair/reselection flow.
- Crash/error: detection/logging/share/upload/restart exist; no actionable mod/runtime root-cause diagnosis or guided repair.
- Download manager: progress, failover, bounded retry, `.part`, SHA-1, and atomic commit exist; durable resume after process interruption is absent because failed attempts remove `.part`.

### Mod Doctor findings
- Current 2.5.3 source contains no `ModDoctor`, `ModDoctorPlanSolver`, or mod-doctor package.
- `AllModReader` parse failures become `notMod` records; they are not a launch-blocking diagnostic.
- `InstalledModMatcher` is installed detection/Auto Dependencies duplicate guarding, not conflict repair.
- `GameLaunchFlow` checks account and game integrity only; it does not validate the mod graph before launch.
- Old ALL contains a separate `ModDoctor.kt`, `ModDoctorPlanSolver.kt`, `SodiumIrisConflictRegressionTest.kt`, and `InstanceBackupManager.kt` under `D:\\All New`; these are reference-only and were not ported.

### Exact Next Action
`MICRO-RUN NEXT: implement and verify a report-only pre-launch mod validation gate for one real Fabric instance; detect missing required dependencies, wrong Minecraft/loader, duplicate logical mods, and unreadable/corrupt mod files, with no automatic repair in that run.`

## MICRO-RUN — Mod Doctor V1 REPORT-ONLY — PASS

Timestamp: `2026-09-16 16:58:47 +0700`.

### Flow / insertion point
- Traced the launch button in `ui/screens/content/elements/LauncherElements.kt`.
- Existing path is `LaunchGameOperation.TryLaunch` → account/game checks → `RealLaunch` → `LaunchGameViewModel.start()` → `GameLaunchFlow` → `runGame()` / `Launcher.launch()`.
- Inserted one shared `ModDoctor().inspect(version)` pass immediately before `RealLaunch` is emitted, before `GameLaunchFlow` and before the Minecraft process starts.
- Clean report continues through the unchanged `RealLaunch` path.
- Non-clean report enters a temporary `ModProblems` operation; Cancel clears it, and Launch Anyway transitions directly to the existing `start()` path for that attempt only. No global bypass state exists.

### Reused upstream components
- `AllModReader.readAllLocals()` for current-instance local mod discovery and existing reader behavior.
- `LocalMod` / `isEnabled()` semantics and the existing active mod extensions (`jar`, `zip`, `litemod`).
- Existing `ModLoader`, `Version`, `VersionInfo`, `VersionFolders.MOD`, and `GameVersionNumber` models/utilities.
- Existing Compose dialog/button infrastructure in `LauncherElements.kt`.
- No Modrinth/CurseForge resolver, download queue, repair planner, backup, or `ModDoctorPlanSolver` was added.

### Detection rules implemented
- `MISSING_REQUIRED_DEPENDENCY`: Fabric `depends` entries excluding environment keys (`minecraft`, `fabricloader`, `java`, `forge`, `neoforge`) are checked against enabled top-level and embedded Fabric mod IDs.
- `WRONG_LOADER`: readable local metadata loader is compared with the selected instance loader.
- `WRONG_MINECRAFT_VERSION`: clear Fabric `depends.minecraft` constraints and Forge old `mcmod.info` `mcversion` values are checked; unsupported/ambiguous syntax is not asserted.
- `DUPLICATE`: multiple enabled readable local files with the same logical mod ID are reported.
- `CORRUPT_METADATA`: unreadable/unsupported metadata from an active mod candidate is reported without crashing the validator; disabled/non-mod-extension entries are ignored.
- Embedded Fabric JAR IDs and Fabric trailing/prerelease range syntax are handled to avoid false positives on the known-good instance.
- Behavior is strictly report-only: no file write, delete, rename, replacement, download, disable, version change, or backup is performed by Mod Doctor.

### Files changed in this micro-run
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/version/mod/ModDoctor.kt`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/elements/LauncherElements.kt`
- `ZalithLauncher/src/main/res/values/strings.xml`
- `ZalithLauncher/src/test/java/com/movtery/zalithlauncher/game/version/mod/ModDoctorTest.kt`
- This checkpoint entry only; `D:\\All New` and `D:\\All Launcher` were not modified.

### Regression check
- Selector: `ModDoctorTest` (`embeddedRequiredDependencyIsNotReportedMissing`, `ambiguousMinecraftRangeIsNotReported`, `reportsMissingRequiredDependency`).
- Result: `BUILD SUCCESSFUL`; all 3 tests passed, `failures=0`, `errors=0` in the focused JUnit run.
- `git diff --check`: `PASS`; new ModDoctor/test files also passed whitespace checks.

### Build / package
- Command from `D:\\ALL001`: `:ZalithLauncher:assembleDebug -Darch=x86_64 --no-daemon --console=plain`.
- Result: `BUILD SUCCESSFUL in 3m 18s`.
- APK: `D:\\ALL001\\ZalithLauncher\\build\\outputs\\apk\\debug\\ALLLauncher-Debug-0.0.1-x86_64.apk`.
- SHA-256: `2ca849b293da95386b17710cb3224fc885878da7c92f84176c45ecfaa4d99379`.
- `adb install -r` on `127.0.0.1:5555`: `Success`.
- Package verified: `com.all.launcher.app.debug`.

### Real QA
- `TEST A — CLEAN`: PASS. Restored original instance reached `VMActivity` and the Minecraft Java Edition main menu for `Minecraft 1.21.11 / Fabric 0.19.5`; no Mod Problems or crash dialog. Evidence: `test-artifacts/mod-doctor-v1/28-final-clean-settled.png`, `28-final-clean-activity.txt`.
- `TEST B — MISSING REQUIRED DEPENDENCY`: PASS. Installed real `Continuity 3.0.1-beta.1+1.21.11` through the existing Modrinth UI; temporarily moved only `fabric-api-0.141.6+1.21.11.jar` to `.moddoctor-disabled`. Report showed exactly one problem identifying `Continuity` and missing `fabric-api`; resumed activity remained `MainActivity`, so Minecraft did not start. Evidence: `20-missing-report.png`, `20-missing-activity.txt`.
- `Cancel`: PASS. Upper Cancel action returned safely to `MainActivity`; no Minecraft activity started. Evidence: `24-after-cancel.png`, `24-after-cancel-activity.txt`.
- `Launch Anyway`: PASS. Lower action bypassed only the report for that attempt and entered the existing launch flow; after 12s it reached existing `ErrorActivity` caused by the intentionally missing Fabric API, with no immediate Mod Problems loop. Evidence: `22-launch-anyway.png`, `22-launch-anyway-activity.txt`.
- `RESTORE`: PASS. Continuity was moved to `files/moddoctor-v1-backup` (not deleted), Fabric API was restored to its original `.jar` name, and the active mods directory returned exactly to `fabric-api`, `modmenu`, and `placeholder-api`. Final clean launch reached the Minecraft main menu.
- No automatic repair behavior exists in this milestone.

### Minimal QA evidence
- `test-artifacts/mod-doctor-v1/20-missing-report.png`
- `test-artifacts/mod-doctor-v1/20-missing-activity.txt`
- `test-artifacts/mod-doctor-v1/24-after-cancel.png`
- `test-artifacts/mod-doctor-v1/24-after-cancel-activity.txt`
- `test-artifacts/mod-doctor-v1/22-launch-anyway.png`
- `test-artifacts/mod-doctor-v1/22-launch-anyway-activity.txt`
- `test-artifacts/mod-doctor-v1/28-final-clean-settled.png`
- `test-artifacts/mod-doctor-v1/28-final-clean-activity.txt`

### Exact Next Action
`MICRO-RUN NEXT: add pure regression coverage for Mod Doctor V1 wrong-loader, wrong-Minecraft-version, duplicate, and corrupt-metadata findings; do not add repair or change the launch UX.`

## MICRO-RUN — verify Mod Doctor V1 landed state + complete regression coverage — PASS

Timestamp: `2026-09-16 18:43:00 +0700`.

### Previous patch warning
- The prior attempted patch used the invalid duplicated path `D:\\ALL Launcher 0.0.1\\Zalith Launcher 0.0.1\\...`.
- It was harmless: the real project path is `D:\\ALL Launcher 0.0.1\\ZalithLauncher\\`, and the intended production integration was already present there.
- No missing production change was hidden by that failed attempt.

### Actual source integrity
- Verified repository root: `D:\\ALL Launcher 0.0.1`.
- Verified branch: `all-0.0.1`.
- `git status --short` shows the pre-existing ALL dirty-tree files plus the expected Mod Doctor files; no reset, clean, or unrelated rollback was performed.
- Actual diff confirms `LauncherElements.kt` contains:
  - `ModDoctor().inspect(version)` at the existing `TryLaunch` path before `RealLaunch`.
  - clean reports continuing to unchanged normal launch.
  - `ModProblems` state and `ModProblemsDialog`.
  - Cancel → `LaunchGameOperation.None`.
  - Launch Anyway → existing `RealLaunch` / `start()` path with no persistent bypass.
- Actual files found in the real source path:
  - `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/version/mod/ModDoctor.kt`
  - `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/elements/LauncherElements.kt`
  - `ZalithLauncher/src/main/res/values/strings.xml`
  - `ZalithLauncher/src/test/java/com/movtery/zalithlauncher/game/version/mod/ModDoctorTest.kt`

### Production correction
- `None.`
- `LauncherElements.kt`, `ModDoctor.kt`, and `strings.xml` were not edited in this verification/coverage micro-run.
- No automatic repair, Fix button, download, replacement, delete, disable, solver, backup, or rollback behavior was added.

### Regression coverage
- Preserved existing checks:
  - missing required dependency
  - embedded required dependency is not falsely reported missing
  - ambiguous Minecraft range is not asserted
- Added pure checks for:
  - wrong loader
  - clearly incompatible Minecraft version
  - duplicate enabled mod IDs
  - unreadable/corrupt metadata
- Added only test resources required by upstream `GameVersionNumber` for JVM execution:
  - `ZalithLauncher/src/test/resources/assets/game/versions.txt`
  - `ZalithLauncher/src/test/resources/assets/game/version-alias.csv`
- Test result: `BUILD SUCCESSFUL in 1m 05s`.
- JUnit XML: `tests=7`, `skipped=0`, `failures=0`, `errors=0`; all seven `ModDoctorTest` cases passed.
- `git diff --check`: `PASS`.
- No APK rebuild or reinstall was performed because production source was already complete and only tests/test resources changed in this micro-run.

### Exact Next Action
`MICRO-RUN NEXT: perform one reversible LDPlayer QA scenario for a non-dependency detector (wrong loader) using the existing verified APK; do not edit production source or add repair behavior.`

## MICRO-RUN — Mod Doctor V1 wrong-loader real-device QA — PASS

Timestamp: `2026-09-16 19:03:08 +0700`.

### Result
- PASS on LDPlayer `127.0.0.1:5555` using package `com.all.launcher.app.debug`.
- Selected instance remained `1.21.11 Fabric 0.19.5`.
- LDPlayer ownership was verified: `Ld9BoxHeadless.exe` owned TCP port `5555`.

### Disposable test mod and provenance
- Final fixture: `balm-neoforge-1.21.11-21.11.9.1.jar`.
- Source: Modrinth project `balm` (`MBAkmtvl`), release `21.11.9.1+neoforge-1.21.11`, version ID `tKWaiIeW`.
- URL: `https://cdn.modrinth.com/data/MBAkmtvl/versions/tKWaiIeW/balm-neoforge-1.21.11-21.11.9.1.jar`.
- JAR metadata: `META-INF/neoforge.mods.toml`, `modId=balm`, explicit NeoForge dependency, Minecraft range `[1.21.11,)`.
- SHA-1: `4def274a463ebc949319305d0177c3e9d84d7782`.
- SHA-256: `af4993fd006b6f06f263a8e1233c0a799e79922edcb97f0af96829d9f29a48fc`.
- A preliminary Forge Balm candidate was rejected because the existing reader reported it as corrupt metadata; it was removed before the valid NeoForge fixture was tested and is not counted as the result.

### Detection and launch gate
- Added exactly one temporary JAR to the active mods directory.
- Real ALL Launcher UI launch produced exactly one report problem:
  `Wrong mod loader: Balm declares NeoForge, but this instance uses Fabric.`
- The affected file was identified as `balm-neoforge-1.21.11-21.11.9.1.jar`.
- Dialog actions were Cancel and Launch Anyway only.
- Launch Anyway was not selected.
- Before user action, resumed activity was `MainActivity`; Minecraft/`VMActivity` did not start.

### Cancel and restoration
- Cancel dismissed the report and returned safely to the ALL Launcher main screen.
- Resumed activity remained `MainActivity`.
- No `FATAL EXCEPTION`, crash marker, or `VMActivity` marker was found in captured log markers.
- Disposable JAR was removed.
- Final mods directory contained exactly the original three files:
  - `fabric-api-0.141.6+1.21.11.jar`
  - `modmenu-17.0.0.jar`
  - `placeholder-api-2.8.2+1.21.10.jar`
- All three final SHA-1 values matched the initial snapshot; no disabled/test JAR remained.

### Source/status/build
- No production source or tests were edited during this QA micro-run.
- No APK rebuild/reinstall was performed.
- Existing pre-run ALL Launcher working-tree changes were left untouched.
- Final `git diff --check`: `PASS`.
- Final branch/status: `all-0.0.1`; existing source changes plus QA evidence/checkpoint files remain as the pre-existing working tree state.

### Evidence
`D:/ALL Launcher 0.0.1/test-artifacts/mod-doctor-v1/wrong-loader/14-qa-summary.txt` and the referenced inventory, provenance, hierarchy, activity, log-marker, and screenshot files.

### Exact Next Action
`MICRO-RUN NEXT: perform one reversible LDPlayer QA scenario for Mod Doctor V1 clearly incompatible Minecraft-version detection; do not edit production source, tests, or launch UX.`

## MICRO-RUN — Mod Doctor V2 Fast QA APK build + provenance — PASS

Timestamp: `2026-09-16 23:44:02 +0700`.

### Result
- PASS: x86_64 non-minified debug APK built from alias `D:\ALL001`, which resolves to the target repository `D:\ALL Launcher 0.0.1`.
- Build command: `:ZalithLauncher:assembleDebug -Darch=x86_64 --no-daemon --console=plain`.
- Wrapper output: `BUILD SUCCESSFUL in 36s`; follow-up task state was `:ZalithLauncher:assembleDebug UP-TO-DATE`.
- Debug configuration: `isMinifyEnabled = false`; no R8/minify gate used.

### Artifact verification
- APK: `D:/ALL Launcher 0.0.1/ZalithLauncher/build/outputs/apk/debug/ALLLauncher-Debug-0.0.1-x86_64.apk`.
- Size: `233267954` bytes.
- File mtime: `2026-09-16 21:51:00 +0700`.
- SHA-256: `b2456ddc6db4ee7331999940f22263726aa38c2bfbd4c28412651df36bec2faa`.
- ZIP integrity: PASS (`unzip -tqq`).
- Packaged ABI: `x86_64` only.
- Package metadata: `com.all.launcher.app.debug`, versionCode `1`, versionName `0.0.1-debug`, label `ALL Launcher`.

### Regression/source provenance
- Current `ModDoctorTest` XML: `tests=10`, `skipped=0`, `failures=0`, `errors=0`.
- Source hashes after build matched the pre-build snapshot:
  - `ModDoctor.kt`: `4bc78f7789bc67b59f803c248772bdf0d05497217b1c0b8566bb53d37bab265a`
  - `_Download.Dependency.Tasks.kt`: `8a922b5419e031608ef24cebd66e8da5b6be2711b6e6caf1d6833d68356233f4`
  - `LauncherElements.kt`: `314a0ba529c3f8ad40ac545f50bc9c0991eb87a1e9483288ef86961ac4f65d59`
  - `strings.xml`: `1135016982f16075be1bdc6d1f76b2d8b2b9fae07f6a0c11a490b575a4335b1b`
  - `ModDoctorTest.kt`: `c58a87c22618d7271a03f3af0b2faa9bb3aaed7516bc357a620c9633c3bb0181`
- `git diff --check`: PASS.
- No secrets were recorded; Gradle secret-presence warnings were not copied into evidence.

### Scope boundary
- APK build/provenance only. APK was not installed and LDPlayer E2E was not started in this micro-run.

### Exact Next Action
`MICRO-RUN NEXT: install the verified x86_64 debug APK on LDPlayer 127.0.0.1:5555 and perform the single controlled Mod Doctor V2 missing-Fabric-API Fix E2E; snapshot and restore the 1.21.11 Fabric mods directory, and stop at the first production-flow divergence.`

## MICRO-RUN — Mod Doctor V2 missing-dependency Fix E2E — FAIL

Timestamp: `2026-09-16 23:58:03 +0700`.

### APK and environment
- APK SHA-256 verified before QA: `b2456ddc6db4ee7331999940f22263726aa38c2bfbd4c28412651df36bec2faa`.
- Installed with `adb -s 127.0.0.1:5555 install -r`: `Success`.
- Package verified: `com.all.launcher.app.debug`, versionName `0.0.1-debug`.
- LDPlayer endpoint verified: `Ld9BoxHeadless.exe` PID `8028` owned TCP `5555`; only target serial was used for QA.
- Launcher start verified: `MainActivity`, account `ALLQA001`, instance `1.21.11 Fabric 0.19.5`.

### Controlled state
- Initial active mods snapshot: exactly three JARs with SHA-1 captured in `01-initial-inventory.txt`.
- Continuity fixture: `continuity-3.0.1-beta.1+1.21.11.jar`, Modrinth project `1IjD5062`, version `mX1iknM1`, SHA-1 `51d34c3f2035db0c7fca321b2d94f01b5167707c`.
- Fixture metadata: Fabric client, requires `fabric-api >=0.140.0`, no embedded Fabric API.
- Controlled mutation changed only the test condition: Continuity active and original Fabric API removed; `modmenu` and `placeholder-api` hashes stayed unchanged.

### First behavioral divergence
- After asset startup completed, Launch produced the real Mod Doctor report before Minecraft.
- Report correctly identified one problem and Continuity:
  `Missing required dependency: Continuity requires fabric-api, but no enabled installation was found. Automatic repair is unavailable: no authoritative compatible provider was resolved.`
- Expected `Fix` action was absent; hierarchy assertions: `Mod Problems=True`, `Continuity=True`, `fabric-api=True`, `Automatic repair is unavailable=True`, `Fix text=False`.
- Report actions were only `ยกเลิก` and `ใช้ไปก่อนเถอะ เนอะ`.
- Minecraft was not started: resumed activity remained `MainActivity`; no `VMActivity`, Minecraft activity, or game process.
- Per stop-at-first-divergence rule, Fix and Launch Anyway were not pressed; no dependency was downloaded/installed and no post-fix Mod Doctor recheck or Main Menu result was attempted.
- AppLog evidence records `PlatformSearch` attempting Official Modrinth, then a mirror search timeout. This is boundary evidence only, not a final root-cause conclusion.

### Restore verification
- Restored original Fabric API from a hash-verified backup and removed Continuity.
- Final active file set exactly matched the initial three filenames and SHA-1 values.
- Continuity absent; no temporary/test file remained.
- No game process remained after restore; package was force-stopped safely.

### Source and evidence
- QA made no production-source or test-source edits; existing pre-QA dirty-tree changes remained unchanged.
- `git diff --check`: PASS.
- Evidence directory:
  `D:/ALL Launcher 0.0.1/test-artifacts/mod-doctor-v2/missing-dependency-fix-e2e/`
- Key evidence: `01-initial-inventory.txt`, `04-controlled-missing-dependency-state.txt`, `07-ready-ui.xml`, `08-divergence-report-ui.xml`, `08-divergence-report.png`, `08-divergence-log-markers.txt`, `09-final-restore-inventory.txt`.

### Exact Next Action
`MICRO-RUN NEXT: investigate ModDoctor authoritative provider resolution at the PlatformSearch boundary using required project P7dR8mSH and the captured timeout evidence; add no device behavior or APK build until a focused resolution test proves the candidate path.`

## MICRO-RUN — Mod Doctor V2 provider-resolution boundary fix (RED→GREEN, focused tests) — PASS

Timestamp: `2026-09-17 02:47:00 +0700`.

### Scope
- Source fix + focused tests only. No APK build, no install, no LDPlayer, no device QA, and no Mod Doctor UI change.
- Baseline before editing: `ModDoctor.kt` SHA-256 `4bc78f7789bc67b59f803c248772bdf0d05497217b1c0b8566bb53d37bab265a`, `ModDoctorTest.kt` `c58a87c22618d7271a03f3af0b2faa9bb3aaed7516bc357a620c9633c3bb0181`, `ModrinthSearcher.kt` `8270e0dd59b5812cbdb6d8106d9b2f1d510078a89840b61a95c4b5ff293f5577`, `CurseForgeSearcher.kt` `77e3a9033c1c9e772d15b525362e8b0603c1a487cf017f8f611cb83937195837`.

### STEP 1 — exact RED failure (before any production behavior change)
- Seams added first with production-preserving defaults; production resolution was still search-first at this point.
- Command: `:ZalithLauncher:testDebugUnitTest --tests ...ModDoctorTest.missingDependencyResolvesToAuthoritativeProjectWithoutBroadSearch --tests ...ModDoctorTest.missingDependencyResolutionDoesNotUseProviderSearch --tests ...ModDoctorTest.unknownDependencyStaysUnresolved --tests ...ModDoctorTest.providerLookupTimeoutIsNotReportedAsMissingProject --no-daemon --console=plain`.
- Result: `BUILD FAILED in 4m 8s`; JUnit XML `tests=4 failures=3 errors=0 skipped=0`.
- Exact failure evidence:
  - `missingDependencyResolvesToAuthoritativeProjectWithoutBroadSearch` → `java.lang.AssertionError: expected:<[(fabric-api, MODRINTH)]> but was:<[]>` (the direct project lookup was never used; the resolver broad-searched first).
  - `missingDependencyResolutionDoesNotUseProviderSearch` → `java.lang.AssertionError` (the provider search was invoked).
  - `providerLookupTimeoutIsNotReportedAsMissingProject` → `java.lang.AssertionError` (a 30 s timeout was still folded into a successful search result, so no timeout reason existed).
  - Control `unknownDependencyStaysUnresolved` → PASS (unknown dependency stayed unresolved both before and after the fix).

### STEP 2 — root cause confirmed
- Divergence is only the **dependency identity** step: the working install path receives provider project IDs as data (`platformDependency.projectId` → `getProject(projectID=dependency.projectId)` at `DownloadAssetsScreen.kt:274`, `getVersions(projectID=request.projectId)` at `_Download.Dependency.Tasks.kt:335`), while Mod Doctor starts from the local Fabric mod ID in `fabric.mod.json` and invented that identity with `searchAssets` + `platformSlug()` equality (`ModDoctor.kt` old lines 417-440).
- The identities `P7dR8mSH` existed in the repo only as `FabricAPIVersions: ModVersions("P7dR8mSH")`, whose sole consumer is the game-install addon picker (`DownloadGameWithAddonScreen.kt:133`), with `modrinthID` `private` — no runtime path into Mod Doctor.
- Captured runtime failure confirmed as the version-list fetch, not the search: the only logged `PlatformSearch` failure in the Mod Doctor window is a `printLog=true` call, which in Mod Doctor is `getVersions(...)`; `23:55:32.600 → 23:56:02.612` = `30.012 s` against the app's own `TIME_OUT`/`callTimeout` of 30 s (`UrlManager.kt:43,163`), one source only (mirror is mainland-only), so no retry.
- Root cause of the timeout: `ModrinthSearcher.getVersions` requested every version of the project in one unbounded response. Measured 2026-09-17: `GET /project/P7dR8mSH/version` = `1,214,701 bytes / 1199 versions`, while `?limit=100` = `84,556 bytes / 100 versions` (14x smaller) and offset paging kept date-descending order.
- `GET /project/fabric-api` and `GET /project/P7dR8mSH` returned byte-identical `200` responses (`19,859 bytes`, id `P7dR8mSH`, slug `fabric-api`), so the direct id/slug lookup is authoritative for this dependency.

### Identity fix (STEP 2)
- `resolveFixCandidate` now resolves the dependency project with the direct provider lookup first (`getProjectByVersion(dependencyId, platform, printLog=false)`), then falls back to the existing search only when the platform explicitly answers "no such project".
- Reused existing upstream functions only (`getProjectByVersion`, `getVersions(...).initAll(...)`, `isCompatibleWithVersion`, `PlatformClasses.MOD`); no resolver, plan solver, cross-provider mapping, or ported architecture was added.

### Version lookup fix (STEP 3) — shared pager reused, no arbitrary one-page truncation
- `ModrinthSearcher.getVersions` now pages through the existing chunked pager `getAllVersions` (`CurseForgeSearcher.kt`, visibility widened `private` → `internal`, zero behavior change for CurseForge) using the existing `getVersionsChunk(projectID, pageSize, offset)` with one page = `100` versions.
- Callers were enumerated before the change: the only caller of `getVersionsChunk` is `ModrinthSearcher.getVersions` itself; consumers of `getVersions` are Mod Doctor, `ModVersions` (Fabric/Quilt/Legacy API lists), `ModUpdater`/`ModData`, and the download UI, and all of them still receive the complete list in the same descending order (verified by the paging regression).
- CurseForge behavior unchanged (its `getVersions` still calls the same pager with its own `pageSize = 50, chunkSize = 20, maxConcurrent = 10`).

### Error handling (STEP 4)
- New internal `ModDoctorFixResolution(candidate, unavailableReason)`; a missing dependency without a candidate now carries a distinct reason instead of one shared sentence.
- Distinct outcomes preserved: `Automatic repair is unavailable: the MODRINTH provider lookup failed (timeout).`, `... the dependency project fabric-api was not found on MODRINTH.`, `... no Fabric API release supports Minecraft 1.21.11 with the Fabric loader.`, `... the resolved MODRINTH version does not belong to <projectId>.`
- Timeout detection walks the cause chain for `InterruptedIOException`/`TimeoutException` (the captured shape was a ktor `ClosedByteChannelException` wrapping `InterruptedIOException: timeout`).
- The UI-asserted prefix `Automatic repair is unavailable` is preserved verbatim; the Mod Doctor dialog and its launch actions were not changed.

### Changed files
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/version/mod/ModDoctor.kt` (untracked): seams + reasons + direct-first identity; SHA-256 `1dcf1975552bd60892f1880904281afdf9e5afc988eb970afc6289d6e54578d3`.
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/download/assets/platform/modrinth/ModrinthSearcher.kt`: paged version fetch, `+31/-4`; SHA-256 `b707c059ead89f77ab8cecfd7362e8f7075e2f1de437c8c3ed6d15d32d892c4f`.
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/download/assets/platform/curseforge/CurseForgeSearcher.kt`: `getAllVersions` `private` → `internal` only; SHA-256 `16d9bac9d7b97475d41be5bad8442f07045d26aa2ac404c6d6d844552302e607`.
- `ZalithLauncher/src/test/java/com/movtery/zalithlauncher/game/version/mod/ModDoctorTest.kt`: 4 new tests + provider stubs; SHA-256 `fae5f197d06b55deffdb5c500d13f89719302955feb764026d9d4366e60507c3`.
- `ZalithLauncher/src/test/java/com/movtery/zalithlauncher/game/download/assets/platform/modrinth/ModrinthVersionPagingTest.kt` (new): SHA-256 `ef9c54b58c8cd0f70346a117d980df49bbd712510d3f68e708432f2709096dbc`.
- Untouched and verified by hash: `_Download.Dependency.Tasks.kt` `8a922b54...` and `LauncherElements.kt` `314a0ba5...` (same as the previous checkpoint record), so the working install path and the Mod Doctor UI are unchanged. Both patched upstream files kept CRLF endings.

### Tests
- Focused + complete suite: `:ZalithLauncher:testDebugUnitTest --tests ...ModDoctorTest --tests ...ModrinthVersionPagingTest --no-daemon --console=plain` → `BUILD SUCCESSFUL in 2m 1s`.
- JUnit XML: `ModDoctorTest tests=14 failures=0 errors=0 skipped=0`; `ModrinthVersionPagingTest tests=2 failures=0 errors=0 skipped=0`; aggregate `tests=16 failures=0 errors=0 skipped=0`, timestamp `2026-09-16T19:45:51Z`.
- Newly green: `missingDependencyResolvesToAuthoritativeProjectWithoutBroadSearch` (candidate `projectId=P7dR8mSH`, platform `MODRINTH`, direct lookup called with the exact `(fabric-api, MODRINTH)` tuple), `missingDependencyResolutionDoesNotUseProviderSearch`, `unknownDependencyStaysUnresolved`, `providerLookupTimeoutIsNotReportedAsMissingProject`; all 10 pre-existing tests still pass.
- `git diff --check`: exit `0` (no whitespace errors). Untracked new files were whitespace-checked with `git diff --no-index --check /dev/null <file>`: no whitespace errors, only the benign `LF will be replaced by CRLF` autocrlf warning.
- No APK was built; no LDPlayer/device QA was performed.

### Scope boundary / limits
- The seam fakes are a provider-boundary replay, not a live provider run; the production defaults (`getProjectByVersion`, `searchProjectBySlug`, `getVersions(...).initAll(...)`, `AllSettings.searchModPlatform`) are unexercised by JVM tests.
- The CurseForge fallback path (direct lookup answers "no such project" → search) is covered only by the `unknownDependencyStaysUnresolved` control; no CurseForge-specific test was added.
- Reproducing the exact 30 s stall was not attempted; the fix bounds each request to one page instead.

### Exact Next Action
`MICRO-RUN NEXT: build the Fast QA x86_64 debug APK from D:\ALL001 with provenance and verify the packaged Mod Doctor V2 fix; do not install or run LDPlayer in that run.`

## MICRO-RUN — Mod Doctor V2 Fast QA APK rebuild + packaged-fix provenance — PASS

Timestamp: `2026-09-17 02:53:11 +0700`.

### Result
- PASS: x86_64 non-minified debug APK built from alias `D:\ALL001` (resolves to Git root `D:\ALL Launcher 0.0.1`), containing the Mod Doctor V2 provider-resolution fix.
- No production source, test, or device state was modified in this run.

### Source provenance (before build)
- Branch: `all-0.0.1`; HEAD: `b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e` (`bump version to 2.5.3`).
- Working tree: the same 15 tracked modifications plus the untracked ALL files recorded in the previous micro-run; no new file appeared.
- Verified source SHA-256 (pre-build = post-build, unchanged):
  - `ModDoctor.kt` `1dcf1975552bd60892f1880904281afdf9e5afc988eb970afc6289d6e54578d3`
  - `ModrinthSearcher.kt` `b707c059ead89f77ab8cecfd7362e8f7075e2f1de437c8c3ed6d15d32d892c4f`
  - `CurseForgeSearcher.kt` `16d9bac9d7b97475d41be5bad8442f07045d26aa2ac404c6d6d844552302e607`
  - `ModDoctorTest.kt` `fae5f197d06b55deffdb5c500d13f89719302955feb764026d9d4366e60507c3`
- Confirmed unchanged from their previously verified state: `_Download.Dependency.Tasks.kt` `8a922b54…`, `LauncherElements.kt` `314a0ba5…`.
- Fix wiring present in production source: `resolveFixCandidate` direct-first (`ModDoctor.kt:458`), `lookupProjectDirect` → `getProjectByVersion(projectIdOrSlug, platform, printLog = false)` (`:507-509`), Modrinth paging call site (`ModrinthSearcher.kt:92-96`, `:107-121`), shared pager `internal suspend fun <E, T> getAllVersions` (`CurseForgeSearcher.kt:180`).
- Debug build config: `debug { isMinifyEnabled = false; applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }`.

### Build
- Command: `gradlew.bat :ZalithLauncher:assembleDebug -Darch=x86_64 --no-daemon --console=plain` executed via `cmd.exe /d /c "cd /d D:\ALL001 && ..."`.
- Result: `BUILD SUCCESSFUL in 2m 7s`; `123 actionable tasks: 15 executed, 108 up-to-date`; exit code `0`.
- Wall clock: first build event `2026-09-17 02:49:58 +0700` → `BUILD SUCCESSFUL` / APK mtime `02:52:11`.
- Task graph contained no R8/minify/shrink task (only `dexBuilderDebug`, `mergeProjectDexDebug`, `packageDebug`, `assembleDebug`); x86_64-only selection confirmed by `[JREAssetsCleanup] arch: x86_64`, removal of non-target-arch LWJGL natives, and `configureNdkBuildDebug[x86_64]` / `buildNdkBuildDebug[x86_64]`.
- Pre-existing non-fatal NDK warning (`process_begin: CreateProcess(... rm ...libawt_headless.so) failed` / `pipe: No error`) was present; it did not fail the build. Build-key environment warnings were recorded as presence/absence only; no secret value was read or stored.

### APK
- Path: `D:\ALL Launcher 0.0.1\ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.1-x86_64.apk`.
- Size: `233267954` bytes; file mtime `2026-09-17 02:52:11.588053300 +0700`.
- SHA-256: `123b1ab3d97b7de9fb164d212dd2561de5d9cce1498d2ec1e67c4ca7089bc485`.
- Pre-build artifact (superseded): SHA-256 `b2456ddc6db4ee7331999940f22263726aa38c2bfbd4c28412651df36bec2faa`, mtime `2026-09-16 21:51:00` — the new hash differs, and the new mtime is after this run's first build event.
- ZIP integrity: PASS (`unzip -tqq` exit `0`).
- `aapt dump badging`: package `com.all.launcher.app.debug`, versionCode `1`, versionName `0.0.1-debug`, label `ALL Launcher`, `native-code: 'x86_64'`.
- Packaged ABI: only `lib/x86_64` (no arm/armeabi/x86 entries).
- Debug / non-minified: debug build type (`isMinifyEnabled = false`, `.debug` id and `-debug` version suffix) and no minify task in the graph.

### Fix provenance (packaged markers)
- Extracted the APK dex payload (`classes*.dex`, `85586388` bytes, temporary file removed afterwards) and searched for the fixed source literals:
  - NEW reason literals present: `was not found on` count `2`, `provider lookup failed (` count `1`, `version lookup for` count `1`, `does not belong to` count `4`.
  - CONTROL existing literals present: `no authoritative compatible provider was resolved.` count `1`, `Automatic repair is unavailable: ` count `1`.
  - Pre-fix-only single literal `Automatic repair is unavailable: no authoritative compatible provider` count `0` — the old search-first reason text is not packaged.
- Paging is bounded by the shared pager reuse and is covered by the source-hash/build-input chain plus the `ModrinthVersionPagingTest` result, not by a packaged string marker.

### Final checks
- `git diff --check`: exit `0`.
- Source hashes after the build are identical to the pre-build snapshot; test sources and test results unchanged (JUnit XML still timestamped `02:45:52`, i.e. the previous GREEN run, not re-run here).
- Tracked-file list is identical to the pre-run baseline; no source or test file was modified during this run.
- No `adb` command was executed and no LDPlayer package/process/state was touched; no APK was installed.

### Exact Next Action
`MICRO-RUN NEXT: install the verified x86_64 debug APK (SHA-256 123b1ab3d97b7de9fb164d212dd2561de5d9cce1498d2ec1e67c4ca7089bc485) on LDPlayer 127.0.0.1:5555 and perform the controlled Mod Doctor V2 missing-fabric-api Fix E2E: snapshot the 1.21.11 Fabric mods directory with per-file hashes, verify the dependency then installs authoritatively (P7dR8mSH) through the Fix action, run Mod Doctor a second time and require a true no-op, and stop at the earliest production-flow divergence without editing source or rebuilding.`

## MICRO-RUN — Mod Doctor V2 missing-dependency Fix E2E (final, real device) — PASS

Timestamp: `2026-09-17 03:07:00 +0700`.

### APK / environment
- APK SHA-256 verified before install: `123b1ab3d97b7de9fb164d212dd2561de5d9cce1498d2ec1e67c4ca7089bc485`.
- `adb connect 127.0.0.1:5555`; LDPlayer endpoint confirmed: `Ld9BoxHeadless.exe` PID `38880` owns TCP `5555`. Only serial `127.0.0.1:5555` was used; `emulator-5554` was not touched.
- `adb install -r` → `Success`; installed `com.all.launcher.app.debug`, versionCode `1`, versionName `0.0.1-debug`, `lastUpdateTime 2026-09-17 02:59:38`; data preserved.
- Baseline before mutation: `ALLQA001` (Offline) current, instance `1.21.11 Fabric 0.19.5`, `MainActivity` resumed, PID `3949`.

### Controlled failure
- Snapshot before: exactly `fabric-api-0.141.6+1.21.11.jar` (`c98467cb…`), `modmenu-17.0.0.jar` (`544a8b34…`), `placeholder-api-2.8.2+1.21.10.jar` (`bbdc920e…`); no disabled files.
- Fixture pushed: `continuity-3.0.1-beta.1+1.21.11.jar` (`51d34c3f2035db0c7fca321b2d94f01b5167707c`, hash verified on host and on device).
- Fabric API pulled to a hash-verified host backup and removed from the device. Resulting state: Continuity active, Fabric API absent, `modmenu`/`placeholder-api` unchanged.

### First Mod Doctor pass — Fix available
- Launch pressed through the real UI at `03:03:48`-era screen (tap derived from a fresh hierarchy, `(1351,834)`).
- Report shown: `Mod Problems` / `Minecraft launch paused. Found 1 mod problem(s).` / `Missing required dependency: Continuity requires fabric-api, but no enabled installation was found. File: continuity-3.0.1-beta.1+1.21.11.jar`.
- `Fix missing dependency` **visible**; `Automatic repair is unavailable` **absent**; actions were `ยกเลิก` and `ใช้ไปก่อนเถอะ เนอะ`.
- Minecraft did not start: `mResumedActivity` remained `MainActivity`.

### Authoritative resolution and install
- Fix pressed at `03:03:29`; provider version-list attempt `03:03:30.769` (`{Official Modrinth}`, zero warnings/timeouts).
- Direct device proof of the authoritative identity:
  `[03:03:31.264] [FileDownloader/INFO] Downloading: https://cdn.modrinth.com/data/P7dR8mSH/versions/6qAuTtLR/fabric-api-0.141.6%2B1.21.11.jar`
  → project `P7dR8mSH`, version `6qAuTtLR`, i.e. the newest Modrinth release compatible with Minecraft 1.21.11 + Fabric.
- Existing Zalith pipeline used: `[03:03:32.690] [DownloadSingle/INFO] Attempting to clear cached resource files.`
- Installed file: `fabric-api-0.141.6+1.21.11.jar`, size `2426039`, SHA-1 `c98467cbbaf4d197377266795ae015f4130d65b6` (identical to the original, authoritative file).
- Duplicate guard / no collateral change: mods directory contains Fabric API exactly once; `modmenu` and `placeholder-api` SHA-1 unchanged; only one dependency download in the whole Fix window.

### Second Mod Doctor pass / launch
- After the repair the app re-inspected and continued to launch: `VMActivity` became resumed, so `decideAfterModDoctorFix` resolved to `CONTINUE_LAUNCH` (a clean re-check).
- No second Mod Problems dialog, no repeated Fix loop, no second dependency download.
- Minecraft evidence (`.../versions/1.21.11 Fabric 0.19.5/logs/latest.log`, pulled):
  - `[03:03:43] Loading Minecraft 1.21.11 with Fabric Loader 0.19.5`
  - `[03:04:01] Setting user: ALLQA001`
  - `[03:04:03] Backend library: LWJGL version 3.3.3-snapshot`
  - `[03:04:12] OpenAL initialized on device OpenSL` / `Sound engine started` (Main Menu reached)
  - `Loading 51 mods:` including `continuity 3.0.1-beta.1+1.21.11`, `fabric-api 0.141.6+1.21.11`, `modmenu 17.0.0`, `placeholder-api 2.8.2+1.21.10`.
- No Minecraft crash lines; launcher fatal markers (`FATAL EXCEPTION` / `Fatal signal`) `0`. The only ERROR lines are pre-existing runtime warnings unrelated to mods (JNA/Narrator `libc.so.6` on LDPlayer).

### Restore
- App force-stopped; Continuity removed from the device after a hash-verified pull back (`51d34c3f…` matches the fixture).
- Final mods directory: exactly the original three files; `FILENAMES_MATCH: True`, `HASHES_IDENTICAL: True` (SHA-1 and MD5), no disabled/test/temporary file left.

### QA operator error (documented, not a product divergence)
- During STEP 7 I tapped a wrongly transcribed coordinate instead of the derived one, which hit the lower `ใช้ไปก่อนเถอะ เนอะ` (Launch Anyway) action. Product behavior was correct and is consistent with the earlier V1 finding: the report was bypassed for that attempt only, the normal launch path ran, and the game JVM exited `1` into `ErrorActivity` (dependency genuinely missing). Evidence kept as `07-mistap-launch-anyway-*.{xml,txt}`.
- The scenario was then repeated from a clean main screen with coordinates derived programmatically from the hierarchy; the mods directory was unchanged by the mis-tap.

### Observation (benign, pre-existing, out of scope)
- One log line during the Fix window: `[03:03:36.229] [BatchDownloader/WARNING] Completed-count mismatch: requests=0 finished=4651 failures=all` (only occurrence in the buffer).
- Source: `BatchDownloader.kt:107-113` compares the session-cumulative `stats.downloadedFiles` against the current batch size, so an empty batch logs this warning; no behavioral effect here (the dependency installed correctly and the verified hash matches). Not investigated or changed in this QA-only run.

### Source safety
- `git status --porcelain`: same 15 modified tracked files + untracked ALL files as the pre-run baseline; no source/test file added or changed during QA.
- `git diff --check`: exit `0`.
- APK hash after QA: `123b1ab3…` (unchanged).
- Fix source hashes after QA: `ModDoctor.kt` `1dcf1975…`, `ModrinthSearcher.kt` `b707c059…`, `ModDoctorTest.kt` `fae5f197…` (unchanged).
- No production source, test, or APK modification occurred in this run.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\missing-dependency-fix-final-e2e\`
- `01-baseline-ui.xml`, `02-mods-snapshot-before.txt`, `03-controlled-missing-dependency-state.txt`
- `09-first-report-ui.xml` (Fix visible), `12-fix-phase-log.txt`, `16-fix-phase-download-evidence.txt` (P7dR8mSH/6qAuTtLR URL)
- `11-mods-after-fix.txt`, `13-minecraft-latest.log`, `14-minecraft-main-menu.png`, `15-mod-restore-verified.txt`
- `fabric-api-original-backup.jar`, `continuity-fixture-returned.jar`, `07-mistap-launch-anyway-*`

### Exact Next Action
`MICRO-RUN NEXT: implement the smallest Mod Doctor V2 repair for the wrong-loader finding by reusing the same direct project lookup plus existing install pipeline (the repair path proven in this run), proven RED→GREEN with focused tests; no APK build, no device QA, no new detector types in that run.`

## MICRO-RUN — Mod Doctor V2 wrong-loader repair (RED→GREEN, focused tests) — PASS

Timestamp: `2026-09-17 03:32:00 +0700`.

### Scope
- Source + tests only. No APK build, no install, no adb/LDPlayer, no change to the missing-dependency repair or to the launch UX.
- Baseline before editing: `ModDoctor.kt` `1dcf1975552bd60892f1880904281afdf9e5afc988eb970afc6289d6e54578d3`, `LocalMod.kt` `?` (unmodified tracked file), `LauncherElements.kt` `314a0ba529c3f8ad40ac545f50bc9c0991eb87a1e9483288ef86961ac4f65d59`, `_Download.Dependency.Tasks.kt` `8a922b54…`.

### STEP 1 — authoritative installed-mod identity path reused (no new identity system)
- `scanModFingerprints(modsDir)` → `computeModFingerprints(file)` (SHA-1 for Modrinth, MurmurHash2 for CurseForge, with a size/mtime-validated memory cache) and `matchInstalledMods(fingerprints, platform)` → `MatchedInstalledMods` / `InstalledMod(platform, projectId, versionId, versionName, notFound)`.
- Mod Doctor now computes the fingerprint of the specific wrong-loader file and matches that single fingerprint (`matchInstalledMods(listOf(computeModFingerprints(file)), platform)`), so identity comes from the provider's own fingerprint database — never from the filename. No new matcher, cache, or heuristic was added.
- Version/loader filtering reuses the same predicate as missing-dependency repair (`PlatformVersion.isCompatibleWithVersion(minecraftVersion, loader)`), and the install reuses the same `downloadSingleForVersions(...)` path (the private helper was renamed `installDependency` → `installModFile` because it now serves both repairs).
- Enable/disable semantics were traced before editing: `LocalMod.disable()/enable()` (renaming to/from `.disabled`) is the launcher's existing reversible mechanism, called by `ModsManagerScreen.kt:753/763`.

### STEP 2 — RED evidence (before any wrong-loader repair existed)
- Plumbing only (new `ModDoctorWrongLoaderFix`, `ModDoctorProblem.wrongLoaderFix`, `hasFixableProblems` extension, `modsDir`/`installedMod` seams) was added first, with no resolution wired.
- Focused run: `:ZalithLauncher:testDebugUnitTest --tests ...ModDoctorWrongLoaderRepairTest --no-daemon --console=plain` → `BUILD FAILED in 4m 31s`; JUnit XML `tests=4 failures=3 errors=0`.
  - `neoForgeBuildInFabricInstanceBecomesFixableWithSameProjectFabricBuild` → `java.lang.AssertionError at ModDoctorWrongLoaderRepairTest.kt:43` (= `assertTrue(resolved.hasFixableProblems)`): a wrong-loader finding produced no fix candidate at all.
  - `wrongLoaderFileWithoutAuthoritativeProviderMatchStaysUnfixable` → failed on the missing "could not be matched to a provider project" reason (no wrong-loader reasoning existed).
  - `replacementCandidateFromAnotherProjectIsRejected` → failed on the missing "does not belong to" reason.
  - `wrongLoaderFileWithoutCompatibleLoaderBuildStaysUnfixable` → PASS (control).
- Evidence limitation, recorded honestly: the RED fixture contained a positional-argument defect (`stubVersion(project, name, "NeoForge")` bound the 3rd argument to `minecraftVersion`, not `loaders`). The RED failure cause is structural — the pre-fix code had no code path that could ever populate `wrongLoaderFix` — but the corrected fixture was not re-run against the pre-fix source.

### STEP 3 — replacement selection
- For `WRONG_LOADER` only: installed file → fingerprint-matched authoritative `InstalledMod` → query versions of that same `projectId` on that platform → `firstOrNull { isCompatibleWithVersion(minecraftVersion, loader) }` → the candidate must still report `platformProjectId() == installed.projectId`.
- Cross-project candidates are rejected; no candidate (unknown project, wrong loader, wrong Minecraft version) leaves the finding unfixable with a specific reason appended after the existing `Automatic repair is unavailable:` prefix.

### STEP 4 — safe replacement action
- `ModDoctor.fixProblems(version, report)` (renamed from `fixMissingRequiredDependencies`; the missing-dependency loop is byte-identical in behavior) runs wrong-loader repairs first: the original file is preserved reversibly via `File.disableModFile()` (rename to `.disabled`), then the compatible build is installed through `downloadSingleForVersions(...)` into the mods folder.
- Failure paths: if the file cannot be preserved → no state change, repair reported failed; if no compatible candidate is found → the file is re-enabled before returning; if the download/install fails → the original file is re-enabled and the repair is reported as failed. The original JAR is never deleted, and no backup subsystem was added.
- The helpers `File.disableModFile()` / `File.enableModFile()` were extracted from `LocalMod.disable()/enable()` (same `.disabled` rename semantics, `LocalMod` now delegates), so the reversible behavior is reused rather than re-implemented and is unit-testable.

### STEP 5 — post-fix recheck
- The launch flow is unchanged: after `fixProblems`, `ModDoctor().inspect(version)` runs again and `decideAfterModDoctorFix(...)` either continues the launch (clean) or shows the remaining problems — so a repaired wrong-loader finding cannot loop, and a replacement that is itself incompatible would be reported as a new finding rather than silently accepted.

### STEP 6 — tests
- Command: `:ZalithLauncher:testDebugUnitTest --tests ...ModDoctorWrongLoaderRepairTest --tests ...ModFileDisableSemanticsTest --tests ...ModDoctorTest --tests ...ModrinthVersionPagingTest --no-daemon --console=plain` → `BUILD SUCCESSFUL in 51s`.
- JUnit XML: `ModDoctorWrongLoaderRepairTest tests=4 failures=0 errors=0`; `ModFileDisableSemanticsTest tests=2 failures=0 errors=0`; `ModDoctorTest tests=14 failures=0 errors=0`; `ModrinthVersionPagingTest tests=2 failures=0 errors=0`; aggregate `tests=22 failures=0 errors=0 skipped=0`, timestamp `2026-09-16T20:30:14Z`.
- Newly green: NeoForge Balm in a Fabric instance resolves to the same authoritative project (`MBAkmtvl`) with a Fabric-compatible build and becomes fixable; an unmatched file stays unfixable; a project with no loader-compatible build stays unfixable; a candidate from another project is rejected; disabling/restoring a mod file preserves the original bytes and is idempotent.
- `git diff --check`: exit `0`. No APK built; no device QA performed.

### Changed files
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/version/mod/ModDoctor.kt` (untracked): `ModDoctorWrongLoaderFix`, problem/resolution field, wrong-loader resolution, `fixProblems` + reversible disable/restore, `installModFile` rename; SHA-256 `ed710c6b6b9380b3b4ca698dec9b8a0b697c10c44d316dd8b739be6221b716d0`.
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/version/mod/LocalMod.kt`: extracted `File.disableModFile()/enableModFile()`, `LocalMod.disable()/enable()` now delegate (60 lines moved, no behavior change for `ModsManagerScreen`); SHA-256 `e74d18a4c04fbaf66f87e365062429ae9bb3eb39f1b6ff05019fcf0e850ad414`.
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/elements/LauncherElements.kt`: 1-line call-site rename `fixMissingRequiredDependencies` → `fixProblems` (no UX change); file SHA-256 `f92d8e1d1caefced2f51566b86ef558b3ea37145ce47bd84a97c2f14f4a62f65` (the 165-line diffstat for this file is the earlier Mod Doctor V1 UI work, not this run).
- `ZalithLauncher/src/test/java/com/movtery/zalithlauncher/game/version/mod/ModDoctorWrongLoaderRepairTest.kt` (new): `af868f4d6414d6e1c1040378b386da79a201d895d6d573370c078224140432d6`.
- `ZalithLauncher/src/test/java/com/movtery/zalithlauncher/game/version/mod/ModFileDisableSemanticsTest.kt` (new): `a1d5a70bcdc3d982b8cde93e419e2efe71e3a67a2db19cd4baec796e647a7c85`.
- Untouched and verified by hash: `_Download.Dependency.Tasks.kt` `8a922b54…`, `ModrinthSearcher.kt` `b707c059…` (missing-dependency repair and the paged Modrinth fetch unchanged).

### Scope boundary / limits
- The wrong-loader repair is unit-proven only; it has not been exercised on a device, and the download/install + disable/restore integration is covered by code review plus the JVM disable/restore test, not by an end-to-end run.
- The fingerprint match uses the same platform source as missing-dependency repair (`AllSettings.searchModPlatform`), so a CurseForge-selected browse platform decides which provider is queried.
- No APK was built in this run, so the packaged fix is not yet verified.

### Exact Next Action
`MICRO-RUN NEXT: build the Fast QA x86_64 debug APK from D:\ALL001 with provenance for the wrong-loader repair, and verify the packaged source markers; do not install or run LDPlayer in that run.`

## MICRO-RUN — Wrong-loader repair Fast QA APK build + packaged provenance — PASS

Timestamp: `2026-09-17 11:42:00 +0700`.

### Result
- PASS: x86_64 non-minified debug APK built from alias `D:\ALL001` (Git root `D:\ALL Launcher 0.0.1`), containing the Mod Doctor wrong-loader repair.
- No production source, test, or device state was modified in this run.

### Source provenance (before build)
- Branch `all-0.0.1`; HEAD `b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e`.
- Pre-build = post-build SHA-256 (unchanged by the build):
  - `ModDoctor.kt` `ed710c6b6b9380b3b4ca698dec9b8a0b697c10c44d316dd8b739be6221b716d0`
  - `LocalMod.kt` `e74d18a4c04fbaf66f87e365062429ae9bb3eb39f1b6ff05019fcf0e850ad414`
  - `LauncherElements.kt` `f92d8e1d1caefced2f51566b86ef558b3ea37145ce47bd84a97c2f14f4a62f65`
  - `ModDoctorWrongLoaderRepairTest.kt` `af868f4d6414d6e1c1040378b386da79a201d895d6d573370c078224140432d6`
  - `ModFileDisableSemanticsTest.kt` `a1d5a70bcdc3d982b8cde93e419e2efe71e3a67a2db19cd4baec796e647a7c85`
- Previously completed systems confirmed untouched: `_Download.Dependency.Tasks.kt` `8a922b54…`, `ModrinthSearcher.kt` `b707c059…`.

### Wrong-loader wiring verified in production source (no edit needed)
1. Identity is authoritative: `ModDoctor.kt:732` `matchInstalledMods(listOf(computeModFingerprints(file)), platform)` → `:733` first non-`notFound` `projectId` (fingerprint database, never the filename).
2. Same-project guard: `:596` `replacement.platformProjectId() != installed.projectId` → rejected with a reason.
3. Compatibility filter: `:583` `isCompatibleWithVersion(minecraftVersion, loader)` (inspection) and `:237` `isCompatibleWith(version, PlatformClasses.MOD)` (install).
4. Reversible preserve: `:173` `target.disableModFile()` (existing `.disabled` rename semantics, `LocalMod.disable()` delegates to it at `LocalMod.kt:79`).
5. Failure restore: `:188` (no candidate) and `:201` (download/install failed) both call `preserved.enableModFile()` before reporting failure.
6. Missing-dependency repair still present: `:227/:241/:243/:252` `markFixFailed` paths and the `MISSING_REQUIRED_DEPENDENCY ->` branch at `:524`.
7. UI gate unchanged: `LauncherElements.kt:378` calls `fixProblems(...)`; `:678` still gates the Fix button on `report.hasFixableProblems` (now includes `wrongLoaderFix`).

### Build
- Command: `gradlew.bat :ZalithLauncher:assembleDebug -Darch=x86_64 --no-daemon --console=plain` via `cmd.exe /d /c "cd /d D:\ALL001 && ..."`.
- Result: `BUILD SUCCESSFUL in 3m 55s`; `123 actionable tasks: 15 executed, 108 up-to-date`.
- Wall clock: first build event `2026-09-17 11:36:59 +0700` → APK mtime `11:41:14`.
- x86_64-only confirmed: `[JREAssetsCleanup] arch: x86_64`, non-target-arch LWJGL natives removed, `configureNdkBuildDebug[x86_64]` / `buildNdkBuildDebug[x86_64]`. No R8/minify/shrink task in the graph (`debug { isMinifyEnabled = false }`).
- Pre-existing non-fatal NDK `CreateProcess ... rm ...` warning was present; build keys unset (presence/absence only, no secret value read or stored).

### APK
- Path: `D:\ALL Launcher 0.0.1\ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.1-x86_64.apk`.
- Size: `233267954` bytes; mtime `2026-09-17 11:41:14.480035100 +0700`.
- SHA-256: `077a679f26a8472eb469f500a3965b2f1a55a052638646f5a3aa352e835585b5` (previous artifact `123b1ab3…` superseded; the byte size is coincidentally the same, so the differing hash plus the newer mtime are the distinguishing evidence).
- ZIP integrity: PASS (`unzip -tqq` exit `0`).
- `aapt dump badging`: package `com.all.launcher.app.debug`, versionCode `1`, versionName `0.0.1-debug`, label `ALL Launcher`, `native-code: 'x86_64'`; packaged ABI entries only `lib/x86_64`.

### Packaged provenance (minimal, non-decompiling)
- Extracted `classes*.dex` (`85608360` bytes, temp file removed afterwards) and searched the string/method tables:
  - NEW present: `Could not preserve the wrong-loader mod file safely.` ×1, `No authoritative compatible replacement file was found.` ×1, `The replacement download/install failed.` ×1, `the installed file could not be matched to a provider project` ×2, `disableModFile` ×3, `enableModFile` ×3, `fixProblems` ×22.
  - OLD absent: `fixMissingRequiredDependencies` ×0 (also 0 occurrences anywhere in `ZalithLauncher/src`), and the pre-fix literal `Automatic repair is unavailable: no authoritative compatible provider` ×0.
- No speculative claim is made about behavior from markers alone; the markers only prove that the repaired source is the code packaged.

### Final safety check
- `git diff --check`: exit `0`.
- Post-build source hashes identical to the pre-build snapshot; test sources unchanged; JUnit XMLs still timestamped `03:30:14` (the previous GREEN run, not re-run here).
- Tracked-file list unchanged from this run's baseline (24 entries; the only difference from the earlier baseline is `LocalMod.kt`, modified by the previous wrong-loader micro-run).
- No `adb` command was executed and no LDPlayer package/process/state was touched. A leftover `adb.exe` **server** from the earlier device-QA session (PID `37464`, created `2026-09-17 02:58:09`) is still resident; it was not invoked by this run.

### Exact Next Action
`MICRO-RUN NEXT: install the verified x86_64 debug APK (SHA-256 077a679f26a8472eb469f500a3965b2f1a55a052638646f5a3aa352e835585b5) on LDPlayer 127.0.0.1:5555 and run the controlled wrong-loader Fix E2E with the Balm NeoForge fixture (project MBAkmtvl, version tKWaiIeW, SHA-1 4def274a463ebc949319305d0177c3e9d84d7782): snapshot the mods directory with hashes, confirm the Wrong Loader finding offers Fix, verify the original JAR is preserved as .disabled and the same project's Fabric build installs, require a clean second Mod Doctor pass and the Minecraft Main Menu, then restore the original mod state; stop at the earliest production-flow divergence without editing source or rebuilding.`

## MICRO-RUN — Wrong-loader repair E2E (final, real device) — PASS

Timestamp: `2026-09-17 11:49:00 +0700`.

### APK / environment
- APK SHA-256 gate passed before install: `077a679f26a8472eb469f500a3965b2f1a55a052638646f5a3aa352e835585b5`.
- LDPlayer endpoint: `Ld9BoxHeadless.exe` PID `38880` owns TCP `5555`; only serial `127.0.0.1:5555` used (`emulator-5554` untouched).
- `adb install -r` → `Success`; installed `com.all.launcher.app.debug`, versionCode `1`, versionName `0.0.1-debug`, `lastUpdateTime 2026-09-17 11:45:01`; app launched to `MainActivity`, PID `8019`.
- Baseline: `ALLQA001` (Offline) current, instance `1.21.11 Fabric 0.19.5`.
- Pre-run expectation verified against the provider API: the Balm project `MBAkmtvl` has 8 Fabric builds for 1.21.11; newest is version `5POKgjJn` / `balm-fabric-1.21.11-21.11.9.1.jar` / sha1 `5bb8ca8fb46953da5777c1b3c12af5b397b46b48`.

### Snapshot + controlled state
- Snapshot before: exactly `fabric-api-0.141.6+1.21.11.jar` (`c98467cb…`), `modmenu-17.0.0.jar` (`544a8b34…`), `placeholder-api-2.8.2+1.21.10.jar` (`bbdc920e…`); no disabled entries.
- Only the fixture was added: `balm-neoforge-1.21.11-21.11.9.1.jar`, device SHA-1 `4def274a463ebc949319305d0177c3e9d84d7782` (matches the expected authoritative hash); unrelated mods untouched.

### Wrong Loader detection (first pass)
- Launch through the real UI (`(1351,834)` derived from a fresh hierarchy) → report shown: `Mod Problems` / `Minecraft launch paused. Found 1 mod problem(s).` / `Wrong mod loader: Balm declares NeoForge, but this instance uses Fabric.` / `File: balm-neoforge-1.21.11-21.11.9.1.jar`.
- `Fix missing dependency` action was present; `Automatic repair is unavailable` absent; Minecraft did not start (`mResumedActivity` stayed `MainActivity`).

### Authoritative identity + replacement
- Fix pressed at `11:46:47` (coordinate `(800,546)` derived from the report hierarchy).
- Provider resolution during the report produced the expected single Modrinth lookup line (`11:46:10.009 [PlatformSearch/DEBUG] {Official Modrinth}`).
- Direct device proof of same-project replacement:
  `[11:46:45.750] [FileDownloader/INFO] Downloading: https://cdn.modrinth.com/data/MBAkmtvl/versions/5POKgjJn/balm-fabric-1.21.11-21.11.9.1.jar`
  → project `MBAkmtvl` (the fingerprint-matched project of the wrong-loader file), version `5POKgjJn` (newest Fabric build for 1.21.11). No cross-project replacement and no filename-based guessing.
- Installed through the existing pipeline (`11:46:46.969 [DownloadSingle/INFO] Attempting to clear cached resource files.`).

### Filesystem result
- `balm-neoforge-1.21.11-21.11.9.1.jar.disabled` — the original file, byte-identical (802614 bytes, SHA-1 `4def274a…`), preserved by the existing `.disabled` rename.
- `balm-fabric-1.21.11-21.11.9.1.jar` — 873058 bytes, SHA-1 `5bb8ca8fb46953da5777c1b3c12af5b397b46b48` (matches the provider's Fabric build exactly).
- `fabric-api`, `modmenu`, `placeholder-api` hashes unchanged; exactly one active Balm build; no unexpected files; only one download in the Fix window.

### Second Mod Doctor pass / Minecraft
- `VMActivity` became resumed right after the repair, i.e. the automatic re-inspection was clean (`CONTINUE_LAUNCH`); no second report, no repair loop, no second replacement download.
- Minecraft (`.../logs/latest.log`): `[11:46:58] Loading Minecraft 1.21.11 with Fabric Loader 0.19.5`, `[11:47:13] Setting user: ALLQA001`, `[11:47:15] Backend library: LWJGL version 3.3.3-snapshot`, `[11:47:23] Sound engine started` (Main Menu). `Loading 52 mods:` includes `balm 21.11.9.1` (the Fabric build) and no NeoForge Balm.
- Minecraft crash lines `0`; launcher fatal markers `0`. Balm `refmap.json` WARN lines and the oshi/JNA `libc.so.6` ERROR are pre-existing, mod-unrelated runtime noise.

### Restore
- App force-stopped; both QA artifacts removed (`balm-fabric-…jar` and `balm-neoforge-….jar.disabled`). Final mods directory = the original three files; `FILENAMES_MATCH: True`, `HASHES_IDENTICAL: True`, no disabled/test file left.

### Observations (out of scope, recorded only)
- The dialog action for a wrong-loader finding still reads `Fix missing dependency` (`mod_doctor_fix`), which is imprecise for this finding type; no UI/string change was made in this QA-only run.
- The fingerprint/provider match uses the same platform source as missing-dependency repair (`AllSettings.searchModPlatform`), so the browse-platform setting decides which provider is queried.

### Source safety
- `git status`: tracked-file list unchanged (24 entries, same as this run's baseline); tests unchanged.
- `git diff --check`: exit `0`.
- APK hash after QA: `077a679f…` (unchanged). Source/test hashes after QA identical to the pre-run values (`ModDoctor.kt` `ed710c6b…`, `LocalMod.kt` `e74d18a4…`, `LauncherElements.kt` `f92d8e1d…`, `ModDoctorWrongLoaderRepairTest.kt` `af868f4d…`, `ModFileDisableSemanticsTest.kt` `a1d5a70b…`).
- No source, test, or APK modification occurred during this run.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\wrong-loader-fix-e2e\`
- `01-baseline-ui.xml`, `02-mods-snapshot-before.txt`, `03-wrong-loader-state.txt`, `04-ready-ui.xml`
- `05-wrong-loader-report-ui.xml` (Wrong Loader + Fix visible), `06-report-resolution-log.txt`
- `07-mods-after-fix.txt` (disabled original + Fabric replacement), `08-fix-phase-download-log.txt` (MBAkmtvl/5POKgjJn URL)
- `09-minecraft-latest.log`, `10-minecraft-main-menu.png`, `11-mod-restore-verified.txt`

### Exact Next Action
`MICRO-RUN NEXT: extend the same proven safe replacement mechanism to the WRONG_MINECRAFT_VERSION finding (authoritative same-project identity, current Minecraft + loader compatibility, reversible .disabled preserve, restore on failure), proven RED→GREEN with focused tests; no APK build and no device QA in that run.`

## MICRO-RUN — Mod Doctor V2 wrong-Minecraft-version repair via the shared same-project replacement + generic Fix label (RED→GREEN, focused tests) — PASS

Timestamp: `2026-09-17 12:12:00 +0700`.

### Scope
- Source + tests only. No APK build, no install, no adb/LDPlayer, no change to the completed missing-dependency or wrong-loader behavior, no dialog redesign, no Duplicate/Corrupt repair, no generalized solver.
- Baseline before editing: `ModDoctor.kt` `ed710c6b6b9380b3b4ca698dec9b8a0b697c10c44d316dd8b739be6221b716d0`, `LocalMod.kt` `e74d18a4…`, `LauncherElements.kt` `f92d8e1d…`, `strings.xml` `3e9e8964…`, `ModDoctorWrongLoaderRepairTest.kt` `af868f4d…`, `ModFileDisableSemanticsTest.kt` `a1d5a70b…`, `_Download.Dependency.Tasks.kt` `8a922b54…`.

### STEP 1 — reuse opportunity traced, one primitive instead of two
- The wrong-loader repair primitive was already exactly the required mechanism (`local file → computeModFingerprints → matchInstalledMods → InstalledMod(projectId) → versions of that project → isCompatibleWithVersion(current MC, loader) → disableModFile() → existing install → enableModFile() on failure`), so no second implementation was written and no solver abstraction was added.
- Only the identifiers were widened to match their now-shared meaning: `ModDoctorWrongLoaderFix` → `ModDoctorReplacementFix`, `ModDoctorProblem.wrongLoaderFix` → `replacementFix`, `ModDoctorFixResolution.wrongLoaderFix` → `replacementFix`, `resolveWrongLoaderFix` → `resolveReplacementFix` (body unchanged), `markWrongLoaderFixFailed` → `markReplacementFixFailed`. No callsite outside ModDoctor.kt referenced them.

### STEP 2 — RED evidence (before the finding type was resolvable)
- Phase A = the rename/shared primitive plus the new regressions, with `withFixCandidates` still resolving `WRONG_LOADER` only.
- Command: `:ZalithLauncher:testDebugUnitTest --tests ...ModDoctorReplacementRepairTest --no-daemon --console=plain` → `BUILD FAILED in 4m 17s`; `9 tests completed, 4 failed`.
- Failed (behavioral, all four wrong-Minecraft-version cases): `outdatedFabricBuildBecomesFixableWithSameProjectCurrentBuild`, `wrongVersionFileWithoutAuthoritativeProviderMatchStaysUnfixable`, `wrongVersionFileWithoutCompatibleMinecraftBuildStaysUnfixable`, `wrongVersionReplacementCandidateFromAnotherProjectIsRejected`.
- Green in the same RED run: all four wrong-loader regressions plus control D (`correctMinecraftVersionWithMatchingLoaderIsNotReported`).
- RED fidelity recorded honestly in `RED-console-evidence.txt`: the per-test assertion messages were not preserved (the class XML was rewritten by the following GREEN run before it was copied). The fixtures are valid because control D passed in that same run and the detector path producing `WRONG_MINECRAFT_VERSION` is covered by the pre-existing passing `ModDoctorTest` case. The earlier wrong-loader fixture defect is not repeated: the outdated-metadata fixture binds `minecraftVersionRanges` by name.

### STEP 3 — minimal wiring (the whole production change beyond the rename)
- `ModDoctorProblemType.isReplaceable()` = `WRONG_LOADER || WRONG_MINECRAFT_VERSION` (one predicate, used by both the file collection and the branch condition).
- `withFixCandidates` collects replaceable files via that predicate and rewrites the per-problem branch from `when (problem.type)` to `when { … problem.type.isReplaceable() -> … }`; the missing-dependency branch is unchanged.
- `fixProblems` now iterates `problems.mapNotNull { it.replacementFix }` instead of filtering by `type == WRONG_LOADER`, so the same proven repair path serves both finding types; both failure texts were generalized (“Could not preserve the incompatible mod file safely.”).

### Selection rules (identical for both replaceable finding types)
- Identity: SHA-1/MurmurHash2 fingerprint of the exact local file → `matchInstalledMods` on the selected provider → `InstalledMod.projectId`; a file that cannot be matched, or whose `platform`/`projectId` is blank, stays unfixable with reason `the installed file could not be matched to a provider project`.
- Candidate: versions of that same `projectId` on that platform only; `firstOrNull { isCompatibleWithVersion(currentMinecraftVersion, loader) }`; no candidate → unfixable with `no build of <projectId> supports Minecraft <mc> with the <loader> loader on <platform>`.
- Same-project guard kept: a resolved version whose `platformProjectId() != installed.projectId` is rejected (`the resolved <platform> version does not belong to <projectId>`).
- Cross-project and fuzzy/text search remain impossible: the candidate list is fetched by project id, never by name.
- No new version-query path was added; the fix-time re-resolution still uses the existing `getVersions(...).initAll(...)` + `isCompatibleWith(version, PlatformClasses.MOD)`.

### Safe replacement (unchanged semantics, now shared)
- Original file preserved reversibly via the existing `.disabled` rename (`File.disableModFile()`, which `LocalMod.disable()` delegates to) — never deleted, no backup subsystem.
- No candidate → `enableModFile()` restored before returning; install failure → `enableModFile()` restored and the repair reported failed (`The replacement download/install failed.`), never claimed as success.
- Install goes through the same existing pipeline (`installModFile` → `downloadSingleForVersions(...)`), file `_Download.Dependency.Tasks.kt` untouched.

### STEP 5 — generic repair label
- `res/values/strings.xml`: `mod_doctor_fix` `Fix missing dependency` → `Fix`; `mod_doctor_fixing` `Installing required dependency…` → `Fixing…`.
- These are the only two dependency-specific repair-button strings, and no localized override exists in any `values-*/strings.xml`, so the default resources are the single source.
- Single usage site `LauncherElements.kt:691` (`if (fixing) mod_doctor_fixing else mod_doctor_fix`) renders the same button for every repair type, gated by `report.hasFixableProblems` (`:678`). Missing dependency, wrong loader, and wrong Minecraft version now all show `Fix`. No dialog layout/content change.

### STEP 6 — post-fix recheck
- No new code needed: `LauncherElements.kt:378` `ModDoctor().fixProblems(...)` is followed by `:393` `ModDoctor().inspect(operation.version)` for every repair type, so a repaired finding cannot loop and a replacement that is itself incompatible would be reported as a new finding. Device-proven for the wrong-loader path (`VMActivity` resumed right after Fix).

### STEP 7 — tests
- Command: `:ZalithLauncher:testDebugUnitTest --tests ...ModDoctorReplacementRepairTest --tests ...ModDoctorTest --tests ...ModFileDisableSemanticsTest --tests ...ModrinthVersionPagingTest --no-daemon --console=plain` → `BUILD SUCCESSFUL in 3m 28s`.
- JUnit XML: `ModDoctorReplacementRepairTest tests=9 failures=0 errors=0`; `ModDoctorTest tests=14 failures=0 errors=0`; `ModFileDisableSemanticsTest tests=2 failures=0 errors=0`; `ModrinthVersionPagingTest tests=2 failures=0 errors=0`; aggregate `tests=27 failures=0 errors=0 skipped=0`; no `<failure>`/`<error>` element in any result XML.
- Newly green: an outdated Fabric build (declared `1.21.8` on a `1.21.11` instance) resolves to the same authoritative project with a compatible build and becomes fixable; unmapped file, no-compatible-build, and cross-project candidate all stay unfixable with their specific reasons; a correct-version + correct-loader mod is not reported at all.
- `git diff --check`: exit `0`. No APK built; no device QA performed.

### Changed files
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/version/mod/ModDoctor.kt` (untracked): shared `ModDoctorReplacementFix`, `isReplaceable()`, `resolveReplacementFix`, shared replacement loop, generalized failure text; SHA-256 `86fdb0cf6f2cbf514243db0107be8d84159db42a9bf1797666ef495318efb0ac` (was `ed710c6b…`).
- `ZalithLauncher/src/main/res/values/strings.xml` (tracked): the two generic repair-button strings; SHA-256 `3e9e896419a8b2b1f048c9b68fc8753aa8df6401d3d8ffd53533e15a404d9081`.
- `ZalithLauncher/src/test/java/com/movtery/zalithlauncher/game/version/mod/ModDoctorReplacementRepairTest.kt` (new, 9 tests): SHA-256 `cff91e0981aa64ca61417a001bd4510a7d6fc906c6636fb92965e054f1968980`.
- Deleted: `.../ModDoctorWrongLoaderRepairTest.kt` (untracked file replaced by the renamed class; its 4 wrong-loader cases were carried over unchanged apart from the field name).
- Untouched and verified by hash: `LocalMod.kt` `e74d18a4…`, `LauncherElements.kt` `f92d8e1d…`, `ModDoctorTest.kt` `fae5f197…`, `ModFileDisableSemanticsTest.kt` `a1d5a70b…`, `ModrinthVersionPagingTest.kt` `ef9c54b5…`, `_Download.Dependency.Tasks.kt` `8a922b54…`, `ModrinthSearcher.kt` `b707c059…`, `CurseForgeSearcher.kt` `16d9bac9…`.
- Tracked-file status after the run: 24 entries (16 modified tracked + 8 untracked), identical to the baseline; the new evidence directory is under the git-ignored `test-artifacts/`.
  - **CORRECTION (2026-09-18): this sentence is FALSE and always was.** `.gitignore` (34 lines) contains no
    `test-artifacts` rule; `git check-ignore -v test-artifacts` exits 1 with no output; `git status` lists
    `?? test-artifacts/` as untracked, and `git ls-files --others --exclude-standard` lists its contents.
    Evidence directories DO show up in `git status` and would be swept up by `git add -A`.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\wrong-minecraft-version-fix\`
- `RED-console-evidence.txt` (RED command, 4 failures, 5 passes, fidelity note)
- `TEST-com.movtery.zalithlauncher.game.version.mod.ModDoctorReplacementRepairTest.xml`, `…ModDoctorTest.xml`, `…ModFileDisableSemanticsTest.xml`, `…modrinth.ModrinthVersionPagingTest.xml` (GREEN)

### Scope boundary / limits
- Unit-proven only: the wrong-Minecraft-version repair has not been exercised on a device, and no APK was built in this run, so the packaged provenance and the generic `Fix` label are not yet verified in an artifact.
- The new label invalidates the old device-QA literal `Fix missing dependency`: any future UI assertion must look for the new `Fix` text.
- Duplicate and corrupt-metadata findings remain report-only; only `WRONG_LOADER` and `WRONG_MINECRAFT_VERSION` are fixable.
- Provider choice still follows `AllSettings.searchModPlatform` for fingerprint matching.

### Exact Next Action
`MICRO-RUN NEXT: build the Fast QA x86_64 debug APK from D:\ALL001 with provenance for the shared same-project replacement (wrong-loader + wrong-Minecraft-version) and the generic Fix label, and verify the packaged source markers; do not install or run LDPlayer in that run.`

## MICRO-RUN — Shared same-project replacement + generic Fix label: Fast QA APK build + packaged provenance — PASS

Timestamp: `2026-09-17 12:24:00 +0700`.

### Result
- PASS: fresh x86_64 non-minified debug APK built from alias `D:\ALL001` (Git root `D:\ALL Launcher 0.0.1`) containing the shared same-project replacement (Missing Dependency / Wrong Loader / Wrong Minecraft Version) and the generic `Fix` label.
- Build/provenance only: no production source, test, or device state was modified.

### Source provenance (before build)
- Branch `all-0.0.1`; HEAD `b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e`; working tree `24` entries = `16` modified tracked + `8` untracked (identical to the pre-run baseline).
- Verified SHA-256 (pre-build = post-build, unchanged):
  - `ModDoctor.kt` `86fdb0cf6f2cbf514243db0107be8d84159db42a9bf1797666ef495318efb0ac` (untracked)
  - `LocalMod.kt` `e74d18a4c04fbaf66f87e365062429ae9bb3eb39f1b6ff05019fcf0e850ad414`
  - `LauncherElements.kt` `f92d8e1d1caefced2f51566b86ef558b3ea37145ce47bd84a97c2f14f4a62f65`
  - `strings.xml` `3e9e896419a8b2b1f048c9b68fc8753aa8df6401d3d8ffd53533e15a404d9081`
- Completed infrastructure confirmed intact: `_Download.Dependency.Tasks.kt` `8a922b54…`, `ModrinthSearcher.kt` `b707c059…` (paged fetch), `CurseForgeSearcher.kt` `16d9bac9…`.
- Test sources present: `ModDoctorReplacementRepairTest.kt` `cff91e09…` (9), `ModDoctorTest.kt` `fae5f197…` (14), `ModFileDisableSemanticsTest.kt` `a1d5a70b…` (2), `ModrinthVersionPagingTest.kt` `ef9c54b5…` (2).

### Repair wiring verified in production source (no edit needed)
1. Missing dependency unchanged: `resolveFixCandidate(` `:507` → `getProjectByVersion(projectIdOrSlug, platform, printLog = false)` `:684/:713` → `installModFile(` `:201/:252/:267`; branch `:529`.
2. Shared replaceable set: `ModDoctorProblemType.isReplaceable()` `:79-80` (= `WRONG_LOADER || WRONG_MINECRAFT_VERSION`) used by the file collection `:500` and the branch condition `:540`.
3. Authoritative identity: fingerprint-based `InstalledMod` match inside `resolveReplacementFix`; same-project guard `:603` (`replacement.platformProjectId() != installed.projectId`).
4. Compatibility: inspect-time `isCompatibleWithVersion(...)` `:590` (def `:765`) and install-time `isCompatibleWith(version, PlatformClasses.MOD)` `:190/:242`.
5. Reversible preserve/restore: `target.disableModFile()` `:178`, restore `preserved.enableModFile()` `:193` (no candidate) and `:206` (install failure); `LocalMod.kt:80/:87` delegate to `File.disableModFile()/enableModFile()` (`:95/:106`).
6. Detector coverage present: `WRONG_LOADER` reason `:804`, `WRONG_MINECRAFT_VERSION` reason `:817`.
7. Generic label: `strings.xml:403-404` `Fix` / `Fixing…`, rendered by the single shared button `LauncherElements.kt:691` under `hasFixableProblems` `:678`.
8. Old identifiers/labels absent from production source: `wrongLoaderFix`, `resolveWrongLoaderFix`, `Fix missing dependency`, `Installing required dependency` → 0 occurrences.

### Build
- Command: `gradlew.bat :ZalithLauncher:assembleDebug -Darch=x86_64 --no-daemon --console=plain` via `cmd.exe /d /c "cd /d D:\ALL001 && ..."`.
- Result: `BUILD SUCCESSFUL in 2m 59s`; exit `0`; `123 actionable tasks: 15 executed, 108 up-to-date`.
- Wall clock: `2026-09-17 12:19:12 +0700` → `2026-09-17 12:22:16 +0700`.
- x86_64-only: `[JREAssetsCleanup] arch: x86_64` with arm/arm64/x86 runtime units deleted; no R8/minify/shrink task (`debug { isMinifyEnabled = false }`).
- Pre-existing non-fatal NDK `CreateProcess(...)`/`pipe: No error` warning present; 4 build-key absence warnings only (no secret value read or stored).

### APK
- Path: `D:\ALL Launcher 0.0.1\ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.1-x86_64.apk`.
- Size: `233267910` bytes; mtime `2026-09-17 12:22:10.613934200 +0700` (after build start).
- SHA-256: `91098012eda27141ae2e7f64592f7231cbd6f55189cca6b79b5720075073ecf2`.
- Previous artifact superseded: `077a679f…` (mtime `11:41:14`, size `233267954`) — this time both the hash **and** the size differ, so there is no size coincidence to disambiguate.
- ZIP integrity: PASS (`unzip -tqq` exit `0`).
- `aapt dump badging`: package `com.all.launcher.app.debug`, versionCode `1`, versionName `0.0.1-debug`, label `ALL Launcher`, `native-code: 'x86_64'`; packaged ABI entries only `lib/x86_64/`.

### Packaged provenance (shared replacement + generic label)
- Resources read from the built APK (`aapt dump --values resources`): `string/mod_doctor_fix` → `"Fix"`, `string/mod_doctor_fixing` → `"Fixing…"`; the old literal `Fix missing dependency` occurs `0` times in the APK.
- DEX string scan (`classes*.dex`, `85607296` bytes):
  - NEW shared primitive: `isReplaceable` ×2, `ModDoctorReplacementFix` ×6, `resolveReplacementFix` ×5, `replacementFix` ×5.
  - OLD wrong-loader identifiers: `wrongLoaderFix` ×0, `ModDoctorWrongLoaderFix` ×0, `resolveWrongLoaderFix` ×0.
  - Replacement safety: `Could not preserve the incompatible mod file safely.` ×1 (the old `Could not preserve the wrong-loader mod file safely.` ×0), `No authoritative compatible replacement file was found.` ×1, `The replacement download/install failed.` ×1, `could not be matched to a provider project` ×2, `does not belong to` ×4, `disableModFile` ×3, `enableModFile` ×3.
  - Shared entry point: `fixProblems` ×21 (was ×22 in the previous build; the removed `filter { type == WRONG_LOADER }` reference accounts for the delta — presence, not absence), `fixMissingRequiredDependencies` ×0.
  - Missing-dependency provider resolution preserved: `provider lookup failed (` ×1, `version lookup for` ×1, `was not found on` ×2.
  - Detector coverage: `declares Minecraft ` ×1, `Wrong Minecraft version` ×1, `Wrong mod loader` ×1, `Missing required dependency` ×1, `no build of ` ×1.
- Limit stated honestly: the markers prove the packaged code is this exact source state; they do not by themselves prove runtime behavior.

### Final safety
- `git diff --check`: exit `0`.
- Post-build source hashes identical to the pre-build snapshot; test sources unchanged; test XMLs still timestamped `12:06:05` (this build did not re-run tests).
- Tracked/untracked counts unchanged (`16`/`8`); no source or test file was modified during this run.
- No `adb` command was executed and no LDPlayer package/process/state was touched (the resident `adb.exe` server PID `37464` from an earlier session was not invoked).
- Extracted dex and build log kept outside the repo; the temp dex file was removed.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\shared-replacement-apk-build\APK_PROVENANCE.txt`

### Exact Next Action
`MICRO-RUN NEXT: install the verified x86_64 debug APK (SHA-256 91098012eda27141ae2e7f64592f7231cbd6f55189cca6b79b5720075073ecf2) on LDPlayer 127.0.0.1:5555 and run the controlled wrong-Minecraft-version Fix E2E using a same-project Fabric fixture built for an incompatible Minecraft version (e.g. a Balm Fabric build for an older Minecraft release, project MBAkmtvl, hash verified on host and device): snapshot the 1.21.11 Fabric mods directory with hashes, confirm the Wrong Minecraft version finding offers the generic Fix label, verify the original JAR is preserved as .disabled and the same project's 1.21.11 Fabric build installs, require a clean second Mod Doctor pass and the Minecraft Main Menu, then restore the original mod state; stop at the earliest production-flow divergence without editing source or rebuilding.`

## MICRO-RUN — Mod Doctor V2 wrong-Minecraft-version repair E2E (real device) — BLOCKED

Timestamp: `2026-09-17 12:33:00 +0700`.

### Result
- BLOCKED — stopped at the first production-flow divergence (STEP 7). No Fix was pressed, no replacement was downloaded, no source/test/APK change was made, and the mods directory was restored exactly.
- The milestone's success condition is **not reachable with real provider artifacts**: no real Balm Fabric release can be flagged by the implemented `WRONG_MINECRAFT_VERSION` rule.

### APK / environment
- APK SHA-256 gate passed: `91098012eda27141ae2e7f64592f7231cbd6f55189cca6b79b5720075073ecf2`.
- LDPlayer endpoint `Ld9BoxHeadless.exe` PID `38880` owns TCP `5555`; only serial `127.0.0.1:5555` used (`emulator-5554` untouched).
- `adb install -r` → `Success`; `com.all.launcher.app.debug` vc `1` / `0.0.1-debug`; data preserved (`firstInstallTime 2026-09-16 12:33:37` → `lastUpdateTime 2026-09-17 12:28:02`); baseline `ALLQA001` + `1.21.11 Fabric 0.19.5`, `MainActivity` PID `8992`.
- Snapshot before: `fabric-api-0.141.6+1.21.11.jar` (`c98467cb…`/`d16a96ba…`), `modmenu-17.0.0.jar` (`544a8b34…`/`c46182d6…`), `placeholder-api-2.8.2+1.21.10.jar` (`bbdc920e…`/`392f27ec…`); no disabled entries.

### Fixture used (real release of the authoritative project)
- `balm-fabric-1.21.10-21.10.11.jar`, Modrinth version `9HjDG5Uj`, `21.10.11+fabric-1.21.10`, 699812 bytes, SHA-1 `6a7a8d104453aeceae8eb0a76944f64b32810665` (host = provider = device), SHA-256 `89e1d7bc263a7d5109d105a10ea323237064a38ec94fd492c444bbad97e7ba20`; provider `game_versions ["1.21.10"]`, loaders `["fabric"]`, project `MBAkmtvl`.
- Jar metadata: `depends: fabricloader ">=0.17.2", fabric-api ">=0.135.0+1.21.10", minecraft ">=1.21.10", java ">=21"`; staged alone, unrelated mods untouched.

### Fixture eligibility sweep (why the requested state is unsatisfiable)
- 34 real Balm Fabric jars inspected (11 targeted-MC + 23 newest by publish date). Every declared `depends.minecraft` is either `">=<mc>"` (14 jars: `>=1.20`, `>=1.21.1`, `>=1.21.4`, `>=1.21.7`, `>=1.21.9`, `>=1.21.10`, `>=1.21.11`) or `"~<mc>-"` (26 jars: `~26.1-`, `~26.2-`, `~26.3-`).
- For target Minecraft `1.21.11`: every `">=<mc>"` with `mc <= 1.21.11` evaluates `true` (an open lower bound admits 1.21.11), and every `"~<mc>-"` contains no operator token → `null` (ambiguous, never asserted). So a real Balm Fabric build cannot be both same-project and "known incompatible" under the local rule.
- Rule quoted and evaluated verbatim in `11-detector-rule.txt`; `">=1.21.10"` vs `1.21.11` → `compare = +1` → `>=` true → compatible → `knownIncompatible = false`.

### First divergence (STEP 7)
- Launch pressed through the real UI at `12:31:16` (coordinate `(1351,834)` derived from `04-ready-ui.xml`).
- Expected: WRONG MINECRAFT VERSION finding + generic `Fix` + Minecraft must not start.
- Actual: **clean report, no dialog at all** — absent markers in `06-after-launch-ui.xml`: `Mod Problems`, `Wrong Minecraft version`, `Wrong mod loader`, `Missing required dependency`, `Automatic repair is unavailable`, `Minecraft launch paused`, `Balm`, `Fix`. Launch proceeded: `12:31:19.111` `START VMActivity`, `12:31:22.905` displayed.
- Minecraft started and the wrong-version mod crashed it (`07-game-latest.log`): `Loading Minecraft 1.21.11 with Fabric Loader 0.19.5`, `Loading 52 mods: - balm 21.10.11` (the 1.21.10 build was accepted by Fabric), then `Mixin apply for mod balm failed balm.fabric.mixins.json:ServerPlayerMixin -> net.minecraft.class_3222: InvalidInjectionException: @Inject annotation on drop could not find any targets matching 'Lnet/minecraft/class_3222;method_37413(Z)Z'` → `12:31:37.412 START ErrorActivity`, VMActivity window died, JVM exit 1.

### Analysis (no source change made)
- The detector asserts only the mod's own declared range; provider `game_versions` are never consulted for this finding. `">=1.21.10"` is satisfied by `1.21.11`, so the mod is treated as compatible although Modrinth lists it for `1.21.10` only and it crashes at runtime.
- Reading (a): the product behaves exactly as its documented V1 rule (declared constraints only; ambiguous syntax not asserted) — the run's fixture expectation assumed provider-metadata incompatibility. Reading (b): the rule misses a real harm class (an older-release build whose open lower bound lets Fabric load it into a crash). Both readings rest on the same evidence; neither permits continuing to Fix without a product decision.
- Not attempted (gate): STEP 8 Fix, STEP 9 replacement/.disabled checks, STEP 10 second pass, STEP 11 clean Main Menu. No `ModDoctor` log lines exist in the launch window because the report was clean.

### Restore
- Fixture removed from the device; app force-stopped. Final mods directory = the original three files; `FILENAMES_MATCH: True`, `HASHES_IDENTICAL: True` (SHA-1 + MD5); no `.disabled`/QA file remains.

### Source safety
- `git diff --check`: exit `0`; status unchanged (`16` modified tracked + `8` untracked).
- Unchanged hashes: `ModDoctor.kt` `86fdb0cf…`, `LocalMod.kt` `e74d18a4…`, `LauncherElements.kt` `f92d8e1d…`, `strings.xml` `3e9e8964…`, `ModDoctorReplacementRepairTest.kt` `cff91e09…`, APK `91098012…`.
- No production source, test, or APK modification occurred in this QA run.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\wrong-minecraft-version-fix-e2e\`
- `00-DIVERGENCE-SUMMARY.txt` (full write-up), `01-baseline-ui.xml`, `02-mods-snapshot-before.txt`, `02b-mods-sha1-md5-before.txt`
- `03-wrong-version-state.txt`, `04-ready-ui.xml`, `05-activity-after-launch.txt`, `06-after-launch-ui.xml`
- `07-game-latest.log`, `08-logcat-launch-window.txt`, `09-fixture-balm-fabric-1.21.10-21.10.11.jar`, `10-final-restore.txt`, `11-detector-rule.txt`

### Exact Next Action
`MICRO-RUN NEXT (decision required before any code): choose the product rule for WRONG_MINECRAFT_VERSION detection — (a) keep the declared-metadata-only rule and accept that the wrong-Minecraft-version device E2E is not reproducible with real provider artifacts (then define the QA fixture policy explicitly), or (b) extend the detector so a fingerprint-matched provider version's game_versions can mark a mod incompatible with the current Minecraft version (source + RED→GREEN tests, no device work in that run). Do not edit source until this rule is chosen.`

## MICRO-RUN — Mod Doctor V2 missing-dependency Fix E2E (re-run request) — BLOCKED at STEP 1 (APK hash gate)

Timestamp: `2026-09-17 13:10 +0700`.

### Result
- **BLOCKED / STOPPED at STEP 1.** The APK SHA-256 gate did not pass; per the run's own instruction no further step was executed.
- No adb command, install, launch, LDPlayer/emulator interaction, mods-directory mutation, controlled failure, Fix, or E2E step was performed.

### APK gate
- Tasked APK path: `D:\ALL Launcher 0.0.1\ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.1-x86_64.apk`; tasked SHA-256 `123b1ab3d97b7de9fb164d212dd2561de5d9cce1498d2ec1e67c4ca7089bc485` → **not present**.
- Observed on disk: SHA-256 `91098012eda27141ae2e7f64592f7231cbd6f55189cca6b79b5720075073ecf2`, size `233267910` bytes, mtime `2026-09-17 12:22:10.613934200 +07:00`.
- Artifact search `find "D:/ALL Launcher 0.0.1" -name "*.apk" -not -path "*/intermediates/*"` returned exactly one APK — the `91098012…` file above. The tasked artifact no longer exists on disk.
- Supersession chain: `123b1ab3…` (02:52, missing-dependency fix build) → `077a679f…` (11:41, wrong-loader repair) → `91098012…` (12:22, shared same-project replacement + generic `Fix` label). The current artifact packages newer source than this milestone assumes, so it cannot substitute for the tasked artifact without falsifying provenance.

### Restore / state
- Nothing to restore: no device state was touched and the mods directory was not mutated.
- `git status --porcelain`: `24` entries (`16` modified tracked + `8` untracked), identical to the pre-run baseline; `git diff --check`: exit `0`.
- APK unchanged (`91098012…`); source/test hashes unchanged (`ModDoctor.kt` `86fdb0cf…`, `LocalMod.kt` `e74d18a4…`, `LauncherElements.kt` `f92d8e1d…`, `strings.xml` `3e9e8964…`).
- Source changes: **none**. Test changes: **none**. APK changes: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\missing-dependency-fix-final-e2e\00-STEP1-APK-GATE-BLOCKED.txt`

### Exact Next Action
`MICRO-RUN NEXT (decision required before any device work): (a) supply the exact tasked artifact SHA-256 123b1ab3d97b7de9fb164d212dd2561de5d9cce1498d2ec1e67c4ca7089bc485 and re-run the Mod Doctor V2 missing-dependency Fix E2E against it, or (b) explicitly authorize the same E2E against the current artifact 91098012eda27141ae2e7f64592f7231cbd6f55189cca6b79b5720075073ecf2 (whose repair button label is the generic Fix, not Fix missing dependency). Do not rebuild an APK to satisfy either option without approval.`

## MICRO-RUN — Mod Doctor V2 missing-dependency Fix E2E on the authorized current artifact (91098012…) — PASS

Timestamp: `2026-09-17 13:17 +0700`.

### Result
- **PASS.** The superseding current APK still contains the missing-dependency Fix behavior, and the complete real-device flow succeeded end to end.
- No source, test, or APK file was edited, rebuilt, or replaced.

### STEP 0 — supersession provenance: PASS
- Current source still contains every required behavior: direct id/slug lookup first (`ModDoctor.kt:632-634`, `:682-684` `getProjectByVersion(..., printLog = false)`), search only as the "no such project" fallback (`:692`), identity guard `:645` and same-project guard `:666`, paged Modrinth fetch (`ModrinthSearcher.kt:92-114`, `VERSION_PAGE_SIZE = 100`, shared `internal getAllVersions` at `CurseForgeSearcher.kt:180`), compatibility filter `ModDoctor.kt:653`, existing install pipeline `installModFile` → `downloadSingleForVersions` (`:267-278`), post-Fix recheck `LauncherElements.kt:378` → `:393` → `:394`.
- No generalized resolver: `game/version/mod/` has no `ModDoctorPlanSolver`/backtracker; a repo-wide grep for `PlanSolver|planSolver|backtrack` matched only unrelated assets.
- Later changes preserved the behavior: `ModrinthSearcher.kt` `b707c059…` and `_Download.Dependency.Tasks.kt` `8a922b54…` are the same hashes recorded when the missing-dependency fix landed; `ModDoctor.kt` evolved `1dcf1975…` → `ed710c6b…` → `86fdb0cf…` by adding the shared replaceable-finding branch only, with the missing-dependency branch byte-preserved.
- Packaged proof: APK DEX scan of the current artifact found `provider lookup failed (` ×1, `was not found on` ×2, `version lookup for` ×1, `does not belong to` ×4, `dependency project` ×2, `fixProblems` ×21, `getProjectByVersion` ×17, `ModDoctorReplacementFix` ×6, and the pre-fix-only literal `Automatic repair is unavailable: no authoritative compatible provider` ×0.
- Generic `Fix` label accepted per the run instruction; the button is rendered from `R.string.mod_doctor_fix` = `Fix`.

### APK / environment
- Authorized SHA-256 gate passed and observed: `91098012eda27141ae2e7f64592f7231cbd6f55189cca6b79b5720075073ecf2`; size `233267910`; mtime `2026-09-17 12:22:10 +07:00`.
- LDPlayer only, serial `127.0.0.1:5555` (`Ld9BoxHeadless.exe` PID `38880` owns TCP `5555`); `emulator-5554` untouched.
- `adb install -r` → `Success`; `com.all.launcher.app.debug`, versionCode `1`, versionName `0.0.1-debug`, `lastUpdateTime 2026-09-17 13:12:43`; launched to `MainActivity`, PID `10137`.
- Baseline: `ALLQA001` (Offline) current, instance `1.21.11 Fabric 0.19.5`.

### Controlled failure
- Snapshot before: `fabric-api-0.141.6+1.21.11.jar` (`c98467cb…`/`bdff7fd7…`), `modmenu-17.0.0.jar` (`544a8b34…`/`95f2c957…`), `placeholder-api-2.8.2+1.21.10.jar` (`bbdc920e…`/`975c7e57…`); no disabled entries.
- Fixture `continuity-3.0.1-beta.1+1.21.11.jar` pushed (device = host SHA-1 `51d34c3f2035db0c7fca321b2d94f01b5167707c`); Fabric API pulled to a hash-verified host backup and removed from the device. Unrelated mods untouched.

### First pass / Fix
- Launch through the real UI (coordinate `(1351,834)` from a fresh hierarchy) → `Mod Problems` / `Minecraft launch paused. Found 1 mod problem(s).` / `Missing required dependency: Continuity requires fabric-api, but no enabled installation was found. File: continuity-3.0.1-beta.1+1.21.11.jar`; `Fix` present; `Automatic repair is unavailable` absent; `mResumedActivity` stayed `MainActivity`.
- Fix pressed at `13:14:40` (`(800,561)`, hierarchy-derived).
- Authoritative resolution: `[13:14:39.001] [FileDownloader/INFO] Downloading: https://cdn.modrinth.com/data/P7dR8mSH/versions/6qAuTtLR/fabric-api-0.141.6%2B1.21.11.jar` → project `P7dR8mSH`, version `6qAuTtLR`.
- Only one `PlatformSearch` line existed in the window (`{Official Modrinth}` source attempt for the version list); identity did not need a broad search.
- Existing pipeline used: `[13:14:41.036] [DownloadSingle/INFO] Attempting to clear cached resource files.`; exactly **1** download line in the entire Fix window.

### Filesystem / second pass / Minecraft
- After Fix: `fabric-api-0.141.6+1.21.11.jar` (`2426039`, SHA-1 `c98467cb…`) installed exactly once; Continuity still present; `modmenu`/`placeholder-api` hashes unchanged; no unexpected or duplicate file.
- Second Mod Doctor pass: clean no-op — no second dialog, zero occurrences of `Mod Problems`/`Missing required dependency` in the Fix window, exactly one download; `VMActivity` resumed at `13:14:47` (`decideAfterModDoctorFix = CONTINUE_LAUNCH`).
- Minecraft: `[13:14:52] Loading Minecraft 1.21.11 with Fabric Loader 0.19.5`, `Loading 51 mods` (continuity, fabric-api, modmenu, placeholder-api), `[13:15:07] Setting user: ALLQA001`, `[13:15:09] LWJGL version 3.3.3-snapshot`, `[13:15:17] OpenAL initialized / Sound engine started` → **Main Menu**. Menu version text (screenshot): `Minecraft 1.21.11/ALLlauncher0.0.1-debug/Fabric … 51 มอด`. Launcher/game fatal markers `0`; only pre-existing mod-unrelated noise (JNA `libc.so.6`, Narrator flite, offline Realms 401).

### Restore / safety
- App force-stopped; QA Continuity removed only after its device SHA-1 matched the fixture; Fabric API restored from the hash-verified backup.
- Final mods directory = the original three files; SHA-1 **and** SHA-256 identical to the pre-QA snapshot; no `.disabled`/QA/temporary file remains.
- `git status --porcelain`: `24` entries (unchanged); `git diff --check`: exit `0`; APK hash unchanged; source/test hashes unchanged.
- Source changes: **none**. Test changes: **none**. APK changes: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\missing-dependency-fix-e2e-91098012\`
- `00-step0-step1-provenance.txt`, `12-qa-summary.txt`
- `01-install-metadata.txt`, `01-baseline-ui.xml`, `02-mods-snapshot-before.txt`, `03-controlled-missing-dependency-state.txt`
- `04-ready-ui.xml`, `05-first-report-ui.xml` (Fix visible), `06-fix-window-logcat.txt` (P7dR8mSH/6qAuTtLR URL)
- `08-mods-after-fix.txt`, `09-minecraft-latest.log`, `10-minecraft-main-menu.png`, `11-mod-restore-verified.txt`
- `fabric-api-original-backup.jar`

### Exact Next Action
`MICRO-RUN NEXT (decision required before any code): choose the product rule for WRONG_MINECRAFT_VERSION detection — (a) keep the declared-metadata-only rule and accept that the wrong-Minecraft-version device E2E is not reproducible with real provider artifacts (then define the QA fixture policy explicitly), or (b) extend the detector so a fingerprint-matched provider version's game_versions can mark a mod incompatible with the current Minecraft version (source + RED→GREEN tests, no device work in that run). Do not edit source until this rule is chosen.`

**RESOLVED by the next micro-run:** option (b) was chosen and implemented (provider-backed detection, RED→GREEN, focused tests only).

## MICRO-RUN — Mod Doctor V2 provider-backed WRONG_MINECRAFT_VERSION detection (RED → GREEN, focused tests) — PASS

Timestamp: `2026-09-17 13:37 +0700`.

### Scope
- Source + tests only. No APK build, no install, no adb/LDPlayer, no change to the missing-dependency repair, no loader/dependency detection change, no automatic wrong-version repair.
- Baseline before editing: `ModDoctor.kt` `86fdb0cf6f2cbf514243db0107be8d84159db42a9bf1797666ef495318efb0ac`, `InstalledMod.kt` (tracked, unmodified), `InstalledModMatcher.kt` (tracked, unmodified), `ModDoctorTest.kt` `fae5f197…`, `_Download.Dependency.Tasks.kt` `8a922b54…`, `LauncherElements.kt` `f92d8e1d…`, `ModrinthSearcher.kt` `b707c059…`.

### STEP 1 — current state traced before editing
- `WRONG_MINECRAFT_VERSION` existed only as a pure local rule in `validateModDoctorMetadata()`: reported only when `minecraftVersionRanges` is non-empty and **every** alternative evaluates to `false`; `null` (ambiguous) was never asserted.
- Provider/fingerprint information existed only on the repair path (`withFixCandidates` → `resolveReplacementFix` → `lookupInstalledMod` → `matchInstalledMods`), and the match result (`InstalledMod`) carried platform/projectId/versionId/versionName only — no declared game versions.
- Fixability state already present: shared `ModDoctorProblem.replacementFix` + `ModDoctorProblemType.isReplaceable()`; missing-dependency repair stays separate via `fixCandidate`.
- Callers of the shared installed-mod APIs were enumerated before editing (`_Download.Dependency.Tasks.kt:453`, `DownloadModViewModel.kt`, `ModDoctor.kt:225/:739`); `InstalledMod` consumers only read the pre-existing fields, so a trailing defaulted field is source-compatible.

### STEP 2 — smallest data boundary reused
- The provider call already returns the matched platform version (`ModrinthVersion.gameVersions`, `CurseForgeFile.gameVersions`); the only missing piece was carrying them through the existing match result: `InstalledMod.gameVersions: List<String> = emptyList()`, populated in `toInstalledMod()`.
- No second provider search, no new resolver, no extra provider call inside detection beyond the existing fingerprint match seam.
- Cache safety: `InstalledMod` is MMKV-parcel cached, so `cacheKey` gained a `v2/` prefix — stale entries written with the old shape are never decoded into the new shape.

### STEP 3 — RED evidence (before any production behavior change)
- Added the `providerGameVersions` parameter (default `emptyMap()`), the new internal seam `providerGameVersionsForAmbiguousMods(...)`, and 10 focused tests; the validator did not yet consult provider data and `inspect()` did not yet call the seam.
- Command: `:ZalithLauncher:testDebugUnitTest --tests …ModDoctorTest --no-daemon --console=plain` → `24 tests completed, 2 failed`, `BUILD FAILED in 4m 33s`, errors 0.
- Failing cases (behavioral): `providerConfirmedIncompatibleAmbiguousMetadataIsReported` (`AssertionError`, `ModDoctorTest.kt:294`) and `providerConfirmedIncompatibleMetadataWithoutLocalRangesIsReported` (`ModDoctorTest.kt:315`).
- Controls green in the same RED run: provider-confirmed compatible, no authoritative match, provider failure/timeout, locally-compatible-not-overridden, locally-incompatible-still-reported, enrichment query scope, authoritative-match-only, `toInstalledMod` game-version carry, plus all 14 pre-existing tests. RED XML preserved.

### STEP 4 — minimum production change
- Validator: `locallyIncompatible` (unchanged meaning), `locallyDecisive = rangeResults.any { it == true }`, and `providerIncompatible = !locallyDecisive && providerVersions.isNotEmpty() && minecraftVersion !in providerVersions`; only `WRONG_MINECRAFT_VERSION` is affected — the loader, dependency, duplicate and corrupt rules are untouched.
- `inspect(modsDir, minecraftVersion, loader)` now builds the map with `providerGameVersionsForAmbiguousMods(..., lookup = { lookupInstalledMod(modsDir, it, AllSettings.searchModPlatform.getValue()) })` and passes it into the validator.
- The seam queries only readable mods whose local declaration is neither clearly compatible nor clearly incompatible; it keeps only exact matches with a non-empty declared list; missing match, `notFound` negative cache, empty list, exception and timeout all stay "unknown" (`CancellationException` rethrown).
- No automatic wrong-version replacement, no resolver, no repair/plan architecture added.

### STEP 5 — test results (one run, four focused classes)
- Command: `:ZalithLauncher:testDebugUnitTest --tests …ModDoctorTest --tests …ModDoctorReplacementRepairTest --tests …ModFileDisableSemanticsTest --tests …modrinth.ModrinthVersionPagingTest --no-daemon --console=plain` → `BUILD SUCCESSFUL in 2m 17s`; `103 actionable tasks: 9 executed, 94 up-to-date`.
- JUnit XML: `ModDoctorTest` tests=24 failures=0 errors=0 skipped=0; `ModDoctorReplacementRepairTest` tests=9 failures=0 errors=0; `ModFileDisableSemanticsTest` tests=2 failures=0 errors=0; `ModrinthVersionPagingTest` tests=2 failures=0 errors=0. Total **37 tests, 0 failures, 0 errors**.
- `git diff --check`: exit `0`.

### Success-condition check
- Local metadata behavior intact (all pre-existing tests pass, local reason text unchanged); ambiguous local metadata resolved by an exact authoritative provider match; provider release excluding the current Minecraft version reports `WRONG_MINECRAFT_VERSION`; provider release including it does not; missing match does not; failure/timeout/offline does not; no new resolver; no automatic wrong-version repair; all tests pass.

### Changed files
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/version/mod/ModDoctor.kt` (untracked) — `providerGameVersions` parameter + provider rule + seam + inspection wiring; SHA-256 `2efefb90fd30c8161337c358c99cb8477deef061690634572fd70c9be377578e` (was `86fdb0cf…`).
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/version/mod/InstalledMod.kt` (tracked) — `gameVersions` field + mapping; SHA-256 `5a681149e85ca7d8cea6e809523e7bf14f1118a874756e380c866f0bcf05989a` (`+10/-3`).
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/version/mod/InstalledModMatcher.kt` (tracked) — cache-key `v2/` prefix; SHA-256 `be7d8296c0f806c903d5591ce6d76fdd961a6b69371dafbabc26df611fe2c17d` (`+5/-1`).
- `ZalithLauncher/src/test/java/com/movtery/zalithlauncher/game/version/mod/ModDoctorTest.kt` (untracked) — 10 new tests; SHA-256 `3f0ea4a44524d511b9e3ac1656eb1654a986d4b655751d30c6a270cac681ffc9`.
- Untouched and hash-verified: `_Download.Dependency.Tasks.kt` `8a922b54…`, `LauncherElements.kt` `f92d8e1d…`, `ModrinthSearcher.kt` `b707c059…`.
- APK unchanged (`91098012…`); no build was performed; tracked-file count moved `16` → `18` modified (the two new tracked edits) with `8` untracked entries unchanged.

### Scope boundary / limits
- The seam tests use injected lookups; the production `lookupInstalledMod` → `matchInstalledMods` path (network + MMKV cache) is exercised only on device — no device QA in this run.
- Provider choice still follows `AllSettings.searchModPlatform`; a locally decisive range (e.g. Balm's Fabric build declaring `>=1.21.10`) is still treated as compatible, so the previously recorded open-lower-bound product question is unchanged by this run.
- Fixability of a provider-detected wrong-version finding still uses the existing shared replacement path; this run added no new repair behavior.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\provider-wrong-version\`
- `00-summary.txt`, `TEST-ModDoctorTest-RED.xml` (24 tests / 2 failures / 0 errors)
- GREEN XMLs: `…ModDoctorTest.xml` (24), `…ModDoctorReplacementRepairTest.xml` (9), `…ModFileDisableSemanticsTest.xml` (2), `…ModrinthVersionPagingTest.xml` (2)

### Exact Next Action
`MICRO-RUN NEXT: build the Fast QA x86_64 debug APK from D:\ALL001 with provenance for the provider-backed WRONG_MINECRAFT_VERSION detection (InstalledMod game-version carry + cache-key v2 + validator rule) and verify the packaged source markers; do not install or run LDPlayer in that run.`

## MICRO-RUN — Fast QA APK build + packaged provenance for provider-backed WRONG_MINECRAFT_VERSION detection — PASS

Timestamp: `2026-09-17 13:48 +0700`.

### Result
- **PASS**: x86_64 non-minified debug APK built from alias `D:\ALL001` (Git root `D:\ALL Launcher 0.0.1`) containing the provider-backed wrong-version detection.
- Build/provenance only: no production source, test, or device state was modified in this run.

### STEP 1 — source provenance (pre-build = post-build, unchanged)
- `ModDoctor.kt` `2efefb90fd30c8161337c358c99cb8477deef061690634572fd70c9be377578e`
- `InstalledMod.kt` `5a681149e85ca7d8cea6e809523e7bf14f1118a874756e380c866f0bcf05989a`
- `InstalledModMatcher.kt` `be7d8296c0f806c903d5591ce6d76fdd961a6b69371dafbabc26df611fe2c17d`
- `ModDoctorTest.kt` `3f0ea4a44524d511b9e3ac1656eb1654a986d4b655751d30c6a270cac681ffc9`
- Logic confirmed present: `inspect()` builds and passes `providerGameVersions` (`:145-152`); provider rule `:823-841` (`locallyDecisive` / `providerIncompatible` + `is published for Minecraft …` reason); seam `:919-921`; `InstalledMod.gameVersions` `:44/:56/:63`; cache key `v2/` `InstalledModMatcher.kt:205`.
- Completed systems intact: missing-dependency fix (`resolveFixCandidate` `:516`, `getProjectByVersion(…, printLog = false)` `:693`, `installModFile` `:210/:261`), Modrinth paging (`ModrinthSearcher.kt:92/:107/:120`, `VERSION_PAGE_SIZE = 100`), post-Fix recheck (`LauncherElements.kt:378` → `:393`).

### STEP 2 — build
- Command: `gradlew.bat :ZalithLauncher:assembleDebug -Darch=x86_64 --no-daemon --console=plain` via `cmd.exe /d /c "cd /d D:\ALL001 && …"`.
- Start `2026-09-17 13:45:00 +07:00` → `BUILD SUCCESSFUL in 2m 48s`; `123 actionable tasks: 15 executed, 108 up-to-date`.
- x86_64-only (`[JREAssetsCleanup] arch: x86_64`, non-target LWJGL natives removed, `configureNdkBuildDebug[x86_64]` / `buildNdkBuildDebug[x86_64]`); no R8/minify/shrink task in the graph (`debug { isMinifyEnabled = false }`).
- Known non-fatal noise only: NDK `CreateProcess(… rm …/libawt_headless.so) failed` / `pipe: No error`; 4 build-key absence warnings (presence/absence only, no secret value read or stored).

### STEP 3 — APK verification
- Path: `D:\ALL Launcher 0.0.1\ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.1-x86_64.apk`; size `233267910` bytes; mtime `2026-09-17 13:47:48.908130000 +07:00`.
- SHA-256: `0d5bfe8804528f89d8ebfa4b52fc6d4df82c43a55ed8607ce6888269ba2eeeef`.
- ZIP integrity: PASS (`unzip -tqq` exit `0`). `aapt dump badging`: package `com.all.launcher.app.debug`, versionCode `1`, versionName `0.0.1-debug`, label `ALL Launcher`, `native-code: 'x86_64'`, sdkVersion `26`, targetSdkVersion `34`; packaged ABI entries only `lib/x86_64`.
- Superseded: `91098012…` (identical byte size, so the differing hash plus the newer mtime are the distinguishing evidence).

### STEP 4 — packaged provenance
- Dex payload extracted (`classes*.dex` concatenated, `85614028` bytes; temp copy outside the repo).
- NEW markers present: `is published for Minecraft ` ×1, `providerGameVersionsForAmbiguousMods` ×7, `providerGameVersions` ×11, `gameVersions` ×6.
- CONTROL markers present: `declares Minecraft ` ×1, `Automatic repair is unavailable: ` ×1, `Wrong Minecraft version` ×1, `Missing required dependency` ×1, `Wrong mod loader` ×1, `fixProblems` ×21, `getProjectByVersion` ×17; the old pre-fix literal `Automatic repair is unavailable: no authoritative compatible provider` ×0.
- Attribution: source mtimes `13:27:53`–`13:34:02` < build start `13:45:00` < APK mtime `13:47:48`, and the packaged code carries the new markers. Marker limit stated honestly: markers prove the packaged source state, not runtime behavior.

### STEP 5 — safety
- `git diff --check`: exit `0`; `git status --porcelain`: `26` entries (`18` modified tracked + `8` untracked) — identical to the pre-run baseline.
- No source or test file changed during the build; test XMLs still timestamped `13:36:28` (previous GREEN run, not re-run).
- No `adb` command was executed; no LDPlayer/emulator state was touched; no install and no device QA.

### Changed this run
- Source: **none**. Tests: **none**. APK: built (new artifact `0d5bfe88…`), replacing the superseded `91098012…`.
- Checkpoint + evidence file only.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\provider-wrong-version-apk-build\APK_PROVENANCE.txt`

### Exact Next Action
`MICRO-RUN NEXT: install the verified x86_64 debug APK (SHA-256 0d5bfe8804528f89d8ebfa4b52fc6d4df82c43a55ed8607ce6888269ba2eeeef) on LDPlayer 127.0.0.1:5555 and run the controlled provider-backed wrong-Minecraft-version device E2E with the real Balm Fabric fixture (project MBAkmtvl, version 9HjDG5Uj, balm-fabric-1.21.10-21.10.11.jar, SHA-1 6a7a8d104453aeceae8eb0a76944f64b32810665): snapshot the 1.21.11 Fabric mods directory with hashes, require the Mod Doctor report to identify Balm as a provider-confirmed wrong Minecraft version with Fix available, then stop at the earliest divergence (no Fix press required in that run unless explicitly authorized); do not rebuild or edit source.`

## MICRO-RUN — Provider-backed WRONG_MINECRAFT_VERSION device E2E — FAIL (first divergence: no report)

Timestamp: `2026-09-17 13:56 +0700`.

### Result
- **FAIL** — stopped at the first production-flow divergence (STEP 5): the expected wrong-version report never appeared, so the launch gate did not hold.
- QA-only: no production source, test, or APK was edited; no Fix was pressed; no UI action beyond the single launch tap.

### APK / environment
- APK SHA-256 gate passed: `0d5bfe8804528f89d8ebfa4b52fc6d4df82c43a55ed8607ce6888269ba2eeeef` (`233267910` bytes).
- LDPlayer only, serial `127.0.0.1:5555` (`Ld9BoxHeadless.exe` PID `38880` owns TCP `5555`); `emulator-5554` untouched.
- `adb install -r` → `Success`; `com.all.launcher.app.debug` versionCode `1` / `0.0.1-debug`, `lastUpdateTime 2026-09-17 13:54:31`; baseline `ALLQA001` (Offline) + `1.21.11 Fabric 0.19.5`, `MainActivity` PID `10959`.

### Fixture / snapshot
- Snapshot before: `fabric-api-0.141.6+1.21.11.jar` (`c98467cb…`/`bdff7fd7…`), `modmenu-17.0.0.jar` (`544a8b34…`/`95f2c957…`), `placeholder-api-2.8.2+1.21.10.jar` (`bbdc920e…`/`975c7e57…`); no disabled entries.
- Fixture added alone: `balm-fabric-1.21.10-21.10.11.jar` (`699812` bytes, SHA-1 `6a7a8d104453aeceae8eb0a76944f64b32810665` = authoritative Modrinth project `MBAkmtvl` / version `9HjDG5Uj`; device SHA-1 identical); unrelated mods untouched.
- Fixture local metadata (from its own JAR): `balm 21.10.11`, `depends: fabricloader ">=0.17.2", fabric-api ">=0.135.0+1.21.10", minecraft ">=1.21.10", java ">=21"`.

### First divergence (STEP 5)
- Launch pressed at `13:55:27` (button center `1351,834` derived from `05-ready-ui.xml`).
- **No Mod Doctor dialog at all**: every marker count in `06-after-launch-ui.xml` is `0` (`Mod Problems`, `Wrong Minecraft version`, `Wrong mod loader`, `Missing required dependency`, `Minecraft launch paused`, `Balm`, `Fix`).
- Launch window log (`08-launch-window-logcat.txt`, 325 lines): `ModDoctor` `0`, `PlatformSearch` `0`, `InstalledModMatcher` `0` lines → no provider lookup was even attempted for this file.
- Minecraft started and crashed: `13:55:30 START …VMActivity`; `09-minecraft-latest.log` `[13:55:37] Loading Minecraft 1.21.11 with Fabric Loader 0.19.5`, `Loading 52 mods:` including `balm 21.10.11`, then `[13:55:48] Mixin apply for mod balm failed … InvalidInjectionException … InvalidAccessorException`; VMActivity died → `ErrorActivity` (`JVM exit 1`, UI `Java Virtual Machine ปิดด้วยรหัส 1`).

### Mechanism (no source change made; implementation matches its own specification)
- The adopted product rule keeps local metadata primary and forbids provider data from overriding a decisive local compatibility. `minecraft ">=1.21.10"` against `1.21.11` is a decisive TRUE (`GameVersionNumber.compare("1.21.11","1.21.10") = +1`, operator `>=`), so `providerGameVersionsForAmbiguousMods` (filter `results.none { it == true }`) correctly never queried the provider — consistent with the zero `PlatformSearch`/`InstalledModMatcher` lines.
- The requested E2E is therefore **not reachable with this fixture**: a real Balm Fabric 1.21.10 build declares an open lower bound that the local rule treats as compatible. This is the same harm class recorded in `test-artifacts/mod-doctor-v2/wrong-minecraft-version-fix-e2e/00-DIVERGENCE-SUMMARY.txt`; the new provider-backed rule covers only the ambiguous-local-metadata case.

### UI actions / restore / safety
- No dialog appeared, so nothing was pressed (no Fix, Cancel, Restart, Exit or Share); the app was force-stopped only as part of the restore step.
- Restore PASS: fixture removed after force-stop; final directory = the original three files; SHA-1 **and** SHA-256 sets identical to the pre-QA snapshot; no `.disabled`/QA/temporary file left.
- `git status --porcelain`: `26` entries (unchanged); `git diff --check`: exit `0`; APK unchanged `0d5bfe88…`; source hashes unchanged (`ModDoctor.kt` `2efefb90…`, `InstalledMod.kt` `5a681149…`, `InstalledModMatcher.kt` `be7d8296…`, `ModDoctorTest.kt` `3f0ea4a4…`); test XMLs still `13:36:28`.
- Source changes: **none**. Test changes: **none**. APK changes: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\provider-wrong-version-e2e\`
- `00-DIVERGENCE-SUMMARY.txt`, `01-install-metadata.txt`, `02-baseline-ui.xml`, `03-mods-snapshot-before.txt`
- `04-fixture-added-state.txt`, `05-ready-ui.xml`, `06-after-launch-ui.xml`, `07-activity-after-launch.txt`
- `08-launch-window-logcat.txt`, `09-minecraft-latest.log`, `10-mod-restore-verified.txt`

### Exact Next Action
`MICRO-RUN NEXT (fixture/decision required before any device work): the provider-backed rule needs a fixture whose LOCAL declaration is non-decisive (e.g. a real Modrinth Fabric release declaring "~1.21-", "[1.21,1.22)", or no depends.minecraft) while its authoritative provider release lists game_versions excluding 1.21.11; find and hash such a real release, then run the same single controlled E2E against it (no source edit, no rebuild). If instead the product should also cover open-lower-bound ranges like ">=1.21.10", that is a separate product-rule decision that must be taken before any code change, because rule 2 currently forbids overriding a decisive local compatibility.`

**RESOLVED by the next micro-run:** the product rule was changed — open/broad local declarations are no longer treated as proof of release-level compatibility; provider data from an exact fingerprint match now decides them (RED→GREEN, focused tests only).

## MICRO-RUN — Broad/open local range: provider-backed WRONG_MINECRAFT_VERSION (RED → GREEN) — PASS

Timestamp: `2026-09-17 14:09 +0700`.

### Result
- **PASS**: the Balm-like case (local `>=1.21.10`, current 1.21.11, exact provider match whose `game_versions` exclude 1.21.11) now reports `WRONG_MINECRAFT_VERSION`, while every control stays clean.
- Source + tests only: no APK build, no install, no adb/LDPlayer, no repair behavior added.

### Why the previous rule failed
- Validator gated the provider rule on `locallyDecisive = rangeResults.any { it == true }`, and the seam only collected mods with `results.none { it == true }`.
- `>=1.21.10` vs `1.21.11` is a decisive TRUE (`GameVersionNumber.compare("1.21.11","1.21.10") = +1`, operator `>=`), so the Balm file was classified locally-compatible, the provider lookup was skipped entirely (device evidence: 0 `PlatformSearch` / 0 `InstalledModMatcher` lines) and no finding was produced — then the game crashed with mixin `InvalidInjectionException` / `InvalidAccessorException`.

### New product rule (adopted)
1. Local clear incompatibility → report with the existing local reason.
2. Local declaration that **precisely** proves release-level compatibility (all alternatives true **and** every alternative carries an upper bound `<`, e.g. `>=1.21.10 <1.21.12`) → never overridden by provider data.
3. Otherwise (ambiguous, empty, single-version, or broad/open such as `>=1.21.10` / `*`) + exact authoritative fingerprint match + non-empty provider `game_versions` excluding the current Minecraft version → report `WRONG_MINECRAFT_VERSION` for that exact installed release.
4. No exact match / unavailable / timeout / offline / empty `game_versions` → UNKNOWN, no report, no launch-blocking error.
5. Provider identity only from the exact installed-file fingerprint; no filename/title/fuzzy/approximate matching.

### STEP 2 — RED evidence
- Command: `:ZalithLauncher:testDebugUnitTest --tests …ModDoctorTest --no-daemon --console=plain` → `29 tests completed, 2 failed`, `BUILD FAILED in 1m 48s`, errors 0.
- Failing (behavioral): `broadOpenLocalRangeWithProviderReleaseExcludingCurrentVersionIsReported` (`ModDoctorTest.kt:403`) and `providerEnrichmentQueriesBroadOpenDeclarationsButSkipsPreciseOnes` (`:528`).
- Controls green in the same RED run: provider includes 1.21.11 → clean; no match → clean; empty `game_versions` → clean; timeout → clean; bounded range not overridden; local clear incompatibility still reported; all pre-existing tests. RED XML preserved.

### STEP 3 — smallest production change (`ModDoctor.kt` only)
- Three shared predicates replace the inline classification (used by both validator and seam): `minecraftRangeResults(...)`, `isLocallyIncompatible(...)`, `provesReleaseCompatibility(...)` (= non-empty, all true, every range contains `<`).
- Validator: `providerIncompatible = !mod.provesReleaseCompatibility(minecraftVersion) && providerVersions.isNotEmpty() && minecraftVersion !in providerVersions`; the local branch stays first so local findings keep their reason text.
- Seam candidates: `readable && !isLocallyIncompatible && !provesReleaseCompatibility` → precise declarations and already-reported incompatible mods are still never queried; broad/open ones now are.
- No new resolver, no repair, no dependency/loader change; `InstalledMod.kt` / `InstalledModMatcher.kt` untouched.

### STEP 4 — authority boundary / offline safety
- Boundary reused unchanged: `lookupInstalledMod` → `matchInstalledMods(computeModFingerprints(file))` → `InstalledMod{projectId, versionId, gameVersions}` (SHA-1 Modrinth / MurmurHash2 CurseForge, cache key `…/v2/…`).
- Missing match, `notFound` negative cache, empty `gameVersions`, exception or timeout all stay "unknown"; `CancellationException` still rethrown. Verified by `providerFailureOnBroadOpenLocalRangeLeavesItUnknown`, `broadOpenLocalRangeWithEmptyProviderGameVersionsIsNotReported`, `broadOpenLocalRangeWithoutAuthoritativeProviderMatchIsNotReported`, `providerEnrichmentKeepsOnlyAuthoritativeMatches`.

### STEP 5 — GREEN test results (one run, four focused classes)
- `BUILD SUCCESSFUL in 2m 40s`; JUnit XML: `ModDoctorTest` tests=29 failures=0 errors=0; `ModDoctorReplacementRepairTest` 9/0/0; `ModFileDisableSemanticsTest` 2/0/0; `ModrinthVersionPagingTest` 2/0/0 → total **42 tests, 0 failures, 0 errors**; no `<failure>`/`<error>` element in any XML.
- `git diff --check`: exit `0`. No APK built; no device work.

### Changed files
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/version/mod/ModDoctor.kt` (untracked) — SHA-256 `728aaa1130da00e82c04556e1e781f72cb785de44336d0f68f7e52223ac09a42` (was `2efefb90…`).
- `ZalithLauncher/src/test/java/com/movtery/zalithlauncher/game/version/mod/ModDoctorTest.kt` (untracked) — SHA-256 `32504cfb10e9226868b208f4533dca608e3f12b4219b8958bdcfd7c1534eb9f6` (was `3f0ea4a4…`).
- Unchanged and hash-verified: `InstalledMod.kt` `5a681149…`, `InstalledModMatcher.kt` `be7d8296…`, `_Download.Dependency.Tasks.kt` `8a922b54…`, `LauncherElements.kt` `f92d8e1d…`, `ModrinthSearcher.kt` `b707c059…`; APK `0d5bfe88…` untouched; `git status --porcelain` still `26` entries.
- Source changes other than the two files above: **none**. APK changes: **none**.

### Scope boundary / limits
- Seam tests use injected lookups; the production network + MMKV cache path is exercised only on device.
- `*` and single-version declarations are treated as "not precisely proven" and therefore also provider-verified; provider absence keeps them unknown, so no false positives arise.
- The device E2E for this new rule has not been run yet.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\provider-broad-range-rule\`
- `00-summary.txt`, `TEST-ModDoctorTest-RED.xml` (29 tests / 2 failures / 0 errors)
- GREEN XMLs: `…ModDoctorTest.xml` (29), `…ModDoctorReplacementRepairTest.xml` (9), `…ModFileDisableSemanticsTest.xml` (2), `…ModrinthVersionPagingTest.xml` (2)

### Exact Next Action
`MICRO-RUN NEXT: build the Fast QA x86_64 debug APK from D:\ALL001 with provenance for the broad/open-range provider rule (ModDoctor.kt 728aaa11…, ModDoctorTest.kt 32504cfb…) and verify the packaged source markers; do not install or run LDPlayer in that run.`

## MICRO-RUN — Fast QA APK build + packaged provenance for the broad/open-range provider rule — PASS

Timestamp: `2026-09-17 14:23 +0700`.

### Result
- **PASS**: x86_64 non-minified debug APK built from alias `D:\ALL001` (Git root `D:\ALL Launcher 0.0.1`) containing the broad/open-range provider rule.
- Build/provenance only: no production source, test, or device state was modified in this run.

### STEP 1 — source provenance (pre-build = post-build, unchanged)
- `ModDoctor.kt` `728aaa1130da00e82c04556e1e781f72cb785de44336d0f68f7e52223ac09a42`
- `ModDoctorTest.kt` `32504cfb10e9226868b208f4533dca608e3f12b4219b8958bdcfd7c1534eb9f6`
- `InstalledMod.kt` `5a681149…`, `InstalledModMatcher.kt` `be7d8296…`, `ModrinthSearcher.kt` `b707c059…`, `_Download.Dependency.Tasks.kt` `8a922b54…`, `LauncherElements.kt` `f92d8e1d…`
- Rule markers confirmed: `:822` provider gate uses `provesReleaseCompatibility`, `:827` local branch first, `:923-927` seam candidates, `:948/:952/:963` the three predicates (bounds test `range.contains('<')`), `:939-941` offline swallow with `CancellationException` rethrown; exact-fingerprint boundary reused via `lookupInstalledMod`.
- Completed systems intact: missing-dependency fix (`resolveFixCandidate` `:516`, `getProjectByVersion(…, printLog = false)` `:693`, `installModFile` `:210/:261/:276` → `downloadSingleForVersions` `:287`), dependency install path `_Download.Dependency.Tasks.kt:453`, post-Fix recheck `LauncherElements.kt:378` → `:393`, Modrinth paging `ModrinthSearcher.kt:92/:107/:120` (`VERSION_PAGE_SIZE = 100`).

### STEP 2 — build
- Command: `gradlew.bat :ZalithLauncher:assembleDebug -Darch=x86_64 --no-daemon --console=plain` via `cmd.exe /d /c "cd /d D:\ALL001 && …"`.
- Start `2026-09-17 14:20:26 +07:00` → `BUILD SUCCESSFUL in 1m 46s`; `123 actionable tasks: 15 executed, 108 up-to-date`.
- x86_64-only (`[JREAssetsCleanup] arch: x86_64`, non-target LWJGL natives removed, `configureNdkBuildDebug[x86_64]` / `buildNdkBuildDebug[x86_64]`); no R8/minify/shrink task (`debug { isMinifyEnabled = false }`).
- Known non-fatal noise only: NDK `CreateProcess(… rm …/libawt_headless.so) failed` / `pipe: No error`; 4 build-key absence warnings (presence/absence only, no secret value read or stored).

### STEP 3 — APK verification
- Path: `D:\ALL Launcher 0.0.1\ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.1-x86_64.apk`; size `233267910` bytes; mtime `2026-09-17 14:22:13.007161500 +07:00`.
- SHA-256: `8aaad6ad57d664eb84c0e2783cb4b60f442ffbf350c6b3f6acaddf6bb8f6ca98`.
- ZIP integrity PASS (`unzip -tqq` exit `0`); `aapt dump badging`: package `com.all.launcher.app.debug`, versionCode `1`, versionName `0.0.1-debug`, `native-code: 'x86_64'`; packaged ABI entries only `lib/x86_64`.
- Superseded: `0d5bfe88…` (identical byte size, so the differing hash plus the newer mtime are the distinguishing evidence).

### STEP 4 — packaged provenance
- Dex payload extracted (`classes*.dex`, `85613256` bytes; temp copy outside the repo).
- NEW markers present: `provesReleaseCompatibility` ×4, `isLocallyIncompatible` ×3, `minecraftRangeResults` ×3, `is published for Minecraft ` ×1, `providerGameVersionsForAmbiguousMods` ×4, `gameVersions` ×6.
- CONTROL markers present: `declares Minecraft ` ×1, `Automatic repair is unavailable: ` ×1, `fixProblems` ×21, `getProjectByVersion` ×17, `fetchVersionsPaged` ×13, `VERSION_PAGE_SIZE` ×1, `installModFile` ×15; the old literal `Automatic repair is unavailable: no authoritative compatible provider` ×0.
- Attribution: `ModDoctorTest.kt` mtime `14:03:21` and `ModDoctor.kt` mtime `14:06:05` < build start `14:20:26` < APK mtime `14:22:13`, and the packaged code carries the new rule markers. Marker limit stated honestly: markers prove the packaged source state, not runtime behavior.

### STEP 5 — safety
- `git diff --check`: exit `0`; `git status --porcelain`: `26` entries — identical to the pre-run baseline.
- No source or test file changed during the build; test XMLs still timestamped `14:08:52` (previous GREEN run, not re-run).
- No `adb` command was executed by this run (the resident `adb.exe` server from earlier sessions was not invoked); no LDPlayer/emulator state touched; no install and no device QA.

### Changed this run
- Source: **none**. Tests: **none**. APK: built (`8aaad6ad…`), replacing the superseded `0d5bfe88…`.
- Checkpoint + evidence file only.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\provider-broad-range-apk-build\APK_PROVENANCE.txt`

### Exact Next Action
`MICRO-RUN NEXT: install the verified x86_64 debug APK (SHA-256 8aaad6ad57d664eb84c0e2783cb4b60f442ffbf350c6b3f6acaddf6bb8f6ca98) on LDPlayer 127.0.0.1:5555 and re-run the single controlled broad/open-range device E2E with the real Balm Fabric fixture (balm-fabric-1.21.10-21.10.11.jar, project MBAkmtvl, version 9HjDG5Uj, SHA-1 6a7a8d104453aeceae8eb0a76944f64b32810665): snapshot the 1.21.11 Fabric mods directory with hashes, require the Mod Doctor report to show WRONG_MINECRAFT_VERSION for Balm with Fix available while Minecraft stays blocked, press Cancel only, then restore the original mod state; stop at the earliest divergence without editing source or rebuilding.`

## MICRO-RUN — Broad/open-range provider rule device E2E (Balm fixture re-run) — PASS

Timestamp: `2026-09-17 14:48 +0700` (device window 14:46 → 14:48).

### Result
- **PASS**: the exact Balm fixture that previously escaped detection is now reported as `WRONG_MINECRAFT_VERSION` before Minecraft starts, Cancel works, and the mods state was restored exactly.
- QA-only: no source, test, or APK change; no Fix press; no repair behavior added.

### APK / environment
- APK SHA-256 gate passed: `8aaad6ad57d664eb84c0e2783cb4b60f442ffbf350c6b3f6acaddf6bb8f6ca98`.
- LDPlayer only, serial `127.0.0.1:5555`; `emulator-5554` untouched. `adb install -r` → `Success`; `com.all.launcher.app.debug` versionCode `1` / `0.0.1-debug`, `lastUpdateTime 2026-09-17 14:46:44`; `MainActivity` resumed, PID `11711`; baseline `ALLQA001` + `1.21.11 Fabric 0.19.5`.

### Balm fixture / snapshot
- Snapshot before: `fabric-api-0.141.6+1.21.11.jar` (`c98467cb…`), `modmenu-17.0.0.jar` (`544a8b34…`), `placeholder-api-2.8.2+1.21.10.jar` (`bbdc920e…`) with SHA-256 recorded; no disabled entries.
- Fixture added alone: `balm-fabric-1.21.10-21.10.11.jar` (`699812` bytes, SHA-1 `6a7a8d104453aeceae8eb0a76944f64b32810665` = Modrinth `MBAkmtvl` / `9HjDG5Uj`; device SHA-1 identical); local metadata `minecraft ">=1.21.10"`; unrelated mods untouched.

### Detection / provider match
- Launch pressed `14:47:24` (button center `1351,834` from `05-ready-ui.xml`).
- One provider call in the window: `[14:47:23.492] [PlatformSearch/DEBUG] … {Official Modrinth}` — the exact-fingerprint match through the existing boundary (`lookupInstalledMod` → `matchInstalledMods(computeModFingerprints(file))` → Modrinth hash lookup → `InstalledMod.gameVersions`).
- Report (`06-mod-doctor-report-ui.xml`, text in `09-report-text.txt`): `Mod Problems` / `Minecraft launch paused. Found 1 mod problem(s).` / `Wrong Minecraft version: Balm is published for Minecraft 1.21.10, not 1.21.11.` / `File: balm-fabric-1.21.10-21.10.11.jar` → provider `game_versions` = `["1.21.10"]` excludes 1.21.11; affected file is exactly the fixture; `Automatic repair is unavailable` absent.

### Launch gate / dialog / Cancel
- Minecraft did NOT start: `mResumedActivity` stayed `MainActivity` (`07-activity-after-launch.txt`); zero `VMActivity` occurrences in the window log.
- Dialog actions: `ยกเลิก` (Cancel) / `Fix` / `ใช้ไปก่อนเถอะ เนอะ` (Launch anyway). `Fix` was present and **not** pressed; only Cancel was pressed (`800,483`, derived from the report hierarchy) at `14:48:04`.
- After Cancel: dialog closed, home screen returned (`10-after-cancel-ui.xml`: `ALLLauncher` present, `Mod Problems`/`Wrong Minecraft version`/`Fix` = 0), `MainActivity` resumed with PID `11711` alive, package crash markers `0`.

### Restore / safety
- Restore PASS: app force-stopped, fixture removed; final directory = the original three files; `SHA1_SET_IDENTICAL` and `SHA256_SET_IDENTICAL` vs the pre-QA snapshot; no QA/test/disabled/temp file left.
- `git status --porcelain`: `26` entries (unchanged); `git diff --check`: exit `0`; APK unchanged `8aaad6ad…`; source unchanged (`ModDoctor.kt` `728aaa11…`, `ModDoctorTest.kt` `32504cfb…`).
- Source changes: **none**. Test changes: **none**. APK changes: **none**.

### Context
- The same fixture at `13:55` (pre-rule APK) produced no report at all, Fabric loaded `balm 21.10.11`, and the game crashed with mixin `InvalidInjectionException` / `InvalidAccessorException` (JVM exit 1 → `ErrorActivity`). With the broad/open-range rule that harm case is now gated before launch.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\provider-broad-range-e2e-final\`
- `00-QA-SUMMARY.txt`, `01-install-metadata.txt`, `02-baseline-ui.xml`, `03-mods-snapshot-before.txt`
- `04-fixture-added-state.txt`, `05-ready-ui.xml`, `06-mod-doctor-report-ui.xml`, `07-activity-after-launch.txt`
- `08-launch-window-logcat.txt`, `09-report-text.txt`, `10-after-cancel-ui.xml`, `11-activity-after-cancel.txt`, `12-mod-restore-verified.txt`

### Exact Next Action
`MICRO-RUN NEXT: verify the provider-backed wrong-Minecraft-version finding is also FIXABLE on device — run one controlled E2E that presses Fix on the Balm fixture and requires the existing shared same-project replacement path (fingerprint-matched project MBAkmtvl → newest Fabric build for 1.21.11 → original JAR preserved as .disabled → clean second Mod Doctor pass → Minecraft Main Menu), then restore the original mods state; no source edit, no rebuild, and stop at the earliest divergence.`

## MICRO-RUN — WRONG_MINECRAFT_VERSION Fix device E2E (Balm same-project replacement) — PASS

Timestamp: `2026-09-17 14:57 +0700` (device window 14:53 → 14:57).

### Result
- **PASS**: the provider-detected wrong-version finding was repaired on device through the existing shared same-project replacement path; the game reached the Main Menu and the original mods state was restored exactly.
- QA-only: no source, test, or APK change; no repair behavior added; no fuzzy/cross-project selection.

### APK / environment
- APK SHA-256 gate passed: `8aaad6ad57d664eb84c0e2783cb4b60f442ffbf350c6b3f6acaddf6bb8f6ca98`.
- LDPlayer only, serial `127.0.0.1:5555`; `emulator-5554` untouched. `adb install -r` → `Success`; `com.all.launcher.app.debug` versionCode `1` / `0.0.1-debug`, `lastUpdateTime 2026-09-17 14:53:13`; `MainActivity` PID `12259`; baseline `ALLQA001` + `1.21.11 Fabric 0.19.5`.

### Original Balm identity
- File `balm-fabric-1.21.10-21.10.11.jar`, `699812` bytes, SHA-1 `6a7a8d104453aeceae8eb0a76944f64b32810665`; Modrinth project `MBAkmtvl`, version `9HjDG5Uj`, version_number `21.10.11+fabric-1.21.10`, declared `game_versions ["1.21.10"]`, local metadata `minecraft ">=1.21.10"`.
- Snapshot before: `fabric-api-0.141.6+1.21.11.jar` (`c98467cb…`), `modmenu-17.0.0.jar` (`544a8b34…`), `placeholder-api-2.8.2+1.21.10.jar` (`bbdc920e…`); no disabled entries.

### Detection → Fix
- Launch `14:53:57` → report `Wrong Minecraft version: Balm is published for Minecraft 1.21.10, not 1.21.11. File: balm-fabric-1.21.10-21.10.11.jar`; Fix present; Minecraft blocked (`MainActivity` stayed resumed).
- Fix pressed `14:54:32` (`800,546` hierarchy-derived). Device log: `[14:54:30.468] PlatformSearch {Official Modrinth}` → `[14:54:30.818] Downloading: https://cdn.modrinth.com/data/MBAkmtvl/versions/5POKgjJn/balm-fabric-1.21.11-21.11.9.1.jar` → `[14:54:32.031] DownloadSingle … clear cached resource files`.

### Same-project replacement / old-file preservation
- Replacement project `MBAkmtvl` (same as the fingerprint-matched installed file) — same-project guard satisfied; no cross-project, no NeoForge/Forge build.
- Replacement version `5POKgjJn`, version_number `21.11.9.1+fabric-1.21.11`, declared `game_versions ["1.21.11"]`, `loaders ["fabric"]`, file `balm-fabric-1.21.11-21.11.9.1.jar`, size `873058`, SHA-1 `5bb8ca8fb46953da5777c1b3c12af5b397b46b48` (verified against the Modrinth API for that version and identical to the installed device file).
- Old file preserved by the existing `.disabled` rename: `balm-fabric-1.21.10-21.10.11.jar.disabled` (`699812`, SHA-1 `6a7a8d10…`, inactive).
- Unrelated mods unchanged; exactly one active Balm; no unexpected/duplicate file.

### Second pass / Minecraft / restore / safety
- Second Mod Doctor pass clean: exactly 1 `Downloading` line in the fix window, `Mod Problems` 0, `Wrong Minecraft version` 0 → recheck clean, launch continued.
- Minecraft: `[14:54:43] Loading Minecraft 1.21.11 with Fabric Loader 0.19.5`, `Loading 52 mods:` with `- balm 21.11.9.1`, `Setting user: ALLQA001`, LWJGL 3.3.3, `Sound engine started` → **Main Menu** (screenshot; version string `Minecraft 1.21.11/ALLlauncher0.1-debug/Fabric`). Balm mixin failures `0` (previous `InvalidInjectionException`/`InvalidAccessorException` class gone); fatal markers `0`.
- Restore PASS: QA replacement and `.disabled` backup removed (disabled file pulled back and hash-verified first, SHA-1 `6a7a8d10…`); final directory = the original three files; `SHA1_SET_IDENTICAL` and `SHA256_SET_IDENTICAL`; no QA/test/disabled/temp file remains.
- `git status --porcelain`: `26` entries (unchanged); `git diff --check`: exit `0`; APK unchanged `8aaad6ad…`; source unchanged (`ModDoctor.kt` `728aaa11…`, `ModDoctorTest.kt` `32504cfb…`).
- Source changes: **none**. Test changes: **none**. APK changes: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\wrong-version-fix-e2e\`
- `00-QA-SUMMARY.txt`, `01-install-metadata.txt`, `02-baseline-ui.xml`, `03-mods-snapshot-before.txt`, `04-fixture-added-state.txt`
- `05-ready-ui.xml`, `06-wrong-version-report-ui.xml`, `06b-report-text.txt`, `07-detection-logcat.txt`, `08-fix-phase-logcat.txt`
- `09-mods-after-fix.txt`, `10-activity-after-fix.txt`, `11-minecraft-latest.log`, `12-minecraft-main-menu.png`, `13-minecraft-activity.txt`
- `14-mod-restore-verified.txt`, `qa-returned-balm-1.21.10.jar`

### Exact Next Action
`MICRO-RUN NEXT: cover the remaining report-only detectors with one controlled device scenario each, starting with DUPLICATE (two enabled copies of the same logical mod id) to confirm Mod Doctor reports it without offering an unsafe automatic repair; snapshot and restore the 1.21.11 Fabric mods directory, no source edit, no rebuild, and stop at the earliest divergence.`

## MICRO-RUN — DUPLICATE mod detection device E2E (report-only) — PASS

Timestamp: `2026-09-17 15:05 +0700`.

### Result
- **PASS**: two active files with the same logical mod id are reported as `DUPLICATE`, Minecraft stays blocked, no automatic (unsafe) duplicate repair is offered, Cancel works, and the original mods state was restored exactly.
- QA-only: no source, test, or APK change; no duplicate repair implemented; nothing deleted/disabled/renamed.

### APK / baseline
- APK SHA-256 gate passed: `8aaad6ad57d664eb84c0e2783cb4b60f442ffbf350c6b3f6acaddf6bb8f6ca98` (already-installed artifact reused).
- `com.all.launcher.app.debug` versionCode `1` / `0.0.1-debug`; baseline UI `ALLQA001` (Offline) + instance `1.21.11 Fabric 0.19.5`; `MainActivity` resumed (PID 13004).
- Snapshot before: `fabric-api-0.141.6+1.21.11.jar` (`c98467cb…`), `modmenu-17.0.0.jar` (`544a8b34…`), `placeholder-api-2.8.2+1.21.10.jar` (`bbdc920e…`), SHA-256 recorded; no disabled entries.

### Duplicate fixture
- Source mod: `modmenu-17.0.0.jar` (Mod Menu); its own `fabric.mod.json` declares id `modmenu`, version `17.0.0`; sha1 `544a8b34…`.
- Controlled duplicate: byte-identical **active** copy `modmenu-17.0.0-copy.jar` (same size `1115809`, same sha1), created on device with `cp` — valid because the implemented rule groups readable local mods by logical mod id regardless of filename.
- Unrelated mods untouched.

### Detection / launch gate
- Launch `15:03:33` → report: `Mod Problems` / `Minecraft launch paused. Found 1 mod problem(s).` / `Duplicate mod: Mod Menu is installed more than once: modmenu-17.0.0.jar, modmenu-17.0.0-copy.jar.` / `File: modmenu-17.0.0.jar, modmenu-17.0.0-copy.jar` — duplicated mod id `modmenu`, both filenames named.
- Exactly one finding; no dependency/loader/version noise. Minecraft/VMActivity did NOT start (`mResumedActivity` stayed `MainActivity`; zero `VMActivity` lines in the launch window).

### Fix availability (report-only safety)
- `Fix` button **absent** (`Fix` count 0). Dialog actions were only `ยกเลิก` (Cancel) and `ใช้ไปก่อนเถอะ เนอะ` (Launch anyway) — the button is gated on `report.hasFixableProblems`, and a DUPLICATE finding carries no `fixCandidate`/`replacementFix`. No automatic duplicate repair was offered or performed.

### Cancel / restore / safety
- Cancel pressed `15:04:12` (`800,529` hierarchy-derived); dialog closed, home screen returned (`ALLLauncher` present, `Mod Problems`/`Duplicate mod` = 0), `MainActivity` PID 13004 alive, crash markers `0`, `VMActivity` `0`.
- Restore PASS: app force-stopped, duplicate copy removed; final directory = the original three files; `SHA1_SET_IDENTICAL` and `SHA256_SET_IDENTICAL`; no QA/temp/disabled file remains.
- `git status --porcelain`: `26` entries (unchanged); `git diff --check`: exit `0`; APK unchanged `8aaad6ad…`; source unchanged (`ModDoctor.kt` `728aaa11…`, `ModDoctorTest.kt` `32504cfb…`).
- Source changes: **none**. Test changes: **none**. APK changes: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\duplicate-detection-e2e\`
- `00-QA-SUMMARY.txt`, `01-baseline-package.txt`, `02-baseline-ui.xml`, `03-mods-snapshot-before.txt`, `04-duplicate-state.txt`
- `05-ready-ui.xml`, `06-duplicate-report-ui.xml`, `06b-report-text.txt`, `07-activity-after-launch.txt`, `08-launch-window-logcat.txt`
- `09-dialog-actions.txt`, `10-after-cancel-ui.xml`, `11-activity-after-cancel.txt`, `12-mod-restore-verified.txt`, `source-modmenu-17.0.0.jar`

### Exact Next Action
`MICRO-RUN NEXT: run one controlled device E2E for the CORRUPT_METADATA (unreadable mod metadata) detector — place a single active JAR/ZIP whose metadata cannot be read by the existing reader in the 1.21.11 Fabric mods directory, require the report to name the affected file and block Minecraft, confirm no unsafe repair is offered, press Cancel only, restore the exact snapshot, and stop at the earliest divergence; no source edit, no rebuild.`

## MICRO-RUN — CORRUPT / unreadable mod metadata detection device E2E (report-only) — PASS

Timestamp: `2026-09-17 15:33 +0700` (device window 15:31 → 15:32).

### Result
- **PASS**: one controlled active unreadable mod is reported as `Unreadable/corrupt mod metadata`, Minecraft stays blocked, no Fix is offered, Cancel works, and the original mods state was restored exactly.
- QA-only: no source, test, or APK change; nothing deleted/disabled/renamed; Launch Anyway not pressed.

### APK / baseline
- APK SHA-256 gate passed: `8aaad6ad57d664eb84c0e2783cb4b60f442ffbf350c6b3f6acaddf6bb8f6ca98`.
- `com.all.launcher.app.debug` versionCode `1` / `0.0.1-debug`; baseline UI `ALLQA001` (Offline) + instance `1.21.11 Fabric 0.19.5`; `MainActivity` resumed.
- Snapshot before: `fabric-api-0.141.6+1.21.11.jar` (`c98467cb…`), `modmenu-17.0.0.jar` (`544a8b34…`), `placeholder-api-2.8.2+1.21.10.jar` (`bbdc920e…`) with SHA-256; no disabled entries.

### Corrupt fixture
- `qa-corrupt-metadata.jar` — `220` bytes, SHA-1 `f4624f6a1de5459c23741b2b6765433adf81523c`, SHA-256 `d5af2868c42a1e3ca860faf10d090c8a0f94cfcf29b1c82fb2a175ebb2bed24d`.
- Valid ZIP/JAR containing only `README.txt` — no `fabric.mod.json`, `mcmod.info`, `neoforge.mods.toml` or `pack.mcmeta`, so `AllModReader` falls through every `jar` reader, calls `createNotMod(file)` (`notMod = true`), and `ModDoctor` maps that to `readable = false` → `CORRUPT_METADATA`.
- Device SHA-1 after push identical; no real user mod was modified.

### Detection / launch gate
- Launch `15:31:25` → report: `Mod Problems` / `Minecraft launch paused. Found 1 mod problem(s).` / `Unreadable/corrupt mod metadata: qa-corrupt-metadata.jar does not contain readable supported mod metadata.` / `File: qa-corrupt-metadata.jar` — exactly one finding naming the fixture.
- Minecraft/VMActivity did NOT start (`mResumedActivity` stayed `MainActivity`; zero `VMActivity` lines in the launch window).

### Fix availability / Cancel
- `Fix` button **absent** (`Fix` count 0); dialog actions only `ยกเลิก` (Cancel) and `ใช้ไปก่อนเถอะ เนอะ` (Launch anyway). No unsafe repair offered; Launch Anyway not pressed.
- Cancel pressed `15:32:02` (`800,529` hierarchy-derived): dialog closed, home screen returned (`ALLLauncher` present, markers 0), `MainActivity` PID `13465` alive, crash markers `0`, `VMActivity` `0`.

### Restore / safety
- Restore PASS: app force-stopped, fixture removed; final directory = the original three files; `SHA1_SET_IDENTICAL` and `SHA256_SET_IDENTICAL`; no QA/temp/.disabled file remains.
- `git status --porcelain`: `26` entries (unchanged); `git diff --check`: exit `0`; APK unchanged `8aaad6ad…`; source unchanged (`ModDoctor.kt` `728aaa11…`, `ModDoctorTest.kt` `32504cfb…`).
- Source changes: **none**. Test changes: **none**. APK changes: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\corrupt-metadata-e2e\`
- `00-QA-SUMMARY.txt`, `01-baseline-package.txt`, `02-baseline-ui.xml`, `03-mods-snapshot-before.txt`, `04-fixture-identity.txt`
- `05-ready-ui.xml`, `06-corrupt-report-ui.xml`, `06b-report-text.txt`, `07-dialog-actions.txt`, `08-activity-after-launch.txt`
- `09-launch-window-logcat.txt`, `10-after-cancel-ui.xml`, `11-activity-after-cancel.txt`, `12-mod-restore-verified.txt`, `qa-corrupt-metadata.jar`

### Exact Next Action
`MICRO-RUN NEXT: run one controlled device E2E for the WRONG_LOADER detector using a real NeoForge build of Balm (project MBAkmtvl, release 21.11.9.1+neoforge-1.21.11, version tKWaiIeW, SHA-1 4def274a463ebc949319305d0177c3e9d84d7782 as recorded earlier): snapshot the 1.21.11 Fabric mods directory with hashes, require the Wrong mod loader finding to name the file and offer Fix, press Cancel only (no Fix), restore the exact snapshot, and stop at the earliest divergence; no source edit, no rebuild.`

## MICRO-RUN — Offline / provider-failure safety device E2E (provider blocked, then restored) — PASS

Timestamp: `2026-09-17 16:02 +0700` (device window 15:53 → 16:01).

### Result
- **PASS**: with provider access unavailable, Mod Doctor produced no provider-backed false positive and did not block the launch; the normal path continued. Network and mods were restored, and a control run with the provider reachable produced the expected finding for the same file.
- QA-only: no source, test, or APK change; no repair behavior added; Fix never pressed.

### Fixture (deliberately uncached)
- `balm-fabric-1.21.10-21.10.10.jar` — `699837` bytes, SHA-1 `8b37ea3751405dce7e96eb1c9024eb266ca5ad78` (Modrinth project `MBAkmtvl` / version `YQB6scYL`, provider `game_versions ["1.21.10"]`), local metadata `minecraft ">=1.21.10"` (broad/open → enrichment-eligible). Its SHA-1 had never been matched before, so no MMKV cache entry could mask the provider call (the earlier 21.10.11 fixture was cached from the previous E2E and was therefore not reused).
- Snapshot before: `fabric-api-0.141.6+1.21.11.jar` (`c98467cb…`), `modmenu-17.0.0.jar` (`544a8b34…`), `placeholder-api-2.8.2+1.21.10.jar` (`bbdc920e…`); no disabled entries; baseline `ALLQA001` + `1.21.11 Fabric 0.19.5`.

### Provider failure method (smallest reversible lever; device has no root)
- `settings put global http_proxy 127.0.0.1:9`, mirrored by the framework into `global_http_proxy_host=127.0.0.1` / `global_http_proxy_port=9` (the keys the app actually consumed). `su` is absent, adbd cannot run as root and `iptables` is unusable as shell, so a UID firewall rule was not available.
- Underlying device network untouched: `ping -c 2 api.modrinth.com` = 2/2, 0% loss during the block; ADB connectivity unaffected.

### Phase A — provider blocked (local detection unchanged, no false positive)
- Launch `15:54:26` → **no** `Mod Problems` dialog, **no** `Wrong Minecraft version`, **no** `Fix`, **no** `Minecraft launch paused` (all markers 0); provider failure treated as UNKNOWN.
- Log evidence: `PlatformSearcher: Failed to perform the operation on source: {Official Modrinth}` / `java.net.ConnectException: Failed to connect to /127.0.0.1:9` (`ECONNREFUSED`), `[InstalledModMatcher/WARNING] Failed to match installed mods on platform: MODRINTH`.
- Normal launch continued (`Start VMActivity`, `Loading 52 mods:` incl. `- balm 21.10.10`). Expected, honestly recorded consequence of "unknown ≠ blocking": Fabric loaded the 1.21.10-only Balm build into 1.21.11 and the game crashed on Balm's own mixins (`InvalidInjectionException` / `InvalidAccessorException`, JVM exit 1 → `ErrorActivity`). Mod Doctor itself produced no finding and no gate.

### Network restore (with one recovery step)
- `settings delete global http_proxy` cleared the legacy key, but a fresh app process still attempted `127.0.0.1:9` even after the split keys were emptied → residual proxy state in the app environment. Recovery: guest reboot (`adb reboot`; `/proc/uptime` `51s` confirms it). Afterwards all proxy keys and props are empty.

### Phase B — provider restored, same fixture (control)
- Launch `16:00:31` → `Mod Problems` / `Minecraft launch paused. Found 1 mod problem(s).` / `Wrong Minecraft version: Balm is published for Minecraft 1.21.10, not 1.21.11.` / `File: balm-fabric-1.21.10-21.10.10.jar`; Fix present; Minecraft blocked; provider connect errors in the window `0`.
- This proves the Phase A clean result came from the unavailable provider (not a broken fixture/cache) and that provider connectivity was fully restored.

### Cancel / mods restore / safety
- Cancel `16:01:15` (`800,483`): dialog closed, home screen returned (`ALLLauncher` present, `Mod Problems` = 0), `MainActivity` healthy, crash markers `0`; Fix not pressed.
- Fixture removed; final directory = the original three files; `SHA1_SET_IDENTICAL` and `SHA256_SET_IDENTICAL`; no QA/temp/disabled file remains.
- `git status --porcelain`: `26` entries (unchanged); `git diff --check`: exit `0`; APK unchanged `8aaad6ad…`; source unchanged (`ModDoctor.kt` `728aaa11…`, `ModDoctorTest.kt` `32504cfb…`).
- Source changes: **none**. Test changes: **none**. APK changes: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\offline-provider-failure-e2e\`
- `00-QA-SUMMARY.txt`, `01-baseline.txt`/`.xml`, `02-mods-snapshot-before.txt`, `03-provider-block.txt`, `04-fixture-added-state.txt`
- `05-ready-ui.xml`, `06-after-launch-ui.xml`, `07-launch-result.txt`, `08-offline-window-logcat.txt`, `09-minecraft-latest.log`
- `10-network-restore.txt`, `11*-ready-ui-restored.xml`, `12d-report-ui-restored-network.xml`, `13d-phase-b-result.txt`, `14d-phase-b-logcat.txt`
- `15-after-cancel-ui.xml`, `16-mods-restore-verified.txt`, `balm-fabric-1.21.10-21.10.10.jar`

### Exact Next Action
`MICRO-RUN NEXT: regression smoke on the clean reference instance — with only the original three mods active (no fixtures) and the provider reachable, launch through the real UI and require a clean Mod Doctor no-op plus the Minecraft 1.21.11 / Fabric 0.19.5 Main Menu, confirming no false-positive gating after all detector work; snapshot and restore the mods directory, no source edit, no rebuild.`

## MICRO-RUN — Clean reference instance smoke (final detector-layer regression) — PASS

Timestamp: `2026-09-17 16:11 +0700` (device window 16:09 → 16:11).

### Result
- **PASS**: the untouched reference instance produces a clean Mod Doctor no-op and launches to the Minecraft Main Menu, with the mods directory byte-identical before and after.
- QA-only: no source, test, or APK change; no fixtures created; no provider/network settings touched; no Fix invoked.

### Clean baseline
- APK SHA-256 re-verified `8aaad6ad57d664eb84c0e2783cb4b60f442ffbf350c6b3f6acaddf6bb8f6ca98` (unchanged artifact).
- `ALLQA001` (Offline) current; instance `1.21.11 Fabric 0.19.5`; launcher UI healthy.
- Provider/network normal: all proxy keys and `http.proxyHost/Port` empty, `ping -c 2 api.modrinth.com` 2/2 packets, 0% loss.
- Active mods = exactly the three original files (no QA/fixture/.disabled entry):
  `fabric-api-0.141.6+1.21.11.jar` (`c98467cb…`/`bdff7fd7…`), `modmenu-17.0.0.jar` (`544a8b34…`/`95f2c957…`),
  `placeholder-api-2.8.2+1.21.10.jar` (`bbdc920e…`/`975c7e57…`).

### Mod Doctor — clean / no-op
- Launch `16:09:12`; no `Mod Problems` dialog and no interruption of the normal launch path. All markers 0:
  `Mod Problems`, `Missing required dependency`, `Wrong mod loader`, `Wrong Minecraft version`, `Duplicate mod`,
  `Unreadable/corrupt mod metadata`, `Fix`, `Minecraft launch paused`, `Automatic repair is unavailable`.
- Provider consulted normally with no failure: `ConnectException` 0, `Failed to match installed mods` 0 → no false positive.
- Evidence limitation stated honestly: once the game surface owns the screen, `uiautomator dump` cannot capture the
  launcher hierarchy, so the no-op conclusion rests on `VMActivity` being resumed plus the zero-marker log and the
  absence of any dialog interruption before the game process started.

### Minecraft
- `VMActivity` resumed; `[16:09:22] Loading Minecraft 1.21.11 with Fabric Loader 0.19.5`, `Loading 50 mods:` with
  `- fabric-api 0.141.6+1.21.11`, `- modmenu 17.0.0`, `- placeholder-api 2.8.2+1.21.10`, `[16:09:36] Setting user: ALLQA001`,
  `[16:09:38] LWJGL 3.3.3-snapshot`, `[16:09:45] OpenAL initialized / Sound engine started` → **Main Menu**
  (screenshot version string `Minecraft 1.21.11/ALLlauncher0.1-debug/Fabric (มอด) 50`).
- Game crash lines 0; launcher/game fatal markers 0; only pre-existing mod-unrelated noise (JNA, Narrator, offline Realms 401).

### Post-launch integrity / safety
- Mods directory byte-identical to baseline (`SHA1_SET_IDENTICAL`, `SHA256_SET_IDENTICAL`); no `.disabled`/temp/QA file created.
- Provider/network still normal after the run; game process force-stopped after evidence capture (no mods change).
- `git status --porcelain`: `26` entries (unchanged); `git diff --check`: exit `0`; APK unchanged; source unchanged.
- Source changes: **none**. Test changes: **none**. APK changes: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\clean-reference-smoke\`
- `00-QA-SUMMARY.txt`, `00-apk.txt`, `01-network-state.txt`, `02-baseline-ui.xml`, `03-mods-snapshot-before.txt`, `04-ready-ui.xml`
- `05-activity-after-launch.txt`, `07-mod-doctor-noop.txt`, `08-launch-window-logcat.txt`, `09-minecraft-main-menu.png`
- `10-minecraft-latest.log`, `11-mods-after-launch.txt`, `12-post-launch-state.txt`, `13-network-after.txt`

### Exact Next Action
`MICRO-RUN NEXT: with the detection layer fully verified on device (missing dependency, wrong loader, wrong Minecraft version incl. provider-backed broad/open ranges, duplicate, corrupt metadata, offline safety, clean no-op), prepare the release-candidate path exactly as the BUILD policy requires — start with one focused non-minified release/QA verification step and record its provenance; do not start R8/minify as an iterative loop, and do not change source.`

## MICRO-RUN — Release/QA non-minified artifact — BLOCKED at STEP 1 (no viable variant without a config change)

Timestamp: `2026-09-17 16:19 +0700`.

### Result
- **BLOCKED** before any packaging build: no existing Gradle task produces a release-like **and** non-minified artifact, and the requirement cannot be met without editing `ZalithLauncher/build.gradle.kts` (forbidden this run) or accepting an R8-minified artifact (excluded by the success condition).
- Inspection-only run: no source, test, or config edit; no keystore/password file created or modified; no secrets read or printed; no build/packaging task executed; no adb/LDPlayer work.

### STEP 1 — build configuration inspection
- Variants: `debug` and `release` only; **no product flavors**. Tasks: `assembleDebug` / `assembleRelease` (+ androidTest/unitTest and bundle variants).
- `release` build type: `isMinifyEnabled = true`, `isShrinkResources = true`, `signingConfig = releaseBuild`, `proguardFiles(proguard-android-optimize.txt, proguard-rules.pro)` → **R8/minified**; cannot be OFF without a script edit.
- `debug` build type: `isMinifyEnabled = false`, `applicationIdSuffix = ".debug"`, `versionNameSuffix = "-debug"` → the only non-minified variant, but debug identity (and identity changes are out of scope).
- Identity / version / ABI: `applicationId = com.all.launcher.app` (debug → `.debug`), `versionCode = 1`, `versionName = 0.0.1`; `splits.abi` driven by `-Darch=x86_64` → x86_64-only.
- Signing (presence/state only, no values): `releaseBuild` → `zalith_launcher.jks` **PRESENT**, alias `movtery_zalith`; `debugBuild` → `zalith_launcher_debug.jks` **PRESENT**. Passwords come from `getKeyFromLocal(env, optional file, default)`: env `STORE_PASSWORD`/`KEY_PASSWORD` unset and `.store_password.txt`/`.key_password.txt` **ABSENT** (root and module dir), so the current build uses the `default_store_password`/`default_key_password` fallback in `gradle.properties` (values never read or printed).
- Empirical signing check: `:ZalithLauncher:validateSigningRelease` → **BUILD SUCCESSFUL in 16s** ⇒ the configured release signing material is loadable; **signing is not the blocker**.

### Why blocked (exact missing requirement)
- `assembleRelease` = release identity but R8 minify + resource shrink ON → violates "R8/minify remains OFF" (and would be the project's R8 final gate, reserved as its own milestone by the BUILD policy).
- `assembleDebug` = non-minified but debug identity, and it is already the verified QA artifact (`8aaad6ad…`).
- Unblock options: **(a)** approve ONE non-minified release-like build type in `ZalithLauncher/build.gradle.kts` (e.g. `create("releaseQa") { initWith(getByName("release")); isMinifyEnabled = false; isShrinkResources = false; signingConfig = signingConfigs.getByName("releaseBuild") }`) then build `:ZalithLauncher:assembleReleaseQa -Darch=x86_64`; or **(b)** approve `assembleRelease` (R8 ON) explicitly as the R8 final gate.

### STEP 2 — provenance snapshot (unchanged by this run)
- Branch `all-0.0.1`; HEAD `b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e` ("bump version to 2.5.3"); `git status --porcelain` 26 entries (baseline); `git diff --check` exit `0`.
- `build.gradle.kts` `671f00f2b27dba928a3cd16b553619c8be1263216754225d9de3e30772917797`, `gradle.properties` `1668c8ec3f45f99bf170bbfb442968ef8a18d0822bd885223664f291c04fc6b6`, `zalith_launcher.jks` `d865642a785bfbaecd09307d535d0cf3b3c219e5beefaeadd6cd7f7a311398cc` (all unchanged after the run).
- Critical source hashes: `ModDoctor.kt` `728aaa11…`, `InstalledMod.kt` `5a681149…`, `InstalledModMatcher.kt` `be7d8296…`, `ModrinthSearcher.kt` `b707c059…`, `LauncherElements.kt` `f92d8e1d…`, `_Download.Dependency.Tasks.kt` `8a922b54…`.
- Current verified QA artifact (reference, untouched): debug APK `8aaad6ad…`, `233267910` bytes, mtime `2026-09-17 14:22:13`; `build/outputs/apk/` contains only the debug directory — no release artifact exists.

### STEP 6 — safety
- `git diff --check`: exit `0`; `git status --porcelain`: `26` entries (baseline); config/keystore hashes identical before/after; no password file created anywhere; no adb/device work; no install.
- Source changes: **none**. Test changes: **none**. Config changes: **none**. APK changes: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\release-qa-artifact\`
- `00-BLOCKED-STEP1.txt`, `00-provenance.txt`

### Exact Next Action
`MICRO-RUN NEXT (decision required): choose (a) add the single non-minified release-like build type ("releaseQa": initWith(release), isMinifyEnabled = false, isShrinkResources = false, release signing) and build :ZalithLauncher:assembleReleaseQa -Darch=x86_64 with provenance, or (b) authorize assembleRelease (R8 ON) as the project's R8 final gate. No build should be started before this decision.`

## MICRO-RUN — Real Release / R8 gate (`assembleRelease`) — FAIL (first blocker: lintVitalAnalyzeRelease, JDK/lint environment)

Timestamp: `2026-09-17 17:18 +0700` (build window 16:26:04 → ~17:13).

### Result
- **FAIL** on the single authorized real release build; the first blocking error was captured and **no repair, config edit or second build attempt** was made (per instruction).
- No release APK was produced (`ZalithLauncher/build/outputs/apk/release/` does not exist).

### STEP 1 — pre-build gate: PASS
- branch `all-0.0.1`; HEAD `b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e`; `git status --porcelain` 26 entries; `git diff --check` exit `0`.
- Critical systems present: provider broad/open rule (3 refs), missing-dependency + replacement fixes (5), Modrinth paging (`VERSION_PAGE_SIZE`, 2), post-Fix recheck (1), offline cancellation rethrow (9).
- Release config verified: `applicationId = com.all.launcher.app`, `versionCode = 1`, `versionName = 0.0.1`, `isMinifyEnabled = true`, `isShrinkResources = true`; signing valid (`validateSigningRelease` UP-TO-DATE; no secret read or printed).
- Hashes: `ModDoctor.kt` `728aaa11…`, `InstalledMod.kt` `5a681149…`, `InstalledModMatcher.kt` `be7d8296…`, `ModrinthSearcher.kt` `b707c059…`, `LauncherElements.kt` `f92d8e1d…`, `_Download.Dependency.Tasks.kt` `8a922b54…`; `build.gradle.kts` `671f00f2…`, `gradle.properties` `1668c8ec…`, `proguard-rules.pro` `45cb8c21…`, `zalith_launcher.jks` `d865642a…`.

### STEP 2 — build
- Command: `gradlew.bat :ZalithLauncher:assembleRelease -Darch=x86_64 --no-daemon --console=plain` from `D:\ALL001`.
- `BUILD FAILED in 47m 4s`; `194 actionable tasks: 193 executed, 1 up-to-date`. Log: `C:\Users\pornp\AppData\Local\Temp\release_r8_gate.log` (copy: `02-release-build.log`).

### STEP 3 — first real blocker
- Failing task: **`:ZalithLauncher:lintVitalAnalyzeRelease`**.
- Exception: `com.android.build.gradle.internal.lint.AndroidLintWorkAction` → "Unexpected failure during lint analysis (this is a bug in lint or one of the libraries it depends on)".
- Root cause in the stack: `java.lang.NoSuchMethodError: 'java.lang.Object java.util.List.removeLast()'` at `JavaDocParser.parseDataItem(JavaDocParser.kt:117)` → `JavaDocParser.parseDocCommentText(…)` → FIR/Java analysis chain while lint visits `Launcher.kt` → `LintDriver$Companion.handleDetectorError(LintDriver.kt:3603)` → `AndroidLintWorkAction.runLint`.
- Category: **build-path / tooling-environment** — Gradle ran on Temurin **17.0.20.1** (`gradlew -version`: Gradle 9.5.0, Launcher JVM 17.0.20.1, Daemon JVM `jdk-17.0.20.101-hotspot`), while the lint library's analysis code calls `List.removeLast()`, which exists only from JDK 21.
- Not a source defect, not an R8/ProGuard failure, not a signing failure, not a packaging failure.
- Secondary note: the daemon reported JVM metaspace exhaustion at the end of the build (log daemon warning: max heap 4 GiB / metaspace 512 MiB).
- No repair was attempted; no ProGuard/R8 rule, source or config change was made.

### What did run (the gate did reach real minification)
- `> Task :ZalithLauncher:minifyReleaseWithR8` executed and **did not fail**; R8 artifacts exist under `build/outputs/mapping/release/`: `configuration.txt`, `seeds.txt`, `usage.txt`, `resources.txt`, `mapping.prt` (16,189,239 bytes), `configanalyzer.pb/.html`.
- Earlier stages executed: `compileReleaseKotlin`, `hiltJavaCompileRelease`, `transformReleaseClassesWithAsm`, `configureNdkBuildRelease[x86_64]` / `buildNdkBuildRelease[x86_64]`.

### STEP 6 — safety
- `git diff --check` exit `0`; `git status --porcelain` 26 entries; config/ProGuard/keystore/source hashes identical before and after.
- No source, test, Gradle-config, ProGuard or signing file was modified; no keystore/password value read, printed or stored; no adb/LDPlayer work; no install.
- Source changes: **none**. Test changes: **none**. Config changes: **none**. APK changes: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\release-r8-gate\`
- `00-FAIL-first-blocker.txt`, `01-prebuild-gate.txt`, `02-release-build.log`, `03-first-blocker.txt`, `04-jdk.txt`, `05-postbuild-hashes.txt`

### Exact Next Action
`MICRO-RUN NEXT (decision required before any further build): choose (a) rerun the identical release command with a JDK 21+ Gradle JVM so Android Lint's List.removeLast() exists (lint vital stays enforced; environment-only change), or (b) rerun the identical release command with -x lintVitalRelease (invocation-only; produces the real R8/minified release APK while accepting that lint vital is skipped in that gate). Optionally (c) raise the Gradle daemon metaspace if the metaspace warning recurs. Do not edit source, build.gradle.kts or ProGuard rules for this.`

## MICRO-RUN — Real Release / R8 gate under JDK 21+ — FAIL (first new blocker: configureNdkBuildRelease[x86_64])

Timestamp: `2026-09-17 17:30 +0700` (build window 17:26:09 → 17:27:08).

### Result
- **FAIL** on the single authorized JDK-21+ release build; the first **new** blocker was captured and nothing was fixed, skipped or reconfigured.
- No release APK was produced (`build/outputs/apk/` still contains only `debug/`).

### JDK used (build-process environment only)
- Only JDK 21+ already installed: Android Studio bundled JetBrains Runtime — vendor JetBrains s.r.o., version **25.0.2**, `C:\Program Files\Android\Android Studio\jbr\bin\java.exe`. (Other installs are Temurin 17.0.20.1 and 8.0.504 — both < 21. No JDK was downloaded or installed.)
- Applied only for this build process: `JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'` + that `bin` prepended to PATH. Proof: `java -version` → openjdk 25.0.2; `gradlew -version` → Gradle 9.5.0, `Launcher JVM: 25.0.2 (JetBrains s.r.o.)`, `Daemon JVM: C:\Program Files\Android\Android Studio\jbr` ⇒ **Gradle ran on JDK 25 (≥21)**; no persistent Windows env change was made.

### STEP 3 — pre-build safety: PASS
- branch `all-0.0.1`; HEAD `b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e`; `git status --porcelain` 26 entries; `git diff --check` exit `0`; hashes matched the checkpoint (`build.gradle.kts` `671f00f2…`, `gradle.properties` `1668c8ec…`, `proguard-rules.pro` `45cb8c21…`, `zalith_launcher.jks` `d865642a…`, `ModDoctor.kt` `728aaa11…`, `ModDoctorTest.kt` `32504cfb…`).

### STEP 4 — build
- Command: `gradlew.bat :ZalithLauncher:assembleRelease -Darch=x86_64 --no-daemon --console=plain` from `D:\ALL001` with the JDK-25 environment; lint **not** skipped, R8/shrink untouched.
- `BUILD FAILED in 56s`; `9 actionable tasks: 9 executed`.

### STEP 5 — first new blocker
- Failing task: **`:ZalithLauncher:configureNdkBuildRelease[x86_64]`**.
- Error: `com.android.ide.common.process.ProcessException` surfaced as `[CXX1429] error when building with ndkBuild using D:\ALL Launcher 0.0.1\ZalithLauncher\src\main\jni\Android.mk: Android NDK: Your APP_BUILD_SCRIPT points to an unknown file: D:\ALL Launcher 0.0.1\ZalithLauncher\src\main\jni\Android.mk`; `…/ndk/25.2.9519653/build/../build/core/add-application.mk:88: *** Android NDK: Aborting... . Stop.`
- Category: **build-path / tooling-environment** (NDK `ndk-build` configure; the real repository path contains spaces — `D:\ALL001` is a Windows **JUNCTION** to `D:\ALL Launcher 0.0.1`, so the space-containing path reaches ndk-build even when the build starts from the alias).
- Old `List.removeLast()` JDK-17 blocker: **not exercised in this run** — zero `lintVital` and zero `removeLast` occurrences in the log because the build died at NDK configure. Cannot be claimed gone (or persisting). Note: in the previous JDK-17 run both `configureNdkBuildRelease[x86_64]` and `buildNdkBuildRelease[x86_64]` had succeeded, and lint was the only failure.
- Cause hypotheses (unproven, single run): (1) JDK-25-dependent behaviour of the ndk-build configure invocation, (2) the space-containing `JAVA_HOME` value breaking a cmd/make quoting step, (3) the pre-existing path-with-spaces NDK class recorded earlier in this project. No fix attempted.

### STEP 8 — metaspace
- Metaspace / JVM-memory warning occurrences in this run's log: **0** (no warning). Gradle JVM args were left untouched; nothing changed preemptively.

### STEP 9 — safety
- `git diff --check` exit `0`; `git status --porcelain` 26 entries; config/ProGuard/keystore/source hashes identical before and after.
- No source, test, Gradle-config, ProGuard or signing file modified; no secret read/printed/stored; no ADB/device action; no install; no persistent environment change.
- Source changes: **none**. Test changes: **none**. Config changes: **none**. APK changes: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\release-r8-gate-jdk21\`
- `00-FAIL-first-blocker.txt`, `01-jdk-env.txt`, `02-prebuild-safety.txt`, `03-release-build-jdk25.log`, `04-first-blocker.txt`, `05-metaspace.txt`, `06-postbuild-hashes.txt`

### Exact Next Action
`MICRO-RUN NEXT (decision required before any further build): isolate the new NDK configure blocker without touching source/config — recommended: rerun this exact release command with the SAME JDK 25 JVM but from a space-free working copy/alias so the space hypothesis is testable, or alternatively rerun it with the SAME JBR JDK installed/copied to a space-free path (e.g. C:\jdk25) to separate "JDK 25" from "JAVA_HOME with spaces". Do not skip lint, do not disable R8/resource shrinking, and do not edit source/build.gradle.kts/ProGuard.`

## MICRO-RUN — JBR 25 from a space-free JAVA_HOME (`C:\jdk25`) — CASE B: same NDK failure → FAIL

Timestamp: `2026-09-17 17:42 +0700` (build window 17:39:16 → 17:39:40).

### Result
- **FAIL / CASE B** — the space-free `JAVA_HOME` did **not** change anything: identical NDK configure failure.
  Stopped immediately per instruction (project not moved/copied, no alternative JDK tried, no fix attempted).
- No release APK produced (`build/outputs/apk/` still only `debug/`).

### STEP 1-2 — JDK copy + identity (process-local env only)
- Copied the runtime only: `C:\Program Files\Android\Android Studio\jbr` → **`C:\jdk25`** (robocopy attempt failed on quoting; used `cp -r`). Fidelity: 426 files == 426 files; `release`: `JAVA_VERSION="25.0.2"`, `IMPLEMENTOR="JetBrains s.r.o."`; `C:\jdk25\bin\java.exe -version` → openjdk 25.0.2 build `25.0.2+-15348964-b329.117`.
- Original Android Studio JBR untouched (`release` mtime still `2026-07-27`); the project was **not** copied or moved (`D:\ALL001` still a JUNCTION to `D:\ALL Launcher 0.0.1`).
- `JAVA_HOME=C:\jdk25` + `C:\jdk25\bin` prepended to PATH for the build process only. Proof: `java -version` → 25.0.2, `which java` → `/c/jdk25/bin/java`; `gradlew -version` → Gradle 9.5.0, `Launcher JVM: 25.0.2 (JetBrains s.r.o.)`, `Daemon JVM: C:\jdk25`. Same Java version as the previous run; only the JAVA_HOME path differed.

### STEP 3 — pre-build safety: PASS
- branch `all-0.0.1`; HEAD `b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e`; status 26 entries; `git diff --check` rc 0; all critical hashes matched the checkpoint.

### STEP 4-5 — build + CASE B classification
- Command identical: `gradlew.bat :ZalithLauncher:assembleRelease -Darch=x86_64 --no-daemon --console=plain` from `D:\ALL001`; lint not skipped. `BUILD FAILED in 22s`; `10 actionable tasks: 2 executed, 8 up-to-date`.
- Failing task: **`:ZalithLauncher:configureNdkBuildRelease[x86_64]`** (same).
- Android.mk path: `D:\ALL Launcher 0.0.1\ZalithLauncher\src\main\jni\Android.mk` (same).
- Error: `com.android.ide.common.process.ProcessException` → `[CXX1429] error when building with ndkBuild using …Android.mk: Android NDK: Your APP_BUILD_SCRIPT points to an unknown file: …Android.mk`; `add-application.mk:88: *** Android NDK: Aborting... . Stop.` (3 abort occurrences, same pattern).
- **Conclusion: a space in `JAVA_HOME` is not sufficient (and per this evidence not necessary) to explain the NDK configure failure.**
- Old lint/`List.removeLast()` blocker: **NOT REACHED** (`lintVital` 0, `removeLast` 0 occurrences) — status stays unproven.
- Metaspace warning: **0** occurrences; Gradle JVM args untouched.

### Cross-run correlation (same project/alias/NDK 25.2.9519653)
- JDK **17.0.20.1**: `configureNdkBuildRelease[x86_64]` + `buildNdkBuildRelease[x86_64]` both **succeeded** on this same space-containing path.
- JDK **25.0.2** (JBR, space-containing JAVA_HOME): NDK configure **fails**.
- JDK **25.0.2** (JBR copy, space-free `C:\jdk25`): NDK configure **fails identically**.
- ⇒ the discriminator tracks the **Gradle JVM version (25 vs 17)**, not the JAVA_HOME path. Correlational only; mechanism unproven, no further experiments run.

### STEP 7 — safety
- `git diff --check` rc 0; status 26 entries; source/tests/config/ProGuard/signing hashes identical before and after; project not copied/moved; original JBR untouched; no ADB/device work; no secrets read/printed; no persistent environment change.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\release-r8-gate-jdk25-nospace\`
- `00-CASE-B-FAIL.txt`, `01-jbr-copy.txt`, `02-prebuild-safety.txt`, `03-release-build-jdk25-nospace.log`, `04-CASE-B-classification.txt`, `05-postbuild-hashes.txt`

### Exact Next Action
`MICRO-RUN NEXT (decision required): the two blockers are mutually exclusive per JDK — JDK 17 passes NDK configure but fails lint vital (List.removeLast(), needs >= JDK 21), while JDK 25 (JBR) fails at NDK configure before lint. Recommended decision: stop trying to make JBR 25 work with this path layout and instead obtain an actual JDK 21 (e.g. Temurin 21) as the Gradle JVM — JDK 21 satisfies lint's >= 21 requirement and is the version AGP/Gradle tooling is validated against. If that is not acceptable, the alternatives are (a) copy the project to a space-free root (previously forbidden; would need authorization) or (b) keep JDK 17 and accept skipping lint vital (previously forbidden). No source/config/ProGuard edits in any case.`

## MICRO-RUN — Temurin JDK 21 as Gradle JVM (`assembleRelease`) — GATE A/PASS, GATE B/PASS, GATE C first blocker = SIGNING

Timestamp: `2026-09-17 18:48 +0700` (build window 17:57:56 → 18:44:01).

### Result
- **FAIL** — the JDK conflict is resolved (NDK gate and lint gate both PASS, R8 and resource shrinking both ran), but the build now stops at the **first new blocker: release signing** (`:ZalithLauncher:packageRelease`). Stopped there; nothing fixed.
- No release APK produced (`build/outputs/apk/release/` exists but is empty).

### JDK 21 used (process-local environment only)
- vendor **Eclipse Adoptium (Temurin)**; version **21.0.12.1+1 LTS** (`JAVA_VERSION="21.0.12.1"`, `IMPLEMENTOR_VERSION="Temurin-21.0.12.1+1"`, `OS_ARCH="x86_64"`); `C:\jdk21\bin\java.exe`.
- Source: official Adoptium API `https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse` → `OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.zip`, 205,073,461 bytes, SHA-256 `f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e` — **matches the official checksum from the Adoptium API**.
- Portable ZIP extracted to `C:\jdk21` (no MSI/system-wide install); JDK 17, `C:\jdk25` and the Android Studio JBR all left intact; no persistent JAVA_HOME/PATH change.
- Proof: `gradlew -version` → Gradle 9.5.0, `Launcher JVM: 21.0.12.1 (Eclipse Adoptium 21.0.12.1+1-LTS)`, `Daemon JVM: C:\jdk21`.

### STEP 3 — pre-build safety: PASS
- branch `all-0.0.1`; HEAD `b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e`; status 26 entries; `git diff --check` rc 0; all critical hashes matched the checkpoint (including `ModrinthSearcher.kt b707c059…`, `LauncherElements.kt f92d8e1d…`, `_Download.Dependency.Tasks.kt 8a922b54…`).

### STEP 4-5 — build and gates
- Command unchanged: `gradlew.bat :ZalithLauncher:assembleRelease -Darch=x86_64 --no-daemon --console=plain` from `D:\ALL001`; lint not skipped; no `-x`, no JVM-arg change. `BUILD FAILED in 43m 56s`; `202 actionable tasks: 38 executed, 164 up-to-date`.

**GATE A — NDK: PASS** — `configureNdkBuildRelease[x86_64]` and `buildNdkBuildRelease[x86_64]` both executed successfully; `Android NDK: Aborting` occurrences = 0 ⇒ **the JDK 25 (JBR) CXX1429 / APP_BUILD_SCRIPT blocker is GONE**, confirming it tracked the JVM version rather than the JAVA_HOME path.

**GATE B — LINT: PASS** — `lintVitalAnalyzeRelease` executed successfully (plus `lintVitalReportRelease`, `lintVitalRelease`, and the sibling-module lint tasks); `removeLast` / `NoSuchMethodError` occurrences = 0 ⇒ **the JDK 17 `java.util.List.removeLast()` blocker is GONE** (lint fully enforced on JDK 21).

**GATE C — R8 / SHRINK: ran clean** — `minifyReleaseWithR8` executed without failure; `convertShrunkResourcesToBinaryRelease` + `optimizeReleaseResources` executed (resource shrinking ran). Then:
- Failing task: **`:ZalithLauncher:packageRelease`**.
- Exception: `com.android.ide.common.signing.KeytoolException` (inside `PackageAndroidArtifact$IncrementalSplitterRunnable`).
- Excerpt: `Failed to read key movtery_zalith from store "D:\ALL001\ZalithLauncher\zalith_launcher.jks": keystore password was incorrect`.
- Category: **signing** (keystore/key access) — not source, not R8, not resource shrinking, not packaging logic, not JVM memory. Keystore file itself unchanged (`zalith_launcher.jks d865642a…`). No credential was read, printed or stored; no signing config/keystore change was made.

### STEP 8 — metaspace
- The daemon metaspace warning **did appear** in this run ("The Daemon will expire after the build after running out of JVM Metaspace"; configured max heap 4 GiB / max metaspace 512 MiB). Recorded only — Gradle JVM args were **not** changed.

### STEP 8 — safety
- `git diff --check` rc 0; status 26 entries; all critical hashes identical before and after; source/tests/config/ProGuard/signing unchanged; project location unchanged (junction intact); JDK 17 / `C:\jdk25` / Android Studio JBR intact; no ADB/device actions; no persistent environment change; no secrets exposed.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\release-r8-gate-jdk21-temurin\`
- `00-GATES-and-SIGNING-blocker.txt`, `01-jdk21-env.txt`, `02-prebuild-safety.txt`, `03-critical-file-paths.txt`, `04-release-build-jdk21.log`, `05-gates-and-first-blocker.txt`, `06-postbuild-hashes.txt`

### Exact Next Action
`MICRO-RUN NEXT (decision required — signing credentials are not guessable and no credential may be printed): the release key alias movtery_zalith in zalith_launcher.jks is not openable with the password currently supplied to the build (env / gradle.properties fallback). Decide the correct path: (a) supply the correct keystore/key password for the existing keystore through the environment for that build only (never echo it), (b) authorise regeneration of a new release keystore (changes the signing identity of the app — user decision, not an agent action), or (c) leave the release gate blocked at signing and record it as such. Do not edit build.gradle.kts, gradle.properties or ProGuard, and do not skip lint/R8/resource shrinking.`

## MICRO-RUN — New ALL Launcher release signing identity created + wired — PASS

Timestamp: `2026-09-17 19:05 +0700`.

### Result
- **PASS** — a NEW ALL-owned release signing identity exists, is wired into the release build type through the project's existing secret seam, is excluded from Git, and Gradle proved it can open it (`validateSigningRelease` executed under JDK 21). No APK was built; nothing else changed.
- Product decision applied: the upstream-derived identity (`zalith_launcher.jks` / alias `movtery_zalith`) is retired for release purposes but **left completely intact**; its password was never recovered, guessed, read or reused.

### New ALL signing identity
- keystore: `D:\ALL Launcher 0.0.1\private-signing\all_launcher_release.jks` (4,324 bytes; PKCS12; RSA 4096; self-signed)
- alias: **`all_launcher`** (`PrivateKeyEntry`)
- certificate metadata (non-secret): owner/issuer `CN=ALL Launcher, O=ALL Launcher, C=TH`; valid `Thu Sep 17 18:56:38 ICT 2026` → `Sat Sep 09 18:56:38 ICT 2056`; signature `SHA384withRSA`; public key 4096-bit RSA; cert SHA-256 `B7:2B:76:BD:56:F3:2A:E1:6A:3C:10:BA:C9:FA:D1:BD:68:A8:D4:8A:DA:27:3F:CF:45:30:F8:54:08:9D:21:77`
- secure credential storage: `private-signing\all_launcher_release_password.txt` exists (32 bytes, no trailing newline so `getKeyFromLocal` reads it verbatim); the value was never printed/echoed/hashed/logged/stored in any artifact or report; `keytool` received it via `-storepass:env` (process-local) so it never appeared in a command line either.
- credentials NOT tracked by Git: root `.gitignore` gained `/private-signing/`; `git check-ignore -v` proves coverage of both files, and `git status --porcelain` lists neither. Local access restricted with `icacls /inheritance:r /grant:r "<current user>:(OI)(CI)F"`.

### Release signing wiring (minimum change)
- `ZalithLauncher/build.gradle.kts` → `signingConfigs.releaseBuild`: `storeFile = file("../private-signing/all_launcher_release.jks")`, `storePassword = getKeyFromLocal("ALL_STORE_PASSWORD", "private-signing/all_launcher_release_password.txt")`, `keyAlias = "all_launcher"`, `keyPassword = getKeyFromLocal("ALL_KEY_PASSWORD", "private-signing/all_launcher_release_password.txt")`. Reused the existing `getKeyFromLocal` seam (env → `File(rootDir, fileName)`); no password literal in `build.gradle.kts` or `gradle.properties`; new env names prevent a stale upstream variable from silently applying.
- Unchanged: `applicationId` `com.all.launcher.app`, versionCode 1 / versionName 0.0.1, debug signingConfig, `isMinifyEnabled = true`, `isShrinkResources = true`, proguard files.

### Gradle validation
- `:ZalithLauncher:validateSigningRelease` under Temurin JDK 21 (`JAVA_HOME=C:\jdk21`, process-local) reported UP-TO-DATE on the first call (cache hit — proves nothing), so it was re-run with `--rerun-tasks`: task **executed** → `BUILD SUCCESSFUL in 16s`, `1 actionable task: 1 executed` ⇒ Gradle resolves the new store, alias and key through the private credential file. No `assembleRelease`, no R8, no packaging.

### Old Zalith signing material
- **UNTOUCHED** — `zalith_launcher.jks` `d865642a…` and `zalith_launcher_debug.jks` `0d2f2900…` identical to the checkpoint; nothing deleted, overwritten, renamed or password-inspected. (Pre-existing note: both files are tracked in the repository from upstream; this run did not change that.)

### Safety
- No APK built (`build/outputs/apk/release/` empty; the debug APK is the pre-existing 14:22:13 artifact, untouched); no ADB/device action; `proguard-rules.pro 45cb8c21…` unchanged; Mod Doctor source/tests unchanged (`ModDoctor.kt 728aaa11…`, `ModDoctorTest.kt 32504cfb…`); `git diff --check` rc 0; status 27 entries with only `.gitignore` and `ZalithLauncher/build.gradle.kts` attributable to this run (the latter's new delta is exactly the four signing lines).

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\release-signing-identity-all\`
- `00-NEW-ALL-SIGNING-IDENTITY.txt`, `01-signing-wiring-diff.txt`, `02-unchanged-hashes.txt`, `03-private-signing.txt`

### Exact Next Action
`MICRO-RUN NEXT: re-run the real Release/R8 gate with Temurin JDK 21 and the new ALL signing identity — gradlew.bat :ZalithLauncher:assembleRelease -Darch=x86_64 --no-daemon --console=plain from D:\ALL001 with process-local JAVA_HOME=C:\jdk21 — then verify the release APK (com.all.launcher.app, versionCode 1 / 0.0.1, x86_64 only, non-debuggable, R8 + resource shrinking applied, signed by cert B7:2B:76…). Do not skip lint, do not disable R8/shrink, do not modify the new signing wiring.`

## MICRO-RUN — FIRST COMPLETE SIGNED ALL LAUNCHER RELEASE APK — PASS

Timestamp: `2026-09-17 19:20 +0700` (build window 19:10:35 → 19:13:22).

### Result
- **PASS** — one `assembleRelease` on Temurin JDK 21 with the new ALL signing identity produced the first complete, minified, resource-shrunk, ALL-signed release APK. Nothing was skipped and nothing changed during the run.

### JDK
- process-local `JAVA_HOME=C:\jdk21`; `gradlew -version` → Gradle 9.5.0, **Launcher JVM 21.0.12.1 (Eclipse Adoptium)**, **Daemon JVM `C:\jdk21`**.

### Gate results
- **NDK**: PASS (`configureNdkBuildRelease[x86_64]` / `buildNdkBuildRelease[x86_64]` already up-to-date from this JDK-21 toolchain; 0 `Android NDK: Aborting`).
- **Lint**: PASS — `lintVitalAnalyzeRelease`, `lintVitalReportRelease`, `lintVitalRelease` all executed in this build.
- **R8 / minify**: PASS — `minifyReleaseWithR8` UP-TO-DATE (its output from the same JDK-21 toolchain is the input consumed here); release dex = 2 files / 12,191,528 B vs debug 31 files / 85,613,256 B, with all class/method identifiers obfuscated. Evidence: `build/outputs/mapping/release/` (`mapping.txt` 163,055,862 B, `usage.txt`, `seeds.txt`, `configuration.txt`, `resources.txt`).
- **Resource shrinking**: PASS — `convertShrunkResourcesToBinaryRelease` + `optimizeReleaseResources` (UP-TO-DATE, same toolchain); `resources.arsc` 2,769,708 B vs debug 3,726,616 B; removed-resource report `resources.txt` 900,964 B.
- **Signing**: PASS — `packageRelease` succeeded; `apksigner verify`: Verifies=true, **v2 scheme true**, 1 signer, cert DN `CN=ALL Launcher, O=ALL Launcher, C=TH`, cert SHA-256 `b72b76bd56f32ae16a3c10bac9fad1bd68a8d48ada273fcf4530f854089d2177` = **exact match** with the recorded ALL certificate `B7:2B:76:…:9D:21:77`; signer key RSA 4096. No password/private material exposed.
- Build summary: `BUILD SUCCESSFUL in 2m 43s`, `203 actionable tasks: 10 executed, 193 up-to-date`.

### Release APK
- path: `D:\ALL Launcher 0.0.1\ZalithLauncher\build\outputs\apk\release\ALLLauncher-0.0.1-x86_64.apk`
- filename: `ALLLauncher-0.0.1-x86_64.apk` (**no `.debug` suffix**)
- size: **193,000,893 bytes** · mtime: `2026-09-17 19:13:17 +07:00` (newer than build start 19:10:35)
- SHA-256: **`920b6a70cab666c727775698ad451850d11c4fded54e57e416f722488c2c7f4a`**
- package `com.all.launcher.app` · versionCode `1` · versionName `0.0.1` · label `ALL Launcher` · minSdk 26 / targetSdk 34
- architecture: **x86_64 only** (`native-code: 'x86_64'`) · **non-debuggable** (0 `debuggable` matches) · ZIP integrity **PASS**

### Packaged provenance (release vs debug dex byte-substring counts)
- `is published for Minecraft ` 1/1 · `Wrong Minecraft version` 1/1 · `Missing required dependency` 1/1 · `Duplicate mod` 1/1 · `Unreadable/corrupt mod metadata` 1/1 (all current detector literals packaged)
- `fixProblems` 0/21 · `getProjectByVersion` 0/17 · `provesReleaseCompatibility` 0/4 · `VERSION_PAGE_SIZE` 0/1 · `ModDoctor` 7/249 (R8-renamed ⇒ real minification)
- Source hashes unchanged from the checkpoint (`ModDoctor.kt 728aaa11…`, `ModDoctorTest.kt 32504cfb…`, `InstalledMod.kt 5a681149…`, `InstalledModMatcher.kt be7d8296…`, `ModrinthSearcher.kt b707c059…`, `LauncherElements.kt f92d8e1d…`, `_Download.Dependency.Tasks.kt 8a922b54…`) ⇒ built from the current source state. **[SUPERSEDED 2026-09-18: this held only at 19:20 on 2026-09-17. `ModDoctor.kt` was rewritten to `210cf50d…` at 23:09 that day, so this APK stopped matching current source; it was rebuilt on 2026-09-18 as `530b2804…` — see the newest section.]** Paging/post-Fix-recheck seams are R8-renamed, so their provenance rests on source-hash equality plus the packaged literals above (labelled as such).

### Metaspace
- Warning occurrences in this build log: **0** (none). Gradle JVM args not changed.

### Safety
- `git diff --check` rc 0; status 27 entries; during the build nothing changed: `build.gradle.kts 39ffc458…`, `gradle.properties 1668c8ec…`, `proguard-rules.pro 45cb8c21…`, `zalith_launcher.jks d865642a…`, `zalith_launcher_debug.jks 0d2f2900…`, Mod Doctor source/tests unchanged.
- `private-signing/` still excluded and untracked (0 tracked files, absent from status); the release credential does **not** appear in the build log (presence-only check, value never printed); no ADB/device action.
- Source/config changes this run: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v2\release-first-all-signed-apk\`
- `00-PASS-first-signed-release-apk.txt`, `01-env.txt`, `02-prebuild-gate.txt`, `03-release-build.log`, `04-apk-identity.txt`, `05-signature.txt`, `06-provenance.txt`, `07-postbuild-unchanged.txt`

### Exact Next Action
`MICRO-RUN NEXT (device gate): install the verified release APK ALLLauncher-0.0.1-x86_64.apk (SHA-256 920b6a70…) on LDPlayer 127.0.0.1:5555 as a NEW package com.all.launcher.app side-by-side with the existing debug package, preserve all existing package data, then verify install metadata, first-run launch, and that no release-specific regression (R8/minify) broke the launcher — separate the installation/launch baseline from any Mod Doctor E2E.`

## MICRO-RUN — Signed R8 Release APK device startup gate (LDPlayer) — PASS

Timestamp: `2026-09-17 19:36 +0700` (device window ~19:30 → 19:34).

### Result
- **PASS** — the signed, R8-minified release APK installs as `com.all.launcher.app`, coexists with the debug package, starts MainActivity/Splash up cleanly, renders the ALL Launcher UI, uses its own data path, and causes no change to the debug package or its data. No source/config change.

### Release install
- APK gate: SHA-256 `920b6a70cab666c727775698ad451850d11c4fded54e57e416f722488c2c7f4a` (exact), 193,000,893 B, mtime 19:13:17, package `com.all.launcher.app`, vc 1, vn 0.0.1, `native-code: 'x86_64'` only, non-debuggable, cert SHA-256 `b72b76…2177`.
- Pre-state: `com.all.launcher.app` was **not** installed; debug package `com.all.launcher.app.debug` present (vn `0.0.1-debug`, uid 10066, dataDir `/data/user/0/com.all.launcher.app.debug`, firstInstallTime 2026-09-16 12:33:37, external files `control_layouts`, `logs`, `moddoctor-v1-backup`).
- Install (fresh, LDPlayer `127.0.0.1:5555` only): **Success** → `com.all.launcher.app` vc 1 · vn **0.0.1** · uid **10060** · dataDir `/data/user/0/com.all.launcher.app` · firstInstallTime `2026-09-17 19:30:54`; both packages coexist.

### First launch
- Launched the resolved activity `com.all.launcher.app/com.movtery.zalithlauncher.ui.activities.SplashActivity`: `am start -W` → Status ok, TotalTime 3479 ms; process pid 5367 alive; `mResumedActivity` = that splash activity.
- **R8 runtime: clean** — 0 fatal markers in the cleared logcat window (no FATAL EXCEPTION, `VerifyError`, `NoSuchMethodError`, `ClassNotFoundException`, `UnsatisfiedLinkError`, `dlopen failed`, no Force finishing). Positive app-owned evidence: `MMKV root dir /data/user/0/com.all.launcher.app/files/mmkv` (own store `ALLLauncher`), `HostRPC::connect sucess`, `gles-version: 3.1`, `ProfileInstaller: Installing profile`.
- UI renders the real first-run ALL Launcher screen (`ALL Launcher`, authlib-injector, caciocavallo/caciocavallo 17, Internal-17/21/25/8 Java environments, JNA, `Launcher Components`, `ติดตั้ง`); uiautomator dump + screenshot captured.
- Operator note: the first `am start` used a guessed component (`…ui.activity.MainActivity`) → "does not exist"; no process started, then the correct component was resolved with `cmd package resolve-activity --brief`. Classified as operator error, not a product divergence.

### Release data path
- Release uid 10060 / dataDir `/data/user/0/com.all.launcher.app` vs debug uid 10066 / `/data/user/0/com.all.launcher.app.debug`; external dirs exist separately under `/sdcard/Android/data/…` for both. App-side proof of its own private store: the MMKV path log line. No debug data copied into release; no Minecraft instance created. (Limitation recorded: app-private `files` is unreadable from the adb shell UID, so separation rests on dataDir/UID values plus the app's own log line.)

### Basic UI smoke
- Rendered as above; a non-mutating swipe was delivered and the process survived with 0 new fatal markers; `dumpsys gfxinfo` → Total frames rendered 54, Missed Vsync 2. The visible text set did not change after the swipe, so it is recorded as "input delivered, process survived, frames rendered" — not as a verified scroll. No setup action (component install) pressed, no deep QA.

### Debug preservation
- `com.all.launcher.app.debug` intact: vn `0.0.1-debug`, same dataDir/uid 10066, firstInstallTime `2026-09-16 12:33:37` and lastUpdateTime `2026-09-17 14:53:13` (both pre-run), external files unchanged, not running.

### Safety
- `git diff --check` rc 0; status 27 entries; release APK `920b6a70…`, `build.gradle.kts 39ffc458…`, `proguard-rules.pro 45cb8c21…`, `ModDoctor.kt 728aaa11…` all unchanged ⇒ no source/test/config/ProGuard/signing change this run; no user data migrated or deleted.
- Source changes this run: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\release-device-startup-gate\`
- `00-PASS-release-startup.txt`, `01-apk-gate.txt`, `02-debug-package.txt`, `03-release-preinstall.txt`, `04-debug-datadir.txt`, `05-install.txt`, `06-launch.txt`, `07-logcat-fatal.txt`, `08-logcat-release.txt`, `09-ui-smoke.txt`, `10-first-launch-ui.png`, `12-data-path.txt`, `13-debug-preserved.txt`, `14-render.txt`, `15-safety.txt`

### Exact Next Action
`MICRO-RUN NEXT (device gate, decision required): the release APK runs a first-run "Launcher Components" setup that would install runtime components into the release app's own data path. Choose (a) authorise completing that first-run component setup on the release package so a Minecraft-capable release instance can be created, or (b) keep the release package install/startup-only and continue Mod Doctor work on the debug package. Do not copy debug data into the release package and do not modify source/config.`

## MICRO-RUN — Release playable baseline: ALLQA001 Offline + Minecraft 1.21.11 + Fabric 0.19.5 — PASS

Timestamp: `2026-09-17 20:23 +0700` (device window 20:04:26 → 20:21).

### Result
- **PASS** — inside the release package: offline account created, Minecraft 1.21.11 installed, Fabric Loader 0.19.5 + Fabric API 0.141.6+1.21.11 installed, version registered as the current version. Minecraft was **not** launched. No source/config change; debug package/data untouched.

### Account
- `ALLQA001` created through the real UI (7 taps on the top-left title at (95,30) → `Add Account` → account-type dialog with **Microsoft · Offline · Add Auth** → `Offline` (575,462) → `Create Offline Account` dialog → typed into the Username field → `ตกลง` (1007,579)).
- Evidence: `[20:08:11.066] [AccountManager/INFO] Saved account: ALLQA001` + `Loaded 1 accounts`; the account list shows **ALLQA001** with status **Offline**. No Microsoft/OAuth flow was invoked (`Loaded 0 auth servers`).

### Minecraft
- Downloads → `เกม` → selected the `1.21.11` entry → pressed `ดาวน์โหลด` (install) → install dialog ran: clear cache ✓ → Downloading Vanilla 1.21.11 → libraries → Fabric → Fabric API → install game files. Completion: `[20:18:26.935] [DownloadGame] GameInstaller.onInstalled` → version list refreshed.
- Assets downloaded from `resources.download.minecraft.net`, libraries from `libraries.minecraft.net`; many transient `[FileDownloader/WARNING] Failed: …` lines occurred during the asset phase (retried) while the game dir grew 340M → 472M → 536M — recorded as transient, not a blocker.

### Fabric
- Fabric row version list → selected **0.19.5** (`รุ่นเสถียร`) → row shows `Selected: 0.19.5`; Fabric API auto-selected `Selected: 0.141.6+1.21.11`; other loaders show `ไม่รองรับกับ Fabric, Fabric API`.
- Resulting identity: version field `1.21.11 Fabric 0.19.5`; version JSON `id = 1.21.11 Fabric 0.19.5`, 115 libraries incl. `net.fabricmc:fabric-loader:0.19.5`, `net.fabricmc:intermediary:1.21.11`, `mainClass = net.fabricmc.loader.impl.launch.knot.KnotClient`.

### Runtime / JRE
- No JRE/runtime extraction happened in this run: no `*java*`/`runtime` dirs under the release data dir or its `.minecraft`, and no jre/runtime log lines. Recorded exactly as: **"runtime payload present but extraction deferred until launch"**.

### Release instance
- Game path `/sdcard/Android/data/com.all.launcher.app/files/.minecraft` (owner u0_a60 = release uid 10060), 536M.
- `versions/1.21.11 Fabric 0.19.5/` = `1.21.11 Fabric 0.19.5.jar` (31,152,600 B) · `.json` (60,920 B) · `mods/fabric-api-0.141.6+1.21.11.jar` (2,426,039 B, installed by the production Fabric flow — not a QA fixture) · `ALLLauncher/version.config` (320 B); assets 441M, libraries 63M.
- Current version saved to the release config: `Current version 1.21.11 Fabric 0.19.5`. Account ALLQA001 (Offline). No debug data copied; no unexpected QA mod.

### R8 runtime
- FATAL EXCEPTION / `VerifyError` / `NoSuchMethodError` / `ClassNotFoundException` / `UnsatisfiedLinkError` / `dlopen failed`: **0**. No installer/download crash; the release process ran continuously through account creation, downloads, Fabric install and version registration.

### Debug preservation
- `com.all.launcher.app.debug` unchanged: vn `0.0.1-debug`, same dataDir, firstInstallTime 2026-09-16 12:33:37, lastUpdateTime 2026-09-17 14:53:13; external files still {control_layouts, logs, moddoctor-v1-backup} with pre-run mtimes; its own `.minecraft` (573M) untouched; not running; nothing copied or reused.

### Safety
- `git diff --check` rc 0; status 27 entries; release APK `920b6a70…`, `build.gradle.kts 39ffc458…`, `proguard-rules.pro 45cb8c21…`, `ModDoctor.kt 728aaa11…` all unchanged ⇒ no source/test/APK/config change; Minecraft **not** launched (the `เริ่มเกม` control was never pressed).
- Method note: `uiautomator dump` intermittently failed with "could not get idle state" on Compose screens; those steps were verified from screenshots (loaded into image analysis) plus hierarchy dumps taken when the UI was idle.
- Source changes this run: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\release-playable-baseline\`
- `00-PASS-playable-baseline.txt`, `01-baseline.txt`, `06-account-state.png`, `10-offline-dialog.png`, `12-account-created.txt`, `17-version-dialog.png`, `21-fabric-selected.txt`, `24-dropdown.png`, `25-instance-state.txt`, `26-runtime-state.txt`, `28-version-identity.txt`, `version.json`, `29-fatal.txt`, `30-debug-preservation.txt`, `31-safety.txt`, `33-final-home.png`

### Exact Next Action
`MICRO-RUN NEXT (device gate): launch Minecraft once from the release package — press เริ่มเกม in `com.all.launcher.app` with ALLQA001 + version 1.21.11 Fabric 0.19.5 selected, capture the JRE extraction/runtime preparation, then verify the release instance reaches the Minecraft main menu (Fabric loader 0.19.5, 1.21.11, offline account ALLQA001), and confirm the debug package/data stay untouched. Do not change source/config and do not add mods yet.`

## MICRO-RUN — Release first Minecraft launch: Main Menu reached — PASS

Timestamp: `2026-09-17 20:40 +0700` (device window 20:32:29 → 20:38).

### Result
- **PASS** — one launch press of `เริ่มเกม` in the release package went: clean Mod Doctor → runtime/JVM start → `:game` process → Minecraft 1.21.11 + Fabric Loader 0.19.5 → **real Main Menu** (`Sound engine started`). No source/config change; debug package/data untouched.

### Baseline
- Release APK `920b6a70cab666c727775698ad451850d11c4fded54e57e416f722488c2c7f4a`; account **ALLQA001 (Offline, current)**; version `1.21.11 Fabric 0.19.5`; mods = only `fabric-api-0.141.6+1.21.11.jar` (SHA-1 `c98467cb…`, SHA-256 `bdff7fd7…`); release runtime/JRE dirs and `.minecraft/logs` absent; debug package unchanged and not running.

### Mod Doctor
- **CLEAN / NO-OP** — no Mod Problems dialog (focus went straight to VMActivity; no dialog window in `dumpsys window`), 0 launcher-log and 0 logcat matches for any detector/report/Fix markers, no Launch Anyway decision. `START …VMActivity` at 20:33:07.941 → `Displayed …VMActivity: +1s223ms`.

### Runtime / JRE
- Launcher: LWJGL requirement detected → `using component=3.3.3`; `Using DNS servers for game: […]`; OpenAL config read from the release package's own `.alsoftrc`.
- **No on-disk JRE/runtime extraction occurred** during the launch either (no runtime/`*jre*` dirs, no jre/runtime log lines) — the bundled payload is used. `version.config` has `"javaRuntime": ""` (default/global runtime).
- Runtime actually used, read from Fabric's own environment list in Minecraft's log: `- fabricloader 0.19.5`, `\-- mixinextras 0.5.5`, **`- java 21`**, `- minecraft 1.21.11` ⇒ **Java 21** (observed, not assumed).

### Minecraft
- Processes: `com.all.launcher.app` (5708) + `:filemanager` (5735) + **`:game` (7542)**; `LwjglLinkerHook: Installing LWJGL dlopen() hook`.
- `[20:33:22] Loading Minecraft 1.21.11 with Fabric Loader 0.19.5`; `- fabric-api 0.141.6+1.21.11` (+modules); Mixin 0.8.7 / MixinExtras 0.5.5; `[20:33:37] Setting user: ALLQA001`; `[20:33:38] Backend library: LWJGL version 3.3.3-snapshot`; `[20:33:41] Reloading ResourceManager: vanilla, fabric-api, …`.

### Main Menu
- `[20:33:46] OpenAL initialized on device OpenSL` + **`Sound engine started`** (1 occurrence); screenshot shows the real Minecraft Java Edition main menu (Singleplayer/Multiplayer/Realms/Options/Quit, Thai localization, mobile control overlay) with the bottom-left version line `Minecraft 1.21.11/ALLLauncher0.0.1/Fabric (…)` and no crash screen.

### Runtime / R8
- `FATAL EXCEPTION` (logcat window): **0**; no `VerifyError`, `NoSuchMethodError`, `ClassNotFoundException`, JVM fatal exit, Fabric loader fatal error, Mod Doctor false positive, or R8 reflection/serialization failure.
- Known harmless noise recorded explicitly: JNA native load failure (`dlopen failed: library "libc.so.6" not found`) → `NoClassDefFoundError: com.sun.jna.NativeLong` → OSHI hardware probe warn + `Error while loading the narrator`; plus expected offline-account noise (`Failed to fetch user properties … 401`, Realms `SignedJWT` errors). None blocks the launch.

### Mods integrity
- Unchanged after launch: only `fabric-api-0.141.6+1.21.11.jar`, same size/mtime, SHA-1 `c98467cb…`, SHA-256 `bdff7fd7…`; no `.disabled`/temp/QA file. Game dir intact (569M, all subdirs present). Release force-stopped only after Main Menu evidence; instance not deleted.

### Debug preservation
- `com.all.launcher.app.debug` unchanged: vn `0.0.1-debug`, same dataDir, firstInstallTime 2026-09-16 12:33:37, lastUpdateTime 2026-09-17 14:53:13; external files {control_layouts, logs, moddoctor-v1-backup} with pre-run mtimes; never started; nothing copied.

### Safety
- `git diff --check` rc 0; status 27 entries; release APK, `build.gradle.kts 39ffc458…`, `proguard-rules.pro 45cb8c21…`, `gradle.properties 1668c8ec…`, `ModDoctor.kt 728aaa11…` all unchanged; one launch attempt only, no mods added/removed, no Fix invoked, no data copied from debug.
- Source changes this run: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\release-first-minecraft-launch\`
- `00-PASS-main-menu.txt`, `01-baseline.txt`, `06-launch-progress.txt`, `minecraft-latest.log`, `11-minecraft-markers.txt`, `12-runtime-evidence.txt`, `13-main-menu.png`, `14-identity-block.txt`, `16-mods-after.txt`, `17-mod-doctor-gate.txt`, `18-debug-preservation.txt`, `19-safety.txt`

### Exact Next Action
`MICRO-RUN NEXT (device gate): verify the release package's Mod Doctor behaviour on the real device (first time in the R8-minified release build) — with the release instance now at Main Menu, run the same controlled detection matrix that was proven on the debug package (missing required dependency, wrong Minecraft version via broad declaration, duplicate, corrupt metadata, offline/provider-failure) and confirm the minified build reports identically; add mods only as temporary verified fixtures and restore them exactly afterwards. Do not change source/config.`

## MICRO-RUN — Release first-run Launcher Components installation (real UI) — PASS

Timestamp: `2026-09-17 19:40 +0700` (device window 19:36:16 → 19:38).

### Result
- **PASS** — the release package's own first-run "Launcher Components" gate was cleared through the real UI, the production unpack flow completed, the app reached its normal post-component home state, no R8/runtime crash occurred, and the debug package/data stayed untouched. No source/config change.

### Release APK
- unchanged `920b6a70cab666c727775698ad451850d11c4fded54e57e416f722488c2c7f4a` (not rebuilt, not reinstalled).

### First-run component installation
- Baseline: first-run dialog with a single action `ติดตั้ง`; tap derived from the hierarchy (`scripts/findtap.py`) at **(1347, 840)** — the only clickable node.
- Production flow ran `UnpackSingleTask` from APK assets (no manual injection): **auth_libs 3 · caciocavallo 4 · caciocavallo17 4 · launcher 5 · jna 26 · lwjgl/3.3.3 21 · lwjgl/3.4.1 23** files unpacked, no failure lines.
- Afterwards the app left the gate for `MainActivity` and showed the normal production update-notice dialog (`LauncherUpgradeVM: Launcher update detected: 1 -> 200040`), dismissed via its own `ไม่สน` action at (1014, 817).

### Installed component / runtime state
- Unpacked by the production installer: auth_libs, caciocavallo, caciocavallo17, launcher, jna, lwjgl 3.3.3, lwjgl 3.4.1.
- Java runtimes ship inside the APK — the app logged `UnpackJreTask: Device requires environment: runtimes/jre-8|17|21|25/bin-x86_64.tar.xz, contains = true` and `SplashActivity: The launcher contains the runtime environment: runtimes/jre-{8,17,21,25}/…` (+ jna packs). **Honest scope: extraction of the jre-* packs was not observed in the captured windows** — recorded as "present, extraction not observed", not as installed by this action.
- App-private component paths could not be listed (shell cannot read `/data/user/0/com.all.launcher.app`); verification rests on the app's own logs plus the production state machine advancing. Renderers enumerated after install: Krypton Wrapper, GL4ES, Kopper Zink, VirGLRenderer, Freedreno, Panfrost.

### R8 / runtime safety
- Fatal markers (FATAL EXCEPTION, `VerifyError`, `NoSuchMethodError`, `ClassNotFoundException`, `UnsatisfiedLinkError`, `dlopen failed`): **0**. Release process alive throughout (pid 5367 → 5708 after restart).
- No infinite first-run loop: `am force-stop` + relaunch → `am start -W` Status ok (854 ms) and the UI went straight to the home screen; the components gate did **not** reappear.
- Normal post-component state: `ALLLauncher`, `Add Account`, `No installed versions`, `เริ่มเกม` (fresh package: 0 accounts, 0 auth servers, 1 game path).

### Release data-path result
- Release writes its own external data `/sdcard/Android/data/com.all.launcher.app/files/{control_layouts,logs}`, owned by **u0_a60** (release uid 10060), including its own app logs (`log_2026-09-17T19-31-29_main.log`, `…T19-36-45_main.log`, header `Launcher Version: 0.0.1, build: x86_64`).

### Debug preservation
- `com.all.launcher.app.debug` untouched: external files still exactly {control_layouts 2026-09-16 13:25, logs 2026-09-17 16:09, moddoctor-v1-backup 2026-09-16 16:56} owned by u0_a66; metadata unchanged (vn `0.0.1-debug`, dataDir, firstInstallTime 2026-09-16 12:33:37, lastUpdateTime 2026-09-17 14:53:13); not running. No debug file copied or reused.

### Safety
- `git diff --check` rc 0; status 27 entries; release APK + `ModDoctor.kt 728aaa11…` + `build.gradle.kts 39ffc458…` unchanged; no source/test/config/ProGuard/signing change; nothing uninstalled/reinstalled; stopped before game setup (no account, no Minecraft download, no Fabric instance, no Minecraft launch, no Mod Doctor).
- Source changes this run: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\release-first-run-components\`
- `00-PASS-first-run-components.txt`, `01-baseline.txt`, `02-tap-derivation.txt`, `05-installer-log.txt`, `07-post-install-log.txt`, `08-component-state-log.txt`, `10-data-isolation.txt`, `11-safety.txt`, `12-after-install-ui.png`, `13-jre-state.txt`, `release_own_log_19-31-29.log`, `release_own_log_19-36-45.log`, UI dumps `rel_pre.xml`/`rel_t1.xml`/`rel_t2.xml`/`rel_t3.xml`

### Exact Next Action
`MICRO-RUN NEXT (device gate): create the first release-package Minecraft target — in the release UI (`com.all.launcher.app`) create an offline account and install a Minecraft version + Fabric instance through the normal flow, then verify the release instance reaches the Minecraft main menu; keep the debug package and its data untouched and do not change source/config.`

## MICRO-RUN — Release first Minecraft launch (task re-delivered) — ALREADY SATISFIED, independently re-verified — PASS

Timestamp: `2026-09-17 21:50 +0700` (verification window ~21:45 → 21:48).

### Result
- **PASS — no new launch performed.** The milestone delivered at 21:44 ("launch Minecraft once from the real Release package and prove the Release runtime path reaches Minecraft Main Menu", evidence under `test-artifacts\release-first-minecraft-launch\`) had **already been completed and recorded as PASS at 20:40** by the preceding run, using exactly that evidence directory. The task text's premise — *"Minecraft has NEVER been launched in Release yet"* — was already stale on arrival. Rather than duplicate a proven run, every success condition was independently re-verified (artifacts + live device). Nothing was launched, installed, force-stopped or modified.

### A — recorded evidence maps 1:1 to the device (direct, not reconstructed)
- Archived `minecraft-latest.log`: **19,058 bytes** · on-device `…/versions/1.21.11 Fabric 0.19.5/logs/latest.log`: **19,058 bytes**, mtime `2026-09-17 20:33:47` — identical size and identical marker line numbers in both copies:
  - line 1 `[20:33:22] Loading Minecraft 1.21.11 with Fabric Loader 0.19.5` · line 50 `- java 21` · line 135 `[20:33:37] Setting user: ALLQA001` · line 214 `[20:33:46] Sound engine started`
- ⇒ the archived log is a copy of the real on-device game log; the Main Menu proof is genuine, not synthesized.

### B — live device state still satisfies every success condition
- Release package `com.all.launcher.app` · vc 1 · vn 0.0.1 · dataDir `/data/user/0/com.all.launcher.app` · firstInstallTime `2026-09-17 19:30:54` (unchanged).
- Release APK on disk: `193,000,893 B`, mtime `19:13`, SHA-256 **`920b6a70cab666c727775698ad451850d11c4fded54e57e416f722488c2c7f4a`** (exact).
- Instance intact: `versions/1.21.11 Fabric 0.19.5/` jar 31,152,600 B + json 60,920 B (20:18), `mods/` (20:18), `logs/` (20:33), `config/ data/ downloads/ ALLLauncher/ log4j2.xml` present.
- Mods integrity: **only** `fabric-api-0.141.6+1.21.11.jar` (2,426,039 B · SHA-1 `c98467cb…` · SHA-256 `bdff7fd7…`) — identical to the recorded baseline; no `.disabled`/temp/QA file.
- No `com.all.launcher.app*` process running (release force-stopped after the recorded capture).
- Debug preservation: `com.all.launcher.app.debug` vn `0.0.1-debug`, firstInstallTime `2026-09-16 12:33:37`, lastUpdateTime `2026-09-17 14:53:13` (both pre-run); external files `{control_layouts 2026-09-16 13:25, logs 2026-09-17 16:09, moddoctor-v1-backup 2026-09-16 16:56}` mtimes unchanged; not running.
- Mod Doctor CLEAN/NO-OP and 0 FATAL EXCEPTION per the recorded run; only known harmless noise (JNA `dlopen` `libc.so.6` → `NoClassDefFoundError` NativeLong → narrator/OSHI warnings, offline-account 401, Realms `SignedJWT`).

### C — safety (this verification run)
- Read-only device access only: no launch, install, force-stop, mod change, data copy or debug interaction.
- `git diff --check` rc 0; `git status --porcelain` 27 entries (same baseline); release APK `920b6a70…` unchanged; no source/test/config/ProGuard/signing change.
- Source changes this run: **none**.

### Evidence (added to the milestone's own directory)
- `D:\ALL Launcher 0.0.1\test-artifacts\release-first-minecraft-launch\21-verification-2026-09-17T2148.txt`

### Exact Next Action
`MICRO-RUN NEXT (device gate, decision required): the Release-package Mod Doctor matrix has never been exercised in the R8-minified build on this device — run the controlled detection matrix proven on the debug package against com.all.launcher.app (missing required dependency, wrong Minecraft version via broad declaration, duplicate mod, corrupt metadata, offline/provider-failure), adding only temporary verified fixtures and restoring the release mods directory exactly (SHA-1 + SHA-256 set identical). Alternative if a fresh launch window is wanted first: repeat ONE launch with a cleared logcat window (a second launch, not a first). Do not change source/config and do not touch the debug package/data.`

## MICRO-RUN — Release (R8-minified) provider-backed broad/open WRONG_MINECRAFT_VERSION — FAIL at first divergence

Timestamp: `2026-09-17 21:58 +0700` (device window 21:53 → 21:56).

### Result
- **FAIL** at the first production-flow divergence: with the Balm fixture present, the R8-minified release build produced **no Mod Doctor finding and did not gate the launch** — VMActivity started and the game crashed on Balm's own mixins (proving the fixture was genuinely incompatible). Stopped immediately; nothing fixed; Fix/Launch Anyway never pressed.

### Release baseline (STEP 1-2): PASS
- `com.all.launcher.app` vc 1 / vn 0.0.1, dataDir `/data/user/0/com.all.launcher.app`, firstInstallTime `19:30:54`; APK SHA-256 `920b6a70…` (exact).
- Account **ALLQA001 (Offline)** · version **1.21.11 Fabric 0.19.5** · provider reachable from guest (`ping api.modrinth.com` 2/2, 0% loss).
- Release mods baseline: only `fabric-api-0.141.6+1.21.11.jar` (2,426,039 B · SHA-1 `c98467cb…` · SHA-256 `bdff7fd7…`).

### Fixture (STEP 3)
- `balm-fabric-1.21.10-21.10.11.jar` · 699,812 B · SHA-1 `6a7a8d104453aeceae8eb0a76944f64b32810665` · SHA-256 `89e1d7bc…` · `fabric.mod.json` → id `balm`, name `Balm`, version `21.10.11`, `depends.minecraft ">=1.21.10"`; pushed alone, device hashes identical, fabric-api untouched.

### First divergence (STEP 4)
- `21:54:38` Start Game pressed once (tap 1351,834) → `21:54:39.587` Mod Doctor ran → `21:54:40.473` `[InstalledModMatcher/WARNING] Failed to match installed mods on platform: CURSEFORGE` (+ CurseForge POST 401) → **`21:54:43.285` `START …VMActivity` from uid 10060** (Minecraft NOT blocked) → `21:54:44.296` `Displayed …VMActivity: +1s2ms`; window list at that moment contains only VMActivity + MainActivity ⇒ **no Mod Problems dialog ever appeared**.
- Game log: `[21:54:47] Loading Minecraft 1.21.11 with Fabric Loader 0.19.5`, line 3 `- balm 21.10.11` → `[21:54:58] Mixin apply for mod balm failed … InvalidInjectionException (@Inject on drop)` → `[21:54:59] …AbstractContainerScreenAccessor → InvalidAccessorException → FormattedException: Mixin transformation of net.minecraft.class_465 failed` → `21:54:59.145` `START …ErrorActivity` → `21:54:59.258` VMActivity WIN DEATH.
- ⇒ fixture genuinely incompatible, yet the release build neither reported nor blocked it.

### Provider/fingerprint evidence (STEP 5)
- Authoritative production logs: the app's own log shows only the `InstalledModMatcher/WARNING … CURSEFORGE` failure; logcat shows the matching `PlatformSearcher … {Official CurseForge}` 401.
- **Zero Modrinth evidence**: 0 matches for `modrinth` in the saved logcat window and none in the app log — where the debug-build run of this same case produced `[PlatformSearch/DEBUG] … {Official Modrinth}`. No provider `game_versions` evidence was produced for Balm in the release build.

### R8 / runtime markers (STEP 5)
- FATAL EXCEPTION 0 · VerifyError 0 · NoSuchMethodError 0 · ClassNotFoundException 0 · UnsatisfiedLinkError 0 · dlopen failed 0 (logcat window). No R8/reflection/serialization crash. The only crash is the game-side Balm mixin failure. Unrelated noise recorded: `[21:54:43.253] [BatchDownloader/WARNING] Completed-count mismatch: requests=0 finished=4651 failures=all` (local game-file check) and the known JNA `dlopen libc.so.6` warnings.

### Dialog / Cancel (STEP 6)
- **Not applicable** — no dialog appeared, so nothing was pressed; Fix and Launch Anyway were never pressed.

### Restore (STEP 7): PASS
- Fixture removed; mods dir = `fabric-api-0.141.6+1.21.11.jar` only; SHA-1 set `{c98467cb…}` and SHA-256 set `{bdff7fd7…}` identical to baseline; no `.disabled`/QA/temp file; release force-stopped (0 processes); release instance/data left in place.

### Debug preservation (STEP 8): PASS
- `com.all.launcher.app.debug` vn `0.0.1-debug`, dataDir unchanged, firstInstallTime `2026-09-16 12:33:37` / lastUpdateTime `2026-09-17 14:53:13`, external files `{control_layouts, logs, moddoctor-v1-backup}` mtimes unchanged, never started.

### Safety (STEP 9)
- `git diff --check` rc 0; status 27 entries; release APK `920b6a70…`, `proguard-rules.pro 45cb8c21…`, `ModDoctor.kt 728aaa11…`, `ModDoctorTest.kt 32504cfb…` unchanged; no rebuild, no reinstall, no source/test/config change.
- Source changes this run: **none**.

### Hypotheses (unproven — no further experiments run, per stop-at-first-divergence)
1. Provider enrichment returned no authoritative Modrinth match for the installed Balm fingerprint in the release build → Balm stayed UNKNOWN → no finding (fits the total absence of Modrinth evidence). 2. Enrichment aborted after the CurseForge 401 instead of continuing to the next provider. 3. R8-related behaviour in fingerprint/match handling. Contrast: the debug build reported this identical fixture correctly at 14:48 with the same product rule (`ModDoctor.kt 728aaa11…`) that is present in the release APK.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\release-moddoctor-wrong-version-e2e\`
- `00-DIVERGENCE-SUMMARY.txt`, `01-baseline-mods.txt`, `02-fixture-added.txt`, `03-restore-verification.txt`, `04-debug-preservation.txt`, `05-safety.txt`, `10-rel-home.xml`, `12-logcat-window.txt`, `13-game-latest.log`, `14-app-launch-log.txt`, `15-app-start-log.txt`, `16-app-log-mod-doctor-run.txt`

### Exact Next Action
`MICRO-RUN NEXT (diagnosis only, no device QA until a hypothesis is chosen): establish why the R8-minified release build produced no authoritative provider match for the Balm fingerprint while the debug build did — read the release app log around 21:54:40 at full verbosity (the seam's own DEBUG lines are missing in the release window), and compare against the debug-build log of the 14:48 case; inspect the release-dex packaging of the fingerprint/match path. Do NOT edit source/rules and do not re-run device QA until the divergence cause is identified.`

## MICRO-RUN — Read-only diagnosis: why R8 Release produced no Modrinth fingerprint match — CONFIRMED ROOT CAUSE

Timestamp: `2026-09-17 22:07 +0700` (diagnosis window 21:59 → 22:05). No implementation performed.

### Result
- **CONFIRMED ROOT CAUSE (for the observed divergence)** — the Release run queried **CurseForge**, not Modrinth, because the Mod Doctor provider is taken from the user setting `searchModPlatform`, whose default is `Platform.CURSEFORGE`; CurseForge's fingerprint endpoint answered **HTTP 401** (no API key possible), the matcher turns that into a failed batch, so no authoritative `gameVersions` existed → no finding → launch not gated. **R8 is exonerated for this divergence.**

### Source trace (STEP 1)
- `ModDoctor.kt:145-153` → `providerGameVersionsForAmbiguousMods` (seam) with
  `lookup = { fileName -> lookupInstalledMod(modsDir, fileName, AllSettings.searchModPlatform.getValue()) }` ← **platform comes from a user setting**.
- `ModDoctor.kt:917-945` seam → `lookupInstalledMod` (`:740-755`) → `matchInstalledMods(fingerprints, platform)` (`InstalledModMatcher.kt:94-179`) → `when (platform) { MODRINTH -> getModrinthVersBySha1 ; CURSEFORGE -> getCFFilesByFingerprints }` (`:147-151`); per-batch failure → `Logger.warning("Failed to match installed mods on platform: $platform")` + `complete = false` (`:152-161`).
- `AllSettings.kt:598`: `val searchModPlatform = enumSetting("searchModPlatform", Platform.CURSEFORGE)` — **default CURSEFORGE**; the same setting also feeds the fix path (`ModDoctor.kt:498, :502`).
- ⇒ **no cross-platform enumeration exists in this path**: exactly one platform is queried per Mod Doctor run.

### Last common point / first divergence (STEP 3)
- Last common point: Mod Doctor ran, classified local metadata, and the provider seam executed the matcher in both builds (release log proves it: `InstalledModMatcher/WARNING … CURSEFORGE`).
- First divergence: the **platform** — Release = CURSEFORGE (proven by that warning interpolating the actual argument) with zero Modrinth evidence; Debug (14:47 run of the identical case) = MODRINTH (`[PlatformSearch/DEBUG] … {Official Modrinth}`).
- Deterministic consequence: 401 → failed batch → `complete=false`, no InstalledMod, no cache write → seam returns `emptyMap()` → `providerIncompatible` false → no problem, no dialog → `START …VMActivity` `21:54:43.285` → Balm mixin crash `21:54:58/59`.

### R8 inspection (STEP 4-5, existing artifacts only; no keep rules added)
- **Modrinth implementation present**: mapping `…modrinth.ModrinthSearcher -> gu6`, `modrinthSearcher -> a`, `mirrorModrinthSearcher -> b`, `ModrinthSearcher.getVersionFiles -> p`, `_PlatformSearchKt$getModrinthVersBySha1$2` retained; dex literals `api.modrinth.com` ×1, `/version_files` ×1, `api.curseforge.com` ×3, `Failed to match installed mods on platform` ×1.
- Provider enum live: `Platform -> lx7`, `Platform$Companion -> jx7`; the runtime warning printed `CURSEFORGE` from `platform.name`.
- No `R8$$REMOVED` entry for Modrinth / InstalledModMatcher / ModFingerprints / Platform. (`usage.txt` is **not** usable as a removal list here — it also lists class-level entries for code proven live at runtime; control: `platform.PlatformClasses`.)
- Release build already completed a real Modrinth API+CDN exchange today: its own log `[20:18:23.890] [FileDownloader/INFO] Downloading: https://cdn.modrinth.com/data/P7dR8mSH/versions/6qAuTtLR/fabric-api-0.141.6%2B1.21.11.jar`.

### Provider dispatch / cache / logging (STEP 6-8)
- Dispatch: single-platform by design; a CurseForge failure cannot suppress a Modrinth call that was never requested, but it does end enrichment for that run (`complete=false`, caught, not rethrown except `CancellationException`).
- Cache: **ruled out** — keys are per platform (`${platform.name}/v2/…`), failed batches write nothing, negative cache only after a successful response, and the Balm file was new at 21:53.
- Logging: **not a logging-only difference** — downstream behaviour proves no provider data reached the validator (no dialog, Minecraft started); the seam's failure line *is* logged in the R8 build, so a Modrinth attempt would have been visible.

### Root cause (STEP 9)
- CLASSIFICATION: **CONFIRMED ROOT CAUSE** · Evidence FOR: the source lines above, the release `CURSEFORGE` warning, the CurseForge 401, zero Modrinth evidence, the Debug Modrinth-source log, and (read-only) Debug MMKV `searchModPlatform → MODRINTH`. Evidence AGAINST: none found for the divergence mechanism. Confidence: **HIGH**. Residual unproven: whether the Modrinth `version_files` (fingerprint) call itself succeeds on the R8 Release build — never exercised there.

### Smallest proposed next change (STEP 10, NOT implemented)
- Option A (zero code, confirms the diagnosis + exercises the Modrinth fingerprint path on R8): set the Release UI mod-search platform to Modrinth and re-run the same Balm fixture QA — expect the WRONG_MINECRAFT_VERSION dialog.
- Option B (smallest code fix if default settings must work): at `ModDoctor.kt:149` (and `:498/:502`) stop deriving the authoritative provider from the *search* setting — use Modrinth for the fingerprint lookup, or fall back to Modrinth when the configured platform yields no match. Not a broad `-keep class ** { *; }`. Regression plan: one focused unit test (seam looks up via Modrinth / falls back), then one Debug device re-run of the same fixture, then one Release device re-run.

### Safety (STEP 11)
- `git diff --check` rc 0; status 27 entries; release APK `920b6a70…`, `build.gradle.kts 39ffc458…`, `proguard-rules.pro 45cb8c21…`, `ModDoctor.kt 728aaa11…`, `InstalledModMatcher.kt be7d8296…`, `AllSettings.kt fb4d9034…` all unchanged.
- No source/test/ProGuard/APK change; no rebuild/reinstall; no launch; no device write (read-only `dumpsys`/`ls`/`run-as` reads only); release mods dir still baseline (fabric-api only; 0 release processes); no credentials accessed or printed.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\release-moddoctor-wrong-version-diagnosis\`
- `00-DIAGNOSIS.txt`, `05-safety.txt`, `06-device-untouched.txt`

### Exact Next Action
`MICRO-RUN NEXT (device gate, zero-code): in the Release UI (`com.all.launcher.app`) switch the mod-search platform to Modrinth, re-add the Balm fixture balm-fabric-1.21.10-21.10.11.jar, press Start Game once and require the WRONG_MINECRAFT_VERSION dialog with Minecraft blocked, then Cancel only and restore the Release mods directory byte-identically; this both confirms the diagnosis end-to-end and exercises the Modrinth fingerprint path on the R8 build. Do not change source/config; do not touch the debug package/data.`

## MICRO-RUN — Mod Doctor authoritative installed-mod fingerprint lookup decoupled from the user Search Platform — PASS

Timestamp: `2026-09-17 23:15 +0700` (run window 22:28 → 23:15). Source + test change only; **no APK build, no device/ADB work**.

### Product rule implemented
`Mod Doctor authoritative installed-mod fingerprint lookup == Platform.MODRINTH`, independent of the user's mod-search/download platform preference.
User preference (`AllSettings.searchModPlatform`, default `CURSEFORGE`) now affects only the user-facing mod browsing/search/download UI.
**[CORRECTED 2026-09-18: this overstates the scope. The preference also still governs the missing-dependency
project lookup at `ModDoctor.kt:498` — which this section's own call-site case C records.]**

### Call-site classification (traced before editing)
- **A — installed-mod fingerprint health-check → CHANGED**: `ModDoctor.kt:149` `lookupInstalledMod(modsDir, fileName, AllSettings.searchModPlatform.getValue())` → `… modDoctorFingerprintPlatform(AllSettings.searchModPlatform.getValue())`.
- **B — authoritative installed-file identity inside the repair path → CHANGED**: `ModDoctor.kt:502` `installedMod = { fileName -> lookupInstalledMod(modsDir, fileName, platform) }` → `… modDoctorFingerprintPlatform(platform)`. Internally consistent because `ModDoctor.kt:596 val platform = installed.platform` (replacement versions resolve on the platform that produced the match).
- **C — dependency resolution in the repair path → NOT CHANGED**: `ModDoctor.kt:498` parameter `platform` (used only by `lookupProjectDirect` / `searchProjectBySlug` / `loadProjectVersions` for `MISSING_REQUIRED_DEPENDENCY`). Out of scope; a CURSEFORGE user still resolves that repair project through CurseForge (deliberate, unchanged behaviour).
- **D — user-facing search/download → NOT CHANGED**: `SearchModScreen.kt:57`, `DownloadModViewModel`, `setting/AllSettings.kt:598` (default stays `CURSEFORGE`). No `ui/` or `download/` file touched.

### Implementation (smallest change; one file)
- Added one internal seam: `internal fun modDoctorFingerprintPlatform(userSearchPlatform: Platform): Platform = Platform.MODRINTH` (parameter = the input whose influence is removed; documented in KDoc).
- Wired `:149` and `:502` through it. No fallback chain, no multi-provider voting, no fuzzy identity, no new abstraction.

### RED → GREEN
- RED (seam added first with identity body = old behaviour): `Tests run: 30, Failures: 1` — `modDoctorFingerprintLookupIgnoresUserSearchPlatform`: `expected:<MODRINTH> but was:<CURSEFORGE>` (`ModDoctorTest.kt:473`).
- GREEN (after the production fix, same compiled classes): `ModDoctorTest` + `ModDoctorReplacementRepairTest` + `ModFileDisableSemanticsTest` + `ModrinthVersionPagingTest` → **`OK (43 tests)`, 0 failures, 0 errors** (ModDoctorTest 30; other three 13). No `InstalledModMatcher` test exists in `src/test`.
- Controls green inside `ModDoctorTest`: platform=MODRINTH unchanged behaviour · no Modrinth match → UNKNOWN · provider timeout/error → UNKNOWN, non-blocking · locally incompatible metadata → still reported · user preference untouched.

### Safety
`git diff --check` rc 0 · status 27 entries ·
`setting/AllSettings.kt fb4d9034…` (`:598` default `Platform.CURSEFORGE`) ·
`build.gradle.kts 39ffc458…` · `proguard-rules.pro 45cb8c21…` (R8 untouched) ·
release APK `920b6a70…` (mtime 2026-09-17 19:13:17) and debug APK `8aaad6ad…` byte-identical ·
no assemble/install/adb/device/credential work.

### Changed source hashes
`ModDoctor.kt 210cf50d472c78671dcb3c6143cc091e684f3befddefa92d4534b5dfc305a33d` (was `728aaa11…`) ·
`ModDoctorTest.kt b39d6f00f7cdc7dec46cebecc47a5958b47f469c9d4cbe1b258a986f71f9eeb3`.

### Tooling divergence (documented, not a product issue)
`:ZalithLauncher:testDebugUnitTest --tests …` reports `No tests found for given includes` for **every** test class on this machine (untouched classes too), under JDK 17 **and** JDK 21, although the class files exist under `testClassesDirs …/transformDebugUnitTestClassesWithAsm/dirs` and `junit-4.13.2.jar` is on the 420-entry test runtime classpath; `--debug` shows the JUnit4 framework detector failing to resolve JDK parent classes (`java/lang/Object`) and then an empty test run. RED/GREEN were produced by running the same compiled classes + the same Gradle-resolved classpath through the real JUnit4 runner (`org.junit.runner.JUnitCore`).

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\release-moddoctor-fingerprint-decouple\`
- `00-PASS-summary.txt`, `01-RED.txt`, `02-GREEN-4suites.txt`, `03-GREEN-other-suites.txt`, `04-safety.txt`, `05-callsite-classification.txt`

### Exact Next Action
`MICRO-RUN NEXT (build + device gate): rebuild the Release APK with this source change (standard JDK 21 command, one variable), verify APK identity/R8 provenance, then re-run the release Balm wrong-version E2E with the release user setting LEFT AT CURSEFORGE (no setting change) and require the Mod Problems dialog "Wrong Minecraft version: Balm is published for Minecraft 1.21.10, not 1.21.11." with VMActivity never starting; then Cancel only and restore mods byte-identically.`

## MICRO-RUN — Release (R8): platform CURSEFORGE → MODRINTH restores provider-backed wrong-version detection — PASS

Timestamp: `2026-09-17 22:20 +0700` (device window 22:12 → 22:18). Zero-code QA; settings changed only through the production UI.

### Result
- **PASS** — with the same Release APK and the same Balm fixture, changing only the user's mod-search platform CURSEFORGE → MODRINTH made the R8-minified Release build produce the provider-backed `WRONG_MINECRAFT_VERSION` finding and block Minecraft. The diagnosis is confirmed end-to-end, and the Modrinth fingerprint path is proven to work under R8.

### Original / temporary settings (STEP 1-2, STEP 8)
- Original (read from the real UI, `ดาวน์โหลด → มอด → แพลตฟอร์มเป้าหมาย`): **CurseForge** (the same screen also showed the CurseForge `(403)` search error).
- Temporary: **Modrinth**, set through the same UI control's dropdown; label re-read as Modrinth and the result list switched to Modrinth data — the production state path writes `AllSettings.searchModPlatform` (single write site: `SearchModScreen.kt:57`). No MMKV/file editing.

### Fixture + provider match (STEP 3-5)
- `balm-fabric-1.21.10-21.10.11.jar` · 699,812 B · SHA-1 `6a7a8d104453aeceae8eb0a76944f64b32810665` · SHA-256 `89e1d7bc…` (device identical); fabric-api untouched.
- Direct provider evidence from the R8 build: logcat `[22:14:26.284] [PlatformSearch/DEBUG] Starting to attempt to perform the operation on source: {Official Modrinth}` (same app log earlier shows the pre-change `InstalledModMatcher/WARNING … CURSEFORGE` + `PlatformSearch/ERROR`). The finding text can only come from provider-supplied `game_versions` (`ModDoctor.kt:833-838`) ⇒ exact authoritative match succeeded (Modrinth `MBAkmtvl` / `9HjDG5Uj`, `game_versions ["1.21.10"]`).

### Detection + launch gate (STEP 4)
- One press of `เริ่มเกม` (22:14:24, 1351,834) produced the real dialog:
  - `Mod Problems` · `Minecraft launch paused. Found 1 mod problem(s).`
  - `Wrong Minecraft version: Balm is published for Minecraft 1.21.10, not 1.21.11.` + `File: balm-fabric-1.21.10-21.10.11.jar`
  - actions `ยกเลิก` (800,488) · `Fix` (800,551) · `ใช้ไปก่อนเถอะ เนอะ` (800,614)
- `mCurrentFocus`/`mResumedActivity` stayed on `…MainActivity`; logcat `VMActivity` occurrences = **0**; no `:game` process ⇒ **Minecraft blocked before launch**.

### Cancel (STEP 6)
- Pressed `ยกเลิก` only (22:14:55) → dialog closed, focus/resumed back on `…MainActivity`, home UI intact, release process healthy (pid 8601 + `:filemanager`), 0 fatal-class markers. `Fix` and the Launch-anyway action were **not** pressed.

### Restores (STEP 7-8)
- Mods: fixture removed; dir = only `fabric-api-0.141.6+1.21.11.jar`; SHA-1 set `{c98467cb…}` and SHA-256 set `{bdff7fd7…}` identical to baseline; no `.disabled`/QA/temp file.
- Setting: restored to **CurseForge** via the same UI control and verified persisted (navigated home → back: label re-read `CurseForge`, no Modrinth results).

### Safety (STEP 9)
- `git diff --check` rc 0; status 27 entries; release APK `920b6a70…`, `proguard-rules.pro 45cb8c21…`, `ModDoctor.kt 728aaa11…`, `AllSettings.kt fb4d9034…` unchanged.
  - **[SUPERSEDED 2026-09-18: true of its own moment. `ModDoctor.kt` is now `210cf50d…` and the release APK is `530b2804…`; neither `728aaa11…` nor `920b6a70…` describes current state.]**
- Debug package untouched (vn `0.0.1-debug`, firstInstallTime `2026-09-16 12:33:37`, lastUpdateTime `2026-09-17 14:53:13`, external files unchanged, never started). No source/test/ProGuard/APK change; no rebuild/reinstall.
- Source changes this run: **none**.

### Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\release-moddoctor-modrinth-setting-confirm\`
- `00-PASS-summary.txt`, `01-baseline.txt`, `02-fixture-added.txt`, `03-restore-mods.txt`, `04-safety-and-debug.txt`, `05-final-mods.txt`, `18-dialog.xml` (+ `dialog-evidence.xml`), `20-logcat.txt`, `21-app-log.txt`, `23-logcat-after-cancel.txt`, UI dumps `10-home.xml` … `32-r21.xml`

### Exact Next Action
**[DONE 2026-09-17 23:15 — the decision below was taken in the section above and the fix is in the tree at
`ModDoctor.kt:917`; the release rebuild and the device gate this line asks for were completed 2026-09-18 and
are recorded in the newest section at the end of this file. Superseded, kept as written.]**

`MICRO-RUN NEXT (product decision, no device QA): the confirmed cause is a design gap — Mod Doctor derives its authoritative provider from the mod-*search* setting, whose default is CurseForge, and CurseForge's fingerprint API needs an API key, so provider enrichment is permanently UNKNOWN on a fresh install. Decide between (a) leaving it as a documented user-setting dependency, or (b) authorising the smallest source fix at ModDoctor.kt:149 (`:498/:502`): use Modrinth for the authoritative fingerprint lookup, or try the configured platform and fall back to Modrinth when it yields no match — with one focused regression test plus a debug and a release device re-run of the same Balm fixture. Do not change source until the decision is made.`

## MICRO-RUN — Release rebuild + Mod Doctor fingerprint-provider decoupling device acceptance — PASS

Timestamp: `2026-09-18`. Device: LDPlayer `127.0.0.1:5555` (transport_id 2; `emulator-5554` present and never
used). This run closed the gate the 2026-09-17 23:15 entry (`:2491`) opened and the aborted 23:21:33 attempt
never finished.

### 1. Release rebuild
- Precondition re-measured: the release APK was still `920b6a70…` (193,000,893 B, mtime 2026-09-17 19:13:17)
  while `ModDoctor.kt` was `210cf50d…` (mtime 23:09:09) and `mapping.txt` had not moved since 18:40 — so no APK
  on disk contained the decoupling. The 23:21:33 build ran R8 (mapping outputs 23:26:38-52) and died before
  packaging; it produced no APK.
- Command: `gradlew.bat :ZalithLauncher:assembleRelease -Darch=x86_64 --no-daemon --console=plain`, run from the
  **junction** `D:\ALL001` with `JAVA_HOME=C:\jdk21` (Temurin 21.0.12.1+1 LTS).
- Result: `BUILD SUCCESSFUL in 6m 42s` · `203 actionable tasks: 10 executed, 193 up-to-date`, with
  `configureNdkBuildRelease[x86_64]`, `buildNdkBuildRelease[x86_64]`, **`minifyReleaseWithR8` executed (not
  UP-TO-DATE)**, `lintVitalAnalyzeRelease`, `lintVitalRelease`, `packageRelease`, `assembleRelease`.
- **Artifact in force:** `…/build/outputs/apk/release/ALLLauncher-0.0.1-x86_64.apk` · 193,000,813 B ·
  mtime 2026-09-18 13:53:40 · **SHA-256 `530B280452DBD34849A3030C0422193ADB326745FC86D4761D6B0ECC3D2C0EAA`** ·
  `com.all.launcher.app` vc 1 / vn 0.0.1 · minSdk 26 / targetSdk 34 · native-code x86_64 only · not debuggable ·
  `apksigner verify -v`: Verifies, v2 true, v1 false, 1 signer, cert SHA-256
  `b72b76bd56f32ae16a3c10bac9fad1bd68a8d48ada273fcf4530f854089d2177`.
- `mapping.txt` rewritten to 163,055,898 B at 13:53:29. Debug APK untouched (`8AAAD6AD…`, mtime 09-17 14:22:13).
  - **[SUPERSEDED 2026-09-18 22:09:42: true of that moment only. The Debug APK has since been rebuilt to
    `8823BEC0…` (233,267,910 B) from `ModDoctor.kt 7ce70230…`, and the debug package was updated and started
    for the missing-dependency E2E. See the newest section.]**
- Two earlier invocations failed on command-line errors, not code (`10-`, `11-build-full.log`): building from the
  real path `D:\ALL Launcher 0.0.1`, whose space makes NDK r25 abort at `add-application.mk:88`; and cmd's
  `set JAVA_HOME=… &&` capturing a trailing space. No source or config was changed for either.

### 2. Device acceptance
- Update install over the existing package (`adb install -r` → `Success`), no uninstall, no data clear:
  `firstInstallTime` 2026-09-17 19:30:54 **unchanged**, `lastUpdateTime` advanced to 2026-09-18 14:08:29.
- Retained and re-read from the real UI: `ALLQA001` (Offline) · `1.21.11 Fabric 0.19.5` · Search Platform
  `แพลตฟอร์มเป้าหมาย` = **CurseForge** — before the update, on the new build before the press, and after the test.
- Fixture `balm-fabric-1.21.10-21.10.11.jar` (699,812 B · SHA-1 `6a7a8d10…` · SHA-256 `89e1d7bc…`, device hashes
  identical) pushed into `…/versions/1.21.11 Fabric 0.19.5/mods/`; Fabric API untouched.
- One press of `เริ่มเกม`. Dialog: `Mod Problems` / `Minecraft launch paused. Found 1 mod problem(s).` /
  `Wrong Minecraft version: Balm is published for Minecraft 1.21.10, not 1.21.11. File: balm-fabric-1.21.10-21.10.11.jar`
  / actions `ยกเลิก` (800,488) · `Fix` (800,551) · `ใช้ไปก่อนเถอะ เนอะ` (800,614).
- Launch blocked: `mResumedActivity` and `mCurrentFocus` stayed `MainActivity`; only `com.all.launcher.app` and
  `:filemanager` running; logcat `VMActivity` = 0; fatal markers = 0.
- `ยกเลิก` only: dialog closed, home UI restored, process healthy. `Fix` and launch-anyway never pressed.
- Restore: fixture removed; mods dir = only `fabric-api-0.141.6+1.21.11.jar`; SHA-1 set `{c98467cb…}` and
  SHA-256 set `{bdff7fd7…}` identical to baseline; no `.disabled`, no temp, no QA file.
- Debug package untouched: `0.0.1-debug`, firstInstallTime 2026-09-16 12:33:37, lastUpdateTime 2026-09-17
  14:53:13, external files unchanged, no process.
- Installed-artifact identity: `codePath /data/app/com.all.launcher.app-u5j5riscIsSkQ9f5_6DNkQ==`, and
  `sha256sum base.apk` = `530b2804…` — the installed binary is the built artifact.

### 3. Why this proves MODRINTH was used while the preference was CURSEFORGE
1. **Source determinism.** `ModDoctor.kt:149` passes `modDoctorFingerprintPlatform(AllSettings.searchModPlatform.getValue())`
   and `:917` returns `Platform.MODRINTH` unconditionally; `InstalledModMatcher.kt:147-151` branches
   MODRINTH → `getModrinthVersBySha1`, CURSEFORGE → `getCFFilesByFingerprints`.
2. **The finding's own wording.** `is published for Minecraft …` is produced only at `ModDoctor.kt:837`, the
   provider branch; the local branch at `:827-832` renders `declares Minecraft …`. The dialog therefore proves
   provider-supplied `game_versions` reached the validator.
3. **CurseForge cannot have supplied it, and would have said so.** Every CurseForge fingerprint batch on this
   device returns 401 (missing API key) and a failed batch writes no cache entry
   (`InstalledModMatcher.kt:152-170`), so a CURSEFORGE-keyed entry cannot exist and a CURSEFORGE lookup would
   necessarily have logged `Failed to match installed mods on platform: CURSEFORGE` at the press. The app log has
   no such line at 14:10:13; the two it does contain (14:09:30.072, 14:11:57.655) belong to the browse screen,
   which follows the user setting by design.
4. **Differential.** Pre-decoupling APK at CURSEFORGE: that failure was logged at its press, no dialog appeared,
   and Minecraft launched. Decoupled APK at CURSEFORGE: no failure, dialog, no game.

### 4. Correction recorded, not hidden
The first write-up of this run offered the logcat line
`[PlatformSearch/DEBUG] Starting to attempt to perform the operation on source: {Official Modrinth}` as the proof.
That was wrong: `mirroredPlatformSearcher` emits it only when `printLog=true` (`_PlatformSearch.kt:75-79`), and
**both** fingerprint routes pass `printLog=false` (`:388-390`, `:400-404`); the line is Mod Doctor's
replacement-version lookup (`ModDoctor.kt:598/734` → `getVersions`). No fingerprint call logs anything, in either
direction. The same misreading exists in `test-artifacts/release-moddoctor-modrinth-setting-confirm/00-PASS-summary.txt:30-35`.
A claim that the call was live rather than a same-day cache hit was also unsupported and has been struck. The
conclusion rests on §3, not on that line.

### 5. Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\release-moddoctor-fingerprint-decouple-e2e\`
- `30-phase2-preinstall-state.txt` · `40-phase5-8-QA-EVIDENCE.txt` (with its CORRECTION text)
- `41-phase5-app-log.txt` (this run) · `42-OLDAPK-2154-app-log.txt` · `43-OLDAPK-2214-modrinth-app-log.txt`
- `44-raw-recapture-2026-09-18.txt` (artifact, mapping, gates, package state, mods, repo)
- `50-REVIEWER-report.txt` (independent review, which identified the error in §4)
- UI dumps `20` `21` `22` `23` `24` `31` `32` `35` `36` `38` `39` and `37-phase5-logcat.txt`
- build logs `10-` (space path) · `11-` (JAVA_HOME) · `12-` (the successful build); `00-`, `01-`, `99-` belong to
  the aborted 2026-09-17 23:21:33 attempt and are not part of this run

### 6. Safety
- `git diff --check` rc 0; 27 working-tree entries; HEAD still `b4c9415b`; no commit made.
- No source, test, ProGuard, signing or APK file was modified during the rebuild or the QA.
- Debug package never started. `emulator-5554` never used.
- Only files added: evidence under `test-artifacts/`, which is untracked and **not** git-ignored.

### 7. Superseded statements (annotation only — dated text left as written)
- `:40`, `:41` — "checkpoint is the only untracked file", "no APK release asset is present": false now.
- `:1170` — "under the git-ignored `test-artifacts/`": **false**, corrected inline.
- `:1181` — "provider choice still follows searchModPlatform for fingerprint matching": superseded by `:2491`.
- `:1302`, `:1382` — duplicate of a decision already resolved at `:1384`.
- `:1902-:1938` — treats `assembleRelease` as forbidden and recommends a `releaseQa` build type that does not
  exist; superseded by `:1940`/`:2066`/`:2144` and by this run.
- `:1946`, `:2072`, `:2135` — "no release APK produced": true when written, stale now.
- `:2173` — "built from the current source state": true only at 19:20 on 09-17; annotated inline.
- `:2218`, `:2262`, `:2327`, `:2375`, `:2384`, `:2430`, `:2481`, `:2518`, `:2565` — present `920b6a70…` as the
  current release artifact: superseded by `530b2804…`.
- `:2497` — overstates the decoupling scope; corrected inline.
- `:2533`, `:2573-:2574` — ask for the rebuild, the device gate and a product decision that are all done.
- `:2535` — its heading sits after the decoupling entry and reads as if a platform switch is still required.
- `ALL_SYSTEM_AUDIT.md:25`, `:26`, `:28`, `:38` — describe the tree before Mod Doctor existed (dated 2026-09-16).

### Exact Next Action
**[DONE 2026-09-18 — the decision below was taken and implemented the same day; see the newest section.
Superseded, kept as written.]**

`MICRO-RUN NEXT (product decision, no device QA): the missing-dependency repair still resolves its project
through the user's platform (ModDoctor.kt:498, :520), so on this device a default-config user has no working
CurseForge project lookup (403). Decide whether the repair path should be decoupled too, or whether the CurseForge
API key is supplied instead. Do not change source until that decision is made. Do not rebuild the Release APK and
do not re-run this device acceptance — both were completed and accepted on 2026-09-18.`

## MICRO-RUN — Missing-dependency repair decoupled from Search Platform — PASS (source + tests only)

Timestamp: `2026-09-18 21:52 +07:00`. Scope: **source and tests only — no APK was built, no device was used,
no credential or API key was changed.**

### 1. The decision
The residual the previous milestone recorded is discharged: Mod Doctor's *missing-dependency repair*
project/version resolution no longer follows the user's Search Platform preference. It resolves on MODRINTH,
independently of what the user browses with.

### 2. Rule in force (quotable)
The user's Search Platform (`AllSettings.searchModPlatform`, default `CURSEFORGE`) governs user-facing
browse/search/download only. Every provider call inside Mod Doctor is pinned to `Platform.MODRINTH` by exactly
two helpers that ignore their user-preference argument — `modDoctorFingerprintPlatform(...)`
(`ModDoctor.kt:920`, installed-file identity) and `modDoctorDependencyRepairPlatform(...)` (`ModDoctor.kt:929`,
missing-dependency project/version resolution, applied at `:523`). There is **no cross-provider fallback**, no
provider voting and no fuzzy matching: timeout, provider error, project-not-found and no-compatible-release each
leave the finding visible with `Automatic repair is unavailable: <reason>` and never block launch.

### 3. The change — one production call site, one new helper
- `ModDoctor.kt:523` — `platform = platform,` → `platform = modDoctorDependencyRepairPlatform(platform),`
  inside the `resolutions` block calling `resolveFixCandidate` (`:518-528`). **The single production behaviour change.**
- `ModDoctor.kt:922-929` — new `internal fun modDoctorDependencyRepairPlatform(userSearchPlatform: Platform): Platform = Platform.MODRINTH`, with KDoc.
- `ModDoctor.kt:490-496` — KDoc paragraph on `withFixCandidates` recording the rule.
- `ModDoctorTest.kt` — stub `ModDoctorProviderStub` gained `searchPlatform` (default `Platform.MODRINTH`) and a
  `versionLookups` recorder; new tests at `:479` (rule guard), `:487` and `:500` (the two coupling regressions).

### 4. RED, then GREEN
RED — the helper was present with its final body but production did not call it yet, so the failure is a
product failure and not a compile error:
`.\gradlew.bat :ZalithLauncher:testDebugUnitTest --tests '*ModDoctorTest'` from `D:\ALL001`, `JAVA_HOME=C:\jdk21`
→ `33 tests completed, 2 failed`, the two being exactly
`missingDependencyRepairIgnoresUserSearchPlatform` — `expected:<[(fabric-api, MODRINTH)]> but was:<[(fabric-api, CURSEFORGE)]>`
and `missingDependencySlugLookupIgnoresUserSearchPlatform` — `expected:<[(fabric-api, 1.21.11, MODRINTH)]> but was:<[(fabric-api, 1.21.11, CURSEFORGE)]>`;
JUnit XML recorded `tests="33" failures="2" errors="0"`.
GREEN — `BUILD SUCCESSFUL in 2m 13s`; totals ModDoctorTest 33/0, ModDoctorReplacementRepairTest 9/0,
ModFileDisableSemanticsTest 2/0, ModrinthVersionPagingTest 2/0 = **46 tests, 0 failures, 0 errors**, all at
`2026-09-18T14:50:25Z`. The filter `*InstalledModMatcher*` matched no class — there is no such test file — so
four suites ran, not five.
Tooling note: the divergence recorded at old `:2549` (filtered `--tests` reporting "No tests found for given
includes" for every class) did **not** reproduce under JDK 21 this run; the plain Gradle test task detected and
ran the filtered suites normally. The earlier record stands as written; this run simply did not hit it.

### 5. Call sites — changed, and deliberately unchanged
CHANGED: `:523` (dependency resolution) and the new helper `:929`.
UNCHANGED: `:149` (health-check fingerprint) and `:505` (repair-path installed-file identity) — both already
MODRINTH through `modDoctorFingerprintPlatform` (`:920`), so applying the new rule there would be a no-op;
`AllSettings.kt:598` default `CURSEFORGE`; `SearchModScreen.kt:46` (read) and `:57` (still the only write site);
`DownloadModViewModel.kt:45`; the whole Download/Search UI; `_PlatformSearch.kt`, `ModrinthSearcher.kt`,
`CurseForgeSearcher.kt`, `InstalledModMatcher.kt`, `InstalledMod.kt`, `LauncherElements.kt`,
`_Download.Dependency.Tasks.kt`.
RESIDUAL, deliberate and citable: after this change the `platform` parameter at `:501` has **no observable
effect anywhere inside `withFixCandidates`** — both of its consumers (`:505`, `:523`) ignore it. It is retained
only so the signature does not change, which would break the named arguments at `ModDoctorTest.kt:771` and
`ModDoctorReplacementRepairTest.kt:293`. It is **not** a decision point and must never be described as one.

### 6. Source hashes
- `ModDoctor.kt` `7CE70230F7E48DACD464453177990714D5AD27FF063EE6BE7EDFC1F5217F88C5` (was `210cf50d…`; 41,799 B;
  mtime 2026-09-18 21:48:04 +07, verified to precede the GREEN run at 21:50:25 +07 — so the reviewed source is
  the revision that was compiled and tested).
- `ModDoctorTest.kt` `BAABB5199D21F9DE379E743034ADEAE53F6A732DFA14BA73C4F109544E3295D0` (was `b39d6f00…`).
- Unchanged and re-verified after the edit: `AllSettings.kt fb4d9034…`, `InstalledModMatcher.kt be7d8296…`,
  `LauncherElements.kt f92d8e1d…`, `SearchModScreen.kt 6f708f7d…`, `DownloadModViewModel.kt 4c277553…`.

### 7. What this milestone did NOT prove — and must not be claimed as proving
- **No installed artifact contains this change.** The Release APK in force (`530b2804…`) was built from
  `ModDoctor.kt 210cf50d…` and therefore predates it. Neither the 2026-09-18 Release device acceptance nor the
  2026-09-16 missing-dependency E2E is evidence about this change.
- **No live provider was called by any test.** The production defaults (`lookupProjectDirect`,
  `searchProjectBySlug`, `loadProjectVersions`) and the real entry point (`:153`, which passes no `platform` and
  so takes the `AllSettings` default at `:501`) are executed by no test; the `:153 → :501 → :523` link rests on
  reading, not on a test. There is no CurseForge-side test.
- **CurseForge was never exercised.** "Mod Doctor needs no CurseForge API key" holds on the stronger ground that
  Mod Doctor no longer passes CURSEFORGE to any provider call.
- **The RED JUnit XML no longer exists** — the GREEN run overwrote it at the same path. The RED rests on the
  transcription in `test-artifacts/mod-doctor-dependency-repair-decouple/00-RED.txt`; both assertions were
  independently re-derived from the current source and are coherent with it.
- **Withdrawn claim.** An earlier draft of that evidence file said the change "fixes a real defect" in which a
  dependency Fix previously died at verification (`InstalledModMatcher.kt:149-161` → `ModDoctor.kt:239-244`). It
  is withdrawn and restated as **latent**: that path is reachable only when `fixable` is non-empty, which before
  the edit required CurseForge resolution to succeed first, so in the only configuration ever observed the two
  failure modes are mutually exclusive. The honest description is that one provider is now used where two could
  previously disagree.

### 8. Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-dependency-repair-decouple\`
- `00-RED.txt` · `01-GREEN-and-verification.txt` · `02-call-site-classification.txt` (carrying the LATENT correction)
- JUnit XML, four files, under `ZalithLauncher/build/test-results/testDebugUnitTest/`
Repo at the end of the run: 27 working-tree entries, `git diff --check` exit 0, HEAD `b4c9415b`, no commit made.
`ModDoctor.kt` and the whole Mod Doctor test directory remain **untracked**, so no repository diff exists for any
of this — a prior copy is the only way to diff it, and none is retained.

### Exact Next Action
**[PARTLY DONE 2026-09-18 22:09-22:22 — the DEBUG half of this line was executed and passed: Debug APK
`8823BEC0…` built from exactly this source state, the fixture E2E run with the Search Platform at CURSEFORGE,
detect → Fix → one cdn.modrinth.com download → clean re-check → Main Menu, mods restored. The RELEASE half is
still open and is the next milestone. See the newest section.]**

`MICRO-RUN NEXT (build + device gate): rebuild the Release APK from this source state (ModDoctor.kt 7ce70230…) and
run the missing-dependency Fix E2E on the device with the Search Platform deliberately LEFT AT CURSEFORGE. Expect
the Mod Problems report to carry a Fix action where the 2026-09-16 run had none, expect exactly Fabric API for
1.21.11/Fabric to be installed, the post-fix re-check to be clean, and Minecraft to reach the main menu. Stop at
the first divergence. Restore the mods directory to its snapshot afterwards and verify the SHA-1s. Do NOT re-run
the fingerprint device acceptance — it is closed on 530b2804…, which does not contain this change.`

## MICRO-RUN — Missing-dependency detect + fix on a Debug build built from the decoupled source — PASS

Timestamp: `2026-09-18 22:09-22:22 +07:00`. Device: LDPlayer `127.0.0.1:5555` (`emulator-5554` present and never
used). Scope: build + device QA. **No Release build was made.** Platform under test: **CURSEFORGE**.

### 1. Artifact accepted
- Built this run from `D:\ALL001`, `JAVA_HOME=C:\jdk21`: `:ZalithLauncher:assembleDebug -Darch=x86_64` →
  `BUILD SUCCESSFUL in 1m 57s`, 123 tasks: 17 executed, 106 up-to-date, with `compileDebugKotlin` and
  `packageDebug` executed.
- `…/build/outputs/apk/debug/ALLLauncher-Debug-0.0.1-x86_64.apk` · 233,267,910 B · mtime 2026-09-18 22:09:42 ·
  **SHA-256 `8823BEC0B9343330BD33D3074689BB1593F37CE1FB9EAE603E9F5B7C9CD3BB59`** · package
  `com.all.launcher.app.debug` · vc 1 / vn 0.0.1-debug · minSdk 26 / targetSdk 34 · native-code x86_64 only ·
  debuggable · 2469 ZIP entries. It supersedes `8AAAD6AD…` (2026-09-17 14:22:13).
- Installed copy `/data/app/com.all.launcher.app.debug-uHY4A_kCIAG5IlhKWeK_Yg==/base.apk` sha256 =
  `8823bec0…`, i.e. the binary under test IS the built artifact.
- **Provenance, and its premise:** source gate passed (`ModDoctor.kt 7ce70230…`, `ModDoctorTest.kt baabb519…`,
  and `AllSettings.kt fb4d9034…`, `InstalledModMatcher.kt be7d8296…`, `ModrinthSearcher.kt b707c059…`,
  `LauncherElements.kt f92d8e1d…` unchanged) → APK mtime (22:09:42) is later than `ModDoctor.kt`'s (21:48:04) →
  installed digest equals the built digest → and, decisively, **the shipped dex carries the new symbol**:
  scanning every `classes*.dex` gives `classes16.dex dependencyRepairHelper=YES fingerprintHelper=YES`, all
  others `no`. `modDoctorDependencyRepairPlatform` exists only in revision `7ce70230…`. This symbol trick works
  **because the debug variant is not minified** (`ZalithLauncher/build.gradle.kts:87`); the release variant is
  (`:78`), which is why it cannot be reused there and Release acceptance must be behavioural.

### 2. Device result
- `adb install -r` → `Success`, no uninstall, no data clear. `firstInstallTime` 2026-09-16 12:33:37 **unchanged**;
  `lastUpdateTime` → 2026-09-18 22:12:42. Retained: `ALLQA001` · `1.21.11 Fabric 0.19.5`.
- **The debug package's Search Platform was Modrinth before this run and the run changed it to CURSEFORGE**
  through the normal UI picker (`ดาวน์โหลด → มอด → แพลตฟอร์มเป้าหมาย`), because the milestone requires CurseForge.
  No MMKV was edited. **The device's debug package now sits at CURSEFORGE where the user had Modrinth.**
- Fixture: `continuity-3.0.1-beta.1+1.21.11.jar` (project `1IjD5062`, version `mX1iknM1`, required dependency
  project `P7dR8mSH` = Fabric API) present, `fabric-api` moved out of `mods` to a `.qa-backup`.
- **Detection** (one press of `เริ่มเกม`): `Mod Problems` / `Minecraft launch paused. Found 1 mod problem(s).` /
  `Missing required dependency: Continuity requires fabric-api, but no enabled installation was found.` /
  `File: continuity-3.0.1-beta.1+1.21.11.jar`, with **Fix offered**. Minecraft blocked — MainActivity stayed
  resumed, no `:game` process.
- **Fix** (one press, platform still CURSEFORGE) — the load-bearing evidence:
  `[22:16:55.157] [PlatformSearch/DEBUG] … {Official Modrinth}` and
  `[22:16:55.947] [FileDownloader/INFO] Downloading: https://cdn.modrinth.com/data/P7dR8mSH/versions/6qAuTtLR/fabric-api-0.141.6%2B1.21.11.jar`.
  `P7dR8mSH` is exactly the fixture's declared required dependency. No `curseforge`/`forgecdn` literal appears in
  either log; exactly one download.
- **Installed:** `fabric-api-0.141.6+1.21.11.jar`, SHA-1 `c98467cb…` / SHA-256 `bdff7fd7…` — byte-identical to
  the original it replaced. No duplicate, no unrelated mod, no wrong loader, no second project.
- **Second Mod Doctor pass: clean BY CONSTRUCTION, and inferred from the launch proceeding** — the app logs
  neither inspect, so this is not an observation. `LauncherElements.kt:393-398` re-inspects and continues only
  on `CONTINUE_LAUNCH` (`ModDoctor.kt:100-102`), and the launch reached VMActivity, so the re-check found
  nothing. No repair loop: one download line in the session.
- **Minecraft:** VMActivity resumed, `:game` alive. `latest.log`: `Loading Minecraft 1.21.11 with Fabric Loader
  0.19.5` · `Setting user: ALLQA001` · `Backend library: LWJGL 3.3.3-snapshot` ·
  `Reloading ResourceManager: vanilla, continuity, fabric-api, … modmenu, placeholder-api` ·
  `OpenAL initialized on device OpenSL` · `Sound engine started` · texture atlases created. `fabric-api` is in
  the loaded list, so the repaired dependency is active.
  **Weakest link, stated plainly:** no log line literally names the title screen — the cited lines are startup
  milestones — so "Main Menu" rests on VMActivity + `:game` + no fatal + continuous frames, plus
  `50-game-screenshot.png`, whose pixels no agent here could read.
  Errors present are the known offline/benign ones: oshi/JNA `libc.so.6` UnsatisfiedLinkError, the flite
  narrator failing downstream of it, `Failed to fetch user properties` 401, and the Realms auth failures.
  None is a Mod Doctor, R8 or launcher failure.
- **Restore:** continuity removed, the downloaded file deleted, the original moved back from `.qa-backup`, the
  backup gone; mods = the three baseline files with **SHA1_SET_IDENTICAL** and **SHA256_SET_IDENTICAL**
  (the restored jar carries its original 2026-09-17 13:13 mtime, so it is the original, not a re-download); no
  `.disabled`, no temp, no QA file.
  Minecraft rewrote the instance's own `options.txt` and appended to `logs/` while it ran — the game's normal
  files, not QA residue.
- **Release preserved:** `com.all.launcher.app` `firstInstallTime` 2026-09-17 19:30:54 and `lastUpdateTime`
  2026-09-18 14:08:29 both unchanged, release mods dir untouched, release package never installed over.

### 3. The Release is one revision behind — record it, do not blur it
The product artifact in force is still `530B2804…`, built from `ModDoctor.kt 210cf50d…`, which has the
fingerprint decoupling but **not** this repair decoupling. On the shipped Release, a missing-dependency finding
at the default CURSEFORGE still ends in `Automatic repair is unavailable: …` instead of a working Fix. Release
has had no R8 pass on this revision at all, so the new helper and the paged repair route have never run minified
or shrunk. Mitigating and worth keeping: the same *mechanism* already ran under R8 on `530b2804` and worked
(the Balm wrong-version dialog at CURSEFORGE), and R8 was exonerated in the earlier diagnosis — so this is a
**coverage gap, not a known defect**.

### 4. Observation raised by review, recorded and left open
`E MMKV : java.lang.IllegalArgumentException: No enum constant …platform.Platform.com…Platform` at
`22:15:54.366` and `22:16:54.520`. A cached `InstalledMod` fails to decode at `InstalledModMatcher.kt:133`;
MMKV logs it and returns null, so the entry is dropped and re-resolved over the network — which is why the run
was unaffected, and why "exactly one provider call" is a statement about the logs and not about cache state.
**Not attributed to this change** (neither the cache key nor the value encoding was edited), **newly recorded**
(no earlier evidence file contains the string — though the earlier release logcat window contains no MMKV lines
at all, so that is absence of coverage, not evidence of absence), and **not diagnosed**. Follow-up.
Also noted: a second `{Official Modrinth}` line at `22:15:57.009` during detection, and the pre-existing
`[BatchDownloader/WARNING] Completed-count mismatch` warning (present in evidence back to auto-dependencies-v1).

### 5. Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\missing-dependency-fix-device-e2e\`
- `00-source-build-baseline.txt` · `60-QA-EVIDENCE.txt` (corrected after review) · `61-raw-device-capture.txt`
- `62-extracted-node-text.txt` (dialog and platform node text, extracted so a reader that clips long lines can see it)
- `70-REVIEWER-report.txt` · `71-LEAD-verdict.txt`
- `10-debug-build.log` · `30-detection-dialog.xml` · `41-fix-logcat.txt` · `51-fix-app-log.txt`
- `52-minecraft-latest.log` · `50-game-screenshot.png` · the platform/UI XML captures `20`-`26`, `31`
Repo: 27 working-tree entries, `git diff --check` exit 0, HEAD `b4c9415b`, no commit. No source or config file
was modified during this run. `test-artifacts/` remains untracked and NOT git-ignored.

### 6. Verdicts
ALL Reviewer (independent, read-only, no shell): **PASS on the device behaviour; FAIL on the milestone as
claimed** at the time it reviewed — every objection was about the record, not the product, and all four were
fixed: the checkpoint is now written, the raw captures exist, the APK↔source link is closed by the dex symbol
scan, and "second pass clean" was reworded. ALL Lead: **PASS — product and evidence**; it re-read the source,
the evidence and the raw capture itself, and identified the false `:49`/`:2630` debug-package lines corrected
above.

### Exact Next Action
`MICRO-RUN NEXT (Release parity gate on the repair decoupling): rebuild the Release APK from this source state
(ModDoctor.kt 7ce70230…) with minifyReleaseWithR8 / lintVitalRelease / packageRelease EXECUTED; install -r over
the Release package with the Search Platform LEFT AT CURSEFORGE; re-run the same fixture (continuity present,
fabric-api moved out); one press of เริ่มเกม then ONE tap on Fix; require exactly one
cdn.modrinth.com/data/P7dR8mSH/… download, the installed file byte-identical (SHA-1 c98467cb…), and VMActivity
+:game. NEW THIS TIME: take a uiautomator dump AFTER the Fix and again after the launch begins, so "second pass
clean / dialog gone" is observed rather than inferred. Restore the mods directory byte-identically, verify the
digest sets, confirm no QA residue, and confirm the Debug package (8823BEC0…) untouched. Stop at the first
divergence.`

## MICRO-RUN — Missing-dependency detect + fix on a NEW R8 Release build at CURSEFORGE — PASS

Device events `2026-09-18 22:47:47-22:53:31 +07:00` · LDPlayer `127.0.0.1:5555` (`emulator-5554` never used)
Scope: Release build + device QA. Search Platform: **CURSEFORGE** throughout.

### 1. Artifact now in force
- Built this run from `D:\ALL001`, `JAVA_HOME=C:\jdk21`: `:ZalithLauncher:assembleRelease -Darch=x86_64` →
  `BUILD SUCCESSFUL in 8m 52s`, 203 tasks: 16 executed, 187 up-to-date. Executed:
  `configureNdkBuildRelease[x86_64]`, `buildNdkBuildRelease[x86_64]`, `lintVitalAnalyzeRelease`,
  **`minifyReleaseWithR8`**, `lintVitalRelease`, `packageRelease`, `assembleRelease`; the two resource-shrink
  tasks were UP-TO-DATE because no resource changed. `mapping.txt` rewritten to 163,057,156 B at 22:47:28.
- `…/build/outputs/apk/release/ALLLauncher-0.0.1-x86_64.apk` · 193,000,621 B · mtime 2026-09-18 22:47:47 ·
  **SHA-256 `A2F3FBB3B4F86CC6C33C2323A9B9187FD3B020EE44F90BD1ED7FF62D65042605`** · `com.all.launcher.app` ·
  vc 1 / vn 0.0.1 · x86_64 only · non-debuggable · v2 signature · cert `b72b76bd…2177` · ZIP 2129 entries.
  **It supersedes `530B2804…`.**
- Installed `base.apk` sha256 = `a2f3fbb3…`, identical to the built artifact, so the binary under test is the
  build. `install -r` → Success; `firstInstallTime` 2026-09-17 19:30:54 unchanged; `lastUpdateTime` →
  2026-09-18 22:50:01.
- **Provenance.** The dex-symbol trick used for Debug does NOT apply here (R8 renames/inlines). What each link
  binds: the source gate binds the reviewed source; the mtime ordering is only an ordering; the installed
  digest binds installed↔built. **Built↔source is bound by R8's own `mapping.txt`**, which preserves ORIGINAL
  names and contains `427:428:…Platform modDoctorDependencyRepairPlatform(…Platform):929:929 -> f0` — the
  helper at SOURCE LINE 929, which exists only in revision `7ce70230…`. Controls: `withFixCandidates` present;
  `modDoctorFingerprintPlatform` absent because R8 inlined it away, so a miss there is no information.
  Beside that, the behavioural discriminator: pre-decoupling code at CURSEFORGE shows no Fix at all, and a
  CurseForge-side resolution would fetch from forgecdn, not cdn.modrinth.com.

### 2. Device result
- Search Platform read from the real UI = **CURSEFORGE**, and re-read AFTER the whole QA = CURSEFORGE
  (`43-platform-after-qa.xml`); no tap in any captured window touched the picker.
- Fixture: `continuity-3.0.1-beta.1+1.21.11.jar` (`51d34c3f…` / `1d4d4e86…`) present, `fabric-api` moved out to
  a `.qa-backup`. **Detection** (one press of `เริ่มเกม`): `Mod Problems` /
  `Missing required dependency: Continuity requires fabric-api, but no enabled installation was found.` /
  `File: continuity-3.0.1-beta.1+1.21.11.jar`, with **Fix offered**; Minecraft blocked, MainActivity resumed.
- **Fix** (one tap at (800,566)) — timeline from the raw logcat:
  `22:52:41.469 input tap` → `22:52:41.566 [PlatformSearch/DEBUG] … {Official Modrinth}` →
  `22:52:42.178 [FileDownloader] Downloading: https://cdn.modrinth.com/data/P7dR8mSH/versions/6qAuTtLR/fabric-api-0.141.6%2B1.21.11.jar`
  → `22:52:49.642 START …VMActivity` (exactly once) → `22:52:50.858 Displayed …VMActivity`.
  The DETECTION pass also resolved on Modrinth (`22:51:41.896`), and that earlier call is what made a Fix
  action exist at all. Exactly one tap and one download in the window; no curseforge/forgecdn literal in any
  runtime log.
- **Installed:** `fabric-api-0.141.6+1.21.11.jar`, SHA-1 `c98467cb…` / SHA-256 `bdff7fd7…` — byte-identical to
  the baseline file it replaced. Project `P7dR8mSH`, version `6qAuTtLR`. No duplicate, no unrelated mod, no
  wrong loader, no fuzzy substitution.
- **Minecraft:** `Loading Minecraft 1.21.11 with Fabric Loader 0.19.5` · `Setting user: ALLQA001` ·
  `Reloading ResourceManager: vanilla, continuity, fabric-api, … fabricloader` (both the fixture and the
  repaired dependency active) · `OpenAL initialized` · `Sound engine started` · atlases created. Only the known
  offline/JNA errors; no Mod Doctor, R8 or launcher failure, and no R8-typical failure markers anywhere.
- **Restore:** continuity removed, the downloaded file deleted, the original moved back from `.qa-backup`;
  mods = only `fabric-api-0.141.6+1.21.11.jar` with its **original mtime 2026-09-17 20:18**; SHA-1 and SHA-256
  sets identical to baseline; no `.qa-backup`/`.disabled`/temp residue. Debug package untouched throughout
  (same codePath `uHY4A_kCIAG5IlhKWeK_Yg==`, same install timestamps, no process).

### 3. The one requirement met as a DEDUCTION, not an observation  — **[SUPERSEDED 2026-09-19: the second pass is now DIRECTLY OBSERVED; see the newest section. Kept as written.]**
This milestone asked for an **observed** second Mod Doctor pass. It is still not observed: the app logs
**neither** inspect — a case-insensitive grep for `doctor` over the whole evidence set matches only write-ups,
never a log or dump. What holds is: one tap landed on Fix, exactly one `VMActivity` START followed, and
`RealLaunch` is reachable only through `decideAfterModDoctorFix` (`LauncherElements.kt:394-398`), which returns
`CONTINUE_LAUNCH` only on `isClean` (`ModDoctor.kt:100-102`) — so "the re-check ran and found nothing" is the
only reachable path. A Lead review accepted that as sufficient for the product but required it be recorded as a
deduction; a Reviewer noted the observation set is IDENTICAL for "re-check ran clean" and "re-check never ran,
launch proceeded". **The ~3.6 s window in which a UI dump would have shown the dialog gone (22:52:46.055 →
22:52:49.642) was missed BY TIMING — both dump attempts ran at ≥22:53:00, after the START — not shown to be
impossible.** The durable fix is the next milestone's step 1.

### 4. Record corrections made after review (kept visible)
- An earlier draft said the second pass was "observed, not merely inferred" — wrong, and it contradicted this
  file's own Debug entry. Corrected above.
- An earlier draft claimed this milestone "permits equivalent window/activity evidence". It does NOT; it added
  the post-Fix dumps as a requirement with no equivalence clause. That sentence is deleted.
- An earlier draft claimed `mCurrentFocus`/`mFocusedApp` had been captured to a file; they had not. Corrected;
  the Fix-time window state is corroborated by raw window-layer events in `41-fix-logcat.txt`, and a fresh
  focus capture is appended to `61-raw-device-capture.txt`.
- `70-REVIEWER-report.txt` said `Automatic repair is unavailable` was absent "anywhere in the set"; that is
  false (it occurs in the write-up, wrapped across a line break). The true form: absent from every
  device-derived artifact.
- The main-menu heading was qualified: no log line names the title screen, and the capture ends 22:53:21, so
  "no Stopping, no FATAL" is trivially true of a truncated file. The screenshot is the only direct visual and
  no agent here can read its pixels.

### 5. Caveats that remain
The strongest static provenance link (`mapping.txt`) is not independently re-checkable by the read-only agents
(`build/` is unreachable to their tooling). R8 was exercised on the **happy path only** — direct lookup, first
page, no timeout, no not-found, no replacement fix — so the timeout cause-chain walk and the paged fallback
have run minified only on their success branches. `git diff --check` says nothing about the untracked
`ModDoctor.kt`. The MMKV `No enum constant …Platform` decode error did **not** reproduce here while MMKV lines
are present, so that is coverage-backed absence; still undiagnosed.

### 6. Verdicts
ALL Reviewer (independent, read-only, no shell): "**FAIL as claimed / PASS on the device behaviour and on
artifact identity**" — every objection was about the record, all ten were acted on (including the one that
pointed at `mapping.txt` as the R8-compatible provenance method). ALL Lead: **PASS — product and artifact
identity**, with the unobserved second pass recorded as a deduction rather than rounded up. Both reports are
filed at `70-REVIEWER-report.txt` and `71-LEAD-verdict.txt`.

### 7. Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\release-missing-dependency-fix-e2e\` — `00-source-build-baseline.txt` ·
`60-QA-EVIDENCE.txt` · `61-raw-device-capture.txt` · `62-extracted-node-text.txt` ·
`70-REVIEWER-report.txt` · `71-LEAD-verdict.txt` · `10-release-build.log` · `30-detection-dialog.xml` ·
`31-detection-logcat.txt` · `41-fix-logcat.txt` · `43-platform-after-qa.xml` · `51-fix-app-log.txt` ·
`52-minecraft-latest.log` · `50-game-screenshot.png` · `20`-`22` UI/platform XML.
Repo: 27 working-tree entries · `git diff --check` 0 · HEAD `b4c9415b` · no commit · no source or config file
modified during this run · `test-artifacts/` untracked and NOT git-ignored.

### Exact Next Action — **[DONE 2026-09-18/19: delivered, with one correction. Superseded, kept as written.]**
**[Step 1 below asked for "one INFO line per inspect … on return from `ModDoctor.kt:123`/`:133`". In the current
revision `:123` is a KDoc line and `:126-134` is the delegating overload, which returns early WITHOUT logging —
a marker there would have doubled every count. On the Lead's ruling what was delivered instead is TWO markers
per real pass, `inspect started` and `inspect finished: N problem(s) [TYPES]` at `ModDoctor.kt:143`/`:174`, plus
the explicit `post-fix re-check` marker at `LauncherElements.kt:396`. Steps 2-6 were carried out as written, and
the second pass is now DIRECTLY OBSERVED. See the newest section.]**

`MICRO-RUN NEXT (observability, then re-prove the Fix with the second pass OBSERVED):`
1. Source, **LOGGING ONLY, no behaviour change**: one INFO line per inspect — e.g. `[ModDoctor/INFO] inspect: N
   problem(s)` on return from `ModDoctor.kt:123`/`:133` — and one at the post-Fix re-check
   (`LauncherElements.kt:393`) that names itself as the re-check. Leave the inert `:501` parameter alone.
2. Build Release from `D:\ALL001` with JDK 21, requiring `minifyReleaseWithR8` / `lintVitalRelease` /
   `packageRelease` executed; record the new hash, size and mtime.
3. `install -r`, Search Platform **left at CURSEFORGE**.
4. Same fixture; one `เริ่มเกม` press → detection line with 1 problem + dialog + Fix; one Fix tap → exactly one
   `cdn.modrinth.com/data/P7dR8mSH/…` download, and then **the second inspect line with 0 problems, timestamped
   between the download and the single `START …VMActivity`** — the observation this milestone lacked.
5. Restore byte-identically, confirm no residue, confirm the Debug package untouched; record the MMKV
   decode-error count and the platform value as free by-products.
6. Checkpoint. Do NOT bundle the MMKV investigation: if the decode error recurs, diagnose it read-only before
   any further Release work; if it does not, it stays a recorded observation.

## MICRO-RUN — Mod Doctor observability: the post-Fix second pass is now DIRECTLY OBSERVED — PASS

Device events `2026-09-18 23:38:03-23:42 +07:00` · LDPlayer `127.0.0.1:5555` (`emulator-5554` never used)
Package `com.all.launcher.app` · Search Platform **CURSEFORGE** · **LOGGING ONLY, no behaviour change**

### 1. Why, and what changed
Both earlier milestones had to qualify the same fact. The Debug run recorded "clean **BY CONSTRUCTION** … the app
logs neither inspect, so this is not an observation"; the Release run recorded "the one requirement met as a
**DEDUCTION**, not an observation", with the Reviewer's sharper note that "the observation set is IDENTICAL for
're-check ran clean' and 're-check never ran, launch proceeded'". **Both qualifications are now discharged: the
two hypotheses have different observation sets.**

The change — 1 line altered, 12 added, 2 files:
- `ModDoctor.kt` (LF): `import … utils.logging.Logger`; `private const val TAG = "ModDoctor"` (`:36`); a two-line
  guard comment (`:141-142`); `Logger.info(TAG, "inspect started")` (`:143`, **outside** the try);
  `return try {` → `val result = try {` (`:144`); the finished line (`:174`); `return result` (`:175`).
- `LauncherElements.kt` (CRLF): the same import; `TAG` (`:125`); and immediately before the re-check,
  `Logger.info(TAG, "post-fix re-check: re-inspecting after Mod Doctor fix")` (`:396`).
Measured: `ModDoctor.kt` 989 → **997** lines (LF-only, CR=0); `LauncherElements.kt` 844 → **848** (CRLF
throughout). The rename to `result` was required: `val report` at `:155` would otherwise shadow.
**Source hashes of the revision that built `A253286D…`** (taken after the build and before any further edit —
this is the handle the next milestone's step 1 asks for, because the file has no git history):
  `ModDoctor.kt` `147C7D550E0394199532C75DC61E4F082F26D769AD2CC6BF69248506D62C7EDC` (was `7ce70230…`)
  `LauncherElements.kt` `5EE626314F7925DD643589C7C9043943D24BDD6B98D7A40F5AA2DEB3C43628C9`

Both markers sit outside the caught region, so the success path and the CORRUPT_METADATA catch both reach the
finished line — which is what makes a laundered failure visible instead of silent. The type suffix
(`[MISSING_REQUIRED_DEPENDENCY]` vs `[]`) is in the line because the degraded path also reports `1 problem(s)`
and the two meanings are opposites. No filename, path, mod id, project id or URL is logged by the new lines; the
bracket prefix `[ModDoctor/INFO]` is produced by Logger and was deliberately not typed into any message.

### 2. Tests
46 tests, 0 failures, 0 errors (ModDoctorTest 33 · ModDoctorReplacementRepairTest 9 · ModFileDisableSemanticsTest
2 · ModrinthVersionPagingTest 2) — unchanged by the logging edit. **Honest limit: no test reaches `inspect`, so
the suite proves compilation and no regression, NOT the log lines.**
`git status --porcelain` 27 entries · `git diff --check` 0 · HEAD `b4c9415b` · no commit.

### 3. Artifact accepted
`BUILD SUCCESSFUL in 8m 24s`, 16 executed / 187 up-to-date, with `configureNdkBuildRelease[x86_64]`,
`buildNdkBuildRelease[x86_64]`, `lintVitalAnalyzeRelease`, **`minifyReleaseWithR8`**, `lintVitalRelease`,
`packageRelease`, `assembleRelease` executed. `mapping.txt` 163,062,576 B at 23:35:54.
**`A253286D5FB3D2FB8CAA16A849AF7A2E16902238488C6408EC87186E5A835793`** · 193,000,665 B · mtime 2026-09-18
23:36:08 · `com.all.launcher.app` vc 1 / vn 0.0.1 · x86_64 only · non-debuggable · v2 signature, cert
`b72b76bd…2177` · 2129 ZIP entries.
**Provenance, doubly bound:** (i) R8 `mapping.txt` carries
`…modDoctorDependencyRepairPlatform(…Platform):937:937 -> f0` — the helper at the MEASURED new source line 937;
(ii) a dex string probe finds `inspect finished` and `post-fix re-check` inside `classes.dex`, so the new log
literals are literally in the shipped artifact and no obfuscation-surviving symbol is needed. Installed
`base.apk` sha256 = `a253286d…`; `install -r` → Success; `firstInstallTime` 2026-09-17 19:30:54 unchanged;
`lastUpdateTime` → 23:37:19.

### 4. THE OBSERVED SEQUENCE
One capture window spanning BOTH presses (logcat opened before the เริ่มเกม press; the launcher's own log pulled
afterwards). Both channels carry the same lines — the app log is the primary artifact, logcat corroborates the
activity ordering. Verbatim, device clock:
```
[23:38:50.067] [ModDoctor/INFO] inspect started
[23:39:02.094] [ModDoctor/INFO] inspect finished: 1 problem(s) [MISSING_REQUIRED_DEPENDENCY]
[23:39:43.130] [FileDownloader/INFO] Downloading: https://cdn.modrinth.com/data/P7dR8mSH/versions/6qAuTtLR/fabric-api-0.141.6%2B1.21.11.jar
[23:39:45.358] [ModDoctor/INFO] post-fix re-check: re-inspecting after Mod Doctor fix
[23:39:45.358] [ModDoctor/INFO] inspect started
[23:39:45.358] [DownloadSingle/INFO] Attempting to clear cached resource files.
[23:39:45.506] [ModDoctor/INFO] inspect finished: 0 problem(s) []
(logcat) 23:39:49.245  ActivityManager: START u0 {cmp=…VMActivity}  ·  23:39:50.148  Displayed …VMActivity: +895ms
```
Assertions, measured: `inspect started` = **2** · `inspect finished` = **2** · `post-fix re-check` = **1** ·
`CORRUPT_METADATA` = **0** · `Downloading: https` = **1**; on the decoded logcat `START …VMActivity` = **1**,
`curseforge|forgecdn` = **0**. Chain: download 23:39:43.130 < marker 23:39:45.358 < second inspect COMPLETE with
0 problems 23:39:45.506 < `VMActivity` START 23:39:49.245 — strictly after, by 3.7 s.
**Two qualifications kept, both structural:** the marker and `inspect started` share millisecond 23:39:45.358, so
their order comes from code (`:396` before `:397`) and not from the clock — the asserted chain deliberately does
not rest on that pair; and the `failureReason` guard (`:385-394`) means the marker prints only when the fix did
not report failure, so the completed install is carried by the code, the `Downloading:` line and the
byte-identical jar, not by the marker alone. A third limit, so a future "exactly 2" is not overstated: a null
`getVersionInfo()` returns early and logs nothing, so the counts hold only when both passes reach the body.

### 5. Detection, repair, Minecraft, restore, preservation
Dialog: `Mod Problems` / `Missing required dependency: Continuity requires fabric-api, but no enabled
installation was found.` / `File: continuity-3.0.1-beta.1+1.21.11.jar`, with **Fix offered**; Minecraft blocked,
MainActivity resumed. Repair resolved on **MODRINTH** — project `P7dR8mSH` (exactly the fixture's declared
required dependency), version `6qAuTtLR` — and installed `fabric-api-0.141.6+1.21.11.jar`, SHA-1 `c98467cb…` /
SHA-256 `bdff7fd7…`, byte-identical to the baseline it replaced, exactly one download. Minecraft:
`Loading Minecraft 1.21.11 with Fabric Loader 0.19.5` · `Setting user: ALLQA001` ·
`Reloading ResourceManager: vanilla, continuity, fabric-api, … fabricloader` · `OpenAL initialized` ·
`Sound engine started`. Restore: `SHA1_SET_IDENTICAL` / `SHA256_SET_IDENTICAL`, no `.qa-backup`/`.disabled`/temp.
Debug package untouched (same codePath, same install timestamps, no process). Search Platform after the QA, read
from the UI: CURSEFORGE.

### 6. MMKV observation — recorded only, NOT diagnosed
`No enum constant … Platform` occurrences: **0** in the capture window, with a positive control from the same
decoded window (`curseforge|forgecdn` = 0, exactly 1 `START …VMActivity`) so it is a coverage-backed zero. It did
**not** recur, which resolves the previous section's conditional to "recorded observation, no diagnosis".

### 7. Record corrections made after review (kept visible)
- The added-line count and both totals were undercounted by 1 (997 and 848, not 995 and 847): the blank line
  after each `TAG` was omitted from the list. The ANCHORS were correct throughout.
- The shift model quoted from the Lead was computed for the FIVE-line plan; as delivered the deltas are +5 above
  the marker, +6 inside the try and +8 at the tail.
- A note that called the `DownloadSingle …clear cached resource files.` line "the game-asset download" was wrong
  and made the evidence look weaker than it is: that line is the `onFinally` of the single-file download job —
  the fabric-api install — so it SUPPORTS the completion step. Only the ms tie with the marker remains.
- "CURSEFORGE throughout" was evidenced only AFTER the run; "`:game` process alive" had no evidence and is
  withdrawn; "original mtime" is really the pre-run state (what is proven is restoration to it).
- The title screen is NOT claimed as pixel-verified: the screenshot is cited by size only and no agent here
  reads pixels; nor is provenance claimed as diff-backed, because `ModDoctor.kt` is untracked.
- **Open debt, deliberate:** the guard comment at `ModDoctor.kt:141-142` gives a reason that is practically
  impossible (`Logger.log` returns early when uninitialised and otherwise only enqueues to an unbounded channel,
  so it cannot throw). The Reviewer's advice was to KEEP it and shorten it to name the real reason (both exits
  must be logged). It was NOT changed: editing source now would invalidate the artifact this run accepted, and
  deleting it would mean paying the anchor cost again. To be corrected the next time that file is genuinely
  edited — see the next action.

### 8. Verdicts
ALL Reviewer (independent, read-only, no shell): **PASS on the software and on the core claim — the sequence is
genuinely observed**; FAIL on the evidence record as submitted, with eight corrections, all now applied.
ALL Lead: **PASS on the software and the milestone's core claim**; FAIL on the record while the checkpoint still
named the superseded APK — now corrected. Both reports are filed at `80-REVIEWER-report.txt` and
`81-LEAD-verdict.txt`.

### 9. Evidence
`D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-observability-e2e\` — `00-logging-change.txt` ·
`10-release-build.log` · `20-home-before-launch.xml` · `30-logcat-raw.txt` · `31-detection-dialog.xml` ·
`40-fix-app-log.txt` (**the primary artifact, pulled from the device**) · `50-minecraft-latest.log` ·
`51-game-screenshot.png` · `60-platform-after-qa.xml` · `71-extracted-node-text.txt` ·
`72-logcat-ModDoctor-lines.txt` · `73-raw-device-capture.txt` · `80-REVIEWER-report.txt` · `81-LEAD-verdict.txt`.
Known record limit, restated: `30-logcat-raw.txt` and `10-release-build.log` are not readable as text by the
agents' file tools, so the decode facts (build task list, mapping line, dex probe, `START …VMActivity`) live in
this record and should be persisted as plain text next time.

### Exact Next Action
`MICRO-RUN NEXT (freeze the revision, then retire the comment debt separately):`
1. **Hash first, edit nothing**: record the SHA-256 of the current `ModDoctor.kt` and `LauncherElements.kt`.
   With no git history, that content hash is the only handle tying `A253286D…` to the reviewed source.
2. **Decide the evidence policy explicitly**: `test-artifacts/` is untracked AND NOT git-ignored, so `git add -A`
   would sweep ~20 evidence directories into any commit. Either add the ignore rule or commit the evidence
   deliberately — not by accident. (Committing is a state change and is the user's call; if declined, snapshot
   those two files outside the repo instead.)
3. **One commit on `all-0.0.1`** holding `ModDoctor.kt`, the four test files and `src/test/resources/*`, labelled
   as the observable-revision snapshot and recording the APK digest it corresponds to.
4. **Only then, and only if the debt is wanted now**, a SEPARATE commit shortening `ModDoctor.kt:141-142` to one
   line naming the real reason, explicitly marked "no released binary corresponds to this commit". No rebuild.
   Otherwise defer it to the next genuine behavioural edit of that file.
The MMKV investigation is **not** the next milestone: its condition ("diagnose if it recurs") was evaluated by
this run's coverage-backed zero and resolved to "record only".

## MICRO-RUN — CurseForge HTTP 403 on the user-facing browse/version/download path — PASS

Timestamp: `2026-09-19 00:56 +07:00`. Device: LDPlayer `127.0.0.1:5555` (transport_id 2; `emulator-5554` never used).
One milestone only: the live CurseForge 403. The Mod Doctor milestone was not reopened or retested.

### 1. Root cause (proven, not inferred)
The official CurseForge API requires a valid `x-api-key` on **every** request. This build has no key wired at any of
the three seams the build script offers — `getKeyFromLocal("CURSEFORGE_API_KEY", ".curseforge_api.txt",
defaultCurseForgeApiKey)` (`build.gradle.kts:218`, helper `:33-42`): the env var, the root file and the
`curseforge_api_key` property are all **absent** (the property exists only commented out at
`ZalithLauncher/gradle.properties:11`). Without the header the official host answers 403 for search, project and
file endpoints alike.
The project already ships a second, genuine CurseForge source — the MCIM mirror
(`MCIM_CURSEFORGE_API`, `CurseForgeAPI.kt:37`, instantiated `_PlatformSearch.kt:57`) — but its use was gated to
mainland China: `mirroredCurseForgeSource(enabledMirror = isChinaMainland())`, and `isChinaMainland()`
(`LocalUtils.kt:395-413`) is true only for timezone `Asia/Shanghai|Chongqing|Urumqi` (the offset test ends in a
literal `false`). Device timezone is `Asia/Bangkok` → gate false → mirror not offered → official-only → 403.
Transport probe from the PC (no credential used, no credential value printed):
`GET api.curseforge.com/v1/mods/search?gameId=432&searchFilter=sodium` → **403**, body
`Forbidden: API Key missing or invalid`; `GET api.curseforge.com/v1/mods/394468` → **403** (same body);
`GET mod.mcimirror.top/curseforge/v1/mods/search?…&searchFilter=sodium` → **200**, first slug `sodium`;
mirror `/mods/394468`, `/mods/394468/files?index=0&pageSize=50` (50 items), `/categories?gameId=432`,
`POST /fingerprints` → all **200**; `HEAD edge.forgecdn.net/files/8888/38/…jar` → **302** to
`mediafilez.forgecdn.net` (public CDN, no key).
**403 stage: A (search) — and B/C/D by construction**, because every stage shares one host, one client and the one
missing header. Live pre-fix proof on the installed Release (`A253286D…`, Search Platform CurseForge):
`W PlatformSearcher: Failed to perform the operation on source: {Official CurseForge}` /
`g51: Client request(GET https://api.curseforge.com/v1/mods/search?gameId=432&classId=6&searchFilter=Sodium&sortField=1&sortOrder=desc&index=0&pageSize=20) invalid: 403 Forbidden. Text: "Forbidden: API Key missing or invalid"`,
with the UI banner `ไม่สามารถดึงผลการค้นหาได้: (403) …`. The mirror was never offered (`{Official CurseForge}` was
the only source attempted).

### 2. The fix (smallest production seam)
Source selection only — which CurseForge source the app is allowed to ask. No provider abstraction, no
cross-provider fallback, no Modrinth routing, no new credential, no change to Mod Doctor's platform rules.
- `…/platform/curseforge/CurseForgeAPI.kt` — new pure predicate next to the two API constants:
  `internal fun curseForgeMirrorEnabled(mainland: Boolean, hasApiKey: Boolean): Boolean = mainland || !hasApiKey`
  (official source without a key is a guaranteed 403, so the mirror must be available wherever the app runs).
- `…/platform/_PlatformSearch.kt:112-119` — `mirroredCurseForgeSource(enabledMirror = curseForgeMirrorEnabled(mainland = isChinaMainland(), hasApiKey = BuildKeys.CURSEFORGE_API.isNotBlank()))`, plus the two imports.
  `resolveMirrorPriority(…, mainland = enabledMirror)` then yields `MIRROR_FIRST` on the default AUTO setting, so the
  keyless build no longer spends a 403 round-trip before the working source; a user who explicitly chose
  OFFICIAL_FIRST still gets the official source first and falls over to the mirror. Mainland behaviour and the
  configured-key case are unchanged.

### 3. Files changed this run
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/download/assets/platform/curseforge/CurseForgeAPI.kt` (+10)
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/download/assets/platform/_PlatformSearch.kt` (+6/−2)
- `ZalithLauncher/src/test/java/com/movtery/zalithlauncher/game/download/assets/platform/curseforge/CurseForgeSourceSelectionTest.kt` (new, 1,290 B)
Nothing else was edited: `ModDoctor.kt` mtime is still 2026-09-18 23:22:07 (SHA-256 `147C7D55…`), Mod Doctor's
two pinned helpers, `modrinth` sources, ProGuard rules and signing are untouched.

### 4. Tests
`:ZalithLauncher:testDebugUnitTest` on package `com.movtery.zalithlauncher.game.download.assets.platform`
(`gradlew.bat … --no-daemon`, `JAVA_HOME=C:\jdk21`, from the junction `D:\ALL001`; the harness **did** discover the
classes this time, so the older `No tests found` note does not reproduce):
- `CurseForgeSourceSelectionTest` — **4 tests, 0 failures, 0 errors** (mainland+key, mainland+no key, non-mainland+no key, non-mainland+key, and that the mirror source carries the same `Platform.CURSEFORGE`).
- `ModrinthVersionPagingTest` — **2 tests, 0 failures, 0 errors** (existing suite, unaffected).
`git diff --check` rc 0.

### 5. Tested artifacts
- Debug `…/build/outputs/apk/debug/ALLLauncher-Debug-0.0.1-x86_64.apk` · 233,267,910 B · mtime 2026-09-19 00:30:00 · **SHA-256 `21712A2EBAD000C379F0FB997E166B76212F47AE884B26602693E1B2797B7695`** · installed over (`lastUpdateTime` 2026-09-19 00:32:06, `firstInstallTime` 2026-09-16 12:33:37 unchanged).
- Release `…/build/outputs/apk/release/ALLLauncher-0.0.1-x86_64.apk` · 193,000,737 B · mtime 2026-09-19 00:50:41 · **SHA-256 `B9821D1C422046B5E7D4D12EC63996EF3823CE91A18AD8529F62057DC701527B`** · built with `minifyReleaseWithR8` + `lintVitalRelease` + `packageRelease` (BUILD SUCCESSFUL in 8m22s) · installed as an update over `A253286D…` (`lastUpdateTime` 2026-09-19 00:51:15, `firstInstallTime` 2026-09-17 19:30:54 unchanged) — **this is now the Release in force**.

### 6. Real CurseForge QA through the production UI
Search Platform = **CurseForge** (read from the UI control on both packages; never written by file).

**Debug package**
- Project lookup: `Search by ID` → `394468` → `Sodium · โดย JellySquid · CurseForge · The fastest and most compatible rendering optimization mod for Minecraft… · 150M · Fabric Quilt NeoForge · การออปติไมซ์ · มอด`.
- Project body: `Underwater Lighting Improvements …`, `Biome Blending Improvements …` (real CF description).
- Version/file lookup: `1.21.11 Fabric` → `Sodium 0.8.14 for Fabric 1.21.11 (95K, 20 วันที่แล้ว, เวอร์ชันเสถียร)`, `0.8.14-beta.2`, `0.8.13`, `0.8.13-beta.3`, `0.8.13-beta.2`, `0.8.13-beta.1`; other groups `1.21.11 NeoForge|Quilt`, `1.21.10 …`, `1.21.9 …`.
- Download: `Sodium 0.8.14 for Fabric 1.21.11` → install dialog (`1.21.11 Fabric 0.19.5`) → `ติดตั้ง` → `AppLog [FileDownloader/INFO] Downloading: https://edge.forgecdn.net/files/8756/612/sodium-fabric-0.8.14%2bmc1.21.11.jar`.
- Result: `mods/sodium-fabric-0.8.14+mc1.21.11.jar` 1,909,150 B; device `sha1sum 4a8733fe544168068d12fa94b2ab03e6c0c7bf91` = the SHA-1 CurseForge publishes for file `8756612`; first bytes `PK\x03\x04` (valid ZIP/JAR).
- **`403` appears 0 times and no `PlatformSearcher` failure is logged anywhere in the run.**

**Release package** (the installed artifact, same flow)
- Search stage: opening the mods tab with CurseForge selected ran the real search and returned a page —
  `log_2026-09-19T00-51-26_main.log`: `[00:51:44.648] [SearchAssetsScreen/INFO] Searched page info: {pageNumber: 1,
  pageIndex: 0, totalPage: 500, isLastPage: false}` (pre-fix this same screen produced the `(403)` banner instead).
- `394468` → `Sodium · CurseForge · 150M`; version groups `1.21.11 Fabric|NeoForge|Quilt`, `1.21.10 …`, `1.21.9 …`; `1.21.11 Fabric` → `Sodium 0.8.14` → `ติดตั้ง` → `[FileDownloader/INFO] Downloading: …sodium-fabric-0.8.14%2bmc1.21.11.jar` → `mods/sodium-fabric-0.8.14+mc1.21.11.jar`, `sha1sum 4a8733fe…` (identical to the debug run).
- Every source attempt in that session log reads `{MCIM CurseForge}`; the whole post-fix Release log contains **no**
  `403`, `Forbidden`, `Failed to perform` or `ERROR` line (pulled as `test-artifacts/cf-probe/release-app-log-postfix.log`).
- Final state left on the Release: app restarted, `แพลตฟอร์มเป้าหมาย = CurseForge` re-read from the control.

### 7. Modrinth regression
Debug package, platform switched through the same production dropdown: `แพลตฟอร์มเป้าหมาย → Modrinth`, then
`Search by ID` with the Modrinth project id `AANobbMI` → `Sodium · Modrinth · A high-performance rendering engine
replacement for Minecraft, which greatly improves frame rates and reduces micro-stutter. · 227M · 40K · Fabric
NeoForge Quilt · การออปติไมซ์ · มอด` — Modrinth's own record, proving no cross-wiring and no Modrinth regression.
Platform then restored to **CurseForge** through the same control on the debug package; the Release package's
setting was CurseForge before and after (read from the UI).

### 8. Secrets
The credential value was never read, printed, hashed, encoded, logged, stored or committed — only PRESENT/ABSENT of
the three build seams was established (`env CURSEFORGE_API_KEY` not set; `.curseforge_api.txt` absent;
`curseforge_api_key` commented out). No new key was requested or created, and no key was added to source. The fix
needs no credential at all.

### 9. Cleanup / restore
Both QA downloads deleted; debug `mods/` back to `fabric-api-0.141.6+1.21.11.jar`, `modmenu-17.0.0.jar`,
`placeholder-api-2.8.2+1.21.10.jar`; release `mods/` back to `fabric-api-0.141.6+1.21.11.jar`
(sha1 `c98467cb…`). No uninstall, no data clear, no `.disabled`/temp/backup residue; the three animation scales
this run set to 0 on the device were restored to 1. Evidence kept under `test-artifacts/cf-probe/`.

### 10. Honest limits
The official `api.curseforge.com` path is **still unusable in this build** and cannot be fixed without a CurseForge
API key that this project does not possess — the mirror is the working CurseForge transport, and it is a mirror of
genuine CurseForge data (search, project, files, categories, fingerprints, CDN), not a substitute provider. If a
key is ever supplied through one of the three existing seams, the sender is attached automatically by
`CURSEFORGE_INTERCEPTOR`/`GLOBAL_CLIENT` and mainland gating returns to exactly its previous behaviour.
The version list of a CurseForge project is only fully enumerable through the app's own paging; this run stepped
the `1.21.11 Fabric` group only, which is what the milestone required.

### Exact Next Action
`MICRO-RUN NEXT (four-mod matrix — NOT started here): Sodium Extra, Reese's Sodium Options and Iris, each
Compatibility QA against the shipped Release `B9821D1C…` with Search Platform left at CurseForge.`

## MICRO-RUN — 4-mod Sodium/Iris Fix QA — FAIL (repair works, dependency version choice does not)

Timestamp: `2026-09-19 01:13 +07:00`. Device LDPlayer `127.0.0.1:5555`. Artifact: Release `B9821D1C…`
(no source change, no rebuild). Search Platform CURSEFORGE throughout.

### Target
`Minecraft 1.21.11` / `Fabric Loader 0.19.5`, instance `1.21.11 Fabric 0.19.5` in `com.all.launcher.app`.

### Final compatible set (real provider metadata, CurseForge ids)
- Sodium — project 394468, file 8756612 `sodium-fabric-0.8.14+mc1.21.11.jar` (sha1 `4a8733fe…`)
- Sodium Extra — project 447673, file 8892324 `sodium-extra-fabric-0.9.4+mc1.21.11.jar` (sha1 `5ee45353…`), requires sodium
- Reese's Sodium Options — project 511319, file 8891844 `reeses-sodium-options-fabric-2.2.4+mc1.21.11.jar`
  (sha1 `295623b7…`), requires sodium
- Iris — project 455508, file 7805348 `iris-fabric-1.10.7+mc1.21.11.jar` (sha1 `aae8567b…`), requires sodium

### Initial problem (one launch press, 01:06:45)
`Mod Problems — Minecraft launch paused. Found 3 mod problem(s).` Each of Iris / Reese's Sodium Options /
Sodium Extra reported `Missing required dependency: … requires sodium, but no enabled installation was found.`
`Fix` was offered; Minecraft did not start before repair. `ModDoctor/INFO inspect finished: 3 problem(s)
[MISSING_REQUIRED_DEPENDENCY × 3]`.

### Fix installed (one press, 01:07:40)
`FileDownloader/INFO Downloading: https://cdn.modrinth.com/data/AANobbMI/versions/rkdTcxoT/sodium-fabric-0.8.14%2Bmc1.21.11.jar`
→ `mods/sodium-fabric-0.8.14+mc1.21.11.jar`, sha1 `4a8733fe…` (byte-identical to the CurseForge file).
Provider rule respected: search platform CURSEFORGE, dependency repair provider MODRINTH.

### Post-Fix: 0 problems
`post-fix re-check: re-inspecting after Mod Doctor fix` → `inspect finished: 0 problem(s) []` (01:07:43).

### Minecraft: FAIL — Fabric rejected the set that Mod Doctor declared healthy
`logs/latest.log`: `Loading Minecraft 1.21.11 with Fabric Loader 0.19.5` → `Mod resolution failed` →
`HARD_DEP iris 1.10.7+mc1.21.11 {depends sodium @ [0.8.x]}, NEG_HARD_DEP sodium 0.8.14+mc1.21.11 {breaks iris @
[<=1.10.7]}`, exit code 1, `ErrorActivity` shown. VMActivity did start; no main menu.

### Root cause (proven from the JARs themselves)
Version selection takes the newest provider version compatible with (MC version, loader) and never consults the
installed mod set, although the candidate JAR is downloaded anyway and its `fabric.mod.json` states the conflict:
- `sodium-fabric-0.8.14+…` : `breaks -> iris: <=1.10.7`
- `sodium-fabric-0.8.13+…` : `breaks -> iris: <=1.10.7`
- `sodium-fabric-0.8.12+…` : `breaks -> iris: <=1.10.6`  ← **the only 1.21.11 Sodium that pairs with Iris 1.10.7**
- `iris-fabric-1.10.7+…` : `depends -> sodium: 0.8.x` (satisfied by every candidate; no upper bound)
Neither provider carries this: Modrinth `version/rkdTcxoT` `dependencies` is empty; CurseForge file 8756612 has no
relations; Iris 1.10.7's only relation is `required: sodium`. Iris has no 1.10.8 for 1.21.11 (newest is 1.10.7,
2026-03-24). So on 1.21.11 the newest Sodium and the newest Iris are mutually exclusive by the mods' own
declarations, and `Sodium 0.8.12` is the correct compatible dependency for this set.

### Verdict
Detection, one-press repair, provider choice, download integrity and the post-fix re-check all PASS. The
dependency **version choice** FAILS: the app installs a version Fabric refuses and then reports `0 problem(s)`.
Not a fabricated error and not a fixture error — the four newest releases genuinely cannot run together on 1.21.11.

### Restore
Both QA cycles' files removed; mods dir back to `fabric-api-0.141.6+1.21.11.jar` only —
SHA-1 set `{c98467cb…}` and SHA-256 set `{bdff7fd7…}` identical to the pre-run snapshot. No `.disabled`, no
backup, no temp file; my `/sdcard/h*.xml` dumps removed.

### Evidence
`test-artifacts/cf-probe/` — `00-PASS-summary.txt` (CurseForge 403 run), `discover*.ps1`, `download-fixtures.ps1`,
`inspect-jars.ps1`, `inspect-older.ps1`, fixtures (`sodium 0.8.12/0.8.13/0.8.14`, `sodium-extra 0.9.4`,
`reeses 2.2.4`, `iris 1.10.7`), UI dumps `h1-h3`.

### Exact Next Action
`MICRO-RUN NEXT (product decision, no device QA): choose how Mod Doctor avoids a candidate that the installed
set cannot accept — (a) skip candidate versions whose own fabric.mod.json 'breaks' an installed mod (needs a
small Fabric version-predicate check plus newest-first retry at the download site), or (b) keep the newest pick
and surface the declared conflict in the fix reason instead of reporting 0 problems. Then rebuild the Release and
re-run THIS SAME 4-mod QA, which must end with all four mods accepted by Fabric.`

## MICRO-RUN — Sodium/Iris dependency-candidate compatibility fix — PASS (Debug)

Timestamp: `2026-09-19 02:04 +07:00`. Device LDPlayer `127.0.0.1:5555`. Debug package only; **Release untouched**.

### Root cause
`ModDoctor.fixProblems` took the **newest** provider release matching (Minecraft version, loader) —
`getVersions(...).firstOrNull { it.isCompatibleWith(version, PlatformClasses.MOD) }` — and never checked the
candidate against the set already installed, although the candidate JAR is downloaded anyway. The authoritative
facts live in each JAR's `fabric.mod.json`:
`breaks` (what the candidate forbids) and `depends` (what the installed mods demand of the candidate).
Neither Modrinth nor CurseForge exposes them (Modrinth `version/rkdTcxoT` dependencies are empty; CurseForge file
8756612 has no relations), so on this device Mod Doctor installed `sodium 0.8.14`, reported `0 problem(s)`, and
Fabric then refused to launch: `NEG_HARD_DEP sodium 0.8.14 {breaks iris @ [<=1.10.7]}`.

### Files changed
- `…/game/version/mod/FabricModCompatibility.kt` **(new, 12,363 B)** — candidate/installed Fabric facts
  (`readFabricModFacts`, `parseFabricModFacts`, `parseFabricBreaks`, `parseFabricDepends`) plus a Fabric
  version-predicate evaluator (`*`, `=`, `>`, `>=`, `<`, `<=`, `~`, `^`, `x` wildcards, space = AND, build metadata
  ignored, prerelease below release) and `candidateRejectionReason`, which rejects in **both** directions:
  candidate `breaks` an enabled mod, and an enabled mod's `depends` range not satisfied by the candidate.
- `…/game/version/mod/ModDoctor.kt` — dependency repair now walks candidates **newest-first**
  (`firstAcceptedCandidate`, `MAX_DEPENDENCY_CANDIDATE_ATTEMPTS = 12`, `tryInstallDependencyCandidate`,
  `enabledModIdentities`) and only installs the first candidate that passes; if none pass it installs nothing and
  marks the finding unavailable with every rejection reason (`markFixUnavailable`). Replacement-fix path, Mod
  Doctor provider pins (MODRINTH) and the post-fix re-check logging are untouched.
- `…/game/download/assets/_Download.Single.Tasks.kt` (`+20/−8`) — `downloadSingleForVersions` gained an optional
  `validate: suspend (File) -> String?` run on the **staged cache file before any copy** and an `onFileRejected`
  callback, so a rejected candidate never enters the version directory (staging file is still cleaned by
  `onFinally`).
- `…/src/test/java/…/game/version/mod/ModDoctorCandidateCompatibilityTest.kt` **(new, 10,802 B, 10 tests)**.
No change to the CurseForge 403 fix, the search-platform setting, or Modrinth provisioning.

### Regression
`gradlew :ZalithLauncher:testDebugUnitTest --tests "…game.version.mod.*"`:
`ModDoctorCandidateCompatibilityTest` **10/10**, `ModDoctorReplacementRepairTest` 9/9, `ModDoctorTest` 33/33,
`ModFileDisableSemanticsTest` 2/2 — **0 failures, 0 errors**. `git diff --check` rc 0.
RED proof by mutation: disabling the compatibility check (`candidateConflictReason → null`) fails 3 of the 10,
including `expected <sodium-fabric-0.8.12…> but was <sodium-fabric-0.8.14…>`; mutation reverted before the build.
Test data is the real `fabric.mod.json` of Sodium 0.8.14/0.8.13/0.8.13-beta.2, Iris 1.10.7, Sodium Extra
0.9.4/0.9.3/0.9.1 and Reese's 2.2.4/2.2.3.

### Candidate evaluation — QA A, the fixture exactly as specified (Iris 1.10.7 + Extra 0.9.4 + RSO 2.2.4)
Detected: `inspect finished: 3 problem(s)` → `Fix` → walk rejected every release, newest-first:
`0.8.14` reject (breaks iris 1.10.7), `0.8.14-beta.2` reject, `0.8.14-beta.1` reject, `0.8.13` reject,
`0.8.13-beta.3` reject, `0.8.13-beta.2` reject (`sodium-extra 0.9.4 requires sodium >=0.8.14`),
`0.8.12` reject (same), … **nothing installed**, mods directory unchanged, UI: `Automatic repair is unavailable:
every compatible release conflicts with the installed mod set: …`. No residue, no launch.
**This fixture is unsatisfiable**: RSO 2.2.4 pins `sodium = 0.8.14`, Extra 0.9.4 needs `sodium >=0.8.14`, and both
Sodium 0.8.13 and 0.8.14 declare `breaks iris <=1.10.7` (0.8.12 declares `<=1.10.6` and would fail the pin as
well). Iris has no 1.10.8 for 1.21.11. Refusing is the only correct outcome, and it is what the fix does.

### Candidate evaluation — QA B, one satisfiable fixture (Iris 1.10.7 + Extra **0.9.1** `>=0.8.1` + RSO **2.2.3** `>=0.8.11`)
Same walk: `0.8.14` reject, `0.8.14-beta.2` reject, `0.8.14-beta.1` reject, `0.8.13` reject, `0.8.13-beta.3` reject,
**`0.8.13-beta.2` accept** (its own `breaks iris <=1.10.6` does not cover the installed 1.10.7, and it satisfies
both installed requirements) → installed `mods/sodium-fabric-0.8.13-beta.2+mc1.21.11.jar`. One user `Fix` press,
13 candidate downloads at most, no residue from the rejected ones.
Note: the accepted release is a beta because the provider's list interleaves betas and selection stays
newest-first; `0.8.12` (the value assumed in the brief) cannot satisfy Extra 0.9.x/RSO 2.x pins.

### Post-Fix = 0 · Fabric
`post-fix re-check: re-inspecting after Mod Doctor fix` → `inspect finished: 0 problem(s) []`, then the client
launched: `Loading Minecraft 1.21.11 with Fabric Loader 0.19.5` → `Loading 57 mods:` including
`iris 1.10.7+mc1.21.11`, `reeses-sodium-options 2.2.3+mc1.21.11`, `sodium 0.8.13-beta.2+mc1.21.11`,
`sodium-extra 0.9.1+mc1.21.11` → `Setting user: ALLQA001` → resource reload → OpenAL started, main menu, no
`Mod resolution failed` / `NEG_HARD_DEP` anywhere. VMActivity was the resumed activity.

### Artifact
Debug `…/build/outputs/apk/debug/ALLLauncher-Debug-0.0.1-x86_64.apk` · SHA-256
`81D6BBEFD9275736602BF77015D56698982B262ADB77F12338E722400E7C16EF` · mtime 2026-09-19 01:52:27 · installed with
`adb install -r` (firstInstallTime 2026-09-16 12:33:37 unchanged). **No Release build this run** — the Release in
force is still `B9821D1C…`, and `com.all.launcher.app` was not installed, launched or modified
(`lastUpdateTime` still 2026-09-19 00:51:15, its mods dir still just `fabric-api-0.141.6+1.21.11.jar`).

### Restore
Debug mods dir back to `fabric-api-0.141.6+1.21.11.jar`, `modmenu-17.0.0.jar`, `placeholder-api-2.8.2+1.21.10.jar`
— SHA-1 set `{c98467cb…, 544a8b34…, bbdc920e…}` and SHA-256 set `{bdff7fd7…, 95f2c957…, 975c7e57…}` identical to
the pre-run snapshot. No Sodium/Extra/Reese's/Iris file, no `.disabled`, no temp file; my `/sdcard/q*.xml` dumps
removed. Evidence in `test-artifacts/cf-probe/` (`q2`, `q5`–`q7` dumps, `gradle-3…9` logs, fixture JARs, inspection
scripts).

### Exact Next Action
`MICRO-RUN NEXT (Debug only, no new feature): decide whether a repair should prefer a stable release over a beta
when both satisfy every constraint (today it stays newest-first and can install a beta, as QA B did); then rebuild
the Release so `B9821D1C…` carries this fix and re-run THIS SAME 4-mod QA on it.`

## MICRO-RUN — Stable-first dependency repair + Release parity — PASS

Timestamp: `2026-09-19 02:37 +07:00`. Device LDPlayer `127.0.0.1:5555`, Release package `com.all.launcher.app`.
Target `Minecraft 1.21.11` / `Fabric Loader 0.19.5`, Search Platform CURSEFORGE, repair provider MODRINTH.

### Product rule now implemented for automatic dependency repair
1. candidates compatible with (Minecraft version, loader) — unchanged;
2. each candidate validated against the enabled installed mod set by the existing Fabric compatibility logic
   (`candidateRejectionReason`, staged download) — unchanged;
3. **compatible RELEASE candidates newest-first are tried first**;
4. only if no compatible release exists, prerelease candidates are tried, still newest-first;
5. an incompatible release is never accepted for being stable, and a beta is never taken while a compatible
   release exists.

### Files changed
- `…/game/version/mod/ModDoctor.kt` — new `internal fun <T> stableFirstCandidates(candidates, isPrerelease)` =
  `partition(...)` → stables + prereleases (order inside each group preserved from the provider, which is
  newest-first); repair path now builds `orderedCandidates = stableFirstCandidates(compatibleCandidates)
  { it.platformReleaseType() != PlatformReleaseType.RELEASE }` and feeds that into the existing
  `firstAcceptedCandidate`. Release type comes from provider metadata (`ModrinthVersion.versionType` /
  `CurseForgeFile.releaseType` → `PlatformReleaseType`), not from filenames. Compatibility evaluation, staging,
  rejection handling and the post-fix re-check are untouched.
- `…/src/test/java/…/game/version/mod/ModDoctorStableFirstCandidateTest.kt` **(new, 5 tests)**.
No change to CurseForge search/provider seams, staging validation or rejected-candidate cleanup.

### Tests
`:ZalithLauncher:testDebugUnitTest --tests "…game.version.mod.*"`:
`ModDoctorStableFirstCandidateTest` 5/5 (case 1 stable over newer beta, case 2 incompatible stables skipped →
0.8.12, case 3 beta fallback only when no stable fits, case 4 nothing compatible → no install candidate, plus
ordering), `ModDoctorCandidateCompatibilityTest` 10/10, `ModDoctorReplacementRepairTest` 9/9,
`ModDoctorTest` 33/33, `ModFileDisableSemanticsTest` 2/2 — **0 failures, 0 errors**. `git diff --check` rc 0.

### QA A — unsatisfiable set, on the Release
Fixture Iris 1.10.7 + Sodium Extra 0.9.4 + Reese's 2.2.4, Sodium absent.
`inspect finished: 3 problem(s)` → one `Fix` → every candidate rejected (all releases newest-first, then the
prereleases), **Sodium not installed**, mods directory unchanged, dialog reason:
`Automatic repair is unavailable: every compatible release conflicts with the installed mod set: sodium-fabric-0.8.14… declares that it breaks the installed iris 1.10.7…; sodium-fabric-0.8.13…; sodium-extra 0.9.4… requires sodium >=0.8.14…, but sodium-fabric-0.8.12… provides 0.8.12…; …`.
No rejected-candidate residue, Minecraft did not proceed with the invalid set.

### QA B — satisfiable set, on the Release (candidate ordering observed)
Fixture Iris 1.10.7 + Sodium Extra 0.9.1 + Reese's 2.2.3, Sodium absent. One `Start`, one `Fix`:
- `sodium 0.8.14` stable → **reject** (`breaks the installed iris 1.10.7`)
- `sodium 0.8.13` stable → **reject** (same)
- `sodium 0.8.12` stable → **ACCEPT** (third attempt; the compatible prerelease `0.8.13-beta.2` was never
  reached, exactly as the stable-first rule requires)

### Selected Sodium
- provider version id: Modrinth `NFkjnzWE` (`mc1.21.11-0.8.12-fabric`), download
  `https://cdn.modrinth.com/data/AANobbMI/versions/NFkjnzWE/sodium-fabric-0.8.12%2Bmc1.21.11.jar`
- file: `mods/sodium-fabric-0.8.12+mc1.21.11.jar`, 1,905,890 B
- SHA-1 `4d80592bef36674e7469ae2737a7704b826289ae`
- SHA-256 `009299748f192f6246fd6759a26304f0fea06f3412cff522d1d533e2b8db8176`

### Post-Fix · Fabric
`post-fix re-check: re-inspecting after Mod Doctor fix` → `inspect finished: 0 problem(s) []`, then
`Loading Minecraft 1.21.11 with Fabric Loader 0.19.5` → `Loading 55 mods:` including
`sodium 0.8.12+mc1.21.11`, `sodium-extra 0.9.1+mc1.21.11`, `reeses-sodium-options 2.2.3+mc1.21.11`,
`iris 1.10.7+mc1.21.11` → `Setting user: ALLQA001` → resource reload → `Sound engine started`, main menu.
No `Mod resolution failed`, no `NEG_HARD_DEP`. VMActivity was the resumed activity.

### Release APK (new artifact in force)
`…/build/outputs/apk/release/ALLLauncher-0.0.1-x86_64.apk` · 193,003,701 B · mtime 2026-09-19 02:27:49 ·
**SHA-256 `5F3B3C25F3A6E41F92D9E8516A3E7FCE7349745066B3ED77BADEE777A28D26B0`** · R8 + lintVital +
packageRelease all successful, signing unchanged · installed with `adb install -r` over `B9821D1C…`
(`lastUpdateTime` 2026-09-19 02:28:17, `firstInstallTime` 2026-09-17 19:30:54 unchanged).
`B9821D1C…` remains the binary the CurseForge-403 milestone was accepted on.

### Restore
Release mods dir back to `fabric-api-0.141.6+1.21.11.jar` — SHA-1 `c98467cb…` and SHA-256 `bdff7fd7…` identical
to the pre-run snapshot; no QA mod, no rejected/temp candidate, no `.disabled`/`.qa-backup`; my `/sdcard/qa1.xml`
removed. **Debug package untouched** (still `fabric-api`, `modmenu-17.0.0`, `placeholder-api-2.8.2+1.21.10`).

### Exact Next Action
`NEXT MILESTONE: Microsoft account login QA — sign-in through the launcher's account screen on this Release
(5F3B3C25…), then a launch that proves the Microsoft profile is used (no Offline account), then restore.`

## FOUR-MOD FULL CERTIFICATION — matrix complete, positive sweep NOT complete (ESCALATE_TO_TEAM)

Timestamp: `2026-09-19 02:56 +07:00`. Matrix: `test-artifacts\4mod-certification\matrix.csv` (+ `matrix-summary.md`,
raw provider dumps `modrinth-*.json`, `curseforge-*.json`, scripts `discover-inventory.ps1`, `build-matrix.ps1`,
`record-matrix-state.ps1`, `fetch-wrong-artifacts.ps1`). Release in force unchanged: **`5F3B3C25…`** (no source
change, therefore no rebuild this run).

### Loader families covered (from `game/addons/modloader/ModLoader.kt`, not assumed)
ALL supports `FORGE, NEOFORGE, FABRIC, LEGACY_FABRIC, BABRIC, QUILT, LITE_LOADER, CLEANROOM` (OptiFine is
vanilla-like). The four projects publish for exactly **fabric, quilt, neoforge**
(`loaders`/`gameVersions` from Modrinth and CurseForge). FORGE / LEGACY_FABRIC / BABRIC / LITE_LOADER / CLEANROOM
have no artifact for any of the four mods at any version → NO_SUPPORT; no substitution (Oculus/Embeddium) performed.

### Matrix
52 Minecraft versions discovered across the four projects (1.16.2 … 26.3, plus `1.19.3-pre*` / `22w4*a` snapshots for
Reese's). **105 cells**: SUPPORTED_ALL_4 **63**, PARTIAL_SUPPORT **38**, NO_SUPPORT **4** (the non-publishing loader
families). Per family: fabric 52 cells / 37 supported · neoforge 20 / 14 · quilt 29 / 12. Every cell is classified;
`result` is `PASS` only for the cell actually executed.

### Scenarios executed (device, Release `5F3B3C25…`, MC 1.21.11 / Fabric Loader 0.19.5) — cell `1.21.11|fabric`
A clean set PASS · B missing Sodium → one Fix → `sodium 0.8.12+mc1.21.11` PASS · C newest-candidate conflict
(0.8.14 and 0.8.13 rejected, 0.8.12 accepted) PASS · D unsatisfiable set (Extra 0.9.4 `>=0.8.14` + RSO 2.2.4
`=0.8.14` + Iris 1.10.7) → nothing installed, refusal with full constraint list, launch stayed paused PASS ·
E wrong Minecraft version (`sodium-extra-fabric-0.7.1+mc1.21.10.jar` → detected, disabled, replaced by the 1.21.11
build) PASS · F wrong loader (`iris-neoforge-1.10.7+mc1.21.11.jar` in a Fabric instance → detected, disabled,
replaced by `iris-fabric-1.10.7+mc1.21.11.jar`) PASS · H both compatibility directions PASS · I stable-first PASS.
G (duplicate mod id) and J (provider failure) NOT RUN.
Loader acceptance for the positive cases: Fabric Loader 0.19.5 loaded 55 mods including all four; no `NEG_HARD_DEP`,
no `Mod resolution failed`; main menu reached.

### Product bugs / fixes this run
None discovered, none fixed — the executed scenarios all behaved correctly. No regression run was needed for the
same reason (last regression state, still valid: `ModDoctorStableFirstCandidateTest` 5, `ModDoctorCandidateCompatibilityTest` 10,
`ModDoctorReplacementRepairTest` 9, `ModDoctorTest` 33, `ModFileDisableSemanticsTest` 2 — 0 failures, 0 errors).

### Why this is not PASS, and the blocker
PASS requires every SUPPORTED_ALL_4 cell to have run the positive scenarios **with a real loader launch**. 62 of the
63 supported cells are still `NOT_RUN`: each needs its own instance (Minecraft + loader download, instance creation
through the launcher) plus a launch that runs real mod resolution — roughly 5–20 minutes of device time per cell,
i.e. many hours on this single emulator, and the versions are spread over 52 Minecraft versions. This is a resourcing
problem, not a technical unknown: the code path being exercised is identical per cell, and the riskiest parts
(dependency repair, satisfiability, stable-first, wrong-loader/wrong-version refusal) are already certified on
`1.21.11|fabric`.

### Restore
Release mods dir back to `fabric-api-0.141.6+1.21.11.jar` — SHA-1 `c98467cb…`, SHA-256 `bdff7fd7…` — identical to the
pre-certification baseline; the QA fixtures and the two `.disabled` files created by the wrong-loader/wrong-version
scenario were removed; `/sdcard/w1.xml`, `/sdcard/w2.xml` removed. Debug package untouched.

### Exact Next Action
`DECISION NEEDED (user): either (a) run the remaining cert sweep as a team — each agent takes a slice of the 62
NOT_RUN SUPPORTED_ALL_4 cells from matrix.csv, creating its own instance and reporting per row; or (b) narrow the
certification target to a representative fixed set of cells (suggest: newest MC version per loader family, i.e.
1.21.11 fabric / 1.21.11 neoforge / newest quilt-supported version, plus the existing 1.21.11 fabric cell) and treat
the rest as classified-but-uncertified.`

## FOUR-MOD FULL CERTIFICATION — resumable runner built, Fabric sweep running (IN PROGRESS)

Timestamp: `2026-09-19 04:10 +07:00`. Matrix: `test-artifacts\4mod-certification\matrix.csv` (authoritative),
snapshot and how-to in `test-artifacts\4mod-certification\matrix-summary.md`, runner in
`test-artifacts\4mod-certification\runner\`. Source changed this run (see the defect below), so the release
in force on the device is still **`5F3B3C25…`** and does NOT contain the fix yet.

### What was built

A host-side, resumable certification runner that drives the installed launcher over adb. It reads
`matrix.csv`, skips cells already `PASS`, provisions the instance, runs the scenarios, writes the row
back immediately and continues, so an interruption loses at most the cell in flight.

`qa-lib.ps1` (adb, provider HTTP, UI dump/tap, log reading) · `qa-provision.ps1` (instance creation) ·
`qa-fixtures.ps1` (the four mod builds for a Minecraft+loader pair) · `qa-cell.ps1` (one cell) ·
`qa-sweep.ps1` (the loop) · `qa-neoforge.ps1` (NeoForge provisioning, written, NOT yet executed).

Key mechanism that made this cheap: an instance is provisioned by pushing two small JSON files
(the Mojang version json and the loader profile json, which already carries `inheritsFrom`). The
launcher resolves inheritance at launch (`VersionInfoParser.setInheriting`, `GameLauncher.kt:114-117`)
and downloads every missing game file itself (`MinecraftLaunchFlow` → `MinecraftDownloader`
VERIFY_AND_REPAIR). No Minecraft installation is built on the host. Proven on `1.21.10|fabric`:
`Loading Minecraft 1.21.10 with Fabric Loader 0.19.5`, 21 mods, main menu.

Scenarios driven per cell: A clean set · B missing core dependency + Mod Doctor repair · E wrong
Minecraft version · F wrong loader · G duplicate mod id; the candidate-walk letters C, D, H, I are
claimed only where the launcher's own log proves them, otherwise recorded NOT_APPLICABLE.

### Certified this run

`1.21.10|fabric` — PASS, scenarios A,B,E,G. Mod Doctor clean; missing-Sodium set repaired and the
loader accepted the repaired set; a Sodium build published for another Minecraft version refused;
a duplicate mod id detected. Fabric Loader 0.19.5 loaded all four mod ids and reached the main menu.
Evidence: `runner\state\1.21.10__fabric.evidence.txt`, row `matrix.csv`.
(`1.21.11|fabric` remains the earlier milestone's PASS.)

### Product defect found — Quilt refuses Fabric mods

`ModDoctor.kt:974` compared the jar's declared loader with the instance's loader for equality, so in a
Quilt instance every Fabric-authored mod was reported `WRONG_LOADER` and the launch was blocked, although
the providers declare those exact builds Quilt-compatible (Modrinth v2 returns Iris `1.10.6+1.21.11` with
`"loaders":["fabric","quilt"]` for `1.21.11`). Observed on the device as
`inspect finished: 4 problem(s) [WRONG_LOADER ×4]` at Minecraft 1.21.11 / Quilt Loader 0.20.0-beta.9.

Fixed on both sides of the same rule: `ModLoader.acceptsModLoader` (`ModDoctor.kt:147`) is now used by
the report filter (`:974`) AND the repair candidate filter (`isCompatibleWithVersion`, `:949-961`), so a
Quilt instance both accepts a Fabric mod and can propose a Fabric build as its repair — previously it
could do neither, and the repair side would have kept rejecting the only candidate that works.
A Quilt-native jar is still refused by a Fabric instance, and Forge/NeoForge mismatches are unchanged.
Tests: `ModDoctorTest.acceptsFabricModInQuiltInstance`, `ModDoctorTest.reportsQuiltModInFabricInstance`,
`ModDoctorReplacementRepairTest.fabricBuildIsAFixCandidateForAQuiltInstance`. The third is a genuine
RED→GREEN: without the repair-side change no candidate is left, so the problem stays unfixable.

### Regression state

`gradlew :ZalithLauncher:testDebugUnitTest --tests '…game.version.mod.*'` → **BUILD SUCCESSFUL**,
62 tests, 0 failures, 0 errors (`ModDoctorTest` 35, `ModDoctorCandidateCompatibilityTest` 10,
`ModDoctorReplacementRepairTest` 10, `ModDoctorStableFirstCandidateTest` 5, `ModFileDisableSemanticsTest` 2).

Edited files: `ModDoctor.kt` SHA-256 `CDEDB66D73B537743E3722F2A21B93AD3AA5BB0223850BD8A5F33B7F2E4B5CA1`,
`ModDoctorTest.kt` SHA-256 `60C7C7BD14C798D99FB116D5B08ACC3806630A2FD9292A2E662A7A4EA03A4645`,
`ModDoctorReplacementRepairTest.kt` SHA-256 `72B2DE6D59ECDBD01179633F8E3EE0A98F9BE4D5F92AB12F45766639375EBBC5`.
Nothing committed; the working tree still carries every earlier milestone's changes.

### Defect found in the runner itself, and fixed

The first Fabric sweep produced a run of BLOCKED cells that were the runner's fault, not the product's:
the set Mod Doctor accepts was saved under one tag shared by every cell, so a cell whose own repair
failed restored the PREVIOUS cell's mods — 1.21.10 builds installed into a 1.16.5 instance — and Mod
Doctor then reported the resulting `WRONG_MINECRAFT_VERSION` as if it were a product verdict. Fixed:
the saved set is tagged per cell and cleared at cell start, the removed file is looked up in the saved
set rather than assumed from the newest published build, and a cell that never reaches an accepted set
now records `B, E, F, G not run` instead of running them against whatever was left over. The reason
text Mod Doctor shows in its dialog is now captured into each cell's evidence as well.

### Cells that came back BLOCKED

Two causes, both recorded per cell in `notes`: (1) Modrinth carries no build of one project for that
Minecraft version although the cell is classified SUPPORTED_ALL_4 from the Modrinth+CurseForge union;
(2) the newest provider-tagged builds do not form a set Mod Doctor accepts and its repair cannot reach
one, while the refusal itself is proven (letters D/E). Cells seen so far: 1.16.3, 1.16.4, 1.16.5, 1.17,
1.17.1, 1.18 (Fabric) and 1.21.11 Quilt (the defect above).

### Product defect found — a wildcard Minecraft range is refused

Mod Doctor reports `WRONG_MINECRAFT_VERSION` for mods whose metadata declares a wildcard release line,
which is the ordinary Fabric form (`"minecraft": "1.16.x"`). Captured from the dialog on the device:
`Wrong Minecraft version: Reese's Sodium Options declares Minecraft 1.16.x, not 1.16.5.`
(Minecraft 1.16.5) and `Wrong Minecraft version: Iris declares Minecraft 1.17.x, not 1.17.`
(Minecraft 1.17). Both ranges cover the instance, so both are false positives.

This is why every older-Minecraft cell comes back BLOCKED with letters D/E: the newest published set is
refused, and the repair cannot propose a better candidate because every build of that project declares
the same range — the same predicate feeds both the report and the repair filter. NOT fixed; it is a
separate change in the Minecraft version-range matcher and was found after this run's fix was closed.
The affected cells are exactly the ones whose notes read `newest set rejected: WRONG_MINECRAFT_VERSION`.

SUPERSEDED (2026-09-19): this defect is FIXED. The Minecraft range check now delegates to the one shared
version evaluator, and the fix was device-confirmed on the Debug package, where the 1.17 cell no longer
reports `WRONG_MINECRAFT_VERSION`. The cells whose notes still read
`newest set rejected: WRONG_MINECRAFT_VERSION` keep that stale reason and must be re-run.

### Exact Next Action

`NEXT MILESTONE: finish the sweep. (1) Let the running Fabric sweep finish (terminal + runner\sweep-fabric.log).
(2) Re-fixture the BLOCKED older-Minecraft cells: resolve candidates by the Minecraft version named in a
build's version number, and fall back to CurseForge where Modrinth has no build, then re-run those cells
with -Force. (3) Validate runner\qa-neoforge.ps1 on one NeoForge cell, then sweep -Loader neoforge.
(4) Build ONE release carrying the Quilt fix, install with adb install -r, re-run -Loader quilt.
(5) Phase 13-18: full relevant tests, final release, final loader-parity sanity, team review, restore,
and replace this state record with the certification record.`

---

## Installed dependency version constraint detection — PASS (2026-09-19)

**Root cause.** The report pass verified only that an enabled mod with the declared dependency id exists
(`validateModDoctorMetadata`), and the declared version predicate was thrown away while parsing:
`readFabricMetadata` kept only the dependency id, and `ModDoctorMetadata` carried no version field at all.
A set could therefore pass Mod Doctor with 0 problems and still be refused by the real loader — observed on
the device as `iris ... {depends sodium @ [0.5.9 || 0.5.11]}` against an installed sodium 0.5.8.

**Semantic rule.** One shared decision, `versionPredicateDecision` (`ModDoctor.kt:1105`), fed by the one
shared `||` splitter `predicateAlternatives` and the one shared evaluator `fabricVersionMatches`
(`FabricModCompatibility.kt`). Any branch matching means matched; when nothing matches, the answer is
"unknown" if any branch could not be fully read, and "not matched" only when every branch was read. The
Minecraft-declaration path and the installed-dependency path now call the same function, so one constraint
cannot get two answers.

**OR wrapper.** `predicateAlternatives` (`FabricModCompatibility.kt:68`) and `fabricVersionPredicateMatches`
(`:76`): `0.5.9 || 0.5.11` matches 0.5.9 and 0.5.11, and does not match 0.5.8, 0.5.10 or 0.5.12. No string
substitution and no fuzzy comparison.

**Metadata carried.** `ModDoctorMetadata` gained `version` (the jar's own `version`, else `LocalMod.version`)
and `requiredDependencyPredicates` (dependency id -> the declared predicates, verbatim). Nested jar ids
(`providedModIds`) deliberately get no version: proving "installed" is all they can do, so a dependency
satisfied only by a nested id stays silent rather than borrowing the host jar's version.

**New detection.** `INCOMPATIBLE_DEPENDENCY_VERSION("Incompatible dependency version")`, appended to the
enum; nothing else switches exhaustively on the type. Reason: `<mod> requires <dependency> <constraint>,
but the installed version is <version>.` It carries no `requiredDependencyId`, so no Fix button appears and
no download is attempted; the report stays report-only.

**RED.** Two tests failed before the detection loop existed, on the assertion rather than on a compile
error: `installedVersionOutsideOrAlternativesIsReported` (0.5.8 against `0.5.9 || 0.5.11`) and
`installedVersionOutsideTheWildcardRangeIsReported` (0.8.9 against `0.9.x`). The other 82 passed.

**GREEN.** `gradlew :ZalithLauncher:testDebugUnitTest --tests 'com.movtery.zalithlauncher.game.version.mod.*'`
-> BUILD SUCCESSFUL, **84 tests, 0 failures, 0 errors** — ModDoctorTest 35, ModDoctorInstalledDependencyTest 12,
ModDoctorCandidateCompatibilityTest 10, ModDoctorReplacementRepairTest 10, ModDoctorMinecraftRangeTest 10,
ModDoctorStableFirstCandidateTest 5, ModFileDisableSemanticsTest 2. `git diff --check` exit 0; note that the
two production files are untracked, so that check does not cover them.

**Source files changed.** `ModDoctor.kt` (1215 lines, sha256 `91A52E6AA2767E8BDDA1F0C98570E3EF0F360978877111CBE98F873AD692C140`)
and `FabricModCompatibility.kt` (324 lines, `A6F2DDE1D58758D85F75F14692C52E18DEAD1999A84B5360B995B62AD628C2A9`);
tests `ModDoctorInstalledDependencyTest.kt` (209 lines, `9DAFECA4029A50AC7551492BA11C90A2A95DF2471CEA5AFC1E4AC291296792E0`)
and `ModDoctorMinecraftRangeTest.kt` (134 lines, `50F927677AC8F86BC5E07187AC4F496793AEE2620AF3C9A6BD7923EC417D8C7B`).
HEAD unchanged at `b4c9415b`; nothing committed.

**NOT implemented in this milestone.** Automatic replacement of an already-installed incompatible
dependency. That needs a separate product decision, because one shared dependency can be required by
several mods at once, and the loader is the final authority on whether a version really works. No provider
selection, candidate ordering, staged validation, loader alias or repair behaviour was touched, and no APK
was built and no device was used.

**Exact Next Action.** `NEXT MILESTONE: one Debug build carrying this change plus the wildcard and Quilt
fixes, then targeted matrix re-runs. (1) Build Debug from D:\ALL001 with JAVA_HOME=C:\jdk21,
:ZalithLauncher:assembleDebug -Darch=x86_64. (2) adb install -r over com.all.launcher.app.debug.
(3) Re-run the 14 cells whose notes say `newest set rejected: WRONG_MINECRAFT_VERSION`, the 3 cells that
FAIL with loader `HARD_DEP_NO_CANDIDATE` (1.20.5, 26.1, 26.1.1) and the 1.17 cell, which must now report
`INCOMPATIBLE_DEPENDENCY_VERSION` where it previously reported 0 problems. (4) If the 1.17 scenario B
anomaly still reproduces, investigate it then — it is unexplained, it is not this check, and it must not be
reported as a product defect until the enabled-set difference is pinned.`

## 4-MOD CERTIFICATION — targeted rerun on fresh Debug `3C51A21B…` — FAIL

Timestamp: `2026-09-19 11:52 +07:00`. Matrix: `test-artifacts\4mod-certification\matrix.csv`
(per-run evidence `runner\state\<cell>.evidence.txt`, logs `runner\rerun-targeted.log`,
`runner\recheck-wildcard-debug2.log`). Release package untouched; no Release build this run.

### Gate — PASSED
`ModDoctor.kt 91A52E6A…C140`, `FabricModCompatibility.kt A6F2DDE1…C2A9`, `ModDoctorInstalledDependencyTest.kt
9DAFECA4…92E0`, `ModDoctorMinecraftRangeTest.kt 50F92767…D8C7B` all match the just-tested revision.
`testDebugUnitTest --tests …game.version.mod.*` = **84 tests, 0 failures, 0 errors**. Debug APK
`3C51A21BABB89B758C60687CF982FB68BA693EE3237C4A185A79D4E1015FD8B7` (233,267,910 B, 11:13:03, package
`com.all.launcher.app.debug`, 0.0.1-debug) installed with `adb install -r`; device `base.apk` sha256 `3c51a21b…5fd8b7`
matches the built artifact. No uninstall, no data clear.

### Wildcard reruns — NOT unlocked (15 cells, all still BLOCKED)
The 14 cells whose notes named `WRONG_MINECRAFT_VERSION` plus explicit `1.16.5` and `1.17` reruns all reproduced the
same finding on the fresh build, e.g. `Wrong Minecraft version: Iris declares Minecraft 1.17.x, not 1.17.`
(`iris-mc1.17-1.1.1.jar`) and `… 1.21.x, not 1.21.` (`iris-fabric-1.8.8+mc1.21.1.jar`). The range fix does not cover a
predicate with more components than the instance version (`a.b.x` vs `a.b`).

### Dependency-version reruns — still FAIL (3 cells)
`1.20.5 fabric`, `26.1 fabric`, `26.1.1 fabric` → FAIL (A,E,G). Each shows `inspect finished: 0 problem(s) []`
immediately followed by the real loader refusing the same set on a sodium version constraint
(`replace [[sodium 0.5.8+mc1.20.6] -> add:sodium 0.5.11 ([[0.5.9,0.5.9],[0.5.11,0.5.11]])]`,
`replace [[sodium 0.8.9+mc26.1.1] -> add:sodium 0.9.1 ([[0.9.1,0.10-)])]`, `Incompatible mods found!`).
**No `INCOMPATIBLE_DEPENDENCY_VERSION` was reported in any of the three.** This is the certification rule violation:
a clean Mod Doctor verdict followed by a real loader resolution failure.

### 1.17 scenario B anomaly — neither reproduced nor resolved
The cell now stops at the `WRONG_MINECRAFT_VERSION` finding before scenario B executes, so the earlier
"sodium removal produced clean instead of a missing dependency" behaviour could not be re-observed on this artifact.

### Control
`1.21.11 fabric` → **PASS (A,E,G)** on the fresh build; no regression introduced by these three changes.

### Matrix after this run
PASS 13 · FAIL 3 · BLOCKED 22 · REMAINING 67 (105 cells). Per loader — fabric: 13/3/21/15 ·
quilt: 0/0/1/28 · neoforge: 0/0/0/20 · legacy_fabric, babric, liteloader, cleanroom: 1 REMAINING each (NO_SUPPORT rows).
The sweep was stopped here per the run's own rule (stop on the next genuine product FAIL); QA instances for the
certified cells remain on the device by design so the sweep stays resumable.

### Exact Next Action
`FIX (Debug, then rerun only the affected cells): (1) make the Minecraft range/wildcard matcher accept "a.b.x" for an
a.b instance version (15 BLOCKED cells), and (2) make the installed-dependency version-constraint check report
INCOMPATIBLE_DEPENDENCY_VERSION when an installed companion's declared range excludes the installed dependency version`

## 4-MOD CERTIFICATION — targeted Debug `1.17|fabric` rerun — FAIL / wildcard unlocked

Timestamp: `2026-09-19 13:30 +07:00`. This was one focused rerun with `QA_PACKAGE_SUFFIX=.debug`; no source,
APK, package identity, signing configuration, or Debug data was changed. The runner wrote the direct evidence to
`test-artifacts\\4mod-certification\\runner\\state\\1.17__fabric.evidence.txt` and updated the matrix row.

### Evidence
- Installed artifact was explicitly `com.all.launcher.app.debug` with SHA-256
  `3c51a21babb89b758c60687cf982fb68ba693ee3237c4a185a79d4e1015fd8b7`, matching the Debug APK built from the
  current source. This corrects the earlier evidence-boundary problem where the row was recorded against Release.
- Scenario A (newest published compatible set): **clean**. The `a.b.x` wildcard declaration is accepted for the
  `a.b` instance (`1.17.x` on Minecraft `1.17`); the wildcard defect is therefore unlocked by direct device evidence.
- Scenario E (wrong-version/dependency fixture): **WRONG_MINECRAFT_VERSION + INCOMPATIBLE_DEPENDENCY_VERSION**
  findings were produced; the installed dependency constraint check is active on this Debug artifact.
- Scenario G: **DUPLICATE** detected. Loader ended with all four expected mod IDs present.
- Cell result remains **FAIL** because Scenario B (core dependency removed) produced a clean Mod Doctor verdict
  instead of the expected missing-dependency finding. Scenario F was NOT_APPLICABLE for this cell.

### Result
The 1.17 Debug rerun proves the wildcard matcher is no longer the current blocker and provides positive device
proof for installed dependency-version detection. Matrix state after the run is `PASS 13 / FAIL 4 / BLOCKED 21 /
remaining 67`; the row `1.17|fabric` now records the Debug artifact hash and `FAIL`.
No source files changed in this micro-run; only the runner's matrix/evidence state changed.

### Exact Next Action
Run only the three dependency-version cells (`1.20.5|fabric`, `26.1|fabric`, `26.1.1|fabric`) against the same
installed Debug artifact. Stop at the first genuine FAIL; do not rerun wildcard cells or investigate the separate
1.17 Scenario-B missing-dependency anomaly in that run.

## 4-MOD CERTIFICATION — 1.17|fabric Scenario B root cause, fix, and device re-check — PASS (root cause proven)

Timestamp: `2026-09-19 14:05 +07:00`. Evidence directory
`test-artifacts\4mod-certification\1.17-scenario-b-rootcause\` (`00-ROOTCAUSE.md`, `01-FIX-AND-DEVICE-EVIDENCE.md`);
runner logs `runner\red-stub-provided-version.log`, `runner\green-bundled-copy-version.log`,
`runner\debug-build-bundled-copy.log`, `runner\scenario-b-1.17-fixed.log`,
`runner\state\1.17__fabric.scenarioB.evidence.txt`.

### Root cause — proven, with artifacts
`iris-mc1.17-1.1.1.jar` contains a nested jar whose `fabric.mod.json` declares `id = "sodium"`,
`version = "0.3.0+IRIS2"`. `ModDoctor.kt` collects every nested id into `providedModIds`
(`readNestedFabricIds`, line 642), `validateModDoctorMetadata` puts them into `installedIds`
(lines 994–997), so with the top-level sodium file deleted the id still read as installed and the
missing rule stayed silent; because provided ids carried no version, the version rule hit
`installedVersions[dependency] ?: return@forEach` (line 1087) and stayed silent too ⇒ `0 problem(s)`,
while the real loader refuses the same set. The pure-logic JVM test disagreed because it builds the
iris metadata by hand with no provided ids
(`ModDoctorInstalledDependencyTest.removedCompanionIsReportedAsAMissingDependency`).

### Fix — smallest correction
`ModDoctorMetadata.providedModVersions` (nested jar's own declared version, never borrowed from the
outer file); `readNestedFabricIds` → `readNestedFabricMods` returning id+version; `validateModDoctorMetadata`
registers provided versions after the mods' own ids (top-level file still wins for its id; blank or
placeholder versions keep the rule silent). Missing-rule semantics unchanged — provided ids still count
as installed, exactly as the loader counts nested jars. `ModDoctor.kt` sha256 `C1462AA1…F2125CE`,
`ModDoctorInstalledDependencyTest.kt` sha256 `1B3A3DBA…C46B8E7F`; `git diff --check` exit 0.

### Tests
RED `87 tests completed, 1 failed` (`aBundledCopyOfTheDependencyDoesNotSilenceTheVersionMismatch`) →
GREEN `tests=87 failures=0 errors=0 skipped=0` for
`--tests "com.movtery.zalithlauncher.game.version.mod.*"`.

### Debug artifact
Built from `D:\ALL001` via `cmd.exe /d /c "cd /d D:\ALL001 && gradlew.bat :ZalithLauncher:assembleDebug
-Darch=x86_64 --no-daemon --console=plain"` with `JAVA_HOME=C:\jdk21`: `BUILD SUCCESSFUL in 1m 14s`,
NDK configure+build executed, no `CXX1429`. APK 233,267,910 B, mtime `13:55:59`, sha256
`667D7FE7F94E645EA7760775076A943EEE211EB71CCADBD881D2C7D09D96AC78`; `adb install -r` → Success, device
`base.apk` sha256 matches, UID 10066 and `firstInstallTime 2026-09-16 12:33:37` unchanged.
Building the same thing from MSYS bash (`cd D:/ALL001`) resolves the junction to the spaced real path
and reproduces the NDK `APP_BUILD_SCRIPT … unknown file` failure — the cmd.exe form is the working one.

### Scenario B on the real device path (Debug `667d7fe7`)
Sodium file deleted, 4 files left: `[13:57:01.375] inspect finished: 1 problem(s) [INCOMPATIBLE_DEPENDENCY_VERSION]`;
dialog `Incompatible dependency version: Reese's Sodium Options requires sodium >=0.3.3, but the installed
version is 0.3.0+IRIS2. File: reeses_sodium_options-mc1.17.1-1.2.3.jar`; buttons Cancel + Launch Anyway only
(Cancel pressed, Minecraft not launched). The `0.3.0+IRIS2` comes from inside the real Iris jar, so the
reader change is proven end-to-end on device, not only in the unit rule.

### Open decision (runner/matrix, not product)
Scenario B's expected type `MISSING_REQUIRED_DEPENDENCY` is unreachable for this fixture set: Iris ships its
own sodium copy and the loader counts nested jars as mods, so removing the top-level file does not remove the
`sodium` id. Deciding whether this cell's B expectation becomes "MISSING **or** INCOMPATIBLE_DEPENDENCY_VERSION"
(or the cell picks a fixture set with no bundled copy) is a certification-policy decision and was not made here.
Matrix row `1.17|fabric` was deliberately left untouched (its `artifact_hash` still names `3c51a21b…`; the
installed artifact is now `667d7fe7…`). No Release build, no signing/package/identity change, no data clear.

### Exact Next Action
`RUN ONLY the three dependency-version cells 1.20.5|fabric, 26.1|fabric, 26.1.1|fabric against the installed
Debug 667d7fe7…, then re-run the full 1.17|fabric cell once with the Scenario-B expectation decided. Stop at the
first genuine FAIL.`

## 4-MOD CERTIFICATION — Scenario B semantics corrected, dependency-version cells re-checked, 1.17 re-run — FAIL (new native crash)

Timestamp: `2026-09-19 14:15 +07:00`. No product source was changed in this run (the Scenario B result is
correct as it stands); Debug artifact unchanged at `667D7FE7…96AC78`; no Release build; the Fabric sweep was
**not** resumed because the 1.17 full cell did not pass.

### QA runner correction — Scenario B is now metadata-derived
`qa-fixtures.ps1`: `Get-JarProvidedMods` (a jar's own `fabric.mod.json` id plus every nested jar's id and
declared version) and `Get-RemovedDependencyFacts` (dependency id read from the removed jar, providers scanned
in the accepted set actually on the device). `qa-cell.ps1` scenario B, now "external dependency removal /
remaining-provider validation": nothing supplies the removed id → `MISSING_REQUIRED_DEPENDENCY` (existing repair
walk unchanged); a remaining provider → Mod Doctor must judge it (dependency problem type + blocked launch) or
stay clean and let the real loader reach the main menu; a clean verdict followed by a loader refusal stays FAIL.
No project name is hard-coded. Evidence records `B.removed` and `B.remainingProviders`.
Superseded record preserved: `test-artifacts\4mod-certification\1.17-scenario-b-rootcause\02-HISTORICAL-FAIL-RECORD.md`
(the per-cell evidence file name is fixed, so the pre-correction text was overwritten — the quotes live there).

### Dependency-version cells — product behaviour correct, rows BLOCKED (not FAIL)
Runner log `runner\rerun-dependency-version-cells.log`; rows updated by the runner itself.
`1.20.5`, `26.1`, `26.1.1` all report `INCOMPATIBLE_DEPENDENCY_VERSION` twice for the newest published set,
offer no Fix (`Mod Doctor repair unavailable: no Fix button offered`) and block the launch (no loader run).
Mechanisms from the fixture metadata: iris 1.7.2 requires `sodium 0.5.9 || 0.5.11` vs newest `0.5.8+mc1.20.6`;
iris 1.11.4 requires `sodium 0.9.x` vs installed `0.8.9+mc26.1.1`.
The earlier FAIL record for these three was measured against the **Release** package (their evidence headers read
`Launcher Version: 0.0.1`; Debug reads `0.0.1-debug`) — an evidence-boundary error, not a product defect. Their
rows now read BLOCKED with the Debug artifact hash. No coherent set exists for those cells' newest artifacts.

### 1.17|fabric — scenario B certifies, cell FAILs on a new native crash
Runner log `runner\cell-1.17-full.log`; row `scenario = A,B,E,G`, artifact `667D7FE7…96AC78`, notes now also carry
the crash. Scenario B: removed `sodium-fabric-mc1.17.1-0.3.4+build.13.jar` (mod id `sodium`), remaining provider
`iris-mc1.17-1.1.1.jar (nested 0.3.0+IRIS2)` → `INCOMPATIBLE_DEPENDENCY_VERSION`, launch blocked ⇒ the corrected
expectation is met. The result is **FAIL** for a new, previously unreported reason: scenario A's loader run ends
`GAME_ENDED` with a native `SIGSEGV` in `libpojavexec.so` (`pojavGetCurrentContext+0x37`) after
`Loading 53 mods` and `Setting user: ALLQA001`, never reaching `Sound engine started`; Mod Doctor was clean for
that accepted set, so it is a launch/runtime issue, not mod logic.

### Matrix state
Whole matrix: PASS 13, FAIL 1, BLOCKED 24, NOT_RUN 67. fabric 13 PASS / 1 FAIL / 23 BLOCKED / 15 not run;
quilt 0/0/1/28; neoforge all 20 not run. Every PASS row reached the main menu; the only FAIL row is `1.17|fabric`.
`matrix-summary.md` carries the same section.

### Exact Next Action
`ATTRIBUTE THE 1.17 NATIVE CRASH as its own micro-run, QA only, no product source change: provision the same
1.17 Fabric 0.19.5 instance twice - once with the four mods and once with an empty mods directory - and capture
the game log each time; if both crash with SIGSEGV in libpojavexec.so, the crash is old-Minecraft-on-this-launcher,
not the four mods. Then resume the remaining Fabric sweep (qa-sweep.ps1 -Loader fabric, 23 BLOCKED cells).`

## 1.17 native crash — differential (empty mods vs four mods) — CASE B: fixture-dependent — BLOCKED (unresolved)

Timestamp: `2026-09-19 14:25 +07:00`. QA only: no product source changed, no APK built, no matrix row touched,
sweep not resumed. Artifact Debug `667D7FE7…96AC78` (versionName `0.0.1-debug`, UID 10066, data untouched).
Evidence: `test-artifacts\4mod-certification\1.17-native-crash\` (`00-COMPARISON.md`, `empty-mods.log`,
`four-mods.log`, `four-mods-repeat.log`, `fixture-hashes.txt`); runner scripts
`runner\qa-1.17-crash-isolation.ps1`, `runner\qa-1.17-four-mods-repeat.ps1`;
`runner\1.17-crash-isolation.log`, `runner\1.17-four-mods-repeat.log`.

### Runs
| launch | instance dir | result |
|---|---|---|
| four mods (cell scenario A, 14:09) | fresh | **CRASH** — `GAME_ENDED`, 53 mods loaded, `SIGSEGV` at `libpojavexec.so+0x9b47 pojavGetCurrentContext+0x37`, never `Sound engine started` |
| empty mods (14:16) | fresh | **PASS** — Fabric reported only its own pseudo mods, `Sound engine started`, atlases created, main menu |
| four mods (14:18, same dir as the empty run) | reused | not usable — `Wait-LoaderOutcome` read the empty run's still-present `latest_game.log` and matched its `Sound engine started`; kept in the evidence as a stale-read example |
| four mods, the one allowed repeat (14:19) | fresh | **CRASH** — same frame, 53 mods, `VMActivity` 0 |

### Same runtime/settings — verified
Runs 2 and 4 report identical `Launcher version 0.0.1-debug`, `Renderer: Krypton Wrapper`, `Java Runtime: Internal-17`,
`Custom Java arguments: NONE`, `1.17 Fabric 0.19.5`, same game path, account `ALLQA001`; the JVM argument list is
byte-identical (86 lines compared) and the only env-map difference is the per-process `DALVIK_APPLICATION` handle.
The four-mod fixture was restored from `compat-1.17-fabric` and its sha1 set verified identical
(`fixture-hashes.txt`); Mod Doctor reported `clean` every time, so no repair altered the fixture.

### Conclusion
`EMPTY_MODS_PASS` + `FOUR_MOD_CRASH` ⇒ the crash depends on the four-mod fixture (CASE B). No single mod is named;
per the run's rule the differential is recorded and the investigation stops here. The 1.17 cell stays unresolved:
Mod Doctor is correct (scenario B certifies) but the runtime never reaches the main menu with the fixture in place.

### QA lesson worth keeping
`Wait-LoaderOutcome` reads `versions/<instance>/ALLLauncher/latest_game.log`, which the game only truncates when it
actually starts — two launches sharing one instance directory can read the previous launch's verdict. Provision a
fresh instance directory per launch when comparing runs (recorded in the `android-launcher-debugging` skill).

### Exact Next Action
`ISOLATE WHICH MOD/COMPONENT CAUSES THE 1.17 NATIVE CRASH, QA only, one variable per launch on a freshly
provisioned 1.17 Fabric 0.19.5 instance, same artifact: start from fabric-api alone, then add sodium, then
sodium-extra and reeses-sodium-options; the first set that crashes names the component. Then resume the Fabric
sweep (qa-sweep.ps1 -Loader fabric, 23 BLOCKED cells).`

## 1.17 native crash — cumulative mod isolation — boundary = Iris + external Sodium (BLOCKED, unresolved)

Timestamp: `2026-09-19 14:52 +07:00`. QA only: no product source changed, no APK built, no matrix row touched,
Fabric sweep not resumed. Artifact Debug `667D7FE7…96AC78`. Evidence:
`test-artifacts\4mod-certification\1.17-native-crash\isolation\` (`00-ISOLATION-SUMMARY.md`, per-config logs
`C1-…` … `IB-…`, `isolation-results.txt`); script `runner\qa-1.17-isolation.ps1`, log `runner\1.17-isolation.log`.

### Results (every launch on its own freshly provisioned instance; `game log existed before this launch: no` in all eight)
| configuration | mods | Mod Doctor | runtime | native crash | main menu |
|---|---|---|---|---|---|
| C1 fabric-api | 1 | clean | MAIN_MENU | no | yes |
| C2 + sodium | 2 | clean | MAIN_MENU | no | yes |
| C3 + sodium-extra | 3 | clean | MAIN_MENU | no | yes |
| C4 + reeses-sodium-options | 4 | clean | MAIN_MENU | no | yes |
| C5 + iris (full certification set) | 5 | clean | GAME_ENDED | **yes** | no |
| C5-confirm (one repeat) | 5 | clean | GAME_ENDED | **yes** | no |
| IA fabric-api + iris | 2 | clean | MAIN_MENU | no | yes |
| IB fabric-api + sodium + iris | 3 | clean | GAME_ENDED | **yes** | no |

First PASS→CRASH boundary: adding **Iris 1.1.1** to the passing four-mod set (C4 → C5), confirmed once.
Iris tree: `IA` PASS, `IB` CRASH ⇒ **the interaction with the external sodium 0.3.4+build.13 is implicated**
(Iris alone is not sufficient; sodium-extra and reeses are not required for the crash). Mod Doctor was clean in
all eight launches, so nothing was BLOCKED_BY_MOD_DOCTOR and no fixture was altered.

### Native signature (identical in the four crashing launches)
`SIGSEGV (0xb) at pc=0x0000000000000000` → `# C  [libpojavexec.so+0x9b47]  pojavGetCurrentContext+0x37`,
after `Setting user: ALLQA001`; `VMActivity` 0 afterwards. The JVM's `hs_err_pid*.log` is removed together with
the instance by the runner, so only the fatal-error block inside the game log is kept.

### Not established (deliberately not claimed)
Whether the cause is Iris itself, Iris's native/render interaction, Iris's bundled nested `sodium 0.3.0+IRIS2`
duplicating the external `sodium` id, or a conflict between the bundled copy and the external one.

### Exact Next Action
`NARROW THE IRIS + EXTERNAL-SODIUM PAIR, QA only, exact fixture, no product source change: first make the QA
script pull the instance's hs_err_pid*.log before the instance is removed, then read the last game-log lines
before the SIGSEGV in IB/C5 against the same point in C4 to see which subsystem is initialising when the native
frame faults, and design the minimal delta test from that evidence. Then resume the Fabric sweep.`

## 1.17 native crash — deep isolation (hs_err, alternate Sodium, alternate renderer) — mechanism narrowed

Timestamp: `2026-09-19 15:12 +07:00`. QA only: no product source changed, no APK built, no matrix row touched,
sweep not resumed. Artifact Debug `667D7FE7…96AC78`. Evidence:
`test-artifacts\4mod-certification\1.17-native-crash\deep-isolation\` (`00-DEEP-ISOLATION.md`, per-run dirs
`R1-pass-control`, `R2-crash-control`, `R3-alt-sodium-033`, `R4-alt-renderer-gl4es`, each with `meta.txt`,
`game.log`, `launcher.log`, `logcat.txt` and the instance's own `hs_err_pid*.log` pulled before removal);
scripts `runner\qa-1.17-deep-isolation.ps1`, `runner\qa-1.17-alt-renderer.ps1`.

### Results
| configuration | resolved sodium (Fabric's own tree) | runtime | hs_err | result |
|---|---|---|---|---|
| R1 fabric-api + iris | `sodium 0.3.0+IRIS2` (Iris's bundled copy is active; Sodium config = 28 options) | `MAIN_MENU` | none | PASS_MAIN_MENU |
| R2 + sodium 0.3.4+build.13 | `sodium 0.3.4+build.13` (external wins the duplicate id; 29 options) | `GAME_ENDED` | pulled | CRASH_NATIVE |
| R3 + sodium 0.3.3+build.8 (previous genuine 1.17.1 build, provider sha1 verified) | `sodium 0.3.3+build.8` | `GAME_ENDED` | pulled | CRASH_NATIVE |
| R4 same as R2 with renderer pinned to GL4ES (`Renderer: GL4ES`, `POJAV_RENDERER = opengles2`) | `sodium 0.3.4+build.13` | `GAME_ENDED` | pulled | CRASH_NATIVE |

### hs_err key finding (identical in R2/R3/R4)
`SIGSEGV (0xb) at pc=0x0000000000000000` with `Render thread [_thread_in_native]`, frames
`libpojavexec.so+0x9b47 pojavGetCurrentContext+0x37` ← `JNI.invokeP` ← `GLFW.glfwGetCurrentContext` ←
`GLX.getOpenGLVersionString` ← `RenderSystem.getApiDescription` ← the loading screen's system-info line ←
`Main.main`. The launcher's GL-context shim jumps through a NULL pointer while the loading screen queries the GL
version. Last PASS-vs-CRASH divergence: PASS runs continue through `Input ready`, `GLFW: Set size`, OpenAL and
`Sound engine started`; CRASH runs stop right after `Saving JVM environ` / `Registered forkAndExec` /
`MioLibPatcher: Patch target class: org/lwjgl/system/Library`.

### Mechanism
Iris 1.1.1 ships a **complete Sodium build** (`sodium-fabric-mc1.17.1-0.3.0+IRIS1-build.79.jar`, id `sodium`,
version `0.3.0+IRIS2`, 322 classes, entrypoint `SodiumClientMod`) and Modrinth lists **no dependencies** for
`iris-mc1.17-v1.1.1`. With any external Sodium jar present, Fabric resolves the duplicate `sodium` id to the
external build, Iris's own Sodium is dropped, and Iris's mixins meet a Sodium build it was not built against —
crashing in the early GL-context query. Two external Sodium builds crash; the empty-mod and no-external-Sodium
configurations all pass. The failure is renderer-path related but **not** renderer-specific (Krypton and GL4ES
both crash).

### Certification implication (fixture, not runtime)
The cell's "newest published build of each project" rule pairs Iris 1.1.1 with `sodium 0.3.4+build.13`, a
configuration that never existed (Iris 1.1.x expected its own bundled Sodium). That makes the 1.17 cell's
**fixture construction** the first suspect, not the launcher runtime. 1.17 stays unresolved: no product source
change and no matrix change were made.

### Exact Next Action
`DECIDE AND APPLY THE CERTIFICATION FIXTURE RULE for cells whose project ships its own copy of another project's
id (evidence: iris-mc1.17-1.1.1 bundles Sodium 0.3.0+IRIS2 and declares no dependency): have the runner keep the
bundled build as the Sodium provider for that cell instead of adding an external Sodium, then re-run the 1.17
cell. Product source stays untouched; only then consider the launcher's GL-context path if the bundled-copy
configuration still crashes.`

## 1.17 fixture resolution — valid combination found, cell PASSES

Timestamp: `2026-09-19 16:20 +07:00`. QA only: no product source changed, no APK built. Artifact Debug
`667D7FE7…96AC78`. Evidence: `test-artifacts\4mod-certification\1.17-fixture-resolution\`
(`00-FIXTURE-RESOLUTION.md`, `S1\`, `S2\`, `candidate-results.txt`); scripts `runner\qa-1.17-fixture-resolution.ps1`,
logs `runner\1.17-fixture-resolution.log`, `runner\cell-1.17-with-new-rule.log`. Crash history preserved under
`1.17-native-crash\` as superseded evidence.

### The answer: a valid 1.17 four-project combination exists (no external Sodium)
* Iris 1.1.1 bundles a **complete** Sodium (`sodium-fabric-mc1.17.1-0.3.0+IRIS1-build.79.jar`, id `sodium`,
  version `0.3.0+IRIS2`) and declares no Sodium dependency; Modrinth's record for `iris-mc1.17-v1.1.1` also lists
  none. An external Sodium wins the duplicate id and crashes (proven on 0.3.4 and 0.3.3, Krypton and GL4ES).
* Provider metadata pruned the companions: RSO `1.2.3`+ need `sodium >=0.3.3` (and `1.3.0`+ declare
  `breaks iris <1.1.4`); Sodium Extra `0.4.4`+ declare `breaks reeses-sodium-options <1.4.2`; Sodium Extra
  `0.4.3`+ exist only for 1.17.1 (candidate `S1` was rejected on device with `WRONG_MINECRAFT_VERSION`).
* Valid set: **fabric-api 0.36.0+1.17 + iris 1.1.1 (bundled Sodium provider) + sodium-extra 0.3.6 +
  reeses-sodium_options 1.2.2**. On device: Mod Doctor clean, loader graph `iris 1.1.1 → \-- sodium 0.3.0+IRIS2`,
  `sodium-extra 0.3.6`, `reeses-sodium-options 1.2.2`, **main menu**, 52 mods, VMActivity 7 (`S2\meta.txt`).

### Runner fixture rule (QA tooling only, generic)
`qa-fixtures.ps1`: `Resolve-ValidFixtureSet` (+ `ConvertTo-QaPredicateMap`, `Compare-QaVersion`,
`Test-QaVersionPredicate`, extended `Get-JarProvidedMods` with depends/breaks). It resolves the newest build per
project, lets a nested copy act as the provider of its id and drops the external duplicate, evaluates declared
`depends`/`breaks` against the resolved provider versions, walks a conflicting project's versions older
(release builds first, max 8), and returns `Valid = $false` when no graph works. `qa-cell.ps1` records
`UNSATISFIABLE` + `NO_VALID_COMBINATION` in that case, treats scenario B and G as `NOT_APPLICABLE`/re-targeted
when the dependency's provider is nested, and counts nested mod ids in `Read-LoadedMods`. `qa-matrix.ps1`
treats `UNSATISFIABLE` as finished so the sweep does not retry it. No project name is hard-coded.

### Matrix
`1.17|fabric` = **PASS (A,E,G)**, artifact `667d7fe7…96AC78`, notes carry the nested-provider and version-walk
reasoning. The Fabric sweep was resumed right after (`runner\sweep-fabric-resume.log`, `qa-sweep.ps1 -Loader fabric`).

### Fabric sweep — COMPLETE (16:02 → 17:15, 23/23 pending cells, same Debug artifact `667d7fe7…96AC78`)
Log `runner\sweep-fabric-resume.log`. Final `matrix.csv` counts for `SUPPORTED_ALL_4` (37 cells):
**PASS 31 · FAIL 1 · BLOCKED 4 · UNSATISFIABLE 1**.

* PASS now includes the cells that earlier looked like product failures — `1.20.5`, `26.1`, `26.1.1` pass with
  `A,B,E,G` once the fixture is resolved by metadata predicates instead of "newest jar of each project".
* `1.21.2|fabric` = **FAIL**: scenario A loaded 59 mods, then Minecraft **crashed** (`#@!@# Game crashed! Crash report
  saved to: …/versions/1.21.2 Fabric 0.19.5/crash-reports/`, no `Sound engine started`, no SIGSEGV). The selected jars
  are all `…+mc1.21.3` builds (`sodium-fabric-0.6.1+mc1.21.3`, `iris-fabric-1.8.0+mc1.21.3`,
  `reeses-sodium-options-fabric-1.8.0+mc1.21.3`, `sodium-extra-fabric-0.6.0+mc1.21.3`) → the cell has **no
  1.21.2-targeted build** and the rule fell back to 1.21.3 jars. Classification (fixture availability vs product
  defect) still open; evidence `runner\state\1.21.2__fabric.evidence.txt`.
* `1.19.2|fabric` = UNSATISFIABLE but **provisional — runner gap**: the recorded reason is
  `sodium-extra requires fabric, which no selected jar provides`. The legacy id `fabric` is the `fabric-api` project
  under an old id, which the rule does not alias, so it walked Sodium instead and concluded no combination exists.
  Re-run this cell after adding the alias.
* BLOCKED 4: `1.16.3`, `1.16.4` (no RSO build published — genuine availability gap); `26.2`, `26.3`
  (runner error `launch button not found on the home screen`).
* Remote observability: cron job `ALL Launcher Fabric sweep watch` (#f3a1fd3495d6, every 3m, monitor-gated on
  `~/AppData/Local/hermes/scripts/all_launcher_sweep_watch.py`, delivers to the Telegram chat) emitted per-cell state
  and was paused when the sweep finished.

### Runner fixture rule — extension for pinned dependencies (QA only, 2026-09-19 17:45)
`qa-fixtures.ps1` step 2.5: every resolved fixture now carries `ProjectId` + `Deps` (Modrinth version
metadata) and a required dependency that names **one exact build** (`version_id`) sets that build as the
provider of its project, provided the pinned build declares the cell's Minecraft version and loader
(`Get-QaVersionById`, cached). Note form: `pin: iris requires sodium exactly mc1.21.3-0.6.0-fabric; sodium
moved mc1.21.3-0.6.13-fabric -> mc1.21.3-0.6.0-fabric`. Why: a project can be compiled against one exact
build of another project, and the loader accepts a newer one while the mixins then fail at runtime — that is
what made `1.21.2|fabric` look like a product defect. Verified with `runner\qa-fixtures-probe.ps1`
(resolver-only, no device) for `1.21.2` (pinned set, Valid), `1.17` (unchanged) and `1.21.11` (still Valid,
but now a different, older set — see the limitation note in `matrix-summary.md`).

### 1.21.2|fabric — FAIL reclassified to **PASS (A,B,E,G)**
Old fixture: Sodium `0.6.1` + Iris `1.8.0` + Sodium Extra `0.6.0` + Reese's `1.8.3` → Minecraft crashed while
Iris applied `mixins.iris.compat.sodium.json:MixinCloudRenderer` (`@Shadow field cachedGeometry was not
located in the target class net.caffeinemc.mods.sodium.client.render.…`) — an invalid artifact pairing, not a
launcher defect. New metadata-valid set (Sodium pinned to `0.6.0` by Iris, Reese's walked `1.8.3` → `1.8.0`,
Sodium Extra `0.6.0`, Fabric API `0.106.1+1.21.2`) reaches **MAIN_MENU** with `Sound engine started` and
`Game crashed` 0. Evidence: `test-artifacts\4mod-certification\1.21.2-fixture-resolution\`
(`00-CLASSIFICATION.md`, `01-device-evidence-pinned-set.txt`, `02-cell-run.log`, `03-A-gamelog-main-menu.txt`),
log `runner\cell-1.21.2-pinned.log`.

### QA cleanup run — 1.19.2 alias + 1.16.3/1.16.4 availability + PRODUCT DEFECT (2026-09-19 18:10)
QA only: no product source changed, no APK built.

* `qa-fixtures.ps1` gained a generic id-alias table `$script:QaModIdAliases = @{ 'fabric' = 'fabric-api' }`
  (a dependency written against a project's legacy id is satisfied by the project's current id; applied to the
  provider map in resolver step 3) and the missing-dependency walk now targets the **declaring** project
  instead of `$set[0]`. Probe (`runner\qa-fixtures-probe.ps1`) after the change: `1.19.2` Valid with
  `sodium 0.4.4, sodium-extra 0.4.16, Reese's 1.6.4, iris 1.6.11+1.19.2, fabric-api 0.76.0+1.19.2`;
  `1.21.2`, `1.17`, `1.21.11` unchanged.
* `1.19.2|fabric` re-run (`-Force -MaxCells 1`, log `runner\cell-1.19.2-alias.log`): the earlier
  `UNSATISFIABLE` is gone, but the cell now blocks on **Mod Doctor**: `MISSING_REQUIRED_DEPENDENCY` —
  "Sodium Extra requires fabric, but no enabled installation was found", no Fix offered, scenario D.
  **Product defect**: the installed `fabric-api-0.76.0+1.19.2.jar` declares `provides: ["fabric"]`, the real
  loader resolves that id, and `ModDoctor.kt` never reads `provides` (0 occurrences; only the jar's own id and
  nested jar-in-jar ids). The dependency-project lookup also uses the raw id (`fabric` not found on Modrinth).
  Evidence `1.19.2-provides-defect\00-DEFECT-REPORT.md`. Row: `BLOCKED` (scenario D); the previous
  `UNSATISFIABLE` verdict is superseded. **Run stopped here as instructed — 26.2/26.3 not investigated.**
* `1.16.3` / `1.16.4` verified as availability-blocked: `reeses-sodium-options` has 0 builds for 1.16.3 and
  1.16.4 (any loader); its oldest published Minecraft version is 1.16.5 (`1.19.2-provides-defect\03-…md`).
  No neighbouring-version artifact is substituted.
* Fabric now: **PASS 32 · FAIL 0 · BLOCKED 5 · UNSATISFIABLE 0** (blocked: 1.16.3, 1.16.4 availability;
  1.19.2 product defect; 26.2, 26.3 runner launch-button).

### PRODUCT FIX — Mod Doctor now honours top-level `provides` (2026-09-19 19:05), 1.19.2 PASS
Source (authorized): `ModDoctor.kt` `readFabricMetadata` reads `provides` and merges those ids into
`providedModIds` (`(embeddedMods.map { it.first } + declaredProvides).distinct()`); `toDoctorMetadata` is
`internal` so the regression drives the same parse path; the model comment for `providedModIds` now names both
sources. No version is invented for an alias (version rules keep "unreadable = stay silent").
Test: new `ModDoctorProvidedModIdTest` (real jars via `AllModReader` → `toDoctorMetadata` →
`validateModDoctorMetadata`) — RED `3 tests completed, 1 failed` (`runner\red-provides2.log`), GREEN
**90 tests, 0 failures, 0 errors over all 8 `game.version.mod` classes** (`runner\green-provides-full.log`), `git diff --check` clean.
Build: Debug `730d4737436b12cdd18270b86e5a3732d24b8acfcd696d20e688d07d25684cf8` (233,267,910 bytes),
`adb install -r`, device base.apk hash identical, uid 10066 unchanged.
Device: `1.19.2|fabric` re-run on the Debug artifact (header `0.0.1-debug`): scenario A Mod Doctor **clean** →
loader `MAIN_MENU`, 64 mods, four ids present; B `MISSING_REQUIRED_DEPENDENCY`; E `WRONG_MINECRAFT_VERSION,
INCOMPATIBLE_DEPENDENCY_VERSION`; G `DUPLICATE` → **PASS (A,E,G)**. Evidence
`1.19.2-provides-defect\04-after-fix\`. One earlier run of this cell had used the Release package by mistake
(env var lost in a nested `cmd.exe`); it is discarded as an evidence-boundary error.
Fabric now: **PASS 33 · FAIL 0 · BLOCKED 4 · UNSATISFIABLE 0** (1.16.3/1.16.4 availability, 26.2/26.3 runner).

### QA RUNNER — 26.2/26.3 "launch button not found" resolved (2026-09-19 19:20-20:15), 26.2 PASS
No product source changed, no APK built; Debug `730d4737…84cf8` only.
* Three runner defects, all generic and label-free: (1) `Tap-PlayButton` treated "no button" as a missed
  press although the press had already moved the app off the home screen; (2) the press was only accepted on
  a `ModDoctor/INFO] inspect` line within 45 s, while 26.x downloads its assets first; (3) `Start-CellApp`
  snapshotted the launcher-log file list once, but the 26.x first launch restarts the app and writes a NEW
  `logs/log_<ts>_main.log`, so that process's verdict was never read.
* Fixes in `qa-lib.ps1`/`qa-cell.ps1` (QA only): `Find-PlayButtonNode` probe split out; `Test-LaunchProgress`
  (marker counts, added FileDownloader/BatchDownloader); `Get-LauncherLogText` re-resolves paths from the
  cell's marker on every read; `Invoke-LauncherDialogPrimaryAction` answers the launcher's own modals by
  structure/count (labels are unreadable in dumps and localised); `Save-UiDiagnostics` captures activity +
  UI dump + screenshot + launcher log on any unclassifiable launch; press loop waits up to 120 s per press.
* `26.2|fabric` = **PASS (A,B,E,G)**: the 26.2 "Vulkan Check" prompt was answered, Mod Doctor ran clean,
  loader reached MAIN_MENU (57 mods, four ids present).
* `26.3|fabric` = **BLOCKED by renderer support** (not the mod graph): the auto-selected Krypton Wrapper
  declares no support for MC 26.3, and the product's override path (`LauncherElements.kt:271-286`
  `UnsupportedRenderer` → `RealLaunch`) skips Mod Doctor entirely, so no verdict can be certified; device log
  for that launch has no `ModDoctor/INFO` line at all and goes straight to `GameLauncher`/`LaunchArgs`.
  Evidence `test-artifacts\4mod-certification\26.x-launch-flow\00-LAUNCH-BUTTON-INVESTIGATION.md` (+ cell logs
  and both dialog screenshots).
* Fabric now: **PASS 34 · FAIL 0 · BLOCKED 3 · UNSATISFIABLE 0** (1.16.3/1.16.4 availability, 26.3 renderer).

### PRODUCT FIX — renderer warning no longer bypasses Mod Doctor (2026-09-19 20:20-21:30)
`LauncherElements.kt`: `TryLaunch` gained `rendererWarningAccepted` (default false); new pure gate
`decideRendererSupportWarning(isRendererUnsupported, userAccepted)` is used by the pre-launch flow; the
`UnsupportedRenderer` dialog's confirm re-issues `TryLaunch(..., rendererWarningAccepted = true)` instead of
`RealLaunch`, so the accepted warning continues through the plugin/permission/**Mod Doctor** checks. Cancel is
unchanged. Regression `RendererSupportWarningTest` (3 tests) in the `ui.screens.content.elements` package.
Tests: **9 classes, 93 tests, 0 failures, 0 errors** (`runner\green-renderer-gate.log`), incl. the whole
`game.version.mod` package. Debug `b4299f93cc6fbf1f6fdcd30ce55e65f2fb81e874c62dbc265341489e7940efa7`
(233,267,910 bytes, `adb install -r`, device hash identical, uid 10066 unchanged).
Device proof: 26.3 scenarios now show real Mod Doctor verdicts (A clean, B missing x3, E wrong-version +
incompatible-version, G duplicate) where previously there was none.
**26.3|fabric = CURRENT_RENDERER_LIMITATION** (not a mod/loader defect): after Mod Doctor, Fabric loads 57
mods and the game dies at graphics init (`Surface Info: <no surface>`, `Graphics Device: <none>`,
`GL debug messages: <no renderer available>`, crash report written). Candidate #2 (Kopper Zink) could not be
applied honestly: the per-version renderer was written and verified in
`versions/<instance>/ALLLauncher/version.config`, but the running app still launched with the empty/default
renderer, so switching needs the app's settings UI (or future ALL Render selection). Every renderer that
reaches 26.x declares `getMaxMCVersion() = "26.3-snapshot-3"` with a lower displayed limit (Kopper Zink: 26.2).
Evidence `test-artifacts\4mod-certification\26.x-launch-flow\01-RENDERER-BYPASS-FIX.md`.
Fabric now: **PASS 34 · FAIL 0 · BLOCKED 3 · UNSATISFIABLE 0**.

### Forge 1.20.1 Popular Certification — PROVISIONING PASS · BASELINE PASS · SCENARIO A = PRODUCT DEFECT (2026-09-19 22:35-23:10)
Target: Minecraft 1.20.1 + **Forge 47.4.10** (promotions API `1.20.1-recommended`). Debug unchanged
(`b4299f93…0efa7`), no rebuild, no product edit.

- **Provisioning PASS** — `qa-forge.ps1` + `qa-neoforge.ps1` import a MultiMC pack through the launcher's own
  Forge installer; instance `1.20.1 Forge 47.4.10` created (app log: `Info: 1.20.1, Forge - 47.4.10`,
  `libraries/net/minecraftforge/{fmlcore,coremods}/…` copied, temp dir cleared).
  Unblocking QA fix: `Tap-CurrentDialogConfirm` (qa-lib.ps1) — the import prompt's confirm is unreachable via
  `uiautomator` on a live dialog, so the button is derived from the current dialog's own bounds (bright panel →
  dark filled button, lower-right); panel must be a real dialog (not the full-screen home card).
- **Baseline PASS** — 0 mods, Mod Doctor clean, one press → MAIN_MENU (`Sound engine started`, Forge UP_TO_DATE
  47.4.10).
- **Fixture installed** — 6 Modrinth Forge 1.20.1 releases (embeddium 0.3.31+mc1.20.1, oculus 1.20.1-1.8.0,
  jei 15.56.0.205, jade 11.13.3+forge, waystones 14.1.21+forge-1.20.1, balm 7.3.43+forge-1.20.1), SHA-1s match
  the API exactly; installed via `qa-forge-fixture-install.ps1` (reuses `Install-Fixtures`).
- **Scenario A = PRODUCT DEFECT (proven, not fixed)** — Mod Doctor reported `CORRUPT_METADATA` ×6 and blocked
  the launch. Side 1: all six jars ship `META-INF/mods.toml` and no `mcmod.info`. Side 2: `ModDoctor.kt`
  `toDoctorMetadata` → `ModLoader.FORGE -> readForgeOldMetadata`, which reads only `mcmod.info`; the modern path
  is read by `reader/ForgeNewModMetadataReader.kt` but is **not wired into Mod Doctor's dispatch**.
  Evidence: `test-artifacts/popular-certification/forge-1.20.1/{00-A-CLEAN-DEFECT.md,00-SUMMARY.md,A-clean.log,
  A-clean-moddoctor-dialog.png,mods.toml-excerpt.txt,baseline-run.log}`.
- **Scenarios B–F NOT RUN** — every Forge mod is rejected before launch, so they cannot be exercised.
- New runner scripts: `qa-forge-baseline.ps1`, `qa-forge-run.ps1` (launch → Mod Doctor → loader verdict → JSON),
  `qa-forge-fixture-install.ps1`.

### Exact Next Action
`MILESTONE: wire modern Forge metadata into Mod Doctor (authorized product fix, one deliverable).`
`readForgeOldMetadata (mcmod.info only) is dispatched for ModLoader.FORGE, so every Forge 1.20.1 mod is`
`CORRUPT_METADATA and blocked. Reuse the existing reader/ForgeNewModMetadataReader.kt path (META-INF/mods.toml,`
`META-INF/neoforge.mods.toml) in ModDoctor.toDoctorMetadata — no new parser, keep the legacy mcmod.info path as`
`fallback, keep dependencies minus environment entries and the "unknown version = do not false-positive" rule.`
`RED test first: a real Forge jar with META-INF/mods.toml must be readable, plus a control that a jar with`
`neither mods.toml nor mcmod.info stays CORRUPT_METADATA. Then: run the mod package tests, build ONE Debug via`
`D:\ALL001 + JAVA_HOME=C:\jdk21, adb install -r, re-run baseline + Scenario A, then B–F.`
Strategy change: certification is no longer every loader × every Minecraft version; it moves to
popular/real-world coverage — Forge 1.20.1 (this milestone), then NeoForge 1.21.1, NeoForge 1.21.11.
No product source changed; Debug `b4299f93cc6fbf1f6fdcd30ce55e65f2fb81e874c62dbc265341489e7940efa7`.
* Fixture resolved from real metadata (Modrinth, forge + 1.20.1, releases only): **embeddium 0.3.31+mc1.20.1**,
  **oculus 1.20.1-1.8.0** (requires embeddium), **jei 15.56.0.205**, **jade 11.13.3+forge** (optional jei),
  **waystones 14.1.21+forge-1.20.1** (requires balm), **balm 7.3.43+forge-1.20.1** — one render mod, its
  required companion, a library dependency chain, plus two standalone ecosystem mods.
* Loader: Forge promotions API → `1.20.1-recommended = 47.4.10` (latest 47.4.23) — the harness resolves the
  promoted build first, never blindly newest.
* Provisioning: Forge is installed by the launcher itself (`game/download/game/forge/Install.ForgeLike.kt`), so
  the harness hands it a MultiMC pack to import (`qa-forge.ps1` + `qa-neoforge.ps1`, component
  `net.minecraftforge`). Fixed on the way: pack must sit in the app's own files dir (a `file://` URI under
  `/sdcard` is refused with Permission denied); this build never logs the import's version-name step, so the
  on-screen EditText is the only signal; `uiautomator dump` needed a retry + `--compressed` fallback;
  `input text` needs `%s` for spaces. **Still open:** the import prompt is dismissed without starting the
  install — the shared confirm press does not hit that dialog's confirm button.
* Nothing else ran: no baseline launch, no fixture install, no scenarios A-F.
Evidence `test-artifacts\popular-certification\forge-1.20.1\00-SUMMARY.md`.

### Exact Next Action
`MICRO-RUN (QA only): finish driving the launcher's Forge install for 1.20.1 — press the import prompt's
confirm button (right-hand button, not "widest clickable"), then verify the Forge installer really runs
(app-log download/processor lines) and that `versions/1.20.1 Forge 47.4.10/…json` appears; then launch the
baseline to main menu. After that: install the six-mod fixture, run scenarios A-F and record them under
test-artifacts\popular-certification\forge-1.20.1\. Do not build Release.`


## PRODUCT FIX — modern Forge metadata + Forge 1.20.1 popular certification PASS

Timestamp: `2026-09-19 23:48 +07:00`. Target Minecraft 1.20.1 / Forge 47.4.10. Debug only; Release untouched.
Detailed evidence: `test-artifacts\popular-certification\forge-1.20.1\00-SUMMARY.md` and scenario logs in that folder.

### 1. Modern Forge metadata support — PASS
Root cause confirmed: Mod Doctor dispatched every `ModLoader.FORGE` file to legacy-only `readForgeOldMetadata`
(`mcmod.info`), although valid modern Forge 1.20.1 mods use `META-INF/mods.toml` and the project already ships
`ForgeNewModMetadataReader`.

Small fix:
- `ForgeNewModMetadataReader.kt` exposes a Mod-Doctor metadata snapshot from its existing TOML parse path (id,
  version incl. `${file.jarVersion}` resolution, loader family, mandatory dependency ids + predicates, Minecraft
  predicate); environment ids are not invented as mod dependencies.
- `ModDoctor.kt` adapts that snapshot to `ModDoctorMetadata` for `ModLoader.FORGE`; any modern-reader failure falls
  back to the unchanged `mcmod.info` reader. Legacy version is now preserved too. NeoForge parsing was not redesigned.
- New `ModDoctorForgeMetadataTest.kt` drives real JAR entries through `AllModReader → toDoctorMetadata → validation`.
  RED: 5 tests / 3 failures (`CORRUPT_METADATA` where Waystones should miss Balm). GREEN full package:
  **95 tests, 0 failures, 0 errors**; legacy mcmod.info and truly corrupt JAR controls pass; `git diff --check` rc 0
  (untracked `ModDoctor.kt` and new test inspected/tested directly).

Debug artifact:
`ALLLauncher-Debug-0.0.1-x86_64.apk`, 233,267,910 bytes, mtime 2026-09-19 23:40:11, SHA-256
**`D360476608E6DDAAAD0DD15363D0F6D6DC85289C3C105FB59FA1116E7B56A309`**; installed by `adb install -r` into
`com.all.launcher.app.debug`, device base.apk hash identical, firstInstallTime unchanged.

### 2. Forge 1.20.1 Popular Certification — PASS
Existing instance and approved fixture reused; Forge was not reprovisioned and the baseline was not repeated.
- A clean: six mods parse, no CORRUPT_METADATA, Mod Doctor clean, Forge MAIN_MENU / Sound engine started.
- B missing Balm: `MISSING_REQUIRED_DEPENDENCY` (`Waystones requires balm`), Fix offered; one Fix installed the exact
  Forge Balm from Modrinth project `MBAkmtvl`, version `1VlYVa3k`, SHA-1 `4248e3b8…`; post-fix 0; Forge MAIN_MENU.
- C wrong Minecraft: real Forge 1.20.2 Balm `TGc6wvHK`, SHA-1 `e2ae429d…` → `WRONG_MINECRAFT_VERSION`.
- D wrong loader: real Fabric 1.20.1 Balm `wIUUe8by`, SHA-1 `8b557142…` → `WRONG_LOADER` (and resulting missing Balm).
- E duplicate: real Forge Balm 7.3.43 + 7.3.42 (`PtvPxXii`, SHA-1 `90340ba5…`) → `DUPLICATE`.
- F Forge authority: every Mod Doctor-clean graph tested (A and repaired B) was accepted by real Forge; no clean/
  loader-fail contradiction.

Restore: original six-fixture set restored with exact provider SHA-1s; no `.disabled` / temp residue. Release package
untouched.

### Exact Next Action
`NeoForge 1.21.1 popular certification — provision through the existing launcher path, baseline, select a real
popular NeoForge fixture from provider metadata, then run A–F with real-loader authority.`

## PRODUCT FIX — NeoForge metadata + NeoForge 1.21.1 popular certification PASS

Timestamp: `2026-09-20 02:20 +07:00`. Debug only; Release untouched (`com.all.launcher.app` lastUpdateTime
still `2026-09-19 02:28:17`). Detailed evidence: `test-artifacts\popular-certification\neoforge-1.21.1\`.

### 0. Resume point and the real blocker
The handoff blamed a legacy game path. The launcher's own log disproved it: `GamePathManager` loaded the
current package's `.minecraft` and read all 30 versions correctly. The instance `1.21.1 NeoForge 21.1.238`
was listed with `Info: null` because the import had created it under a **doubled** directory name
(`…21.1.2381.21.1 NeoForge 21.1.238`) while a name-only directory, made by the runner's fixture install,
shadowed the intended name; the launcher then fell back to `26.3` ("Stored version … not found").
QA fix (`fix-instance-name.ps1`): the finished instance was renamed to the intended name with its json and
jar, the five fixture jars were carried over, and the phantom directory was removed. Proven by the app
selecting `1.21.1 NeoForge 21.1.238` (the runner fails the run otherwise).

### 1. Product defect — NeoForge mods were never read for metadata
Proven on device: with the approved five-mod fixture in place, removing `balm-neoforge-1.21.1-21.0.65.jar`
left `waystones` without its required `balm`, Mod Doctor reported **`inspect finished: 0 problem(s) []`** and
NeoForge rejected the same set (`Missing or unsupported mandatory dependencies: Mod ID: 'balm', Requested by:
'waystones'` / `Mod loading has failed`) — a clean verdict over a set the real loader refuses.

Two independent gaps, both from the jars' own metadata:
1. `ModDoctor.toDoctorMetadata` dispatched only `ModLoader.FABRIC` and `ModLoader.FORGE`; `ModLoader.NEOFORGE`
   fell through to a default with no dependency table, so a NeoForge mod was always "readable and clean".
   NeoForge 21 mods ship **only** `META-INF/neoforge.mods.toml`.
2. The modern-TOML adapter kept only `mandatory = true` (Forge's schema). NeoForge 21 declares requirements as
   **`type = "required"`**, so even on the right path every NeoForge dependency and Minecraft range was dropped.

Fix:
- `ModDoctor.kt`: `ModLoader.FORGE, ModLoader.NEOFORGE -> readForgeMetadata(local)`.
- `reader/ForgeNewModMetadataReader.kt`: `isRequiredDependency()` accepts `mandatory = true` **or**
  `type = "required"`; the doctor snapshot gained `providedModIds` / `providedModVersions`, read from the
  jar-in-jar mods under `META-INF/jarjar/` (each nested jar's own TOML), reusing `fixAuthorsField` exactly as
  the other two TOML paths do. Without this, JEI's own nested `mezz_config` requirement became a false
  "missing dependency" the moment (1) and (2) started working.
- New regression `ModDoctorNeoForgeMetadataTest` (6 tests) drives real `neoforge.mods.toml` jars through
  `AllModReader → toDoctorMetadata → validateModDoctorMetadata`. RED: `4 tests completed, 2 failed`; GREEN:
  101 tests / 0 failures / 0 errors over all 10 `game.version.mod` classes; `git diff --check` rc 0
  (untracked `ModDoctor.kt` inspected and tested directly).

Debug artifact: `ALLLauncher-Debug-0.0.1-x86_64.apk`, 233,267,910 bytes, mtime 2026-09-20 02:15:06, SHA-256
**`2E5D543AB6DDE2AEDDB4BDD5F6E7A01095B525982EDC980FB32C08AE8D5E0C30`**; `adb install -r` into
`com.all.launcher.app.debug`, device base.apk hash identical, firstInstallTime and uid 10066 unchanged.

### 2. NeoForge 1.21.1 Popular Certification — PASS
Minecraft 1.21.1 / **NeoForge 21.1.238**; approved fixture Embeddium 1.0.15, JEI 19.56.0.441, Jade 15.10.6,
Waystones 21.1.45, Balm 21.0.65 (Modrinth, SHA-1s verified against the API).
- **A clean**: Mod Doctor clean; NeoForge `Mod List` shows all five (`balm`, `embeddium`, `jade`, `jei`,
  `waystones`); `Sound engine started` → MAIN_MENU.
- **B missing dependency**: `MISSING_REQUIRED_DEPENDENCY`; Fix offered and pressed **once** → post-fix
  `0 problem(s)` → the exact NeoForge Balm installed (SHA-1 `69e0b3b6…`) → NeoForge MAIN_MENU.
- **C wrong Minecraft**: real `Jade-1.21.11-NeoForge-21.1.7.jar` (sha1 `601c9077…`) → `WRONG_MINECRAFT_VERSION`
  ("published for Minecraft 1.21.11, not 1.21.1"), launch paused.
- **D wrong loader**: real `jei-1.21.1-fabric-19.56.0.441.jar` (sha1 `a36e33f0…`) → `WRONG_LOADER`, plus the
  consequent `MISSING_REQUIRED_DEPENDENCY` for `fabric-api` with an honest unavailable reason
  ("no Fabric API release supports Minecraft 1.21.1 with the NeoForge loader"); nothing cross-loader installed.
- **E duplicate**: two real Balm builds → `DUPLICATE` naming both files, no Fix offered.
- **F NeoForge authority**: every Mod Doctor-clean graph tested (A and repaired B) was accepted by real
  NeoForge and reached the main menu.

### 3. QA fixes made on the way
- `qa-cell.ps1`: the game log outlives its run and is only truncated when the next launch starts, so a stale
  copy was read as the current pass's outcome — `Invoke-ModDoctorPass` now clears it before pressing launch.
  Without this, a launch that failed at FML resolution was reported as MAIN_MENU.
- `resolve-negative-fixtures.ps1` / fixture downloads go through the runner's `curl.exe` helpers; Modrinth
  arrays must not be wrapped in `@()` (the array comes back as one object).

Restore: instance mods back to the approved five-jar set with exact SHA-1s; no `.disabled` / `.part` / temp
residue; the doubled directory is gone; my UI dumps removed. Release package untouched. QA scripts and
evidence remain under `test-artifacts\popular-certification\neoforge-1.21.1\`.

### Exact Next Action
`NeoForge 1.21.11 popular certification — same method: provision through the launcher's own import with the
instance name typed once (Set-ImportVersionName still appends to the pack's pre-filled name), resolve a real
popular fixture from provider metadata, then run A–F with real-loader authority. The four-mod matrix
(NonForge 1.21.11, Quilt, and the remaining Fabric cells) is still open.`

## NeoForge 1.21.11 Popular Certification — target + fixture resolved (2026-09-20 09:50 +07:00)

Status: **READY** (Step 1–3 done, verified on disk). Provisioning / baseline / scenarios A–F have **not**
run yet; no source change, no rebuild — installed Debug is still
`2E5D543AB6DDE2AEDDB4BDD5F6E7A01095B525982EDC980FB32C08AE8D5E0C30` (device `base.apk` hash re-checked
identical). The validated `1.21.1 NeoForge 21.1.238` instance and its five fixture jars are intact.
Detailed evidence: `test-artifacts\popular-certification\neoforge-1.21.11\`.

- **Loader** — `NeoForge 21.11.45` for Minecraft 1.21.11. Rule reused from the 1.21.1 cell: newest
  neoforged maven release whose **own** `install_profile.json` declares 1.21.11; three candidates
  (21.11.45 / 21.11.44 / 21.11.42) all declare 1.21.11, newest taken. Installer SHA-256
  `e54350cc68d7dec6cb0523a0597a6a2fa664c1acd878b456af4564ee126da00a`. The choice is load-bearing: JEI
  27.35.0.90 requires `neoforge [21.11.44,)`, so 21.11.42 would fail.
- **Fixture (6 real Modrinth releases, neoforge + 1.21.11, taken by version id, SHA-1 verified)** —
  Sodium `sC6wj32u` (mc1.21.11-0.8.14-neoforge), Iris `v6TgIIUM` (1.10.7+1.21.11-neoforge), JEI
  `HHAZAOwD` (27.35.0.90), Jade `LDFqgwEA` (21.1.7+neoforge), Waystones `z7behAlZ`
  (21.11.9+neoforge-1.21.11), Balm `tKWaiIeW` (21.11.9.1+neoforge-1.21.11). The 1.21.1 fixture was not
  reused blind: Embiddium and ModernFix publish **no** 1.21.11 NeoForge build, so the real render pair
  for 1.21.11 (Sodium + Iris) replaces them.
- **Static graph from the jars**: required `iris → sodium [0.6,)`, `waystones → balm [21.11.3,)`; nested
  providers `balm → kuma_api`, `sodium → sodium`; `iris` declares `embeddium` incompatible (absent);
  all six ship `META-INF/neoforge.mods.toml` and no legacy metadata. Expected Mod Doctor for A = 0.
- **Recorded risks for scenario A** (real-world packaging the 1.21.1 fixture never exercised): Sodium
  declares `modId = "sodium"` in both wrapper and nested jar (duplicate detection must not false-fire);
  Waystones' predicate `[21.11.3,)` against 4-component Balm `21.11.9.1`; Jade writes its neoForge
  dependency with no `type`/`mandatory`; Iris's three nested jars carry no mod metadata.

### Exact Next Action
`MICRO-RUN: NeoForge 1.21.11 provisioning + clean baseline. Reuse qa-neoforge.ps1 — Install-NeoForgeInstance`
`-MinecraftVersion 1.21.11 -NeoForgeVersion 21.11.45 -InstanceName '1.21.11 NeoForge 21.11.45' (the launcher's`
`own import pipeline), verify the on-disk instance name immediately after import (the prompt is pre-filled with`
`the pack name — the 21.1.238 doubled-name failure must not repeat), then launch with 0 mods and require fresh`
`Mod Doctor clean + NeoForge initialised + Sound engine started + MAIN_MENU evidence before any fixture is`
`installed. No rebuild, no source change.`


## NeoForge 1.21.11 — provisioning + zero-mod baseline **PASS** (2026-09-20 09:46–09:55 +07:00)

Micro-run 2 of the NeoForge 1.21.11 cell. No source change, no rebuild — installed Debug still
`2E5D543AB6DDE2AEDDB4BDD5F6E7A01095B525982EDC980FB32C08AE8D5E0C30` (device hash re-checked).
Evidence: `test-artifacts\popular-certification\neoforge-1.21.11\`.

- **Provisioning PASS** — requested and actual instance `1.21.11 NeoForge 21.11.45`: one directory,
  exact name, no doubled name and no shadow dir; version json 69,077 B, jar 31,152,600 B,
  `ALLLauncher/version.config`, `mods/` empty; `net.neoforged` libraries + fancymodloader 10.0.36 in the
  version json. Installed by the launcher's own import (`ImportModpackActivity` → `GameInstaller` →
  `Install.ForgeLike`); the pack was parsed with `name=1.21.11 NeoForge 21.11.45`. The 1.21.1 failure
  mode did not recur.
- **QA fix (harness only)** — the first attempt threw `the import never reached its version-name step`.
  Product side was correct: `ModpackImportViewModel.waitForVersionName` only moves UI state
  (`VersionNameOperation.Waiting`) and suspends — it logs **nothing** — so the old log-token gate could
  only time out while the prompt sat on screen pre-filled with the right name. `qa-neoforge.ps1` now
  waits on the on-screen `EditText` prompt (`Wait-ImportVersionNamePrompt`, log token still accepted)
  and the json wait moved into `Wait-NeoForgeInstanceJson`, shared by the install and resume paths.
  One retry (`resume-import.ps1`) confirmed the pending prompt; the instance appeared under the exact
  requested name. **No product defect found in the import flow.**
- **Zero-mod baseline PASS** — 0 mod files (before and after), Mod Doctor `0 problem(s) []`, game log
  proves `Selected Minecraft version: 1.21.11 NeoForge 21.11.45`, `Game Path: …/versions/1.21.11
  NeoForge 21.11.45 (Isolation: true)`, `NeoForge mod loading, version 21.11.45, for MC 1.21.11`, load
  list = Minecraft 1.21.11 + NeoForge 21.11.45 only, `Sound engine started`, MAIN_MENU. The game log did
  not exist before the launch (fresh evidence by construction), no fatal FML/dependency line, no crash
  report. 1.21.1 instance untouched (5 approved jars, certified SHA-1s).

### Exact Next Action
`MICRO-RUN: NeoForge 1.21.11 Scenario A only — copy the six resolved fixture jars (Sodium sC6wj32u, Iris`
`v6TgIIUM, JEI HHAZAOwD, Jade LDFqgwEA, Waystones z7behAlZ, Balm tKWaiIeW; SHA-1s already verified in`
`fixture-manifest.json) into 1.21.11 NeoForge 21.11.45/mods, then ONE launch via Invoke-ModDoctorPass:`
`require Mod Doctor 0 problems, a real NeoForge graph accepted by the loader (ids sodium, iris, jei, jade,`
`waystones, balm + nested kuma_api), Sound engine started and MAIN_MENU on a fresh game log. Watch the three`
`recorded risks in 00-SUMMARY.md §4 (Sodium's duplicated mod id across wrapper+nested jar, Waystones'`
`balm [21.11.3,) predicate against Balm 21.11.9.1, Iris's nested jars without metadata). B/C/D/E/F stay in`
`later micro-runs; no rebuild.`


## NeoForge 1.21.11 — Scenario A (clean fixture) **PASS** (2026-09-20 10:11–10:12 +07:00)

Micro-run 3. Product source untouched, no rebuild — installed Debug still
`2E5D543AB6DDE2AEDDB4BDD5F6E7A01095B525982EDC980FB32C08AE8D5E0C30` (device hash re-verified).
Evidence: `test-artifacts\popular-certification\neoforge-1.21.11\`.

- **Fixture** — the six approved Modrinth NeoForge 1.21.11 releases, taken from `fixture-manifest.json`
  with SHA-1 re-verified locally **and** on the device after the push: Sodium `sC6wj32u` (0.8.14),
  Iris `v6TgIIUM` (1.10.7), JEI `HHAZAOwD` (27.35.0.90), Jade `LDFqgwEA` (21.1.7), Waystones `z7behAlZ`
  (21.11.9), Balm `tKWaiIeW` (21.11.9.1). Exactly six top-level jars, no residue.
- **Mod Doctor = `0 problem(s) []`** (10:11:49) — no false `CORRUPT_METADATA`,
  `MISSING_REQUIRED_DEPENDENCY`, `INCOMPATIBLE_DEPENDENCY_VERSION`, `WRONG_MINECRAFT_VERSION`,
  `WRONG_LOADER`, `DUPLICATE`, and no Embeddium incompatibility although Iris declares `embeddium`
  incompatible (it is not installed). All three recorded risks held up: Sodium's wrapper+nested `sodium`
  id is not a false duplicate; Waystones' `balm [21.11.3,)` is satisfied by the 4-component `21.11.9.1`
  (not compared as a number); Iris's un-metadata'd nested helpers (`jcpp`, `antlr4-runtime`) contribute
  no mod ids and cause no `CORRUPT_METADATA`.
- **Real NeoForge authority PASS** — one launch, fresh game log: `Mod List` = balm, iris, jade, jei,
  **kuma_api (nested jar-in-jar provider of Balm)**, minecraft, neoforge, sodium, waystones;
  `NeoForge mod loading, version 21.11.45, for MC 1.21.11`; `Sound engine started`; MAIN_MENU; zero hits
  for any FML resolution failure and no crash report. **Mod Doctor CLEAN and the real loader accepts the
  same graph — no product defect in Scenario A.**
- **QA fix (harness only)** — `qa-cell.ps1` `Read-LoadedMods` understood only Fabric/Quilt's
  `Loading N mods:` tree, so NeoForge's `Mod List:` table read as "0 mods loaded" (the pass JSON said
  `loaderMods 0`). Added the NeoForge branch (indented `Name Version (Mod Id)` rows, nested providers
  included) + self-check `check-modlist-parser.ps1` → `PASS fabric=3 … neoforge=3 …`; re-reading the
  saved real log now yields `count=9 ids=balm,iris,jade,jei,kuma_api,minecraft,neoforge,sodium,waystones`.
  No relaunch, no APK rebuild.
- **Restore** — the approved six-mod clean fixture stays installed in `1.21.11 NeoForge 21.11.45`;
  1.21.1 / Forge / Fabric instances untouched.
- **Certification intentionally PAUSED after Scenario A** for today's shipping goal; B/C/D/E/F stay open
  for a later session.

### Exact Next Milestone
`Real-device ARM64 shipping validation (iQOO Neo 10): build the ARM64 Debug, install over the existing`
`data without clearing it, then smoke the first launch + account + a known-good Minecraft launch, and`
`prepare today's Release Candidate. Do NOT resume NeoForge 1.21.11 B–F in this session.`


## Phase 5B — Microsoft account QA **PAUSED at the private-auth step** (2026-09-20 10:23–10:36 +07:00)

No source change, no rebuild (Debug still `2E5D543A…0C30`), certification stayed paused.
Evidence: `test-artifacts\microsoft-account-qa\00-STATE-AT-PAUSE.md` (+ `01-home.txt`, `02-account-panel.png`,
`03-account-type-chooser.png`, `04-add-microsoft-account-dialog.png`).

- **Step 1 done** — safe pre-login state from the product's own UI: selected account `ALLQA001`
  (type Offline, still present), no Microsoft account, no other accounts, selected instance
  `1.21.11 NeoForge 21.11.45`. Nothing deleted, nothing logged out. No token/credential/account-store
  value was read, printed or stored anywhere.
- **Step 2 done up to the pause point** — the real flow was navigated, not bypassed: account chip →
  account list panel (`02-account-panel.png`) → `Add Account` → account-type chooser with **Microsoft** /
  Add Auth / Offline (`03-account-type-chooser.png`) → the product's own `Add Microsoft Account` modal
  with a **Login** button (`04-add-microsoft-account-dialog.png`). No account JSON or token was injected
  and no storage was touched.
- **PAUSE** — pressing Login opens the Microsoft sign-in page; authentication belongs to the user alone.
  The agent does not read, type or capture anything from that page (password, 2FA/OTP, passkey, cookies,
  tokens) and does not inspect WebView storage.
- **Prerequisite gap for Steps 4–6** — `adb devices` has only `127.0.0.1:5555` and `emulator-5554`, both
  `ro.product.cpu.abi=x86_64` with a spoofed `model=V2266A` (LDPlayer emulators); **no physical iQOO is
  attached**. And `ZalithLauncher/build/outputs/apk/` holds only the x86_64 Debug/Release — **no ARM64
  Debug exists yet**, so the "same config as the ARM64 device smoke" that Step 6 requires is not there.

### Exact Next Action
`Waiting on the user: (a) confirm/attach the iQOO Neo 10 and let the ARM64 Debug be built+installed first`
`(phase-5B ordering), or (b) authenticate on the currently connected emulator, in which case Steps 4–6 run`
`there and the phone case still needs its own pass. Nothing else proceeds until that answer and the user's`
`private login.`


## Phase 5B — Microsoft account QA: **MICROSOFT_LOGIN_UI_FAIL (missing build input, not a product defect)** (2026-09-20 10:37 +07:00)

Appended to the 10:36 pause entry above. No source change, no rebuild (Debug still `2E5D543A…0C30`).
Evidence: `test-artifacts\microsoft-account-qa\00-STATE-AT-PAUSE.md`, `05-login-failure-redacted.md`.

- User authorised the agent to press the product's `Login` button. It was pressed; the flow starts and
  dies in 2 s:
  `Starting Microsoft account login` → `ClientRequestException: POST https://login.microsoftonline.com/
  consumers/oauth2/v2.0/devicecode → 400 Bad Request` → `Microsoft account login failed`.
- Microsoft's reason (from the launcher's own log, `code=`/opaque values redacted): **`AADSTS900144: The
  request body must contain the following parameter: 'client_id'`**.
- **Root cause proven from source**: the device-code body is built with `BuildKeys.OAUTH_CLIENT_ID`
  (`MicrosoftAuthenticator.kt:104`), which `ZalithLauncher/build.gradle.kts:213` resolves as env
  `OAUTH_CLIENT_ID` → `<rootDir>/.oauth_client_id.txt` → gradle property `oauth_client_id` → **`""`**
  (`getKeyFromLocal`, line 33, warns `BUILD: OAUTH_CLIENT_ID not set`). None of the three sources exists
  on this machine (env unset, no `.oauth_client_id.txt`, no property) and earlier Debug build logs in
  `test-artifacts\4mod-certification\runner\debug-build-*.log` carry that warning → the installed Debug
  APK ships an empty client id.
- **Classification**: MICROSOFT_LOGIN_UI_FAIL — **build configuration**, not product logic. The product
  sends what the build gave it and fails fast with a precise message; no product fix is warranted and
  none was attempted. Nothing secret was read, printed or stored (no password, 2FA/OTP, passkey, cookie,
  token or account-store value; the OAuth error URI's `code=` value was redacted before it was written
  down).
- **Still open from phase 5B**: Steps 4 (account import), 5 (persistence across a normal restart), 6
  (known-good launch with that account) — all blocked on the client id. And no physical iQOO is attached
  (`adb` shows only the two x86_64 LDPlayer emulators) with no ARM64 Debug built, so the phone leg of 5B
  also remains open.

### Exact Next Action
`MICRO-RUN (build input, then 5B step 2 again): user writes the launcher's Azure app client id to`
`D:\ALL Launcher 0.0.1\.oauth_client_id.txt (or exports OAUTH_CLIENT_ID / sets oauth_client_id in`
`gradle.properties) — the agent must not be given the value in chat and must not read the file. Then build`
`ONE x86_64 Debug, adb install -r, verify the installed base.apk hash, re-run Add Account → Microsoft, and`
`only then continue 5B Steps 4–6.`


## Phase 5B — Microsoft QA: **WAITING_FOR_OAUTH_CLIENT_ID** (2026-09-20 10:49 +07:00)

Checked, not built. `.oauth_client_id.txt` does **not** exist at `D:\ALL Launcher 0.0.1\` (and none of the
other two documented sources exists either: env `OAUTH_CLIENT_ID` unset in the build shell, no
`oauth_client_id` property in `gradle.properties`; the module dir has no copy). Nothing was read,
printed, hashed or copied — only existence was checked, and the file's value never entered this session.

- No APK was built (a Debug built without the client id would repeat the proven `AADSTS900144` failure)
  and no device state changed: installed Debug is still `2E5D543A…0C30`, data/accounts/instances untouched.
- The diagnosis still holds and was re-confirmed as unchanged: `MicrosoftAuthenticator.kt` mtime
  `2026-09-16 02:00` (untouched), the only working-tree changes in that area are the pre-existing
  modifications from earlier milestones (`build.gradle.kts` mtime 2026-09-17 18:56, `AccountsManager.kt`).

### Exact Next Action
`User writes the launcher's Azure app client id to D:\ALL Launcher 0.0.1\.oauth_client_id.txt (agent must not`
`be told the value, must not read it, must not print it). Then: build ONE x86_64 Debug with JAVA_HOME=C:\jdk21`
`(verify the log no longer says 'BUILD: OAUTH_CLIENT_ID not set'), adb install -r, verify the installed`
`base.apk hash + UID + firstInstallTime, and re-run account chip → Add Account → Microsoft → Login, expecting`
`the device-code POST to stop returning AADSTS900144. Then Steps 5–8 (import, persistence, launch, restore`
`QA default to ALLQA001).`


## Phase 5B — Microsoft login **FIXED** (device code obtained, polling) · PAUSED for user auth (2026-09-20 11:57 +07:00)

Product source changed for the first time in this phase; Debug rebuilt and reinstalled.

- **Root cause (2 approaches, both device-verified)**: (1) an unconfigured build sent an empty
  `client_id` → `AADSTS900144`; giving it Minecraft's public client id fixed that but produced
  `AADSTS700016` on the Azure `consumers` endpoint, because that id is a Microsoft-account (Live)
  application. (2) Porting the reference implementation's dual path
  (`D:\All New\ZalithLauncher2` @ 2.4.11) — legacy Live device-code endpoint
  `login.live.com/oauth20_connect.srf` + `MBI_SSL` scope when no client id is configured, Azure v2
  endpoint when one is — plus two details from the same reference (lowercase `bearer` `token_type`,
  and `DeviceCodeResponse.message` optional) made the flow work. Pre-flight curl confirmed the legacy
  endpoint still answers HTTP 200 today.
- **Files**: new `game/account/microsoft/MicrosoftClientId.kt`; `MicrosoftAuthenticator.kt` (client-id
  call sites + endpoint/grant-type switch + case-insensitive `token_type`);
  `models/DeviceCodeResponse.kt` (`message` default `""`); new test
  `src/test/java/.../account/microsoft/MicrosoftClientIdTest.kt`. `git diff --check` rc 0. Reference
  tree untouched.
- **Tests**: RED `ComparisonFailure expected:<[00000000402b5328]> but was:<[]>` (2 tests, 1 failure) →
  GREEN **4 tests, 0 failures, 0 errors**.
- **Build**: from `D:\ALL001` (space-free alias; the spaced path breaks the NDK step) with
  `JAVA_HOME=C:\jdk21`, `-Darch=x86_64`. Debug
  `0f328524e42407f561d7fd7ab12ed32b503af2a36f934fff90543f2056821dfc`, 233,267,910 bytes; `adb install -r`
  Success, installed hash identical, `userId=10066` + `firstInstallTime=2026-09-16 12:33:37` unchanged
  (no data loss).
- **Device retest PASS at this stage**: `Device code received, verification url:
  https://www.microsoft.com/link, expires in 900s` → `Polling for token, interval = 5000ms, expires in
  900s`; no `AADSTS900144`, no HTTP 400, no login failure. The dialog on the emulator shows the user
  code.
- **PAUSED**: the user must complete the private Microsoft sign-in at `https://www.microsoft.com/link`.
  The agent does not read, type or capture anything from that page (no password, 2FA/OTP, passkey,
  cookie, device/user code or token); retest evidence is written redacted.
- Still open after auth: account import check (type/profile name/UUID), persistence across a normal
  restart, one known-good Minecraft launch with that account, then restore the QA default account to
  `ALLQA001` (keeping the Microsoft account stored). No physical iQOO is attached and no ARM64 Debug
  exists yet, so the phone leg of 5B remains open.

### Exact Next Action
`User completes the private Microsoft login at https://www.microsoft.com/link with the code shown in the`
`Add Microsoft Account dialog on the emulator, then says it is finished. Then: verify the imported account`
`(type Microsoft, profile name, UUID if shown) without recording tokens, restart the app normally and prove`
`persistence, launch one known-good instance with that account to MAIN_MENU, and finally reselect ALLQA001`
`as the QA default while keeping the Microsoft account stored.`


## Phase 5B — post-auth failure diagnosed: **XBL stage** (product divergence, fixed + installed) (2026-09-20 12:20–12:34 +07:00)

Fresh log `log_2026-09-20T11-56-32_main.log` (redacted copy `08-auth-attempt-redacted.log`). The user's
browser step **succeeded twice** — `Access token successfully retrieved` at 12:13:38 and 12:16:56 — and
the failure is one step later:

`POST https://user.auth.xboxlive.com/user/authenticate` → **401 Unauthorized, empty body** →
`Microsoft account login failed`. XSTS was never attempted; no Minecraft Services call was made. The
first attempt (11:56) merely expired (`Device code expired`, 900 s) and a third was cancelled by the user
leaving the page, so neither is the defect.

- **Stage**: C — Xbox Live authenticate. OAuth/device-code/polling on this build are proven good.
- **Legacy client id**: **not** proven rejected — it issued the device code, the user authorized and the
  token exchange returned a token; the 401 is on a request whose shape differs from the reference.
- **Exact divergence vs `D:\All New\ZalithLauncher2` @ 2.4.11** (this stage only; XSTS / `login_with_xbox`
  / entitlements / profile match character for character): ALL sends **no** `x-xbl-contract-version: 1`
  header; in the legacy flow it sends the `d=`-prefixed RpsTicket (the reference's *fallback* form, whose
  primary is the bare token); and its alternate-form retry fires on **400 only**, so the 401 was final.
- **Fix (smallest, faithful)**: new `XblTicket.kt` — `xblTicketForms(legacy, token)` (legacy → bare first,
  v2 → `d=` first) and `shouldRetryXblWithAlternateTicket(status)` (400 **or** 401); `authenticateXBL` now
  sends the contract-version header and uses both forms with the 400/401 retry.
- **RED → GREEN**: `XblTicketTest` RED `tests=3 failures=2` (legacy form + 401 retry) → GREEN
  **7 tests, 0 failures, 0 errors** across `MicrosoftClientIdTest` + `XblTicketTest`; `git diff --check`
  rc 0.
- **Installed**: Debug `eb1d635af6c51cb776833e5da9ec3ffc401ffc6e96a4ee0db9dd3199c1b81e09`, 233,267,910
  bytes; `adb install -r` Success, installed hash identical, `userId=10066` +
  `firstInstallTime=2026-09-16 12:33:37` unchanged (no data loss).
- No credential, device/user code or token was read, printed or stored; evidence logs are redacted.

### Exact Next Action
`One user attempt at https://www.microsoft.com/link with the code the app shows (the fixed build is already`
`installed). Then read the fresh log: if XBL returns 200 the flow continues to XSTS → login_with_xbox →`
`profile → account import, and 5B can finish (import check, persistence, known-good launch, restore QA`
`default to ALLQA001). If XBL still returns 401 with the contract-version header and both ticket forms`
`tried, the remaining cause is an Xbox-side rejection of the legacy app identity: classify`
`LEGACY_CLIENT_ID_REJECTED and report what ALL's own Microsoft app registration needs — no further code`
`guessing.`


## Phase 5B — **MICROSOFT_QA_PASS** (2026-09-20 13:03–13:12 +07:00)

No login was started by the agent, no device code requested, no auth code modified, no rebuild, no
fixture touched; installed build is still the previous run's
`eb1d635af6c51cb776833e5da9ec3ffc401ffc6e96a4ee0db9dd3199c1b81e09`. Evidence:
`test-artifacts\microsoft-account-qa\10-MICROSOFT-QA-REPORT.md` (+ `09-launch.*`, `10-*`/`11-*` screenshots).

- **Import PASS** — with the XBL fix installed the user's login completed the whole chain
  (`Access token retrieved` → XBL → XSTS → Minecraft services → `Verifying Minecraft ownership`) and the
  product logged `Microsoft account login successful: THE_BOUY2` → `Saved account: THE_BOUY2` →
  `Loaded 2 accounts`. Type Microsoft; profile name `THE_BOUY2`; UUID not exposed by the normal UI, so not
  recorded. No token/cookie/credential value read or stored.
- **Persistence PASS** — process restart (data untouched) + relaunch → `Loaded 2 accounts`, **no** new
  `Starting Microsoft account login`; the account list shows `THE_BOUY2 — Microsoft` still selectable
  alongside `ALLQA001 — Offline`.
- **Game launch PASS** — one press on `1.21.11 NeoForge 21.11.45` with the six approved jars unchanged
  (sha1 before == after): Mod Doctor `0 problem(s)`, `▷ Account: THE_BOUY2 (Microsoft)` in the game log and
  Minecraft's own `Setting user: THE_BOUY2`, NeoForge 21.11.45 for MC 1.21.11 with 9 loader entries, no
  fatal FML line, `Sound engine started`, MAIN_MENU (plus a screenshot of the in-game title screen:
  *Minecraft 1.21.11, NeoForge 21.11.45 (9 mods)*).
- **Cleanup done** — Microsoft account kept (not logged out, not deleted); `ALLQA001` restored as the
  active account (home panel reads `ALLQA001, Offline`).

### Exact Next Milestone
`Physical device: connect the iQOO Neo 10 → build the ARM64 Debug (from D:\ALL001, JAVA_HOME=C:\jdk21,`
`-Darch=arm64) → install over the existing data (no clear) → real-device smoke: app first launch + account`
`+ a known-good Minecraft launch → then today's ARM64 Release Candidate. NeoForge 1.21.11 scenarios B–F stay`
`open for a later session.`


---

## Phase 5C — ARM64_RC_READY (2026-09-20 13:32–14:05 +07:00) — SUPERSEDED by Phase 5D

STATE: RC artifact-level READY. ไม่มี blocker. source ยังไม่ commit — HEAD = b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e (2026-09-15, bump version to 2.5.3).
หมายเหตุ: RC_READY รอบนี้ = artifact ลงเครื่องจริงได้และเปิดได้จริง ไม่ได้แปลว่าพร้อมแจกสาธารณะ/ขึ้นสโตร์ และไม่ครอบคลุมเส้นทางอัปเดตทับของเดิม (รอบนี้เป็น fresh install).
รายละเอียดเต็มอยู่ใน `test-artifacts/shipping/0.0.1-rc/00-RC-SUMMARY.md` — section นี้เป็นข้อสรุป ไม่ใช่ที่เก็บหลักฐานซ้ำ.

ARTIFACTS (arm64-v8a เท่านั้น: aapt native-code = 'arm64-v8a' · lib/ = arm64-v8a only · runtimes = bin-arm64.tar.xz only · zipalign -P 16 "Verification successful")

- Debug (internal QA — ห้ามแจก): `D:\ALL001\ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.1-arm64-v8a.apk`
  - 223,429,662 B · mtime 2026-09-20 13:32:36 · sha256 `92282EBAB8CA0B1C1641E2670BB599906D1CFB64DDCB0A833FC0EC2DDDBD24AC`
  - `com.all.launcher.app.debug` · versionCode 1 · versionName 0.0.1-debug · debuggable
  - signed v2 · 1 signer · CN=MovTery · cert sha256 `8adad0d5…` (debug keystore — ถูกต้องตามที่ควรเป็น ไม่ใช่ cert release)
  - BUILD SUCCESSFUL in 4m 24s (19 executed / 104 up-to-date) · log: `test-artifacts\shipping\0.0.1-rc\arm64-debug-build.log` (UTF-16LE)
- Release (RC artifact): `D:\ALL001\ZalithLauncher\build\outputs\apk\release\ALLLauncher-0.0.1-arm64-v8a.apk`
  - 187,187,716 B · mtime 2026-09-20 13:49:42 · sha256 `FA41CB60A27B6CDF55585B81E10E7A48B232284556788F3A3E87C8F8C1ED3B1D`
  - `com.all.launcher.app` · versionCode 1 · versionName 0.0.1 · ไม่อยู่ในบรรทัด debuggable
  - signed v2 · 1 signer · DN "CN=ALL Launcher, O=ALL Launcher, C=TH" · cert sha256 `b72b76bd56f32ae16a3c10bac9fad1bd68a8d48ada273fcf4530f854089d2177`
  - ★ ตรงกับ anchor ของโปรเจกต์ (B7:2B:76:BD:…:9D:21:77)
  - BUILD SUCCESSFUL in 13m 30s (34 executed / 169 up-to-date) · minifyReleaseWithR8 + lintVitalRelease รันจริง · log: `test-artifacts\shipping\0.0.1-rc\arm64-release-build.log`
  - command (ทั้งคู่): `cd D:\ALL001; $env:JAVA_HOME='C:\jdk21'; .\gradlew.bat :ZalithLauncher:assembleDebug|assembleRelease -Darch=arm64 --no-daemon --console=plain`

ORDERING: source ใหม่สุด 2026-09-20 12:26:49 (MicrosoftAuthenticator.kt `9BCC6C28…`) < debug 13:32:36 < release 13:49:42 ⇒ ไม่มี source edit ใหม่กว่า artifact ทั้งคู่ (ไม่มี stale build). ตรวจ source freeze ซ้ำหลัง build: ไม่มีไฟล์ใต้ `ZalithLauncher\src` ที่ใหม่กว่า 13:28:00.

DEVICE: serial `10AF681QSC001YK` · vivo/iQOO · model I2405 · device I2405 · Android 16 · SDK 36 · abi arm64-v8a · abilist arm64-v8a · page size 4096 · /data ว่าง 28.4 GB

FRESH INSTALL: ก่อนติดตั้ง `pm path` ว่างทั้ง com.all.launcher.app.debug และ com.all.launcher.app ⇒ เครื่องนี้ไม่เคยมีแอปมาก่อน ข้อสันนิษฐานเดิมในเช็คพอยต์ (อัปเดตทับข้อมูลเดิม) **ไม่ตรงกับเครื่องนี้**

- debug: `install -r` Success · runtime แตก JNA 26 ไฟล์ + lwjgl arm64-v8a (8+9 entries) · MainActivity ขึ้น · ไม่มี FATAL/ANR/dlopen · `Loaded 0 accounts` (เครื่องใหม่)
- release: install Success · firstInstallTime/lastUpdateTime 2026-09-20 14:00:55 · primaryCpuAbi=arm64-v8a · versionName 0.0.1
  - base.apk บนเครื่อง sha256 `fa41cb60a27b6cdf55585b81e10e7a48b232284556788f3a3e87c8f8c1ed3b1d` ★ ตรงกับ APK บนโฮสต์ (bit-for-bit)
  - SplashActivity → ติดตั้ง runtime (`runtimes/jre-8/bin-arm64.tar.xz`) → MainActivity · Renderers loaded 10 · `Loaded 1 game paths` · ไม่มี FATAL/ANR

MICROSOFT ACCOUNT (ยืนยันบนเครื่องจริงด้วย debug package): [13:49:23] login start → [13:49:24] device code (https://www.microsoft.com/link, expires 900s) → [13:57:25] access token → XBL → XSTS → Minecraft services → ownership → [13:57:30] cape/skin success → "Microsoft account login successful: THE_BOUY2"

- UI: THE_BOUY2 ชนิด Microsoft · Saved account → `Loaded 1 accounts`
- PERSISTENCE: force-stop แล้วเปิดใหม่ → THE_BOUY2 ยังอยู่ ไม่ต้องล็อกอินซ้ำ
- SECRETS: ไม่มีการอ่าน/บันทึก รหัสผ่าน OTP passkey device code หรือ token ใด ๆ (UI dump ที่มี device code ถูกลบทิ้ง ไม่เก็บเป็นหลักฐาน)

MINECRAFT ON ARM64 (debug package, สร้าง instance ผ่าน UI จริง): ดาวน์โหลด 518 MB · Vanilla 1.21.11 + Fabric Loader 0.19.5 + Fabric API 0.141.6+1.21.11 · `mods/fabric-api-0.141.6+1.21.11.jar`

- Mod Doctor ก่อนเข้าเกม: `inspect finished: 0 problem(s) []`
- MARKERS: `Loading Minecraft 1.21.11 with Fabric Loader 0.19.5` · `Setting user: THE_BOUY2` · `LWJGL 3.3.3-snapshot` · `Reloading ResourceManager: vanilla, fabric-api, … fabricloader` · `OpenAL initialized on device OpenSL` · `Sound engine started` · texture atlas ครบ (gui.png-atlas/items/beds ฯลฯ) · overlay FPS 30
- กิจกรรม: `com.all.launcher.app.debug:game` pid 24800 RSS ~1.98 GB · VMActivity focused · screenshot `test-artifacts/shipping/0.0.1-rc/iqoo-minecraft-mainmenu.png` (sha256 `C572A0FE…`)

FIX อยู่ใน binary ที่ติดตั้งจริง (ปิดข้อจำกัดที่ Reviewer ติ): แตก dex จาก APK แล้ว probe แบบ binary-safe (findstr) พร้อม control

- control: `ModDoctor` → classes16.dex, classes25.dex ✔ · `zalithlauncher` → 16 ไฟล์ ✔ · `SplashActivity` → หลายไฟล์ ✔
- เป้าหมาย: `xboxlive` → classes21.dex · `login.live.com` → classes21.dex
- ⇒ endpoint MS/XBL อยู่ใน dex ที่ติดตั้งจริง (ก่อนหน้าได้ 0 เพราะ Select-String โหมด text อ่านไบนารีไม่ได้ ไม่ใช่เพราะไม่มี)
- หลักฐานหลักของ fix คือ functional: MS chain วิ่งครบถึง ownership บนเครื่องจริง; dex marker เป็นหลักฐานประกอบ

VERDICT ของ all-reviewer (task_3): ARM64_DEBUG_ARTIFACT_APPROVED (artifact-level) พร้อม caveat: ไม่มี dex marker ตอนนั้น (ปิดแล้ว), เครื่องเป็น fresh install, และ Release ยังไม่มี artifact ตอนที่ตอบ (บัดนี้มีแล้ว + cert ตรง)

ยังยืนยันไม่ได้ (บันทึกตรง ไม่ใช่ blocker)

- release APK (R8) ไม่ได้รัน flow เต็ม — พิสูจน์แค่ install + launch + runtime unpack ส่วน MS login และการเข้าเกม ยืนยันบน debug package เท่านั้น (source เดียวกัน แต่ R8 ลบ/เปลี่ยนชื่อได้)
- source ทั้งหมดยังไม่ commit ⇒ RC นี้ผูกกับ working tree ไม่ใช่ commit/tag (reproduce จากประวัติ git ไม่ได้)
- เส้นทางอัปเดตทับของเดิมไม่ถูกทดสอบเลยในรอบนี้ (fresh install เท่านั้น)
- signing scheme ที่รายงานคือ v2 (ไม่มี v3) — พอสำหรับ sideload; ยังไม่ทดสอบเส้นทางแจกจ่าย/สโตร์

DEVIATIONS / NOTES

- **all-scout (task_2) ล้มเหลว**: `opencode-go request failed with status 400: Upstream request failed: [invalid_request_error] messages[3]: "name" is not supported by this endpoint` ⇒ รอบนี้ไม่มีผล discovery จาก scout งานถูกทำโดย central session + Lead + Reviewer แทน — บันทึกเป็น "scout ไม่มีผล" ไม่ใช่ "ผ่าน"
- fresh install ไม่ใช่ update (สมมติฐานเดิมในเช็คพอยต์ผิดสำหรับเครื่องนี้)
- `[BatchDownloader/WARNING] Completed-count mismatch: requests=0 finished=4651 failures=all` — ไม่ขัดขวางการเข้าเกม (518 MB โหลดสำเร็จจริง, Mod Doctor 0 problems) แต่ตัวนับรายงาน "failures=all" ซึ่งเชื่อถือไม่ได้ ต้องตามต่อ
- dialog "New Version Found / 2.5.3" โผล่ใน release — upstream update prompt ไม่ใช่ปัญหาของรอบนี้

EXACT NEXT ACTION (หนึ่งข้อ): ติดตั้ง release APK (`D:\ALL001\ZalithLauncher\build\outputs\apk\release\ALLLauncher-0.0.1-arm64-v8a.apk`, sha256 `FA41CB60…3B1D`) ลง iQOO 10AF681QSC001YK แล้วรัน flow เต็มหนึ่งรอบ — Microsoft login (THE_BOUY2) + สร้าง/เข้า Fabric 1.21.11 + force-stop/เปิดใหม่เพื่อเช็ค persistence — เก็บ FATAL/ANR และ marker เดียวกับรอบ debug เป็นหลักฐาน

- ถ้าผ่าน: ปิด RC ที่ artifact level ได้เต็ม (commit + tag source เป็นงานตามหลัง ไม่นับเป็นส่วนของ action นี้)
- ถ้าไม่ผ่าน: RC ยังคงใช้ได้เฉพาะ debug artifact — ต้องบันทึกเป็น blocker ทันที

[SUPERSEDED 2026-09-20 — section นี้ยังอ้าง artifact `FA41CB60…`/`92282EBA…` และยังนับ update dialog ของ upstream เป็น deviation;
RC artifact ปัจจุบันคือ `3BD3E79D…` (release) / `6271FBF5…` (debug) ใน Phase 5D ด้านล่าง]

---

## Phase 5D — ALL_0_0_1_FINAL_RC_PASS — update-gate off + final ARM64 Release verified on the real iQOO (2026-09-20 14:20–15:05 +07:00)

STATE: `ALL_0_0_1_FINAL_RC_PASS` (artifact level, เครื่องจริง iQOO `10AF681QSC001YK`). ไม่มี blocker.
Phase 5C ถูก SUPERSEDED (ยังอ้าง artifact เก่า และยังนับ update dialog เป็น deviation).
ยังไม่ครอบคลุม: store/OTA (v2 เท่านั้น) · reproducible build จากประวัติ git (ยังไม่ commit) ·
first-run ของ final artifact บน data ว่าง (รอบนี้เป็น `install -r` ทับของเดิม ⇒ ทางอัปเดตทับ + data ไม่หาย ได้ทดสอบแล้ว).

UPDATE GATE — 2 ไฟล์ ไม่แตะไฟล์อื่น
- `viewmodel/LauncherUpgradeViewModel.kt` +12: `private const val ALL_UPDATE_FEED_ENABLED = false` (L95 + KDoc)
  และ `if (!ALL_UPDATE_FEED_ENABLED) return null` เป็น statement แรกของ `fetchRemoteData()` (L204)
- `ui/screens/content/settings/AboutInfoScreen.kt` −5: ลบปุ่ม `onClick = checkUpdate` เหลือ Project Link
- ประตูเหนือทุกเส้นทาง: `checkOnAppStart()` (L147) และ `checkManually()` (L182) เรียก `fetchRemoteData()` ทั้งคู่;
  custom-homepage `check_update` → `MainActivity.checkUpdate()` (L577) → `checkManually()`.
  HTTP ต่อ Zalith มีสองรายการเดียวในโปรเจกต์ (L209 · fallback `repo.miawa.cn` L220) อยู่ใต้ประตู ⇒ ปิด = ไม่มี request เลย
- `git diff --check` exit 0 · mtime ทั้งสองไฟล์ 2026-09-20 14:20:32 · UrlManager / gradle.properties / BuildConfig / provider มอด ไม่ถูกแก้
- เป็นโค้ด upstream ไม่ใช่ของ ALL: `bf62ab93` `feat：启动器内检查更新` โดย `movte <movtery228@qq.com>`
  (Tue Dec 23 22:08:51 2025 +0800) เป็น ancestor ของ HEAD; ตัวเทียบคือ `BuildConfig.VERSION_CODE (1) < data.code (200040)`
- residual: parameter `checkUpdate` ใน AboutInfoScreen กับ `EventViewModel.Event.CheckUpdate` ยังอยู่ในซอร์ส;
  ถ้าถูกเรียกจะจบที่ toast `upgrade_get_remote_failed` (MainActivity L592–598) ไม่ยิงเน็ต ไม่เด้ง dialog ไม่มี UI stock ของ ALL เข้าถึง

ARTIFACTS (arm64-v8a เท่านั้น; aapt ยืนยัน lib/ + runtime มีเฉพาะ arm64)
- Debug: `ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.1-arm64-v8a.apk`
  223,429,662 B · mtime 14:24:49 · sha256 `6271FBF51CCF6879B3EFA031C9E7FAE70E7C9CB770B2EFC4F7C704C22E569193`
  (แทน `92282EBA…`) · BUILD SUCCESSFUL in 3m 51s · หลักฐาน `arm64-debug-build.txt`, `arm64-debug-audit.txt`
- Release (FINAL): `ZalithLauncher\build\outputs\apk\release\ALLLauncher-0.0.1-arm64-v8a.apk`
  187,174,100 B · mtime 14:39:18 · sha256 `3BD3E79D323F9A24BCA775FF9849E3E7AFF779FDB70651CC5CA321E1D715BAB9`
  (แทน `FA41CB60…`) · BUILD SUCCESSFUL in 13m 9s · minifyReleaseWithR8 + lintVitalRelease + packageRelease รันจริง
  · หลักฐาน `arm64-release-build.txt`, `arm64-release-audit.txt`
- apksigner Verifies (v2, 1 signer) · cert sha256 `b72b76bd…2177` = anchor ALL · zipalign -P 16 ผ่าน ·
  `base.apk` บนเครื่อง = `3bd3e79d…` ตรงกับไฟล์บนโฮสต์ · all-reviewer: `FINAL_RELEASE_ARTIFACT_APPROVED` (ก่อนติดตั้ง)

DEVICE PROOF — update gate (เกณฑ์ชี้ขาด = launch ที่ไม่พิมพ์บรรทัด LauncherUpgradeVM)
- Debug: [13:35:44.697] detected (บิลด์เก่า) → [14:00:13] / [14:25:33] / [14:31:47] skip → **[14:39:15] ไม่มีบรรทัดเลย** (พ้น 1 ชม.)
- Release: [14:01:55.056] detected (บิลด์เก่า) → [14:48:34] / [14:58:42] / [15:00:56] skip → **[15:02:55] ไม่มีบรรทัดเลย**
- About: "ALL Launcher" / "Launcher Version: 0.0.1-debug" + Project Link + Sponsorship เท่านั้น
  (`iqoo-about-no-update-button.png` CAA0DB7B…)
- `install -r` ทับของเดิม: debug firstInstallTime 13:33:35 คงเดิม · release 14:00:55 คงเดิม, lastUpdateTime 14:48:30,
  primaryCpuAbi arm64-v8a, versionName 0.0.1 ⇒ ทางอัปเดตทับของเดิม + data ไม่หาย ได้รับการทดสอบแล้ว
- Debug regression บนบิลด์ใหม่: `Loaded 1 accounts`, `current version is 1.21.11 Fabric 0.19.5`, Minecraft ถึง main menu

MICROSOFT + MINECRAFT บน artifact สุดท้าย (Release)
- [14:56:53] start → device code 900s (ผู้ใช้ทำขั้น private เอง) → [14:58:04] access token → XBL → XSTS →
  Minecraft services → ownership → skin/cape → "Microsoft account login successful: THE_BOUY2" → `Loaded 1 accounts`
- persistence: force-stop + เปิดใหม่ → THE_BOUY2 ยังอยู่ ไม่ต้องล็อกอินซ้ำ
- instance สร้างผ่าน UI จริง: 1.21.11 + Fabric 0.19.5 + Fabric API 0.141.6+1.21.11 · 518.22 MB · "การติดตั้งเสร็จสมบูรณ์"
- `ModDoctor inspect finished: 0 problem(s)` · [14:59:51] Loading Minecraft 1.21.11 with Fabric Loader 0.19.5 ·
  [15:00:04] Setting user: THE_BOUY2 · [15:00:07] Reloading ResourceManager: vanilla, fabric-api, … fabricloader ·
  [15:00:12] OpenAL OpenSL / Sound engine started · FPS 61 · `com.all.launcher.app:game` alive ·
  `iqoo-release-minecraft-mainmenu.png` 0386760C… · ไม่มี FATAL/ANR
- ไม่มี secret ถูกอ่าน/เก็บ: device code, OTP, passkey, token ไม่ถูกบันทึกเป็นหลักฐาน (UI dump ที่มี device code ถูกลบทิ้ง)

SOURCE FREEZE
- HEAD `b4c9415bf5be0d2f1ad6af511d695a50f7c6dc6e` — ไม่มีการ commit อะไรเลย · 27 modified tracked + untracked
  (plain `git status` ยุบโฟลเดอร์; 40 คือจำนวนแบบ -uall)
- รอบนี้แก้ไฟล์โปรดักชัน 2 ไฟล์ (+12 / −5) · source ใหม่สุด 14:20:32 < debug 14:24:49 < release 14:39:18 ⇒ ไม่มี stale build
- ไม่ bump version · ไม่แตะ gradle.properties / BuildConfig / UrlManager / ระบบดาวน์โหลดมอด

DEVIATIONS
- all-scout ล้มทั้งสองรอบ (400 `"name" is not supported by this endpoint`) ⇒ รอบนี้ **ไม่มีผล discovery จาก scout**
  (บันทึกเป็น "ไม่มีผล" ไม่ใช่ "ผ่าน"); discovery ทำโดย central session, review ทำโดย all-reviewer 2 รอบ
- USB ของ iQOO หลุดกลางคัน 2 ครั้ง กู้ด้วย `adb kill-server` / `start-server` — ไม่กระทบผล แต่บันทึกไว้
- ร่องรอย upstream ใน About ที่ยังไม่แก้: Project Link → repo Zalith · Sponsorship → ลิงก์ของ MovTery (ยังไม่มี URL ของ ALL จริง)
- `[BatchDownloader/WARNING] Completed-count mismatch: requests=0 finished=4651 failures=all` — ตัวนับเชื่อถือไม่ได้
  แต่การติดตั้ง 518 MB สำเร็จจริงและ Mod Doctor 0 เคส ⇒ ยังไม่ได้ตามต่อ ไม่ใช่ blocker
- ค่าที่ควรวัดซ้ำก่อนใช้ในเอกสารเผยแพร่: ขนาด Debug เท่ากับบิลด์ก่อนแก้เป๊ะทั้งที่ hash ต่าง — ตัวชี้ตัวตนคือ SHA-256
- หลักฐานดิบไม่ถูกเก็บ: logcat รอบนี้อยู่ในรูปไฟล์สรุป (`update-gate-verification.txt`, `iqoo-*.txt`) ไม่มี raw device log
  ใน `test-artifacts/shipping/0.0.1-rc/`; บิลด์ล็อกเป็น UTF-16LE ⇒ ต้องอ่านด้วย `-Encoding Unicode`

งานถัดไป (เรียงตามลำดับ)
1. commit source ทั้งหมด + tag `0.0.1` — ตอนนี้ RC ผูกกับ working tree เท่านั้น
2. ใส่ URL จริงของ ALL ใน About (Project Link / Sponsorship) แล้วปิดร่องรอย upstream
3. เก็บ residual ของ gate ให้สะอาด (parameter `checkUpdate` ใน AboutInfoScreen, `EventViewModel.Event.CheckUpdate`,
   ข้อความ doc ของ homepage) หรือรอเปิดใช้พร้อมช่องทางอัปเดตของ ALL จริง
4. ถ้าต้อง belt-and-braces: uninstall + ติดตั้ง `3BD3E79D…` แบบ fresh install หนึ่งรอบ เพื่อปิดช่องว่าง first-run ของ artifact สุดท้าย
5. ตามต่อ BatchDownloader completed-count mismatch (ไม่บล็อก แต่เป็นหนี้ความน่าเชื่อถือ)

EXACT NEXT ACTION: commit + tag source เท่านั้น (ไม่มี build และไม่มี adb ในสเต็ปนี้)

## MODPACK_SAFE_MODE_V0 (2026-09-20 16:0x–16:30 +07:00)

Post-0.0.1 work. The frozen archive `D:\ALL Launcher Releases\0.0.1\` was not touched (APK and source
zip re-hashed after this milestone, unchanged).

### 检测：实例是不是整合包安装的

安装整合包时由启动器自己写一份**包内清单**：`<实例>/ALLLauncher/modpack.json`（`ModpackPolicy.FILE_NAME`），
内容为 `{name, platform, mods[{name, sha1, url}], modsComplete}`。这份文件存在 = 该实例由整合包安装
（pack-managed）；不存在 = 手工管理的实例，行为完全不变。写入点只有两个（都是安装收尾、`targetClientDir`
已经就绪的地方）：`ModPackInstaller`（在线安装 CurseForge/Modrinth）与 `ModPackInfoTask`
（导入 .mrpack / CurseForge zip，被 `ModrinthPack`/`CurseForgePack` 继承）。
MultiMC/MCBBS 这类没有包内固定清单的格式 V0 不写标记，因此保持原行为（见限制）。

### 归属判断

文件名 + 清单：`ModpackPolicy.isPackOwned(name)`。
- 清单里有的文件 → 整合包自带（权威，精确身份 = 清单里的 sha1 + url）
- 清单里没有的文件 → **只有** `modsComplete=true`（即实例 mods 目录里的每个 jar 都在清单里，安装时
  自动核对；overrides 里塞进来的模组会让它变成 false）才算“用户自己加的”，其余一律按整合包文件对待
- 清单读不出来（损坏）→ 不退化成“没有限制”，按 pack-managed 处理（保守）

### 行为（`ModDoctor.applyPackSafeMode` + `fixProblems` 双重把关）

- 整合包文件的**替换/升级/降级/换版本/换平台/删除**：不做。`replacementFix` 在检查阶段就被摘掉，
  问题照旧报告，描述里追加 "This mod is managed by the modpack, so ALL won't change it automatically."
  （复用现有 Mod Doctor 对话框的 reason 渲染，没有改 UI）
- 依赖自动安装（本身就是“挑一个最新兼容版本”）：不做，除非整包里**恰好只有一个**文件缺失、且它的
  精确身份（url + sha1）已知 —— 此时改成 `exactRestoreFile`，用现有 `downloadFile(url, target, sha1)`
  按原身份取回并校验；多个缺失或身份不明 → 只报告
- 缺失/损坏的整合包文件：只按精确身份恢复，否则报告（见限制）
- 可选依赖：安全模式不会新造 fixCandidate，因此缺失的可选依赖永远不会成为自动安装目标；必装依赖的
  判定逻辑没有改动
- 真实错误（错误加载器、错误 MC 版本、重复、缺必装依赖、元数据损坏）照样报告：安全模式只限制“改动”，
  不伪造兼容性
- 非整合包实例：`applyPackSafeMode` 原样返回，`fixProblems` 先过滤再执行，路径与从前一致

### 改动文件

- 新增 `game/version/mod/ModpackPolicy.kt`（清单模型 / 读写 / 归属 / 精确恢复身份 / `toPackOwnedMods()`）
- `game/version/mod/ModDoctor.kt`：`ModDoctorProblem.exactRestoreFile`、`PACK_MANAGED_REASON`、
  `PACK_RESTORE_REASON`、`applyPackSafeMode()`、`inspect(version)` 过滤、`fixProblems` 过滤 + 精确恢复 + 拦截
- `game/download/modpack/install/ModPackInstaller.kt`、`ModPackInfoTask.kt`：安装收尾写入包内清单
- 新增测试 `src/test/java/.../game/version/mod/ModpackSafeModeTest.kt`（11 个用例）

### 测试

`:ZalithLauncher:testDebugUnitTest --tests 'com.movtery.zalithlauncher.game.version.mod.*'` →
**112 tests, 0 failures, 0 errors**（新增 ModpackSafeModeTest 11，既有 Mod Doctor 回归全部保持不变：
ModDoctorTest 35、ModDoctorInstalledDependencyTest 15、ModDoctorCandidateCompatibilityTest 10、
ModDoctorReplacementRepairTest 10、ModDoctorMinecraftRangeTest 10、ModDoctorNeoForgeMetadataTest 6、
ModDoctorForgeMetadataTest 5、ModDoctorStableFirstCandidateTest 5、ModDoctorProvidedModIdTest 3、
ModFileDisableSemanticsTest 2）。`git diff --check` rc 0。

### ARM64 Debug

`D:\ALL001\ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.1-arm64-v8a.apk`
223,990,349 bytes, SHA-256 **`3486481378c41e6080b0b528a9eb13fd39a03bf82b787b6d8493b95d5977a468`**
（`.\gradlew.bat :ZalithLauncher:assembleDebug -Darch=arm64 --no-daemon`，从 `D:\ALL001`，JAVA_HOME=C:\jdk21）。
实机 smoke：**SKIPPED** —— `adb devices` 只有 `emulator-5554`（LDPlayer），没有物理设备。

### 限制（V0 有意保留）

- 整合包文件的缺失/损坏只做**精确恢复的那一种情形**；无法把“缺失的依赖项目”权威映射到具体包内文件，
  因此其余情况只报告，不猜版本
- 用户自己加的模组：只有清单完整（`modsComplete=true`）时才继续走普通 Mod Doctor 自动修复；清单不完整时
  一律保守处理（可能少修一些用户模组，但绝不会动整合包文件）
- MultiMC / MCBBS 格式没有包内模组清单，V0 不标记为 pack-managed（行为与从前一致）
- CurseForge 清单不含 hashes/直链，这类包内文件按文件名保护、但没有精确恢复身份


## MODPACK_SAFE_MODE_V0_SMOKE (2026-09-20 16:44–16:58 +07:00) — PRACTICAL_PASS

Practical smoke of Safe Mode V0 on LDPlayer `emulator-5554` (x86_64). Real pack, real import path,
real Mod Doctor. Evidence: `test-artifacts\safe-mode-smoke\` (`00-SUMMARY.md` + logs/JSON).

- Artifact: x86_64 Debug `a153053f27e188ea081c39729adcccecb1fbdcbe26ae62fd45e6b84d3a8e1ebd`, installed
  hash matches, userId 10066 / firstInstallTime unchanged. One rebuild was needed because the installed
  12:34 build predates Safe Mode and no current x86_64 artifact existed.
- Pack: `[Fabric] Haste-1.21.1` (Modrinth `plo` / version `iQyGNliq`) imported through ALL's own
  `ImportModpackActivity`; installed as `Haste 1.21.1 SafeMode QA[Fabric]Haste-1.21.1`.
- Marker: `ALLLauncher/modpack.json` written by the product — `name=[Fabric]Haste-1.21.1`,
  `platform=Modrinth`, `mods=29` (all with sha1+url), `modsComplete=false`.
  `false` is correct: the instance has 31 jars because the pack ships 2 jars inside `overrides/` →
  ownership cannot be fully enumerated → conservative mode for that instance.
- Clean pack: `2 problem(s) [WRONG_MINECRAFT_VERSION x2]` (pack ships two 1.21 builds in a 1.21.1
  instance) → dialog offers **no Fix**, only Cancel / launch-anyway; every reason carries
  "This mod is managed by the modpack, so ALL won't change it automatically." No pack-owned
  upgrade/downgrade/replacement/delete attempted.
- User-added mod (real `balm-fabric-1.20.1-7.3.43.jar` pushed into the instance): `3 problem(s)`,
  the added file also reported and also answered with the pack-managed reason → **behavior B**
  (conservative, ambiguous ownership). Behavior A is the `modsComplete=true` path and is covered by
  `ModpackSafeModeTest`. The added jar was removed afterwards.
- Integrity: mods sha1 list before == after (31 jars) → no pack-owned file was mutated by either pass.
- Not done: no Minecraft certification (both passes stop at the Mod Doctor verdict; no MAIN_MENU claim).
  Archived `D:\ALL Launcher Releases\0.0.1\` untouched.
- QA notes (not product defects): the version-name prompt is pre-filled with the pack name and the
  runner typed into it without clearing → doubled instance name (harness now reads the name back);
  the shared runner's default device `127.0.0.1:5555` is not attached in this session.


## MOD_DOCTOR_REAL_LAUNCH_GATE_FIX (2026-09-20 17:05–17:45 +07:00)

**User-reported failure:** ALL let Minecraft launch with an incompatible mod installed — Mod Doctor
did not protect the real launch flow.

### Root cause (proven by tracing every path into `RealLaunch`)

The Mod Doctor check lived in exactly one place: the `TryLaunch` branch of the launcher's launch state
machine (`LaunchGameOperation(...)` in `ui/screens/content/elements/LauncherElements.kt`). `RealLaunch`
is the operation that actually starts the game, and two other operations transitioned **straight** to
it, so a user could reach the game with a completely unvalidated mod set:

| from | where | before |
|---|---|---|
| `UnsupportedPlugins.onConfirm` ("launch anyway" for an unsupported native plugin) | `LauncherElements.kt:327` | → `RealLaunch` directly, Mod Doctor never ran |
| `RendererNoStoragePermission.onDialogCancel` (user refuses the storage permission) | `LauncherElements.kt:291` | → `RealLaunch` directly, Mod Doctor never ran |

Same class as the earlier renderer-"Proceed" bypass that had already been fixed for
`UnsupportedRenderer` (which now re-enters `TryLaunch`).

### Fix (smallest change that makes the invariant structural)

- `LaunchGameOperation.RealLaunch` gained `modCheckPassed: Boolean = false`.
- The `RealLaunch` branch is now a choke point: when the flag is absent it runs `ModDoctor().inspect`
  and, on a blocking verdict, shows `ModProblems` and returns **without** calling
  `LaunchGameViewModel.start()`. Nothing can reach Minecraft while a blocking result stands.
- Only three call sites pass `modCheckPassed = true`: the `TryLaunch` path right after a clean report,
  the post-fix re-check after a successful repair, and the `ModProblems` "still launch" action — the
  product's one intentional, explicit user override.
- Account continuations (relogin / refresh-failed retry) continue an attempt that was already let
  through, so they carry the flag instead of re-inspecting.
- New pure gate in `game/version/mod/ModDoctor.kt`:
  `decideLaunchGate(report, userOverride = false)` → `LaunchGateDecision.LAUNCH | BLOCK`
  (only a clean report, or the explicit override, may launch).

### Files changed

- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/version/mod/ModDoctor.kt`
  (`LaunchGateDecision`, `decideLaunchGate`)
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/elements/LauncherElements.kt`
  (`RealLaunch.modCheckPassed`, gate in the `RealLaunch` branch, `TryLaunch` + post-fix +
  override call sites pass the flag)
- `ZalithLauncher/src/test/java/com/movtery/zalithlauncher/game/version/mod/ModDoctorLaunchGateTest.kt` (new)

### Tests

`./gradlew :ZalithLauncher:testDebugUnitTest --tests 'com.movtery.zalithlauncher.game.version.mod.*'`
→ **119 passed / 0 failed / 0 errors** (new `ModDoctorLaunchGateTest` 7: clean → LAUNCH;
wrong loader / wrong MC / missing required dependency / duplicate → BLOCK; override → LAUNCH;
pack-managed instance with a real incompatibility → still BLOCK). `git diff --check` rc 0.

### Practical QA on the real app (LDPlayer `emulator-5554`, x86_64)

Instance A: `Haste 1.21.1 SafeMode QA[Fabric]Haste-1.21.1` (pack-managed Fabric 1.21.1, 31 mods) with a
controlled incompatible mod added: `Jade-1.21.1-NeoForge-15.10.6.jar` (genuine NeoForge 1.21.1 build →
wrong loader only). Instance B (clean): `1.21.11 NeoForge 21.11.45` (certified 6-mod fixture).

| case | Mod Doctor | problem UI | `:game` process | MAIN_MENU marker |
|---|---|---|---|---|
| bad mod, Launch pressed | 3 problems (`WRONG_LOADER` + 2× `WRONG_MINECRAFT_VERSION`) | shown ("Minecraft launch paused.") | **not started** | 0 |
| bad mod, before the fix (same device/instance) | same 3 problems | shown | not started | 0 |
| clean instance, bad mod removed | clean | none | **started** (`com.all.launcher.app.debug:game`) | 1 |

The controlled bad mod was removed after each run; the instance's mod set is back to the pack's own 31
jars. Note: the bad-mod case already blocked on the pre-fix build through the normal Play button, i.e.
the user's failure did not reproduce on that path — it requires one of the two ungated continuations
above (or the user's own tap on the explicit "still launch" action), and those paths are now closed by
the choke point. Neither of the two bypass dialogs can be triggered on this emulator (no native plugin
with a version constraint is installed), so their closure is proven by code trace + the choke point +
regression tests rather than by a UI reproduction.



## 0.0.1 FREEZE — GIT_IDENTITY_REQUIRED (2026-09-20 19:36 +07:00)

- **Archive: already done, verified, untouched.** `D:\ALL Launcher Releases\0.0.1\` holds
  `ALLLauncher-0.0.1-arm64-v8a.apk` (187,174,100 B, SHA-256 `3BD3E79D…BAB9` — re-hashed this run, and the
  copy's hash equals the source APK's hash), `ALLLauncher-0.0.1-SOURCE.zip` (2548 entries, SHA-256
  `8D38DEF6…6095`, taken 16:01), `RELEASE_INFO.txt`, `SHA256.txt`, `SOURCE_SNAPSHOT_INFO.txt`. Nothing in
  that folder was added, changed or removed by this run.
- **Git freeze: blocked at `GIT_IDENTITY_REQUIRED`.** `git config --get user.name` / `user.email` return
  nothing at local *and* global scope, and `git config --list --show-origin` contains no `user.*` entry at
  all. Per the freeze brief that is a stop: no identity was invented, no git config was written, **no commit
  and no tag `v0.0.1-rc1` were created**, and no git bundle exists.
- **Premise correction (measured, not assumed).** The freeze brief assumes the worktree is the 0.0.1 source.
  It is not: `gradle.properties` was rewritten at **17:54:02**, three hours after the 0.0.1 RC was built at
  **14:39:18**, and now reads `launcher_version_code=2` / `launcher_version_name=0.0.2-beta1`; product source
  under `src/` was edited at 18:20–18:32. So the exact 0.0.1 source revision is **not reconstructible from
  the worktree** — the 16:01 source ZIP is the closer snapshot, and tagging today's tree `v0.0.1-rc1` would
  put a 0.0.2-beta1 label on a 0.0.1 tag. That call is the user's, so it was not made.
- Uncommitted work remains: 29 modified tracked files + 15 untracked entries. `HEAD` = `b4c9415b`; the only
  tags in the repo are upstream's, newest `2.5.3`.

## PUBLIC_SHIPPING_GATE_0_0_2_BETA1 — Forge gate PASS, ARM64 Release built/audited/archived (2026-09-20 19:36–19:55 +07:00)

Fabric practical gate: **PASS — not repeated** (Fabulously Optimized v6.5.0, MC 1.21.1, Fabric 0.19.3;
`fabric-fo-fixed-verdict.txt`: Mod Doctor clean, 48 jars, `MAIN_MENU`, 151 loaded mods, pack files unchanged).
Bad-mod launch gate: **PASS — not repeated** (Jade NeoForge inside Fabric; `:game` never started).

**Forge practical gate: PASS.** Real modpack import through ALL's own `ImportModpackActivity`, Debug package,
`emulator-5554`:

- pack `Optimized fps 1.20.1` (Modrinth, 22 declared mods, 25 jars installed), instance
  `OFP 1.20.1 BetaOptimized fps 1.20.1`, loaders resolved by the product as **`Minecraft Info: 1.20.1,
  Forge - 47.4.3`**.
- Mod Doctor: `inspect started` → `inspect finished: 0 problem(s) []` — clean.
- Real Forge ran: `ModLauncher 10.0.9 starting` · `Forge mod loading, version 47.4.3, for MC 1.20.1` ·
  `MinecraftForge v47.4.3 Initialized` · 25 × `Loading mod:` · `Setting user: ALLQA001` ·
  `OpenAL initialized on device OpenSL` · `Sound engine started` · texture atlases created ·
  `Mixing … into net.minecraft.client.gui.screens.TitleScreen` · `Game took 35.96 seconds to start`.
- `pack-owned files: UNCHANGED` (identical SHA-1 sets before and after), `CrashReports: 0`,
  `LauncherFatals: none`, `GameProcess: 1`.
- **The previous Forge failure was a QA-harness defect, not the Maven/emulator timeout the brief assumed.**
  `01-fo-state.log` shows the earlier run hit `SKIP import … already installed` because the Forge run reused
  the *Fabric* run's `-NamePrefix` and matched the Fabric instance instead of importing; and
  `qa-pack-import.ps1:91-92` used `\"` escapes inside a PowerShell double-quoted string, which PowerShell
  5.1 does not treat as an escape, so the summary block died on a positional-parameter bind. Both fixed:
  distinct prefix (`OFP 1.20.1 Beta`) and single-quoted grep patterns. `20-forge-ofps2-import.log`,
  `21-forge-ofps2-launch.log`.
- Harness limits recorded, not hidden: `LoadedMods: 0` in the verdict is a parsing gap (the mod-count regex
  matches Fabric's log shape, not Forge's) — the real count is 25, visible in the `Loading mod:` lines and the
  25-jar mods listing. Known-benign log noise, none of it a product failure: JNA/oshi `libc.so.6`, the
  `flite` narrator, `Failed to retrieve profile key pair` and the Realms auth line (offline local account),
  mixin `minVersion` warnings, and the cosmetic `BatchDownloader Completed-count mismatch`.

**ARM64 Release 0.0.2-beta1: BUILT, AUDITED, ARCHIVED.**
`D:\ALL001` with `JAVA_HOME=C:\jdk21`, `:ZalithLauncher:assembleRelease -Darch=arm64 --no-daemon`:
`BUILD SUCCESSFUL in 7m 37s`, exit 0, with `configureNdkBuildRelease[arm64-v8a]`,
`buildNdkBuildRelease[arm64-v8a]`, `minifyReleaseWithR8`, `lintVitalAnalyzeRelease`, `lintVitalRelease`,
`optimizeReleaseResources` and `packageRelease` all **executed** (not UP-TO-DATE). Log:
`test-artifacts/public-beta/98-release-build-002.log`. The 0.0.1 RC remains untouched at its own path and in
its archive.

- `ALLLauncher-0.0.2-beta1-arm64-v8a.apk` · 187,188,460 B · 2026-09-20 19:46:40 ·
  **SHA-256 `66E7552C97F7142FE1A1729FAD0940D700A7496B8E46CD1FBCC1BA14E9AFF5F4`**
- `com.all.launcher.app` · versionName `0.0.2-beta1` · versionCode `2` · `native-code: 'arm64-v8a'` only ·
  not debuggable · zipalign verified · signature verifies (v2, single signer)
- certificate SHA-256 `B72B76BD56F32AE16A3C10BAC9FAD1BD68A8D48ADA273FCF4530F854089D2177` =
  `B7:2B:76:BD:…:9D:21:77` — **exact match**
- provenance: latest product source edit 18:32:38 < APK mtime 19:46:40, so the artifact postdates its source
- archive: `D:\ALL Launcher Releases\0.0.2-beta1\` — APK + `SHA256.txt` + `VERIFY.txt`
  (`CertificateMatch: True`); archived APK re-hashes to `66E7552C…`

**Previous R8 interruption — cause established.** Not OOM and not an R8 failure: no `OutOfMemory`,
`Java heap space`, `GC overhead` or kill line exists in any `9.5.0` daemon log, and `90-release-build.log`
ends abruptly at the `minifyReleaseWithR8` task line with no `BUILD SUCCESSFUL`/`FAILED`, while
`daemon-9812.out.log` records *"the daemon has exited normally or was terminated in response to a user
interrupt"* (and `daemon-9020.out.log` is 0 bytes). That is an externally interrupted session, so **no
memory or R8 setting was changed** — the rebuild was run with the ordinary command and succeeded.

**Release smoke: `ARM64_RELEASE_DEVICE_SMOKE_PENDING`.** `adb devices` shows only `emulator-5554`
(x86_64, `ro.product.cpu.abi=x86_64`); the physical iQOO `10AF681QSC001YK` is not attached, and an x86_64
emulator cannot install or validate an arm64-only artifact. Not claimed as a pass.

**QA-only source changes this run (no product behaviour touched):** `qa-pack-import.ps1` escaped-quote fix;
`verify-and-archive.ps1` certificate extraction — the old regex stripped non-hex from the whole
`Signer #1 certificate SHA-256 digest: …` line, so the label's hex letters leaked in and the record read
`CertificateMatch: False` while the real certificate was an exact match; it now slices after the last colon.
`VERIFY.txt` was regenerated and reads `CertificateMatch: True`.

Known limitations carried forward: the 0.0.2-beta1 artifact has no physical-device smoke; the whole product
line is still uncommitted; and the Forge/Fabric QA instances plus the staged `.mrpack` files were left on the
emulator (deleting them was not done unasked).

Exact next milestone: **attach the iQOO `10AF681QSC001YK`, `adb install -r` the archived
`ALLLauncher-0.0.2-beta1-arm64-v8a.apk` (`66E7552C…`) without clearing data, and run one full Release
smoke** — launch, no Zalith update popup, existing account/instance intact, one known-good Fabric 1.21.11
launch to `MAIN_MENU`, no FATAL/ANR. No build and no source change is needed first. (The git freeze stays
blocked until a git identity is supplied.)


## THAI_LOCALIZATION_V1 — all translatable keys translated; device-verified on LDPlayer (2026-09-20 20:05–20:45 +07:00)

**STATUS: NOT PASS.** The resource work is complete and verified on a real device, but three conditions of
the Thai-localization V1 acceptance are unmet: Mod Doctor's problem labels are still hardcoded English, and
the "one real launch" and "one Mod Doctor blocking dialog" regressions were not re-run this session. Exact
numbers and gaps below; nothing is claimed beyond what was measured.

### Inventory (deterministic, from an audit script — not a manual scan)

| measure | before | after |
|---|---|---|
| base keys total (`values/strings.xml` + `values/terracotta.xml`) | 1518 | 1518 |
| base translatable | 1500 | 1500 |
| base `translatable="false"` | 18 | 18 |
| Thai keys (`values-th/…`) | 468 | **1518** |
| **MISSING_THAI_KEYS** | **1032** | **0** |
| **PLACEHOLDER_MISMATCH** | 0 | **0** |
| **EMPTY_THAI_VALUE** | 0 | **0** |
| **ORPHANED_THAI_KEYS** | 0 | **0** |

Audit tool: `test-artifacts/localization-th/audit-thai.ps1` (writes `audit-report.txt`). It parses both files
with `[xml]` (so a malformed resource fails the audit), skips `translatable="false"`, joins array/plurals
items for the placeholder comparison, and normalises placeholders to `%(n$)?[sdf]` so a reordered or dropped
one is caught. The empty-value rule was corrected mid-run: an empty Thai value is only a defect when the base
has text, because a resource that is empty in the base must stay empty rather than gain invented filler.

Placeholders preserved exactly, including numbered and multi-placeholder forms:
`mods_update_updating (%1$d/%2$d, %3$s)` · `minecraft_download_downloading_game_files (%1$d/%2$d, %3$s/%4$s)`
with its literal `\n` · `settings_game_java_memory_used_text (%1$dMB / %2$dMB)` ·
`versions_manage_cleanup_success (%1$d … %2$s)` · `versions_update_loader_diff_change (%1$s %2$s → %3$s)` ·
`screenshots_manage_export_selected_message (%1$d)`.

### Coverage added (1032 strings across the whole UI)

accounts/Microsoft flow (112) · version settings, modpack export, version management, loader update (137) ·
settings: controls, game, gamepad, launcher, renderer (218) · control-layout editor (157) · file manager
(150) · mods and mod updates, Mod Doctor dialog chrome, download, server errors, About (134) · cape names,
saves, servers, screenshots, Java runtimes, resource/shader packs, festivals, notifications, plugins (124).
Style follows the brief's examples rather than source grammar: `mods_manage_no_loader` →
"เวอร์ชันนี้ไม่มีตัวโหลดม็อด จึงใช้ม็อดไม่ได้", `mods_manage_delete_warning` →
"ต้องการลบม็อด %s ใช่ไหม การลบย้อนกลับไม่ได้", `upgrade_get_remote_failed` →
"ตรวจสอบอัปเดตไม่สำเร็จ กรุณาลองใหม่ภายหลัง". Established proper nouns kept as-is (Minecraft, Java, Fabric,
Forge, NeoForge, Quilt, Modrinth, CurseForge, Sodium, Iris, OpenGL, Vulkan, FPS, RAM, Microsoft, Xbox,
Mod Doctor, ALL); `Mod`→ม็อด, `Modpack`→ม็อดแพ็ก, `Launcher`→ลอนเชอร์; language names kept as their own
endonyms. No filename, path, ID, version or technical identifier was translated.

### GAP 1 — Mod Doctor labels are still hardcoded English (the significant one)

`ModDoctorProblemType` (`ModDoctor.kt:140-147`) carries its user-facing label as a Java string literal:
`MISSING_REQUIRED_DEPENDENCY("Missing required dependency")`, `WRONG_LOADER("Wrong mod loader")`,
`WRONG_MINECRAFT_VERSION("Wrong Minecraft version")`, `DUPLICATE("Duplicate mod")`,
`CORRUPT_METADATA("Unreadable/corrupt mod metadata")`, `INCOMPATIBLE_DEPENDENCY_VERSION("Incompatible
dependency version")`. There is **no** `mod_doctor_*` resource for them (checked: no matches in
`values/`), and the detail sentences are built the same way (`ModDoctor.kt:1010`, `:1073`, `:1237`). A Thai
user therefore still sees the English reason line in the Mod Problems dialog — the very place the brief calls
out as most important. This was **not** migrated: it means touching Mod Doctor's own strings, which the brief
places off-limits, and it deserves the separate decision about whether those labels should become
display-localized or stay as stable technical diagnostics.

### GAP 2 — the two functional regressions were not re-run

No Minecraft launch and no Mod Doctor blocking dialog were exercised on the Thai build this session. The
install (233 MB) went onto the existing Debug package without clearing data, and the previously installed
`OFP 1.20.1 BetaOptimized fps 1.20.1` Forge instance is still present and selected, but launching it was not
part of this run. Earlier milestones already prove both behaviours on the same source line, but that is
inheritance, not a fresh Thai-build result.

### GAP 3 — the pre-existing 468 Thai strings were not systematically rewritten

New strings were written to the brief's style; the 468 that already existed were sampled (they read
naturally, e.g. `generic_cancel` → "ยกเลิก", `generic_cannot_empty` → "ข้อมูลไม่สามารถว่างเปล่าได้") but no
full rewrite pass was performed, so "no obvious machine-like Thai remains in core screens" is asserted from
inspection of the screens below, not from a line-by-line review of the older half.

### Deliberately not translated

- `chinese_festivals.xml` (5 strings: `festivals_spring`, `_qing_ming`, `_dragon_boat`, `_mid_autumn`,
  `_national_day`) — the file declares `tools:ignore="MissingTranslation"` and holds Chinese holiday
  greetings with a `values-zh-rTW` variant only. Chinese-only by design; adding Thai would be wrong.
- 18 base resources marked `translatable="false"` (listed in the audit report) — Android would not use a
  translation for them.
- `themes.xml` (0 strings, styles only) and `ic_launcher_background.xml` (a colour).

### Real-device QA (LDPlayer `emulator-5554`, x86_64, Debug package `com.all.launcher.app.debug`)

Installed with `adb install -r` — **Success**, no uninstall and no data clear. Screens inspected through the
real UI (`uiautomator` dumps kept as `ui-01-home.xml` … `ui-05-after-restart.xml`):

- **Home** — `คำเตือน` + the full experimental-build warning, account `ALLQA001` / **`ออฟไลน์`**
  (a string added by this run), the instance row, `เริ่มเกม`.
- **Download → Game** — tabs `ดาวน์โหลด · เกม · มอดแพ็ค · มอด · รีซอร์สแพ็ค · โลก · เชเดอร์แพ็ก`,
  `ค้นหาด้วยรหัส`, `เวอร์ชันทางการ` / `สแนปชอต` / `เอพริลฟูลส์` / `เวอร์ชันเก่า`, and Thai date
  formatting (`15, ก.ย., 2026, 6:23:02`).
- **Settings → Renderer** — tab strip `การตั้งค่า · ตัวเรนเดอร์ · เกม · การควบคุม · เกมแพด · ลอนเชอร์ ·
  Java · เกี่ยวกับ`, plus `ตัวเรนเดอร์หลัก` and its description, `เลือกไว้: Krypton Wrapper`,
  `ไดรเวอร์ Vulkan`, `Graphics API`, `สเกลความละเอียด`, `เกมเต็มจอ`.
- **Settings → About** — `เกี่ยวกับลอนเชอร์`, `เวอร์ชันลอนเชอร์: 0.0.2-beta1-debug`, `ลิงก์โปรเจกต์`,
  `ผู้พัฒนา ALL Launcher`, `สนับสนุนผู้พัฒนา`, `กิตติกรรมประกาศ`, and the acknowledgement lines
  (`ALL ใช้แหล่งมิเรอร์ BMCL API`, `ALL นำโค้ดจาก FCL มาใช้ และอ้างอิงการทำงานบางส่วน`, …).
- **Persistence** — `am force-stop` then relaunch: Thai still active, account and instance intact, no
  re-login.

No clipped text, no placeholder garbage and no broken layout was seen on those screens. Screens not
inspected: Accounts/Add Account, Mod Doctor dialog, instance detail, Mods, Modpack area, the game-version
download flow, and any error/warning dialog.

### Files changed

- `ZalithLauncher/src/main/res/values-th/strings.xml` — 468 → 1518 keys (the only product file touched).
- `test-artifacts/localization-th/audit-thai.ps1` (new, QA infrastructure), `audit-report.txt`,
  `th-debug-build.log`, `ui-01…ui-05` dumps.

`git diff --check` rc 0. No source outside `values-th/` was modified: no code, no logic, no version number,
no signing, no frozen archive.

### Debug build

`cd D:\ALL001` · `JAVA_HOME=C:\jdk21` · `:ZalithLauncher:assembleDebug -Darch=x86_64 --no-daemon` →
**BUILD SUCCESSFUL in 2m 31s**, which is also the resource-compilation proof (the Thai resources compile).
`ALLLauncher-Debug-0.0.2-beta1-x86_64.apk` · 233,487,306 B · 2026-09-20 20:32:24 ·
**SHA-256 `66565FFED4C7A358B19430D622FFD9324DFC40BC2C441FA50CFCA4A5819D8FD7`**.

### Exact next milestone

Finish the three gaps in this order: (1) decide whether Mod Doctor's `displayName` labels and its reason
sentences become localized resources, and if so add `mod_doctor_*` strings for all six types and route the
dialog through them — this is the one remaining visible English in a core screen; (2) with that build, run
one real Minecraft launch on the Thai build to `MAIN_MENU` and one real Mod Doctor blocking dialog (add a
single wrong-loader jar to the existing Forge instance, confirm the Thai dialog blocks the launch, then
remove it), which closes both functional conditions; (3) only then a second Thai review pass over the
pre-existing 468 strings.


## THAI_LOCALIZATION_V1 — Mod Doctor localised, language picker verified; clean launch blocked by a pre-existing mod-set conflict (2026-09-20 20:50–21:18 +07:00)

**STATUS: NOT PASS.** Two of the three closing goals are done and verified on the real device — Mod Doctor's
user-visible text is localised, and the in-app language picker was explicitly exercised. The third cannot be
signed off: the clean state does **not** reach `MAIN_MENU`, because the QA instance's own mod set is rejected
by the Fabric loader. That failure is **not** caused by this change and is not a localisation defect; see
below for the exact evidence.

### Mod Doctor localisation (presentation only)

`ModDoctorProblemType` now carries `labelRes`, and `ModDoctorProblem` gained three optional presentation
fields: `reasonRes`, `reasonArgs`, `reasonSuffixes: List<ModDoctorReasonSuffix>`. The English `reason` string
is **kept exactly as it was** and still accumulates the same way, so 122 existing tests and every log line read
the identical English text they did before; only the UI resolves resources.

- 18 new resources in `values/strings.xml` (English) and `values-th/strings.xml` (Thai): 6 labels, 8 core
  reasons, 2 pack-managed sentences, the whole-line template, plus the repair-availability suffix.
- The whole line is built by one helper, `modDoctorProblemText(context, problem)` in `LauncherElements.kt`,
  which falls back to the English `reason` if a resource is ever 0 — the dialog can never render blank.
- English values reproduce the previous text byte for byte, including `File: %3$s`; Thai uses `ไฟล์: %3$s`.
- **Detection, severity, launch blocking, fix-candidate generation, Modpack Safe Mode, dependency logic and
  loader logic were not touched** — the change adds fields and reads them in the UI.

Thai wording (matching the brief's examples): `ม็อดที่จำเป็นยังไม่ครบ` · `ม็อดนี้ทำมาสำหรับตัวโหลดอื่น` ·
`ม็อดนี้ใช้กับ Minecraft เวอร์ชันนี้ไม่ได้` · `พบม็อดซ้ำ` · `อ่านข้อมูลของม็อดนี้ไม่ได้` ·
`เวอร์ชันของม็อดที่จำเป็นใช้ด้วยกันไม่ได้`, and details read
`%1$s เผยแพร่สำหรับ Minecraft %2$s ไม่ใช่ %3$s`.

Compile errors caught and fixed on the way: `LauncherElements.kt` had referenced `Context` and
`ModDoctorProblem` only by inference, so both needed explicit imports once named in a signature.

### Audit and tests

- Thai audit: base total 1536 / translatable 1518 / Thai 1536 · **MISSING_THAI_KEYS = 0 ·
  PLACEHOLDER_MISMATCH = 0 · EMPTY = 0 · ORPHANED = 0** (XML well-formedness is enforced by the audit's
  `[xml]` parse).
- `:ZalithLauncher:testDebugUnitTest --tests '…game.version.mod.*'` → **BUILD SUCCESSFUL, 122 tests, 0 failures,
  0 errors** (ModDoctorTest 35, InstalledDependency 15, ModpackSafeMode 11, CandidateCompatibility 10,
  MinecraftRange 10, ReplacementRepair 10, LaunchGate 7, NeoForgeMetadata 6, StableFirst 5, ForgeMetadata 5,
  MultiLoaderJar 3, ProvidedModId 3, FileDisableSemantics 2).
- `git diff --check` rc 0.

### Debug

`ALLLauncher-Debug-0.0.2-beta1-x86_64.apk` · 233,497,978 B · 2026-09-20 21:02:14 ·
**SHA-256 `175EE17B19D3F9C52F03359DCB8E9DF141C4AC984C99390DF971F17A3E821792`** · `adb install -r` →
Success, no uninstall, no data clear.

### In-app language picker — PASS (explicitly, not via system locale)

The picker was opened from Settings ▸ Launcher ▸ ภาษา. It reported `เลือกไว้: ตามการตั้งค่าระบบปฏิบัติการ`
(selected: follow system), confirming the app was **not** already pinned to Thai and that the earlier Thai UI
came from the system locale.

- Tapping **English** turned the whole UI English (`Settings - Launcher`) — the selector demonstrably drives
  the app language.
- Scrolling to and tapping **ไทย** turned it back to Thai.
- `am force-stop` then relaunch: **Thai persists**; account and instance intact.
Dumps: `ui-12-language-picker.xml`, `ui-13-after-english.xml`, `ui-14-picker-scrolled.xml`,
`ui-15-picker-thai.xml`, `ui-16-after-thai.xml`, `ui-17-after-restart-th.xml`.

### Mod Doctor real-device QA — PASS for localisation and for blocking

Controlled case: `balm-fabric-1.20.1-7.3.43.jar` (a genuine **Fabric 1.20.1** build) pushed into the plain
`1.21.11 Fabric 0.19.5` instance beside its seven real mods. One press of `เริ่มเกม` produced:

```
ปัญหาม็อด
หยุดเปิด Minecraft ไว้ก่อน พบปัญหาม็อด 1 รายการ
ม็อดนี้ใช้กับ Minecraft เวอร์ชันนี้ไม่ได้: Balm เผยแพร่สำหรับ Minecraft 1.20 or 1.20.1 ไม่ใช่ 1.21.11
ไฟล์: balm-fabric-1.20.1-7.3.43.jar
ยกเลิก · แก้ไข · ใช้ไปก่อนเถอะ เนอะ
```

Label, reason and the `ไฟล์:` prefix are all Thai and all come from the new resources; the mod name, the
declared versions and the file name stay technically exact; no placeholder garbage; the layout is readable.
`ps -A` showed only `com.all.launcher.app.debug` and `:filemanager` — **`:game` never started**.
Evidence: `ui-20-moddoctor-th.xml`. Cancel was used; the fixture was then deleted and the mods directory
verified byte-identical to its pre-test seven files.

### Clean recovery — blocked, and the reason is a pre-existing product gap

Mod Doctor **did not** block the restored set (no dialog), and the launcher went on to start the game — so the
localisation change did not break the gate. The JVM then exited with code 1, reproducibly (two launches), and
the launcher showed its own Thai crash screen (`Java Virtual Machine ปิดด้วยรหัส 1`,
`ประเภทการแครช: ปัญหาจากระบบเกม`).

The cause is in the instance, not the change — `latest.log`:

```
Loading Minecraft 1.21.11 with Fabric Loader 0.19.5
Mod resolution failed
Immediate reason: [HARD_DEP iris 1.10.7+mc1.21.11 {depends sodium @ [0.8.x]},
                   NEG_HARD_DEP sodium 0.8.14+mc1.21.11 {breaks iris @ [<=1.10.7]}, ROOT_FORCELOAD_SINGLE iris …]
Reason: [NEG_HARD_DEP sodium 0.8.14+mc1.21.11 {breaks iris @ [<=1.10.7]}]
Incompatible mods found! ... Mod 'Sodium' (sodium) 0.8.14+mc1.21.11 is incompatible with version 1.10.7
or earlier of mod 'Iris' (iris), yet a conflicting version is present: 1.10.7+mc1.21.11!
```

This is precisely the certification invariant *"no tested case has Mod Doctor = 0 followed by loader
dependency-resolution failure"* being violated on this instance: Mod Doctor reports 0 problems while the real
Fabric loader rejects the set. The mechanism is the gap the earlier team run identified and **deliberately
left out of scope** — Mod Doctor evaluates `depends` between installed mods but not **`breaks` between two
installed mods**. Nothing about localisation is involved: the pair Sodium 0.8.14+mc1.21.11 / Iris
1.10.7+mc1.21.11 is simply an incompatible pair that Mod Doctor does not flag.

So `Minecraft :game starts` / `MAIN_MENU` is **NOT PROVEN** on the Thai build, and the honest reason is this
instance-level conflict, not the change under test.

### Files changed

- `ZalithLauncher/src/main/res/values/strings.xml` (18 English resources)
- `ZalithLauncher/src/main/res/values-th/strings.xml` (18 Thai resources)
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/version/mod/ModDoctor.kt` (enum `labelRes`,
  `ModDoctorReasonSuffix`, three `ModDoctorProblem` fields, 15 construction/copy sites)
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/elements/LauncherElements.kt`
  (two imports, `modDoctorProblemText`, one text site)
No behaviour, severity, blocking, fix generation, Safe Mode, version, signing or frozen-archive change.

### Remaining intentional English

Minecraft, Java, Fabric, Forge, NeoForge, Quilt, Modrinth, CurseForge, Sodium, Iris, OpenGL, Vulkan, FPS,
RAM, Microsoft, Xbox, Mod Doctor, ALL, and all filenames/IDs/versions/URLs, plus the 17 language endonyms.
`chinese_festivals.xml` stays Chinese-only by design. The one remaining English surface inside a Thai
sentence is the dynamic provider detail substituted into
`mod_doctor_reason_repair_unavailable` (`ซ่อมอัตโนมัติใช้ไม่ได้: %1$s`), which is the fix-failure path.

### Exact next milestone

**The `breaks`-between-installed-mods gap is now the top product item** — it is a real Mod Doctor hole that
lets an ordinary user press play on a set the loader will reject, and the certification invariant already
claims it should not. (1) Add `breaks` evaluation between enabled installed mods to `ModDoctor`, as a
report-only problem with no automatic fix, reusing the existing version-predicate evaluator (RED first).
(2) Rebuild and re-run this exact QA on a **known-good** instance so the clean-launch and `MAIN_MENU`
conditions can finally be closed. (3) Only then re-run the Thai localisation final report.


## MOD_DOCTOR_BREAKS_AND_THAI_V1 — PASS (2026-09-20 21:30–22:05 +07:00)

### Proven product bug and root cause

On the real `1.21.11 Fabric 0.19.5` instance, enabled `Sodium 0.8.14+mc1.21.11` declares
`breaks iris <=1.10.7`; enabled `Iris 1.10.7+mc1.21.11` matches. Before this fix Mod Doctor reported
0 problems, launched the JVM, then Fabric Loader rejected the set with `NEG_HARD_DEP`. Root cause:
`readFabricMetadata` carried only `depends`; installed-mod validation never received or evaluated Fabric
`breaks`. The staged download-candidate path had a private breaks check, but that did not inspect the set
already installed on disk.

### RED → GREEN

New `ModDoctorInstalledBreaksTest` covers six behaviours: matching predicate blocks; non-matching predicate
stays clean; absent target stays clean; disabled target (absent from the enabled validation set) stays clean;
normal `depends` presence/version behaviour stays unchanged; pack-safe mode keeps the real conflict visible,
blocking and non-fixable.

- Structural compile setup first added only the metadata field and enum value, with no evaluation.
- Behavioral RED: `5 tests completed, 1 failed`; only
  `matchingInstalledBreaksPredicateIsBlocking` failed. `breaks-red-behavior.log`.
- Focused GREEN: all breaks tests passed after the implementation.
- Full `game.version.mod.*`: **128 tests, 0 failures, 0 errors** (the previous 122 + 6 breaks tests).
  `breaks-full-green.log`.

### Implementation (report-only)

- `ModDoctorMetadata.breakPredicates: Map<String, List<String>>` carries Fabric `breaks` target id → original
  predicate list.
- `readFabricMetadata` parses `fabric.mod.json.breaks` with the same `asStringList` path used by `depends`.
- `validateModDoctorMetadata` looks up enabled installed targets, requires known source/target versions, and
  calls the existing shared `versionPredicateDecision` / `fabricVersionPredicateMatches` path. A matching
  target produces blocking `INCOMPATIBLE_MOD_PAIR`; non-match, absent target, unknown version or unreadable
  predicate produces no false positive.
- New problem has **no** dependency id, fix candidate, replacement candidate or restore file: no deletion,
  downgrade, replacement or version shopping. `isReplaceable()` remains false.
- Pack safe mode does not remove it; the explicit test confirms it still blocks and `hasFixableProblems=false`.
- Presentation: `Incompatible mods` / `ม็อดสองตัวนี้ใช้ร่วมกันไม่ได้`, with exact source and target names and
  versions as resource arguments (`%1$s %2$s … %3$s %4$s`). Raw predicate syntax is not the only explanation.

### Thai corrections and audit

- Missing-dependency label is exactly `ม็อดที่จำเป็นยังไม่ครบ`.
- Launch Anyway is now `เล่นต่อทั้งที่ยังมีปัญหา` (replaces `ใช้ไปก่อนเถอะ เนอะ`).
- Audit: base total 1538 / translatable 1520 / Thai 1538 · **missing 0 · placeholder mismatch 0 · empty 0 ·
  orphaned 0**; XML parse succeeds. `git diff --check` rc 0.
- Explicit picker proof from the previous run remains valid and persisted through this install/restart:
  English was selected and visibly changed the app to English, then `ไทย` was explicitly selected;
  after `force-stop` + reopen and after this `install -r`, Home remains Thai.

### Debug artifact

`ALLLauncher-Debug-0.0.2-beta1-x86_64.apk` · 233,499,122 B · 2026-09-20 21:56:02 ·
**SHA-256 `A963D0198A3491D5CB263B5F009DCDC4F84E0C6AE74681DF7AAAFFC9C28370A7`** ·
`adb install -r` → Success; no uninstall or data clear.

### Real bad-state QA — PASS

Real current failing instance, through ALL's actual Home → `เริ่มเกม`:

```
ปัญหาม็อด
หยุดเปิด Minecraft ไว้ก่อน พบปัญหาม็อด 1 รายการ
ม็อดสองตัวนี้ใช้ร่วมกันไม่ได้: Sodium 0.8.14+mc1.21.11 ใช้ร่วมกับ Iris 1.10.7+mc1.21.11 ไม่ได้
ไฟล์: sodium-fabric-0.8.14+mc1.21.11.jar, iris-fabric-1.10.7+mc1.21.11.jar
ยกเลิก · เล่นต่อทั้งที่ยังมีปัญหา
```

Technical names/versions/file names are exact, no placeholder garbage, layout readable. `ps -A` showed only
launcher + filemanager: **`:game` did not start**. Evidence `ui-31-breaks-dialog.xml`.

### Real known-good QA — PASS

Used the already-proven genuine Forge pack (not the conflicting Fabric set):
`OFP 1.20.1 BetaOptimized fps 1.20.1`, MC 1.20.1, Forge 47.4.3, 25 jars. The existing QA driver selected the
named instance and pressed Launch through the real ALL UI.

- Mod Doctor: clean; no dialog.
- Real loader: `Minecraft Info: 1.20.1, Forge - 47.4.3` · `Forge mod loading, version 47.4.3` ·
  `MinecraftForge v47.4.3 Initialized` · `Setting user: ALLQA001` · graphics initialized ·
  `OpenAL initialized on device OpenSL` · `Sound engine started` · `Game took 33.292 seconds to start`.
- Verdict: `OutcomeState: MAIN_MENU`, `GameProcess: 1`, `CrashReports: 0`, `LauncherFatals: none`, pack-owned
  files unchanged. Fresh `ps -A` confirms `com.all.launcher.app.debug:game` alive.
  `thai-clean-launch.log`, `test-artifacts/public-beta/thai-v1-clean-verdict.txt`,
  `thai-v1-clean-gamelog.txt`.

### Final state

**MOD_DOCTOR_BREAKS_AND_THAI_V1_PASS.** Installed enabled `breaks` conflicts are now caught before Fabric,
Thai V1's labels/reasons and wording are verified in the real dialog, the app language remains Thai after
restart/update, and a genuinely valid instance still launches to `MAIN_MENU`. No Release build, no version,
signing, Microsoft, renderer, or Modpack Safe Mode behaviour change. Frozen `0.0.1` archive untouched.


## MOD_DOCTOR_BREAKS_ONE_CLICK_FIX_V1 — BLOCKED: no safe source/target replacement exists for the real graph (2026-09-20 22:15–23:35 +07:00)

### Requested real conflict

MC 1.21.11 / Fabric 0.19.5: Sodium `0.8.14+mc1.21.11` breaks Iris `<=1.10.7`; installed Iris is
`1.10.7+mc1.21.11`. Mod Doctor already reports `INCOMPATIBLE_MOD_PAIR` and blocks launch.

### Implemented repair architecture

The new action reuses the existing authoritative fingerprint/project resolution, Modrinth version paging,
stable-first ordering, `DownloadSingle` staging/copy pipeline and shared Fabric predicate evaluator. It does
not add a downloader or parser.

- `ModDoctorBreaksFix(target, source?)`: target project first, source project as fallback.
- `withFixCandidates` resolves same-project identities for both installed files; a resolved plan makes the
  existing `แก้ไข` button appear.
- Candidate validation is now bidirectional: candidate `breaks` → installed set **and** installed
  `breaks` → candidate; existing installed `depends` predicates are also checked.
- Execution fetches candidates before mutation, reversibly renames the old file to `.disabled`, downloads
  into the existing staging area, validates the staged JAR, copies only the first accepted candidate, and
  restores the original on every rejection/failure. Per-candidate wait is bounded to 45 seconds so the UI
  cannot remain in `กำลังแก้ไข…` indefinitely.
- Target side is tried first; source side only after every target candidate is safely rejected.
- Report-only failure: no safe result removes `breaksFix`, adds the Thai explanation, and leaves the original
  files byte-identical. No deletion, manual selection, predicate suppression or fake success.
- Modpack Safe Mode is preserved: if either repair side is pack-owned, the new action is stripped and the
  existing pack-managed explanation is appended. No Safe Mode redesign.

### RED / GREEN

- Behavioral RED before reverse validation: new
  `installedModsBreaksRejectsTheConflictingTargetAndSelectsANewerOne` → **1 test, 1 failed**; old logic
  accepted Iris 1.10.7 because it checked only candidate→installed, not installed→candidate.
  `breaks-fix-red.log`.
- GREEN full `game.version.mod.*`: **129 tests, 0 failures, 0 errors** (previous 128 + candidate selection
  regression). Existing dependency Fix suite remains green. `breaks-fix-full-green.log`.
- Thai audit: base 1539 / translatable 1521 / Thai 1539; missing 0, placeholder mismatch 0, empty 0,
  orphaned 0; XML parse passes. `git diff --check` rc 0.

### Debug

Final safety build: `ALLLauncher-Debug-0.0.2-beta1-x86_64.apk` · 233,499,906 B ·
2026-09-20 23:24:49 · **SHA-256 `6D520A95525C9A8D92A38F44C37499F4E47BDC3590290AFB6B4889AFAB92768F`** ·
`adb install -r` Success; no uninstall/data clear.

### Real UI result — safe failure, not PASS

Through Home → Launch, the Thai dialog showed `แก้ไข`; one tap was performed. Provider/download evidence:

1. Target Iris project `YL57xq9U`: every available compatible 1.21.11 candidate is Iris 1.10.7 or older,
   and each is rejected because installed Sodium 0.8.14 declares it breaks that version.
2. Source Sodium project `AANobbMI`: Sodium 0.8.14/0.8.13 candidates still declare the same Iris break;
   Sodium 0.8.12 and older no longer satisfy installed Reese's Sodium Options
   `2.2.4+mc1.21.11`, which requires exactly Sodium `0.8.14+mc1.21.11`. Those candidates are rejected by
   the existing installed-dependency graph check.

Therefore the policy requested by this milestone — replace target, otherwise replace source, while respecting
the existing graph — has **no safe solution in the authoritative catalog**. A successful repair would require
changing a third mod (Reese's Sodium Options) as part of a multi-mod solver, which is outside the declared V1
source/target policy and cannot be silently invented.

The first build exposed an indefinite wait after the finite candidate walk. Focused safety fix: 45-second
per-candidate timeout + guaranteed restore + localised failure report. The real retry ended and showed:

```
ม็อดสองตัวนี้ใช้ร่วมกันไม่ได้: Sodium 0.8.14+mc1.21.11 ใช้ร่วมกับ Iris 1.10.7+mc1.21.11 ไม่ได้
ไม่พบเวอร์ชันที่ซ่อมให้อัตโนมัติได้อย่างปลอดภัย จึงคืนไฟล์เดิมไว้โดยไม่เปลี่ยนแปลง
```

The Fix button is removed after failure. All seven pre-fix SHA-1s equal the post-fix set exactly; original
Iris and Sodium are enabled under their original filenames, no `.disabled`, duplicate, `.part`, or replacement
JAR remains. `ui-40-fix-visible.xml`, `ui-44-safe-failure.xml`, `breaks-fix-mods-before.txt`,
`breaks-fix-mods-after.txt`, launcher log `23:06:43–23:07:15`.

### Verdict

**BLOCKED — not `MOD_DOCTOR_BREAKS_ONE_CLICK_FIX_V1_PASS`.** A real Fix button appears and performs genuine
provider resolution/download/staged validation, but the real graph has no safe replacement under the V1
policy, so one press cannot produce a clean set or launch. The app now fails safely and explains why; it does
not fake success or damage the instance. No Release build/version/signing/auth/renderer change; frozen 0.0.1
archive untouched.

Exact next product decision: either (A) define a bounded multi-mod repair policy that may also replace
installed dependents such as Reese's Sodium Options transactionally, or (B) explicitly classify this graph as
not one-click-repairable. That is a new solver/product decision and is not made here.

## MOD DOCTOR SIMPLE RULES V1 â€” known-compatible-set rule Â· LD proof

Timestamp: `2026-09-22 01:30 +07:00`. Debug only; protected Release `D:\ALL Launcher Releases\0.0.1\` untouched
(`com.all.launcher.app` lastUpdateTime still `2026-09-19 02:28:17`). Evidence `test-artifacts\moddoctor-rules-v1\`.

### What was already there (reuse first, nothing duplicated)
`REQUIRES`, `REQUIRES_VERSION` and `BREAKS_WITH` are already implemented **from the jars' own metadata**
(`depends`, `mandatory`, `type=required`, `breaks`), with provider resolution, staging, whole-group
validation, rollback and the `.disabled` original-file semantics all in place. A hand-written rule for those
three would go stale the day a mod updates and would ignore requirements the jar declares that no rule names
(TMF, for one, requires three projects, not one). So the only rule family added is the one metadata cannot
express: **COMPATIBLE_SET**.

### New production code
- `src/main/assets/mod_doctor_rules.json` (16 lines) â€” one versioned, hand-editable file; `rules_version: 1`,
  one rule: minecraft `1.21.11`, loader `fabric`, `when_present` the four ids, `allowed` = the verified quartet
  (sodium `0.8.12`, sodium-extra `0.9.1`, reeses-sodium-options `2.2.3`, iris `1.10.7`).
- `game/version/mod/ModDoctorRules.kt` (151 lines) â€” rule model, JSON parse (null-tolerant, snake_case mapped),
  asset load via `GlobalContext`, and the pure evaluator. Rules fire only when *every* named mod is installed,
  so Sodium alone can never be flagged; an unreadable version stays silent, matching the project's existing rule.
- `ModDoctor.kt` (â‰ˆ150 lines inside the existing file, untracked so git shows no diff): one enum entry, one
  `setFix` field, two small data classes, the validator's optional `setRules` parameter plus one evaluation call,
  and `repairKnownSet`. The repair **reuses `ModDoctorMultiModRepair.apply`** for the whole transaction and the
  existing staged-candidate walk (`firstAcceptedCandidate`) per member, so nothing transactional is new.

Ordering matters and was a real bug found here: the set fix now runs **before** the `breaks` walk, because that
walk returns on failure and previously pre-empted the group rule â€” which is exactly the graph that ended the
previous milestone BLOCKED (2026-09-20 23:24). This rule is that milestone's option (A).

### Bugs found while building it
1. GSON could not see `when_present` (no `@SerializedName`) and a missing field arrived as null into a
   non-null model, so the shipped file would have parsed to nothing on device and crashed on a hand-edited one.
2. The platform writes versions differently from the jar: Modrinth Sodium is `mc1.21.11-0.8.12-fabric` while the
   mod declares `0.8.12+mc1.21.11`. An exact-string lookup could never find the rule's target. `matchesModVersion`
   now accepts both shapes, and the candidate is confirmed by the **staged jar's own declared id and version**.
3. The ordering bug above.

### Tests
`ModDoctorSetRulesTest` (9 tests) plus the whole `game.version.mod` package: **163 tests, 0 failures, 0 errors**
across 17 classes. RED proven by mutation: commenting out the single wiring line fails exactly
`incompatibleQuartetReportsOneProblem` and `fixTargetsOnlyTheMembersThatDiffer` (`2 failed`, `RED-mutation.log`),
then restored. `git diff --check` rc 0. Items 7â€“13 of the milestone's list are already covered by existing
suites (`ModDoctorInstalledDependencyTest` 15, `ModDoctorMultiModRepairTest` 14, `ModDoctorReplacementRepairTest` 10).

Debug artifact `ALLLauncher-Debug-0.0.2-beta1-x86_64.apk`, 233,502,044 B, mtime 2026-09-22 01:15:32, SHA-256
**`DEFBA7BC7A81711DF0A279C236CF5104E6826AC67DDB8BAD9EBC2176D53C8A84`**; `adb install -r`, device base.apk hash
identical, firstInstallTime and uid 10066 unchanged.

### LDPlayer real UI â€” `1.21.11 Fabric 0.19.5`
- **A Sodium alone**: Mod Doctor clean, the set rule stayed inactive, loader MAIN_MENU.
- **B allowed quartet**: clean, MAIN_MENU.
- **C newest published set** (sodium 0.8.14, extra 0.9.4, reeses 2.2.4, iris 1.10.7): detected
  `INCOMPATIBLE_MOD_PAIR,INCOMPATIBLE_MOD_SET`, Fix offered, **one press** â†’ post-fix clean, the allowed quartet
  installed, and a confirmation launch of that exact quartet reached MAIN_MENU with 55 mods loaded and all four
  ids present (`C-confirm.json`). Originals of the three replaced files were preserved as `.disabled`.
- **D only Sodium off** (extra/reeses/iris already on target): one press replaced **exactly one file**
  (`sodium-fabric-0.8.12+mc1.21.11.jar`, sha1 `4d80592bâ€¦` = the provider's) and left the other three byte-identical,
  then clean + MAIN_MENU. No unnecessary replacement.

### TEST E â€” BLOCKED as specified, with the provider's own numbers
The Man From The Fog publishes **nothing for 1.21.11**: Forge project `hIZHlLOF` stops at 1.20.1, Fabric project
`118OmVLd` stops at 1.20.1 (0 builds for 1.21.11, 2 for 1.20.1). GeckoLib `8BmcQJ2H` does have 1.21.11 (20 builds).
TMF's own 1.20.1 fabric build (`TheManFromTheFog-1.1.1+fabric.mc.1.20.1.jar`, sha1 `06669c0câ€¦`) declares **three**
required projects â€” GeckoLib `8BmcQJ2H`, `lhGA9TYQ` and `P7dR8mSH` (Fabric API) â€” so even the named rule alone
would be an incomplete statement of what that mod needs. Evidence `04-test-e-facts.log`. The generic
missing-dependency + one-press Fix path this scenario is meant to exercise is already certified on device for
Forge 1.20.1 and NeoForge 1.21.1 (Waystones â†’ Balm, same mechanism), so nothing about it is unproven.

### Restore
Instance mods back to the pre-run four files with identical sizes and provider SHA-1s (fabric-api, reeses 2.2.4,
sodium-extra 0.9.4, sodium 0.8.15-beta.1); no `.disabled`, `.tmp`, duplicate or dump residue. Honest note: the QA
set-up clears the mods directory per scenario, which removed the two files the repair had kept as `.disabled`
before the restore ran; both were put back from provider-verified copies with the same SHA-1s, and Sodium
0.8.15-beta.1 was re-fetched and SHA-1-checked. Nothing was left changed.

### Exact Next Action
`TEST E as written needs a target where The Man From The Fog exists: run it on Minecraft 1.20.1 Fabric (both
halves publish there), or name a different 1.21.11 mod that requires GeckoLib. Either way the rule already
exists generically â€” only the fixture has to change. Still open: NeoForge 1.21.11 popular certification, and
the four-mod matrix cells already listed in the checkpoint.`

## FABRIC LEGACY CONFLICTS ADVISORY V1 — 2026-09-24 (PASS)

Separate, user-approved milestone. Mod Doctor now surfaces Fabric's LEGACY metadata key `conflicts` as a
NON-BLOCKING advisory. Crash Lab established the semantics first (see
`D:\ALL Mod Doctor Warehouse\crash-lab\CHECKPOINT.md`): chatpatches 8.0-alpha.11 declares
`conflicts: { "morechathistory": "*" }` and has NO `breaks` key at all; Fabric Loader 0.19.5 RECOGNIZED the
declaration, printed a non-fatal startup WARN ("While this won't prevent you from starting the game…") and
loaded both mods to the main menu. It is NOT "the loader ignored conflicts", and the pair is NOT a proven
crash pair.

### Starting state
- Branch `all-0.0.1` @ HEAD `b4c9415b`; tree dirty from earlier milestones: 49 uncommitted entries
  (33 modified tracked + 16 untracked). Nothing committed, nothing staged.
- Pre-milestone SHA-256 baseline of the whole `game/version/mod` package plus `assets/mod_doctor_rules.json`
  (55 files) at
  `D:\ALL Mod Doctor Warehouse\crash-lab\milestone-fabric-legacy-conflicts-advisory-v1\00-baseline.txt`.

### Semantics now in force
- `breaks` → `ModDoctorMetadata.breakPredicates` → breaks loop → `problems` → `isClean=false` →
  `decideLaunchGate` = BLOCK → the fix pipeline may run. UNCHANGED.
- `conflicts` → `ModDoctorMetadata.conflictPredicates` → a new mirror loop → `ModDoctorReport.advisories`
  (new, defaulted second parameter) → `problems` is never touched → `isClean=true` → `decideLaunchGate` =
  LAUNCH → `withFixCandidates` returns `this` before reaching any seam. New type `DECLARED_CONFLICT` (10th
  enum value) carries no fix field. The silence rules and the shared `versionPredicateDecision` evaluator are
  the same as for `breaks`; object-form `{"version": …}` entries are DROPPED exactly as for `breaks` — no
  invented support.

### Files (5 paths; four were ALREADY dirty before this milestone)
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/version/mod/ModDoctor.kt` — untracked, dirty before this milestone.
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/elements/LauncherElements.kt` — modified, dirty before.
- `ZalithLauncher/src/main/res/values/strings.xml` — modified, dirty before. Four keys added.
- `ZalithLauncher/src/main/res/values-th/strings.xml` — modified, dirty before. Four keys added.
- `ZalithLauncher/src/test/java/com/movtery/zalithlauncher/game/version/mod/ModDoctorDeclaredConflictAdvisoryTest.kt` — NEW, created by this milestone.

UI: an additive advisory branch in the TryLaunch flow and in the unchecked-launch backstop (the
`decideLaunchGate` call and both log lines untouched); on an advisory-only report the dialog does not compose
Cancel and EVERY reachable exit launches. No unrelated file was overwritten: re-hashing all 55 baseline files
flags EXACTLY ONE change, `ModDoctor.kt`; every other file, including `FabricModCompatibility.kt`,
`ModpackPolicy.kt`, `ModDoctorRules.kt`, `mod_doctor_rules.json` and all 19 pre-existing test files, is
byte-identical to the pre-milestone state.

### Tests
- Baseline before: 168 in the `game.version.mod` package (258 across the whole test source set).
- After: **178** in the package (268 across the source set). The new class holds 10 tests; no existing test
  file was edited.
- Full suite run by central with `cleanTest`: `Task :ZalithLauncher:testDebugUnitTest` executed (not
  UP-TO-DATE), BUILD SUCCESSFUL. The reviewer independently read all 20 XMLs: `failures="0" errors="0"
  skipped="0"` on every one, the 19 pre-existing classes totalling exactly 168.
- Real-bytes test run by central with
  `MOD_DOCTOR_REAL_PAIR_DIR=D:\ALL Mod Doctor Warehouse\crash-lab\cache\mods`: BUILD SUCCESSFUL;
  `theRealCrashLabPairProducesTheAdvisory` took 1.492 s and `system-out` was EMPTY, which is the proof it read
  the real chatpatches jar rather than taking the early return (that path prints). XML, verbatim:

```
<testsuite name="com.movtery.zalithlauncher.game.version.mod.ModDoctorDeclaredConflictAdvisoryTest" tests="10" skipped="0" failures="0" errors="0" timestamp="2026-09-24T02:25:08.929Z" hostname="THE_BOUY2" time="2.392">
  <testcase name="aNonMatchingConflictPredicateProducesNoAdvisory" ... time="0.51"/>
  <testcase name="declaredConflictIsReportedAsAnAdvisoryNotAProblem" ... time="0.002"/>
  <testcase name="anAdvisoryOnlyReportStillLaunches" ... time="0.014"/>
  <testcase name="packSafeModeKeepsTheAdvisoryVisible" ... time="0.099"/>
  <testcase name="breaksAndConflictsOnTheSameModStaySeparate" ... time="0.002"/>
  <testcase name="theRealCrashLabPairProducesTheAdvisory" ... time="1.492"/>
  <testcase name="realChatPatchesMetadataIsReadFromAJarIntoTheAdvisory" ... time="0.026"/>
  <testcase name="anAdvisoryDoesNotReachTheRepairPipeline" ... time="0.203"/>
  <testcase name="anAbsentConflictTargetProducesNoAdvisory" ... time="0.001"/>
  <testcase name="anObjectFormConflictEntryIsNotInvented" ... time="0.031"/>
  <system-out><![CDATA[]]></system-out>
  <system-err><![CDATA[]]></system-err>
</testsuite>
```
- `assembleDebug -Darch=x86_64`: BUILD SUCCESSFUL. Artifact
  `ZalithLauncher/build/outputs/apk/debug/ALLLauncher-Debug-0.0.2-beta1-x86_64.apk`, 229,852,069 B,
  mtime 2026-09-24 08:48:03, SHA-256 `305DCCD6FAFF0F6DB401794101DE57F5C35B48F2B98260A669A4C0AA803CFEFC`
  (central re-hashed it independently and got the same).

### Regressions
`ModDoctorSetRulesTest` 10 (the Sodium / Sodium Extra / Reese's Sodium Options / Iris quartet),
`ModpackSafeModeTest` 12 and `ModpackRequiredCoreTest` 3 (Modpack Rules V1) all green; their test files are
byte-identical to the baseline. `mod_doctor_rules.json` unchanged (hash matches the baseline).

### Verdict
**PASS** on all twelve Definition-of-Done points. Reviewer verification ran in two rounds: round 1 verified
the production change structurally and found four of the ten tests would pass with the feature removed (one of
them vacuous); the implementer strengthened them and round 2 verified each now carries an assertion that only
the feature can satisfy. Lead reconciled and issued the final PASS.

### Honest limits — do not read the PASS as covering these
1. NO device or emulator run exists for this milestone. The advisory-only dialog's rendering, the Thai strings
   on screen, and "every exit launches" are proven by READING the Compose state machine, not by execution.
2. Warehouse provider counts were NOT re-counted. Nothing in this milestone has a path to the provider corpus
   (a different project; the tests read jars into temp directories), but that is scope reasoning, not a
   measurement.
3. No mutation test was run by anyone: "these tests are discriminating" is reasoning from the code.
4. `anAdvisoryDoesNotReachTheRepairPipeline` would still pass if an advisory were ALSO misfiled into
   `problems`; tests 1, 4 and 8 cover that case instead.
5. The new `crash`-substring assertion cannot fail against any text the code can currently produce — the word
   appears nowhere in `ModDoctor.kt` — so it guards future wording only.
6. One of the ten tests is env-gated and a no-op in an ordinary run.
7. The full-suite 20-XML evidence was overwritten by the filtered gated run; the counts were read and recorded
   by the reviewer before that and re-created on disk afterwards.
8. Three of the five changed files (`LauncherElements.kt`, `values/strings.xml`, `values-th/strings.xml`) sit
   outside the hashed baseline, so for them the evidence is the implementer's whole-tree mtime scan (exactly
   five files written) and the byte-count arithmetic on the strings files, not a pre-milestone digest.

## POST-CRASH MOD DOCTOR + LOCAL LEARNING V1 — 2026-09-24 (PARTIAL)

The direction changed with this milestone: Crash Lab is now a verification/support tool only, not the discovery
engine, and no new blind launch budget was authorized. The product line is PRE-LAUNCH DOCTOR + POST-CRASH
DOCTOR + LOCAL LEARNING RULES. This milestone used **2 real launches** on the already-proven axiom fixture.

### Starting state
- Branch `all-0.0.1` @ HEAD `b4c9415b`; 36 modified tracked + 20 untracked = **56** working-tree entries, nothing
  staged, dirty from earlier milestones. Pre-milestone SHA-256 baseline of all 1348 files under
  `ZalithLauncher\src` plus `build.gradle.kts` and `gradle.properties`:
  `D:\ALL Mod Doctor Warehouse\crash-lab\milestone-post-crash-doctor-v1\00-baseline.txt`.
- Device `127.0.0.1:5555` (LDPlayer, G576D), package `com.all.launcher.app.debug`, instance
  `1.21.11 Fabric 0.19.5`. `emulator-5554` was never contacted.

### What changed
Six files edited — `game/version/mod/_MMKV.kt`, `ui/activities/ErrorActivity.kt`, `ui/activities/MainActivity.kt`,
`ui/screens/main/ErrorScreen.kt`, `res/values/strings.xml`, `res/values-th/strings.xml` — and seven created:
`assets/crash_signatures.json`, `game/version/mod/CrashSignatures.kt`, `CrashDiagnosis.kt`, `CrashLearning.kt`,
plus their three test files. Re-hashing the 1348-file baseline flags **exactly those 6 changed and 0 missing**:
`VMActivity.kt`, `LauncherElements.kt`, `ModDoctor.kt`, `ModDoctorRules.kt`, `ModpackPolicy.kt`, `LocalMod.kt`,
`mod_doctor_rules.json` and every pre-existing test file are byte-identical to the pre-milestone state.

- `CrashDiagnosis.kt` — a pure classifier for four launch outcomes with explicit precedence (crash marker → D;
  marker + exit 0 → B; marker → A; otherwise C, **never defaulting to D**), the two marker constants, a 1 MiB
  capped tail reader, and the instance directory derived from the log path (`logFile.parentFile.parentFile`).
- `assets/crash_signatures.json` + `CrashSignatures.kt` — three signatures as data (`platform_limit_native`,
  `mixin_apply_failed`, `loader_fatal_unattributed`), parsed by a pure drop-don't-throw function in the style of
  `ModDoctorRules.kt`, with `message_key` → string resource kept in Kotlin. Only signatures with a real captured
  log line behind them were included; Java/runtime, missing-dependency, wrong-version and class-ownership were
  deliberately NOT added because no captured example exists.
- `CrashLearning.kt` — `CANDIDATE` / `CONFIRMED_LOCAL` records in a new multi-process MMKV store
  `PostCrashLearning`; one record per `signatureId:modSetFingerprint`; promotion needs the same signature a
  second time on the same fingerprint AND a marker-seen next launch; three matches without promotion latch
  inconclusive. `VERIFIED` does not exist. The store also holds the record's own absolute log path, which is how
  the pending action is resolved later.
- `ErrorActivity.kt` / `ErrorScreen.kt` — the crash screen reads the captured log's tail, classifies it, renders
  a diagnosis, and offers at most ONE action: disable the named mod through the existing reversible
  `LocalMod.disableModFile()` rename. MEDIUM and LOW never produce an action.
- `MainActivity.kt` — one call in `onCreate` resolving a pending action on IO.

**Semantics:** a button appears only when confidence is HIGH, the action is disable, the log named an owner id and
that mod's file really exists and is enabled in the mods dir derived from the log path. `breaks` and the pre-launch
gate are untouched.

### Tests and build
Package `game.version.mod`: **178 → 218** tests, 0 failures / 0 errors / 0 skipped, `cleanTest` forcing real
execution. `assembleDebug -Darch=x86_64` BUILD SUCCESSFUL; debug artifact
`ALLLauncher-Debug-0.0.2-beta1-x86_64.apk`, 233,707,354 B, SHA-256
`71E307768FCE422B1A4B67F0979B8A14D7487C4054A7CAE28D42ED2C925E2C6C`, and that exact digest was on the device
before launch 1.

### Device proof
Gate clean (`inspect finished: 0 problem(s) []`, no dialog). Launch 1 reproduced the frozen axiom crash exactly
— entrypoint line 506, `dlopen failed: library "libstdc++.so.6" not found` line 516, crash marker line 648, no
`Sound engine started` — and the crash screen rendered a Thai diagnosis naming `axiom` at HIGH confidence:
`การวินิจฉัย` / the platform-limit sentence / `ความมั่นใจสูง` / `ม็อดที่น่าจะเป็นต้นเหตุ: axiom`. **The first device
round had FOUR buttons and no action** because the launcher's main process had been killed and `ErrorActivity`
cold-started with an empty `VersionsManager`; the root cause was fixed by removing that dependency entirely (the
crash path now derives everything from the log PATH plus the log TEXT), and in the second round the screen
carried a FIFTH topmost button `ปิด axiom แล้วรีสตาร์ท` at node `[670,268][944,316]`. Pressing it renamed the jar
to `.jar.disabled` and the next launch concluded `[23:03:13] [Render thread/INFO]: Sound engine started` with
zero crash markers and zero entrypoint failures. The record then resolved to `CANDIDATE, count 1,
markerSeenAfterFix: true, crashMarkerSeenAfterFix: false`, which is what the implementation's own promotion rule
must produce. The instance was restored to its 8-file inventory, sha1-verified, three of them legitimately
`.disabled`.

### Verdict: PARTIAL
Not PASS, for three reasons, and not FAIL/BLOCKED because nothing was broken and every gap was disclosed:
1. **The relaunch is NOT automatic.** The action calls `ProcessPhoenix.triggerRebirth`; the launcher returns to
   its main screen and the second launch was a human press 90 s later. "offers → applies → launches again" is met
   only with that press.
2. **The session did not demonstrably stay up.** The `:game` process was SIGKILLed 2.4 s after the marker, with no
   tombstone, no ANR and no named killer; the marker itself means "client finished initialising, title screen not
   yet drawn", so reaching it is not "playable".
3. **The cold-start branch the fix exists for was NOT exercised on device** — the main process happened to survive
   in round 2, so it ran the warm branch. That branch has code evidence only.

Two Definition-of-Done lines are also false as written: **the existing Fix Engine is not reused** —
`ModDoctor().fixProblems` is called from exactly one place, `LauncherElements.kt` in the pre-launch gate, and
nothing on the crash path calls it (the crash path reuses `AllModReader`, `LocalMod.disableModFile()` and
`VersionFolders.MOD`); the user's intent of adding no duplicate download/install system does hold. And **"rules
separable from the APK for future updating" is true in form but false in effect**: the signatures are a bundled
asset read only through `applicationContext.assets` with a memory cache and no filesystem, remote or version
override, so changing a rule needs a rebuild and reinstall.

### Safety and measurements
Protected `D:\ALL Launcher Releases\0.0.1\ALLLauncher-0.0.1-arm64-v8a.apk` = `3BD3E79D…715BAB9`, 187,174,100 B —
unchanged. Frozen `D:\ALL Launcher Releases\0.0.2-beta1\ALLLauncher-0.0.2-beta1-arm64-v8a.apk` =
`66E7552C…F5F4`, 187,188,460 B — unchanged (the round-2 report had asserted this without a measured row; central
measured it afterwards). Nothing staged, no commit/stash/reset/clean, HEAD unchanged. No CurseForge contact, no
provider recrawl, RDC not used. Corrections made during verification: the crash log has at least 648 lines, not
the "641" the round-2 report claimed, and the working-tree entry count is 56, not 55.

### Evidence and a known defect
`D:\ALL Mod Doctor Warehouse\crash-lab\milestone-post-crash-doctor-v1\` — `00-baseline.txt`,
`10-IMPLEMENTER-REPORT.md`, `20-IMPLEMENTER-ROOT-CAUSE-FIX.md`, `30-LEAD-VERDICT.md`, `e2e\` (round 1) and
`e2e2\` (round 2). **Defect:** most round-2 files were written as UTF-16, so `read` and `search` refuse them as
binary — read them with `Get-Content -LiteralPath` or they will look empty.

## MOD DOCTOR V1 — CLOSE THE LOOP — 2026-09-25 (PARTIAL)

The loop of the post-crash doctor was closed: a learned repair is now CONSUMED before the same crash repeats,
the crash-signature rule pack updates without rebuilding the APK, and the Fix completes the loop with a one-shot
automatic relaunch. Verdict PARTIAL for one DoD item only: **the `all-player` role never ran** — its sub-agent
failed twice with the same upstream 400 error (`"name" is not supported by this endpoint`), which the user has
forbidden fixing here, so the real-user QA was carried out by the central operator under the Player protocol
(`player\99-PLAYER-REPORT.md`). "Playable" also remains unclaimed.

### Starting state
Branch `all-0.0.1` @ HEAD `b4c9415b`; 36 modified tracked + 20 untracked = 56 entries, nothing staged. A
pre-milestone SHA-256 baseline of 1353 files under `ZalithLauncher\src` plus the two build files:
`D:\ALL Mod Doctor Warehouse\crash-lab\milestone-mod-doctor-v1-close-loop\00-baseline.txt`. Device
`127.0.0.1:5555` (LDPlayer, G576D), package `com.all.launcher.app.debug`, instance `1.21.11 Fabric 0.19.5`.

### What changed (7 edited + 3 created, and nothing else)
- `ModDoctor.kt` — `LEARNED_LOCAL_CRASH` type, `ModDoctorLocalDisableFix` + the defaulted `localDisableFix`
  field, `hasFixableProblems`, a pack-safe-mode branch, the consumer glue (`learnedWarningCandidates` /
  `learnedLocalCrashProblems` / `withLearnedLocalCrashWarnings`) injected ONLY in the `inspect(version)`
  orchestrator, and one local-disable loop in `fixProblems` using the existing `LocalMod.disableModFile()`.
  **Match identity is `signatureId + ownerModId (+ instance, MC version, loader)` — never the whole-set
  fingerprint**, because the fingerprint changes the moment the named mod is disabled and would make the record
  unreachable forever; the record's own key is unchanged, so the record already on the device was consumable
  with no migration. A disabled/absent owner emits NOTHING (no problem, no advisory), which is what prevents an
  endless dialog after a fix.
- `CrashLearning.kt` — `relaunch|` prefix, `relaunchIsDue` (due only if `at in 1..now && now-at <= 5 min`),
  `LEARNED_WARNING_LIMIT`, `learnedRecords`, `markRelaunchPending`, `@Synchronized consumePendingRelaunch`
  (read → remove → validate), and the guard in `resolvePendingOutcome`: a rewritten log with NEITHER marker
  concludes nothing and keeps the `pending|` key (without it, an automatic relaunch could destroy the promotion
  window by reading the truncated log too early).
- `CrashSignatures.kt` — `CRASH_SIGNATURE_SCHEMA_VERSION`, `parseExternalRulePack` (missing / 0 / non-numeric /
  newer / empty → reject), the external-first loader with a `length:lastModified` stamp, fallback
  external → bundled → empty, and one `crash rule pack loaded` log line per load. The external path is
  `File(PathManager.DIR_FILES_PRIVATE, "rules/crash_signatures.json")`; `PathManager.kt` untouched; no
  transport, no installer.
- `ErrorActivity.kt` (+7) — `markRelaunchPending` inside the existing `preState != null` branch, after
  `recordActionTaken`, before `ProcessPhoenix`.
- `MainActivity.kt` (+31) — `resolvePendingActions()` then `consumePendingRelaunch()`, name-based version
  resolution (`getVersion(name)`, never `currentVersion`), and the SAME event the `เริ่มเกม` button sends.
- Both `strings.xml` (+3 each, Thai truthful), plus three new test files
  (`ModDoctorLearnedWarningTest` 13, `CrashLearningRelaunchTest` 5, `CrashSignaturesExternalPackTest` 10).

**Untouched by design:** `LauncherElements.kt` (the dialog's Fix button lights up automatically because
`hasFixableProblems` now includes the new field), `ErrorScreen.kt`, `MainScreen.kt`, `CrashDiagnosis.kt`,
`ModpackPolicy.kt`, `ModDoctorRules.kt`, `mod_doctor_rules.json`, `_MMKV.kt`, `LocalMod.kt`,
`assets/crash_signatures.json`.

### Tests and build
Package `game.version.mod`: **246** tests, 0 failures / 0 errors / 0 skipped (218 pre-existing unchanged +
28 new). `assembleDebug -Darch=x86_64` BUILD SUCCESSFUL → `ALLLauncher-Debug-0.0.2-beta1-x86_64.apk`,
233,709,926 B, SHA-256 `CB99A0EAE4C501356E157255B8A2B21D4B44B0EBDCBC24DE3187AAA04FD010DD` — that digest was
installed and driven on the device.

### Device proof (4 real launches; Lead locked 3 and pre-approved the 4th)
1. **L1 — the learned warning fires BEFORE the crash.** The pre-launch doctor blocked with
   `เคยพังในเครื่องนี้ (เรียนรู้ได้): Axiom … — เรียนรู้จากบันทึกของเครื่องนี้ ไม่ได้สรุปว่าม็อดนี้เข้ากันไม่ได้โดยทั่วไป`,
   derived from the store record the previous milestone left behind, with no migration. (Launch Anyway →
   crash → count 2.)
2. **L2 — the loop closes.** On the crash screen, one tap on `ปิด axiom แล้วรีสตาร์ท` → the jar became
   `Axiom-6.0.5-for-MC1.21.11.jar.disabled`, the other two jars byte-identical → **the launcher restarted and
   launched Minecraft by itself**: 12 s after the tap the screen was the game overlay (`เกมกำลังทำงาน…`,
   version `1.21.11 Fabric 0.19.5`) with no further press, 25 s later `FPS: 51`. The pulled log has
   `[01:17:20] [Render thread/INFO]: Sound engine started`, **zero** crash markers, **zero** entrypoint
   failures. Opening the app again promoted the record to **CONFIRMED_LOCAL** (count 2, `actionPreState`
   non-blank, marker seen, no crash marker).
3. **L3 — the second encounter warns before the crash repeats.** Axiom re-enabled by the user →
   `เริ่มเกม` → the warning appeared again BEFORE the crash, now with `ยืนยันแล้วในเครื่องนี้` (the confirmed
   wording) — the learned knowledge changed future behaviour.
4. **The rule pack proof, with NO APK rebuild.** Pack B (same match structure, `confidence` HIGH → MEDIUM)
   installed by adb push + rename into the app-private dir → the crash screen said `ความมั่นใจปานกลาง` and the
   fix button was ABSENT. Corrupt pack (`{"signatures_version": 99}`) → the crash screen returned to
   `ความมั่นใจสูง` with the fix button: the bundled fallback, safely. The 4th launch was needed because adb
   cannot kill a foreground process (`am kill` refused, `kill -9` returned Operation not permitted), so the
   zero-launch kill-and-resume route Lead pre-approved was unavailable.

### Reviewer and Player verdicts
Reviewer (two rounds): every structural claim VERIFIED by file:line, all 28 new tests judged discriminating, no
refuted claim. Player: performed by the central operator under the Player protocol (see the role note above);
the verdict is PASS for the user flow with the playable caveat.

### Safety
Protected `0.0.1` = `3BD3E79D…715BAB9` and frozen `0.0.2-beta1` = `66E7552C…F5F4` — re-measured unchanged after
the QA. 56 worktree entries, nothing staged, HEAD unchanged. The instance restored by SHA-1; the external pack
deleted. RDC not used; `emulator-5554` never contacted; no recrawl, no AI, no cloud, no UX/UI overhaul.

### Honest limits
1. **Playable is not claimed**: the game died ~7 s after the marker, twice here and once before, with no
   tombstone and no named killer.
2. **DoD item 15 is not met by the designated role** (the `all-player` agent's upstream endpoint rejected its
   requests twice); the QA was performed by central under the same protocol.
3. The learned warning only fires when the owner mod is present and enabled; the disabled/absent case is
   silent by design (it prevents an endless dialog after a fix).
4. The promotion is only written when the launcher is opened again; a user who never reopens stays on CANDIDATE.
5. One fixture, one instance, one device; MEDIUM/LOW signatures have no device evidence (unit tests only); there
   is no rule-pack installer UI in V1 (adb only).

### Evidence
`D:\ALL Mod Doctor Warehouse\crash-lab\milestone-mod-doctor-v1-close-loop\` — `00-baseline.txt`,
`10-implementer-report.md`, `30-LEAD-VERDICT.md`, `player\99-PLAYER-REPORT.md` and the numbered screen dumps,
plus the pulled game logs. Full record also in `ALL_CORE_CHECKPOINT.md` and `crash-lab\CHECKPOINT.md`.

## FINAL V1 STABILITY CLOSEOUT — 2026-09-25 (PARTIAL / ROOT CAUSE INCONCLUSIVE)

Source-of-truth evidence: `D:\ALL Mod Doctor Warehouse\crash-lab\milestone-mod-doctor-v1-close-loop\player\60-process-lifecycle\99-investigation.md`.

The previously observed `:game` death after `Sound engine started` remains unexplained: historical evidence reports SIGKILL/no tombstone/no named killer, and the PID/termination owner was not recoverable from saved logs. No repeated identical launch was done. One bounded instrumented Fix attempt (08:16:38 +07, LDPlayer `127.0.0.1:5555`) produced a separate result: Phoenix PID 6996 intentionally killed old launcher main PID 4603, fresh launcher PID 7045 resumed `MainActivity`, and no `:game` PID appeared in the 90-second window. This did not reproduce the marker death; no extra launch was pressed. Android 9/API 28 has no supported `dumpsys activity exit-info` here, and current logcat cannot recover the rolled-over earlier window.

Read-only source tracing found `VMActivity`/`GameService` in `:game`, with `VMActivity.onDestroy()` stopping `GameService`; the observed Phoenix kill targeted only the old main process. Other self-kill calls were found, but no caller or stack connects them to the earlier game death. Therefore cause and owner remain UNKNOWN; production files were not changed and no tests/build were rerun. Local debug APK and installed APK both hash `CB99A0EAE4C501356E157255B8A2B21D4B44B0EBDCBC24DE3187AAA04FD010DD`. Frozen releases re-hashed unchanged: `0.0.1` = `3BD3E79D323F9A24BCA775FF9849E3E7AFF779FDB70651CC5CA321E1D715BAB9`; `0.0.2-beta1` = `66E7552C97F7142FE1A1729FAD0940D700A7496B8E46CD1FBCC1BA14E9AFF5F4`.

After the Fix action, the Axiom QA fixture was restored from the existing local cache into the selected instance and SHA-1 verified as `8fbab015a84d46a1a750e64f7142a11bcb431a0f`. Device stayed on `MainActivity`; Minecraft playability is NOT PROVEN. `all-player` was not dispatched because stability was not proven; its prior provider error (`"name" is not supported by this endpoint`) remains unresolved. No RDC, provider crawl, release edit, or production source edit occurred. Existing worktree state was preserved (56 entries, none staged at start).

`PRODUCT_STABILITY = NOT PROVEN`; `MOD DOCTOR V1 = PARTIAL` pending process-death attribution, stable usable Minecraft, and designated `all-player` dispatch.

## PHYSICAL iQOO STABILITY PROOF — 2026-09-25 (PARTIAL)

One controlled normal-launch run on physical iQOO I2405 (serial 10AF681QSC001YK, Android 16/API 36, arm64-v8a) is documented at D:\ALL Mod Doctor Warehouse\crash-lab\milestone-mod-doctor-v1-close-loop\player\70-iQOO-physical-proof\99-iQOO-report.md. Installed app was the older 0.0.1-debug APK, SHA-256 6271FBF51CCF6879B3EFA031C9E7FAE70E7C9CB770B2EFC4F7C704C22E569193, not the available x86_64 current checkout APK. Game PID 21828 started 09:04:26.980 and exit-info reports SIGNALED/status 9 at 09:04:32.517 without sender or trace. This run did not reach Sound engine started: captured latest.log proves Fabric rejected Sodium 0.8.14+mc1.21.11 against Iris 1.10.7+mc1.21.11 because Sodium declares that it breaks Iris. No second run, production edit, build/install, or all-player dispatch. The earlier LDPlayer post-sound SIGKILL cause remains inconclusive; physical product stability and current Mod Doctor V1 physical behavior remain unproven. Instance mods were not changed; latest.log was copied to evidence. Worktree remains 36 modified tracked + 20 untracked, none staged. Protected 0.0.1 and frozen 0.0.2-beta1 release hashes reverified unchanged. RDC = NO. Verdict: PARTIAL; stop here.

## MOD DOCTOR V1 — FINAL PHYSICAL CLOSEOUT — 2026-09-25 (BLOCKED: device unreachable before the run)

**The product side of this closeout succeeded; the physical run could not start.** Current source → ARM64 APK →
installed on the iQOO → certified mod set in place, all hash-verified. Then the phone stopped answering adb and
never came back, so the controlled launch never happened and PRODUCT_STABILITY remains **NOT PROVEN**.

### Completed and verified
- **Build**: `.\gradlew.bat :ZalithLauncher:assembleDebug -Darch=arm64 --no-daemon --console=plain` with
  `JAVA_HOME=C:\jdk21` → `ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.2-beta1-arm64-v8a.apk`,
  223,692,722 B, SHA-256 `4693808DE7BA6A274BCEEDC23EF4C783A3B48D27EA99E156E28837F5C23CFFE3` (re-measured by
  central after the fact, unchanged). ARM64-only proven four ways: the build log's `[JREAssetsCleanup] arch:
  arm64` with 13 non-arm64 JRE deletes; `aapt2 dump badging` → `native-code: 'arm64-v8a'` only; 40 `.so` files
  all under `lib/arm64-v8a/` and only `bin-arm64.tar.xz` present; `zipalign -c -v 4` rc 0.
- **Install**: `adb -s 10AF681QSC001YK install -r …` → Success. `versionName=0.0.2-beta1-debug`, `versionCode=2`,
  `primaryCpuAbi=arm64-v8a`, `firstInstallTime` UNCHANGED (2026-09-20 13:33:35), `lastUpdateTime` 2026-09-25
  09:43:06. The pulled installed `base.apk` SHA-256 equals the built APK — the phone was running the current
  source, not the stale `6271FBF5…` Phase-5D binary.
- **Fixture**: the instance held three mods (fabric-api correct, iris 1.10.7 correct, **sodium 0.8.14 = the
  declared breaker**). The five certified files were pushed individually with host SHA-1 verified first and
  device SHA-1 verified after: fabric-api `c98467cb…`, iris `aae8567b…`, reeses 2.2.3 `4f6b87af…`,
  sodium-extra 0.9.1 `49783b5a…`, sodium 0.8.12 `4d80592b…`. Sodium 0.8.14 was MOVED (not deleted) to
  `mods_backup_20260925/` and still re-hashes to `4a8733fe…7bf91`.
- **Read route settled**: a plain non-root `adb shell ls`/`sha1sum` on `/sdcard/Android/data/<pkg>/…` works on
  this API 36 phone — the open question both inspectors left is now answered.
- **Safety**: protected `0.0.1` = `3BD3E79D…715BAB9` and frozen `0.0.2-beta1` = `66E7552C…F5F4`, both
  re-measured unchanged; HEAD `b4c9415b`; 56 worktree entries; nothing staged; no production file changed at
  all (so the 246-test suite did not need re-running).

### The blocker, with evidence
`adb devices -l` now lists only `emulator-5554`; the phone's serial is gone, though the implementer had just
been reading its filesystem. Windows still enumerates `Android Composite ADB Interface` (`USB\VID_2D95&PID_6003
&MI_01\…`, Status OK) and the `iQOO Neo 10` WPD device, so the cable and interface are present and it is the
device-side daemon that stopped answering. Failed recoveries: `adb reconnect`; `adb kill-server` +
`start-server` twice; a six-attempt 60-second poll; plus one `could not read ok from ADB Server`. It needs hands
on the phone (unlock / re-accept the USB-debugging prompt / toggle USB debugging / replug).
**Disclosure:** the phone vanished immediately after central ran `adb kill-server` to clear a hung
`dumpsys activity exit-info` (that command hangs for minutes on Android 16). Causation is not proven, but the
correlation is recorded rather than hidden.

### Also settled while investigating
The "unexplained post-marker SIGKILL" has a known *teardown*, not a known *trigger*:
`ZLNativeInvoker.kt:127-134` calls `killProgress()` unconditionally and `LocalUtils.kt:327-329` is
`Process.killProcess(Process.myPid())`, so `reason=2 (SIGNALED) status=9` with a sender tid inside the dying pid
is exactly what the app's own normal JVM teardown produces and identifies nobody. A future death must be
attributed by the JVM's own exit line and the sender pid, not by the signal. The previous iQOO death, by
contrast, IS explained: Fabric aborted on the certified-forbidden pair (`sodium 0.8.14 {breaks iris <=1.10.7}`).

### Role status
`all-lead` failed once with `opencode-go offered no model` and succeeded on retry (provider intermittent).
`all-player` was not attempted: its endpoint failed in the previous milestone with `"name" is not supported by
this endpoint` and the user forbids changing team configuration. Recorded separately as required:
**PRODUCT_STABILITY = NOT PROVEN (blocked by device access)**; **ALL_PLAYER = BLOCKED_BY_PROVIDER**.

### Evidence
`D:\ALL Mod Doctor Warehouse\crash-lab\milestone-v1-physical-closeout\` — `00-gates.txt`,
`10-arm64-debug-build.log`, `11-arm64-audit.txt`, `11-arm64-zipalign-full.log`, `12-read-route.txt`,
`20-inventory-before.txt`, `21-install.txt`, `22-installed-base.apk`, `23-`/`24-`/`25-sha1-both-sides.txt`,
`41-`/`42-` (empty: the snapshot commands hung), and `99-CLOSEOUT.md`.

### Exact next action
Reconnect the iQOO and press Launch once — the instance is left ready (certified five mods, the new build
installed). Expect a CLEAN pre-launch gate; a dialog there would itself be a finding. Then capture telemetry per
the locked plan, and pull `/sdcard/closeout-live.txt` (left on the phone) as the pre-run baseline.

## MOD DOCTOR V1 — FINAL PHYSICAL CLOSEOUT on the iQOO — 2026-09-25 (run COMPLETED; "playable" NOT claimed)

The physical proof ran. The current source was built for arm64, installed on the iQOO, the certified five-file
mod set was put in place, and ONE controlled launch was performed from the real UI. Minecraft reached its
client-initialised marker and the game process was still alive about twelve minutes later. No production file
was changed for this milestone. What is still NOT claimed is the specific on-screen menu, and the user-facing
role never ran.

### Build, install and fixture
- `.\gradlew.bat :ZalithLauncher:assembleDebug -Darch=arm64 --no-daemon --console=plain` (JAVA_HOME `C:\jdk21`)
  → `ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.2-beta1-arm64-v8a.apk`, 223,692,722 B,
  SHA-256 `4693808DE7BA6A274BCEEDC23EF4C783A3B48D27EA99E156E28837F5C23CFFE3`. arm64-only proven four ways:
  build-log `[JREAssetsCleanup] arch: arm64` (13 non-arm64 JRE deletes), `aapt2 dump badging` →
  `native-code: 'arm64-v8a'` only, 40 `.so` all under `lib/arm64-v8a/` and only `bin-arm64.tar.xz`,
  `zipalign -c -v 4` rc 0.
- Installed as an update: `versionName=0.0.2-beta1-debug`, `versionCode=2`, `primaryCpuAbi=arm64-v8a`,
  `firstInstallTime` UNCHANGED (2026-09-20 13:33:35). The pulled installed `base.apk` SHA-256 EQUALS the built
  APK — the phone ran the current source, not the stale `6271FBF5…` Phase-5D binary.
- The instance held three mods (fabric-api correct, iris 1.10.7 correct, **sodium 0.8.14 = the declared
  breaker**). The five certified files were pushed individually with host SHA-1 verified first and device SHA-1
  after: fabric-api `c98467cb…`, iris `aae8567b…`, reeses 2.2.3 `4f6b87af…`, sodium-extra 0.9.1 `49783b5a…`,
  sodium 0.8.12 `4d80592b…`. Sodium 0.8.14 was MOVED to `mods_backup_20260925/` (still `4a8733fe…7bf91`), not
  deleted.
- A plain non-root `adb shell ls`/`sha1sum` on `/sdcard/Android/data/<pkg>/…` works on this API 36 phone.

### The launch (device clock, from the launcher's own captured log)
`10:03:50` `เริ่มเกม` → `10:04:38` an **account-refresh** dialog (HTTP 400/401, NOT a Mod Doctor dialog) →
`10:05:15` `ข้ามการเฟรช` → `10:05:24` `Loading 55 mods:` with exactly the five certified jars and **no 0.8.14**
→ `10:05:40` `Setting user: THE_BOUY2` → `10:05:43` `OpenGL Renderer: Adreno (TM) 825 | OpenGL ES 3.2` via the
Krypton Wrapper → `10:05:48` `OpenAL initialized on device OpenSL` then **`Sound engine started`** → 15 ×
`Created: … textures/atlas/…-atlas` → `10:05:51` `Creating pipeline for dimension minecraft:overworld`.
**Zero `#@!@# Game crashed!`, zero `Could not execute entrypoint`, zero mod-resolution failure.** The captured
log stops at 10:05:51 while the process lives on (expected when nothing logs).

### Lifecycle — no death
`ps -A -o PID,ETIME,ARGS` (saved raw in `65-lifecycle-fresh.txt`) → `29044  13:19  com.all.launcher.app.debug:game`.
**ETIME is the process lifetime from `exec`, not the time since the marker** — so at that reading the process
had outlived `Sound engine started` by about twelve to thirteen minutes. **Stronger evidence arrived later:**
the captured log grew to 55,307 B because the JVM wrote a **full thread dump at 10:26:11** whose Render thread
reports `elapsed=1249.68s` (**20 min 50 s of JVM uptime**) and is `RUNNABLE` inside
`RenderSystem.limitFPS`-class `RenderSystem.limitDisplayFPS` — the frame limiter at the top of Minecraft's client
loop — with a healthy heap (396 MB of 2 GB) and **no `crash-reports/` directory in the instance at all**. So the
game outlived the marker by **about twenty minutes with a live render loop**, against the 2.4 s and ~7 s deaths
recorded on LDPlayer. The session's **end is not attributed**: the dump's trigger is unknown (a `SIGQUIT`, which
on Android also gathers ANR traces) and the covering logcat does not exist. An earlier draft of the closeout said "more than thirteen minutes";
the independent Reviewer caught the conflation and it was corrected. Screen liveness independently of the
process: non-black pixel fraction 79.3% / 77.2%, frame-to-frame change 47.51 over 110 s, and the launcher's own
overlay reading `FPS: 60`.

### The interaction, measured where the key matters
The on-screen `F3` control sits at `[344,31][400,88]`; Minecraft's debug overlay draws in the TOP-LEFT, so that
quadrant (1200×540, sampled every 10 px) was measured apart from the full frame. Idle 3 s pairs gave quadrant
**13.49 / 14.58 / 14.57**; after F3 tap #1 it jumped to **62.34**; three seconds later with no tap it fell to
**25.28**; after tap #2 it jumped again to **65.18**. A 4.3–4.8× jump in exactly that quadrant, twice, with a
fall-back in between — a rotating panorama moves the whole frame roughly uniformly and does not quadruple one
quadrant. An earlier independent run reproduced the pattern (idle 13.23–19.71, after-tap 44.69 / 47.11).

### Reviewer and Player
- **Reviewer round 1** (independent, on the raw folder): VERIFIED the build/install identity, the certified mod
  set, the marker and its surrounding lines, and the non-fatality of the 401/Realms noise; **REFUTED** the
  "7+ minute lifecycle" claim as then evidenced (the captured game log covers only 10:05:24→10:05:51 and PID
  29044 appeared in no file), marked the interaction and the screen identity NOT PROVEN, and correctly found
  that **`40-run-logcat.txt` does not cover the run** (an operator capture error) and that `99-CLOSEOUT.md` was
  stale. All of that was accepted.
- **Reviewer round 2** (narrow, on the new raw evidence): lifecycle **PARTLY VERIFIED** — liveness beyond the
  marker proven from the saved `ps`, but the "thirteen minutes" wording overstated (corrected to ~12);
  interaction **PARTLY VERIFIED** — the quadrant method is technically sound, a panorama or a frame-timing
  change cannot explain a single-quadrant 4× jump, and the jump-fall-jump pattern is the expected F3 signature,
  but the Reviewer had no shell and could not re-measure it independently.
- **Player: BLOCKED, never ran.** Its session has only `read`/`search` — no shell, so no adb, and `read` cannot
  decode a PNG, so it could not see the screenshots either. It declined to grade a user flow it could not see
  and used its tools to review the evidence files instead, where it added two useful findings: the
  account-refresh dialog interrupts every launch and shows no status code, and "a black surface with the
  launcher's overlay is not Minecraft" — so nobody should score "not black" as usable.

### Not proven
1. **Which Minecraft screen was up.** The evidence for "usable" is the animation, the 60 FPS overlay, the atlas
   load, the overworld pipeline and the input response — not a person identifying a menu. `uiautomator` cannot
   see the game at all (it renders into the launcher's own Surface).
2. **That the process was rendering (not merely resident) during the 10:05:48→10:17 window** — the screenshots
   document liveness at nearby moments; `ps` proves only residency.
3. **The `F3` control's pixel bounds** are the operator's measurement, not derivable from any saved XML.
4. **`40-run-logcat.txt` does not cover this run**, so its zero counts for `Sending signal` / `Force stopping`
   say nothing about it; the lifecycle claim rests on `ps`/`pidof` and the screenshots.
5. **The user-facing role did not run.** ALL_PLAYER = BLOCKED.

### Safety
No production source changed, so the 246-test suite did not need re-running. `HEAD b4c9415b`, 56 worktree
entries, nothing staged. Both protected releases re-hashed unchanged: `0.0.1` = `3BD3E79D…715BAB9`,
`0.0.2-beta1` = `66E7552C…F5F4`. Exactly one of the two allowed launches was spent. RDC not used;
`emulator-5554` never contacted; no recrawl, no AI, no cloud, no UX/UI work.

### Evidence
`D:\ALL Mod Doctor Warehouse\crash-lab\milestone-v1-physical-closeout\` — `00-gates.txt`,
`10-arm64-debug-build.log`, `11-arm64-audit.txt`, `11-arm64-zipalign-full.log`, `12-read-route.txt`,
`20-inventory-before.txt`, `21-install.txt`, `22-installed-base.apk`, `23-`/`24-`/`25-sha1-both-sides.txt`,
`30-app-open.xml`, `31-after-tap-12s.xml`, `32-after-skip.xml`, `40-run-logcat.txt` (see "not proven" 4),
`60-run-game-log.log`, `65-lifecycle-fresh.txt`, `68-game-overlay.xml`, `70-RUN-MEASUREMENTS.md`, `99-CLOSEOUT.md`,
and the screenshots `61`-`67`, `eA`/`eB`, `n1`-`n8`, `p1`-`p7`.

### Exact next action
Give the `all-player` session a terminal and let it run the user-flow brief on this phone — `screencap`, press
`เริ่มเกม`, quote the login dialog verbatim, watch `pidof :game` for a minute, and say what the screen showed.
That is the one thing a file cannot settle.

## STATUS NOTE — all-player user proof attempted, BLOCKED (2026-09-25). No PASS is claimed.

A status record, deliberately not a milestone closeout.

The user asked that the `all-player` role be given the minimum capabilities for its own user-facing QA and that
the role itself perform the final proof on the iQOO, explicitly forbidding the operator from substituting for it.
The role was dispatched with the full brief and answered with a capability check first: its session exposes only
`read`, `search`, `ask_main` — **no shell** (hence no adb, `pidof`, `screencap`, `input tap`) and **no ability to
see a screenshot as an image**. It reported BLOCKED, refused to infer the device state from files, and refused to
claim a PASS. Evidence: `crash-lab\milestone-v1-physical-closeout\player-run\99-PLAYER-BLOCKED.md`.

This is a different obstacle from the earlier `all-player` failure, which was a provider error
(`"name" is not supported by this endpoint`); this time the role ran and reported — its tool list is the limit.

**ALL-PLAYER = BLOCKED**, and the blocker is in the team configuration, which the operator cannot change. It needs
a **shell/terminal** and a way to **see an image**.

Attribution is kept strict: the physical run recorded above was performed by the operator, not by the role, and
the Reviewer's two rounds exist only inside the operator's documents rather than as artifacts in the evidence
folder — recorded as an evidence gap, not as a product defect.

Final line from the Lead's decision: **MOD DOCTOR V1 = PARTIAL — the `all-player` role has no shell/adb and cannot
see the screen, so the user-facing proof has never been performed by the role it belongs to.**

## STATUS — all-player capability probe re-run (2026-09-25). No PASS.

The designated `all-player` role re-ran its capability brief in its own session. **Correction to the entry above:
the role now HAS `shell` and used it** — `echo PLAYER_SHELL_OK`; `adb devices -l` →
`10AF681QSC001YK device product:I2405 model:I2405`; property read-back `iQOO`/`I2405`/release 16/sdk 36/`
`arm64-v8a`; `screencap` → a real PNG (`player-run\shot1.png`, 15,580 B, PNG magic verified). It stopped at the
image step and ran nothing else. **ALL_PLAYER = BLOCKED on image viewing only** (`media_read`).

Persisted `…\aetox\agents\all-player\AGENT.md` (re-read verbatim by the Lead this round) declares
`tools: read, search, shell, media_read`, `deny: change, git, shell_kill`, and its body instructs
`media_read image` — so the grant is on disk and the tool is dropped at session assembly; `ask_main` is
delivered while undeclared. Config validation: deny list PASS, tool list FAIL (missing `ask_main`); the file is
not the defect, because the runtime disagrees with it in BOTH directions at once, which proves `tools:` is not
the literal whitelist. No config edit was made or possible (the operator has no write tool for that path and the
role is denied `change`), and none would have helped since the file already grants the missing tool.

No real user QA ran. The physical iQOO run stands as the OPERATOR's work and is not credited to the role.
Evidence: `crash-lab\milestone-v1-physical-closeout\player-run\98-CAPABILITY-PROBE.md` (and
`99-PLAYER-BLOCKED.md`, `shot1.png`).

Final line: **MOD DOCTOR V1 = PARTIAL — the `all-player` role cannot see an image; its session drops
`media_read` although its own role file grants it.**

## STATUS — all-player user proof PERFORMED; MOD DOCTOR V1 CLOSED AS REPORT-ONLY (2026-09-25). Supersedes the two entries above.

Why the earlier blocker was mis-stated: `media_read` is not dropped by the model, the provider, the role file or the team — Aetox serves an agent only the tools of the desk its session runs on. This session and the team `all` are on the coding desk, whose profile declares `categories: files, shell, code, web, agent`; the tool group `media` (`media_read`, `image_ocr`, `pdf_read`, `video_ocr`, `audio_transcribe`, `doc_write`, `sheet_write`) is not in it. The desk's own prompt, embedded in the app, says "the media senses are not on this desk", and `…\aetox\teams\all\TEAM.md` pins `desk: coding`. Every session on this desk served the same 27 tools across 8+ models and 3 providers over ten days, vision-capable ones included, and the app's own guide states that a tool is served to every model equally. Evidence: `aetox-engine.exe` / `aetox.exe` symbol and string tables, `…\aetox\model-catalog.json`, `…\aetox\logs\aetox-20260925-112623.log` ("sending tools: = 4 definitions"), `…\aetox\teams\all\TEAM.md`.

Role correction (made by the operator, outside the project): `…\aetox\agents\all-player\AGENT.md` now declares `tools: read, search, shell` and instructs text evidence — `uiautomator dump`, `dumpsys window`, `logcat`, pulled log files — because this role cannot read a picture on this desk. Configuration drift is recorded, not repaired: the file carries `model: deepseek-v4.1-flash`, not the intended `GLM-5.3-Flash`, and it has no bearing on the tool set.

Run 100 — the role's own clean user QA on the iQOO: opened ALL from its home icon (three installed apps share the label "ALL Launcher"; the first icon opened `com.movtery.zalithlauncher.v2.local`), pressed `เริ่มเกม`, skipped the known account-refresh dialog, reached `:game` with `VMActivity`, game log reading `Loading Minecraft 1.21.11 with Fabric Loader 0.19.5`, `Loading 55 mods:`, `Sound engine started`, and no crash marker, then ended deliberately through the launcher's own force-close. `inspect finished: 0 problem(s)` twice, so no report was shown. Evidence: `player-run\100-PLAYER-QA.md` with `q-*`.

Run 101 — the user-visible report path, one launch with one deliberate fault: the proven Continuity fixture (SHA-1 `51d34c3f2035db0c7fca321b2d94f01b5167707c`) active with `fabric-api-0.141.6+1.21.11.jar` renamed aside, never deleted. The launcher held Minecraft back and showed `ปัญหาม็อด` / `ม็อดที่จำเป็นยังไม่ครบ: Continuity ต้องใช้ fabric-api…`, naming the affected file, with `ยกเลิก` / `แก้ไข` / `เล่นต่อทั้งที่ยังมีปัญหา`. `ยกเลิก` returned to the main window with no game process, and the instance's five mods were restored byte-for-byte (5 of 5 SHA-1 match). `inspect finished: 1 problem(s) [MISSING_REQUIRED_DEPENDENCY]` at 12:58:34.757. Evidence: `player-run\101-PLAYER-REPORT-PATH.md` with `r-*`.

Verified independently by `all-reviewer`. Gaps it could not close, recorded rather than hidden: that run's `pidof` reading exists only in the markdown; `r-logcat.txt` does not cover the run window (only `r-logcat-apponly.txt` does); the on-device fixture hash is quoted in the markdown only.

Final line: **MOD DOCTOR V1 = DONE (report-only) — the pre-launch gate and the user-visible report path are proven on the physical device by the role that owns the user-facing claim. NOT PROVEN: what the shipped `แก้ไข` (Fix) action does, what `เล่นต่อทั้งที่ยังมีปัญหา` does after a fault, whether a stale report reappears on relaunch, the other detector types on this build, and `PLAYABLE` (unchanged — no game session on this device has ever been recorded).**

Next safe action, if the Fix path is to be certified: one launch on the same fixture pressing `แก้ไข`, capturing the response and the mods directory before and after by SHA-1, restoring by hash, stopping at the first divergence. If V1 is to stay report-only, its declared scope is closed and no product action remains.

## 2026-09-25 22:4x — MOD DOCTOR V1 FINAL FIX PROOF ON LDPLAYER — PASS
- LDPlayer `127.0.0.1:5555`, installed APK `cb99a0ea…fd010dd` (current x86_64 debug 0.0.2-beta1). Instance `1.21.11 Fabric 0.19.5`.
- Fixture: Continuity `51d34c3f…` present, fabric-api (`c98467cb…`) removed after hash-verified backup. Axiom (`8fbab015…`, learned-crash record → designed local-disable Fix) parked for the run so the report was exactly one MISSING_REQUIRED_DEPENDENCY; this is a documented fixture deviation, not a product change.
- แก้ไข pressed once → one Modrinth download `P7dR8mSH/6qAuTtLR` → fabric-api restored, SHA-1 `c98467cb…` (identical), no duplicate, no temp/staging, unrelated mods byte-identical → post-fix re-check 0 problems → automatic relaunch (VMActivity 22:38:15) → Sound engine started 22:39:07.
- Restore: final mods SHA-1 set = baseline. Production/test/build changes: none. Protected 0.0.1 release hash re-verified.
- Evidence: `test-artifacts/mod-doctor-v1-final-fix-ldplayer/` (`99-summary.md`).
- Physical iQOO report proof + LDPlayer Fix proof → **MOD DOCTOR V1 = DONE**.

## 2026-09-25 23:0x — MOD DOCTOR V1 FINAL LIVE-PLAY (LDPlayer) — PARTIAL
> **CURRENT STATUS — 2026-09-25 23:0x +07:00 (authoritative; supersedes the status text below):**
> **MOD DOCTOR V1 = PARTIAL — LDPlayer live-play blocked by the emulator killing the game for low memory (not an ALL defect).**
> - Mod Doctor product behavior: DONE. Physical iQOO proved the report path (gate, report, Cancel, 20-min live render loop). LDPlayer proved Fix: Continuity missing fabric-api → แก้ไข once → fabric-api 0.141.6+1.21.11 (`c98467cb…`) installed once → post-fix recheck 0 problems → automatic relaunch → Sound engine started.
> - LDPlayer live-play smoke (22:54): clean gate `inspect finished: 0 problem(s)`, `:game` PID 12802 started, then SIGKILLed at 22:55:43 as a foreground app while loading resources, before the title menu. No crash-report. Android killed the IME at 22:55:32 with "died as low mem" first. LDPlayer RAM = 2 GB. Same pattern as the earlier LDPlayer 2.4 s / ~7 s deaths.
> - To close: give LDPlayer more RAM (e.g. 4 GB) and rerun live-play once, OR accept the iQOO 20-min live render loop as the live-play proof.
> - Production changes: none. Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v1-live-play-ldplayer\`.


## 2026-09-25 23:2x — MOD DOCTOR V1 LIVE-PLAY RETEST (LDPlayer 4 GB) — PARTIAL
> **CURRENT STATUS — 2026-09-25 23:2x +07:00 (authoritative; supersedes all status text below):**
> **MOD DOCTOR V1 = PARTIAL — LDPlayer world entry killed by an emulator GPU-pipe fatal assert (SIGTRAP in libOpenglSystemCommon via gl4es), not by Mod Doctor.**
> - Mod Doctor: product DONE. Clean gate 0 problems (23:12:54). The Fix proof stands (Continuity missing fabric-api → แก้ไข → fabric-api 0.141.6+1.21.11 `c98467cb…` → recheck 0 problems → automatic relaunch). The iQOO report proof stands.
> - LDPlayer 4 GB retest: real Minecraft title screen at 60 FPS (screenshot 02-title.png) → Options opened and closed (04) → Singleplayer world list (07). `:game` PID 3918 stayed alive in usable UI from 23:13:29 to 23:15:46 (~2 min 17 s). Input: adb taps do not reach the game UI, so keyboard events (TAB/ENTER/ESC) were used.
> - Entering the test world `โลกใหม่`: the player joined (23:15:51), then at 23:15:53 LDPlayer's host GL pipe hit assert_fatal (`HostRPC::readback` ← `glGetShaderInfoLog` ← `libng_gl4es`), and at 23:15:56 the process exited with signal 5. Memory was fine (3.1 GB free). No crash-report. The session did not end through an intentional exit.
> - Classification: Android/emulator GPU translation + renderer (gl4es), not Mod Doctor. Production changes: none. The world was restored from backup and the mods SHA-1 = baseline.
> - To close: accept title/menu live-play (>2 min) + the iQOO 20-min render loop as proof, OR retest world entry on LDPlayer with another renderer / on the physical device.
> - Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v1-live-play-ldplayer-4gb\`.

## 2026-09-25 23:3x — MOD DOCTOR V1 WORLD LIVE-PLAY (LDPlayer, Axiom-free, renderer fallback) — PARTIAL
> **CURRENT STATUS — 2026-09-25 23:3x +07:00 (authoritative; supersedes all status text below):**
> **MOD DOCTOR V1 = PARTIAL — no ALL renderer can enter a world on LDPlayer x86_64; world live-play must be proven on a physical device.**
> - **Axiom is diagnostic-only.** Axiom 6.0.5 needs native desktop Linux libraries and must never be in an Android playable baseline. Its fixture lives in `test-artifacts\diagnostic-fixtures\`. It was removed from the LDPlayer playable instance and not reinserted.
> - PLAYABLE_BASELINE = CLEAN (LDPlayer `1.21.11 Fabric 0.19.5`): fabric-api 0.141.6 `c98467cb`, iris 1.10.7 `aae8567b`, sodium 0.8.12 `4d80592b`, sodium-extra 0.9.1 `49783b5a`, reeses 2.2.3 `4f6b87af` (plus 3 inactive `.disabled`). No Axiom, no tmp/part files, Mod Doctor gate 0 problems.
> - Renderer results (4 GB RAM, Axiom absent):
>   - Krypton Wrapper (default, NG-GL4ES): title screen + Options + Singleplayer worked for ~2 min 17 s, but world entry crashed (SIGTRAP, LDPlayer GL host pipe `HostRPC::readback` ← `glGetShaderInfoLog` ← `libng_gl4es`).
>   - Kopper Zink: `No window pointer found: 0` at init (no usable Vulkan/Zink window).
>   - VirGL: OpenGL 4.3 Mesa virgl came up, then the JVM aborted with code 6 during init, before the title screen.
>   - GL4ES: not eligible (max MC 1.21.4).
> - Classification: LDPlayer GPU translation / renderer compatibility. Not Mod Doctor and not an ALL logic bug. Production changes: none.
> - Mod Doctor itself: gate, report (iQOO), Fix + recheck + automatic relaunch (LDPlayer) all PASS.
> - To close: enter a world once on the physical iQOO (arm64, real GPU) with the clean baseline.
> - The renderer setting was restored to its original value (`""` = global default). The world `โลกใหม่` is unchanged. Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v1-live-play-world\`.

## 2026-09-25 23:5x — MOD DOCTOR V1 PHYSICAL WORLD-PLAY PROOF (iQOO) — PASS → MOD DOCTOR V1 = DONE
> **CURRENT STATUS — 2026-09-25 23:5x +07:00 (authoritative; supersedes all status text below):**
> **MOD DOCTOR V1 = DONE. LIVE_PLAY_WORLD = PASS on the physical iQOO I2405 (10AF681QSC001YK, Android 16, arm64).**
> - APK: installed debug `4693808D…CFFE3` (current ARM64 build), used as-is. Renderer: Krypton Wrapper (default).
> - PLAYABLE_BASELINE = CLEAN: fabric-api 0.141.6 `c98467cb`, iris 1.10.7 `aae8567b`, sodium 0.8.12 `4d80592b`, sodium-extra 0.9.1 `49783b5a`, reeses 2.2.3 `4f6b87af`. Axiom absent. **Axiom is diagnostic-only and must never be in an Android playable baseline.**
> - Mod Doctor gate `inspect finished: 0 problem(s)` (23:47:36) → real title screen at 60 FPS → Singleplayer → `โลกใหม่` (a verified copy of the LDPlayer test-world backup; the iQOO had no saves) → world rendered 23:51:24 (village, HUD, hotbar) → moved with the joystick → camera rotated → inventory opened and closed with ESC → `:game` PID 11255 alive 23:51:24–23:54:18 (~2 min 54 s of usable world), 0 crash-reports.
> - Intentional exit: pause → บันทึกและออกไปยังเมนูหลัก (worlds saved) → title → ออกจากเกม at 23:55:37 → `Stopping!` → the process ended itself (normal exit), and ALL returned to its launcher.
> - Evidence split: physical iQOO = report path + live world play; LDPlayer = deterministic Fix (Continuity missing fabric-api → แก้ไข → fabric-api 0.141.6 `c98467cb` → recheck 0 problems → automatic relaunch). LDPlayer world entry is not possible with ALL renderers (emulator limitation, recorded below).
> - Production changes: none. Restore: world copy removed from the iQOO, mods unchanged. Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\mod-doctor-v1-physical-world\`. Next: UI work (not started).

## 2026-09-26 00:3x — MICROSOFT ACCOUNT REFRESH HTTP 400 — FIXED, ACCOUNT_REFRESH = PASS
> **CURRENT STATUS — 2026-09-26 00:3x +07:00 (authoritative; supersedes all status text below):**
> **MOD DOCTOR V1 = DONE (unchanged). ACCOUNT_REFRESH = PASS on the physical iQOO (10AF681QSC001YK).**
> - Microsoft refresh 400 root cause: the very first auth step. `POST https://login.live.com/oauth20_token.srf` returned **400 `invalid_scope`**, "The provided request must include a scope input parameter". This build has no custom client id, so it uses the legacy public Minecraft client on Live. Device-code login sent scope `service::user.auth.xboxlive.com::MBI_SSL`, but `refreshAccessToken()` sent none. Category D (missing required parameter). Not an expired login, not a network error. XBL/XSTS/Minecraft services were never reached.
> - Fix: `MicrosoftAuthenticator.kt` `refreshAccessToken()` appends `scope` via the new `refreshScope()` in `MicrosoftClientId.kt` (legacy client only; the Azure path is unchanged). `LEGACY_MINECRAFT_SCOPE` moved to `MicrosoftClientId.kt`. Test: `MicrosoftClientIdTest` "a legacy refresh carries the same scope as the legacy login"; account tests 8/8 pass.
> - Build: `:ZalithLauncher:assembleDebug -Darch=arm64 --no-daemon --console=plain` run through PowerShell from `D:\ALL001` (Git Bash resolves the junction to the spaced path and ndk-build then fails). APK `ALLLauncher-Debug-0.0.2-beta1-arm64-v8a.apk`, 225,339,011 B, SHA-256 `2F738CD3C67B3313FC397D6D11FF20111B84D9A8BD91FD27142290B2BF265415`. Installed with `install -r` (data kept); the installed base.apk hash equals the build.
> - Physical verification: THE_BOUY2 (Microsoft) → เริ่มเกม 00:30:26 → token refresh OK → XBL → XSTS → Minecraft services → ownership → `Saved account: THE_BOUY2` → no 400 dialog, no ข้ามการรีเฟรช → game PID 31272 → title screen, `Setting user: THE_BOUY2` → Quit Game. ALL closed and reopened → เริ่มเกม → `Saved account` 00:32:14, no error → PID 32740 → title screen → Quit Game. Re-login required: NO. The account persisted.
> - Security: no token values saved; evidence redacted and scanned. Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\account-refresh-400\`. Next: UI work (not started).

## 2026-09-26 01:5x — ALL DESKTOP UI V1 — PASS
> **CURRENT STATUS — 2026-09-26 01:5x +07:00 (authoritative; supersedes all status text below):**
> **ALL DESKTOP V1 = PASS. MOD DOCTOR V1 = DONE and ACCOUNT_REFRESH = PASS (both unchanged).**
> - A separate UI mode: `InterfaceMode { CLASSIC, ALL_DESKTOP }` (new `setting/enums/InterfaceMode.kt`) persisted with the existing MMKV setting `AllSettings.launcherInterfaceMode` (default CLASSIC). Classic `LauncherScreen.kt` is untouched.
> - Switch: Classic → Desktop via Settings › Launcher › "หน้าตาตัวเรียกใช้" (existing ListSettingsCard). Desktop → Classic via Start menu › "กลับไปหน้าแบบคลาสสิก". Proven both ways on LDPlayer; the choice survives an app restart.
> - `MainScreen.kt`: the `LauncherMain` entry renders `AllDesktopScreen` or `LauncherScreen` by mode. The top bar is hidden only on the Desktop home screen; every sub-page keeps it.
> - `ui/screens/content/AllDesktopScreen.kt` (new): drawn sky-and-hill wallpaper; real `VersionsManager.versions` as desktop icons (`VersionIconImage`); draggable XP-style instance window (Minecraft version, loader, account, neutral "Mod Doctor ตรวจเมื่อเริ่มเกม"); minimize to taskbar; taskbar with a green ALL button and clock; Start menu (Desktop, Download, Account, Files, Settings, Classic).
> - Hooks reuse existing paths: Play → `EventViewModel.Event.Launch.Game(version)`, the same event Classic sends. Mods → `NestedNavKey.VersionSettings` + `Versions.ModsManager`. Settings → `navigateToVersions`. Files → `Event.OpenFileManager`. Download → `navigateToDownload()`. Account → `NormalNavKey.AccountManager`.
> - Core systems touched: none (auth, Mod Doctor, LaunchGameOperation, downloader, renderer and the version model are untouched). Also edited: `AllSettings.kt` (+1 setting), `LauncherSettingsScreen.kt` (+1 card), strings EN/TH (+13 each).
> - Build: x86_64 debug via PowerShell from `D:\ALL001`, SHA-256 `0FF6B7BC3F1E74C2D9FB5F9B497C72789995644F26385783187A21463FBF1079`, installed on LDPlayer 127.0.0.1:5555. Evidence screenshots: `D:\ALL Launcher 0.0.1\test-artifacts\all-desktop-v1\` (01 classic, 07 desktop, 08 instance window, 10 start menu + minimized, 12 mods, 14 classic again).
> - Play on Desktop was verified at code level (same event as Classic); Minecraft was not launched for this UI milestone.
> - V2 ideas (deferred): custom wallpaper picker; task list access on the Desktop (the top-bar task menu is hidden there); Mod Doctor status from real persisted health once Known-Good Memory exists; icon placement; more tray items.

## 2026-09-26 08:3x — ALL DESKTOP V1.1 (wallpaper, delete instance, spacing) — PASS
> **CURRENT STATUS — 2026-09-26 08:3x +07:00 (authoritative; supersedes all status text below):**
> **ALL DESKTOP V1.1 = PASS. ALL DESKTOP V1 = PASS, MOD DOCTOR V1 = DONE, ACCOUNT_REFRESH = PASS (all unchanged).**
> - Wallpaper: user file `C:\Users\pornp\Downloads\All XP.png` (PNG 1672×941, SHA-1 `d67096fa…`) bundled as `res/drawable-nodpi/all_desktop_wallpaper.png`; shown with `ContentScale.Crop` + `Alignment.BottomCenter` (aspect kept, only sky trimmed). The drawn sky/hill Canvas remains underneath as fallback. Classic does not use it.
> - Delete Instance: reuses the existing `DeleteVersionDialog` (shows the instance name) → `SimpleTaskDialog` on `Dispatchers.IO` → existing `VersionsManager.deleteVersion()` (deletes the folder + refreshes the list), the same path as Classic `VersionOverViewScreen`. The button is in its own row away from Play and only opens the confirmation; ✕ only closes. After deletion the window closes and the minimized chip clears.
> - LDPlayer delete test on a throwaway copy `QA-DELETE-ME` (copied from the 44 KB vanilla `1.16.4` json folder): Cancel → version list unchanged. Confirm → only `QA-DELETE-ME` removed, icon gone immediately without a restart, window closed.
> - Spacing: grid rows 108→118 dp, column gap 8→20 dp, row gap 6 dp, padding 20/14 dp, icon width 96→100 dp, labels up to 2 lines. Large counts scroll horizontally; icon size and touch targets unchanged.
> - Build: x86_64 debug via PowerShell from `D:\ALL001`, SHA-256 `B94535ECAC345E751761DA03566B579480E0C10C144870CAD73A01EC0C026CBD`, LDPlayer only (iQOO not touched).
> - Classic regression: Classic opens with instance + เริ่มเกม; Desktop→Classic via Start menu persisted across an app restart. LDPlayer left in Classic.
> - Files changed in V1.1: `AllDesktopScreen.kt` only, plus the new wallpaper resource. Core systems untouched. Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\all-desktop-v1.1\`.

## 2026-09-26 08:4x — ALL DESKTOP V1.2 (instance window polish) — PASS
> **CURRENT STATUS — 2026-09-26 08:4x +07:00 (authoritative; supersedes all status text below):**
> **ALL DESKTOP V1.2 = PASS. V1.1 / V1 = PASS, MOD DOCTOR V1 = DONE, ACCOUNT_REFRESH = PASS (all unchanged).**
> - Delete button: the floating red text is now a framed `XpButton` "🗑 ลบเวอร์ชัน" (soft red gradient #FFF4F2→#FBE0DC, red text #B3261E, border #D9837A, same 8 dp radius and 48 dp minimum height as the other buttons, 180 dp wide, right-aligned in its own row below the actions). `XpButton` got a `borderColor` parameter (default unchanged). Behavior is unchanged: it opens the existing `DeleteVersionDialog`; ✕ still only closes.
> - Spacing: info row padding 16 → top 14 / bottom 12, +4 dp before the Mod Doctor line, actions row bottom 16 → 10, delete row bottom 14. Touch sizes unchanged.
> - Build: x86_64 debug via PowerShell from `D:\ALL001`, SHA-256 `E92317F77BB04BEA9957AE5D36FA302C444F437BADB882B845B5BBD004C15458`, LDPlayer 127.0.0.1:5555 only.
> - LDPlayer: your 2 real instances (`1.21.11 Fabric 0.19.5`, `26.3 Fabric 0.19.5`) shown on the All XP wallpaper and left untouched. Window verified: Delete → Cancel changed nothing; minimize → taskbar chip → restore; Mods → existing Mods page; Settings → existing version overview; Files → FileManagerActivity; ✕ closes. No destructive retest (no throwaway instance; the backend was proven in V1.1).
> - Classic regression: Classic opens with the instance and เริ่มเกม. Switched back to ALL Desktop afterwards (your chosen mode).
> - Files changed: `AllDesktopScreen.kt` only. Core systems untouched. Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\all-desktop-v1.2\`.

## 2026-09-26 09:0x — ALL DESKTOP V1 PHYSICAL VERIFICATION (iQOO) — PHYSICAL PASS
> **CURRENT STATUS — 2026-09-26 09:0x +07:00 (authoritative; supersedes all status text below):**
> **ALL DESKTOP V1 = PHYSICAL PASS (iQOO 10AF681QSC001YK). ALL DESKTOP V1.2 = PASS, MOD DOCTOR V1 = DONE, ACCOUNT_REFRESH = PASS (all unchanged).**
> - Wallpaper source safety: `ZalithLauncher/src/main/res/drawable-nodpi/all_desktop_wallpaper.png`, 2,179,713 B, SHA-256 `c46df9b2f1948216c4e66760d89d9d355468b07038fdf28bbf0f227ad0fd4589` (from `Downloads\All XP.png`). Not ignored (`git check-ignore` = no match); git lists the file itself as untracked (`??`), like all uncommitted ALL work. It is a normal project resource: aapt2 shows `drawable/all_desktop_wallpaper` (0x7f080077) → `res/drawable-nodpi-v4/all_desktop_wallpaper.png` in the APK, byte-identical (same SHA-256). No fix needed. Drawn fallback kept.
> - ARM64 build: `:ZalithLauncher:assembleDebug -Darch=arm64 --no-daemon --console=plain` via PowerShell from `D:\ALL001` → `ALLLauncher-Debug-0.0.2-beta1-arm64-v8a.apk`, 225,930,951 B, SHA-256 `6B68D535442C90AEB41932F1DD0E7D36C500EC909372C0ABBD96204062AA020B` (includes ALL Desktop V1.2 + account refresh fix + Mod Doctor). `install -r` (data kept); the installed base.apk hash equals the build.
> - Physical results (Minecraft not launched): All XP wallpaper correct on the 20:9 screen with no stretching; 3 real instances (1.20.1 Forge 47.4.23, 1.21.1 Fabric 0.19.5, 1.21.11 Fabric 0.19.5) readable and large. Window opens; finger drag moves it (+324,+187 px); minimize → taskbar chip → restore; ✕ closes; reopen works. Mods → existing Mods page; Settings → existing version overview; Files → FileManagerActivity (back returns). Delete → confirmation naming `./versions/1.21.11 Fabric 0.19.5` → Cancel: versions unchanged. Start menu: 6 items, closes on outside tap. Desktop → Classic (Classic shows เริ่มเกม) → Desktop via Settings; the mode persisted after force-stop and reopen.
> - Touch UX: buttons, taskbar, ALL button and icons are comfortable; the window leaves the desktop visible. No polish change needed (no code change this round).
> - Core regression: none (no source change). Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\all-desktop-physical\` (one screenshot showing a personal notification was deleted).

## 2026-09-26 09:4x — ALL DESKTOP V2 — PARTIAL (LDPlayer PASS, iQOO not connected)
> **CURRENT STATUS — 2026-09-26 09:4x +07:00 (authoritative; supersedes all status text below):**
> **ALL DESKTOP V2 = PARTIAL — LDPlayer PASS; physical iQOO step not run because 10AF681QSC001YK was not connected to adb (ARM64 APK built and ready).** ALL DESKTOP V1 = PHYSICAL PASS, MOD DOCTOR V1 = DONE, ACCOUNT_REFRESH = PASS (unchanged).
> - V2 changes (all in `ui/screens/content/AllDesktopScreen.kt`, plus `AllSettings.allDesktopWallpaper` and 4 strings EN/TH):
>   - Wallpaper picker (Start › วอลเปเปอร์): [ใช้ All XP] / [เลือกภาพจากเครื่อง] via `ActivityResultContracts.OpenDocument(image/*)` + `takePersistableUriPermission(READ)`. The URI is saved as a string in the existing MMKV setting (empty = All XP) and loaded with coil3 `AsyncImage` (Crop, BottomCenter). `placeholder`/`error` = All XP, so an unavailable URI falls back automatically; the drawn sky/hill is kept underneath. No image copy.
>   - Start menu V2: XP two-column layout. Left: Desktop, Download, Mods (current version, shown only if one is set), Files. Right (light blue): Account, Settings, Wallpaper, About (existing Settings › AboutInfo). Footer: back to Classic. Mod Doctor omitted (no standalone screen).
>   - Taskbar V2: italic ALL button with shadow; the window task button shows pressed/bold when in front and raised when minimized, tapping toggles minimize/restore; ellipsis up to 240 dp; task count from the existing `TaskSystem.tasksFlow` only when > 0; gradient clock tray.
>   - Instance window V2: shared `XpWindowFrame` (icon in the title bar), a header band with the framed 56 dp icon, info + neutral "Mod Doctor ตรวจเมื่อเริ่มเกม", large Play, Mods/Settings/Files, framed Delete. Drag is clamped inside the desktop area.
>   - Icons: 104 dp, label text shadow, press scale, selected highlight for the open instance.
>   - Animations: Start menu slide+fade (≈160 ms), window scale+fade (≈160 ms, also used for minimize/restore), button/icon press scale. No blur, no polling.
> - Builds: x86_64 SHA-256 `040EBC6660D31B62845A3CE41930ED07D112FC664789F06D4B60E357B8BCD640`; ARM64 225,958,711 B, SHA-256 `ED6E675D5BD2868805B7690A62F41A8A3C131F89C390991369169B02B2B312B5` (built, not installed).
> - LDPlayer PASS: All XP default; picked `h1.jpg` via DocumentsUI → applied (crop, no stretch) → survived restart; "ใช้ All XP" restored the default. Start menu two columns, About → Settings › About. Instance window, far drag clamped on-screen, taskbar toggle minimize/restore, minimize button, close (chip removed), Mods/Settings/Files → existing screens, Delete → Cancel unchanged. Classic → Desktop, mode persisted after restart. The URI-unavailable fallback is verified at code level only.
> - Classic untouched; core systems untouched. Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\all-desktop-v2\`.
> - Next: connect the iQOO, install the ARM64 APK above, run the physical UI checks (wallpaper/picker/touch/start/taskbar/window/Classic).

## 2026-09-26 10:1x — START MENU GAP = FIXED
> **CURRENT STATUS — 2026-09-26 10:1x +07:00 (authoritative; supersedes all status text below):**
> **START MENU GAP = FIXED (LDPlayer).** ALL DESKTOP V2 = PARTIAL (iQOO step pending), V1 = PHYSICAL PASS, MOD DOCTOR V1 = DONE, ACCOUNT_REFRESH = PASS.
> - Cause: the menu width cap (≤460 dp) was larger than the two fixed 220 dp columns (440 dp), so the full-width header/footer were wider than the body and the wallpaper showed through on the right.
> - Fix (`AllDesktopScreen.kt` StartMenu only): menu `fillMaxWidth(0.5f).widthIn(360.dp, 480.dp)`; body Row `fillMaxWidth().height(IntrinsicSize.Min)`; columns `weight(0.59f)` / `weight(0.41f)` + `fillMaxHeight()`.
> - x86_64 SHA-256 `CE1D2E44FA25DD5176E85551C9DFE738E2EE8FCAF9D7C1256886239DA34FA015` on LDPlayer; screenshot `test-artifacts\all-desktop-v2-startfix\start-menu.png`. ARM64 not rebuilt yet (more UI work coming).

## 2026-09-26 10:3x — COMPUTER THEME / XP THEME — INSTANCE SETTINGS WINDOW V1 — PASS
> **CURRENT STATUS — 2026-09-26 10:3x +07:00 (authoritative; supersedes all status text below):**
> **COMPUTER THEME / XP THEME — INSTANCE SETTINGS WINDOW V1 = PASS (LDPlayer).** START MENU GAP = FIXED; ALL DESKTOP V2 = PARTIAL (iQOO step pending); MOD DOCTOR V1 = DONE; ACCOUNT_REFRESH = PASS.
> - Terminology: **Computer Theme** = the desktop/window/taskbar UI system (implemented as `InterfaceMode.ALL_DESKTOP` / `AllDesktopScreen.kt`); **XP Theme** = its current primary visual theme; **Classic UI** = the original launcher UI, preserved separately.
> - Feature: Instance window → "การตั้งค่า" opens a floating XP window "⚙ การตั้งค่า — <instance>" on the desktop (does not leave the desktop; the Instance window stays open). Left rail = the three existing categories: การตั้งค่าเฉพาะเวอร์ชัน (isolation, skip integrity, renderer, Vulkan driver, Graphics API, controls), การตั้งค่าเฉพาะเกมนี้ (Java runtime, memory, custom info, JVM args, auto-join server), การตั้งค่าสนับสนุนเพิ่มเติม (vibration, Vulkan check, microphone check). Draggable (clamped), minimize → "⚙ <name>" taskbar chip → restore, close; focus/z-order between the Instance and Settings windows; `key(version)` so another instance shows its own values.
> - Reuse, no duplication: the existing `VersionConfigs`/`GameConfigs`/`SupportConfigs` in `VersionConfigScreen.kt` changed `private` → `internal` (visibility only) and are embedded directly with `version.getVersionConfig()` and the same `config.saveOrShowError` immediate save. Callbacks passed from MainScreen: `VulkanCheck` event, `sendToast`, `submitError`. Classic still uses its full-screen `VersionConfigScreen` unchanged.
> - Shared-state proof (instance 26.3 Fabric 0.19.5, harmless "ข้ามการตรวจสอบความสมบูรณ์"): Computer Theme set ENABLE → `version.config` on disk = ENABLE → close/reopen window shows เปิดใช้งาน → Classic version settings shows เปิดใช้งาน → restored in Classic to ตามการตั้งค่าหลัก → disk FOLLOW_GLOBAL, file identical to before → Computer Theme window shows ตามการตั้งค่าหลัก.
> - Other actions: Play/Mods/Files unchanged; Delete → Cancel left versions unchanged. Start menu gap still fixed (checked at 1920×1080).
> - Files changed: `AllDesktopScreen.kt` (InstanceSettingsWindow, TaskChip taskbar list, window focus/layering), `VersionConfigScreen.kt` (3× private→internal), `MainScreen.kt` (3 callbacks passed). Core systems untouched.
> - Build: x86_64 SHA-256 `3A6141EEC9188B704F30FBECE98EC8B8528E447EEEF820FB043800868F8ED6ED` on LDPlayer 127.0.0.1:5555 (now 1920×1080). ARM64 not rebuilt (by request). Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\instance-settings-window-v1\`.

## 2026-09-26 10:5x — COMPUTER THEME / XP THEME — FLOATING MODS WINDOW V1 — PASS
> **CURRENT STATUS — 2026-09-26 10:5x +07:00 (authoritative; supersedes all status text below):**
> **COMPUTER THEME / XP THEME — FLOATING MODS WINDOW V1 = PASS (LDPlayer).** INSTANCE SETTINGS WINDOW V1 = PASS; START MENU GAP = FIXED; ALL DESKTOP V2 = PARTIAL (iQOO step pending); MOD DOCTOR V1 = DONE; ACCOUNT_REFRESH = PASS.
> - Feature: the Instance window "ม็อด" button (and Start menu › ม็อด for the current instance) opens a floating XP window "🧩 ม็อด — <instance>" on the desktop instead of the full-screen page. Classic keeps its full-screen Mods page unchanged.
> - Reuse, no duplication: the body of the existing `ModsManagerScreen` (ModsManageViewModel/ModsUpdaterViewModel, filter/sort/search header, `ModsList`, enable/disable via `localMod.enable()/disable()`, delete via the existing `ModsOperation` confirmation, delete-all, update, import, refresh) was lifted verbatim into `internal fun ModsManagerContent(...)` in `ModsManagerScreen.kt`; the Classic screen now calls it inside the same `BaseScreen`/animation (pure extraction, 39+/10-). The floating window embeds the same function with that instance's `VersionFolders.MOD` dir and `key(version)`. "Download" and mod "info" open the existing full-screen download / AssetInfo pages.
> - Window system: focus generalized to 3 windows (instance / settings / mods); the focused window draws on top; the taskbar shows one chip per open window ("🧩 …", "⚙ …"): in front = pressed, tap minimizes; behind = tap brings to front; minimized = tap restores. Drag clamped to the desktop.
> - LDPlayer proof (1.21.11 Fabric 0.19.5): real list with correct enabled/disabled states. Enabled the spare `reeses-sodium-options-fabric-2.2.4…jar.disabled` in Computer Theme → disk renamed to `.jar` → close/reopen showed it enabled → Classic Mods page showed it checked → disabled again in Classic → mods dir SHA-1 list identical to before. Search ("iris") filtered and cleared; refresh ran; Delete (trash) → existing confirmation → Cancel left files unchanged; drag/clamp, minimize, taskbar restore, close OK; Settings + Mods + Instance windows coexist (3 taskbar chips); Start menu gap still fixed.
> - Performance: uses the existing lazy `ModsList` and ViewModel (cached mod loading); no new scanning, no per-row animation.
> - Test-run incident: a BACK key left ALL and later taps landed in LDStore (an app-store game page); nothing on that page was tapped; returned with HOME.
> - Files changed: `ModsManagerScreen.kt` (extraction), `AllDesktopScreen.kt` (ModsWindow, 3-window focus/taskbar, Start menu Mods), `MainScreen.kt` (+eventViewModel). Core systems untouched.
> - Build: x86_64 SHA-256 `DCDFF1D0D152DDF67706B131F4807DD939024928CF328EB3F2FB2EEC345B4A9C`. ARM64 not rebuilt. Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\floating-mods-window-v1\`.
> - Gaps: the reused Mods content keeps its Material look inside the XP frame; Download/Info still open full-screen pages; the mod-update and delete-all flows were not exercised in this run.

## 2026-09-26 11:1x — COMPUTER THEME / XP THEME — FLOATING FILES WINDOW V1 — PASS
> **CURRENT STATUS — 2026-09-26 11:1x +07:00 (authoritative; supersedes all status text below):**
> **COMPUTER THEME / XP THEME — FLOATING FILES WINDOW V1 = PASS (LDPlayer).** FLOATING MODS WINDOW V1 = PASS; INSTANCE SETTINGS WINDOW V1 = PASS; START MENU GAP = FIXED; ALL DESKTOP V2 = PARTIAL (iQOO step pending); MOD DOCTOR V1 = DONE; ACCOUNT_REFRESH = PASS.
> - Feature: the Instance window "ไฟล์" button opens a floating XP window "📁 ไฟล์ — <instance>" on the desktop. Start › ไฟล์ opens it for the current (selected) instance, or falls back to the existing general file manager when none is selected. Classic keeps launching the separate FileManagerActivity unchanged.
> - Reuse, no duplication: the window embeds the existing `FileManagerRootScreen` with the existing Hilt `FileManagerViewModel`, created the same way `FileManagerActivity` does (Hilt factory + `KEY_ROOT_PATH` in default args), with root = `version.getGameDir()` (the same root the classic per-instance action uses). No file-manager source changed; path scope, operations, dialogs and trash are the original implementation.
> - Supported (from the reused UI): folder navigation, back/forward, up (clamped at the instance root; cannot go above it), scrolling grid, create file/folder, and per-item edit/rename/compress/copy/cut/delete (trash optional)/properties/share, plus the existing overflow menu.
> - Shared filesystem proof (1.21.11 Fabric 0.19.5): created `ALL-FILES-QA.txt` with the existing Create dialog in the floating window → on disk → still listed after close/reopen → Classic File Manager (Classic › version settings › โฟลเดอร์เวอร์ชัน) listed the same file → deleted there with the existing delete confirmation ("move to trash" unticked, since it was our own empty test file) → instance root listing identical to before; no copy left anywhere.
> - Window system: 4th window type; Instance + Settings + Mods + Files coexist with 4 taskbar chips (📁 / 🧩 / ⚙); drag clamped; minimize → chip → restore; close. Start menu gap still fixed; Play/Delete Version unchanged (Delete → Cancel left versions unchanged).
> - Performance: the existing ViewModel-driven lazy grid; no new scanning or polling.
> - Files changed: `AllDesktopScreen.kt` only (FilesWindow, WINDOW_FILES layer/chip, instance/Start menu wiring). Core systems untouched.
> - Build: x86_64 SHA-256 `E3B4780CEC73E0CC9896642B7ED0F6ED958BD3BFAA4699DF030899AC4EF0475D`. ARM64 not rebuilt. Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\floating-files-window-v1\`.
> - Gaps: the reused file manager keeps its Material look inside the XP frame; its "orientation toggle" button (Activity-only) does nothing inside the window; the opening of files in external apps / text editor was not exercised in this run.

## 2026-09-26 11:4x — COMPUTER THEME / XP THEME — TOOL ROUTING + FLOATING DOWNLOAD V1 — PASS
> **CURRENT STATUS — 2026-09-26 11:4x +07:00 (authoritative; supersedes all status text below):**
> **COMPUTER THEME / XP THEME — TOOL ROUTING + FLOATING DOWNLOAD V1 = PASS (LDPlayer).** FLOATING FILES / MODS / INSTANCE SETTINGS WINDOW V1 = PASS; START MENU GAP = FIXED; ALL DESKTOP V2 = PARTIAL (iQOO step pending); MOD DOCTOR V1 = DONE; ACCOUNT_REFRESH = PASS.
> - Rule: **Start menu = global Computer Theme tools; Instance window = tools for that one instance.** Start › ม็อด / ไฟล์ no longer assume the selected instance — they open an XP "เลือก Instance" picker (LazyColumn over the real `VersionsManager.versions`, existing icons, ✕/outside-tap cancels, closes on pick) → that instance's floating Mods / Files window. Instance-window ม็อด / ไฟล์ / การตั้งค่า still go directly to that instance.
> - Routing proof: Start › ม็อด → 1.21.11 → "🧩 ม็อด — 1.21.11" (1.21.11 mods); again → 26.3 → "🧩 ม็อด — 26.3" listing exactly its 5 disk mods (fabric-api 0.161.0+26.3, iris 1.11.6, reeses 2.2.4, sodium-extra 0.9.4, sodium 0.9.2). Start › ไฟล์ → 1.21.11 → root `versions/1.21.11 Fabric 0.19.5`; → 26.3 → root `versions/26.3 Fabric 0.19.5`. Picker ✕ cancels.
> - Floating Download: Start › ดาวน์โหลด opens "⬇ ดาวน์โหลด" on the desktop. Reuse: the body of the existing `DownloadScreen` (category rail + all download sub-screens) extracted into `internal fun DownloadContent(key, mainScreenKey, …)` in `DownloadScreen.kt`; Classic calls it with `mainScreen.currentKey` (unchanged behavior); the window passes the download key itself as `mainScreenKey` so the sub-screens' existing visibility checks pass on the desktop. Same `ScreenBackStackViewModel.downloadScreen` nav stack, same ViewModels, providers, task system, install logic. No second downloader.
> - Verified: 7 existing categories (เกม, มอดแพ็ค, มอด, รีซอร์สแพ็ค, โลก, เชเดอร์แพ็ก, ค้นหาด้วยรหัส); Minecraft version list loaded; Mods category loaded real Modrinth results (3815 pages); name search "lithium" → Lithium / Lithium Raycast Fix; drag clamped; minimize → "⬇ ดาวน์โหลด" chip → restore; close. No install performed (browse/search only).
> - Mods window › ดาวน์โหลด now opens the floating Download window on the existing `downloadModScreen` category (no extra version/loader pre-scoping, which would need new state); the Mods window stays open behind it.
> - Multi-window: Instance + Settings + Mods + Files + Download coexisted with 5 taskbar chips; tapping the ⬇ chip brought Download to the front. Start menu gap still fixed; Delete Version → Cancel unchanged; Play unchanged. Classic full-screen Download page ("ดาวน์โหลด - เกม") unchanged.
> - Files changed: `DownloadScreen.kt` (extraction + mainScreenKey parameter), `AllDesktopScreen.kt` (InstancePicker, DownloadWindow, Start menu routing, 5th window/chip, TaskChip version nullable), `MainScreen.kt` (+modpackImportViewModel), strings EN/TH (+1 each). Core systems untouched.
> - Build: x86_64 SHA-256 `2FC62E92634090E2FFAE0ABA80ECB20260CB9CF7EF78E3EA48FB904FD2704467`. ARM64 not rebuilt. Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\tool-routing-download-v1\`.
> - Gaps: with 5 chips the last taskbar button is clipped at the right edge (no overflow handling yet); the Download window shares the Classic download nav/search state (by design, same backend); an actual install through the window and its progress UI were not exercised; download sub-pages that navigate the main screen (e.g. install flows) may still leave the desktop.

## 2026-09-26 — NEXT TASK CLARIFICATION (docs only)
> **CURRENT STATUS — 2026-09-26 (authoritative for NEXT; supersedes every older "Next"/"NEXT" line in this file):**
> - **CURRENT VERIFIED BASELINE:** COMPUTER THEME TOOL ROUTING + DOWNLOAD V1 = PASS — x86_64 SHA-256 `2FC62E92634090E2FFAE0ABA80ECB20260CB9CF7EF78E3EA48FB904FD2704467`.
> - **ACTIVE NEXT TASK: Computer Theme / XP Theme — Taskbar Overflow V1.** Reason: with Instance + Settings + Mods + Files + Download open (5 windows), the last taskbar chip is clipped at the right edge.
> - **Pending verification (NOT the active next task):** iQOO physical validation of ALL Desktop V2 and the floating windows (needs a rebuilt ARM64 APK).
> - **Crash Lab NEXT (QUEUE B2 / QUEUE A attribution / platform-capability filter / harness `fabric`-id gap) is NOT active.**
> - Do not touch protected releases (`D:\ALL Launcher Releases\0.0.1`, `D:\ALL Launcher Releases\0.0.2-beta1\`). Preserve the dirty worktree (41 modified + 22 untracked, **0 staged**, HEAD `b4c9415b`).
> - This update changed docs only: no source code, no build, no RDC.
> - **MEMPALACE SYNC PENDING — remote session lacks MemPalace access.** Wing `project_all_launcher` not read or written; SQLite not touched.

## 2026-09-26 14:1x — FLOATING DOWNLOAD ACTUAL INSTALL — PASS
> **CURRENT STATUS — 2026-09-26 14:1x +07:00 (authoritative; supersedes all status text below):**
> **COMPUTER THEME — FLOATING DOWNLOAD ACTUAL INSTALL = PASS (LDPlayer).** TOOL ROUTING + FLOATING DOWNLOAD V1 = PASS; FLOATING FILES / MODS / INSTANCE SETTINGS WINDOW V1 = PASS; START MENU GAP = FIXED; ALL DESKTOP V2 = PARTIAL (iQOO step pending); MOD DOCTOR V1 = DONE; ACCOUNT_REFRESH = PASS.
> - Real install proof through the floating Download window (build x86_64 `2FC62E92…4467`, unchanged — no code change needed): Start › ดาวน์โหลด › มอด › search "modmenu" → Mod Menu project page (inside the window) → 1.21.11 Fabric › v17.0.1 → the existing install dialog (target 1.21.11 Fabric 0.19.5 preselected; required deps: Fabric API already installed/unticked, Text Placeholder API ticked) → ติดตั้ง.
> - Existing backend did the work: `FileDownloader` fetched `cdn.modrinth.com/.../mOgUt4GM/versions/cLkYaVuL/modmenu-17.0.1.jar` and `.../eXts2L7r/versions/qxjzQ9xY/placeholder-api-2.8.2+1.21.10.jar`; `DownloadSingle` cleared its cache. Files landed in `versions/1.21.11 Fabric 0.19.5/mods`: `modmenu-17.0.1.jar` (960,882 B, SHA-1 `d5612d70…7d19`) and `placeholder-api-2.8.2+1.21.10.jar` (268,662 B, SHA-1 `bbdc920e…ce40`) — both equal to Modrinth's published SHA-1.
> - Progress/task: the existing `TaskSystem` ran 2 tasks; the taskbar tray showed "2 งาน" during the install and cleared when done. The window stayed on the desktop (MainActivity, no navigation away) before, during and after. The floating 1.21.11 Mods window then listed Mod Menu and Text Placeholder API.
> - Restore: both mods deleted through the Mods window's existing delete confirmation ("ต้องการลบม็อด … ใช่ไหม") → mods dir SHA-1 list identical to the pre-test snapshot; no leftover download copies. (`config/modmenu.json` pre-existed from 2026-09-16 and was left alone.)
> - Observation, not caused by this test: at test start the 1.21.11 mods dir held only `fabric-api-0.141.6+1.21.11.jar` (dir mtime 10:57, after the earlier Mods-window test had verified the full 8-file set at ~10:51); iris/sodium/sodium-extra/reeses were gone. Not restored — cause unknown (possibly user action).
> - Gaps: the detailed per-task progress list lives in the Classic top-bar task menu, which the desktop hides; the desktop only shows the task count. Taskbar overflow with 5+ chips still clipped (out of scope). Modpack/game-version installs through the window not exercised.
> - Files changed: none. Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\floating-download-install-v1\`.

## 2026-09-26 14:4x — COMPUTER THEME — TASKBAR OVERFLOW — PASS
> **CURRENT STATUS — 2026-09-26 14:4x +07:00 (authoritative; supersedes all status text below):**
> **COMPUTER THEME — TASKBAR OVERFLOW = FIXED / PASS (LDPlayer).** FLOATING DOWNLOAD ACTUAL INSTALL = PASS; TOOL ROUTING + FLOATING DOWNLOAD V1 = PASS; FLOATING FILES / MODS / INSTANCE SETTINGS WINDOW V1 = PASS; START MENU GAP = FIXED; ALL DESKTOP V2 = PARTIAL (iQOO step pending); MOD DOCTOR V1 = DONE; ACCOUNT_REFRESH = PASS.
> - Root cause: taskbar window chips were a plain `forEach` inside the taskbar `Row`, each up to 220 dp wide, followed by `Spacer(weight(1f))`; the chip group had no width bound and no scrolling, so with 5 chips the last one was squeezed/clipped against the clock tray.
> - Fix (`AllDesktopScreen.kt`, Taskbar only): chips moved into a `LazyRow` with `weight(1f)` between the ALL button and the tray (the Spacer is gone); chips keep their size (40 dp tall, max 220 dp) and the row scrolls horizontally; `LaunchedEffect(frontIndex, chips.size)` → `animateScrollToItem(frontIndex)` keeps the front window's chip in view. ALL button, task count and clock tray unchanged. At most 5 chips can exist (one per window type), and more would simply scroll.
> - LDPlayer proof (build x86_64 `93FA1BEA…4655`): Instance + Settings + Mods + Files + Download open → ALL button, all chips and clock laid out without clipping of the front chip; swipe reveals the others. Each of the 5 chips: tap → window to front, tap again → minimized, tap again → restored (all 5 correct; the chip auto-scrolls into view). Start menu opens with 5 windows open (gap still fixed); clock intact; closing all leaves ALL + clock.
> - Files changed: `ZalithLauncher/.../ui/screens/content/AllDesktopScreen.kt` (Taskbar). Classic UI and download backend untouched. ARM64 not rebuilt.
> - Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\taskbar-overflow-v1\` (00 before = previous build 2FC62E92 five-window clipping; 01 after five windows; 02 swiped; 03 instance front; 04 instance minimized; 05 Start menu with 5 windows; 06 all closed).
> - Gaps: no explicit "more" indicator — the partially visible chip at the scroll edge is the only overflow hint; physical iQOO not re-verified for V2 windows.

## 2026-09-26 16:1x — COMPUTER THEME — WOBBLY / BOUNCY WINDOW V1 — PASS
> **CURRENT STATUS — 2026-09-26 16:1x +07:00 (authoritative; supersedes all status text below):**
> **COMPUTER THEME — WOBBLY / BOUNCY WINDOW V1 = PASS (LDPlayer).** TASKBAR OVERFLOW = PASS; FLOATING DOWNLOAD ACTUAL INSTALL = PASS; TOOL ROUTING + FLOATING DOWNLOAD V1 = PASS; FLOATING FILES / MODS / INSTANCE SETTINGS WINDOW V1 = PASS; START MENU GAP = FIXED; ALL DESKTOP V2 = PARTIAL (iQOO physical validation BLOCKED — device not connected at last attempt); MOD DOCTOR V1 = DONE; ACCOUNT_REFRESH = PASS.
> - Implementation (`AllDesktopScreen.kt` only): one shared `Modifier.wobbly(target, clamp)` replaces `.offset { offset }` on all 5 floating windows (Instance, Settings, Mods, Files, Download). The existing drag logic and `clamp` are unchanged and still own the target position; the displayed position follows it with `Animatable<Offset>` + `spring` (launched per target change — no polling), and is passed through the same `clamp`, so overshoot never leaves the desktop. A small tilt `rotationZ = velocity.x / 1100` clamped to ±2.5° via `graphicsLayer`. The animated value is read only in the offset/graphicsLayer lambdas (layout/draw phase), so it does not recompose the window content. No mesh/shader deformation, no blur.
> - Spring parameters: drag `spring<Offset>(dampingRatio = 0.58f, stiffness = 650f)`; tilt `velocity.x / 1100f` clamped ±2.5°; open/restore pop `scaleIn(spring(dampingRatio = 0.62f, stiffness = 520f), initialScale = 0.9f)` (was `tween(160)`, 0.92) on all 5 windows. Exit / fade unchanged.
> - LDPlayer proof (x86_64 `AC4EFC0F…B22D`): during a 900 ms title-bar swipe, an in-drag screencap shows the window trailing the finger, then it settles exactly on the drag end position (06-drag-sheet); hard drags to far right/down and far left/up on Settings/Mods/Files/Download stop at x≈18 and the right edge ≈1900/1920 (clamp intact); Instance window clamps at the right/bottom edge. Focus → minimize → restore via the taskbar correct for all 5 windows with 5 open; Start menu opens with 5 windows; closing all leaves ALL + clock.
> - Evidence limits: LDPlayer `screenrecord` produced only 1–6 frames and `input motionevent` is not supported on its Android version, so the overshoot and tilt were not captured frame-by-frame — the spring lag during a drag and the correct settle are captured; the overshoot/tilt values come from the parameters above and are subtle by design.
> - Files changed: `ZalithLauncher/.../ui/screens/content/AllDesktopScreen.kt`. Classic UI and download backend untouched. ARM64 not built.
> - Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\wobbly-window-v1\`.
> - Gaps: visual tuning on a real 120 Hz phone (iQOO) not done; no user toggle to turn the effect off; overshoot/tilt not frame-captured on the emulator.


## 2026-09-26 19:4x — COMPUTER THEME — A1 CLEANUP + A2 FLOATING MODPACK INSTALL — PASS
> **CURRENT STATUS — 2026-09-26 19:4x +07:00 (authoritative; supersedes all status text below):**
> **COMPUTER THEME — A1 FLOATING GAME VERSION INSTALL = PASS (cleanup DONE); A2 FLOATING MODPACK INSTALL = PASS (LDPlayer).** Wobbly V1, Taskbar Overflow, Floating Download install, Tool Routing, Files/Mods/Settings windows = PASS; ALL DESKTOP V2 = PARTIAL (iQOO pending); MOD DOCTOR V1 = DONE; ACCOUNT_REFRESH = PASS.
> - A1 (done in the previous session): Minecraft 1.21.11 installed via Floating Download as throwaway `QA-GAME-1.21.11` (client JAR 31,152,600 B, SHA-1 `ba2df812…60f5bd`). Cleanup this session: identity re-verified (same SHA-1/size, created 17:09, only jar/json/version.config), folder removed, app relaunched → Desktop shows only the 2 user instances.
> - A2: Start › ดาวน์โหลด › มอดแพ็ค → platform Modrinth → search "FPS Modpack" → FPS Modpack (MIRRONAKE) detail → 1.21.11 Fabric › 2.9 → existing warning → existing name dialog (named `QA-MODPACK-FPS`) → existing progress dialog (stages, 4/9 mods, MB/s, Cancel; Vanilla 1.21.11 + Fabric 0.18.4 dependency stages) → "การติดตั้งเสร็จสมบูรณ์". Stayed on Desktop throughout.
> - Proof: 9/9 mods SHA-1 identical to the pack's `modrinth.index.json`; 9/9 override files present with identical SHA-1; version JSON mainClass `KnotClient`, `fabric-loader:0.18.4`; desktop icon (pack art) appeared without relaunch (VersionsManager refresh); Instance window shows Minecraft 1.21.11 / Fabric 0.18.4; floating Mods window lists the pack's mods. System Back inside the window returns detail → list without leaving the Desktop.
> - Cleanup: `QA-MODPACK-FPS` removed through the app's own Delete Version (dialog scoped to `./versions/QA-MODPACK-FPS`); user instances byte-identical to the pre-test md5 baseline (logs/.fabric excluded); search platform restored to CurseForge. Shared caches (Fabric 0.18.4 loader libraries, vanilla assets) intentionally left as normal launcher cache. Pre-existing `files/qa-stage/*.mrpack` from earlier sessions untouched.
> - No bug found → no source change, no build. Build in force unchanged (Wobbly V1 x86_64 `AC4EFC0F…B22D`). ARM64 not built; iQOO not tested.
> - Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\computer-theme-comprehensive-v1\A-download\` (a1-cleanup-*, a2-*).
> - Git: 41 modified + 22 untracked, 0 staged, HEAD `b4c9415b` (unchanged). Protected releases untouched.
> - **NEXT: A3 of the computer-theme-comprehensive-v1 plan (not started).**


## 2026-09-26 20:0x — COMPUTER THEME — A4 FLOATING RESOURCE PACK INSTALL — PASS
> **CURRENT STATUS — 2026-09-26 20:0x +07:00 (authoritative; supersedes all status text below):**
> **COMPUTER THEME — A4 FLOATING RESOURCE PACK INSTALL = PASS (LDPlayer).** A1 Game / A2 Modpack / A3 Mod install = PASS (not re-tested); Wobbly V1, Taskbar Overflow, Tool Routing, Files/Mods/Settings windows = PASS; ALL DESKTOP V2 = PARTIAL (iQOO pending); MOD DOCTOR V1 = DONE; ACCOUNT_REFRESH = PASS.
> - Flow: Start › ดาวน์โหลด › รีซอร์สแพ็ค → platform Modrinth → search "Low On Fire" → Low On Fire (Haikis) detail → 1.21.11 › "Low On Fire 26.2" → existing target dialog (preselected 26.3; changed to `1.21.11 Fabric 0.19.5` only) → ติดตั้ง. Tray showed "1 งาน" during the task, cleared after. Stayed on Desktop.
> - Proof: file `versions/1.21.11 Fabric 0.19.5/resourcepacks/LowOnFire v26.2§8.zip` (isolation ENABLE), 42,760 B, SHA-1 `96932a034fff4cd0cad08ba12db730450c4772da` = Modrinth `version_file` lookup (project RRxvWKNC, "Low On Fire 26.2", same filename/size). No copy in 26.3 or global `resourcepacks/`. The instance's floating Files window lists the pack in `resourcepacks` (41.76 KB). In-game pack selection not exercised (no launch).
> - Cleanup: removed the zip and the `resourcepacks/` folder (absent before the test); user instances md5-identical to the pre-test baseline (logs/.fabric excluded); search platform restored to CurseForge.
> - No bug → no source change, no build. Build in force unchanged (Wobbly V1 x86_64 `AC4EFC0F…B22D`). Git 41M + 22??, 0 staged, HEAD `b4c9415b`. Protected releases untouched.
> - Evidence: `D:\ALL Launcher 0.0.1\test-artifacts\computer-theme-comprehensive-v1\A-download\a4-*`.
> - **NEXT: A5 — Floating World install (not started).**


## 2026-09-27 — COMPUTER THEME — F INSTANCE ROUTING (RESUME) — BLOCKED (not run)

> **CURRENT STATUS — 2026-09-27. F (Instance Routing) = NOT RUN / BLOCKED. G NOT STARTED.**
> - The F checklist (B→Mods, B→Files, B→Settings, repeated A↔B switching with no state leak, minimize/restore,
>   focus change, E position-preservation regression, final logcat/crash scan, cleanup + baseline diff) needs live
>   LDPlayer `127.0.0.1:5555` execution over adb.
> - The execution environment denied every shell command, including the single pre-approved validation command
>   `python -c import sys;sys.exit(0)`. No adb, `uiautomator dump`, `dumpsys`, `logcat`, screenshot, or baseline
>   hash/diff could be run. Therefore no F item was exercised and no new F evidence exists.
> - `PROJECT_STATE.md` / a dedicated handoff file are absent at the repo root, the campaign root, and
>   `test-artifacts/`; this core checkpoint is the canonical state and it does not yet record B–F.
> - Existing on-disk F evidence is partial only: `test-artifacts\computer-theme-comprehensive-v1\F-routing\`
>   holds `f-00-before.md5` and screenshots `f-01`…`f-07`; there is no F summary/report and G/H/I/J/K are empty.
> - No source edit, no rebuild, no install, and no state marked PASS in this attempt. NEXT remains
>   F (Instance Routing), then G — Shared State Classic/Desktop.

## PHASE 2.1 — ALL-owned Mods window (Computer Theme)

STATUS: PASS (Debug, device-verified on LDPlayer 127.0.0.1:5555)

BEFORE
  ALL Mods window -> AllBridge.AllModsManager -> Zalith ModsManagerContent (Zalith UI embedded)

AFTER
  ALL Mods window -> AllModsWindow (ALL UI, no Zalith presentation)
                  -> AllModsViewModel -> AllModsService -> AllModReader / LocalMod (.disabled rename)
                  -> rememberAllModsImportAction -> Zalith import task builder (backend only)

FILES ADDED (ALL-owned, 632 lines)
  all/mods/AllModsWindow.kt      410
  all/mods/AllModsViewModel.kt    81
  all/mods/AllModsService.kt      68
  all/mods/AllModsImport.kt       46
  all/mods/AllModsState.kt        27

FILES CHANGED (ALL-owned only)
  ui/screens/content/AllDesktopScreen.kt  1516 -> 1501  (ModsWindow keeps only the window shell)
  all/bridge/AllBridge.kt                  222 -> 192   (AllModsManager removed as dead code)

ZALITH FILES CHANGED: 0
REMAINING MODS DEPENDENCIES ON ZALITH UI: 0

BACKEND REUSED INTENTIONALLY
  AllModReader, LocalMod.enable/disable + File.isEnabled/.disabled rename, FileUtils delete,
  rememberMultipleUriImportTaskBuilder + TaskSystem, VersionFolders, extensionToMimeType,
  AllSubmitError / AllToast aliases through AllBridge

DEVICE TESTS (Debug 6C317879...)
  1.21.11 Fabric 0.19.5 -> 1 mod (Fabric API 0.141.6+1.21.11), counts 1/0
  26.3 Fabric 0.19.5    -> 4 mods (iris / reeses-sodium-options / sodium-extra / sodium, all mc26.3), counts 4/0
  no instance bleed; enable/disable round trip renamed the real file and back
  delete: confirm dialog, throwaway jar removed from device, row gone, 4 real mods intact
  search "sodium": 4 -> 3 rows, filter state survives window close/reopen
  download button opens the shared download window on the Mods category
  import button opens the Android document picker
  minimize/restore via chip, focus/z-order, UI scale 150% then restored to 100%
  crash buffer empty, no FATAL/ANR

NOT CARRIED OVER (deliberate, reported)
  per-mod update check (ModsUpdater presentation) and row "more info -> asset page"
  (needs RemoteMod remote lookup). Classic UI keeps both, untouched.

ARTIFACT
  ALLLauncher-Debug-0.0.2-beta1-x86_64.apk
  SHA-256 6C317879942AC918EAB62C8BFFBAF00F01CA0EB59773615F8FD4623F05A064E2
RELEASE PACKAGE: untouched (3BD3E79D...15BAB9, mtime 2026-09-20 14:39:18)
QA CLEANUP: 26.3 test instance (copied from the Release package) removed; 1.21.11 mods dir back to fabric-api only; /sdcard dumps removed

NEXT: PHASE 2.2 — Files window, same pattern (AllFilesWindow -> ALL UI + Zalith file-manager backend)

## PHASE 2.2 — ALL-owned Files window

STATUS: PASS (Debug UI/device pass; copy/move runtime action not exercised)

BEFORE
  ALL Files window -> AllBridge.AllFileManager -> Zalith FileManagerRootScreen/FileManager UI

AFTER
  ALL Files window -> AllFilesWindow (ALL presentation)
                  -> AllFilesViewModel -> AllFilesService -> existing Zalith FileManagerViewModel/controllers
  Path scope remains the selected Version game directory; each instance has a distinct Hilt VM key and ALL VM key.

ALL-OWNED FILES ADDED
  all/files/AllFilesWindow.kt     505 lines
  all/files/AllFilesViewModel.kt   59 lines
  all/files/AllFilesService.kt     37 lines
  all/files/AllFilesState.kt       15 lines

ALL-OWNED FILES CHANGED
  all/bridge/AllBridge.kt: removed AllFileManager composable and Zalith FileManager UI/Hilt imports
  ui/screens/content/AllDesktopScreen.kt: Files window shell now calls AllFilesWindow

ZALITH FILES CHANGED: 0
REMAINING FILE MANAGER UI DEPENDENCY FROM ALL: 0 (no FileManagerRootScreen/FileManagerActivity imports in ALL files/bridge/desktop screen)
BACKEND DEPENDENCIES RETAINED
  FileManagerViewModel/UiState/FmEntry; AccessScope and existing navigation, operations, trash, conflict resolution,
  filename validation, import, and errorEvents remain in Zalith backend. No filesystem operations reimplemented.

VALIDATION
  :ZalithLauncher:compileDebugKotlin PASS
  :ZalithLauncher:assembleDebug -Darch=x86_64 PASS; after initial incremental APK omitted AllFilesWindowKt despite Kotlin/Dex intermediate definitions,
  a forced rerun of Gradle tasks produced an APK with AllFilesWindowKt verified by apkanalyzer. One later incremental assemble after the error-stream fix also packaged the class correctly.
  Final APK installed base.apk SHA-256 matched exactly.
  LDPlayer 127.0.0.1:5555: 1.21.11 root/breadcrumb/path correct; entered .fabric and config; Back/up returned to parent; close/reopen restored 1.21.11/config independently.
  A temporary copy of existing Release 26.3 was made in Debug: root path showed Debug/26.3; mods subfolder listed 4 files; its navigation stayed separate from 1.21.11.
  Create QA folder -> rename -> move to Zalith trash succeeded; trash file verified, then QA cache item removed.
  Android import picker opened and cancelled safely.
  UI scale 125% visibly applied, restored to 100%; Files window minimized/restored with 26.3/mods path retained.
  Crash buffer empty after final artifact; no final-install NoClassDefFoundError/FATAL/ANR.
  Copy/move/paste and conflict dialog compile through existing backend but were NOT exercised on device.
  Error events are forwarded into ALL state and shown; duplicate-name runtime feedback was not conclusively verified (screen focus changed during probe).
  Path traversal probe was NOT run: shell policy rejected the command before it executed; no device action occurred.

FINAL DEBUG ARTIFACT
  D:\ALL001\ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.2-beta1-x86_64.apk
  SHA-256 E051094A25351ADED5EC26E2B02BF0E8D7868C143B5A19EE5FD1FF018A8A9266
  232,688,597 bytes; 2026-09-29 00:26:41 +07:00

RESTORE / SAFETY
  Debug versions restored to only 1.21.11 Fabric 0.19.5; mods dir remains fabric-api-0.141.6+1.21.11.jar only.
  Temporary Debug 26.3 copy removed. The QA folder was moved to app-private trash then only its known QA trash entry was removed.
  Release 0.0.1 APK untouched; SHA-256 remains 3BD3E79D323F9A24BCA775FF9849E3E7AFF779FDB70651CC5CA321E1D715BAB9.
  No source files under Zalith filemanager changed.

NEXT: PHASE 2.3 — Settings presentation (leave Download, Instance, Mods, Classic UI unchanged).

## PHASE 2.3 — ALL-owned Settings window

STATUS: PASS (Debug / LDPlayer; Classic per-instance VersionConfig page directly checked)

BEFORE
  ALL Settings floating window -> AllBridge -> Zalith VersionConfigs / GameConfigs / SupportConfigs

AFTER
  ALL SettingsWindow (ALL presentation) -> AllSettingsViewModel -> AllSettingsService
    -> existing VersionConfig (per-instance file + saveWithThrowable)
    -> existing AllSettings registry (global fallback only; no global writes)
    -> existing renderer, driver, runtime, control, vibration and microphone backend utilities

ALL-OWNED FILES ADDED
  all/settings/AllSettingsWindow.kt      666 lines
  all/settings/AllSettingsViewModel.kt    87 lines
  all/settings/AllSettingsService.kt     108 lines
  all/settings/AllSettingsState.kt        25 lines

ALL-OWNED FILES CHANGED
  all/bridge/AllBridge.kt: removed AllVersionConfigs / AllGameConfigs / AllSupportConfigs and Zalith Settings UI imports
  ui/screens/content/AllDesktopScreen.kt: Settings window content delegates to AllSettingsWindow

ZALITH SOURCE FILES CHANGED: 0
REMAINING ZALITH SETTINGS UI DEPENDENCIES FROM ALL: 0
CLASSIC UI SOURCE: untouched

SETTINGS IMPLEMENTED
  Version: isolation, skip integrity, renderer, Vulkan driver, graphics API, control layout
  Game: Java runtime, memory allocation/follow-global, custom version text, JVM args, auto-join server IP
  Support: vibration pattern/duration and preview, Vulkan callback, microphone check with existing MicMeter + permission callback
  Global fallback values remain in AllSettings; only VersionConfig is written by per-instance controls.

VALIDATION
  :ZalithLauncher:compileDebugKotlin PASS; :ZalithLauncher:assembleDebug -Darch=x86_64 PASS
  apkanalyzer confirmed AllSettingsWindowKt is defined in dex; installed base.apk hash exactly matches build artifact.
  LDPlayer: ALL Settings UI opened on 1.21.11 and 26.3; title/category routing matched selected instance. Version, Game and Support sections render; runtime/renderer and advanced rows populate from live existing manager data.
  RAM display note: ALL now reads the canonical Version.getRamAllocation(context) value (Follow Global = 1144 MB in this environment). Classic's legacy slider visually clamps its displayed control to 1024 MB (getMaxMemoryForSettings); stored/global RAM was never changed. This is a legacy UI display difference; ALL retains the existing runtime getter semantics.
  Per-instance isolation dropdown changed 1.21.11 ENABLE -> DISABLE; 1.21.11 config changed while 26.3 config hash stayed unchanged. Restored ENABLE; both config SHA-256 sets returned exactly to baseline.
  Java runtime selector changed 1.21.11 Follow Global -> Internal-21; value survived close/reopen; restored Follow Global. Both configs returned to baseline hash a7256680039ac76a8d9ac4b15d767fe749800e1e9093dfe497f329ac93d75539.
  Classic global Settings screen and Classic per-instance VersionConfig Settings tab both opened; Classic showed 1.21.11 isolation ENABLE, Java Runtime Follow Global, and the effective RAM value 1024 MB. Same VersionConfig/global registry values are used; Classic source unchanged.
  UI-scale compatibility: Settings tested at 150%, all three sections stayed usable; scale restored to the prior 100% baseline. Vulkan check and microphone recording buttons were visible but not invoked (mic permission/recording not granted for this run).
  Crash buffer empty; no app FATAL/ANR. git diff --check had no whitespace errors (only existing LF/CRLF warnings in unrelated dirty files).

FINAL DEBUG ARTIFACT
  D:\ALL001\ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.2-beta1-x86_64.apk
  SHA-256 2B4174D75A797CCE770618D315D6688E2AF0628BCAC17901129FCB95F1ECE083
  232,688,597 bytes; 2026-09-29 02:12:29 +07:00

RESTORE / SAFETY
  Debug instance list restored to 1.21.11 Fabric 0.19.5 only; QA 26.3 copy removed.
  Both original per-instance config hashes restored to baseline; no settings QA values left.
  Release APK/package untouched. Protected release APK SHA-256 remains 3BD3E79D323F9A24BCA775FF9849E3E7AFF779FDB70651CC5CA321E1D715BAB9.
  Launcher mode returned to ALL Desktop; Debug app remains installed via update semantics; no clear/uninstall.

NEXT: PHASE 2.4 — Download presentation.

## PHASE 2.4 — ALL-owned Download window

STATUS: PARTIAL — ALL UI/build installed; live ALL search did not finish, so no successful real install is claimed.

BEFORE
  ALL Download window -> AllBridge -> Zalith DownloadContent / SearchAssetsScreen / DownloadAssetsScreen UI

AFTER
  ALL DownloadWindow -> AllDownloadViewModel / AllDownloadState -> AllDownloadService
    -> existing provider search helpers, asset task installers/dependency resolver, GameInstaller,
       ModPackInstaller, MinecraftVersions and TaskSystem.

ALL-OWNED FILES ADDED
  all/download/AllDownloadWindow.kt
  all/download/AllDownloadViewModel.kt
  all/download/AllDownloadService.kt
  all/download/AllDownloadState.kt

ALL-OWNED FILES CHANGED
  all/bridge/AllBridge.kt: removed AllDownloadContent/Zalith DownloadContent wrapper
  ui/screens/content/AllDesktopScreen.kt: preserved floating XP shell; routes body to AllDownloadWindow; Mods shortcut opens Mod category

ZALITH SOURCE FILES CHANGED: 0
REMAINING ZALITH DOWNLOAD SCREENS/COMPOSABLES REFERENCED FROM ALL: 0
BACKENDS INTENTIONALLY RETAINED
  searchAssets/getProjectByVersion/getVersions, Modrinth and CurseForge search clients, required dependency resolver,
  downloadSingleForVersions/TaskSystem, MinecraftVersions/GameInstaller, ModPackInstaller and save unpacker.
  No new provider client, TaskSystem, installer or URL/path builder was created.

CATEGORIES/BEHAVIOR IMPLEMENTED IN ALL UI
  Games (official vanilla list + GameInstaller), Modpacks (project/file + ModPackInstaller/new instance name),
  Mods, Resource Packs, Shader Packs, Worlds, Search By ID; independent category query/result/detail state;
  provider selector backed by existing global per-category search settings; project details/version list;
  target instance chooser; required dependency expansion; Save ZIP unpack callback; task progress/cancel;
  error, Back, pagination, minimize/restore and close/reopen through existing shell/ViewModel.

VALIDATION
  :ZalithLauncher:compileDebugKotlin PASS.
  :ZalithLauncher:assembleDebug -Darch=x86_64 PASS (Gradle reported BUILD SUCCESSFUL; native make emitted the existing Windows `rm` process warning).
  apkanalyzer confirmed AllDownloadWindowKt in dex. Debug installed via adb install -r; installed base.apk hash matched artifact.
  LDPlayer: ALL Download window opened and displayed all seven categories. Search By ID/search/detail/install controls are present. Classic Download opened and listed existing Modrinth entries; Classic sources unchanged.
  ALL Mods search for Sodium submitted with Modrinth; device resolved api.modrinth.com, but the search remained in loading state past 60 seconds with no results or ALL error state. Host request returned HTTP 200; app stayed alive with no ANR/FATAL. This remains unresolved; project/version/install paths could not be exercised through the ALL UI.
  No real asset install was performed. Cancel/error controls are wired to TaskSystem and installer cancellation, but not runtime-verified.
  Known parity gaps: no Game loader-addon picker (GameInstaller currently installs vanilla versions only); local modpack archive import is not represented; task actions were not device-tested; provider search spinner must be resolved before broader QA.

FINAL DEBUG ARTIFACT
  D:\ALL001\ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.2-beta1-x86_64.apk
  SHA-256 C28BAE02AE65B167B29CE439F61E3D797E5C031E598B5055EBB0A320999A41CD
  233,238,543 bytes; 2026-09-29 21:16:47 +07:00

RESTORE / SAFETY
  Debug remains on its original 1.21.11 Fabric instance; mods baseline unchanged; no QA pack/mod installed.
  Release package/data untouched. ALL Desktop mode restored. QA screen dumps and temporary mods backup removed.
  No reset/clean/stash/commit performed.

NEXT: PHASE 2.4 is not complete. Resolve why ALL's shared provider search stays loading and prove one reversible real install; only then begin “PHASE 2 COMPLETE — review remaining ALL → Zalith UI dependencies.”

## PHASE 2.4 — Search / ID / Install / Icons (2026-10-03, authoritative for ALL Download)
STATUS: Search PASS · Search By ID PASS · project detail PASS · version list/filter PASS · install target dialog PASS ·
Real Install (Modrinth, with dependency) PASS-with-gaps (Cancel + install controlled-error NOT exercised) · Cover Images PASS (broken-image fallback code-level only).

SEARCH
- Original ">60s loading" could not be reproduced on the current build (the installed APK was a stale 2026-09-29 build). No search code changed.
- Normal search PASS: Modrinth sodium/lithium/fabric api; CurseForge lithium; Modpack/RP/Shader/Map queries.
- Error path PASS via temporary debug-only hook in AllDownloadService.search (BuildConfig.DEBUG && flag): explicit red error shown, no spinner, stable at 30 s.
  Hook REMOVED (file restored byte-identical, sha256 verified; 0 "TEMP-HOOK" left in source).
- Test-method notes: app ignores system http_proxy and Private DNS; disabling wifi makes LDPlayer ADB go offline (recover: `ldconsole reboot --index 0`, C:\LDPlayer\LDPlayer9). Do not toggle wifi again.

BUGS FIXED THIS ROUND (all ALL-owned)
1. CategoryRail not scrollable -> "ค้นหาด้วย ID" clipped on 540px screen: added verticalScroll (AllDownloadWindow.kt).
2. Crash on tapping a version: UninitializedPropertyAccessException thisPrimaryFile. Root cause: ALL never called initFile(projectId). Fix: loadVersions now uses
   Classic's `initAll(item.projectId)` (ui/.../download/assets/elements/DownloadAssetsElements.kt) and throws "No downloadable file found" if none survive (AllDownloadViewModel.kt).
3. Icons: ProjectCard + ProjectDetails header now use Zalith's existing Coil `AssetsIcon` (AssetsCommonElements.kt: cache, shimmer, ic_unknown_icon on error); letter placeholder only when provider gives no URL.

REAL INSTALL (Debug app, instance "1.21.11 Fabric 0.19.5")
- Dir: /storage/emulated/0/Android/data/com.all.launcher.app.debug/files/.minecraft/versions/1.21.11 Fabric 0.19.5/mods
- Baseline: only fabric-api-0.141.6+1.21.11.jar sha256 bdff7fd7e220085cfad2ff9b1f40dde6534ae0b96cf378f97a374bc54cb9ed0f
- Installed via ALL UI: Sodium Extra 0.9.4 (Modrinth) -> task shown in task menu ("downloading and caching required project AANobbMI") -> finished.
  Files: sodium-extra-fabric-0.9.4+mc1.21.11.jar + required dep sodium-fabric-0.8.15-beta.1+mc1.21.11.jar (sha1 both equal Modrinth API values). fabric-api not duplicated.
- Re-install same version: no duplicates, same hashes (sodium-extra overwritten in place).
- Cancel NOT tested: install completes <1 s (cached) so cancel could not be hit. Controlled install error NOT tested.
- Baseline RESTORED: both added jars deleted; dir hash list identical to baseline.
- Note: dependency resolver picked a beta Sodium (0.8.15-beta.1) because it is newest compatible; Zalith resolver behaviour, unchanged.

COVER IMAGES runtime: Mods (Modrinth), Resource Packs, Shader Packs, Maps/Worlds, Modpacks (CurseForge) all show real icons; detail header icon OK; 4x fast scroll over a 193-page list OK; 0 FATAL, 0 "Coil: error".
Broken-image fallback and no-URL letter fallback verified by code only.

FILES CHANGED THIS ROUND
- all/download/AllDownloadWindow.kt (rail scroll, AssetsIcon x2, import)
- all/download/AllDownloadViewModel.kt (initAll + empty guard + imports)
(AllDownloadService.kt unchanged vs pre-hook). Zalith/Classic files touched: 0. Nothing committed/reset/stashed.

BUILD / APK
- :ZalithLauncher:assembleDebug -Darch=x86_64 PASS. APK D:\ALL Launcher 0.0.1\ZalithLauncher\build\outputs\apk\debug\ALLLauncher-Debug-0.0.2-beta1-x86_64.apk
  (sha256/size appended below); installed with adb install -r.
- Build gotcha: gradlew.bat maps a temporary W: drive; two concurrent builds fail with "Cannot create temporary W:". Wait for the first to finish.

KNOWN UX GAPS (not fixed)
- Empty-result and never-searched states show the same hint text. Stale results remain under spinner while loading.

NEXT
1. Test Cancel on a large asset (e.g. big resource/shader pack) and a controlled install error (reversible, restore after).
2. Modpack install + Game install + Save ZIP unpack through ALL UI (not yet runtime-tested).
3. Then "PHASE 2 COMPLETE — review remaining ALL -> Zalith UI dependencies."
APK sha256 58bf4c57626612e5b53d12844be39c769ef1ef443ca50cd9567e9e17b3772153, 233242590 bytes

## PHASE 2.4 — Real Install CLOSED + UX fixes (2026-10-03, later; supersedes the "PARTIAL" gaps above)
STATUS: Real Install = PASS (install, dependency, duplicate, Cancel, retry-after-Cancel, Install Error, final-APK regression). Search/ID/detail/version/target dialog = PASS. Cover Images = PASS.
BLOCKER: none. NOT yet tested through ALL UI: Modpack install, Game install, Save-ZIP unpack.

CANCEL (evidence)
- No real asset was slow enough (18.9 MB Icon Xaero's finishes in ~1 s on LDPlayer; no root/throttle available), so a temporary DEBUG-only hook was used in
  game/download/assets/_Download.Single.Tasks.kt (inside the TaskSystem task body; mode read from device marker file files/ALL_DEBUG_MODE: "slow" = write partial cache file then delay, "error" = write partial + throw).
- slow mode: task visible as "Background tasks · 1" + taskbar "1 งาน" + in-window ยกเลิก button; partial present at app cache/assets/a14777ad…(sha1 name) while running.
  After pressing ยกเลิก: panel + taskbar cleared, cache/assets EMPTY, target resourcepacks/ not created (onCancel + onFinally cleanup works), log "Attempting to clear cached resource files".
- Retry after Cancel (hook off): Icon Xaero's 1.22.zip installed, sha1 a14777ad5dac4fd37c4429c29165c87f6270102e == Modrinth API. Then deleted (resourcepacks/ did not exist at baseline).
INSTALL ERROR (evidence)
- error mode: explicit dialog "การติดตั้งข้อมูล ผิดพลาด / DEBUG injected install failure" (Zalith onError -> submitError -> ALL error dialog), task gone from taskbar, no spinner,
  cache/assets empty, no target file. Dialog dismissed with ตกลง.
HOOK REMOVED: file restored from backup; sha256 816f391a…ecfac == pre-hook; 0 "TEMP-HOOK"/ALL_DEBUG_MODE in ZalithLauncher/src; marker file deleted from device; git diff --stat unchanged (43 files).
FINAL-APK REGRESSION: Sodium Extra 0.9.4 via ALL UI -> sodium-extra + sodium dep, sha1 5ee45353…d27a / 5469cef6…5f33 match Modrinth; fabric-api not duplicated; then removed. mods/ == baseline (only fabric-api sha256 bdff7fd7…ed0f).

UX FIXES (done after Real Install PASS)
- AllDownloadCategoryState.searched: empty list shows "ไม่พบผลลัพธ์" after a completed search, original hint before any search / after the query is edited.
- New search (page 0) clears old projects at start, so spinner no longer sits above stale results. Verified: zzqqxxnotfound -> "ไม่พบผลลัพธ์"; "fabric" -> spinner only, then results.
- Minor leftover: page counter ("1 / 6") from the previous search still shows during loading.

FILES CHANGED THIS ROUND
- all/download/AllDownloadState.kt (searched), AllDownloadViewModel.kt (searched/clear on search), AllDownloadWindow.kt (empty-state text).
- _Download.Single.Tasks.kt: temporary hook added and fully removed (net 0 vs before this round).
BUILD / APK: :ZalithLauncher:assembleDebug -Darch=x86_64 PASS; installed with adb install -r.
  ZalithLauncher/build/outputs/apk/debug/ALLLauncher-Debug-0.0.2-beta1-x86_64.apk  sha256 1e3d721ecdb76898cd9d61f0a5b499f4e7e46a729d679a54f5b8ba86da6c1649  234006940 bytes
RESTORED: Resource Pack provider set back to CurseForge (was switched to Modrinth for the test); Mods provider Modrinth; no QA files left in instance; ADB marker removed.
ENV NOTES: wifi toggling kills LDPlayer ADB (use ldconsole reboot --index 0); adb occasionally drops - run "adb kill-server; adb start-server". Never run 2 gradle builds at once (W: mapping).
NEXT: (1) Modpack install via ALL UI (new instance, reversible, delete after). (2) Game install (vanilla) + cancel. (3) Save/world ZIP unpack. (4) then "PHASE 2 COMPLETE — review remaining ALL -> Zalith UI dependencies."

## PHASE 2.4 — Modpack / Game / Save-ZIP through ALL UI (2026-10-03, night)
STATUS: Modpack = PASS · Game = PASS · Save-ZIP = PASS (after one data-safety patch). PHASE 2.4 is therefore proven for all seven categories at the install level
(Mods/RP/Shader via earlier rounds; Modpack/Game/World this round). Not exercised: Cancel for Modpack/Game/World, controlled install-error for those three.
BLOCKER: none (see EXTERNAL note: Modrinth /version?limit&offset endpoint was intermittently 18-30 s+ from this network; app showed explicit "Socket timeout" errors; recovered by itself).

MODPACK (Modrinth "MAX FPS: Optimized" 5.49, mrpack c03f48b1…, installer = ModPackInstaller, "Install as a new instance")
- Baseline: versions = [1.21.11 Fabric 0.19.5]; main mods = fabric-api only (sha256 bdff7fd7…ed0f); saves = โลกใหม่ (15 files); libraries 530 files; assets 11299.
- Install name QA-MODPACK-1 -> new instance versions/QA-MODPACK-1 (jar 31,152,600 B + merged Fabric json), spinner + ยกเลิก while running, ~40 s.
- 12/12 mod jar sha1 == modrinth.index.json; overrides present (config/moreculling.toml, sodium-fingerprint.json, sodium-options.json, options.txt). Main instance untouched.
- Reinstall same name -> explicit error, existing instance files unchanged (hash list identical). Leftover cache/temp_modpack after the failed attempt (Zalith behaviour; cleaned manually).
- Second pack SkyBlock Plus 1.0.7 -> QA-MODPACK-2 installed; duplicate name now shows "An instance with this name already exists." (was raw class name GameAlreadyInstalledException).
GAME (category semantics confirmed in UI: "Install official Minecraft version to the launcher game directory" = VANILLA only, new instance named by user; no loader picker)
- 1.21.11 as QA-GAME-1 -> versions/QA-GAME-1/QA-GAME-1.jar 31,152,600 B sha1 ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd == Mojang manifest; json 42,184 B.
- Same name again: UI shows "This version name is already installed." and disables the button (no overwrite).
- Gap: no explicit success message after install (progressMessage "Installed X" is not rendered).
SAVE/WORLD ZIP (CurseForge "GamerPotion's FPS Tester", FPS Test.zip 1,337,587 B sha1 2c4331bd…b8465d2, 43 files, root folder "FPS Test/")
- Code review (utils/file/FileUtils.kt extractZipEntries, game/version/saves/_Install.Saves.kt): rejects entries containing ../ or ..\ and any resolved path outside the output dir (SecurityException); level.dat located by findLevelEntryName; destination = <instance>/saves/<zip name without .zip>.
- Runtime: saves/FPS Test with 43 files, per-file sha1 identical to zip contents; original world โลกใหม่ unchanged (15 files); zip + cache/assets removed after unpack; logs "Found the level … Decompression is complete".
- FINDING (data safety): reinstalling over an existing same-name world MERGED silently (tampered data/raids.dat was overwritten, user-added file kept; no warning). Zalith core behaviour.
  FIX (ALL layer only): AllDownloadService.installAsset, WORLD category: if saves/<zipName> exists in any target -> IOException "A world named "X" already exists in <instance>. Rename or delete it first." BEFORE downloading.
  Verified: tampered raids.dat (sha1 43cf2a5d…) and USER_NOTE.txt unchanged after the blocked reinstall; after deleting the world a fresh install works and hashes match again.
- Not tested: zips with traversal entries (no such zip available; code-reviewed only), zips without level.dat.
OTHER FIX: search error text was shown as raw "Error(message=StringRes(key=…, args=[403]))" (CurseForge HTTP 403 seen once). Now AllDownloadService.search resolves it with toAndroidString(context) (AllDownloadService now takes Context). Compile-verified; the 403 could not be re-triggered to see the resolved text.

FILES CHANGED THIS ROUND (all ALL-owned)
- all/download/AllDownloadService.kt (world-exists guard; Context ctor param; resolved search error text)
- all/download/AllDownloadViewModel.kt (service ctor arg; friendly duplicate-instance message for modpack)
Zalith/Classic files touched: 0. No debug hooks in tree (grep TEMP-HOOK = 0). No commit/reset/stash.
BUILD / APK: :ZalithLauncher:assembleDebug -Darch=x86_64 PASS (one failed build mid-round: private exception class, fixed); installed adb install -r.
  ZalithLauncher/build/outputs/apk/debug/ALLLauncher-Debug-0.0.2-beta1-x86_64.apk  sha256 da59aa8e4523dfadd2814137f38229a95aa385a8c6e2a834a14cdd98da60c0ab  234006940 bytes
BASELINE RESTORATION (verified): versions = [1.21.11 Fabric 0.19.5]; saves = โลกใหม่ (15 files); main mods = fabric-api only (bdff7fd7…); cache/assets and temp_modpack empty; no resourcepacks dir; Modpack provider back to CurseForge.
  NOT restored (shared caches, harmless): .minecraft/assets 11299 -> 11441 files (vanilla asset objects fetched by the modpack/game installs); libraries still 530.
KNOWN GAPS (unfixed): no success message after Game install; installed-target list is cached (deleting an instance on disk by hand leaves a stale entry until refresh - my cleanup artefact); detail card says "N installed target(s)" for Modpack; browse screen keeps a stale error after Back.
NEXT: (1) optional: Cancel + error tests for Modpack/Game/World; world-zip with traversal entry (craft locally and push). (2) success feedback after Game install. (3) "PHASE 2 COMPLETE — review remaining ALL -> Zalith UI dependencies."

## PHASE 2.4 — FINAL VERIFICATION (2026-10-04 00:40) — PHASE 2.4 = PASS
STATUS: all seven ALL Download categories + Search / Search By ID / project / versions / target dialog / cover images verified on the FINAL x86_64 debug APK. No blocker.
APK: ZalithLauncher/build/outputs/apk/debug/ALLLauncher-Debug-0.0.2-beta1-x86_64.apk  sha256 96b26830d8b88955af7fc781af673b1f813d800e0c2a3471d4610d74e7f56704  233284178 bytes  (installed with adb install -r; no debug hook inside).

CANCEL / ERROR / RETRY for the three remaining types (temporary debug hook via device marker file ALL_DEBUG_MODE = slow|error|evil, REMOVED afterwards)
- Modpack (SkyBlock Plus 1.0.7, new instance QA-MODPACK-3): slow -> spinner + ยกเลิก, partial.tmp in cache/temp_modpack/fkVersion -> Cancel -> UI idle, NO versions/QA-MODPACK-3 -> retry OK (6 mods, temp cleaned). error -> dialog "DEBUG injected install failure", no instance, no spinner.
- Game (vanilla 1.21.11 -> QA-GAME-1): slow -> Cancel -> no instance dir; retry OK (jar sha1 ba2df812…60f5bd == Mojang); error (QA-GAME-2) -> explicit dialog, no instance.
- World (FPS Test.zip): slow -> task "Background tasks · 1" + taskbar "1 งาน" + ยกเลิก; Cancel -> saves = baseline, cache/assets empty; retry OK (43 files, hashes == zip); error -> explicit dialog, nothing written.
- Finding: after Cancel/Error Modpack+Game left partial files in cache/temp_modpack|temp_game (Zalith clears only at next install). FIX (ALL layer): AllDownloadViewModel.clearInstallerTemp() on cancelInstall and on modpack/game onError. Verified: game Cancel -> cache/temp_game gone; modpack Error -> cache/temp_modpack gone.
ZIP TRAVERSAL (crafted evil.zip: FPS Test/level.dat, region/r.0.0.mca, data/a.dat, "FPS Test/../ESCAPE.txt", more.txt; fed through the real download->copy->unpack path by the temp hook)
- Run 1 (before fix): rejected with explicit "Illegal path traversal detected: FPS Test/../ESCAPE.txt"; NO ESCAPE.txt anywhere; BUT partial world saves/FPS Test (level.dat, region, data) and saves/FPS Test.zip were left behind = FAIL on cleanup.
- FIX (ALL layer, AllDownloadService.installAsset): wrap unpackSaveZip; on any failure delete saves/<zipname> and the zip, rethrow (safe because the world-exists guard guarantees the folder is ours).
- Run 2 (after fix): explicit error, no ESCAPE.txt, saves = baseline (only โลกใหม่), no zip, cache/assets empty. PASS.
OTHER BUG FOUND BY FINAL REGRESSION AND FIXED: installing through Search By ID failed with "No asset folder is available" (assetClass is null for BY_ID).
  FIX: installAsset takes idClass (state.idSearchClass); dependency/world logic keys on the resolved class; BY_ID + MOD_PACK is refused with "Install modpacks from the Modpacks category." Verified: Sodium by ID AANobbMI installed, sha1 5469cef6… == Modrinth.
UX CLEANUP (ALL-owned state/UI)
- Game/modpack install success: "✓ Installed <name>" banner (cleared on category switch/Back) — verified for Game.
- Stale target list: VersionsManager.refresh when opening target chooser / modpack dialog / game version — verified (deleted QA-GAME-1 no longer listed).
- Page counter reset on new search and error cleared on Back: implemented + compiled; NOT visually verified (search returns <0.5 s, loading frame not catchable; no error state to test Back).
- Left as is: after a Game install the "This version name is already installed." validation text also shows (name now exists) next to the success banner.
FINAL REGRESSION on final APK: Mods search + cover images PASS; Search By ID -> project -> versions -> target dialog PASS; By-ID mod install PASS; Mods-category install Sodium Extra + required Sodium PASS (sha1 5ee45353… / 5469cef6… == Modrinth); Modpack PASS; Game PASS; World PASS (43 files, hashes match, single-target dialog); 0 FATAL EXCEPTION.
  Accident: one scripted tap landed on Resource Packs and installed a CurseForge pack zip (also a working RP install); removed, resourcepacks/ dir deleted.
DEBUG HOOK REMOVAL PROOF: hooks were in game/download/game/GameInstaller.kt, modpack/install/ModPackInstaller.kt, assets/_Download.Single.Tasks.kt. Restored from backup; sha256 of all three identical to pre-hook (bfe1f676…, 6c3d94f8…, 816f391a…); grep for TEMP-HOOK / ALL_DEBUG_MODE / ALL_DEBUG_EVIL / "injected install" in ZalithLauncher/src = 0; device marker + evil zip deleted; git diff --stat unchanged (43 files; all/ is untracked ALL-owned code).
FILES CHANGED THIS ROUND (ALL-owned only): all/download/AllDownloadService.kt, AllDownloadViewModel.kt, AllDownloadWindow.kt (success banner). Zalith/Classic files: net 0.
BASELINE RESTORATION (verified): versions = [1.21.11 Fabric 0.19.5]; saves = โลกใหม่ (15 files); mods = fabric-api only (bdff7fd7…ed0f); no resourcepacks dir; cache/assets, temp_game, temp_modpack empty; Modpack provider back to CurseForge; no files in device external files dir except app dirs.
  Not restored (shared caches, harmless): .minecraft/assets 11299 -> 11441, libraries 530 -> 531.
ENV: LDPlayer ADB flaky (adb kill-server/start-server); Git Bash mangles adb push paths (use MSYS_NO_PATHCONV=1); Modrinth /version endpoint had a slow spell (timeouts surfaced as explicit errors).
NEXT: "PHASE 2 COMPLETE — review remaining ALL -> Zalith UI dependencies." Optional: visually verify page-counter/Back-error fixes; hide the duplicate-name text right after a successful Game install.


## PHASE 2 COMPLETE — UI DEPENDENCY MAP (2026-10-04 00:55) — status: PASS (audit + 2 small decoupling fixes, runtime-verified)
Scope: where the ALL experience (InterfaceMode.ALL_DESKTOP) is still rendered by / navigates to Zalith-Classic UI. No Zalith backend was removed or rewritten.
Base dir for paths below: ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ (call it `Z/`).
Entry: Z/ui/screens/main/MainScreen.kt:559 (entry<NormalNavKey.LauncherMain>) hosts AllDesktopScreen (Z/ui/screens/content/AllDesktopScreen.kt) when AllSettings.launcherInterfaceMode == ALL_DESKTOP, else Classic LauncherScreen. The desktop hides only the Zalith TopBar on its home page (MainScreen.kt:203).

### A. ALL-OWNED UI (own composables + ViewModel/Service; no Zalith screen composables)
- Desktop shell, icons, instance window, start menu, taskbar, wallpaper + UI-scale dialogs, window frames: Z/ui/screens/content/AllDesktopScreen.kt (about 1440 lines; sits under ui/ but is ALL code).
- Download window (7 categories, search, By ID, project/version, target + modpack-name + game-name dialogs, progress/cancel/error, covers): Z/all/download/{AllDownloadWindow,AllDownloadViewModel,AllDownloadService,AllDownloadState}.kt.
- Mods window: Z/all/mods/*. Files window: Z/all/files/*.
- Instance settings window (isolation, renderer, Vulkan driver, graphics API, control layout, Java runtime, memory, custom info, JVM args, auto-join, vibration, Vulkan/microphone checks): Z/all/settings/*.
- Delete-instance dialog, task dialog and instance icon are called from ALL code but render Zalith composables through the bridge (see C).

### B. ZALITH/CLASSIC UI STILL OPENED DIRECTLY FROM THE ALL DESKTOP
Runtime-verified 2026-10-04 on LDPlayer: each opens a full Zalith page (Zalith TopBar over the classic artwork); one Back returns to the desktop (a second Back leaves the app).
1. Start menu > Accounts: AllDesktopScreen.kt:441 `mainScreen.navigateTo(NormalNavKey.AccountManager(FirstLoginMenu.NONE))` -> Z/ui/screens/content/AccountManageScreen.kt. Shows THE_BOUY2 (real Microsoft account) + ALLQA001 (offline test). Never touch the real one in tests.
2. Start menu > Settings: AllDesktopScreen.kt:175 `toSettings` -> backStackViewModel.settingsScreen (NestedNavKey.Settings) -> Z/ui/screens/content/SettingsScreen.kt. Launcher-wide pages: Renderer, Game, Control, Gamepad, Launcher (InterfaceMode switch at settings/LauncherSettingsScreen.kt:219), JavaManager, ControlManager, AboutInfo.
3. Start menu > About: AllDesktopScreen.kt:446 `settingsScreen.navigateTo(NormalNavKey.Settings.AboutInfo)` then toSettings().
4. Instance window > Resource Packs: AllDesktopScreen.kt:302-309 `NestedNavKey.VersionSettings(version){clearWith(NormalNavKey.Versions.ResourcePackManager)}` -> Zalith per-instance page. Its rail also holds Overview, Config, UpdateLoader, ModsManager, SavesManager, ShadersManager, ScreenshotsManager, ServerList, Export. So Worlds / Shaders / Screenshots / Servers / loader update / export are reachable ONLY through Classic or this rail.
5. Start menu > back to classic: AllDesktopScreen.kt:449 saves InterfaceMode.CLASSIC (intentional escape hatch).
6. Play: onPlay -> EventViewModel.Event.Launch.Game -> Z/ui/activities/MainActivity.kt:295 `launchGameViewModel.tryLaunch` (Z/viewmodel/LaunchGameViewModel.kt). Launch dialogs (Mod Doctor, Java/account/memory prompts, errors) are Zalith composables hosted by MainActivity; the in-game UI is VMActivity + Z/ui/screens/game/{GameScreen,JVMScreen,elements/*}. Proven earlier in this file: reaches the Minecraft main menu.
7. Global overlays shared by both modes (Zalith-owned): Task menu (MainScreen.kt ~259 TaskMenu; the ALL taskbar only toggles AllSettings.launcherTaskMenuExpanded), error/fatal screens (ErrorActivity, FatalErrorActivity, ErrorViewModel dialogs), modpack-import dialogs (MainActivity.kt:459-474), update dialog, Vulkan check, SplashActivity/UnpackDeps, ControlEditorActivity.
8. Classic-only pages with NO ALL entry point: VersionsManageScreen, VersionExportScreen, HomePageEditorScreen, MultiplayerScreen (Terracotta), LogViewScreen, FileSelectorScreen, WebViewScreen, LicenseScreen.

### C. SHARED BACKEND / SERVICES — KEEP AND REUSE (proven by Phase 2.x runtime tests)
- Providers/search/versions: Z/game/download/assets/platform/* (searchAssets, getVersions, getProjectByVersion, Modrinth/CurseForge searchers, MCIM mirrors), PlatformVersion.initFile (+ initAll in ui/.../download/assets/elements/DownloadAssetsElements.kt).
- Install engines: Z/game/download/assets/_Download.Single.Tasks.kt + _Download.Dependency.Tasks.kt, game/download/game/GameInstaller, game/download/modpack/install/ModPackInstaller, game/version/saves/_Install.Saves.kt (unpackSaveZip) with utils/file/FileUtils.kt extractZipEntries (traversal guard), coroutine/TaskSystem + TaskFlowExecutor.
- Instances/accounts/runtime: game/version/installed/{Version,VersionsManager,VersionConfig}, game/account/AccountsManager (+ microsoft/*), game/multirt/RuntimesManager, game/control/ControlManager, game/renderer/Renderers, game/plugin/driver, game/launch/GameLauncher, game/version/mod/* (ModDoctor, InstalledMod*), filemanager/* engine (FileManagerViewModel used by all/files), setting/AllSettings registry.
- Bridge: Z/all/bridge/AllBridge.kt is the single seam: typealiases AllBackStackViewModel / AllEventViewModel / AllModpackImportViewModel / AllToast / AllSubmitError; wrappers AllDeleteVersionDialog, AllTaskDialog, AllVersionIcon, and (new) AllAssetIcon, allInitFiles, rememberAllUriImportTaskBuilder.

### D. UNCLEAR / DEBT
- AllDesktopScreen parameters navigateToVersions, openFiles, eventViewModel, modpackImportViewModel are declared but UNUSED (dead plumbing from MainScreen.kt:560-575).
- Local modpack FILE import has no ALL button: it only works through Zalith MainActivity's incoming-file handler/dialogs.
- NormalNavKey/NestedNavKey remain the navigation contract for every Classic exit; ALL has no back stack of its own beyond windows.
- Zalith pages render over a busy artwork background when opened from the desktop (readability); cosmetic.
- Not exercised this round: Settings sub-pages beyond opening them, Java Manager, Control editor, a game launch re-run (earlier evidence exists in this file).

### FIXES DONE THIS ROUND (small, ALL-owned, behaviour-neutral)
1. Z/all/bridge/AllBridge.kt: added AllAssetIcon, allInitFiles, rememberAllUriImportTaskBuilder. AllDownloadWindow, AllDownloadViewModel and AllModsImport now import ONLY the bridge (they had imported Zalith UI helpers directly: AssetsIcon, initAll, rememberMultipleUriImportTaskBuilder).
2. Z/ui/screens/content/AllDesktopScreen.kt: openDownloadWindow now takes AllDownloadCategory? and no longer touches Zalith's shared downloadScreen / downloadXxxScreen nav stack; removed the TitledNavKey import. The Mods window "download" button opens category MOD directly.
Runtime on the rebuilt APK: Start > Download opens; Mods window > ดาวน์โหลด opens Download on the Mods category; search "sodium" shows covers; Sodium project -> version select shows the install card (initFile through the bridge, no crash); Mods window > นำเข้าข้อมูล opens the system picker; 0 FATAL. No install performed, baseline untouched.

FILES CHANGED: all/bridge/AllBridge.kt, all/download/AllDownloadViewModel.kt, all/download/AllDownloadWindow.kt, all/mods/AllModsImport.kt, ui/screens/content/AllDesktopScreen.kt. Zalith/Classic files: 0. No debug hooks (grep TEMP-HOOK = 0). No commit/reset/stash.
BUILD: :ZalithLauncher:assembleDebug -Darch=x86_64 PASS; installed with adb install -r.
OLD UX ITEMS (page counter during loading, old error after Back): still code-verified only (searches finish in <0.5 s, the loading frame cannot be caught); not blockers.
BLOCKER: none for Phase 2.

### RECOMMENDED PHASE 3 ORDER (small / low-risk first; one ALL window per step, same backend, verify on LDPlayer before the next)
- 3.0 Remove the dead AllDesktopScreen params + MainScreen.kt:560-575 plumbing — trivial.
- 3.1 ALL About window (static content of Settings.AboutInfo); replaces Start > About; no backend.
- 3.2 ALL Resource Packs window (reuse the Files/Mods window pattern + game/version resourcepack services); replaces the only instance-window exit to VersionSettings.
- 3.3 ALL Shaders, then Worlds (Saves; keep the world-exists guard and unpackSaveZip), then Screenshots, then Servers.
- 3.4 ALL Accounts window (AccountsManager + login flows). HIGH care: a real Microsoft account is present; use the offline test account only.
- 3.5 ALL launcher Settings window in slices: Launcher/Appearance (InterfaceMode; wallpaper and UI scale already ALL) -> Renderer/Game -> Java Manager (RuntimesManager) -> Controls Manager (ControlEditorActivity stays Zalith).
- 3.6 Instance management: create/rename/copy/export/update loader (VersionsManage, VersionExport, UpdateLoader).
- 3.7 Reskin global overlays (TaskMenu, launch/error dialogs): touches MainActivity, do last; keep the in-game UI (VMActivity/GameScreen/ControlLayout) Zalith.
- Do NOT delete Classic mode or Zalith pages until 3.1-3.6 pass; do not rewrite backends.
NEXT EXACT TASK: Phase 3.0 + 3.1 (dead-param cleanup, ALL About window), then 3.2 Resource Packs window.
APK: ZalithLauncher/build/outputs/apk/debug/ALLLauncher-Debug-0.0.2-beta1-x86_64.apk  sha256 8872e23a59d50c9f54eab871bc983587eed164d5b8c9f135f7da231aa4955529  233284178 bytes


## PHASE 3.0 / 3.1 / 3.2 (2026-10-04 09:20) — 3.0 PASS · 3.1 PASS · 3.2 PASS
Base dir `Z/` = ZalithLauncher/src/main/java/com/movtery/zalithlauncher/.

### 3.0 Cleanup — PASS
- Traced: navigateToVersions, openFiles, eventViewModel, modpackImportViewModel were declared in AllDesktopScreen and never read (grep: 0 uses).
- Removed from Z/ui/screens/content/AllDesktopScreen.kt (signature + imports), from the call site Z/ui/screens/main/MainScreen.kt (LauncherMain entry), and the now-unused typealiases AllEventViewModel / AllModpackImportViewModel from Z/all/bridge/AllBridge.kt.
- MainScreen keeps its own use of navigateToVersions for Classic LauncherScreen (unchanged).
- Build PASS; Desktop smoke: Start > Download opens, instance window > Mods opens, 0 FATAL.

### 3.1 ALL About window — PASS
- New Z/all/about/AllAboutWindow.kt, hosted as a floating Desktop window (shell: new generic FloatingWindow in AllDesktopScreen.kt, same XpWindowFrame/drag/minimize/focus as Mods). Start > About now calls openAboutWindow; it no longer navigates to NormalNavKey.Settings.AboutInfo / Zalith Settings.
- Content kept from Zalith's AboutInfoScreen (nothing dropped): launcher name + version (BuildKeys.LAUNCHER_NAME, BuildConfig.VERSION_NAME + versionCode/applicationId/buildType + "Based on ZalithLauncher2 (GPL-3.0)"), project link, MovTery + sponsor, 9 acknowledgements (bangbang93, FCL, HMCL, MCMod, mcmod-info-mirror, PCL2, PojavLauncher, GitHub/Weblate communities) with License + Link/Sponsor actions, full third-party library list (libraryData: copyright, license, link), loaded plugins (PluginLoader.allPlugins, shown only if any).
- License text is shown in an in-window dialog (context.readRawContent on the same R.raw files; GPL text verified). Links use Intent.ACTION_VIEW (LDPlayer has no browser: no crash; toast fallback on failure).
- Runtime: opens as window with taskbar chip; scroll through credits and 40+ libraries to the end (XZ for Java); License dialog opens and closes; Back minimizes (same as other windows), chip restores, X closes; 0 FATAL.
- Zalith dependency REMOVED: Start > About no longer opens Zalith Settings/AboutInfo.

### 3.2 ALL Resource Packs window — PASS
- Trace of Classic (Z/ui/screens/content/versions/ResourcePackManageScreen.kt, 922 lines): list = dir.listFiles -> game/version/resource_pack/parseResourcePack (pack.mcmeta/pack.png, validity) -> ResourcePackInfo; filter = "only valid" + name; delete = FileUtils.deleteQuietly after confirm; import = rememberMultipleUriImportTaskBuilder (zip check); Download = swapToDownload. Classic has NO enable/disable for packs (enabled list lives in the game's options.txt) and NO open-folder button, so none was added.
- New Z/all/resourcepacks/AllResourcePacksWindow.kt (+ AllResourcePacksViewModel, state kept across minimize; reload only when the file count changed). Reuses parseResourcePack/ResourcePackInfo, Zalith import task via bridge (rememberAllUriImportTaskBuilder), FileUtils delete, Zalith string resources. No new parser.
- AllDesktopScreen: onResourcePacks = openPacksWindow(version) (was NestedNavKey.VersionSettings(...ResourcePackManager) -> Zalith page); new WINDOW_PACKS layer + taskbar chip + Back handling + delete-instance cleanup; Download button opens ALL Download on RESOURCE_PACK (verified).
- Runtime on 1.21.11 Fabric 0.19.5 (baseline: no resourcepacks/ dir; mods = fabric-api only sha256 bdff7fd7…ed0f):
  - window opens with empty state "no resource packs"; no Zalith VersionSettings page appears;
  - Import via system picker (2 test zips pushed to /sdcard/Download): both copied to resourcepacks/; list shows QA-pack-valid (description "ALL QA valid pack", ZIP, format 46, 347 bytes) and QA-pack-invalid (red "invalid format");
  - "Available" filter shows only the valid one (1/2); search "valid" matches, "validzzz" shows "no matching items" (0/2);
  - Refresh after an out-of-band file copy (QA-extra.zip) picks it up (3/3); delete dialog: Cancel keeps the file, Confirm deletes it and list updates (2/2);
  - Download button opens ALL Download on Resource Packs (CurseForge provider);
  - taskbar chip minimize/restore/focus works; Mods, Files and Settings windows still open and render normally; 0 FATAL EXCEPTION.
- Not done (no backend): enable/disable pack, open-folder. Error state: delete failure shows red "Error: <file>" line; import errors use the shared ErrorViewModel dialog (not forced in this run).

### Zalith UI dependencies removed this round
1. Start > About -> Zalith Settings/AboutInfo (now ALL window).
2. Instance window > Resource Packs -> Zalith NestedNavKey.VersionSettings/ResourcePackManager page (now ALL window).
3. Dead plumbing (4 unused params + 2 typealiases).
Remaining Zalith exits from the Desktop: Accounts, Settings (launcher-wide), plus overlays (see PHASE 2 map). Instance-window rail pages (Shaders/Worlds/Screenshots/Servers/UpdateLoader/Export) are no longer reachable from the Desktop at all through Resource Packs (they were only reachable via that Zalith page) — use Classic mode until 3.3.

### Files changed (ALL-owned)
- NEW Z/all/about/AllAboutWindow.kt, NEW Z/all/resourcepacks/AllResourcePacksWindow.kt
- Z/ui/screens/content/AllDesktopScreen.kt, Z/all/bridge/AllBridge.kt, Z/ui/screens/main/MainScreen.kt (only the AllDesktopScreen call site).
Zalith/Classic pages touched: 0 (MainScreen change only removes arguments of the ALL call). No hooks (grep 0). No commit/reset/stash. Warehouse untouched.

### Build / APK
`:ZalithLauncher:assembleDebug -Darch=x86_64` PASS; installed with adb install -r.
APK ZalithLauncher/build/outputs/apk/debug/ALLLauncher-Debug-0.0.2-beta1-x86_64.apk  sha256 739647b0d7d5fd81c928a07347767f9db838d3f4b83cfba461ca2cba90f9200a  234,156,523 bytes.

### Baseline restoration (verified)
resourcepacks/ dir removed (did not exist before); mods = fabric-api only (bdff7fd7…ed0f); /sdcard/Download test zips removed; cache/assets, temp_game, temp_modpack empty; app force-stopped, no accounts or settings changed.

### Blocker: none
NEXT EXACT TASK: Phase 3.3a — ALL Shaders window (same pattern: Z/ui/screens/content/versions/ShadersManagerScreen.kt + versions/elements/ShadersScreenElements.kt trace first; reuse shader-pack parsing/delete/import; Download button -> RESOURCE... category SHADER; new instance-window button "เชเดอร์แพ็ก"), then 3.3b Worlds (keep unpackSaveZip + world-exists guard), 3.3c Screenshots, 3.3d Servers. Needs a new button in the instance window (InstanceWindow in AllDesktopScreen.kt).


## PHASE 3.3 a-d — Shaders / Worlds / Screenshots / Servers (2026-10-04 10:00) — 3.3a PASS · 3.3b PASS · 3.3c PASS · 3.3d PASS
Base dir `Z/` = ZalithLauncher/src/main/java/com/movtery/zalithlauncher/. All four are ALL-owned floating Desktop windows reusing Zalith backends; none opens VersionSettings.

### Shared plumbing (Z/ui/screens/content/AllDesktopScreen.kt)
- EXTRA_WINDOWS list + ExtraWindowContent(id) = add a window with one list row + one `when` branch. State via extras/extraMin/extraShown maps; layers, taskbar chips, Back-handling (minimize), delete-instance cleanup, front/topShown ordering all driven from the list.
- InstanceWindow got an `onExtra` callback and the new buttons laid out two per row (4 in one row clipped the Thai labels): Shaders, Screenshots, Servers, Worlds.
- Z/all/bridge/AllBridge.kt: rememberAllUriImportTaskBuilder now also forwards errorMessage + onFileCopied (needed for world unpack).

### 3.3a Shaders — PASS (Z/all/shaders/AllShadersWindow.kt)
- Trace: Classic ShadersManagerScreen lists only `*.zip` files in shaderpacks/ (ShaderPackInfo = file + size), rename/delete via file ops, import via rememberMultipleUriImportTaskBuilder(zip), Download button. Reused ShaderPackInfo + Zalith import task + FileUtils delete. (Classic rename not ported.)
- Runtime on 1.21.11 Fabric 0.19.5 (baseline: shaderpacks/ empty): empty state; import 2 zips via picker -> listed (2/2); out-of-band copy + Refresh -> 3/3; delete dialog Cancel keeps file, Confirm deletes (list updates); Download -> ALL Download, Shader Packs category. Baseline restored (shaderpacks empty).

### 3.3b Worlds — PASS (Z/all/worlds/AllWorldsWindow.kt)
- Reuses parseLevelDatFile (name/version/gamemode/last played/valid), unpackSaveZip (incl. FileUtils.extractZipEntries traversal guard), Zalith import task + messages, FileUtils delete.
- Guard moved into the window's onFileCopied: if saves/<zipname> exists -> delete the copied zip and fail with "A world named X already exists…" BEFORE unpacking; on any unpack failure delete saves/<zipname> + the zip (so no half world).
- Runtime (baseline: 1 world "โลกใหม่", 15 files): list shows existing world with parsed info; import FPS Test.zip -> saves/FPS Test, 43 files, per-file sha1 == zip, list 2/2 shows level name "GamerPotion's FPS Test", 1.18.1; re-import with tampered data/raids.dat + USER_NOTE.txt -> explicit error dialog, both untouched (sha1 43cf2a5d…), no zip left; delete Cancel keeps, Confirm removes; crafted evil.zip (entry "FPS Test/../ESCAPE.txt") -> explicit "Illegal path traversal detected", no ESCAPE.txt anywhere, no partial world, no zip; Download -> World category. Baseline restored (saves = only โลกใหม่, cache/assets empty).
- Not ported: rename, backup copy, quick-play (Classic-only extras).

### 3.3c Screenshots — PASS (Z/all/screenshots/AllScreenshotsWindow.kt)
- Same rule as Classic (png files in screenshots/), Coil AsyncImage straight from the file (no new image subsystem), FileUtils delete. Tap thumbnail = in-window enlarged preview with delete.
- Runtime (baseline: no screenshots dir; 3 generated PNGs pushed): grid with thumbnails; preview dialog (name + size); delete via preview -> confirm dialog -> Cancel keeps / Confirm deletes; out-of-band copy + Refresh shows it (3). Test dir removed afterwards.
- Not ported: export-to-gallery and multi-select (Classic's export code is private in its ViewModel; not duplicated). No open/share added (no safe existing backend).

### 3.3d Servers — PASS (Z/all/servers/AllServersWindow.kt)
- Trace: Classic ServerListScreen uses AllServers (servers.dat NBT read/write, hidden entries, servers.dat_old backup) and ServerData.load() (resolve + Minecraft status ping, favicon). Reused unchanged: AllServers.loadServers/addServer/removeServer/save, ServerData.load/edit fields. No parser/protocol written.
- Runtime (baseline: no servers.dat): empty state; Add disabled until an address is typed; add mc.hypixel.net -> live ping "22740/200000 · Requires MC 1.8/1.21 · 869 ms"; add no-such-host.invalid -> red "cannot connect"; servers.dat (+ servers.dat_old) written; edit name -> persisted (verified after reload from disk); delete dialog Cancel keeps both, Confirm removes both; file becomes empty list. Test files removed (no servers.dat at baseline).
- Not ported: copy-address and Classic's launch/quick-play entry.

### Regression
Mods, Files, Settings (per-instance), Resource Packs, Download, Shaders, Screenshots, Servers, Worlds windows each open and close on the final APK; 0 FATAL EXCEPTION. Settings window is wider (its X is at x≈833) — scripted taps meant for other windows once landed inside it and opened a dropdown; version.config mtime/content verified unchanged (skipGameIntegrityCheck still FOLLOW_GLOBAL).
Test accident: blind Back presses left the app once and opened the emulator's Google Play (nothing done there, stopped).

### Files changed
NEW Z/all/shaders/AllShadersWindow.kt, Z/all/worlds/AllWorldsWindow.kt, Z/all/screenshots/AllScreenshotsWindow.kt, Z/all/servers/AllServersWindow.kt.
EDITED Z/ui/screens/content/AllDesktopScreen.kt, Z/all/bridge/AllBridge.kt. Zalith/Classic pages touched: 0. No hooks (grep 0). No commit/reset/stash. Warehouse untouched.

### Build / APK
`:ZalithLauncher:assembleDebug -Darch=x86_64` PASS; installed adb install -r.
APK ZalithLauncher/build/outputs/apk/debug/ALLLauncher-Debug-0.0.2-beta1-x86_64.apk  sha256 2a32ff4608fb6aec4143f01f8774be15ea4ba049ab01698406c04fa264c3deea  234,901,353 bytes.

### Baseline restoration (verified)
saves = only โลกใหม่; no resourcepacks/, screenshots/, servers.dat*; shaderpacks empty; mods = fabric-api only (bdff7fd7…ed0f); /sdcard/Download only ldsdk; cache/assets, temp_game, temp_modpack empty; version.config untouched.

### Zalith UI dependency status after 3.3
Instance-window rail pages now all have ALL windows EXCEPT: UpdateLoader, ScreenshotsManager export, Overview/Config pages, Export (modpack export), per-instance rename/copy. Remaining Desktop exits to Zalith: Accounts, launcher Settings, shared overlays (see PHASE 2 map).

### Blocker: none
NEXT EXACT TASK: Phase 3.4 — ALL Accounts window (trace Z/ui/screens/content/AccountManageScreen.kt + AccountsManager + login flows; offline account create/select/delete first, Microsoft login dialog last; real Microsoft account THE_BOUY2 must not be touched/removed in tests — use offline test account only and restore). Then 3.5 launcher Settings slices, 3.6 instance management (UpdateLoader/Export/rename/copy), 3.7 overlays.

## PHASE 3.4 — ALL Accounts (2026-10-04 10:46) — PASS (offline create/select/delete runtime-verified; Start > Account no longer opens Zalith UI)
Base dir `Z/` = ZalithLauncher/src/main/java/com/movtery/zalithlauncher/.
- Trace: Classic AccountManageScreen.kt + AccountManageViewModel are only UI/intent plumbing. Backend reused unchanged: game/account/AccountsManager (Room DB `launcher_data.db`; accountsFlow, currentAccountFlow, setCurrentAccount, deleteAccount, suspendSaveAccount), game/account/AccountUtils.localLogin(name, null) (creates the Local account with the same default UUID as Classic; saveAccount also makes it current), Account model, isLocalAccount()/isMicrosoftAccount(), AllSettings.currentAccount (selection). setCurrentAccount only writes the current-account setting; it reads/refreshes no token (code-verified before use).
- NEW Z/all/accounts/AllAccountsWindow.kt: list (name, type Offline/Microsoft/auth server, active marker), Select, Create Offline (same name rule as Classic: letters/digits/_, max 16 here), Delete Offline with confirm dialog. Microsoft rows have Select only: NO delete, login, logout or refresh in this window (Microsoft login = later phase).
- Z/ui/screens/content/AllDesktopScreen.kt: Start > Account = openAccountsWindow (new WINDOW_ACCOUNTS layer + taskbar chip + minimize/Back handling); the NormalNavKey.AccountManager navigation is gone from the Desktop (grep 0). Strings added: all_accounts_username, all_accounts_select (EN + TH).
- Runtime (LDPlayer, debug app). Baseline DB (read via run-as copy, tokens never printed): ALLQA001 Local (active) + THE_BOUY2 Microsoft, Microsoft token-field digest 34e8e585e745e199.
  - Start > Account opens the ALL window (MainActivity stays resumed; no Zalith page). Lists both accounts with types, active marked.
  - Create ALL_QA_OFFLINE -> in DB as Local, became active (same as Classic).
  - App restart -> still listed and still active.
  - Select ALLQA001 -> active; restart -> instance window shows account ALLQA001; DB credentials unchanged.
  - Delete: Cancel keeps; Confirm removes ALL_QA_OFFLINE. Final DB == baseline exactly (rows + Microsoft digest 34e8e585…). Microsoft account never selected, deleted, refreshed or logged in.
  - Minimize -> taskbar chip -> restore -> close OK. Regression: Mods, Files, Resource Packs, Shaders, Worlds, Settings windows open/close; FATAL 0.
- INCIDENT + RECOVERY: a patch script wrote AllDesktopScreen.kt with an encoding error and truncated it to 0 bytes (file is untracked in git, so no git copy). Rebuilt from my pre-3.0 backup (which already held the 3.0 edits) by replaying the About/Packs/EXTRA_WINDOWS/worlds/shots/servers/layout patches, then Accounts. Compiled and re-verified at runtime (all windows above). A good copy is kept at C:\Users\pornp\AppData\Local\Temp\desk_good_34.kt. LESSON: AllDesktopScreen.kt and all/* are untracked — back them up before scripted edits (writer must encode before opening the file).
- Files changed: NEW all/accounts/AllAccountsWindow.kt; EDITED ui/screens/content/AllDesktopScreen.kt, res/values/strings.xml, res/values-th/strings.xml. Zalith/Classic pages touched: 0. No hooks. No commit/reset/stash. Warehouse untouched.
- Build/APK: :ZalithLauncher:assembleDebug -Darch=x86_64 PASS; installed adb install -r. APK ZalithLauncher/build/outputs/apk/debug/ALLLauncher-Debug-0.0.2-beta1-x86_64.apk
  sha256 ba07270dac8ce01d8b5016d3a8028f48a0021bb8f264c1ebf5add2394f771d1e  237884755 bytes
- Baseline restoration: accounts DB identical to baseline; saves = only โลกใหม่; mods = fabric-api only; caches empty.
- Not covered: skin/cape, wardrobe, auth-server (Yggdrasil) accounts, Microsoft login/refresh/remove, offline-mode unlock gate (7-tap) — all remain Zalith-only.
- Blocker: none.
NEXT EXACT TASK: Phase 3.5 launcher Settings in slices (start with Launcher/Appearance: InterfaceMode switch, theme, task-menu; then Renderer/Game, Java Manager via RuntimesManager, Controls Manager keeps ControlEditorActivity). Separate later phase: Microsoft login flow window (needs explicit go-ahead because it touches the real account's tokens).

## GIT CHECKPOINT (2026-10-04)
- Branch all-0.0.1: commit 22a7e7a7 "ALL checkpoint: Phase 2 complete and Phase 3.4 accounts" on top of upstream b4c9415b (2.5.3). NOT pushed. 120 files (77 added, 43 modified): all/** (25 files), AllDesktopScreen.kt, bridge, Mod Doctor/crash sources, tests, 4 wallpaper PNGs (11 MB), strings EN/TH, gradle/identity files, checkpoint + audit docs.
- Staged by explicit list (git add -u for already-tracked files + named untracked paths); secret scan of staged additions found only code/doc references, no credentials; `git diff --cached --check` clean (3 whitespace nits fixed, no logic touched); assembleDebug -Darch=x86_64 PASS before commit.
- Deliberately NOT tracked: legacy-archive/ (15 GB), test-artifacts/ (1.1 GB) — still untracked, not ignored (decide later: add to .gitignore or move out of the repo). Already ignored: build/, .gradle/, .kotlin/, local.properties, private-signing/ (now in .gitignore), keys. release-archive/ and work/ do not show in status.
- Pre-existing upstream tracked keystores ZalithLauncher/zalith_launcher.jks and zalith_launcher_debug.jks are part of upstream HEAD (not added by us); gradle.properties still holds upstream default_store_password/default_key_password values unchanged from upstream.
- Restore: `git checkout 22a7e7a7 -- <path>` for any ALL file. No git identity was configured on this machine; the commit used a one-off `-c user.name/user.email` (global/repo config untouched).
