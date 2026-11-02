# MobileGlues (LGPL-2.1) — source bundle

Binary: `libmobileglues.so` for arm64-v8a, SHA-256 `5cc791dc42662d6f653b8f1f7ce85eb555a9ccfcb3b4b42575d680de9c3e0933` (6,411,912 bytes), built for ALL Launcher 1.0.0-rc3 and shipped unchanged in 1.0.0 and shipped unchanged in 1.0.0 and shipped unchanged in 1.0.0 and shipped unchanged in 1.0.0 and shipped unchanged in 1.0.0 and shipped unchanged in 1.0.0 and shipped unchanged in 1.0.0-rc4 and shipped unchanged in 1.0.0-rc4 and shipped unchanged in 1.0.0-rc4. It is inside `ZalithLauncher/libs/MobileGlues-release.aar` (SHA-256 `5d7678795308b538f7bcc1f50c1b8fdc1c07b89a222e03580249f046d4e26439`, arm64-v8a only).

## Files

| File | What it is |
|---|---|
| `MobileGlues-97558a69.tar.gz` (`63f5e072…`) | Upstream MobileGlues at commit `97558a69157b033b890958b1e49258c5b51a80c6` (https://github.com/MobileGL-Dev/MobileGlues). It does not contain submodules. |
| `submodules/` | The four submodules the build uses, at the revisions pinned by that commit: glslang `f5f664de…`, SPIRV-Cross `a0fba56c…`, xxhash `c2866db3…`, flat_hash_map (`include/ska`) `21c1cec9…`. (perfetto is only used when `PROFILING` is on and is not part of this build.) |
| `ALL-mg-26.3-minimal.patch` (`e46a2c4d…`) | The change applied: `MobileGlues-cpp/gl/glsl/glsl_for_es.cpp`, 4 hunks. |
| `ALL-mg-26.3-notice.patch` | Comment-only notice appended after the last line of that file (LGPL-2.1 §2(b); modified for ALL Launcher on 2026-10-30; credits the upstream project and the community patch). Apply after the minimal patch. |
| `origin/mobileglues-mc26.3.patch` | The community patch set (Dap-boss, LGPL-2.1) the change was taken from. Its `glsl_for_es.cpp` section equals the minimal patch after normalising line endings. |
| `BUILD-RECIPE.md`, `build-recipe.sh` | How the binary was built. |
| `LICENSE-LGPL-2.1.txt` | The licence text. |

## What the change does (4 hunks, 84 lines added, 1 removed)

1. Adds `#include <vector>` and `#include <cctype>`.
2. `process_uniform_declarations()`: the `uniform` keyword is matched only at a token boundary (the character before is not a letter, digit or `_`; the character after is whitespace or the end), so identifiers such as `_uniform_instance_00_03` are no longer split.
3. New helpers `find_main_close()` and `legalize_output_arrays()`: for fragment shaders, `layout(location=N) out <type> name[SIZE];` becomes one global array plus `SIZE` scalar outputs `_mg_out_<name>_<i>`, copied from the array before the closing brace of `main()`. The patch's comment gives the reason: Adreno rejects dynamic indexing of fragment output arrays (S0015).
4. `GLSLtoGLSLES_2()` calls `legalize_output_arrays()` for fragment shaders.

## Status

- The shipped binary can be rebuilt from this bundle with the recipe. Two builds of the same sources differ byte for byte (different build-id), so the hash cannot be reproduced exactly; function was checked on a device (Minecraft 26.3: title screen and a world load with the shipped library).
- The binary used by earlier builds (`2c2e8ff2…`, 6,332,360 bytes) had no complete build record and is not part of this release.
- The AAR no longer carries the armeabi-v7a, x86 and x86_64 binaries (provenance never verified).
- LGPL-2.1 §6: the corresponding source accompanies each APK in `ALLLauncher-<version>-SOURCE.zip`.
