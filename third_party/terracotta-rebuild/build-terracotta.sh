#!/bin/sh
# ALL Launcher: rebuild of Terracotta android-custom-nodes 7d3f2f6 for arm64-v8a (mirrors .github/workflows/build.yaml "Build (Android)")
export CARGO_HOME=/d/ALL-Rust-Build/cargo RUSTUP_HOME=/d/ALL-Rust-Build/rustup
export PATH=/d/ALL-Rust-Build/shim:/d/ALL-Rust-Build/rustup/toolchains/nightly-2026-03-08-x86_64-pc-windows-gnu/lib/rustlib/x86_64-pc-windows-gnu/bin/self-contained:/d/ALL-Rust-Build/cargo/bin:/d/ALL-Rust-Build/protoc/bin:$PATH
export CC_x86_64_pc_windows_gnu=D:/ALL-Rust-Build/llvm-mingw-20261020-ucrt-x86_64/bin/x86_64-w64-mingw32-clang.exe AR_x86_64_pc_windows_gnu=D:/ALL-Rust-Build/llvm-mingw-20261020-ucrt-x86_64/bin/llvm-ar.exe
export PROTOC=/d/ALL-Rust-Build/protoc/bin/protoc.exe
export ANDROID_NDK_HOME="$ANDROID_SDK_ROOT/ndk/27.3.13750724" NDK_HOME="$ANDROID_SDK_ROOT/ndk/27.3.13750724"
export RUSTFLAGS="-Zunstable-options -Cpanic=unwind"
export TERRACOTTA_VERSION=all-7d3f2f6
cd /d/ALL-Rust-Build/src/Terracotta-7d3f2f6
rustup component add rust-src --toolchain nightly-2026-03-08
cargo +nightly-2026-03-08 ndk -t arm64-v8a build --release --lib   -Z build-std=core,std,alloc,proc_macro,panic_abort -Z build-std-features=default,optimize_for_size
