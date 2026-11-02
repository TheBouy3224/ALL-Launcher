#!/bin/sh
# Builds ALLCompatAgent.jar into the launcher assets. Needs a JDK (11+) and the ASM jar (compile time only; not bundled).
# Usage: ./build.sh /path/to/asm-9.x.jar
set -e
cd "$(dirname "$0")"
ASM="$1"
rm -rf out && mkdir -p out
javac --release 8 -nowarn -cp "$ASM" -d out src/com/all/launcher/compat/*.java
printf 'Manifest-Version: 1.0\nPremain-Class: com.all.launcher.compat.CompatAgent\n' > out/MANIFEST.MF
jar --create --file ../ZalithLauncher/src/main/assets/components/launcher/ALLCompatAgent.jar --manifest out/MANIFEST.MF -C out com
rm -rf out
