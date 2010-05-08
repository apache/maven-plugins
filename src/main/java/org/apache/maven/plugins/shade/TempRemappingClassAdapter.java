package org.apache.maven.plugins.shade;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
 * @see <a href="http://forge.ow2.org/tracker/index.php?func=detail&aid=314982&group_id=23&atid=100023">bug #314982</a>
 */
class TempRemappingClassAdapter
    extends RemappingClassAdapter
{

    private static class MethRemapVisitor
        extends RemappingMethodAdapter
    {
        public MethRemapVisitor( int access, String desc, MethodVisitor mv, Remapper renamer )
        {
            super( access, desc, mv, renamer );
        }

        public AnnotationVisitor visitAnnotation( String desc, boolean visible )
        {
            // The original source from asm did not have the call to remapper.mapDesc()
            AnnotationVisitor av = mv.visitAnnotation( remapper.mapDesc( desc ), visible );
            return av == null ? av : new RemappingAnnotationAdapter( av, remapper );
        }
    }

    public TempRemappingClassAdapter( ClassVisitor cv, Remapper remapper )
    {
        super( cv, remapper );
    }

    protected MethodVisitor createRemappingMethodAdapter( int access, String newDesc, MethodVisitor mv )
    {
        return new MethRemapVisitor( access, newDesc, mv, remapper );
    }

}
