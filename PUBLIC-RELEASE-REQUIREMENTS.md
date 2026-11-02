# What a public release of ALL Launcher still needs

Checked on 2026-11-01 against the source of 1.0.0. This build is an internal release candidate.

## 1. Microsoft sign-in (cannot be done by anyone but the project owner)

- `ZalithLauncher/build.gradle.kts` reads `OAUTH_CLIENT_ID` from the environment, `.oauth_client_id.txt` or the Gradle property `oauth_client_id`; if none is set the build embeds an empty value.
- With an empty value, `MicrosoftClientId.kt` falls back to the public client id of the official Minecraft launcher (`00000000402b5328`) and the old Microsoft Live device-code endpoint. That is another party's identity. It is fine for tests; a public release must not use it, and the sign-in screen says so ("internal test build").
- The Public Release Guard (`ALL_PUBLIC_RELEASE=1`) refuses to build unless `OAUTH_CLIENT_ID` is an Azure application (client) id (a GUID). It was not bypassed and not set.
- What the owner must provide: an Azure (Microsoft Entra) application registered for ALL Launcher with the device-code flow enabled, approved for Minecraft services where Microsoft requires it. Put the GUID in `.oauth_client_id.txt` (never commit it) and build with `ALL_PUBLIC_RELEASE=1`. Zalith Launcher 2's own client id is not available to forks and must not be reused.

## 2. Open source items

See `THIRD-PARTY-SOURCES.md`: Caciocavallo 1.19.1-SNAPSHOT exact build, , OpenAL Soft build flags, and the Terracotta commit match (identified, not byte-proven).

## 3. Decisions

- Publish the APK, `ALLLauncher-1.0.0-SOURCE.zip` and `ALLLauncher-1.0.0-OPENJDK-SOURCES.zip` together at the same place.
- Keep the signing key (SHA-256 `b72b76bd...`) private and backed up.
- A device test of the real package id needs a phone without Beta 3 installed; tests so far used the isolated QA package built from the same source.
