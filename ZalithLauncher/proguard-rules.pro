-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn com.github.luben.zstd.**
-dontwarn org.brotli.dec.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.**

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# SDL
-keep class org.libsdl.app.** { *; }

# Launcher
-keep class org.lwjgl.glfw.CallbackBridge {
    *;
}
-keep class com.oracle.dalvik.VMLauncher {
    *;
}

#
## Hilt
#-keep class dagger.hilt.** { *; }
#-keep class javax.inject.** { *; }
#-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
#-keepclasseswithmembers class * {
#    @dagger.hilt.* <methods>;
#}

# Prevent R8 from over-optimizing constructors (causes StackOverflow with Hilt + proguard-android-optimize.txt)
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.internal.GeneratedEntryPoint <init>(...);
}
-keep,allowobfuscation @dagger.hilt.android.AndroidEntryPoint class *


-keep class com.movtery.zalithlauncher.bridge.** { *; }
-keep class com.movtery.zalithlauncher.utils.device.VulkanChecker {
    *;
}
-keep class com.movtery.zalithlauncher.utils.device.VulkanCapabilities {
    *;
}
-keep interface com.movtery.zalithlauncher.utils.device.VulkanLogCallback {
    *;
}
-keep class com.movtery.zalithlauncher.game.input.CriticalNativeTest {
    *;
}

# Libraries
-keep class com.github.steveice10.opennbt.** { *; }

# SoraEditor language-textmate
-keep class org.jcodings.** { *; }

# Gson models: ALL-owned JSON (crash rule packs, repair-snapshot manifests, learning records, mod metadata...) is read/written by
# reflection with the Kotlin property names as JSON keys. Keep field names and the no-arg constructor (so Kotlin default values apply)
# or R8 renames them (e.g. "signatures" -> "a") and old data/rule packs stop matching. Class names may still be obfuscated.
-keepclassmembers class com.movtery.zalithlauncher.game.version.mod.** {
    <init>();
    <fields>;
}
