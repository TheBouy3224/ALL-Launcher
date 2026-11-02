#!/usr/bin/env bash
# Rebuild libmobileglues.so (arm64-v8a) following the community build.sh recipe.
set -euo pipefail
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
NDK=$SDK/ndk/27.3.13750724
CM=$SDK/cmake/3.30.5/bin
export PATH="$CM:$PATH"
SRC="$1"          # source tree root (has MobileGlues-cpp)
OUT="$2"          # build dir
cmake --version | head -1
cmake -S "$SRC/MobileGlues-cpp" -B "$OUT" -G Ninja \
  -DCMAKE_MAKE_PROGRAM="$CM/ninja.exe" \
  -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-21 \
  -DCMAKE_BUILD_TYPE=Release -DANDROID_STL=c++_static
"$CM/ninja.exe" -C "$OUT" mobileglues
"$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-strip.exe" --strip-unneeded "$OUT/libmobileglues.so"
sha256sum "$OUT/libmobileglues.so"
ls -l "$OUT/libmobileglues.so"
