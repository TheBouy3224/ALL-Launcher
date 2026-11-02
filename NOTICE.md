# NOTICE — ALL Launcher

ALL Launcher is free software under the **GNU General Public License v3.0** (`LICENSE`). It is a **modified version of Zalith Launcher 2**.

**ALL Launcher is an Unofficial Modified Version of Zalith Launcher 2.** It is not made, endorsed or supported by the Zalith Launcher developers. As the upstream additional terms (GPLv3 section 7) require: the program name does not contain "ZalithLauncher" or "ZL"; the app says "Unofficial Modified Version" on its startup screen, in the start menu and in About; the upstream copyright notice stays visible in About (© 2025 MovTery and contributors · GNU GPL v3). Zalith Launcher 2 in turn builds on PojavLauncher and other open-source projects, credited below and in the app.

## Origin and modifications

- ALL Launcher project owner: THE_BOUY2. The ALL triangle logo is the ALL Launcher project's own branding.
- Upstream: Zalith Launcher 2 — Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors — GPL-3.0 — https://github.com/ZalithLauncher/ZalithLauncher2
- The upstream copyright and license headers in the source files are kept. ALL Launcher changes (ALL Desktop interface, ALL installer/loader management, Mod Doctor Family Repair and ALL repair UI, branding, build configuration) are released under the same GPL-3.0 license.
- Files `README_EN_US.md` and `README_ZH_TW.md` are the unmodified upstream documents; `README.md` is ALL Launcher's own.

## Projects acknowledged by the upstream launcher (also shown in the in-app About page)

PojavLauncher, Fold Craft Launcher (FCL), Hello Minecraft! Launcher (HMCL), Plain Craft Launcher 2 (PCL2), BMCLAPI (bangbang93), MCIM mod information mirror, MC百科 (mcmod.cn) and the GitHub/Weblate communities. Their licenses are those of the respective projects; see their repositories (links in About → Acknowledgements).

## Third-party libraries

The app lists its libraries in **About → Third-Party Libraries**, including OpenAL Soft, the OpenJDK-based runtimes, Caciocavallo, JNA and EasyTier, with their licence texts. The per-component source status and version evidence are in `THIRD-PARTY-SOURCES.md` of the source package.

## Prebuilt native library: MobileGlues (LGPL-2.1)

The APK bundles `libmobileglues.so` (arm64-v8a) from the MobileGlues project, licensed under the GNU Lesser General Public License v2.1 (`LICENSE-LGPL-2.1.txt`, also shown in About → Third-Party Libraries).

- Copyright (c) 2025-2026 MobileGL-Dev — https://github.com/MobileGL-Dev/MobileGlues
- Upstream commit used: `97558a69157b033b890958b1e49258c5b51a80c6`
- Modification: `MobileGlues-cpp/gl/glsl/glsl_for_es.cpp` (4 hunks), taken from https://github.com/Dap-boss/MobileGlues-MC26.3-patch (LGPL-2.1). Patch SHA-256: `e46a2c4d…`. Date of change: 2026-10-30 (the day the binary was built from upstream plus this patch, per the ALL project checkpoint). The change notice required by LGPL-2.1 §2(b) is in `third_party/mobileglues/ALL-mg-26.3-notice.patch`.
- Corresponding source, patch and build recipe: `third_party/mobileglues/` in `ALLLauncher-<version>-SOURCE.zip`, which accompanies every distributed APK (LGPL-2.1 §6 route: source with the binary).
- Architectures in this build: arm64-v8a only. Shipped binary SHA-256: `5cc791dc…`.

## Trademarks and services

Minecraft is a trademark of Mojang AB / Microsoft. ALL Launcher is not affiliated with or endorsed by Mojang, Microsoft, Modrinth, CurseForge, Fabric, Quilt, NeoForged or MinecraftForge. Game files, mods and loaders are downloaded from their official sources at the user's request.

## Corresponding source (GPLv3 §6)

Each distributed APK is accompanied by `ALLLauncher-<version>-SOURCE.zip` containing the complete corresponding source of that exact build. The package never contains signing keys, passwords, tokens, account data or `local.properties`.

For ALL Launcher 1.0.0-rc4 the source is split in two files that accompany every APK: `ALLLauncher-1.0.0-rc4-SOURCE.zip` (ALL Launcher; MobileGlues with its build recipe; OpenAL Soft; Terracotta with its pinned dependencies; authlib-injector; ZalithJars; the OpenJDK build scripts and patches; Caciocavallo sources) and `ALLLauncher-1.0.0-rc4-OPENJDK-SOURCES.zip` (the four OpenJDK trees the bundled Java runtimes were built from). Revisions that are identified but not byte-verified, and the few components with no public source, are listed in `THIRD-PARTY-SOURCES.md`.
