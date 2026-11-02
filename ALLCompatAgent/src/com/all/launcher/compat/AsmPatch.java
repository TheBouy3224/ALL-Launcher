/*
 * ALL Launcher compatibility agent.
 * Copyright (C) 2026 ALL Launcher contributors. Licensed under the GNU GPL v3 (see LICENSE at the repository root).
 *
 * Uses the ASM library that Fabric, Quilt, Forge and NeoForge already put on the game class path.
 */
package com.all.launcher.compat;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class AsmPatch {
    private AsmPatch() {}

    /** Returns the class with every isUsingPojavLauncher()Z body replaced by "return false", or null if there is none. */
    static byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        final boolean[] hit = {false};
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (!CompatAgent.METHOD.equals(name) || !"()Z".equals(desc) || (access & Opcodes.ACC_ABSTRACT) != 0) return mv;
                hit[0] = true;
                mv.visitCode();
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitInsn(Opcodes.IRETURN);
                mv.visitMaxs(1, (access & Opcodes.ACC_STATIC) != 0 ? 0 : 1);
                mv.visitEnd();
                return null; //drop the original body
            }
        }, 0);
        return hit[0] ? writer.toByteArray() : null;
    }
}
