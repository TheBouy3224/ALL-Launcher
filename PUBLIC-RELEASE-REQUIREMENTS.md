# Public release notes for ALL Launcher 1.1.0 (updated 2026-11-10; replaces the 1.0.0 text)

## 1. Microsoft sign-in (decision of the project owner, facts checked in the source)
- Same code as ALL 1.0.0 and Zalith Launcher 2 (`game/account/microsoft/`, byte-identical to the 1.0.0 source). With `OAUTH_CLIENT_ID` empty, `MicrosoftClientId.kt` uses the public client id of the official Minecraft launcher (`00000000402b5328`, a Microsoft-account "Live" app) on the legacy device-code endpoint `login.live.com/oauth20_connect.srf`, then Xbox Live (`user.auth.xboxlive.com`), XSTS (`xsts.auth.xboxlive.com`, relying party `rp://api.minecraftservices.com/`) and `api.minecraftservices.com`. A client id is public, not a secret.
- ALL has no Microsoft application of its own. Nothing found states that Mojang/Microsoft approve or forbid third-party launchers using that id; ALL claims no approval. A launcher that registers its own Azure app must be approved by Microsoft for the Minecraft API (form `aka.ms/mce-reviewappid`), otherwise `api.minecraftservices.com` answers 403.
- `ALL_PUBLIC_RELEASE=1` is ALL's own build check, not a Microsoft rule: it accepts an empty `OAUTH_CLIENT_ID` (public client path); a non-empty value must be a GUID.
- The sign-in screens say that ALL uses the shared public client id and is not endorsed by Mojang or Microsoft.

## 2. Open source items
See `THIRD-PARTY-SOURCES.md`: Caciocavallo 1.19.1-SNAPSHOT exact build, OpenAL Soft build flags, and the Terracotta commit match (identified, not byte-proven). 1.1.0 adds Tabler Icons (MIT) artwork licence and Noto Sans Thai (OFL).

## 3. Decisions
- Publish the APK, `ALLLauncher-1.1.0-SOURCE.zip` and `ALLLauncher-1.0.0-OPENJDK-SOURCES.zip` (still valid: runtimes unchanged) together at the same place.
- Keep the signing key (certificate SHA-256 `b72b76bd...`) private and backed up.
