# Known issues - ALL Launcher 1.0.0 (versionCode 21)

Checked on 2 November 2026 on two phones: TECNO LI9 and vivo iQOO (I2405).

## Accounts and sign-in
- **Microsoft sign-in uses a temporary fallback client id** (no ALL-owned Entra app yet). It works today but Microsoft can block it at any time. See `PUBLIC-RELEASE-REQUIREMENTS.md`.
- **Nide8 accounts** need an internet connection the first time a game starts: `nide8auth.jar` is downloaded from Nide8 on first use. Not tested with a real Nide8 account.

## Helper jars downloaded on first use
`MioLibPatcher.jar`, `forge_installer.jar` and `nide8auth.jar` are not inside the APK (no licence that allows ALL to redistribute them). The launcher fetches them from their publishers into `files/components_dl/` the first time they are needed. Without internet on the first launch, the game still starts but without MioLibPatcher's mod fixes. To turn MioLibPatcher off for one instance, create an empty file `ALLLauncher/disable-miolibpatcher` in that instance's folder.

## Performance
- A **new Minecraft 26.3 world** ran at about 30 FPS on TECNO while terrain was still generating; saved worlds ran at 110-130 FPS. The cause (world generation vs. a frame-rate setting) was not isolated.
- Earlier builds showed **Vanilla 1.21.1 menus at 1-2 FPS on TECNO LI9** without a rendering mod (60 FPS with Sodium). Not re-tested on 1.0.0. Mobile players should install Sodium.
- TECNO LI9 is limited to 60 Hz.

## Controls
- In Minecraft menus, a tap does nothing when the pointer rests exactly on a button's edge (the button is not highlighted). Move the pointer inside the button first. Same behaviour with and without MioLibPatcher.
- Controls Studio: Send Text / Thai chat / F3+G were verified on Beta 3 and not re-run on 1.0.0 (source unchanged).

## Mods
- **Controlify 3.5.3 on Minecraft 26.3+**: mouse and keyboard stop working in menus (its SDL event filter drops non-gamepad events). Fixed upstream in Controlify commit c0ea63c7, not in 3.5.3. Remove Controlify or use a newer build. ALL does not change mod jars.
- Download page: the shader check looks for "iris", "oculus" or "optifine" in a jar name or an OptiFine loader.

## Other
- **Mic to Live Stream is EXPERIMENTAL** (off by default). One unexplained auto-stop at about 18 s was seen once. Use headphones.
- Minecraft 26.x shows a Vulkan information dialog before launch (existing behaviour).
- An install can report "Failed downloads" once on a flaky network; Retry completes it.
- Updating the app while a game is running closes that game; close Minecraft first.
- Minecraft's font does not draw every Thai glyph, so Thai chat can look broken in game. The launcher UI shows Thai correctly.
- App data cannot be backed up with adb (`allowBackup=false`). Updating 1.0.0 over Beta 3 kept accounts, instances, mods, worlds and settings on both test phones.
- Building needs JDK 21 or newer (1.0.0 used JDK 25); JDK 17 crashes the release lint task.
