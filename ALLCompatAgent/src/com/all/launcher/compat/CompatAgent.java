/*
 * ALL Launcher compatibility agent.
 * Copyright (C) 2026 ALL Launcher contributors. Licensed under the GNU GPL v3 (see LICENSE at the repository root).
 *
 * Sodium (and forks that copied its check) refuse to start when they detect an Android launcher
 * ("It appears that you are using PojavLauncher, which is not supported when using Sodium").
 * This agent makes the method named isUsingPojavLauncher()Z return false, nothing else.
 * Written from that observable behaviour only; it does not contain code from any other agent.
 */
package com.all.launcher.compat;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;

public final class CompatAgent implements ClassFileTransformer {
    static final String METHOD = "isUsingPojavLauncher";
    private static final byte[] MARKER = METHOD.getBytes(StandardCharsets.UTF_8);

    public static void premain(String args, Instrumentation inst) {
        inst.addTransformer(new CompatAgent());
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> redefined, ProtectionDomain domain, byte[] bytes) {
        //cheap pre-filter: only classes that mention the method name go through ASM (vanilla games never reach ASM)
        if (bytes == null || !contains(bytes, MARKER)) return null;
        try {
            byte[] patched = AsmPatch.patch(bytes);
            if (patched != null) System.out.println("[ALL CompatAgent] patched " + METHOD + " in " + className);
            return patched;
        } catch (Throwable t) {
            //ASM missing or unexpected class shape: leave the class untouched
            System.out.println("[ALL CompatAgent] skipped " + className + ": " + t);
            return null;
        }
    }

    static boolean contains(byte[] data, byte[] needle) {
        outer:
        for (int i = 0, last = data.length - needle.length; i <= last; i++) {
            for (int j = 0; j < needle.length; j++) if (data[i + j] != needle[j]) continue outer;
            return true;
        }
        return false;
    }
}
