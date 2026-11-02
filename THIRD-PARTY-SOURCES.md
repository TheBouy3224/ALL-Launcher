# Third-party components in ALL Launcher 1.0.0 (arm64-v8a)

Evidence checked on 2026-11-01. "Read from the binary" = taken from strings or metadata inside the shipped file. Components ALL did not modify are shipped as Zalith Launcher 2 ships them; for those the package gives the source of the actual version, not a byte-identical rebuild. Where the exact build record is missing it says so.

## Components ALL rebuilt or modified

| Component | Licence | Source in the package | Recipe / status |
|---|---|---|---|
| MobileGlues `libmobileglues.so` | LGPL-2.1 | `third_party/mobileglues/`: upstream `97558a69…`, four submodule archives at the pinned revisions, minimal patch, notice patch, licence | `BUILD-RECIPE.md` + `build-recipe.sh` (NDK 27.3.13750724, CMake 3.30.5). Closed. Not bit-reproducible. Change notice (LGPL-2.1 section 2(b)) is in the patched file. |
| ALL Launcher (changes on top of Zalith Launcher 2) | GPL-3.0 | the tree itself | Closed. |

## Components shipped as Zalith Launcher 2 ships them

| Component | Licence | Version / revision evidence | Source in the package | Status |
|---|---|---|---|---|
| OpenAL Soft `libopenal.so` | LGPL-2.0 | binary string `1.25.2`; the licence file shipped equals `COPYING` at upstream tag `1.25.2` | `third_party/openal-soft/openal-soft-1.25.2.tar.gz` | Source of the shipped version included. Build flags not recorded. |
| Terracotta native `libterracotta.so` (Rust) | AGPL-3.0 | The native API has three string arguments, which first appears in `burningtnt/Terracotta` branch `feature/android-custom-nodes`, commit `7d3f2f624aba2abc23068e1b710083073cab2b16` (2026-03-07), one day before the project imported it. The binary contains the cargo checkout paths of its git dependencies: EasyTier fork `66c82d0`, smoltcp `0a92676`, kcp-sys `71eff18`, http_req `b10aa9f`, rust-tun `1237883`; built on GitHub Actions (`/home/runner/work/Terracotta/Terracotta`). | `third_party/candidates/terracotta-7d3f2f6-android-custom-nodes.tar.gz` and `third_party/terracotta-deps/` (EasyTier fork `66c82d0a99ae6733463e37eaa3b8c91f584c4689`, smoltcp `0a926767…`, kcp-sys `71eff18c…`, http_req `b10aa9fc…`, rust-tun `12378839…`) | Source and dependency revisions identified and included. Terracotta's own commit is matched by the API change and dates, not proven. crates.io dependencies are not bundled (cargo resolves them by name and version at build time). Build recipe: the repository's cargo workflow (inside the Terracotta archive). |
| OpenJDK Java runtimes `jre-8`, `jre-17`, `jre-21`, `jre-25`, and `libunpack200.so`, `libawt_headless.so`, `libawt_xawt.so` | GPL-2.0 with Classpath Exception | The `release` file inside each runtime names its OpenJDK commit: jre-8 `1a6e3a5ea32d` = tag `jdk8u442-b05`; jre-17 `ca760c86642a` = `jdk-17.0.10+7` (`jdk-17.0.10-ga`); jre-21 `060c4f7589e7` = `jdk-21.0.1+12`; jre-25 `2a26511bdc07` = `jdk-25.0.5+1`. The `+` means the Android patch set was applied. | `ALLLauncher-1.0.0-OPENJDK-SOURCES.zip` (the four trees at those exact commits). Android patches and build scripts: `third_party/openjdk-build/` (jre-25 at FCL commit `7a0266e7`, read from the runtime's `version` file) and `third_party/candidates/fcl-openjdk-build-jre{8,17,21}-*.tar.gz` | Source included. For jre-8/17/21 the FCL commit is the last one before the import and is consistent with the shipped metadata: `clone_jdk.sh` clones `jdk-17.0.10-ga` / `jdk21.0.1`, and the 17 and 21 patches apply cleanly to the exact commits; jre-8 and jre-25 scripts clone the branch tip and apply the patch with `git apply --reject`, so partial application is expected. Build recipe: `ci_build_arch_aarch64.sh`, `clone_jdk.sh`, `build_jdk.sh`, `tar_jdk.sh` and the workflow in the archives. |
| Caciocavallo jars | GPL-2.0 | `net.java.openjdk.cacio` 1.10-SNAPSHOT (cacio-shared, cacio-androidnw): every file inside is dated 2023-07-12 = last commit `9388be3` of `PojavLauncherTeam/caciocavallo`. `com.github.caciocavallosilano` 1.19.1-SNAPSHOT (cacio-shared, cacio-tta, cacio-agent): built 2025-08-23. | `third_party/candidates/cacio-pojav-9388be3.tar.gz`; `third_party/candidates/cacio17-fcl-head-7c8475a.tar.gz` | 1.10-SNAPSHOT: source identified by the date match. 1.19.1-SNAPSHOT: the 2025-08-23 build is not in any public commit found; the FCL repository head (2026-09-23) is a later revision of the same code (same `cacio-agent` class) and is the nearest public source. **Open (limited).** |
| authlib-injector 1.2.8 | AGPL-3.0 or later, with the authlib-injector exception | the shipped `authlib-injector.jar` is byte-identical to the official release jar (SHA-256 `9c7f4343…`) | `third_party/authlib-injector-v1.2.8.tar.gz` (tag `v1.2.8`) | Closed. Licence and credit in the app. |
| ZalithJars (AWTBlockerAgent, JarExceptionCatcher) | Apache-2.0 | upstream repository | `third_party/ZalithJars-75e4265.tar.gz` | Source included; the shipped jars were not rebuilt for comparison. |
| JNA | Apache-2.0 (chosen; dual with LGPL-2.1) | natives for JNA 4.4.0 to 5.18.0 in `jna-arm64.zip` | licence texts | Licence and credit in the app. Source version of the standalone `libjnidispatch.so` not identified (permissive licence chosen). |
| EasyTier (inside Terracotta) | LGPL-3.0 per upstream | fork revision `66c82d0` as above | `third_party/terracotta-deps/easytier-fork-66c82d0a.tar.gz` | Licence and credit in the app. Source included. |

## Helper jars fetched at run time (not in the APK)

ALL does not put these three jars in the APK or the source package: none has a licence that lets ALL redistribute it. The launcher downloads each one, on first use, from the place its publisher already serves it, and checks it (`ZalithLauncher/src/main/java/com/movtery/zalithlauncher/path/OptionalJars.kt`). They are stored in `files/components_dl/`.

| Jar | Downloaded from | Check | Used when |
|---|---|---|---|
| `MioLibPatcher.jar` (Mio, `com.mio.libpatcher`) | Zalith Launcher 2 repository, commit `cbf877a979b9375569010b1021fe885d3a05895a` (the copy Zalith Launcher 2 ships) | SHA-256 `80445f93...3539` and `Premain-Class` | every game launch (`-javaagent`), together with ALL's own `ALLCompatAgent` |
| `forge_installer.jar` (bangbang93) | same Zalith Launcher 2 commit | SHA-256 `7e37ac73...6745` | fetched during a Forge/NeoForge install, as upstream keeps it; no install step runs it |
| `nide8auth.jar` (Nide8) | Nide8's own address `https://login.mc-user.com:233/index/jar` (redirects to `static.mc-user.com`) | `Premain-Class com.nide8.login2.Nide8Auth` (the vendor updates the file, so no fixed hash) | launching a Nide8 (`auth.mc-user.com`) account |

If a download fails, the game still starts without MioLibPatcher; a Nide8 account shows a clear "could not download" error. Other accounts are not affected.

`ResConfHack.jar` (Java 8 only) is byte-identical to the file in PojavLauncher `v3_openjdk` (commit `b12ad048157b3aa255d078c235dd4571e1900309`); its classes derive from OpenJDK (GPL-2.0 with Classpath Exception).

## What changed in the app for licensing (this release)

About now lists OpenAL Soft, OpenJDK, Caciocavallo, JNA, EasyTier, authlib-injector and ZalithJars with licence texts; the MobileGlues licence copy is the official text; the "Unofficial Modified Version" statement appears on the startup screen, start menu and About.
