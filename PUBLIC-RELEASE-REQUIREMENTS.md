# What a public release of ALL Launcher still needs

Status for 1.0.0 (versionCode 21, 2 November 2026). 1.0.0 is published on GitHub for testers and friends; the items below are still open for a general public release.

## 1. Microsoft sign-in (project owner only)

- `ZalithLauncher/build.gradle.kts` reads `OAUTH_CLIENT_ID` from the environment, `.oauth_client_id.txt` or the Gradle property `oauth_client_id`. 1.0.0 was built without one.
- With no id, `MicrosoftClientId.kt` falls back to the public client id of the official Minecraft launcher (`00000000402b5328`). That is another party's identity: acceptable for tests, not for a public release.
- The Public Release Guard (`ALL_PUBLIC_RELEASE=1`) refuses to build unless `OAUTH_CLIENT_ID` is an Azure application (client) id (a GUID). It was not bypassed.
- Needed: an Azure (Microsoft Entra) application owned by ALL Launcher with the device-code flow, approved for Minecraft services. Put the GUID in `.oauth_client_id.txt` (never commit it) and build with `ALL_PUBLIC_RELEASE=1`. Zalith Launcher 2's own client id must not be reused.

## 2. Third-party source items (`THIRD-PARTY-SOURCES.md`)

- **Terracotta** native library: source and dependency revisions are included; the binary was not rebuilt (a rebuild with the upstream cargo-ndk workflow was prepared but not completed).
- **Caciocavallo 1.19.1-SNAPSHOT**: the exact build is not in any public commit; the nearest public source is included.
- **OpenAL Soft**: source of the shipped version is included; its build flags are not recorded.

## 3. Done in 1.0.0

- Unofficial Modified Version notice, name and copyright notices as required by Zalith Launcher 2's additional terms.
- Licence texts and credits in the app (About); `NOTICE.md`.
- Jars without a redistribution licence (`MioLibPatcher`, `forge_installer`, `nide8auth`) removed from the APK and downloaded from their publishers on first use.
- Complete corresponding source published with the APK (GitHub release v1.0.0).
- Updating over Beta 3 tested on two phones with the real package id: data kept.

## 4. Keep

- Publish the APK together with `ALLLauncher-1.0.0-SOURCE.zip` and `ALLLauncher-1.0.0-OPENJDK-SOURCES.zip`.
- Keep the signing key (certificate SHA-256 `b72b76bd56f32ae16a3c10bac9fad1bd68a8d48ada273fcf4530f854089d2177`) private and backed up; only APKs signed with it can update installed copies.
