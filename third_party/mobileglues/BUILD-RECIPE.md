# Build recipe: libmobileglues.so (arm64-v8a) for ALL Launcher 1.0.0-rc3

Built on 2026-11-01 on Windows with the Android SDK tools. No third-party binary was used.

1. Source: `git clone https://github.com/MobileGL-Dev/MobileGlues.git`, `git checkout 97558a69157b033b890958b1e49258c5b51a80c6`.
2. Submodules at the pinned revisions (archives in `submodules/`): `MobileGlues-cpp/3rdparty/glslang` f5f664dee8146676b04a332a7233959fc3ce9681, `MobileGlues-cpp/3rdparty/SPIRV-Cross` a0fba56c34a6700f1724bf9b751da5b488a3775c, `MobileGlues-cpp/3rdparty/xxhash` c2866db364b6ea3a11933e62235ddc166ba18565, `MobileGlues-cpp/include/ska` 21c1cec95abee1beef827e4a7c95f692875d9594.
3. Patches, in order, with `git apply`: `ALL-mg-26.3-minimal.patch` (file blob `c7eee88` becomes `ccab1cc`), then `ALL-mg-26.3-notice.patch` (blob `d43e94b`).
4. Toolchain: Android NDK 27.3.13750724, CMake 3.30.5 and its Ninja (Android SDK `cmake;3.30.5`).
5. Configure and build (`build-recipe.sh`): `cmake -S MobileGlues-cpp -B <dir> -G Ninja -DCMAKE_MAKE_PROGRAM=<ninja> -DCMAKE_TOOLCHAIN_FILE=<ndk>/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-21 -DCMAKE_BUILD_TYPE=Release -DANDROID_STL=c++_static`, then `ninja mobileglues`, then `llvm-strip --unneeded libmobileglues.so`. This follows the `build.sh` of https://github.com/Dap-boss/MobileGlues-MC26.3-patch (NDK 27 is the version it names as tested).
6. Results: without the notice patch `48ccabb3ca4b526a6f8b9d70a2e3713cccea48c8ca0bb86121e60756325bd37c`; with it (shipped) `5cc791dc42662d6f653b8f1f7ce85eb555a9ccfcb3b4b42575d680de9c3e0933`. Both are 6,411,912 bytes and the two differ in many bytes, so builds are not bit-reproducible.
7. The shipped file replaced `jni/arm64-v8a/libmobileglues.so` in `ZalithLauncher/libs/MobileGlues-release.aar`. The in-app shader-cache stamp (`LIB_BUILD` in `MobileGluesRenderer.kt`) was bumped so cached shader translations from older libraries are set aside.
