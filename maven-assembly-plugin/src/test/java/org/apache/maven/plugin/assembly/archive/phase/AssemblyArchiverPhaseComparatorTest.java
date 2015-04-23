package org.apache.maven.plugin.assembly.archive.phase;

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
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.artifact.DependencyResolutionException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.codehaus.plexus.archiver.Archiver;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class AssemblyArchiverPhaseComparatorTest
{

    class Basic implements  AssemblyArchiverPhase {
        public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource )
            throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException,
            DependencyResolutionException
        {

        }
    }

    class Ordered1 extends Basic implements PhaseOrder
    {
        public int order()
        {
            return 20;
        }
    }

    class Ordered2 extends Basic implements PhaseOrder
    {
        public int order()
        {
            return 30;
        }
    }
    class Unordered1 extends Basic
    {
    }

    class Unordered2 extends Basic
    {
    }

    @Test
    public void comparatorSortsCorrectly()
        throws Exception
    {
        List<AssemblyArchiverPhase> items = new ArrayList<AssemblyArchiverPhase>(  );
        Unordered2 u2 = new Unordered2();
        items.add( u2 );
        Ordered2 o2 = new Ordered2();
        items.add( o2 );
        Ordered1 o1 = new Ordered1();
        items.add( o1 );
        Unordered1 u1 = new Unordered1();
        items.add( u1 );
        Collections.sort( items, new AssemblyArchiverPhaseComparator() );
        assertSame( u1, items.get( 0 ) );
        assertSame(  u2, items.get(1) );
        assertSame(  o1, items.get(2) );
        assertSame(  o2, items.get(3) );
    }
}