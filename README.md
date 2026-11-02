# ALL Launcher

[English](README_EN_US.md) | [繁體中文](README_ZH_TW.md)

ALL Launcher is an Android launcher for **Minecraft: Java Edition** with a desktop-style interface ("ALL Desktop"), a built-in installer for Vanilla, Fabric, Quilt, NeoForge and Forge, instance and mod management, and **Mod Doctor** (checks installed Fabric/Quilt mods and offers a previewed, reversible repair).

ALL Launcher is a **modified version of [Zalith Launcher 2](https://github.com/ZalithLauncher/ZalithLauncher2)** by MovTery and contributors, which in turn builds on PojavLauncher, FCL and other open-source projects (see `NOTICE.md` and the in-app About page). It is **not** an official Zalith Launcher release and is not affiliated with Mojang or Microsoft.

**ALL Launcher is an Unofficial Modified Version.** It is not made, endorsed or supported by the Zalith Launcher developers.

- Package: `com.all.launcher.app` · Version: see `ZalithLauncher/gradle.properties` (`launcher_version_name` / `launcher_version_code`)
- Targets arm64 Android 8.0+ (API 26+)
- License: **GNU GPL v3** (see `LICENSE`)
- Project owner: THE_BOUY2

## Status

**1.0.0** (versionCode 21, 2 November 2026). Download the signed arm64 APK, its source package and SHA-256 sums from the [v1.0.0 release](https://github.com/TheBouy3224/ALL-Launcher/releases/tag/v1.0.0). Known limits are in `KNOWN_ISSUES.md`; what a public release still needs is in `PUBLIC-RELEASE-REQUIREMENTS.md` (Microsoft sign-in currently falls back to a borrowed client id, so treat this build as a release for testers and friends). Release builds disable R8/minification and resource shrinking.

## Source code (GPLv3)

Every APK of ALL Launcher is distributed together with the complete corresponding source for that exact build (`ALLLauncher-<version>-SOURCE.zip`, created from the working tree used to build the APK; it contains no signing keys, passwords or tokens). If you received an APK without it, request it from whoever gave you the APK.

## Building

Requirements: Android SDK platform 37 with NDK 25.2.9519653, **JDK 25** (the 1.0.0 release was built with Temurin 25.0.5; the release lint task crashes on JDK 17), Gradle 9.5.0 wrapper included. Step-by-step setup for Linux: [`BUILDING-UBUNTU.md`](BUILDING-UBUNTU.md).

```
./gradlew :ZalithLauncher:assembleDebug      # debug build (package suffix .debug)
./gradlew :ZalithLauncher:testDebugUnitTest  # unit tests
./gradlew :ZalithLauncher:assembleRelease    # needs your own signing config in private-signing/ (not in this repo)
```

For this AGP 9.3.0 checkout, run Gradle with JDK 21 or newer (JDK 25 is what 1.0.0 used) so the bundled lint can use `List.removeLast`; JDK 17 can fail in lint's JavaDoc parser. The application's Java/Kotlin bytecode target remains 17. See Google's [AGP issue 522845800](https://issuetracker.google.com/issues/522845800).


### Optional keys

- **CurseForge**: no CurseForge API key is bundled. Without one, CurseForge data is served through the community MCIM mirror and Modrinth is unaffected. To use the official CurseForge API, put your own key in the `CURSEFORGE_API_KEY` environment variable or in an untracked `.curseforge_api.txt` at the repository root.
- **Microsoft login client ID**: `OAUTH_CLIENT_ID` / `.oauth_client_id.txt`, same mechanism.
- **Debug signing**: debug builds use the Android default debug key unless you provide the upstream debug keystore and `default_store_password` / `default_key_password` in your own `gradle.properties`.
- **Release signing**: environment variables `ALL_STORE_PASSWORD` / `ALL_KEY_PASSWORD` or a local password file, plus your keystore in `private-signing/` (never committed).
- Select the ABI with `-Darch=arm64` (default builds all ABIs).

Release signing material (`private-signing/`) and `local.properties` are deliberately excluded from version control and from source packages. Use your own key if you build a release yourself; APKs signed with a different key cannot update an installed ALL Launcher.

## Privacy

No analytics or crash-reporting service is built in. Android cloud/device backup is disabled for the app. Stored Microsoft/auth-server tokens and passwords are encrypted at rest with a key in the Android Keystore (AES-256-GCM); older plain-text rows are encrypted automatically on the first launch after an update. Game logs mask the Minecraft access token; if you share a log it can still contain your player name, file paths and mod list. The launcher talks to Mojang/Microsoft (login), Modrinth/CurseForge (mods), Mojang/Fabric/Quilt/NeoForged/MinecraftForge (game and loader files) and, only when you choose to share a log, mclo.gs.
