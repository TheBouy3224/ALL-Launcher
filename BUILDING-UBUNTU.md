# Building ALL Launcher on Ubuntu

Tested set of tools for ALL Launcher 1.0.0 (versionCode 21). The 1.0.0 APK was built on Windows with exactly these versions; the steps below are the Linux equivalents.

| Tool | Version used for 1.0.0 |
|---|---|
| JDK | Temurin 25.0.5+8 (JDK 21+ works for Gradle; JDK 17 crashes the release lint task) |
| Gradle | 9.5.0 (wrapper, downloaded automatically) |
| Android Gradle Plugin | 9.3.0 |
| Kotlin | 2.4.10 |
| Android SDK platform | 37 minor 2 (`compileSdk = release(37) { minorApiLevel = 2 }` in the app; library modules use 37; `targetSdk = 34`, `minSdk = 26`) |
| Android SDK Build-Tools | 36.0.0 (installed automatically by AGP if missing) |
| Android NDK | 25.2.9519653 |

## 1. Packages

```bash
sudo apt update
sudo apt install -y git unzip curl
```

## 2. JDK 25

Download Temurin 25 for Linux x64 from https://adoptium.net/ and unpack it, for example to `~/jdk-25`:

```bash
export JAVA_HOME="$HOME/jdk-25"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

## 3. Android SDK (command-line tools)

```bash
mkdir -p "$HOME/Android/Sdk/cmdline-tools"
cd "$HOME/Android/Sdk/cmdline-tools"
# download "Command line tools only" for Linux from https://developer.android.com/studio#command-tools
unzip commandlinetools-linux-*_latest.zip && mv cmdline-tools latest
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-37.0" "platforms;android-37.2" "build-tools;36.0.0" "ndk;25.2.9519653"
```

Android Studio on Linux installs the same SDK if you prefer an IDE.

## 4. Clone and configure

```bash
git clone https://github.com/TheBouy3224/ALL-Launcher.git
cd ALL-Launcher
echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
chmod +x gradlew
```

## 5. Build

```bash
./gradlew :ZalithLauncher:assembleDebug -Darch=arm64       # debug APK, package com.all.launcher.app.debug
./gradlew :ZalithLauncher:testDebugUnitTest                # unit tests
```

Output: `ZalithLauncher/build/outputs/apk/debug/`.

### Release build (signed)

The release signing key is **not** in this repository. Restore it from your private backup:

```
private-signing/all_launcher_release.jks              # keystore, alias all_launcher
private-signing/all_launcher_release_password.txt     # or set ALL_STORE_PASSWORD / ALL_KEY_PASSWORD
```

```bash
./gradlew :ZalithLauncher:assembleRelease -Darch=arm64
```

Only APKs signed with the same key (certificate SHA-256 `b72b76bd56f32ae16a3c10bac9fad1bd68a8d48ada273fcf4530f854089d2177`) can update an installed ALL Launcher without losing data. Do not set `ALL_PUBLIC_RELEASE=1` unless you have your own Microsoft (Entra) client id; the build refuses it otherwise.

## Not in the repository

- `private-signing/` (release key and password), `local.properties`, `.oauth_client_id.txt`, `.curseforge_api.txt`
- Large third-party source archives (OpenJDK, Terracotta and its dependencies, Caciocavallo, MobileGlues sources, OpenAL Soft): attached to the GitHub release as `ALLLauncher-1.0.0-SOURCE.zip` and `ALLLauncher-1.0.0-OPENJDK-SOURCES.zip`. The scripts and patches are in `third_party/` and `ALLCompatAgent/`.
