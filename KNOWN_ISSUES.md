# Known issues - 1.0.0-rc1 (release candidate, versionCode 10)

Status: RELEASE CANDIDATE, not labelled 1.0.0. Owner approval pending.

- **Microsoft sign-in still uses the temporary shared client ID** (OAUTH_CLIENT_ID not set). A public 1.0.0 needs ALL's own Entra app ID. (BLOCKER for a stable 1.0.0 label; accepted for an RC.)
- **Vanilla Minecraft 1.21.1 menu runs at ~1-2 FPS on TECNO LI9** (Krypton Wrapper / OpenGL ES) when no rendering mod is installed; with Sodium it runs at 60 FPS. Same result in the previous dev build, renderer code unchanged since Beta 3. Not fixed in this RC. Mobile players should install Sodium.
- **Mic to Live Stream is EXPERIMENTAL** (off by default; labelled "ทดลอง"). The recording indicator follows the switch, stops when the game closes. One unexplained auto-stop at ~18 s was seen once and not reproduced. Echo/howl/delay and Simple Voice Chat interplay were not re-measured in this RC; use headphones.
- Minecraft 26.x shows a Vulkan information dialog before launch (existing behaviour).
- Download: the shader check looks for "iris", "oculus" or "optifine" in a jar name or an OptiFine loader; Portrait layout was not verified.
- Install can report "Failed downloads" once on a flaky network; Retry completes it (seen once, 102 files, retry OK).
- Controls Studio: Send Text / Thai chat / F3+G were verified on Beta 3; their source is unchanged in this RC but they were not re-run on the RC build.
- An in-place update while a game is running closes that game; close Minecraft before updating.
- Private app data cannot be backed up by adb (allowBackup=false). Updating over Beta 3 was verified on the QA companion package only (8,212 data files byte-identical before/after); the real com.all.launcher.app was not touched.
- Build needs JDK 21 (lintVital crashes on JDK 17). 60 Hz limit on TECNO LI9 unchanged.

## Controlify 3.5.3 on Minecraft 26.3+ (mouse/keyboard dead)
Controlify 3.5.3 installs a global SDL event filter that drops every non-gamepad event, so menus cannot be clicked and nothing can be typed on ALL (SDL3 render path). Fixed upstream in Controlify commit c0ea63c7 (not in 3.5.3). Workaround: remove Controlify, or use a Controlify build newer than 3.5.3 that contains that commit. ALL does not change or replace mod jars.

## Login gate shows again after the game process restarts (QA build)
In the QA build the ALL sign-in gate can appear again after the app is force-stopped and relaunched when the saved Microsoft account is no longer usable; passing it with the offline QA account works as before. Not changed in the Controls Studio work.

## Thai text inside Minecraft
Minecraft's own font does not draw every Thai glyph (marks above/below letters are dropped or replaced), so a Thai Send Text message is sent correctly but may look broken in the in-game chat. The launcher UI itself shows Thai correctly.
