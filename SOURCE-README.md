# ALL Launcher 1.0.0 — source package (arm64-v8a)

> **ALL Launcher is an Unofficial Modified Version of Zalith Launcher 2.** It is not made, endorsed or supported by the Zalith Launcher developers.
>
> This file describes `ALLLauncher-1.0.0-SOURCE.zip` attached to the [v1.0.0 release](https://github.com/TheBouy3224/ALL-Launcher/releases/tag/v1.0.0). The Git repository holds the same tree except the large third-party archives (they are only in the zip). Open items for a general public release: `PUBLIC-RELEASE-REQUIREMENTS.md`.

## 1. What this is

- Source of ALL Launcher 1.0.0 (applicationId `com.all.launcher.app`, versionCode 21, versionName 1.0.0).
- Input of the arm64-v8a release APK `ALLLauncher-1.0.0-arm64-v8a.apk`, SHA-256 `213f91ef2978d73fca3771f271033d5666ea43e7379ddb617bc95ec9417b2956`.
- Licence: GNU GPL-3.0 (`LICENSE`). Upstream: Zalith Launcher 2 by MovTery and contributors (GPL-3.0 with additional terms), which builds on PojavLauncher and other open-source projects (`NOTICE.md`, About page).
- Two files accompany every APK: this zip and `ALLLauncher-1.0.0-OPENJDK-SOURCES.zip` (the four OpenJDK trees behind the bundled Java runtimes). `BUILD_SOURCE_SHA256_1.0.0.txt` lists every file hash; `CHANGED_vs_rc3_1.0.0.txt` lists changes against the rc3 source package.

## 2. Upstream terms (Zalith Launcher 2, additional terms under GPLv3 section 7)

- The program name must not contain "ZalithLauncher" or "ZL": the name is "ALL Launcher" (`launcher_name=ALLLauncher` in `ZalithLauncher/gradle.properties`).
- Modified versions must say "Unofficial Modified Version" on the startup screen or main interface: done on the sign-in (startup) screen, in the start menu footer and in About.
- Copyright notices must stay: About shows "© 2025 MovTery and contributors · GNU GPL v3"; source headers are kept.

## 3. Build

- Gradle 9.5.0 (wrapper), Kotlin 2.4.10 (`gradle/libs.versions.toml`), Android NDK 25.2.9519653 (`ZalithLauncher/build.gradle.kts`), JDK 25.
- Command: `./gradlew :ZalithLauncher:assembleRelease -Darch=arm64`. The lint task ran; no check was disabled; `ALL_PUBLIC_RELEASE` was not set.
- Needed but **not included**: `local.properties` (`sdk.dir`); the release keystore `../private-signing/all_launcher_release.jks` (alias `all_launcher`) and its passwords. Optional, not included: `.oauth_client_id.txt`, `.curseforge_api.txt` (empty in this build).
- `ZalithLauncher/libs/MobileGlues-release.aar` holds the arm64 `libmobileglues.so` built from `third_party/mobileglues/` (recipe: `third_party/mobileglues/BUILD-RECIPE.md`).
- The other prebuilt native libraries and runtimes in the tree come from Zalith Launcher 2 and are not modified by ALL; their upstream sources are listed in `THIRD-PARTY-SOURCES.md`.

## 4. Not included, by policy

Signing keys and passwords, tokens, account data, `local.properties`, Gradle and Kotlin caches, build outputs. Text files contain no private-key blocks, no token-shaped strings and no local user paths (one third-party e-mail address is part of the MMKV licence text).

## 5. Correspondence with the APK (evidence, not proof)

- The 1.0.0 (versionCode 21) release APK was built on 2026-11-02 (+07:00) from this tree; the package was made right after the build.
- After the release, `url_home` in `ZalithLauncher/gradle.properties` was pointed at this repository; it only feeds `BuildConfig.URL_HOME`, which no code reads.
- Not proven: byte-level equality with a fresh build. Nothing was rebuilt for this package.

## 6. Third-party sources

`THIRD-PARTY-SOURCES.md` gives, per component, the version evidence, where the source is in these two zips, and what is still open. Licence texts: `third_party/licenses/` and the app (About -> Open-source credits and libraries).
