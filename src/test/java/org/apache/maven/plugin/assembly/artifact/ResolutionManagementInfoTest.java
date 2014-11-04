package org.apache.maven.plugin.assembly.artifact;

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

import junit.framework.TestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class ResolutionManagementInfoTest
    extends TestCase
{

    public void testName()
        throws Exception
    {

    }

    public void testAddSingleArtifactWithReplacemen()
        throws Exception
    {
        ResolutionManagementInfo rmi = new ResolutionManagementInfo( new MavenProject(  ) );
        Artifact a1 = new DefaultArtifact( "groupid", "1", VersionRange.createFromVersion("1.0"), "test", "jar", null, new DefaultArtifactHandler() );
        rmi.addArtifacts( Collections.singleton( a1));
        Artifact a2 = new DefaultArtifact( "groupid", "1", VersionRange.createFromVersion("1.0"), "compile", "jar", null, new DefaultArtifactHandler() );
        rmi.addArtifacts( Collections.singleton( a2));
        assertEquals( 1, rmi.getArtifacts().size());
        Artifact next = rmi.getArtifacts().iterator().next();
        assertEquals("compile", next.getScope());
    }

    public void testAddMultiArtifactWithReplacemen()
        throws Exception
    {
        ResolutionManagementInfo rmi = new ResolutionManagementInfo( new MavenProject(  ) );
        Artifact a1 = new DefaultArtifact( "groupid", "a1", VersionRange.createFromVersion("1.0"), "test", "jar", null, new DefaultArtifactHandler() );
        Artifact a2 = new DefaultArtifact( "groupid", "a2", VersionRange.createFromVersion("1.0"), "test", "jar", null, new DefaultArtifactHandler() );
        Artifact a3 = new DefaultArtifact( "groupid", "a3", VersionRange.createFromVersion("1.0"), "test", "jar", null, new DefaultArtifactHandler() );
        rmi.addArtifacts( new HashSet<Artifact>( Arrays.asList( a1, a2, a3 )));
        Artifact b2 = new DefaultArtifact( "groupid", "a2", VersionRange.createFromVersion("1.0"), "compile", "jar", null, new DefaultArtifactHandler() );
        Artifact b3 = new DefaultArtifact( "groupid", "a3", VersionRange.createFromVersion("1.0"), "compile", "jar", null, new DefaultArtifactHandler() );
        rmi.addArtifacts( new HashSet<Artifact>( Arrays.asList( b2, b3)));
        assertEquals( 3, rmi.getArtifacts().size());
        int compile = 0;
        int test = 0;
        for ( Artifact artifact : rmi.getArtifacts() )
        {
            if ( Artifact.SCOPE_COMPILE.equals( artifact.getScope() )) compile++;
            else test++;
        }
        assertEquals(2, compile);
        assertEquals(1, test);
    }
}