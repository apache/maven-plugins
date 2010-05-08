package org.apache.maven.plugins.shade;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingAnnotationAdapter;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.RemappingMethodAdapter;

/**
 * A temporary class to fix a bug in objectweb asm.
 *
 */
public class TempRemappingClassAdapter extends RemappingClassAdapter {
    private static class MethRemapVisitor extends RemappingMethodAdapter {
        public MethRemapVisitor(int access, String desc, MethodVisitor mv, Remapper renamer) {
            super(access, desc, mv, renamer);
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            // The original source from asm did not have the call to remapper.mapDesc()
            AnnotationVisitor av = mv.visitAnnotation(remapper.mapDesc(desc), visible);
            return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
        }
    }

    public TempRemappingClassAdapter(ClassVisitor cv, Remapper remapper) {
        super(cv, remapper);
    }

    protected MethodVisitor createRemappingMethodAdapter(int access, String newDesc, MethodVisitor mv) {
        return new MethRemapVisitor(access, newDesc, mv, remapper);
    }
}
