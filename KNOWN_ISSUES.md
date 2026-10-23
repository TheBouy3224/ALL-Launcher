# ALL Launcher 0.0.2-beta2 - Known issues (final build, owner approval pending)

## beta2 final build (branding) - 2026-10-24

- This build supersedes the 2026-10-22 candidate. The only code/resource change is branding: ALL triangle app icon (adaptive + legacy), splash/game-entry logo, and the About page (THE_BOUY2 as project owner; MovTery credited as developer of upstream Zalith Launcher 2; GPLv3 license viewable in the app). Login, download, Minecraft, Controls Studio and other features are unchanged since the 2026-10-22 candidate.
- **Microsoft sign-in still uses the temporary shared client ID** (`OAUTH_CLIENT_ID` was not set for this build, same as the 2026-10-22 candidate). The sign-in screen says so. A public release needs ALL's own Microsoft Entra application ID (`ALL_PUBLIC_RELEASE=1` enforces this).
- **60 Hz limit on TECNO LI9 is unchanged** (game title screen showed FPS 60 in this build's QA).
- Device QA for this build used the isolated `com.all.launcher.app.releaseqa` companion (same release config and certificate, package suffix only) and offline QA accounts. Installing/updating the production package over Friends Beta with real data remains UNVERIFIED.
- The old mascot images (`all_launcher_cover*.png`) are still in the source tree but are no longer used by any screen.

## beta2 current validation - 2026-10-22

- **TECNO LI9 / Android 15: 60 Hz presentation limit remains.** Surface.setFrameRate is already requested at surface creation. The isolated QA package measured 59.216 presented FPS with the original game flags and 54.670 without both flags in the same Minecraft 1.20.1 QA world/view (about 27 seconds each; one sample per variant, not a performance benchmark). Both retained the 60 Hz display mode and the QA UID's gameDefaultOverride=60. The manifest flags remain unchanged because removing them did not demonstrate a reliable benefit. No system refresh/game settings were changed. Fresh-UID/reboot behavior and other phones are unverified.
- **Initial full unit run had a transient DNS failure:** 791 tests, 785 passed, 5 skipped, 1 failed. The failure was the existing live log-upload test `TestAbstractAPI.testMirroredAPI`: DNS resolution of `api.logshare.cn` failed, including fallback DoH. After DNS recovered, an isolated rerun passed 1/1 with no code/assertion changes. Both new Persistent Controls tests passed. All 786 non-skipped cases have therefore passed across the original run and retry; the complete suite was not rerun after recovery. Do not describe the initial run as 791 passing tests.
- **Select World:** the reported complete freeze was not reproduced in two bounded keyboard-driven QA runs; each loaded New World and saved back to the title screen. Touch-only navigation and extended play are not established by these runs. No speculative fix was applied.
- **Runtime scope:** QA uses isolated packages and the offline QA_GATE account. Production APK installation/update over Friends Beta, real Microsoft accounts, real worlds, iQOO and LDPlayer are excluded from this takeover's device testing.
- Prior handoff limitations still apply where not superseded: physical gamepad, camera response to physical gyro motion, other loaders/devices, long sessions/heat/battery and some provider paths remain unverified. The previous handoff reports real Microsoft login in QA, but this takeover does not repeat or claim that acceptance.
- TECNO HiOS may freeze the launcher while the Microsoft browser tab is open; the prior handoff observed login finishing after returning to ALL. Shared Microsoft client ID registration remains unchanged.

## Historical beta1 notes (retained for provenance)

The following notes describe beta1 and may be superseded by beta2's handoff/report (including Controls Studio, cursor packs and gamepad settings added since beta1).

This is a private beta. Please read before you use your real account.

## Accounts
- **Microsoft sign-in uses a temporary shared client ID.** ALL does not have its own Microsoft application registration yet. Sign-in goes through the same public client ID the official Minecraft launcher uses. The Accounts window says so. It can stop working without notice; if it fails, tell us the message (not the code).
- Microsoft sign-in was tested only up to the device-code step. **Please report whether a full login works for you.** Game Pass accounts and child accounts are untested.
- Tokens are encrypted at rest with a key from the Android Keystore. This is new in this build and has only been tested with test data. Do not rely on it for an account you cannot afford to lose; signing out/removing the account in ALL does not sign you out at Microsoft (do that at account.live.com).
- Offline accounts are hidden on purpose (tap the ALL logo 7 times in Accounts). Online servers reject offline accounts.

## Game
- Verified: Vanilla and Fabric 1.20.1 (Fabric 1.21.x earlier). **Forge, NeoForge, Quilt and OptiFine installers exist but are not tested enough** - expect failures.
- Long play sessions, heat and battery use are untested. Crashes after a long session may not be diagnosed.
- Only arm64 phones (Android 8+) are supported. Phones with 4 GB RAM or less may be killed by Android while playing.
- Mod Doctor (crash diagnosis and one-tap repair) knows a small set of cases (Fabric dependency/version problems). Anything else shows "unknown cause".
- If the game window closes by itself, ALL shows "game ended abnormally"; it cannot tell you why.

## Touch controls
- The control editor menus and dialogs are still Zalith's, re-coloured. Undo is available (one step = one finished edit); there is no Redo.
- Import/Export use the system file picker and share sheet.

## Settings
- Some Classic settings are not in the ALL settings yet: native-library plugins, renderer plugin config/download, mouse-pointer images, gamepad button mapping.

## Build / legal
- There is no automatic update. Install new builds by hand over the old one (same signing key keeps your data).
- ALL Launcher is GPLv3 and based on Zalith Launcher 2 (see NOTICE.md). The matching source is in the SOURCE.zip next to the APK.
